/*
 * Node representing the frame index.
 * The method SelectAddrFI is used to determine
 * if the specific register is a frame pointer.
 */
def AddrFI : ComplexPattern<iPTR, 1, "SelectAddrFI", [frameindex], []>;

// symbol operand used for matching load and call sequences.
// the size is determined by the program counter size.
def bare_symbol : Operand<i64>;

def SDT_CallSeqStart : SDCallSeqStart<[SDTCisVT<0, i64>, SDTCisVT<1, i64>]>;
def SDT_CallSeqEnd   : SDCallSeqEnd<[SDTCisVT<0, i64>, SDTCisVT<1, i64>]>;

// Target-dependent type requirements
def SDT_CPU_Call : SDTypeProfile<0, -1, [SDTCisVT<0, i64>]>;

class InstFormat<bits<5> val> {
    bits<5> Value = val;
}

class GenericInstCommon<dag outs, dag ins, string opcodestr, string argstr,
           list<dag> pattern, InstFormat format> : Instruction {
    let Namespace = "processornamevalue";

    dag OutOperandList = outs;
    dag InOperandList = ins;
    let AsmString = opcodestr # !if(!empty(argstr), "", "\t" # argstr);
    let Pattern = pattern;
    let TSFlags{4-0} = format.Value;
}

class GenericInst<dag outs, dag ins, string opcodestr, string argstr,
     list<dag> pattern, InstFormat format>
    : GenericInstCommon<outs, ins, opcodestr, argstr, pattern, format> {
    field bits<32> Inst;
    // SoftFail is a field the disassembler can use to provide a way for
    // instructions to not match without killing the whole decode process. It is
    // mainly used for ARM, but Tablegen expects this field to exist or it fails
    // to build the decode table.
    field bits<32> SoftFail = 0;
    let Size = 4;
}

class Pseudo<dag outs, dag ins, list<dag> pattern, string opcodestr = "", string argstr = "">
    : GenericInst<outs, ins, opcodestr, argstr, pattern, InstFormat<0>> {
    let isPseudo = 1;
    let isCodeGenOnly = 1;
}

// Target-independent nodes, but with target-specific formats
def callseq_start : SDNode<"ISD::CALLSEQ_START", SDT_CallSeqStart, [SDNPHasChain, SDNPOutGlue]>;
def callseq_end   : SDNode<"ISD::CALLSEQ_END", SDT_CallSeqEnd, [SDNPHasChain, SDNPOptInGlue, SDNPOutGlue]>;

def target_call : SDNode<"processornamevalueISD::CALL", SDT_CPU_Call,
                           [SDNPHasChain, SDNPOptInGlue, SDNPOutGlue, SDNPVariadic]>;

def target_ret_flag : SDNode<"processornamevalueISD::RET_FLAG", SDTNone,
                           [SDNPHasChain, SDNPOptInGlue, SDNPVariadic]>;

/*
 * ADJCALLSTACKDOWN is a pseudo instruction used to represent the
 * 'CFSetupOpcode', which is needed for the call frame setup
 */
def ADJCALLSTACKDOWN : Instruction
{
    let InOperandList = (ins i64imm:$amt1, i64imm:$amt2); /* i32imm : Operand<i32> */
    let OutOperandList = (outs);
    let Pattern = [ (callseq_start timm:$amt1, timm:$amt2) ];
    let Namespace = "processornamevalue";
    let isPseudo = 1;
    let isCodeGenOnly = 1;
    let Defs = [ X2 ]; // stack pointer
    let Uses = [ X2 ]; // stack pointer
}

/*
 * ADJCALLSTACKUP is a pseudo instruction used to represent the
 * 'CFDestroyOpcode', which is needed for the call frame setup
 */
def ADJCALLSTACKUP : Instruction
{
    let InOperandList = (ins i64imm:$amt1, i64imm:$amt2);
    let OutOperandList = (outs);
    let Pattern = [ (callseq_end timm:$amt1, timm:$amt2) ];
    let Namespace = "processornamevalue";
    let isPseudo = 1;
    let isCodeGenOnly = 1;
    let Defs = [ X2 ]; // stack pointer
    let Uses = [ X2 ]; // stack pointer
}

def RET : Instruction
{
    let Namespace = "processornamevalue";
    let InOperandList = (ins);
    let OutOperandList = (outs);
    let Pattern =  [ (target_ret_flag) ];
    let isTerminator  = 1;
    let isBranch      = 0;
    let isCall        = 0;
    let isReturn      = 1;
    let isPseudo      = 1;
    let isCodeGenOnly = 1;
    let mayLoad       = 0;
    let mayStore      = 0;
    let Defs = [];
    let Uses = [];
}

/*
 * PSEUDO_CALL is a pseudo instruction used to represent the
 * 'target_call', which marks a function call.
 * It will be later expanded into the defined calling sequence during code emission.
 */
def CALL : Instruction
{
    let Namespace = "processornamevalue";
    let InOperandList = (ins bare_symbol:$addr);
    let OutOperandList = (outs);
    let Pattern = [];
    let isTerminator  = 0;
    let isBranch      = 0;
    let isCall        = 1;
    let isReturn      = 0;
    let isPseudo      = 1;
    let isCodeGenOnly = 1;
    let mayLoad       = 0;
    let mayStore      = 0;
    let Defs = [ X1 ];
    let Uses = [];
}

/* Match the call sequence for global and external symbols */
def : Pat<(target_call tglobaladdr:$func), (CALL tglobaladdr:$func)>;
def : Pat<(target_call texternalsym:$func), (CALL texternalsym:$func)>;

def SDT_processornamevalueSelectCC     : SDTypeProfile<1, 5, [SDTCisSameAs<1, 2>,
                                         SDTCisSameAs<0, 4>,
                                         SDTCisSameAs<4, 5>]>;
def target_selectcc  : SDNode<"processornamevalueISD::SELECT_CC", SDT_processornamevalueSelectCC,
                     [SDNPInGlue]>;

def vadl_imm32 : ImmLeaf<i32, [{ return true; }]>, Operand<i32>
{

}

def vadl_imm64 : ImmLeaf<i64, [{ return true; }]>, Operand<i64>
{

}

def vadl_lga : SDNode<"processornamevalueISD::LGA", SDTLoad, [SDNPHasChain, SDNPMayLoad, SDNPMemOperand]>;




def SelectCC_X: Instruction {
    field bits<64> Inst;
    // SoftFail is a field the disassembler can use to provide a way for
    // instructions to not match without killing the whole decode process. It is
    // mainly used for ARM, but Tablegen expects this field to exist or it fails
    // to build the decode table.
    field bits<32> SoftFail = 0;

    bits<7> Opcode = 0;

    let Inst{6-0} = Opcode;

    let Namespace = "processornamevalue";

    dag OutOperandList = (outs X:$dst);
    dag InOperandList = (ins X:$lhs, X:$rhs, vadl_imm64:$imm, X:$truev, X:$falsev);
    let Pattern = [ (set X:$dst, (target_selectcc X:$lhs, X:$rhs,
                (i64 vadl_imm64:$imm), X:$truev, X:$falsev)) ];

    let TSFlags{4-0} = 0;
    let usesCustomInserter = 1;
    let isPseudo = 1;
    let isCodeGenOnly = 1;
}




class RV3264Base_ADD_shamt<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV3264Base_ADD_wrapper";
  let DecoderMethod = "RV3264Base_ADD_shamt_decode_wrapper";
}

def RV3264Base_ADD_shamtAsInt64
    : RV3264Base_ADD_shamt<i64>
    , ImmLeaf<i64, [{ return Imm >= 0 && Imm <= 31 && RV3264Base_ADD_shamt_predicate(Imm); }]>;

def RV3264Base_ADD_shamtAsLabel : RV3264Base_ADD_shamt<OtherVT>;

class RV3264Base_SUB_shamt<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV3264Base_SUB_wrapper";
  let DecoderMethod = "RV3264Base_SUB_shamt_decode_wrapper";
}

def RV3264Base_SUB_shamtAsInt64
    : RV3264Base_SUB_shamt<i64>
    , ImmLeaf<i64, [{ return Imm >= 0 && Imm <= 31 && RV3264Base_SUB_shamt_predicate(Imm); }]>;

def RV3264Base_SUB_shamtAsLabel : RV3264Base_SUB_shamt<OtherVT>;

class RV3264Base_AND_shamt<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV3264Base_AND_wrapper";
  let DecoderMethod = "RV3264Base_AND_shamt_decode_wrapper";
}

def RV3264Base_AND_shamtAsInt64
    : RV3264Base_AND_shamt<i64>
    , ImmLeaf<i64, [{ return Imm >= 0 && Imm <= 31 && RV3264Base_AND_shamt_predicate(Imm); }]>;

def RV3264Base_AND_shamtAsLabel : RV3264Base_AND_shamt<OtherVT>;

class RV3264Base_OR_shamt<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV3264Base_OR_wrapper";
  let DecoderMethod = "RV3264Base_OR_shamt_decode_wrapper";
}

def RV3264Base_OR_shamtAsInt64
    : RV3264Base_OR_shamt<i64>
    , ImmLeaf<i64, [{ return Imm >= 0 && Imm <= 31 && RV3264Base_OR_shamt_predicate(Imm); }]>;

def RV3264Base_OR_shamtAsLabel : RV3264Base_OR_shamt<OtherVT>;

class RV3264Base_XOR_shamt<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV3264Base_XOR_wrapper";
  let DecoderMethod = "RV3264Base_XOR_shamt_decode_wrapper";
}

def RV3264Base_XOR_shamtAsInt64
    : RV3264Base_XOR_shamt<i64>
    , ImmLeaf<i64, [{ return Imm >= 0 && Imm <= 31 && RV3264Base_XOR_shamt_predicate(Imm); }]>;

def RV3264Base_XOR_shamtAsLabel : RV3264Base_XOR_shamt<OtherVT>;

class RV3264Base_SLT_shamt<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV3264Base_SLT_wrapper";
  let DecoderMethod = "RV3264Base_SLT_shamt_decode_wrapper";
}

def RV3264Base_SLT_shamtAsInt64
    : RV3264Base_SLT_shamt<i64>
    , ImmLeaf<i64, [{ return Imm >= 0 && Imm <= 31 && RV3264Base_SLT_shamt_predicate(Imm); }]>;

def RV3264Base_SLT_shamtAsLabel : RV3264Base_SLT_shamt<OtherVT>;

class RV3264Base_SLTU_shamt<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV3264Base_SLTU_wrapper";
  let DecoderMethod = "RV3264Base_SLTU_shamt_decode_wrapper";
}

def RV3264Base_SLTU_shamtAsInt64
    : RV3264Base_SLTU_shamt<i64>
    , ImmLeaf<i64, [{ return Imm >= 0 && Imm <= 31 && RV3264Base_SLTU_shamt_predicate(Imm); }]>;

def RV3264Base_SLTU_shamtAsLabel : RV3264Base_SLTU_shamt<OtherVT>;

class RV3264Base_SLL_shamt<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV3264Base_SLL_wrapper";
  let DecoderMethod = "RV3264Base_SLL_shamt_decode_wrapper";
}

def RV3264Base_SLL_shamtAsInt64
    : RV3264Base_SLL_shamt<i64>
    , ImmLeaf<i64, [{ return Imm >= 0 && Imm <= 31 && RV3264Base_SLL_shamt_predicate(Imm); }]>;

def RV3264Base_SLL_shamtAsLabel : RV3264Base_SLL_shamt<OtherVT>;

class RV3264Base_SRL_shamt<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV3264Base_SRL_wrapper";
  let DecoderMethod = "RV3264Base_SRL_shamt_decode_wrapper";
}

def RV3264Base_SRL_shamtAsInt64
    : RV3264Base_SRL_shamt<i64>
    , ImmLeaf<i64, [{ return Imm >= 0 && Imm <= 31 && RV3264Base_SRL_shamt_predicate(Imm); }]>;

def RV3264Base_SRL_shamtAsLabel : RV3264Base_SRL_shamt<OtherVT>;

class RV3264Base_SRA_shamt<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV3264Base_SRA_wrapper";
  let DecoderMethod = "RV3264Base_SRA_shamt_decode_wrapper";
}

def RV3264Base_SRA_shamtAsInt64
    : RV3264Base_SRA_shamt<i64>
    , ImmLeaf<i64, [{ return Imm >= 0 && Imm <= 31 && RV3264Base_SRA_shamt_predicate(Imm); }]>;

def RV3264Base_SRA_shamtAsLabel : RV3264Base_SRA_shamt<OtherVT>;

class RV3264Base_ADDI_immS<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV3264Base_ADDI_wrapper";
  let DecoderMethod = "RV3264Base_ADDI_immS_decode_wrapper";
}

def RV3264Base_ADDI_immSAsInt64
    : RV3264Base_ADDI_immS<i64>
    , ImmLeaf<i64, [{ return Imm >= -2048 && Imm <= 2047 && RV3264Base_ADDI_immS_predicate(Imm); }]>;

def RV3264Base_ADDI_immSAsLabel : RV3264Base_ADDI_immS<OtherVT>;

class RV3264Base_ANDI_immS<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV3264Base_ANDI_wrapper";
  let DecoderMethod = "RV3264Base_ANDI_immS_decode_wrapper";
}

def RV3264Base_ANDI_immSAsInt64
    : RV3264Base_ANDI_immS<i64>
    , ImmLeaf<i64, [{ return Imm >= -2048 && Imm <= 2047 && RV3264Base_ANDI_immS_predicate(Imm); }]>;

def RV3264Base_ANDI_immSAsLabel : RV3264Base_ANDI_immS<OtherVT>;

class RV3264Base_ORI_immS<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV3264Base_ORI_wrapper";
  let DecoderMethod = "RV3264Base_ORI_immS_decode_wrapper";
}

def RV3264Base_ORI_immSAsInt64
    : RV3264Base_ORI_immS<i64>
    , ImmLeaf<i64, [{ return Imm >= -2048 && Imm <= 2047 && RV3264Base_ORI_immS_predicate(Imm); }]>;

def RV3264Base_ORI_immSAsLabel : RV3264Base_ORI_immS<OtherVT>;

class RV3264Base_XORI_immS<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV3264Base_XORI_wrapper";
  let DecoderMethod = "RV3264Base_XORI_immS_decode_wrapper";
}

def RV3264Base_XORI_immSAsInt64
    : RV3264Base_XORI_immS<i64>
    , ImmLeaf<i64, [{ return Imm >= -2048 && Imm <= 2047 && RV3264Base_XORI_immS_predicate(Imm); }]>;

def RV3264Base_XORI_immSAsLabel : RV3264Base_XORI_immS<OtherVT>;

class RV3264Base_SLTI_immS<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV3264Base_SLTI_wrapper";
  let DecoderMethod = "RV3264Base_SLTI_immS_decode_wrapper";
}

def RV3264Base_SLTI_immSAsInt64
    : RV3264Base_SLTI_immS<i64>
    , ImmLeaf<i64, [{ return Imm >= -2048 && Imm <= 2047 && RV3264Base_SLTI_immS_predicate(Imm); }]>;

def RV3264Base_SLTI_immSAsLabel : RV3264Base_SLTI_immS<OtherVT>;

class RV3264Base_SLTIU_immS<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV3264Base_SLTIU_wrapper";
  let DecoderMethod = "RV3264Base_SLTIU_immS_decode_wrapper";
}

def RV3264Base_SLTIU_immSAsInt64
    : RV3264Base_SLTIU_immS<i64>
    , ImmLeaf<i64, [{ return Imm >= -2048 && Imm <= 2047 && RV3264Base_SLTIU_immS_predicate(Imm); }]>;

def RV3264Base_SLTIU_immSAsLabel : RV3264Base_SLTIU_immS<OtherVT>;

class RV3264Base_AUIPC_immUp<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV3264Base_AUIPC_wrapper";
  let DecoderMethod = "RV3264Base_AUIPC_immUp_decode_wrapper";
}

def RV3264Base_AUIPC_immUpAsInt64
    : RV3264Base_AUIPC_immUp<i64>
    , ImmLeaf<i64, [{ return Imm >= -524288 && Imm <= 524287 && RV3264Base_AUIPC_immUp_predicate(Imm); }]>;

def RV3264Base_AUIPC_immUpAsLabel : RV3264Base_AUIPC_immUp<OtherVT>;

class RV3264Base_LUI_immUp<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV3264Base_LUI_wrapper";
  let DecoderMethod = "RV3264Base_LUI_immUp_decode_wrapper";
}

def RV3264Base_LUI_immUpAsInt64
    : RV3264Base_LUI_immUp<i64>
    , ImmLeaf<i64, [{ return Imm >= -524288 && Imm <= 524287 && RV3264Base_LUI_immUp_predicate(Imm); }]>;

def RV3264Base_LUI_immUpAsLabel : RV3264Base_LUI_immUp<OtherVT>;

class RV3264Base_LB_immS<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV3264Base_LB_wrapper";
  let DecoderMethod = "RV3264Base_LB_immS_decode_wrapper";
}

def RV3264Base_LB_immSAsInt64
    : RV3264Base_LB_immS<i64>
    , ImmLeaf<i64, [{ return Imm >= -2048 && Imm <= 2047 && RV3264Base_LB_immS_predicate(Imm); }]>;

def RV3264Base_LB_immSAsLabel : RV3264Base_LB_immS<OtherVT>;

class RV3264Base_LBU_immS<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV3264Base_LBU_wrapper";
  let DecoderMethod = "RV3264Base_LBU_immS_decode_wrapper";
}

def RV3264Base_LBU_immSAsInt64
    : RV3264Base_LBU_immS<i64>
    , ImmLeaf<i64, [{ return Imm >= -2048 && Imm <= 2047 && RV3264Base_LBU_immS_predicate(Imm); }]>;

def RV3264Base_LBU_immSAsLabel : RV3264Base_LBU_immS<OtherVT>;

class RV3264Base_LH_immS<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV3264Base_LH_wrapper";
  let DecoderMethod = "RV3264Base_LH_immS_decode_wrapper";
}

def RV3264Base_LH_immSAsInt64
    : RV3264Base_LH_immS<i64>
    , ImmLeaf<i64, [{ return Imm >= -2048 && Imm <= 2047 && RV3264Base_LH_immS_predicate(Imm); }]>;

def RV3264Base_LH_immSAsLabel : RV3264Base_LH_immS<OtherVT>;

class RV3264Base_LHU_immS<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV3264Base_LHU_wrapper";
  let DecoderMethod = "RV3264Base_LHU_immS_decode_wrapper";
}

def RV3264Base_LHU_immSAsInt64
    : RV3264Base_LHU_immS<i64>
    , ImmLeaf<i64, [{ return Imm >= -2048 && Imm <= 2047 && RV3264Base_LHU_immS_predicate(Imm); }]>;

def RV3264Base_LHU_immSAsLabel : RV3264Base_LHU_immS<OtherVT>;

class RV3264Base_LW_immS<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV3264Base_LW_wrapper";
  let DecoderMethod = "RV3264Base_LW_immS_decode_wrapper";
}

def RV3264Base_LW_immSAsInt64
    : RV3264Base_LW_immS<i64>
    , ImmLeaf<i64, [{ return Imm >= -2048 && Imm <= 2047 && RV3264Base_LW_immS_predicate(Imm); }]>;

def RV3264Base_LW_immSAsLabel : RV3264Base_LW_immS<OtherVT>;

class RV3264Base_SB_immS<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV3264Base_SB_wrapper";
  let DecoderMethod = "RV3264Base_SB_immS_decode_wrapper";
}

def RV3264Base_SB_immSAsInt64
    : RV3264Base_SB_immS<i64>
    , ImmLeaf<i64, [{ return Imm >= -2048 && Imm <= 2047 && RV3264Base_SB_immS_predicate(Imm); }]>;

def RV3264Base_SB_immSAsLabel : RV3264Base_SB_immS<OtherVT>;

class RV3264Base_SH_immS<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV3264Base_SH_wrapper";
  let DecoderMethod = "RV3264Base_SH_immS_decode_wrapper";
}

def RV3264Base_SH_immSAsInt64
    : RV3264Base_SH_immS<i64>
    , ImmLeaf<i64, [{ return Imm >= -2048 && Imm <= 2047 && RV3264Base_SH_immS_predicate(Imm); }]>;

def RV3264Base_SH_immSAsLabel : RV3264Base_SH_immS<OtherVT>;

class RV3264Base_SW_immS<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV3264Base_SW_wrapper";
  let DecoderMethod = "RV3264Base_SW_immS_decode_wrapper";
}

def RV3264Base_SW_immSAsInt64
    : RV3264Base_SW_immS<i64>
    , ImmLeaf<i64, [{ return Imm >= -2048 && Imm <= 2047 && RV3264Base_SW_immS_predicate(Imm); }]>;

def RV3264Base_SW_immSAsLabel : RV3264Base_SW_immS<OtherVT>;

class RV3264Base_BEQ_immS<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV3264Base_BEQ_wrapper";
  let DecoderMethod = "RV3264Base_BEQ_immS_decode_wrapper";
}

def RV3264Base_BEQ_immSAsInt64
    : RV3264Base_BEQ_immS<i64>
    , ImmLeaf<i64, [{ return Imm >= -2048 && Imm <= 2047 && RV3264Base_BEQ_immS_predicate(Imm); }]>;

def RV3264Base_BEQ_immSAsLabel : RV3264Base_BEQ_immS<OtherVT>;

class RV3264Base_BNE_immS<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV3264Base_BNE_wrapper";
  let DecoderMethod = "RV3264Base_BNE_immS_decode_wrapper";
}

def RV3264Base_BNE_immSAsInt64
    : RV3264Base_BNE_immS<i64>
    , ImmLeaf<i64, [{ return Imm >= -2048 && Imm <= 2047 && RV3264Base_BNE_immS_predicate(Imm); }]>;

def RV3264Base_BNE_immSAsLabel : RV3264Base_BNE_immS<OtherVT>;

class RV3264Base_BGE_immS<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV3264Base_BGE_wrapper";
  let DecoderMethod = "RV3264Base_BGE_immS_decode_wrapper";
}

def RV3264Base_BGE_immSAsInt64
    : RV3264Base_BGE_immS<i64>
    , ImmLeaf<i64, [{ return Imm >= -2048 && Imm <= 2047 && RV3264Base_BGE_immS_predicate(Imm); }]>;

def RV3264Base_BGE_immSAsLabel : RV3264Base_BGE_immS<OtherVT>;

class RV3264Base_BGEU_immS<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV3264Base_BGEU_wrapper";
  let DecoderMethod = "RV3264Base_BGEU_immS_decode_wrapper";
}

def RV3264Base_BGEU_immSAsInt64
    : RV3264Base_BGEU_immS<i64>
    , ImmLeaf<i64, [{ return Imm >= -2048 && Imm <= 2047 && RV3264Base_BGEU_immS_predicate(Imm); }]>;

def RV3264Base_BGEU_immSAsLabel : RV3264Base_BGEU_immS<OtherVT>;

class RV3264Base_BLT_immS<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV3264Base_BLT_wrapper";
  let DecoderMethod = "RV3264Base_BLT_immS_decode_wrapper";
}

def RV3264Base_BLT_immSAsInt64
    : RV3264Base_BLT_immS<i64>
    , ImmLeaf<i64, [{ return Imm >= -2048 && Imm <= 2047 && RV3264Base_BLT_immS_predicate(Imm); }]>;

def RV3264Base_BLT_immSAsLabel : RV3264Base_BLT_immS<OtherVT>;

class RV3264Base_BLTU_immS<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV3264Base_BLTU_wrapper";
  let DecoderMethod = "RV3264Base_BLTU_immS_decode_wrapper";
}

def RV3264Base_BLTU_immSAsInt64
    : RV3264Base_BLTU_immS<i64>
    , ImmLeaf<i64, [{ return Imm >= -2048 && Imm <= 2047 && RV3264Base_BLTU_immS_predicate(Imm); }]>;

def RV3264Base_BLTU_immSAsLabel : RV3264Base_BLTU_immS<OtherVT>;

class RV3264Base_JAL_immS<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV3264Base_JAL_wrapper";
  let DecoderMethod = "RV3264Base_JAL_immS_decode_wrapper";
}

def RV3264Base_JAL_immSAsInt64
    : RV3264Base_JAL_immS<i64>
    , ImmLeaf<i64, [{ return Imm >= -524288 && Imm <= 524287 && RV3264Base_JAL_immS_predicate(Imm); }]>;

def RV3264Base_JAL_immSAsLabel : RV3264Base_JAL_immS<OtherVT>;

class RV3264Base_JALR_immS<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV3264Base_JALR_wrapper";
  let DecoderMethod = "RV3264Base_JALR_immS_decode_wrapper";
}

def RV3264Base_JALR_immSAsInt64
    : RV3264Base_JALR_immS<i64>
    , ImmLeaf<i64, [{ return Imm >= -2048 && Imm <= 2047 && RV3264Base_JALR_immS_predicate(Imm); }]>;

def RV3264Base_JALR_immSAsLabel : RV3264Base_JALR_immS<OtherVT>;

class RV3264Base_LWU_immS<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV3264Base_LWU_wrapper";
  let DecoderMethod = "RV3264Base_LWU_immS_decode_wrapper";
}

def RV3264Base_LWU_immSAsInt64
    : RV3264Base_LWU_immS<i64>
    , ImmLeaf<i64, [{ return Imm >= -2048 && Imm <= 2047 && RV3264Base_LWU_immS_predicate(Imm); }]>;

def RV3264Base_LWU_immSAsLabel : RV3264Base_LWU_immS<OtherVT>;

class RV3264Base_LD_immS<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV3264Base_LD_wrapper";
  let DecoderMethod = "RV3264Base_LD_immS_decode_wrapper";
}

def RV3264Base_LD_immSAsInt64
    : RV3264Base_LD_immS<i64>
    , ImmLeaf<i64, [{ return Imm >= -2048 && Imm <= 2047 && RV3264Base_LD_immS_predicate(Imm); }]>;

def RV3264Base_LD_immSAsLabel : RV3264Base_LD_immS<OtherVT>;

class RV3264Base_SD_immS<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV3264Base_SD_wrapper";
  let DecoderMethod = "RV3264Base_SD_immS_decode_wrapper";
}

def RV3264Base_SD_immSAsInt64
    : RV3264Base_SD_immS<i64>
    , ImmLeaf<i64, [{ return Imm >= -2048 && Imm <= 2047 && RV3264Base_SD_immS_predicate(Imm); }]>;

def RV3264Base_SD_immSAsLabel : RV3264Base_SD_immS<OtherVT>;

class RV3264Base_ADDIW_immS<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV3264Base_ADDIW_wrapper";
  let DecoderMethod = "RV3264Base_ADDIW_immS_decode_wrapper";
}

def RV3264Base_ADDIW_immSAsInt64
    : RV3264Base_ADDIW_immS<i64>
    , ImmLeaf<i64, [{ return Imm >= -2048 && Imm <= 2047 && RV3264Base_ADDIW_immS_predicate(Imm); }]>;

def RV3264Base_ADDIW_immSAsLabel : RV3264Base_ADDIW_immS<OtherVT>;

class RV3264Base_SLLIW_shamt<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV3264Base_SLLIW_wrapper";
  let DecoderMethod = "RV3264Base_SLLIW_shamt_decode_wrapper";
}

def RV3264Base_SLLIW_shamtAsInt64
    : RV3264Base_SLLIW_shamt<i64>
    , ImmLeaf<i64, [{ return Imm >= 0 && Imm <= 31 && RV3264Base_SLLIW_shamt_predicate(Imm); }]>;

def RV3264Base_SLLIW_shamtAsLabel : RV3264Base_SLLIW_shamt<OtherVT>;

class RV3264Base_SRLIW_shamt<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV3264Base_SRLIW_wrapper";
  let DecoderMethod = "RV3264Base_SRLIW_shamt_decode_wrapper";
}

def RV3264Base_SRLIW_shamtAsInt64
    : RV3264Base_SRLIW_shamt<i64>
    , ImmLeaf<i64, [{ return Imm >= 0 && Imm <= 31 && RV3264Base_SRLIW_shamt_predicate(Imm); }]>;

def RV3264Base_SRLIW_shamtAsLabel : RV3264Base_SRLIW_shamt<OtherVT>;

class RV3264Base_SRAIW_shamt<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV3264Base_SRAIW_wrapper";
  let DecoderMethod = "RV3264Base_SRAIW_shamt_decode_wrapper";
}

def RV3264Base_SRAIW_shamtAsInt64
    : RV3264Base_SRAIW_shamt<i64>
    , ImmLeaf<i64, [{ return Imm >= 0 && Imm <= 31 && RV3264Base_SRAIW_shamt_predicate(Imm); }]>;

def RV3264Base_SRAIW_shamtAsLabel : RV3264Base_SRAIW_shamt<OtherVT>;

class RV3264Base_ADDW_shamt<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV3264Base_ADDW_wrapper";
  let DecoderMethod = "RV3264Base_ADDW_shamt_decode_wrapper";
}

def RV3264Base_ADDW_shamtAsInt64
    : RV3264Base_ADDW_shamt<i64>
    , ImmLeaf<i64, [{ return Imm >= 0 && Imm <= 31 && RV3264Base_ADDW_shamt_predicate(Imm); }]>;

def RV3264Base_ADDW_shamtAsLabel : RV3264Base_ADDW_shamt<OtherVT>;

class RV3264Base_SUBW_shamt<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV3264Base_SUBW_wrapper";
  let DecoderMethod = "RV3264Base_SUBW_shamt_decode_wrapper";
}

def RV3264Base_SUBW_shamtAsInt64
    : RV3264Base_SUBW_shamt<i64>
    , ImmLeaf<i64, [{ return Imm >= 0 && Imm <= 31 && RV3264Base_SUBW_shamt_predicate(Imm); }]>;

def RV3264Base_SUBW_shamtAsLabel : RV3264Base_SUBW_shamt<OtherVT>;

class RV3264Base_SLLW_shamt<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV3264Base_SLLW_wrapper";
  let DecoderMethod = "RV3264Base_SLLW_shamt_decode_wrapper";
}

def RV3264Base_SLLW_shamtAsInt64
    : RV3264Base_SLLW_shamt<i64>
    , ImmLeaf<i64, [{ return Imm >= 0 && Imm <= 31 && RV3264Base_SLLW_shamt_predicate(Imm); }]>;

def RV3264Base_SLLW_shamtAsLabel : RV3264Base_SLLW_shamt<OtherVT>;

class RV3264Base_SRLW_shamt<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV3264Base_SRLW_wrapper";
  let DecoderMethod = "RV3264Base_SRLW_shamt_decode_wrapper";
}

def RV3264Base_SRLW_shamtAsInt64
    : RV3264Base_SRLW_shamt<i64>
    , ImmLeaf<i64, [{ return Imm >= 0 && Imm <= 31 && RV3264Base_SRLW_shamt_predicate(Imm); }]>;

def RV3264Base_SRLW_shamtAsLabel : RV3264Base_SRLW_shamt<OtherVT>;

class RV3264Base_SRAW_shamt<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV3264Base_SRAW_wrapper";
  let DecoderMethod = "RV3264Base_SRAW_shamt_decode_wrapper";
}

def RV3264Base_SRAW_shamtAsInt64
    : RV3264Base_SRAW_shamt<i64>
    , ImmLeaf<i64, [{ return Imm >= 0 && Imm <= 31 && RV3264Base_SRAW_shamt_predicate(Imm); }]>;

def RV3264Base_SRAW_shamtAsLabel : RV3264Base_SRAW_shamt<OtherVT>;

class RV3264Base_SLLI_shamt<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV3264Base_SLLI_wrapper";
  let DecoderMethod = "RV3264Base_SLLI_shamt_decode_wrapper";
}

def RV3264Base_SLLI_shamtAsInt64
    : RV3264Base_SLLI_shamt<i64>
    , ImmLeaf<i64, [{ return Imm >= 0 && Imm <= 63 && RV3264Base_SLLI_shamt_predicate(Imm); }]>;

def RV3264Base_SLLI_shamtAsLabel : RV3264Base_SLLI_shamt<OtherVT>;

class RV3264Base_SRLI_shamt<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV3264Base_SRLI_wrapper";
  let DecoderMethod = "RV3264Base_SRLI_shamt_decode_wrapper";
}

def RV3264Base_SRLI_shamtAsInt64
    : RV3264Base_SRLI_shamt<i64>
    , ImmLeaf<i64, [{ return Imm >= 0 && Imm <= 63 && RV3264Base_SRLI_shamt_predicate(Imm); }]>;

def RV3264Base_SRLI_shamtAsLabel : RV3264Base_SRLI_shamt<OtherVT>;

class RV3264Base_SRAI_shamt<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV3264Base_SRAI_wrapper";
  let DecoderMethod = "RV3264Base_SRAI_shamt_decode_wrapper";
}

def RV3264Base_SRAI_shamtAsInt64
    : RV3264Base_SRAI_shamt<i64>
    , ImmLeaf<i64, [{ return Imm >= 0 && Imm <= 63 && RV3264Base_SRAI_shamt_predicate(Imm); }]>;

def RV3264Base_SRAI_shamtAsLabel : RV3264Base_SRAI_shamt<OtherVT>;

class RV3264I_ECALL_immS<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV3264I_ECALL_wrapper";
  let DecoderMethod = "RV3264I_ECALL_immS_decode_wrapper";
}

def RV3264I_ECALL_immSAsInt64
    : RV3264I_ECALL_immS<i64>
    , ImmLeaf<i64, [{ return Imm >= -2048 && Imm <= 2047 && RV3264I_ECALL_immS_predicate(Imm); }]>;

def RV3264I_ECALL_immSAsLabel : RV3264I_ECALL_immS<OtherVT>;

class RV3264I_EBREAK_immS<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV3264I_EBREAK_wrapper";
  let DecoderMethod = "RV3264I_EBREAK_immS_decode_wrapper";
}

def RV3264I_EBREAK_immSAsInt64
    : RV3264I_EBREAK_immS<i64>
    , ImmLeaf<i64, [{ return Imm >= -2048 && Imm <= 2047 && RV3264I_EBREAK_immS_predicate(Imm); }]>;

def RV3264I_EBREAK_immSAsLabel : RV3264I_EBREAK_immS<OtherVT>;

class RV3264M_MUL_shamt<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV3264M_MUL_wrapper";
  let DecoderMethod = "RV3264M_MUL_shamt_decode_wrapper";
}

def RV3264M_MUL_shamtAsInt64
    : RV3264M_MUL_shamt<i64>
    , ImmLeaf<i64, [{ return Imm >= 0 && Imm <= 31 && RV3264M_MUL_shamt_predicate(Imm); }]>;

def RV3264M_MUL_shamtAsLabel : RV3264M_MUL_shamt<OtherVT>;

class RV3264M_MULH_shamt<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV3264M_MULH_wrapper";
  let DecoderMethod = "RV3264M_MULH_shamt_decode_wrapper";
}

def RV3264M_MULH_shamtAsInt64
    : RV3264M_MULH_shamt<i64>
    , ImmLeaf<i64, [{ return Imm >= 0 && Imm <= 31 && RV3264M_MULH_shamt_predicate(Imm); }]>;

def RV3264M_MULH_shamtAsLabel : RV3264M_MULH_shamt<OtherVT>;

class RV3264M_MULHSU_shamt<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV3264M_MULHSU_wrapper";
  let DecoderMethod = "RV3264M_MULHSU_shamt_decode_wrapper";
}

def RV3264M_MULHSU_shamtAsInt64
    : RV3264M_MULHSU_shamt<i64>
    , ImmLeaf<i64, [{ return Imm >= 0 && Imm <= 31 && RV3264M_MULHSU_shamt_predicate(Imm); }]>;

def RV3264M_MULHSU_shamtAsLabel : RV3264M_MULHSU_shamt<OtherVT>;

class RV3264M_MULHU_shamt<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV3264M_MULHU_wrapper";
  let DecoderMethod = "RV3264M_MULHU_shamt_decode_wrapper";
}

def RV3264M_MULHU_shamtAsInt64
    : RV3264M_MULHU_shamt<i64>
    , ImmLeaf<i64, [{ return Imm >= 0 && Imm <= 31 && RV3264M_MULHU_shamt_predicate(Imm); }]>;

def RV3264M_MULHU_shamtAsLabel : RV3264M_MULHU_shamt<OtherVT>;

class RV3264M_DIV_shamt<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV3264M_DIV_wrapper";
  let DecoderMethod = "RV3264M_DIV_shamt_decode_wrapper";
}

def RV3264M_DIV_shamtAsInt64
    : RV3264M_DIV_shamt<i64>
    , ImmLeaf<i64, [{ return Imm >= 0 && Imm <= 31 && RV3264M_DIV_shamt_predicate(Imm); }]>;

def RV3264M_DIV_shamtAsLabel : RV3264M_DIV_shamt<OtherVT>;

class RV3264M_DIVU_shamt<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV3264M_DIVU_wrapper";
  let DecoderMethod = "RV3264M_DIVU_shamt_decode_wrapper";
}

def RV3264M_DIVU_shamtAsInt64
    : RV3264M_DIVU_shamt<i64>
    , ImmLeaf<i64, [{ return Imm >= 0 && Imm <= 31 && RV3264M_DIVU_shamt_predicate(Imm); }]>;

def RV3264M_DIVU_shamtAsLabel : RV3264M_DIVU_shamt<OtherVT>;

class RV3264M_REM_shamt<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV3264M_REM_wrapper";
  let DecoderMethod = "RV3264M_REM_shamt_decode_wrapper";
}

def RV3264M_REM_shamtAsInt64
    : RV3264M_REM_shamt<i64>
    , ImmLeaf<i64, [{ return Imm >= 0 && Imm <= 31 && RV3264M_REM_shamt_predicate(Imm); }]>;

def RV3264M_REM_shamtAsLabel : RV3264M_REM_shamt<OtherVT>;

class RV3264M_REMU_shamt<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV3264M_REMU_wrapper";
  let DecoderMethod = "RV3264M_REMU_shamt_decode_wrapper";
}

def RV3264M_REMU_shamtAsInt64
    : RV3264M_REMU_shamt<i64>
    , ImmLeaf<i64, [{ return Imm >= 0 && Imm <= 31 && RV3264M_REMU_shamt_predicate(Imm); }]>;

def RV3264M_REMU_shamtAsLabel : RV3264M_REMU_shamt<OtherVT>;

class RV3264M_MULW_shamt<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV3264M_MULW_wrapper";
  let DecoderMethod = "RV3264M_MULW_shamt_decode_wrapper";
}

def RV3264M_MULW_shamtAsInt64
    : RV3264M_MULW_shamt<i64>
    , ImmLeaf<i64, [{ return Imm >= 0 && Imm <= 31 && RV3264M_MULW_shamt_predicate(Imm); }]>;

def RV3264M_MULW_shamtAsLabel : RV3264M_MULW_shamt<OtherVT>;

class RV3264M_DIVW_shamt<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV3264M_DIVW_wrapper";
  let DecoderMethod = "RV3264M_DIVW_shamt_decode_wrapper";
}

def RV3264M_DIVW_shamtAsInt64
    : RV3264M_DIVW_shamt<i64>
    , ImmLeaf<i64, [{ return Imm >= 0 && Imm <= 31 && RV3264M_DIVW_shamt_predicate(Imm); }]>;

def RV3264M_DIVW_shamtAsLabel : RV3264M_DIVW_shamt<OtherVT>;

class RV3264M_DIVUW_shamt<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV3264M_DIVUW_wrapper";
  let DecoderMethod = "RV3264M_DIVUW_shamt_decode_wrapper";
}

def RV3264M_DIVUW_shamtAsInt64
    : RV3264M_DIVUW_shamt<i64>
    , ImmLeaf<i64, [{ return Imm >= 0 && Imm <= 31 && RV3264M_DIVUW_shamt_predicate(Imm); }]>;

def RV3264M_DIVUW_shamtAsLabel : RV3264M_DIVUW_shamt<OtherVT>;

class RV3264M_REMW_shamt<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV3264M_REMW_wrapper";
  let DecoderMethod = "RV3264M_REMW_shamt_decode_wrapper";
}

def RV3264M_REMW_shamtAsInt64
    : RV3264M_REMW_shamt<i64>
    , ImmLeaf<i64, [{ return Imm >= 0 && Imm <= 31 && RV3264M_REMW_shamt_predicate(Imm); }]>;

def RV3264M_REMW_shamtAsLabel : RV3264M_REMW_shamt<OtherVT>;

class RV3264M_REMUW_shamt<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV3264M_REMUW_wrapper";
  let DecoderMethod = "RV3264M_REMUW_shamt_decode_wrapper";
}

def RV3264M_REMUW_shamtAsInt64
    : RV3264M_REMUW_shamt<i64>
    , ImmLeaf<i64, [{ return Imm >= 0 && Imm <= 31 && RV3264M_REMUW_shamt_predicate(Imm); }]>;

def RV3264M_REMUW_shamtAsLabel : RV3264M_REMUW_shamt<OtherVT>;

class RV3264Base_J_immS<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV3264Base_J_wrapper";
  let DecoderMethod = "RV3264Base_J_immS_decode_wrapper";
}

def RV3264Base_J_immSAsInt64
    : RV3264Base_J_immS<i64>
    , ImmLeaf<i64, [{ return Imm >= -524288 && Imm <= 524287 && RV3264Base_J_immS_predicate(Imm); }]>;

def RV3264Base_J_immSAsLabel : RV3264Base_J_immS<OtherVT>;

class RV3264Base_BEQZ_immS<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV3264Base_BEQZ_wrapper";
  let DecoderMethod = "RV3264Base_BEQZ_immS_decode_wrapper";
}

def RV3264Base_BEQZ_immSAsInt64
    : RV3264Base_BEQZ_immS<i64>
    , ImmLeaf<i64, [{ return Imm >= -2048 && Imm <= 2047 && RV3264Base_BEQZ_immS_predicate(Imm); }]>;

def RV3264Base_BEQZ_immSAsLabel : RV3264Base_BEQZ_immS<OtherVT>;

class RV3264Base_BNEZ_immS<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV3264Base_BNEZ_wrapper";
  let DecoderMethod = "RV3264Base_BNEZ_immS_decode_wrapper";
}

def RV3264Base_BNEZ_immSAsInt64
    : RV3264Base_BNEZ_immS<i64>
    , ImmLeaf<i64, [{ return Imm >= -2048 && Imm <= 2047 && RV3264Base_BNEZ_immS_predicate(Imm); }]>;

def RV3264Base_BNEZ_immSAsLabel : RV3264Base_BNEZ_immS<OtherVT>;

class RV3264Base_BLEZ_immS<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV3264Base_BLEZ_wrapper";
  let DecoderMethod = "RV3264Base_BLEZ_immS_decode_wrapper";
}

def RV3264Base_BLEZ_immSAsInt64
    : RV3264Base_BLEZ_immS<i64>
    , ImmLeaf<i64, [{ return Imm >= -2048 && Imm <= 2047 && RV3264Base_BLEZ_immS_predicate(Imm); }]>;

def RV3264Base_BLEZ_immSAsLabel : RV3264Base_BLEZ_immS<OtherVT>;

class RV3264Base_BGEZ_immS<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV3264Base_BGEZ_wrapper";
  let DecoderMethod = "RV3264Base_BGEZ_immS_decode_wrapper";
}

def RV3264Base_BGEZ_immSAsInt64
    : RV3264Base_BGEZ_immS<i64>
    , ImmLeaf<i64, [{ return Imm >= -2048 && Imm <= 2047 && RV3264Base_BGEZ_immS_predicate(Imm); }]>;

def RV3264Base_BGEZ_immSAsLabel : RV3264Base_BGEZ_immS<OtherVT>;

class RV3264Base_BLTZ_immS<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV3264Base_BLTZ_wrapper";
  let DecoderMethod = "RV3264Base_BLTZ_immS_decode_wrapper";
}

def RV3264Base_BLTZ_immSAsInt64
    : RV3264Base_BLTZ_immS<i64>
    , ImmLeaf<i64, [{ return Imm >= -2048 && Imm <= 2047 && RV3264Base_BLTZ_immS_predicate(Imm); }]>;

def RV3264Base_BLTZ_immSAsLabel : RV3264Base_BLTZ_immS<OtherVT>;

class RV3264Base_BGTZ_immS<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV3264Base_BGTZ_wrapper";
  let DecoderMethod = "RV3264Base_BGTZ_immS_decode_wrapper";
}

def RV3264Base_BGTZ_immSAsInt64
    : RV3264Base_BGTZ_immS<i64>
    , ImmLeaf<i64, [{ return Imm >= -2048 && Imm <= 2047 && RV3264Base_BGTZ_immS_predicate(Imm); }]>;

def RV3264Base_BGTZ_immSAsLabel : RV3264Base_BGTZ_immS<OtherVT>;




def ADD : Instruction
{
let Namespace = "processorNameValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs X:$rd );
let InOperandList = ( ins X:$rs1, X:$rs2 );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b0110011;
bits<3> funct3 = 0b000;
bits<7> funct7 = 0b0000000;
bits<64> rs2;
bits<64> rs1;
bits<64> rd;

let Inst{31-25} = funct7{6-0};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator       = 0;
let isBranch           = 0;
let isCall             = 0;
let isReturn           = 0;
let isPseudo           = 0;
let isCodeGenOnly      = 0;
let mayLoad            = 0;
let mayStore           = 0;
let isBarrier          = 0;
let isReMaterializable = 0;
let isAsCheapAsAMove   = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}

def ADDI : Instruction
{
let Namespace = "processorNameValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs X:$rd );
let InOperandList = ( ins X:$rs1, RV3264Base_ADDI_immSAsInt64:$imm );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b0010011;
bits<3> funct3 = 0b000;
bits<64> imm;
bits<64> rs1;
bits<64> rd;

let Inst{31-20} = imm{11-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator       = 0;
let isBranch           = 0;
let isCall             = 0;
let isReturn           = 0;
let isPseudo           = 0;
let isCodeGenOnly      = 0;
let mayLoad            = 0;
let mayStore           = 0;
let isBarrier          = 0;
let isReMaterializable = 1;
let isAsCheapAsAMove   = 1;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}

def ADDIW : Instruction
{
let Namespace = "processorNameValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs X:$rd );
let InOperandList = ( ins X:$rs1, RV3264Base_ADDIW_immSAsInt64:$imm );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b0011011;
bits<3> funct3 = 0b000;
bits<64> imm;
bits<64> rs1;
bits<64> rd;

let Inst{31-20} = imm{11-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator       = 0;
let isBranch           = 0;
let isCall             = 0;
let isReturn           = 0;
let isPseudo           = 0;
let isCodeGenOnly      = 0;
let mayLoad            = 0;
let mayStore           = 0;
let isBarrier          = 0;
let isReMaterializable = 0;
let isAsCheapAsAMove   = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}

def ADDW : Instruction
{
let Namespace = "processorNameValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs X:$rd );
let InOperandList = ( ins X:$rs1, X:$rs2 );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b0111011;
bits<3> funct3 = 0b000;
bits<7> funct7 = 0b0000000;
bits<64> rs2;
bits<64> rs1;
bits<64> rd;

let Inst{31-25} = funct7{6-0};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator       = 0;
let isBranch           = 0;
let isCall             = 0;
let isReturn           = 0;
let isPseudo           = 0;
let isCodeGenOnly      = 0;
let mayLoad            = 0;
let mayStore           = 0;
let isBarrier          = 0;
let isReMaterializable = 0;
let isAsCheapAsAMove   = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}

def AND : Instruction
{
let Namespace = "processorNameValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs X:$rd );
let InOperandList = ( ins X:$rs1, X:$rs2 );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b0110011;
bits<3> funct3 = 0b111;
bits<7> funct7 = 0b0000000;
bits<64> rs2;
bits<64> rs1;
bits<64> rd;

let Inst{31-25} = funct7{6-0};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator       = 0;
let isBranch           = 0;
let isCall             = 0;
let isReturn           = 0;
let isPseudo           = 0;
let isCodeGenOnly      = 0;
let mayLoad            = 0;
let mayStore           = 0;
let isBarrier          = 0;
let isReMaterializable = 0;
let isAsCheapAsAMove   = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}

def ANDI : Instruction
{
let Namespace = "processorNameValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs X:$rd );
let InOperandList = ( ins X:$rs1, RV3264Base_ANDI_immSAsInt64:$imm );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b0010011;
bits<3> funct3 = 0b111;
bits<64> imm;
bits<64> rs1;
bits<64> rd;

let Inst{31-20} = imm{11-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator       = 0;
let isBranch           = 0;
let isCall             = 0;
let isReturn           = 0;
let isPseudo           = 0;
let isCodeGenOnly      = 0;
let mayLoad            = 0;
let mayStore           = 0;
let isBarrier          = 0;
let isReMaterializable = 0;
let isAsCheapAsAMove   = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}

def AUIPC : Instruction
{
let Namespace = "processorNameValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs X:$rd );
let InOperandList = ( ins RV3264Base_AUIPC_immUpAsInt64:$imm );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b0010111;
bits<64> imm;
bits<64> rd;

let Inst{31-12} = imm{19-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator       = 0;
let isBranch           = 0;
let isCall             = 0;
let isReturn           = 0;
let isPseudo           = 0;
let isCodeGenOnly      = 0;
let mayLoad            = 0;
let mayStore           = 0;
let isBarrier          = 0;
let isReMaterializable = 0;
let isAsCheapAsAMove   = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}

def BEQ : Instruction
{
let Namespace = "processorNameValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs  );
let InOperandList = ( ins X:$rs1, X:$rs2, RV3264Base_BEQ_immSAsLabel:$imm );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b1100011;
bits<3> funct3 = 0b000;
bits<64> imm;
bits<64> rs2;
bits<64> rs1;

let Inst{11-8} = imm{3-0};
let Inst{30-25} = imm{13-8};
let Inst{7} = imm{20};
let Inst{31} = imm{22};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{6-0} = opcode{6-0};

let isTerminator       = 1;
let isBranch           = 1;
let isCall             = 0;
let isReturn           = 0;
let isPseudo           = 0;
let isCodeGenOnly      = 0;
let mayLoad            = 0;
let mayStore           = 0;
let isBarrier          = 0;
let isReMaterializable = 0;
let isAsCheapAsAMove   = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}

def BGE : Instruction
{
let Namespace = "processorNameValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs  );
let InOperandList = ( ins X:$rs1, X:$rs2, RV3264Base_BGE_immSAsLabel:$imm );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b1100011;
bits<3> funct3 = 0b101;
bits<64> imm;
bits<64> rs2;
bits<64> rs1;

let Inst{11-8} = imm{3-0};
let Inst{30-25} = imm{13-8};
let Inst{7} = imm{20};
let Inst{31} = imm{22};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{6-0} = opcode{6-0};

let isTerminator       = 1;
let isBranch           = 1;
let isCall             = 0;
let isReturn           = 0;
let isPseudo           = 0;
let isCodeGenOnly      = 0;
let mayLoad            = 0;
let mayStore           = 0;
let isBarrier          = 0;
let isReMaterializable = 0;
let isAsCheapAsAMove   = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}

def BGEU : Instruction
{
let Namespace = "processorNameValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs  );
let InOperandList = ( ins X:$rs1, X:$rs2, RV3264Base_BGEU_immSAsLabel:$imm );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b1100011;
bits<3> funct3 = 0b111;
bits<64> imm;
bits<64> rs2;
bits<64> rs1;

let Inst{11-8} = imm{3-0};
let Inst{30-25} = imm{13-8};
let Inst{7} = imm{20};
let Inst{31} = imm{22};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{6-0} = opcode{6-0};

let isTerminator       = 1;
let isBranch           = 1;
let isCall             = 0;
let isReturn           = 0;
let isPseudo           = 0;
let isCodeGenOnly      = 0;
let mayLoad            = 0;
let mayStore           = 0;
let isBarrier          = 0;
let isReMaterializable = 0;
let isAsCheapAsAMove   = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}

def BLT : Instruction
{
let Namespace = "processorNameValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs  );
let InOperandList = ( ins X:$rs1, X:$rs2, RV3264Base_BLT_immSAsLabel:$imm );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b1100011;
bits<3> funct3 = 0b100;
bits<64> imm;
bits<64> rs2;
bits<64> rs1;

let Inst{11-8} = imm{3-0};
let Inst{30-25} = imm{13-8};
let Inst{7} = imm{20};
let Inst{31} = imm{22};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{6-0} = opcode{6-0};

let isTerminator       = 1;
let isBranch           = 1;
let isCall             = 0;
let isReturn           = 0;
let isPseudo           = 0;
let isCodeGenOnly      = 0;
let mayLoad            = 0;
let mayStore           = 0;
let isBarrier          = 0;
let isReMaterializable = 0;
let isAsCheapAsAMove   = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}

def BLTU : Instruction
{
let Namespace = "processorNameValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs  );
let InOperandList = ( ins X:$rs1, X:$rs2, RV3264Base_BLTU_immSAsLabel:$imm );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b1100011;
bits<3> funct3 = 0b110;
bits<64> imm;
bits<64> rs2;
bits<64> rs1;

let Inst{11-8} = imm{3-0};
let Inst{30-25} = imm{13-8};
let Inst{7} = imm{20};
let Inst{31} = imm{22};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{6-0} = opcode{6-0};

let isTerminator       = 1;
let isBranch           = 1;
let isCall             = 0;
let isReturn           = 0;
let isPseudo           = 0;
let isCodeGenOnly      = 0;
let mayLoad            = 0;
let mayStore           = 0;
let isBarrier          = 0;
let isReMaterializable = 0;
let isAsCheapAsAMove   = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}

def BNE : Instruction
{
let Namespace = "processorNameValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs  );
let InOperandList = ( ins X:$rs1, X:$rs2, RV3264Base_BNE_immSAsLabel:$imm );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b1100011;
bits<3> funct3 = 0b001;
bits<64> imm;
bits<64> rs2;
bits<64> rs1;

let Inst{11-8} = imm{3-0};
let Inst{30-25} = imm{13-8};
let Inst{7} = imm{20};
let Inst{31} = imm{22};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{6-0} = opcode{6-0};

let isTerminator       = 1;
let isBranch           = 1;
let isCall             = 0;
let isReturn           = 0;
let isPseudo           = 0;
let isCodeGenOnly      = 0;
let mayLoad            = 0;
let mayStore           = 0;
let isBarrier          = 0;
let isReMaterializable = 0;
let isAsCheapAsAMove   = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}

def DIV : Instruction
{
let Namespace = "processorNameValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs X:$rd );
let InOperandList = ( ins X:$rs1, X:$rs2 );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b0110011;
bits<3> funct3 = 0b100;
bits<7> funct7 = 0b0000001;
bits<64> rs2;
bits<64> rs1;
bits<64> rd;

let Inst{31-25} = funct7{6-0};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator       = 0;
let isBranch           = 0;
let isCall             = 0;
let isReturn           = 0;
let isPseudo           = 0;
let isCodeGenOnly      = 0;
let mayLoad            = 0;
let mayStore           = 0;
let isBarrier          = 0;
let isReMaterializable = 0;
let isAsCheapAsAMove   = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}

def DIVU : Instruction
{
let Namespace = "processorNameValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs X:$rd );
let InOperandList = ( ins X:$rs1, X:$rs2 );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b0110011;
bits<3> funct3 = 0b101;
bits<7> funct7 = 0b0000001;
bits<64> rs2;
bits<64> rs1;
bits<64> rd;

let Inst{31-25} = funct7{6-0};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator       = 0;
let isBranch           = 0;
let isCall             = 0;
let isReturn           = 0;
let isPseudo           = 0;
let isCodeGenOnly      = 0;
let mayLoad            = 0;
let mayStore           = 0;
let isBarrier          = 0;
let isReMaterializable = 0;
let isAsCheapAsAMove   = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}

def DIVUW : Instruction
{
let Namespace = "processorNameValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs X:$rd );
let InOperandList = ( ins X:$rs1, X:$rs2 );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b0111011;
bits<3> funct3 = 0b101;
bits<7> funct7 = 0b0000001;
bits<64> rs2;
bits<64> rs1;
bits<64> rd;

let Inst{31-25} = funct7{6-0};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator       = 0;
let isBranch           = 0;
let isCall             = 0;
let isReturn           = 0;
let isPseudo           = 0;
let isCodeGenOnly      = 0;
let mayLoad            = 0;
let mayStore           = 0;
let isBarrier          = 0;
let isReMaterializable = 0;
let isAsCheapAsAMove   = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}

def DIVW : Instruction
{
let Namespace = "processorNameValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs X:$rd );
let InOperandList = ( ins X:$rs1, X:$rs2 );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b0111011;
bits<3> funct3 = 0b100;
bits<7> funct7 = 0b0000001;
bits<64> rs2;
bits<64> rs1;
bits<64> rd;

let Inst{31-25} = funct7{6-0};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator       = 0;
let isBranch           = 0;
let isCall             = 0;
let isReturn           = 0;
let isPseudo           = 0;
let isCodeGenOnly      = 0;
let mayLoad            = 0;
let mayStore           = 0;
let isBarrier          = 0;
let isReMaterializable = 0;
let isAsCheapAsAMove   = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}

def EBREAK : Instruction
{
let Namespace = "processorNameValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs  );
let InOperandList = ( ins  );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b1110011;
bits<3> funct3 = 0b000;
bits<5> rd = 0b00000;
bits<5> rs1 = 0b00000;
bits<12> imm = 0b000000000001;

let Inst{31-20} = imm{11-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator       = 0;
let isBranch           = 0;
let isCall             = 0;
let isReturn           = 0;
let isPseudo           = 0;
let isCodeGenOnly      = 0;
let mayLoad            = 0;
let mayStore           = 0;
let isBarrier          = 0;
let isReMaterializable = 0;
let isAsCheapAsAMove   = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}

def ECALL : Instruction
{
let Namespace = "processorNameValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs  );
let InOperandList = ( ins  );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b1110011;
bits<3> funct3 = 0b000;
bits<5> rd = 0b00000;
bits<5> rs1 = 0b00000;
bits<12> imm = 0b000000000000;

let Inst{31-20} = imm{11-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator       = 0;
let isBranch           = 0;
let isCall             = 0;
let isReturn           = 0;
let isPseudo           = 0;
let isCodeGenOnly      = 0;
let mayLoad            = 0;
let mayStore           = 0;
let isBarrier          = 0;
let isReMaterializable = 0;
let isAsCheapAsAMove   = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}

def JAL : Instruction
{
let Namespace = "processorNameValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs X:$rd );
let InOperandList = ( ins RV3264Base_JAL_immSAsInt64:$imm );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b1101111;
bits<64> imm;
bits<64> rd;

let Inst{30-21} = imm{9-0};
let Inst{20} = imm{20};
let Inst{19-12} = imm{29-22};
let Inst{31} = imm{38};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator       = 0;
let isBranch           = 0;
let isCall             = 0;
let isReturn           = 0;
let isPseudo           = 0;
let isCodeGenOnly      = 0;
let mayLoad            = 0;
let mayStore           = 0;
let isBarrier          = 0;
let isReMaterializable = 1;
let isAsCheapAsAMove   = 1;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}

def JALR : Instruction
{
let Namespace = "processorNameValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs X:$rd );
let InOperandList = ( ins X:$rs1, RV3264Base_JALR_immSAsInt64:$imm );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b1100111;
bits<3> funct3 = 0b000;
bits<64> imm;
bits<64> rs1;
bits<64> rd;

let Inst{31-20} = imm{11-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator       = 0;
let isBranch           = 0;
let isCall             = 0;
let isReturn           = 0;
let isPseudo           = 0;
let isCodeGenOnly      = 0;
let mayLoad            = 0;
let mayStore           = 0;
let isBarrier          = 0;
let isReMaterializable = 0;
let isAsCheapAsAMove   = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}

def LB : Instruction
{
let Namespace = "processorNameValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs X:$rd );
let InOperandList = ( ins X:$rs1, RV3264Base_LB_immSAsInt64:$imm );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b0000011;
bits<3> funct3 = 0b000;
bits<64> imm;
bits<64> rs1;
bits<64> rd;

let Inst{31-20} = imm{11-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator       = 0;
let isBranch           = 0;
let isCall             = 0;
let isReturn           = 0;
let isPseudo           = 0;
let isCodeGenOnly      = 0;
let mayLoad            = 1;
let mayStore           = 0;
let isBarrier          = 0;
let isReMaterializable = 0;
let isAsCheapAsAMove   = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}

def LBU : Instruction
{
let Namespace = "processorNameValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs X:$rd );
let InOperandList = ( ins X:$rs1, RV3264Base_LBU_immSAsInt64:$imm );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b0000011;
bits<3> funct3 = 0b100;
bits<64> imm;
bits<64> rs1;
bits<64> rd;

let Inst{31-20} = imm{11-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator       = 0;
let isBranch           = 0;
let isCall             = 0;
let isReturn           = 0;
let isPseudo           = 0;
let isCodeGenOnly      = 0;
let mayLoad            = 1;
let mayStore           = 0;
let isBarrier          = 0;
let isReMaterializable = 0;
let isAsCheapAsAMove   = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}

def LD : Instruction
{
let Namespace = "processorNameValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs X:$rd );
let InOperandList = ( ins X:$rs1, RV3264Base_LD_immSAsInt64:$imm );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b0000011;
bits<3> funct3 = 0b011;
bits<64> imm;
bits<64> rs1;
bits<64> rd;

let Inst{31-20} = imm{11-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator       = 0;
let isBranch           = 0;
let isCall             = 0;
let isReturn           = 0;
let isPseudo           = 0;
let isCodeGenOnly      = 0;
let mayLoad            = 1;
let mayStore           = 0;
let isBarrier          = 0;
let isReMaterializable = 0;
let isAsCheapAsAMove   = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}

def LH : Instruction
{
let Namespace = "processorNameValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs X:$rd );
let InOperandList = ( ins X:$rs1, RV3264Base_LH_immSAsInt64:$imm );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b0000011;
bits<3> funct3 = 0b001;
bits<64> imm;
bits<64> rs1;
bits<64> rd;

let Inst{31-20} = imm{11-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator       = 0;
let isBranch           = 0;
let isCall             = 0;
let isReturn           = 0;
let isPseudo           = 0;
let isCodeGenOnly      = 0;
let mayLoad            = 1;
let mayStore           = 0;
let isBarrier          = 0;
let isReMaterializable = 0;
let isAsCheapAsAMove   = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}

def LHU : Instruction
{
let Namespace = "processorNameValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs X:$rd );
let InOperandList = ( ins X:$rs1, RV3264Base_LHU_immSAsInt64:$imm );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b0000011;
bits<3> funct3 = 0b101;
bits<64> imm;
bits<64> rs1;
bits<64> rd;

let Inst{31-20} = imm{11-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator       = 0;
let isBranch           = 0;
let isCall             = 0;
let isReturn           = 0;
let isPseudo           = 0;
let isCodeGenOnly      = 0;
let mayLoad            = 1;
let mayStore           = 0;
let isBarrier          = 0;
let isReMaterializable = 0;
let isAsCheapAsAMove   = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}

def LUI : Instruction
{
let Namespace = "processorNameValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs X:$rd );
let InOperandList = ( ins RV3264Base_LUI_immUpAsInt64:$imm );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b0110111;
bits<64> imm;
bits<64> rd;

let Inst{31-12} = imm{19-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator       = 0;
let isBranch           = 0;
let isCall             = 0;
let isReturn           = 0;
let isPseudo           = 0;
let isCodeGenOnly      = 0;
let mayLoad            = 0;
let mayStore           = 0;
let isBarrier          = 0;
let isReMaterializable = 1;
let isAsCheapAsAMove   = 1;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}

def LW : Instruction
{
let Namespace = "processorNameValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs X:$rd );
let InOperandList = ( ins X:$rs1, RV3264Base_LW_immSAsInt64:$imm );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b0000011;
bits<3> funct3 = 0b010;
bits<64> imm;
bits<64> rs1;
bits<64> rd;

let Inst{31-20} = imm{11-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator       = 0;
let isBranch           = 0;
let isCall             = 0;
let isReturn           = 0;
let isPseudo           = 0;
let isCodeGenOnly      = 0;
let mayLoad            = 1;
let mayStore           = 0;
let isBarrier          = 0;
let isReMaterializable = 0;
let isAsCheapAsAMove   = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}

def LWU : Instruction
{
let Namespace = "processorNameValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs X:$rd );
let InOperandList = ( ins X:$rs1, RV3264Base_LWU_immSAsInt64:$imm );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b0000011;
bits<3> funct3 = 0b110;
bits<64> imm;
bits<64> rs1;
bits<64> rd;

let Inst{31-20} = imm{11-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator       = 0;
let isBranch           = 0;
let isCall             = 0;
let isReturn           = 0;
let isPseudo           = 0;
let isCodeGenOnly      = 0;
let mayLoad            = 1;
let mayStore           = 0;
let isBarrier          = 0;
let isReMaterializable = 0;
let isAsCheapAsAMove   = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}

def MUL : Instruction
{
let Namespace = "processorNameValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs X:$rd );
let InOperandList = ( ins X:$rs1, X:$rs2 );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b0110011;
bits<3> funct3 = 0b000;
bits<7> funct7 = 0b0000001;
bits<64> rs2;
bits<64> rs1;
bits<64> rd;

let Inst{31-25} = funct7{6-0};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator       = 0;
let isBranch           = 0;
let isCall             = 0;
let isReturn           = 0;
let isPseudo           = 0;
let isCodeGenOnly      = 0;
let mayLoad            = 0;
let mayStore           = 0;
let isBarrier          = 0;
let isReMaterializable = 0;
let isAsCheapAsAMove   = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}

def MULH : Instruction
{
let Namespace = "processorNameValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs X:$rd );
let InOperandList = ( ins X:$rs1, X:$rs2 );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b0110011;
bits<3> funct3 = 0b001;
bits<7> funct7 = 0b0000001;
bits<64> rs2;
bits<64> rs1;
bits<64> rd;

let Inst{31-25} = funct7{6-0};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator       = 0;
let isBranch           = 0;
let isCall             = 0;
let isReturn           = 0;
let isPseudo           = 0;
let isCodeGenOnly      = 0;
let mayLoad            = 0;
let mayStore           = 0;
let isBarrier          = 0;
let isReMaterializable = 0;
let isAsCheapAsAMove   = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}

def MULHSU : Instruction
{
let Namespace = "processorNameValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs X:$rd );
let InOperandList = ( ins X:$rs1, X:$rs2 );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b0110011;
bits<3> funct3 = 0b010;
bits<7> funct7 = 0b0000001;
bits<64> rs2;
bits<64> rs1;
bits<64> rd;

let Inst{31-25} = funct7{6-0};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator       = 0;
let isBranch           = 0;
let isCall             = 0;
let isReturn           = 0;
let isPseudo           = 0;
let isCodeGenOnly      = 0;
let mayLoad            = 0;
let mayStore           = 0;
let isBarrier          = 0;
let isReMaterializable = 0;
let isAsCheapAsAMove   = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}

def MULHU : Instruction
{
let Namespace = "processorNameValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs X:$rd );
let InOperandList = ( ins X:$rs1, X:$rs2 );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b0110011;
bits<3> funct3 = 0b011;
bits<7> funct7 = 0b0000001;
bits<64> rs2;
bits<64> rs1;
bits<64> rd;

let Inst{31-25} = funct7{6-0};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator       = 0;
let isBranch           = 0;
let isCall             = 0;
let isReturn           = 0;
let isPseudo           = 0;
let isCodeGenOnly      = 0;
let mayLoad            = 0;
let mayStore           = 0;
let isBarrier          = 0;
let isReMaterializable = 0;
let isAsCheapAsAMove   = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}

def MULW : Instruction
{
let Namespace = "processorNameValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs X:$rd );
let InOperandList = ( ins X:$rs1, X:$rs2 );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b0111011;
bits<3> funct3 = 0b000;
bits<7> funct7 = 0b0000001;
bits<64> rs2;
bits<64> rs1;
bits<64> rd;

let Inst{31-25} = funct7{6-0};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator       = 0;
let isBranch           = 0;
let isCall             = 0;
let isReturn           = 0;
let isPseudo           = 0;
let isCodeGenOnly      = 0;
let mayLoad            = 0;
let mayStore           = 0;
let isBarrier          = 0;
let isReMaterializable = 0;
let isAsCheapAsAMove   = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}

def OR : Instruction
{
let Namespace = "processorNameValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs X:$rd );
let InOperandList = ( ins X:$rs1, X:$rs2 );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b0110011;
bits<3> funct3 = 0b110;
bits<7> funct7 = 0b0000000;
bits<64> rs2;
bits<64> rs1;
bits<64> rd;

let Inst{31-25} = funct7{6-0};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator       = 0;
let isBranch           = 0;
let isCall             = 0;
let isReturn           = 0;
let isPseudo           = 0;
let isCodeGenOnly      = 0;
let mayLoad            = 0;
let mayStore           = 0;
let isBarrier          = 0;
let isReMaterializable = 0;
let isAsCheapAsAMove   = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}

def ORI : Instruction
{
let Namespace = "processorNameValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs X:$rd );
let InOperandList = ( ins X:$rs1, RV3264Base_ORI_immSAsInt64:$imm );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b0010011;
bits<3> funct3 = 0b110;
bits<64> imm;
bits<64> rs1;
bits<64> rd;

let Inst{31-20} = imm{11-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator       = 0;
let isBranch           = 0;
let isCall             = 0;
let isReturn           = 0;
let isPseudo           = 0;
let isCodeGenOnly      = 0;
let mayLoad            = 0;
let mayStore           = 0;
let isBarrier          = 0;
let isReMaterializable = 1;
let isAsCheapAsAMove   = 1;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}

def REM : Instruction
{
let Namespace = "processorNameValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs X:$rd );
let InOperandList = ( ins X:$rs1, X:$rs2 );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b0110011;
bits<3> funct3 = 0b110;
bits<7> funct7 = 0b0000001;
bits<64> rs2;
bits<64> rs1;
bits<64> rd;

let Inst{31-25} = funct7{6-0};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator       = 0;
let isBranch           = 0;
let isCall             = 0;
let isReturn           = 0;
let isPseudo           = 0;
let isCodeGenOnly      = 0;
let mayLoad            = 0;
let mayStore           = 0;
let isBarrier          = 0;
let isReMaterializable = 0;
let isAsCheapAsAMove   = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}

def REMU : Instruction
{
let Namespace = "processorNameValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs X:$rd );
let InOperandList = ( ins X:$rs1, X:$rs2 );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b0110011;
bits<3> funct3 = 0b111;
bits<7> funct7 = 0b0000001;
bits<64> rs2;
bits<64> rs1;
bits<64> rd;

let Inst{31-25} = funct7{6-0};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator       = 0;
let isBranch           = 0;
let isCall             = 0;
let isReturn           = 0;
let isPseudo           = 0;
let isCodeGenOnly      = 0;
let mayLoad            = 0;
let mayStore           = 0;
let isBarrier          = 0;
let isReMaterializable = 0;
let isAsCheapAsAMove   = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}

def REMUW : Instruction
{
let Namespace = "processorNameValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs X:$rd );
let InOperandList = ( ins X:$rs1, X:$rs2 );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b0111011;
bits<3> funct3 = 0b111;
bits<7> funct7 = 0b0000001;
bits<64> rs2;
bits<64> rs1;
bits<64> rd;

let Inst{31-25} = funct7{6-0};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator       = 0;
let isBranch           = 0;
let isCall             = 0;
let isReturn           = 0;
let isPseudo           = 0;
let isCodeGenOnly      = 0;
let mayLoad            = 0;
let mayStore           = 0;
let isBarrier          = 0;
let isReMaterializable = 0;
let isAsCheapAsAMove   = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}

def REMW : Instruction
{
let Namespace = "processorNameValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs X:$rd );
let InOperandList = ( ins X:$rs1, X:$rs2 );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b0111011;
bits<3> funct3 = 0b110;
bits<7> funct7 = 0b0000001;
bits<64> rs2;
bits<64> rs1;
bits<64> rd;

let Inst{31-25} = funct7{6-0};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator       = 0;
let isBranch           = 0;
let isCall             = 0;
let isReturn           = 0;
let isPseudo           = 0;
let isCodeGenOnly      = 0;
let mayLoad            = 0;
let mayStore           = 0;
let isBarrier          = 0;
let isReMaterializable = 0;
let isAsCheapAsAMove   = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}

def SB : Instruction
{
let Namespace = "processorNameValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs  );
let InOperandList = ( ins X:$rs1, X:$rs2, RV3264Base_SB_immSAsInt64:$imm );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b0100011;
bits<3> funct3 = 0b000;
bits<64> imm;
bits<64> rs2;
bits<64> rs1;

let Inst{11-7} = imm{4-0};
let Inst{31-25} = imm{16-10};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{6-0} = opcode{6-0};

let isTerminator       = 0;
let isBranch           = 0;
let isCall             = 0;
let isReturn           = 0;
let isPseudo           = 0;
let isCodeGenOnly      = 0;
let mayLoad            = 0;
let mayStore           = 1;
let isBarrier          = 0;
let isReMaterializable = 0;
let isAsCheapAsAMove   = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}

def SD : Instruction
{
let Namespace = "processorNameValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs  );
let InOperandList = ( ins X:$rs1, X:$rs2, RV3264Base_SD_immSAsInt64:$imm );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b0100011;
bits<3> funct3 = 0b011;
bits<64> imm;
bits<64> rs2;
bits<64> rs1;

let Inst{11-7} = imm{4-0};
let Inst{31-25} = imm{16-10};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{6-0} = opcode{6-0};

let isTerminator       = 0;
let isBranch           = 0;
let isCall             = 0;
let isReturn           = 0;
let isPseudo           = 0;
let isCodeGenOnly      = 0;
let mayLoad            = 0;
let mayStore           = 1;
let isBarrier          = 0;
let isReMaterializable = 0;
let isAsCheapAsAMove   = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}

def SH : Instruction
{
let Namespace = "processorNameValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs  );
let InOperandList = ( ins X:$rs1, X:$rs2, RV3264Base_SH_immSAsInt64:$imm );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b0100011;
bits<3> funct3 = 0b001;
bits<64> imm;
bits<64> rs2;
bits<64> rs1;

let Inst{11-7} = imm{4-0};
let Inst{31-25} = imm{16-10};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{6-0} = opcode{6-0};

let isTerminator       = 0;
let isBranch           = 0;
let isCall             = 0;
let isReturn           = 0;
let isPseudo           = 0;
let isCodeGenOnly      = 0;
let mayLoad            = 0;
let mayStore           = 1;
let isBarrier          = 0;
let isReMaterializable = 0;
let isAsCheapAsAMove   = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}

def SLL : Instruction
{
let Namespace = "processorNameValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs X:$rd );
let InOperandList = ( ins X:$rs1, X:$rs2 );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b0110011;
bits<3> funct3 = 0b001;
bits<7> funct7 = 0b0000000;
bits<64> rs2;
bits<64> rs1;
bits<64> rd;

let Inst{31-25} = funct7{6-0};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator       = 0;
let isBranch           = 0;
let isCall             = 0;
let isReturn           = 0;
let isPseudo           = 0;
let isCodeGenOnly      = 0;
let mayLoad            = 0;
let mayStore           = 0;
let isBarrier          = 0;
let isReMaterializable = 0;
let isAsCheapAsAMove   = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}

def SLLI : Instruction
{
let Namespace = "processorNameValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs X:$rd );
let InOperandList = ( ins X:$rs1, RV3264Base_SLLI_shamtAsInt64:$sft );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b0010011;
bits<4> zero = 0b0000;
bits<3> funct3 = 0b001;
bits<2> funct2 = 0b00;
bits<64> sft;
bits<64> rs1;
bits<64> rd;

let Inst{31-30} = funct2{1-0};
let Inst{29-26} = zero{3-0};
let Inst{25-20} = sft{5-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator       = 0;
let isBranch           = 0;
let isCall             = 0;
let isReturn           = 0;
let isPseudo           = 0;
let isCodeGenOnly      = 0;
let mayLoad            = 0;
let mayStore           = 0;
let isBarrier          = 0;
let isReMaterializable = 0;
let isAsCheapAsAMove   = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}

def SLLIW : Instruction
{
let Namespace = "processorNameValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs X:$rd );
let InOperandList = ( ins X:$rs1, RV3264Base_SLLIW_shamtAsInt64:$rs2 );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b0011011;
bits<3> funct3 = 0b001;
bits<7> funct7 = 0b0000000;
bits<64> rs2;
bits<64> rs1;
bits<64> rd;

let Inst{31-25} = funct7{6-0};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator       = 0;
let isBranch           = 0;
let isCall             = 0;
let isReturn           = 0;
let isPseudo           = 0;
let isCodeGenOnly      = 0;
let mayLoad            = 0;
let mayStore           = 0;
let isBarrier          = 0;
let isReMaterializable = 0;
let isAsCheapAsAMove   = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}

def SLLW : Instruction
{
let Namespace = "processorNameValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs X:$rd );
let InOperandList = ( ins X:$rs1, X:$rs2 );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b0111011;
bits<3> funct3 = 0b001;
bits<7> funct7 = 0b0000000;
bits<64> rs2;
bits<64> rs1;
bits<64> rd;

let Inst{31-25} = funct7{6-0};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator       = 0;
let isBranch           = 0;
let isCall             = 0;
let isReturn           = 0;
let isPseudo           = 0;
let isCodeGenOnly      = 0;
let mayLoad            = 0;
let mayStore           = 0;
let isBarrier          = 0;
let isReMaterializable = 0;
let isAsCheapAsAMove   = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}

def SLT : Instruction
{
let Namespace = "processorNameValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs X:$rd );
let InOperandList = ( ins X:$rs1, X:$rs2 );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b0110011;
bits<3> funct3 = 0b010;
bits<7> funct7 = 0b0000000;
bits<64> rs2;
bits<64> rs1;
bits<64> rd;

let Inst{31-25} = funct7{6-0};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator       = 0;
let isBranch           = 0;
let isCall             = 0;
let isReturn           = 0;
let isPseudo           = 0;
let isCodeGenOnly      = 0;
let mayLoad            = 0;
let mayStore           = 0;
let isBarrier          = 0;
let isReMaterializable = 0;
let isAsCheapAsAMove   = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}

def SLTI : Instruction
{
let Namespace = "processorNameValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs X:$rd );
let InOperandList = ( ins X:$rs1, RV3264Base_SLTI_immSAsInt64:$imm );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b0010011;
bits<3> funct3 = 0b010;
bits<64> imm;
bits<64> rs1;
bits<64> rd;

let Inst{31-20} = imm{11-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator       = 0;
let isBranch           = 0;
let isCall             = 0;
let isReturn           = 0;
let isPseudo           = 0;
let isCodeGenOnly      = 0;
let mayLoad            = 0;
let mayStore           = 0;
let isBarrier          = 0;
let isReMaterializable = 0;
let isAsCheapAsAMove   = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}

def SLTIU : Instruction
{
let Namespace = "processorNameValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs X:$rd );
let InOperandList = ( ins X:$rs1, RV3264Base_SLTIU_immSAsInt64:$imm );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b0010011;
bits<3> funct3 = 0b011;
bits<64> imm;
bits<64> rs1;
bits<64> rd;

let Inst{31-20} = imm{11-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator       = 0;
let isBranch           = 0;
let isCall             = 0;
let isReturn           = 0;
let isPseudo           = 0;
let isCodeGenOnly      = 0;
let mayLoad            = 0;
let mayStore           = 0;
let isBarrier          = 0;
let isReMaterializable = 0;
let isAsCheapAsAMove   = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}

def SLTU : Instruction
{
let Namespace = "processorNameValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs X:$rd );
let InOperandList = ( ins X:$rs1, X:$rs2 );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b0110011;
bits<3> funct3 = 0b011;
bits<7> funct7 = 0b0000000;
bits<64> rs2;
bits<64> rs1;
bits<64> rd;

let Inst{31-25} = funct7{6-0};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator       = 0;
let isBranch           = 0;
let isCall             = 0;
let isReturn           = 0;
let isPseudo           = 0;
let isCodeGenOnly      = 0;
let mayLoad            = 0;
let mayStore           = 0;
let isBarrier          = 0;
let isReMaterializable = 0;
let isAsCheapAsAMove   = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}

def SRA : Instruction
{
let Namespace = "processorNameValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs X:$rd );
let InOperandList = ( ins X:$rs1, X:$rs2 );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b0110011;
bits<3> funct3 = 0b101;
bits<7> funct7 = 0b0100000;
bits<64> rs2;
bits<64> rs1;
bits<64> rd;

let Inst{31-25} = funct7{6-0};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator       = 0;
let isBranch           = 0;
let isCall             = 0;
let isReturn           = 0;
let isPseudo           = 0;
let isCodeGenOnly      = 0;
let mayLoad            = 0;
let mayStore           = 0;
let isBarrier          = 0;
let isReMaterializable = 0;
let isAsCheapAsAMove   = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}

def SRAI : Instruction
{
let Namespace = "processorNameValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs X:$rd );
let InOperandList = ( ins X:$rs1, RV3264Base_SRAI_shamtAsInt64:$sft );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b0010011;
bits<4> zero = 0b0000;
bits<3> funct3 = 0b101;
bits<2> funct2 = 0b01;
bits<64> sft;
bits<64> rs1;
bits<64> rd;

let Inst{31-30} = funct2{1-0};
let Inst{29-26} = zero{3-0};
let Inst{25-20} = sft{5-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator       = 0;
let isBranch           = 0;
let isCall             = 0;
let isReturn           = 0;
let isPseudo           = 0;
let isCodeGenOnly      = 0;
let mayLoad            = 0;
let mayStore           = 0;
let isBarrier          = 0;
let isReMaterializable = 0;
let isAsCheapAsAMove   = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}

def SRAIW : Instruction
{
let Namespace = "processorNameValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs X:$rd );
let InOperandList = ( ins X:$rs1, RV3264Base_SRAIW_shamtAsInt64:$rs2 );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b0011011;
bits<3> funct3 = 0b101;
bits<7> funct7 = 0b0100000;
bits<64> rs2;
bits<64> rs1;
bits<64> rd;

let Inst{31-25} = funct7{6-0};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator       = 0;
let isBranch           = 0;
let isCall             = 0;
let isReturn           = 0;
let isPseudo           = 0;
let isCodeGenOnly      = 0;
let mayLoad            = 0;
let mayStore           = 0;
let isBarrier          = 0;
let isReMaterializable = 0;
let isAsCheapAsAMove   = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}

def SRAW : Instruction
{
let Namespace = "processorNameValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs X:$rd );
let InOperandList = ( ins X:$rs1, X:$rs2 );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b0111011;
bits<3> funct3 = 0b101;
bits<7> funct7 = 0b0100000;
bits<64> rs2;
bits<64> rs1;
bits<64> rd;

let Inst{31-25} = funct7{6-0};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator       = 0;
let isBranch           = 0;
let isCall             = 0;
let isReturn           = 0;
let isPseudo           = 0;
let isCodeGenOnly      = 0;
let mayLoad            = 0;
let mayStore           = 0;
let isBarrier          = 0;
let isReMaterializable = 0;
let isAsCheapAsAMove   = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}

def SRL : Instruction
{
let Namespace = "processorNameValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs X:$rd );
let InOperandList = ( ins X:$rs1, X:$rs2 );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b0110011;
bits<3> funct3 = 0b101;
bits<7> funct7 = 0b0000000;
bits<64> rs2;
bits<64> rs1;
bits<64> rd;

let Inst{31-25} = funct7{6-0};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator       = 0;
let isBranch           = 0;
let isCall             = 0;
let isReturn           = 0;
let isPseudo           = 0;
let isCodeGenOnly      = 0;
let mayLoad            = 0;
let mayStore           = 0;
let isBarrier          = 0;
let isReMaterializable = 0;
let isAsCheapAsAMove   = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}

def SRLI : Instruction
{
let Namespace = "processorNameValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs X:$rd );
let InOperandList = ( ins X:$rs1, RV3264Base_SRLI_shamtAsInt64:$sft );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b0010011;
bits<4> zero = 0b0000;
bits<3> funct3 = 0b101;
bits<2> funct2 = 0b00;
bits<64> sft;
bits<64> rs1;
bits<64> rd;

let Inst{31-30} = funct2{1-0};
let Inst{29-26} = zero{3-0};
let Inst{25-20} = sft{5-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator       = 0;
let isBranch           = 0;
let isCall             = 0;
let isReturn           = 0;
let isPseudo           = 0;
let isCodeGenOnly      = 0;
let mayLoad            = 0;
let mayStore           = 0;
let isBarrier          = 0;
let isReMaterializable = 0;
let isAsCheapAsAMove   = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}

def SRLIW : Instruction
{
let Namespace = "processorNameValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs X:$rd );
let InOperandList = ( ins X:$rs1, RV3264Base_SRLIW_shamtAsInt64:$rs2 );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b0011011;
bits<3> funct3 = 0b101;
bits<7> funct7 = 0b0000000;
bits<64> rs2;
bits<64> rs1;
bits<64> rd;

let Inst{31-25} = funct7{6-0};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator       = 0;
let isBranch           = 0;
let isCall             = 0;
let isReturn           = 0;
let isPseudo           = 0;
let isCodeGenOnly      = 0;
let mayLoad            = 0;
let mayStore           = 0;
let isBarrier          = 0;
let isReMaterializable = 0;
let isAsCheapAsAMove   = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}

def SRLW : Instruction
{
let Namespace = "processorNameValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs X:$rd );
let InOperandList = ( ins X:$rs1, X:$rs2 );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b0111011;
bits<3> funct3 = 0b101;
bits<7> funct7 = 0b0000000;
bits<64> rs2;
bits<64> rs1;
bits<64> rd;

let Inst{31-25} = funct7{6-0};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator       = 0;
let isBranch           = 0;
let isCall             = 0;
let isReturn           = 0;
let isPseudo           = 0;
let isCodeGenOnly      = 0;
let mayLoad            = 0;
let mayStore           = 0;
let isBarrier          = 0;
let isReMaterializable = 0;
let isAsCheapAsAMove   = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}

def SUB : Instruction
{
let Namespace = "processorNameValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs X:$rd );
let InOperandList = ( ins X:$rs1, X:$rs2 );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b0110011;
bits<3> funct3 = 0b000;
bits<7> funct7 = 0b0100000;
bits<64> rs2;
bits<64> rs1;
bits<64> rd;

let Inst{31-25} = funct7{6-0};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator       = 0;
let isBranch           = 0;
let isCall             = 0;
let isReturn           = 0;
let isPseudo           = 0;
let isCodeGenOnly      = 0;
let mayLoad            = 0;
let mayStore           = 0;
let isBarrier          = 0;
let isReMaterializable = 0;
let isAsCheapAsAMove   = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}

def SUBW : Instruction
{
let Namespace = "processorNameValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs X:$rd );
let InOperandList = ( ins X:$rs1, X:$rs2 );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b0111011;
bits<3> funct3 = 0b000;
bits<7> funct7 = 0b0100000;
bits<64> rs2;
bits<64> rs1;
bits<64> rd;

let Inst{31-25} = funct7{6-0};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator       = 0;
let isBranch           = 0;
let isCall             = 0;
let isReturn           = 0;
let isPseudo           = 0;
let isCodeGenOnly      = 0;
let mayLoad            = 0;
let mayStore           = 0;
let isBarrier          = 0;
let isReMaterializable = 0;
let isAsCheapAsAMove   = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}

def SW : Instruction
{
let Namespace = "processorNameValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs  );
let InOperandList = ( ins X:$rs1, X:$rs2, RV3264Base_SW_immSAsInt64:$imm );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b0100011;
bits<3> funct3 = 0b010;
bits<64> imm;
bits<64> rs2;
bits<64> rs1;

let Inst{11-7} = imm{4-0};
let Inst{31-25} = imm{16-10};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{6-0} = opcode{6-0};

let isTerminator       = 0;
let isBranch           = 0;
let isCall             = 0;
let isReturn           = 0;
let isPseudo           = 0;
let isCodeGenOnly      = 0;
let mayLoad            = 0;
let mayStore           = 1;
let isBarrier          = 0;
let isReMaterializable = 0;
let isAsCheapAsAMove   = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}

def XOR : Instruction
{
let Namespace = "processorNameValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs X:$rd );
let InOperandList = ( ins X:$rs1, X:$rs2 );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b0110011;
bits<3> funct3 = 0b100;
bits<7> funct7 = 0b0000000;
bits<64> rs2;
bits<64> rs1;
bits<64> rd;

let Inst{31-25} = funct7{6-0};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator       = 0;
let isBranch           = 0;
let isCall             = 0;
let isReturn           = 0;
let isPseudo           = 0;
let isCodeGenOnly      = 0;
let mayLoad            = 0;
let mayStore           = 0;
let isBarrier          = 0;
let isReMaterializable = 0;
let isAsCheapAsAMove   = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}

def XORI : Instruction
{
let Namespace = "processorNameValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs X:$rd );
let InOperandList = ( ins X:$rs1, RV3264Base_XORI_immSAsInt64:$imm );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b0010011;
bits<3> funct3 = 0b100;
bits<64> imm;
bits<64> rs1;
bits<64> rd;

let Inst{31-20} = imm{11-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator       = 0;
let isBranch           = 0;
let isCall             = 0;
let isReturn           = 0;
let isPseudo           = 0;
let isCodeGenOnly      = 0;
let mayLoad            = 0;
let mayStore           = 0;
let isBarrier          = 0;
let isReMaterializable = 1;
let isAsCheapAsAMove   = 1;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}



def : Pat<(i64 AddrFI:$rs1),
  (ADDI AddrFI:$rs1, (i64 0))>;


def BEQZ : Instruction
{
let Namespace = "processorNameValue";

let OutOperandList = ( outs  );
let InOperandList = ( ins X:$rs, bare_symbol:$offset );

let isTerminator  = 1;
let isBranch      = 1;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 1;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;
let isBarrier     = 0;
let isReMaterializable = 0;
let isAsCheapAsAMove   = 0;

let Constraints = "";
let AddedComplexity = 0;

let Uses = [  ];
let Defs = [  ];
}



def BGEZ : Instruction
{
let Namespace = "processorNameValue";

let OutOperandList = ( outs  );
let InOperandList = ( ins X:$rs, bare_symbol:$offset );

let isTerminator  = 1;
let isBranch      = 1;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 1;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;
let isBarrier     = 0;
let isReMaterializable = 0;
let isAsCheapAsAMove   = 0;

let Constraints = "";
let AddedComplexity = 0;

let Uses = [  ];
let Defs = [  ];
}



def BGTZ : Instruction
{
let Namespace = "processorNameValue";

let OutOperandList = ( outs  );
let InOperandList = ( ins X:$rs, bare_symbol:$offset );

let isTerminator  = 1;
let isBranch      = 1;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 1;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;
let isBarrier     = 0;
let isReMaterializable = 0;
let isAsCheapAsAMove   = 0;

let Constraints = "";
let AddedComplexity = 0;

let Uses = [  ];
let Defs = [  ];
}



def BLEZ : Instruction
{
let Namespace = "processorNameValue";

let OutOperandList = ( outs  );
let InOperandList = ( ins X:$rs, bare_symbol:$offset );

let isTerminator  = 1;
let isBranch      = 1;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 1;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;
let isBarrier     = 0;
let isReMaterializable = 0;
let isAsCheapAsAMove   = 0;

let Constraints = "";
let AddedComplexity = 0;

let Uses = [  ];
let Defs = [  ];
}



def BLTZ : Instruction
{
let Namespace = "processorNameValue";

let OutOperandList = ( outs  );
let InOperandList = ( ins X:$rs, bare_symbol:$offset );

let isTerminator  = 1;
let isBranch      = 1;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 1;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;
let isBarrier     = 0;
let isReMaterializable = 0;
let isAsCheapAsAMove   = 0;

let Constraints = "";
let AddedComplexity = 0;

let Uses = [  ];
let Defs = [  ];
}



def BNEZ : Instruction
{
let Namespace = "processorNameValue";

let OutOperandList = ( outs  );
let InOperandList = ( ins X:$rs, bare_symbol:$offset );

let isTerminator  = 1;
let isBranch      = 1;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 1;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;
let isBarrier     = 0;
let isReMaterializable = 0;
let isAsCheapAsAMove   = 0;

let Constraints = "";
let AddedComplexity = 0;

let Uses = [  ];
let Defs = [  ];
}



def J : Instruction
{
let Namespace = "processorNameValue";

let OutOperandList = ( outs  );
let InOperandList = ( ins RV3264Base_J_immSAsLabel:$imm );

let isTerminator  = 1;
let isBranch      = 1;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 1;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;
let isBarrier     = 1;
let isReMaterializable = 1;
let isAsCheapAsAMove   = 1;

let Constraints = "";
let AddedComplexity = 0;

let Uses = [  ];
let Defs = [  ];
}

def : Pat<(br bb:$imm),
        (J RV3264Base_J_immSAsLabel:$imm)>;


def LA : Instruction
{
let Namespace = "processorNameValue";

let OutOperandList = ( outs X:$rd );
let InOperandList = ( ins bare_symbol:$symbol );

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 1;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;
let isBarrier     = 0;
let isReMaterializable = 0;
let isAsCheapAsAMove   = 0;

let Constraints = "";
let AddedComplexity = 0;

let Uses = [  ];
let Defs = [  ];
}



def LGA_32 : Instruction
{
let Namespace = "processorNameValue";

let OutOperandList = ( outs X:$rd );
let InOperandList = ( ins bare_symbol:$symbol );

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 1;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;
let isBarrier     = 0;
let isReMaterializable = 0;
let isAsCheapAsAMove   = 0;

let Constraints = "";
let AddedComplexity = 0;

let Uses = [  ];
let Defs = [  ];
}



def LGA_64 : Instruction
{
let Namespace = "processorNameValue";

let OutOperandList = ( outs X:$rd );
let InOperandList = ( ins bare_symbol:$symbol );

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 1;
let isCodeGenOnly = 0;
let mayLoad       = 1;
let mayStore      = 0;
let isBarrier     = 0;
let isReMaterializable = 0;
let isAsCheapAsAMove   = 0;

let Constraints = "";
let AddedComplexity = 0;

let Uses = [  ];
let Defs = [  ];
}



def LI : Instruction
{
let Namespace = "processorNameValue";

let OutOperandList = ( outs X:$rd );
let InOperandList = ( ins bare_symbol:$symbol );

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 1;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;
let isBarrier     = 0;
let isReMaterializable = 0;
let isAsCheapAsAMove   = 0;

let Constraints = "";
let AddedComplexity = 0;

let Uses = [  ];
let Defs = [  ];
}



def LLA : Instruction
{
let Namespace = "processorNameValue";

let OutOperandList = ( outs X:$rd );
let InOperandList = ( ins bare_symbol:$symbol );

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 1;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;
let isBarrier     = 0;
let isReMaterializable = 0;
let isAsCheapAsAMove   = 0;

let Constraints = "";
let AddedComplexity = 0;

let Uses = [  ];
let Defs = [  ];
}



def MV : Instruction
{
let Namespace = "processorNameValue";

let OutOperandList = ( outs X:$rd );
let InOperandList = ( ins X:$rs1 );

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 1;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;
let isBarrier     = 0;
let isReMaterializable = 0;
let isAsCheapAsAMove   = 0;

let Constraints = "";
let AddedComplexity = 0;

let Uses = [  ];
let Defs = [  ];
}



def NEG : Instruction
{
let Namespace = "processorNameValue";

let OutOperandList = ( outs X:$rd );
let InOperandList = ( ins X:$rs1 );

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 1;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;
let isBarrier     = 0;
let isReMaterializable = 0;
let isAsCheapAsAMove   = 0;

let Constraints = "";
let AddedComplexity = 0;

let Uses = [  ];
let Defs = [  ];
}



def NOP : Instruction
{
let Namespace = "processorNameValue";

let OutOperandList = ( outs  );
let InOperandList = ( ins  );

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 1;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;
let isBarrier     = 0;
let isReMaterializable = 0;
let isAsCheapAsAMove   = 0;

let Constraints = "";
let AddedComplexity = 0;

let Uses = [ X0 ];
let Defs = [ X0 ];
}



def NOT : Instruction
{
let Namespace = "processorNameValue";

let OutOperandList = ( outs X:$rd );
let InOperandList = ( ins X:$rs1 );

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 1;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;
let isBarrier     = 0;
let isReMaterializable = 0;
let isAsCheapAsAMove   = 0;

let Constraints = "";
let AddedComplexity = 0;

let Uses = [  ];
let Defs = [  ];
}



def SGTZ : Instruction
{
let Namespace = "processorNameValue";

let OutOperandList = ( outs X:$rd );
let InOperandList = ( ins X:$rs1 );

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 1;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;
let isBarrier     = 0;
let isReMaterializable = 0;
let isAsCheapAsAMove   = 0;

let Constraints = "";
let AddedComplexity = 0;

let Uses = [  ];
let Defs = [  ];
}



def SLTZ : Instruction
{
let Namespace = "processorNameValue";

let OutOperandList = ( outs X:$rd );
let InOperandList = ( ins X:$rs1 );

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 1;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;
let isBarrier     = 0;
let isReMaterializable = 0;
let isAsCheapAsAMove   = 0;

let Constraints = "";
let AddedComplexity = 0;

let Uses = [  ];
let Defs = [  ];
}



def SNEZ : Instruction
{
let Namespace = "processorNameValue";

let OutOperandList = ( outs X:$rd );
let InOperandList = ( ins X:$rs1 );

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 1;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;
let isBarrier     = 0;
let isReMaterializable = 0;
let isAsCheapAsAMove   = 0;

let Constraints = "";
let AddedComplexity = 0;

let Uses = [  ];
let Defs = [  ];
}



def TAIL : Instruction
{
let Namespace = "processorNameValue";

let OutOperandList = ( outs  );
let InOperandList = ( ins bare_symbol:$symbol );

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 1;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;
let isBarrier     = 0;
let isReMaterializable = 0;
let isAsCheapAsAMove   = 0;

let Constraints = "";
let AddedComplexity = 0;

let Uses = [ X6 ];
let Defs = [  ];
}






def constMat0 : Instruction
{
let Namespace = "processorNameValue";

let OutOperandList = ( outs X:$rd );
let InOperandList = ( ins bare_symbol:$symbol );

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 1;
let isCodeGenOnly = 1;
let mayLoad       = 0;
let mayStore      = 0;
let isBarrier     = 0;
let isReMaterializable = 0;
let isAsCheapAsAMove   = 0;

let Constraints = "";
let AddedComplexity = 0;

let Uses = [  ];
let Defs = [  ];
}



def constMat1 : Instruction
{
let Namespace = "processorNameValue";

let OutOperandList = ( outs X:$rd );
let InOperandList = ( ins bare_symbol:$symbol );

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 1;
let isCodeGenOnly = 1;
let mayLoad       = 0;
let mayStore      = 0;
let isBarrier     = 0;
let isReMaterializable = 0;
let isAsCheapAsAMove   = 0;

let Constraints = "";
let AddedComplexity = 0;

let Uses = [  ];
let Defs = [  ];
}



def constMat2 : Instruction
{
let Namespace = "processorNameValue";

let OutOperandList = ( outs X:$rd );
let InOperandList = ( ins bare_symbol:$imm );

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 1;
let isCodeGenOnly = 1;
let mayLoad       = 0;
let mayStore      = 0;
let isBarrier     = 0;
let isReMaterializable = 0;
let isAsCheapAsAMove   = 0;

let Constraints = "";
let AddedComplexity = 0;

let Uses = [  ];
let Defs = [  ];
}



def registerAdjustment0 : Instruction
{
let Namespace = "processorNameValue";

let OutOperandList = ( outs X:$rd );
let InOperandList = ( ins X:$rs1, bare_symbol:$imm );

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 1;
let isCodeGenOnly = 1;
let mayLoad       = 0;
let mayStore      = 0;
let isBarrier     = 0;
let isReMaterializable = 0;
let isAsCheapAsAMove   = 0;

let Constraints = "";
let AddedComplexity = 0;

let Uses = [  ];
let Defs = [  ];
}













def : InstAlias<"BEQZ $rs,$offset", (BEQ X:$rs, X0, RV3264Base_BEQ_immSAsLabel:$imm)>;

def : InstAlias<"BGEZ $rs,$offset", (BGE X:$rs, X0, RV3264Base_BGE_immSAsLabel:$imm)>;

def : InstAlias<"BGTZ $rs,$offset", (BLT X0, X:$rs, RV3264Base_BLT_immSAsLabel:$imm)>;

def : InstAlias<"BLEZ $rs,$offset", (BGE X0, X:$rs, RV3264Base_BGE_immSAsLabel:$imm)>;

def : InstAlias<"BLTZ $rs,$offset", (BLT X:$rs, X0, RV3264Base_BLT_immSAsLabel:$imm)>;

def : InstAlias<"BNEZ $rs,$offset", (BNE X:$rs, X0, RV3264Base_BNE_immSAsLabel:$imm)>;

def : InstAlias<"J $offset", (JAL X0, RV3264Base_JAL_immSAsInt64:$imm)>;

def : InstAlias<"MV $rd,$rs1", (ADDI X:$rd, X:$rs1, 0)>;

def : InstAlias<"NEG $rd,$rs1", (SUB X:$rd, X0, X:$rs1)>;

def : InstAlias<"NOP", (ADDI X0, X0, 0)>;

def : InstAlias<"NOT $rd,$rs1", (XORI X:$rd, X:$rs1, 4095)>;

def : InstAlias<"RET", (JALR X0, X1, 0)>;

def : InstAlias<"SGTZ $rd,$rs1", (SLT X:$rd, X0, X:$rs1)>;

def : InstAlias<"SLTZ $rd,$rs1", (SLT X:$rd, X:$rs1, X0)>;

def : InstAlias<"SNEZ $rd,$rs1", (SLTU X:$rd, X0, X:$rs1)>;




let isCall = 1, isBranch = 0, isIndirectBranch = 0, isTerminator = 0,
isBarrier = 0, Defs = [X1]
in
    def PseudoCALLIndirect : Pseudo<(outs ), (ins X:$rs1),
                        [(target_call X:$rs1)]>,
                 PseudoInstExpansion<(JALR X1, X:$rs1, 0)>;

let isCall = 1, isBranch = 1, isIndirectBranch = 1, isTerminator = 1,
isBarrier = 1
in
    def PseudoBRIND : Pseudo<(outs ), (ins X:$rs1, RV3264Base_JALR_immSAsInt64:$imm),
                        []>,
                 PseudoInstExpansion<(JALR X0, X:$rs1, RV3264Base_JALR_immSAsInt64:$imm)>;

def : Pat<(add X:$rs1, X:$rs2),
        (ADD X:$rs1, X:$rs2)>;


def : Pat<(add X:$rs1, RV3264Base_ADDI_immSAsInt64:$imm),
        (ADDI X:$rs1, RV3264Base_ADDI_immSAsInt64:$imm)>;

def : Pat<(add AddrFI:$rs1, RV3264Base_ADDI_immSAsInt64:$imm),
        (ADDI AddrFI:$rs1, RV3264Base_ADDI_immSAsInt64:$imm)>;






def : Pat<(and X:$rs1, X:$rs2),
        (AND X:$rs1, X:$rs2)>;


def : Pat<(and X:$rs1, RV3264Base_ANDI_immSAsInt64:$imm),
        (ANDI X:$rs1, RV3264Base_ANDI_immSAsInt64:$imm)>;




def : Pat<(brcc SETEQ, X:$rs1, X:$rs2, bb:$imm),
        (BEQ X:$rs1, X:$rs2, RV3264Base_BEQ_immSAsLabel:$imm)>;

def : Pat<(brcond (i64 (seteq X:$rs1, X:$rs2)), bb:$imm),
        (BEQ X:$rs1, X:$rs2, RV3264Base_BEQ_immSAsLabel:$imm)>;


def : Pat<(brcc SETGE, X:$rs1, X:$rs2, bb:$imm),
        (BGE X:$rs1, X:$rs2, RV3264Base_BGE_immSAsLabel:$imm)>;

def : Pat<(brcc SETLE, X:$rs2, X:$rs1, bb:$imm),
        (BGE X:$rs1, X:$rs2, RV3264Base_BGE_immSAsLabel:$imm)>;

def : Pat<(brcond (i64 (setge X:$rs1, X:$rs2)), bb:$imm),
        (BGE X:$rs1, X:$rs2, RV3264Base_BGE_immSAsLabel:$imm)>;

def : Pat<(brcond (i64 (setle X:$rs2, X:$rs1)), bb:$imm),
        (BGE X:$rs1, X:$rs2, RV3264Base_BGE_immSAsLabel:$imm)>;


def : Pat<(brcc SETUGE, X:$rs1, X:$rs2, bb:$imm),
        (BGEU X:$rs1, X:$rs2, RV3264Base_BGEU_immSAsLabel:$imm)>;

def : Pat<(brcc SETULE, X:$rs2, X:$rs1, bb:$imm),
        (BGEU X:$rs1, X:$rs2, RV3264Base_BGEU_immSAsLabel:$imm)>;

def : Pat<(brcond (i64 (setuge X:$rs1, X:$rs2)), bb:$imm),
        (BGEU X:$rs1, X:$rs2, RV3264Base_BGEU_immSAsLabel:$imm)>;

def : Pat<(brcond (i64 (setule X:$rs2, X:$rs1)), bb:$imm),
        (BGEU X:$rs1, X:$rs2, RV3264Base_BGEU_immSAsLabel:$imm)>;


def : Pat<(brcc SETLT, X:$rs1, X:$rs2, bb:$imm),
        (BLT X:$rs1, X:$rs2, RV3264Base_BLT_immSAsLabel:$imm)>;

def : Pat<(brcc SETGT, X:$rs2, X:$rs1, bb:$imm),
        (BLT X:$rs1, X:$rs2, RV3264Base_BLT_immSAsLabel:$imm)>;

def : Pat<(brcond (i64 (setlt X:$rs1, X:$rs2)), bb:$imm),
        (BLT X:$rs1, X:$rs2, RV3264Base_BLT_immSAsLabel:$imm)>;

def : Pat<(brcond (i64 (setgt X:$rs2, X:$rs1)), bb:$imm),
        (BLT X:$rs1, X:$rs2, RV3264Base_BLT_immSAsLabel:$imm)>;


def : Pat<(brcc SETULT, X:$rs1, X:$rs2, bb:$imm),
        (BLTU X:$rs1, X:$rs2, RV3264Base_BLTU_immSAsLabel:$imm)>;

def : Pat<(brcc SETUGT, X:$rs2, X:$rs1, bb:$imm),
        (BLTU X:$rs1, X:$rs2, RV3264Base_BLTU_immSAsLabel:$imm)>;

def : Pat<(brcond (i64 (setult X:$rs1, X:$rs2)), bb:$imm),
        (BLTU X:$rs1, X:$rs2, RV3264Base_BLTU_immSAsLabel:$imm)>;

def : Pat<(brcond (i64 (setugt X:$rs2, X:$rs1)), bb:$imm),
        (BLTU X:$rs1, X:$rs2, RV3264Base_BLTU_immSAsLabel:$imm)>;


def : Pat<(brcc SETNE, X:$rs1, X:$rs2, bb:$imm),
        (BNE X:$rs1, X:$rs2, RV3264Base_BNE_immSAsLabel:$imm)>;

def : Pat<(brcond (i64 (setne X:$rs1, X:$rs2)), bb:$imm),
        (BNE X:$rs1, X:$rs2, RV3264Base_BNE_immSAsLabel:$imm)>;

def : Pat<(brcond X:$rs1, bb:$imm),
        (BNE X:$rs1, X0, bb:$imm)>;


def : Pat<(sdiv X:$rs1, X:$rs2),
        (DIV X:$rs1, X:$rs2)>;


def : Pat<(udiv X:$rs1, X:$rs2),
        (DIVU X:$rs1, X:$rs2)>;












def : Pat<(brind X:$rs1),
        (PseudoBRIND X:$rs1, 0)>;

def : Pat<(brind (add X:$rs1, RV3264Base_JALR_immSAsInt64:$imm)),
        (PseudoBRIND X:$rs1, RV3264Base_JALR_immSAsInt64:$imm)>;


def : Pat<(i64 (sextloadi8 (add X:$rs1, RV3264Base_LB_immSAsInt64:$imm))),
        (LB X:$rs1, RV3264Base_LB_immSAsInt64:$imm)>;

def : Pat<(i64 (sextloadi8 (add AddrFI:$rs1, RV3264Base_LB_immSAsInt64:$imm))),
        (LB AddrFI:$rs1, RV3264Base_LB_immSAsInt64:$imm)>;

def : Pat<(i64 (sextloadi8 X:$rs1)),
        (LB X:$rs1, (i64 0))>;

def : Pat<(i64 (extloadi8 X:$rs1)),
        (LB X:$rs1, (i64 0))>;

def : Pat<(i64 (sextloadi8 AddrFI:$rs1)),
        (LB AddrFI:$rs1, (i64 0))>;

def : Pat<(i64 (extloadi8 AddrFI:$rs1)),
        (LB AddrFI:$rs1, (i64 0))>;


def : Pat<(i64 (zextloadi8 (add X:$rs1, RV3264Base_LBU_immSAsInt64:$imm))),
        (LBU X:$rs1, RV3264Base_LBU_immSAsInt64:$imm)>;

def : Pat<(i64 (zextloadi8 (add AddrFI:$rs1, RV3264Base_LBU_immSAsInt64:$imm))),
        (LBU AddrFI:$rs1, RV3264Base_LBU_immSAsInt64:$imm)>;

def : Pat<(i64 (zextloadi8 X:$rs1)),
        (LBU X:$rs1, (i64 0))>;

def : Pat<(i64 (zextloadi8 AddrFI:$rs1)),
        (LBU AddrFI:$rs1, (i64 0))>;


def : Pat<(i64 (load (add X:$rs1, RV3264Base_LD_immSAsInt64:$imm))),
        (LD X:$rs1, RV3264Base_LD_immSAsInt64:$imm)>;

def : Pat<(i64 (load (add AddrFI:$rs1, RV3264Base_LD_immSAsInt64:$imm))),
        (LD AddrFI:$rs1, RV3264Base_LD_immSAsInt64:$imm)>;

def : Pat<(i64 (load X:$rs1)),
        (LD X:$rs1, (i64 0))>;

def : Pat<(i64 (load AddrFI:$rs1)),
        (LD AddrFI:$rs1, (i64 0))>;


def : Pat<(i64 (sextloadi16 (add X:$rs1, RV3264Base_LH_immSAsInt64:$imm))),
        (LH X:$rs1, RV3264Base_LH_immSAsInt64:$imm)>;

def : Pat<(i64 (sextloadi16 (add AddrFI:$rs1, RV3264Base_LH_immSAsInt64:$imm))),
        (LH AddrFI:$rs1, RV3264Base_LH_immSAsInt64:$imm)>;

def : Pat<(i64 (sextloadi16 X:$rs1)),
        (LH X:$rs1, (i64 0))>;

def : Pat<(i64 (extloadi16 X:$rs1)),
        (LH X:$rs1, (i64 0))>;

def : Pat<(i64 (sextloadi16 AddrFI:$rs1)),
        (LH AddrFI:$rs1, (i64 0))>;

def : Pat<(i64 (extloadi16 AddrFI:$rs1)),
        (LH AddrFI:$rs1, (i64 0))>;


def : Pat<(i64 (zextloadi16 (add X:$rs1, RV3264Base_LHU_immSAsInt64:$imm))),
        (LHU X:$rs1, RV3264Base_LHU_immSAsInt64:$imm)>;

def : Pat<(i64 (zextloadi16 (add AddrFI:$rs1, RV3264Base_LHU_immSAsInt64:$imm))),
        (LHU AddrFI:$rs1, RV3264Base_LHU_immSAsInt64:$imm)>;

def : Pat<(i64 (zextloadi16 X:$rs1)),
        (LHU X:$rs1, (i64 0))>;

def : Pat<(i64 (zextloadi16 AddrFI:$rs1)),
        (LHU AddrFI:$rs1, (i64 0))>;




def : Pat<(i64 (sextloadi32 (add X:$rs1, RV3264Base_LW_immSAsInt64:$imm))),
        (LW X:$rs1, RV3264Base_LW_immSAsInt64:$imm)>;

def : Pat<(i64 (sextloadi32 (add AddrFI:$rs1, RV3264Base_LW_immSAsInt64:$imm))),
        (LW AddrFI:$rs1, RV3264Base_LW_immSAsInt64:$imm)>;

def : Pat<(i64 (sextloadi32 X:$rs1)),
        (LW X:$rs1, (i64 0))>;

def : Pat<(i64 (extloadi32 X:$rs1)),
        (LW X:$rs1, (i64 0))>;

def : Pat<(i64 (sextloadi32 AddrFI:$rs1)),
        (LW AddrFI:$rs1, (i64 0))>;

def : Pat<(i64 (extloadi32 AddrFI:$rs1)),
        (LW AddrFI:$rs1, (i64 0))>;


def : Pat<(i64 (zextloadi32 (add X:$rs1, RV3264Base_LWU_immSAsInt64:$imm))),
        (LWU X:$rs1, RV3264Base_LWU_immSAsInt64:$imm)>;

def : Pat<(i64 (zextloadi32 (add AddrFI:$rs1, RV3264Base_LWU_immSAsInt64:$imm))),
        (LWU AddrFI:$rs1, RV3264Base_LWU_immSAsInt64:$imm)>;

def : Pat<(i64 (zextloadi32 X:$rs1)),
        (LWU X:$rs1, (i64 0))>;

def : Pat<(i64 (zextloadi32 AddrFI:$rs1)),
        (LWU AddrFI:$rs1, (i64 0))>;


def : Pat<(mul X:$rs1, X:$rs2),
        (MUL X:$rs1, X:$rs2)>;


def : Pat<(mulhs X:$rs1, X:$rs2),
        (MULH X:$rs1, X:$rs2)>;




def : Pat<(mulhu X:$rs1, X:$rs2),
        (MULHU X:$rs1, X:$rs2)>;




def : Pat<(or X:$rs1, X:$rs2),
        (OR X:$rs1, X:$rs2)>;


def : Pat<(or X:$rs1, RV3264Base_ORI_immSAsInt64:$imm),
        (ORI X:$rs1, RV3264Base_ORI_immSAsInt64:$imm)>;


def : Pat<(srem X:$rs1, X:$rs2),
        (REM X:$rs1, X:$rs2)>;


def : Pat<(urem X:$rs1, X:$rs2),
        (REMU X:$rs1, X:$rs2)>;






def : Pat<(truncstorei8 X:$rs2, (add X:$rs1, RV3264Base_SB_immSAsInt64:$imm)),
        (SB X:$rs1, X:$rs2, RV3264Base_SB_immSAsInt64:$imm)>;

def : Pat<(truncstorei8 X:$rs2, (add AddrFI:$rs1, RV3264Base_SB_immSAsInt64:$imm)),
        (SB AddrFI:$rs1, X:$rs2, RV3264Base_SB_immSAsInt64:$imm)>;

def : Pat<(truncstorei8 X:$rs2, X:$rs1),
        (SB X:$rs1, X:$rs2, (i64 0))>;

def : Pat<(truncstorei8 X:$rs2, AddrFI:$rs1),
        (SB AddrFI:$rs1, X:$rs2, (i64 0))>;


def : Pat<(store X:$rs2, (add X:$rs1, RV3264Base_SD_immSAsInt64:$imm)),
        (SD X:$rs1, X:$rs2, RV3264Base_SD_immSAsInt64:$imm)>;

def : Pat<(store X:$rs2, (add AddrFI:$rs1, RV3264Base_SD_immSAsInt64:$imm)),
        (SD AddrFI:$rs1, X:$rs2, RV3264Base_SD_immSAsInt64:$imm)>;

def : Pat<(store X:$rs2, X:$rs1),
        (SD X:$rs1, X:$rs2, (i64 0))>;

def : Pat<(store X:$rs2, AddrFI:$rs1),
        (SD AddrFI:$rs1, X:$rs2, (i64 0))>;


def : Pat<(truncstorei16 X:$rs2, (add X:$rs1, RV3264Base_SH_immSAsInt64:$imm)),
        (SH X:$rs1, X:$rs2, RV3264Base_SH_immSAsInt64:$imm)>;

def : Pat<(truncstorei16 X:$rs2, (add AddrFI:$rs1, RV3264Base_SH_immSAsInt64:$imm)),
        (SH AddrFI:$rs1, X:$rs2, RV3264Base_SH_immSAsInt64:$imm)>;

def : Pat<(truncstorei16 X:$rs2, X:$rs1),
        (SH X:$rs1, X:$rs2, (i64 0))>;

def : Pat<(truncstorei16 X:$rs2, AddrFI:$rs1),
        (SH AddrFI:$rs1, X:$rs2, (i64 0))>;


def : Pat<(shl X:$rs1, X:$rs2),
        (SLL X:$rs1, X:$rs2)>;


def : Pat<(shl X:$rs1, RV3264Base_SLLI_shamtAsInt64:$sft),
        (SLLI X:$rs1, RV3264Base_SLLI_shamtAsInt64:$sft)>;






def : Pat<(setcc X:$rs1, X:$rs2, SETLT),
        (SLT X:$rs1, X:$rs2)>;

def : Pat<(setcc X:$rs1, X:$rs2, SETEQ),
        (SLTIU (XOR X:$rs1, X:$rs2), 1)>;

def : Pat<(setcc X:$rs1, X:$rs2, SETGT),
        (SLT X:$rs2, X:$rs1)>;

def : Pat<(setcc X:$rs1, X:$rs2, SETLE),
        (XORI (SLT X:$rs2, X:$rs1), 1)>;

def : Pat<(setcc X:$rs1, X:$rs2, SETGE),
        (XORI (SLT X:$rs1, X:$rs2), 1)>;


def : Pat<(setcc X:$rs1, RV3264Base_SLTI_immSAsInt64:$imm, SETLT),
        (SLTI X:$rs1, RV3264Base_SLTI_immSAsInt64:$imm)>;


def : Pat<(setcc X:$rs1, RV3264Base_SLTIU_immSAsInt64:$imm, SETULT),
        (SLTIU X:$rs1, RV3264Base_SLTIU_immSAsInt64:$imm)>;

def : Pat<(setcc X:$rs1, RV3264Base_SLTIU_immSAsInt64:$imm, SETNE),
        (SLTU X0, (XORI X:$rs1, RV3264Base_SLTIU_immSAsInt64:$imm))>;


def : Pat<(setcc X:$rs1, X:$rs2, SETULT),
        (SLTU X:$rs1, X:$rs2)>;

def : Pat<(setcc X:$rs1, X:$rs2, SETNE),
        (SLTU X0, (XOR X:$rs1, X:$rs2))>;

def : Pat<(setcc X:$rs1, X:$rs2, SETUGE),
        (XORI (SLTU X:$rs1, X:$rs2), 1)>;

def : Pat<(setcc X:$rs1, X:$rs2, SETUGT),
        (SLTU X:$rs2, X:$rs1)>;

def : Pat<(setcc X:$rs1, X:$rs2, SETULE),
        (XORI (SLTU X:$rs2, X:$rs1), 1)>;


def : Pat<(sra X:$rs1, X:$rs2),
        (SRA X:$rs1, X:$rs2)>;


def : Pat<(sra X:$rs1, RV3264Base_SRAI_shamtAsInt64:$sft),
        (SRAI X:$rs1, RV3264Base_SRAI_shamtAsInt64:$sft)>;






def : Pat<(srl X:$rs1, X:$rs2),
        (SRL X:$rs1, X:$rs2)>;


def : Pat<(srl X:$rs1, RV3264Base_SRLI_shamtAsInt64:$sft),
        (SRLI X:$rs1, RV3264Base_SRLI_shamtAsInt64:$sft)>;






def : Pat<(sub X:$rs1, X:$rs2),
        (SUB X:$rs1, X:$rs2)>;




def : Pat<(truncstorei32 X:$rs2, (add X:$rs1, RV3264Base_SW_immSAsInt64:$imm)),
        (SW X:$rs1, X:$rs2, RV3264Base_SW_immSAsInt64:$imm)>;

def : Pat<(truncstorei32 X:$rs2, (add AddrFI:$rs1, RV3264Base_SW_immSAsInt64:$imm)),
        (SW AddrFI:$rs1, X:$rs2, RV3264Base_SW_immSAsInt64:$imm)>;

def : Pat<(truncstorei32 X:$rs2, X:$rs1),
        (SW X:$rs1, X:$rs2, (i64 0))>;

def : Pat<(truncstorei32 X:$rs2, AddrFI:$rs1),
        (SW AddrFI:$rs1, X:$rs2, (i64 0))>;


def : Pat<(xor X:$rs1, X:$rs2),
        (XOR X:$rs1, X:$rs2)>;


def : Pat<(xor X:$rs1, RV3264Base_XORI_immSAsInt64:$imm),
        (XORI X:$rs1, RV3264Base_XORI_immSAsInt64:$imm)>;
















def : Pat<(br bb:$imm),
        (J RV3264Base_J_immSAsLabel:$imm)>;

































def : Pat<(iPTR (vadl_lga tglobaladdr:$in)), (LGA_64 tglobaladdr:$in)>;