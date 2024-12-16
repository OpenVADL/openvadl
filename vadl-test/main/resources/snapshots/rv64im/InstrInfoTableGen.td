/*
 * Node representing the frame index.
 * The method CPUDAGToDAGISel::SelectAddrFI is used to determine
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
    let Namespace = "rv64im";

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

def target_call : SDNode<"rv64imISD::CALL", SDT_CPU_Call,
                           [SDNPHasChain, SDNPOptInGlue, SDNPOutGlue, SDNPVariadic]>;

def target_ret_flag : SDNode<"rv64imISD::RET_FLAG", SDTNone,
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
    let Namespace = "rv64im";
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
    let Namespace = "rv64im";
    let isPseudo = 1;
    let isCodeGenOnly = 1;
    let Defs = [ X2 ]; // stack pointer
    let Uses = [ X2 ]; // stack pointer
}

def RESERVED_PSEUDO_RET : Instruction
{
    let Namespace = "rv64im";
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
def RESERVED_PSEUDO_CALL : Instruction
{
    let Namespace = "rv64im";
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
    let Defs = [];
    let Uses = [];
}

/* Match the call sequence for global and external symbols */
def : Pat<(target_call tglobaladdr:$func), (RESERVED_PSEUDO_CALL tglobaladdr:$func)>;
def : Pat<(target_call texternalsym:$func), (RESERVED_PSEUDO_CALL texternalsym:$func)>;

def SDT_rv64imSelectCC     : SDTypeProfile<1, 5, [SDTCisSameAs<1, 2>,
                                         SDTCisSameAs<0, 4>,
                                         SDTCisSameAs<4, 5>]>;
def target_selectcc  : SDNode<"rv64imISD::SELECT_CC", SDT_rv64imSelectCC,
                     [SDNPInGlue]>;

def vadl_imm32 : ImmLeaf<i32, [{ return true; }]>, Operand<i32>
{

}

def vadl_imm64 : ImmLeaf<i64, [{ return true; }]>, Operand<i64>
{

}




def SelectCC_X: Instruction {
    field bits<64> Inst;
    // SoftFail is a field the disassembler can use to provide a way for
    // instructions to not match without killing the whole decode process. It is
    // mainly used for ARM, but Tablegen expects this field to exist or it fails
    // to build the decode table.
    field bits<32> SoftFail = 0;

    bits<7> Opcode = 0;

    let Inst{6-0} = Opcode;

    let Namespace = "rv64im";

    dag OutOperandList = (outs X:$dst);
    dag InOperandList = (ins X:$lhs, X:$rhs, vadl_imm64:$imm, X:$truev, X:$falsev);
    let Pattern = [ (set X:$dst, (target_selectcc X:$lhs, X:$rhs,
                (i64 vadl_imm64:$imm), X:$truev, X:$falsev)) ];

    let TSFlags{4-0} = 0;
    let usesCustomInserter = 1;
    let isPseudo = 1;
    let isCodeGenOnly = 1;
}




class RV64IM_Ftype_sft<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV64IM_Ftype_shamt_encoding_wrapper";
  let DecoderMethod = "RV64IM_Ftype_shamt_decode_wrapper";
}

def RV64IM_Ftype_sftAsInt64
    : RV64IM_Ftype_sft<i64>
    , ImmLeaf<i64, [{ return isInt<6>(Imm) && Imm >= -32 && Imm <= 31 && RV64IM_Ftype_shamt_predicate(Imm); }]>;

def RV64IM_Ftype_sftAsLabel : RV64IM_Ftype_sft<OtherVT>;

class RV64IM_Btype_imm<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV64IM_Btype_immS_encoding_wrapper";
  let DecoderMethod = "RV64IM_Btype_immS_decode_wrapper";
}

def RV64IM_Btype_immAsInt64
    : RV64IM_Btype_imm<i64>
    , ImmLeaf<i64, [{ return isInt<12>(Imm) && Imm >= -2048 && Imm <= 2047 && RV64IM_Btype_immS_predicate(Imm); }]>;

def RV64IM_Btype_immAsLabel : RV64IM_Btype_imm<OtherVT>;

class RV64IM_Stype_imm<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV64IM_Stype_immS_encoding_wrapper";
  let DecoderMethod = "RV64IM_Stype_immS_decode_wrapper";
}

def RV64IM_Stype_immAsInt64
    : RV64IM_Stype_imm<i64>
    , ImmLeaf<i64, [{ return isInt<12>(Imm) && Imm >= -2048 && Imm <= 2047 && RV64IM_Stype_immS_predicate(Imm); }]>;

def RV64IM_Stype_immAsLabel : RV64IM_Stype_imm<OtherVT>;

class RV64IM_Rtype_rs2<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV64IM_Rtype_shamt_encoding_wrapper";
  let DecoderMethod = "RV64IM_Rtype_shamt_decode_wrapper";
}

def RV64IM_Rtype_rs2AsInt64
    : RV64IM_Rtype_rs2<i64>
    , ImmLeaf<i64, [{ return isInt<5>(Imm) && Imm >= -16 && Imm <= 15 && RV64IM_Rtype_shamt_predicate(Imm); }]>;

def RV64IM_Rtype_rs2AsLabel : RV64IM_Rtype_rs2<OtherVT>;

class RV64IM_Itype_imm<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV64IM_Itype_immS_encoding_wrapper";
  let DecoderMethod = "RV64IM_Itype_immS_decode_wrapper";
}

def RV64IM_Itype_immAsInt64
    : RV64IM_Itype_imm<i64>
    , ImmLeaf<i64, [{ return isInt<12>(Imm) && Imm >= -2048 && Imm <= 2047 && RV64IM_Itype_immS_predicate(Imm); }]>;

def RV64IM_Itype_immAsLabel : RV64IM_Itype_imm<OtherVT>;

class RV64IM_Utype_imm<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV64IM_Utype_immU_encoding_wrapper";
  let DecoderMethod = "RV64IM_Utype_immU_decode_wrapper";
}

def RV64IM_Utype_immAsInt64
    : RV64IM_Utype_imm<i64>
    , ImmLeaf<i64, [{ return isInt<20>(Imm) && Imm >= -524288 && Imm <= 524287 && RV64IM_Utype_immU_predicate(Imm); }]>;

def RV64IM_Utype_immAsLabel : RV64IM_Utype_imm<OtherVT>;

class RV64IM_Jtype_imm<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV64IM_Jtype_immS_encoding_wrapper";
  let DecoderMethod = "RV64IM_Jtype_immS_decode_wrapper";
}

def RV64IM_Jtype_immAsInt64
    : RV64IM_Jtype_imm<i64>
    , ImmLeaf<i64, [{ return isInt<20>(Imm) && Imm >= -524288 && Imm <= 524287 && RV64IM_Jtype_immS_predicate(Imm); }]>;

def RV64IM_Jtype_immAsLabel : RV64IM_Jtype_imm<OtherVT>;




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

bits<7> opcode = 0b1100110;
bits<3> funct3 = 0b000;
bits<7> funct7 = 0b0000000;
bits<5> rs2;
bits<5> rs1;
bits<5> rd;

let Inst{31-25} = funct7{6-0};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;

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
let InOperandList = ( ins X:$rs1, RV64IM_Itype_immAsInt64:$imm );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b1100100;
bits<3> funct3 = 0b000;
bits<12> imm;
bits<5> rs1;
bits<5> rd;

let Inst{31-20} = imm{11-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;

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
let InOperandList = ( ins X:$rs1, RV64IM_Itype_immAsInt64:$imm );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b1101100;
bits<3> funct3 = 0b000;
bits<12> imm;
bits<5> rs1;
bits<5> rd;

let Inst{31-20} = imm{11-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;

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

bits<7> opcode = 0b1101110;
bits<3> funct3 = 0b000;
bits<7> funct7 = 0b0000000;
bits<5> rs2;
bits<5> rs1;
bits<5> rd;

let Inst{31-25} = funct7{6-0};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;

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

bits<7> opcode = 0b1100110;
bits<3> funct3 = 0b111;
bits<7> funct7 = 0b0000000;
bits<5> rs2;
bits<5> rs1;
bits<5> rd;

let Inst{31-25} = funct7{6-0};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;

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
let InOperandList = ( ins X:$rs1, RV64IM_Itype_immAsInt64:$imm );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b1100100;
bits<3> funct3 = 0b111;
bits<12> imm;
bits<5> rs1;
bits<5> rd;

let Inst{31-20} = imm{11-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;

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
let InOperandList = ( ins RV64IM_Utype_immAsInt64:$imm );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b1110100;
bits<20> imm;
bits<5> rd;

let Inst{31-12} = imm{19-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [ PC ];
let Defs = [  ];
}

def BEQ : Instruction
{
let Namespace = "processorNameValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs  );
let InOperandList = ( ins X:$rs1, X:$rs2, RV64IM_Btype_immAsLabel:$imm );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b1100011;
bits<3> funct3 = 0b000;
bits<12> imm;
bits<5> rs2;
bits<5> rs1;

let Inst{11-8} = imm{3-0};
let Inst{30-25} = imm{9-4};
let Inst{7} = imm{10};
let Inst{31} = imm{11};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 1;
let isBranch      = 1;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [ PC ];
}

def BGE : Instruction
{
let Namespace = "processorNameValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs  );
let InOperandList = ( ins X:$rs1, X:$rs2, RV64IM_Btype_immAsLabel:$imm );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b1100011;
bits<3> funct3 = 0b101;
bits<12> imm;
bits<5> rs2;
bits<5> rs1;

let Inst{11-8} = imm{3-0};
let Inst{30-25} = imm{9-4};
let Inst{7} = imm{10};
let Inst{31} = imm{11};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 1;
let isBranch      = 1;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [ PC ];
}

def BGEU : Instruction
{
let Namespace = "processorNameValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs  );
let InOperandList = ( ins X:$rs1, X:$rs2, RV64IM_Btype_immAsLabel:$imm );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b1100011;
bits<3> funct3 = 0b111;
bits<12> imm;
bits<5> rs2;
bits<5> rs1;

let Inst{11-8} = imm{3-0};
let Inst{30-25} = imm{9-4};
let Inst{7} = imm{10};
let Inst{31} = imm{11};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 1;
let isBranch      = 1;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [ PC ];
}

def BLT : Instruction
{
let Namespace = "processorNameValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs  );
let InOperandList = ( ins X:$rs1, X:$rs2, RV64IM_Btype_immAsLabel:$imm );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b1100011;
bits<3> funct3 = 0b001;
bits<12> imm;
bits<5> rs2;
bits<5> rs1;

let Inst{11-8} = imm{3-0};
let Inst{30-25} = imm{9-4};
let Inst{7} = imm{10};
let Inst{31} = imm{11};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 1;
let isBranch      = 1;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [ PC ];
}

def BLTU : Instruction
{
let Namespace = "processorNameValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs  );
let InOperandList = ( ins X:$rs1, X:$rs2, RV64IM_Btype_immAsLabel:$imm );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b1100011;
bits<3> funct3 = 0b011;
bits<12> imm;
bits<5> rs2;
bits<5> rs1;

let Inst{11-8} = imm{3-0};
let Inst{30-25} = imm{9-4};
let Inst{7} = imm{10};
let Inst{31} = imm{11};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 1;
let isBranch      = 1;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [ PC ];
}

def BNE : Instruction
{
let Namespace = "processorNameValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs  );
let InOperandList = ( ins X:$rs1, X:$rs2, RV64IM_Btype_immAsLabel:$imm );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b1100011;
bits<3> funct3 = 0b100;
bits<12> imm;
bits<5> rs2;
bits<5> rs1;

let Inst{11-8} = imm{3-0};
let Inst{30-25} = imm{9-4};
let Inst{7} = imm{10};
let Inst{31} = imm{11};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 1;
let isBranch      = 1;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [ PC ];
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

bits<7> opcode = 0b1100110;
bits<3> funct3 = 0b001;
bits<7> funct7 = 0b1000000;
bits<5> rs2;
bits<5> rs1;
bits<5> rd;

let Inst{31-25} = funct7{6-0};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;

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

bits<7> opcode = 0b1100110;
bits<3> funct3 = 0b101;
bits<7> funct7 = 0b1000000;
bits<5> rs2;
bits<5> rs1;
bits<5> rd;

let Inst{31-25} = funct7{6-0};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;

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

bits<7> opcode = 0b1101110;
bits<3> funct3 = 0b101;
bits<7> funct7 = 0b1000000;
bits<5> rs2;
bits<5> rs1;
bits<5> rd;

let Inst{31-25} = funct7{6-0};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;

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

bits<7> opcode = 0b1101110;
bits<3> funct3 = 0b001;
bits<7> funct7 = 0b1000000;
bits<5> rs2;
bits<5> rs1;
bits<5> rd;

let Inst{31-25} = funct7{6-0};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;

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

bits<7> opcode = 0b1100111;
bits<5> rd = 0b00000;
bits<3> funct3 = 0b000;
bits<5> rs1 = 0b00000;
bits<12> imm = 0b100000000000;

let Inst{31-20} = imm{11-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;

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

bits<7> opcode = 0b1100111;
bits<5> rd = 0b00000;
bits<3> funct3 = 0b000;
bits<5> rs1 = 0b00000;
bits<12> imm = 0b000000000000;

let Inst{31-20} = imm{11-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;

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
let InOperandList = ( ins RV64IM_Jtype_immAsInt64:$imm );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b1111011;
bits<20> imm;
bits<5> rd;

let Inst{30-21} = imm{9-0};
let Inst{20} = imm{10};
let Inst{19-12} = imm{18-11};
let Inst{31} = imm{19};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 1;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [ PC ];
let Defs = [ PC ];
}

def JALR : Instruction
{
let Namespace = "processorNameValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs X:$rd );
let InOperandList = ( ins X:$rs1, RV64IM_Itype_immAsInt64:$imm );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b1110011;
bits<3> funct3 = 0b000;
bits<12> imm;
bits<5> rs1;
bits<5> rd;

let Inst{31-20} = imm{11-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 1;
let isBranch      = 1;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [ PC ];
let Defs = [ PC ];
}

def LB : Instruction
{
let Namespace = "processorNameValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs X:$rd );
let InOperandList = ( ins X:$rs1, RV64IM_Itype_immAsInt64:$imm );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b1100000;
bits<3> funct3 = 0b000;
bits<12> imm;
bits<5> rs1;
bits<5> rd;

let Inst{31-20} = imm{11-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 1;
let mayStore      = 0;

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
let InOperandList = ( ins X:$rs1, RV64IM_Itype_immAsInt64:$imm );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b1100000;
bits<3> funct3 = 0b001;
bits<12> imm;
bits<5> rs1;
bits<5> rd;

let Inst{31-20} = imm{11-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 1;
let mayStore      = 0;

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
let InOperandList = ( ins X:$rs1, RV64IM_Itype_immAsInt64:$imm );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b1100000;
bits<3> funct3 = 0b110;
bits<12> imm;
bits<5> rs1;
bits<5> rd;

let Inst{31-20} = imm{11-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 1;
let mayStore      = 0;

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
let InOperandList = ( ins X:$rs1, RV64IM_Itype_immAsInt64:$imm );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b1100000;
bits<3> funct3 = 0b100;
bits<12> imm;
bits<5> rs1;
bits<5> rd;

let Inst{31-20} = imm{11-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 1;
let mayStore      = 0;

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
let InOperandList = ( ins X:$rs1, RV64IM_Itype_immAsInt64:$imm );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b1100000;
bits<3> funct3 = 0b101;
bits<12> imm;
bits<5> rs1;
bits<5> rd;

let Inst{31-20} = imm{11-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 1;
let mayStore      = 0;

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
let InOperandList = ( ins RV64IM_Utype_immAsInt64:$imm );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b1110110;
bits<20> imm;
bits<5> rd;

let Inst{31-12} = imm{19-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;

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
let InOperandList = ( ins X:$rs1, RV64IM_Itype_immAsInt64:$imm );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b1100000;
bits<3> funct3 = 0b010;
bits<12> imm;
bits<5> rs1;
bits<5> rd;

let Inst{31-20} = imm{11-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 1;
let mayStore      = 0;

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
let InOperandList = ( ins X:$rs1, RV64IM_Itype_immAsInt64:$imm );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b1100000;
bits<3> funct3 = 0b011;
bits<12> imm;
bits<5> rs1;
bits<5> rd;

let Inst{31-20} = imm{11-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 1;
let mayStore      = 0;

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

bits<7> opcode = 0b1100110;
bits<3> funct3 = 0b000;
bits<7> funct7 = 0b1000000;
bits<5> rs2;
bits<5> rs1;
bits<5> rd;

let Inst{31-25} = funct7{6-0};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;

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

bits<7> opcode = 0b1100110;
bits<3> funct3 = 0b100;
bits<7> funct7 = 0b1000000;
bits<5> rs2;
bits<5> rs1;
bits<5> rd;

let Inst{31-25} = funct7{6-0};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;

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

bits<7> opcode = 0b1100110;
bits<3> funct3 = 0b010;
bits<7> funct7 = 0b1000000;
bits<5> rs2;
bits<5> rs1;
bits<5> rd;

let Inst{31-25} = funct7{6-0};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;

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

bits<7> opcode = 0b1100110;
bits<3> funct3 = 0b110;
bits<7> funct7 = 0b1000000;
bits<5> rs2;
bits<5> rs1;
bits<5> rd;

let Inst{31-25} = funct7{6-0};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;

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

bits<7> opcode = 0b1101110;
bits<3> funct3 = 0b000;
bits<7> funct7 = 0b1000000;
bits<5> rs2;
bits<5> rs1;
bits<5> rd;

let Inst{31-25} = funct7{6-0};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;

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

bits<7> opcode = 0b1100110;
bits<3> funct3 = 0b011;
bits<7> funct7 = 0b0000000;
bits<5> rs2;
bits<5> rs1;
bits<5> rd;

let Inst{31-25} = funct7{6-0};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;

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
let InOperandList = ( ins X:$rs1, RV64IM_Itype_immAsInt64:$imm );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b1100100;
bits<3> funct3 = 0b011;
bits<12> imm;
bits<5> rs1;
bits<5> rd;

let Inst{31-20} = imm{11-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;

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

bits<7> opcode = 0b1100110;
bits<3> funct3 = 0b011;
bits<7> funct7 = 0b1000000;
bits<5> rs2;
bits<5> rs1;
bits<5> rd;

let Inst{31-25} = funct7{6-0};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;

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

bits<7> opcode = 0b1100110;
bits<3> funct3 = 0b111;
bits<7> funct7 = 0b1000000;
bits<5> rs2;
bits<5> rs1;
bits<5> rd;

let Inst{31-25} = funct7{6-0};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;

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

bits<7> opcode = 0b1101110;
bits<3> funct3 = 0b111;
bits<7> funct7 = 0b1000000;
bits<5> rs2;
bits<5> rs1;
bits<5> rd;

let Inst{31-25} = funct7{6-0};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;

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

bits<7> opcode = 0b1101110;
bits<3> funct3 = 0b011;
bits<7> funct7 = 0b1000000;
bits<5> rs2;
bits<5> rs1;
bits<5> rd;

let Inst{31-25} = funct7{6-0};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;

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
let InOperandList = ( ins X:$rs1, X:$rs2, RV64IM_Stype_immAsInt64:$imm );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b1100010;
bits<3> funct3 = 0b000;
bits<12> imm;
bits<5> rs2;
bits<5> rs1;

let Inst{11-7} = imm{4-0};
let Inst{31-25} = imm{11-5};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 1;

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
let InOperandList = ( ins X:$rs1, X:$rs2, RV64IM_Stype_immAsInt64:$imm );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b1100010;
bits<3> funct3 = 0b110;
bits<12> imm;
bits<5> rs2;
bits<5> rs1;

let Inst{11-7} = imm{4-0};
let Inst{31-25} = imm{11-5};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 1;

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
let InOperandList = ( ins X:$rs1, X:$rs2, RV64IM_Stype_immAsInt64:$imm );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b1100010;
bits<3> funct3 = 0b100;
bits<12> imm;
bits<5> rs2;
bits<5> rs1;

let Inst{11-7} = imm{4-0};
let Inst{31-25} = imm{11-5};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 1;

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

bits<7> opcode = 0b1100110;
bits<3> funct3 = 0b100;
bits<7> funct7 = 0b0000000;
bits<5> rs2;
bits<5> rs1;
bits<5> rd;

let Inst{31-25} = funct7{6-0};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;

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
let InOperandList = ( ins X:$rs1, RV64IM_Ftype_sftAsInt64:$sft );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b1100100;
bits<3> funct3 = 0b100;
bits<6> fun67 = 0b000000;
bits<6> sft;
bits<5> rs1;
bits<5> rd;

let Inst{31-26} = fun67{5-0};
let Inst{25-20} = sft{5-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;

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
let InOperandList = ( ins X:$rs1, RV64IM_Rtype_rs2AsInt64:$rs2 );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b1101100;
bits<3> funct3 = 0b100;
bits<7> funct7 = 0b0000000;
bits<5> rs2;
bits<5> rs1;
bits<5> rd;

let Inst{31-25} = funct7{6-0};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;

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

bits<7> opcode = 0b1101110;
bits<3> funct3 = 0b100;
bits<7> funct7 = 0b0000000;
bits<5> rs2;
bits<5> rs1;
bits<5> rd;

let Inst{31-25} = funct7{6-0};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;

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

bits<7> opcode = 0b1100110;
bits<3> funct3 = 0b010;
bits<7> funct7 = 0b0000000;
bits<5> rs2;
bits<5> rs1;
bits<5> rd;

let Inst{31-25} = funct7{6-0};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;

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
let InOperandList = ( ins X:$rs1, RV64IM_Itype_immAsInt64:$imm );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b1100100;
bits<3> funct3 = 0b010;
bits<12> imm;
bits<5> rs1;
bits<5> rd;

let Inst{31-20} = imm{11-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;

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
let InOperandList = ( ins X:$rs1, RV64IM_Itype_immAsInt64:$imm );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b1100100;
bits<3> funct3 = 0b110;
bits<12> imm;
bits<5> rs1;
bits<5> rd;

let Inst{31-20} = imm{11-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;

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

bits<7> opcode = 0b1100110;
bits<3> funct3 = 0b110;
bits<7> funct7 = 0b0000000;
bits<5> rs2;
bits<5> rs1;
bits<5> rd;

let Inst{31-25} = funct7{6-0};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;

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

bits<7> opcode = 0b1100110;
bits<3> funct3 = 0b101;
bits<7> funct7 = 0b0000010;
bits<5> rs2;
bits<5> rs1;
bits<5> rd;

let Inst{31-25} = funct7{6-0};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;

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
let InOperandList = ( ins X:$rs1, RV64IM_Ftype_sftAsInt64:$sft );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b1100100;
bits<3> funct3 = 0b101;
bits<6> fun67 = 0b000001;
bits<6> sft;
bits<5> rs1;
bits<5> rd;

let Inst{31-26} = fun67{5-0};
let Inst{25-20} = sft{5-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;

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
let InOperandList = ( ins X:$rs1, RV64IM_Rtype_rs2AsInt64:$rs2 );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b1101100;
bits<3> funct3 = 0b101;
bits<7> funct7 = 0b0000010;
bits<5> rs2;
bits<5> rs1;
bits<5> rd;

let Inst{31-25} = funct7{6-0};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;

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

bits<7> opcode = 0b1101110;
bits<3> funct3 = 0b101;
bits<7> funct7 = 0b0000010;
bits<5> rs2;
bits<5> rs1;
bits<5> rd;

let Inst{31-25} = funct7{6-0};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;

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

bits<7> opcode = 0b1100110;
bits<3> funct3 = 0b101;
bits<7> funct7 = 0b0000000;
bits<5> rs2;
bits<5> rs1;
bits<5> rd;

let Inst{31-25} = funct7{6-0};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;

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
let InOperandList = ( ins X:$rs1, RV64IM_Ftype_sftAsInt64:$sft );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b1100100;
bits<3> funct3 = 0b101;
bits<6> fun67 = 0b000000;
bits<6> sft;
bits<5> rs1;
bits<5> rd;

let Inst{31-26} = fun67{5-0};
let Inst{25-20} = sft{5-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;

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
let InOperandList = ( ins X:$rs1, RV64IM_Rtype_rs2AsInt64:$rs2 );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b1101100;
bits<3> funct3 = 0b101;
bits<7> funct7 = 0b0000000;
bits<5> rs2;
bits<5> rs1;
bits<5> rd;

let Inst{31-25} = funct7{6-0};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;

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

bits<7> opcode = 0b1101110;
bits<3> funct3 = 0b101;
bits<7> funct7 = 0b0000000;
bits<5> rs2;
bits<5> rs1;
bits<5> rd;

let Inst{31-25} = funct7{6-0};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;

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

bits<7> opcode = 0b1100110;
bits<3> funct3 = 0b000;
bits<7> funct7 = 0b0000010;
bits<5> rs2;
bits<5> rs1;
bits<5> rd;

let Inst{31-25} = funct7{6-0};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;

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

bits<7> opcode = 0b1101110;
bits<3> funct3 = 0b000;
bits<7> funct7 = 0b0000010;
bits<5> rs2;
bits<5> rs1;
bits<5> rd;

let Inst{31-25} = funct7{6-0};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;

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
let InOperandList = ( ins X:$rs1, X:$rs2, RV64IM_Stype_immAsInt64:$imm );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b1100010;
bits<3> funct3 = 0b010;
bits<12> imm;
bits<5> rs2;
bits<5> rs1;

let Inst{11-7} = imm{4-0};
let Inst{31-25} = imm{11-5};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 1;

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

bits<7> opcode = 0b1100110;
bits<3> funct3 = 0b001;
bits<7> funct7 = 0b0000000;
bits<5> rs2;
bits<5> rs1;
bits<5> rd;

let Inst{31-25} = funct7{6-0};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;

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
let InOperandList = ( ins X:$rs1, RV64IM_Itype_immAsInt64:$imm );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b1100100;
bits<3> funct3 = 0b001;
bits<12> imm;
bits<5> rs1;
bits<5> rd;

let Inst{31-20} = imm{11-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;

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
let InOperandList = ( ins  );

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 1;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}



def BGEZ : Instruction
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

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}



def BGTZ : Instruction
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

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}



def BLEZ : Instruction
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

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}



def BLTZ : Instruction
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

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}



def BNEZ : Instruction
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

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}



def CALL : Instruction
{
let Namespace = "processorNameValue";

let OutOperandList = ( outs  );
let InOperandList = ( ins bare_symbol:$symbol );

let isTerminator  = 1;
let isBranch      = 1;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 1;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [ PC,X1 ];
let Defs = [ X1,PC ];
}



def J : Instruction
{
let Namespace = "processorNameValue";

let OutOperandList = ( outs  );
let InOperandList = ( ins RV64IM_Jtype_immAsLabel:$imm );

let isTerminator  = 1;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 1;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [ PC ];
let Defs = [ PC,X0 ];
}

def : Pat<(br bb:$imm),
        (J RV64IM_Jtype_immAsLabel:$imm)>;


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

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

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

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}



def MOV : Instruction
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

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}

def : Pat<(add X:$rs1, (i64 0)),
        (MOV X:$rs1)>;

def : Pat<(add AddrFI:$rs1, (i64 0)),
        (MOV AddrFI:$rs1)>;


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

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}

def : Pat<(sub (i64 0), X:$rs1),
        (NEG X:$rs1)>;


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

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [ X0 ];
let Defs = [ X0 ];
}

def : Pat<(add (i64 0), (i64 0)),
        (NOP )>;


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

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}

def : Pat<(xor X:$rs1, (i64 4095)),
        (NOT X:$rs1)>;


def RET : Instruction
{
let Namespace = "processorNameValue";

let OutOperandList = ( outs  );
let InOperandList = ( ins  );

let isTerminator  = 1;
let isBranch      = 1;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 1;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [ PC,X1 ];
let Defs = [ PC,X0 ];
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

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}

def : Pat<(setcc (i64 0), X:$rs1, SETLT),
        (SGTZ X:$rs1)>;


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

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}

def : Pat<(setcc X:$rs1, (i64 0), SETLT),
        (SLTZ X:$rs1)>;


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

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}

def : Pat<(setcc (i64 0), X:$rs1, SETULT),
        (SNEZ X:$rs1)>;


def TAIL : Instruction
{
let Namespace = "processorNameValue";

let OutOperandList = ( outs  );
let InOperandList = ( ins bare_symbol:$symbol );

let isTerminator  = 1;
let isBranch      = 1;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 1;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [ PC,X6 ];
let Defs = [ PC ];
}






def : Pat<(add X:$rs1, X:$rs2),
        (ADD X:$rs1, X:$rs2)>;


def : Pat<(add X:$rs1, RV64IM_Itype_immAsInt64:$imm),
        (ADDI X:$rs1, RV64IM_Itype_immAsInt64:$imm)>;

def : Pat<(add AddrFI:$rs1, RV64IM_Itype_immAsInt64:$imm),
        (ADDI AddrFI:$rs1, RV64IM_Itype_immAsInt64:$imm)>;


def : Pat<(add X:$rs1, RV64IM_Itype_immAsInt64:$imm),
        (ADDIW X:$rs1, RV64IM_Itype_immAsInt64:$imm)>;


def : Pat<(add X:$rs1, X:$rs2),
        (ADDW X:$rs1, X:$rs2)>;


def : Pat<(and X:$rs1, X:$rs2),
        (AND X:$rs1, X:$rs2)>;


def : Pat<(and X:$rs1, RV64IM_Itype_immAsInt64:$imm),
        (ANDI X:$rs1, RV64IM_Itype_immAsInt64:$imm)>;




def : Pat<(brcc SETEQ, X:$rs1, X:$rs2, bb:$imm),
        (BEQ X:$rs1, X:$rs2, RV64IM_Btype_immAsLabel:$imm)>;

def : Pat<(brcond (i64 (seteq X:$rs1, X:$rs2)), bb:$imm),
        (BEQ X:$rs1, X:$rs2, RV64IM_Btype_immAsLabel:$imm)>;


def : Pat<(brcc SETGE, X:$rs1, X:$rs2, bb:$imm),
        (BGE X:$rs1, X:$rs2, RV64IM_Btype_immAsLabel:$imm)>;

def : Pat<(brcc SETLE, X:$rs2, X:$rs1, bb:$imm),
        (BGE X:$rs1, X:$rs2, RV64IM_Btype_immAsLabel:$imm)>;

def : Pat<(brcond (i64 (setge X:$rs1, X:$rs2)), bb:$imm),
        (BGE X:$rs1, X:$rs2, RV64IM_Btype_immAsLabel:$imm)>;

def : Pat<(brcond (i64 (setle X:$rs2, X:$rs1)), bb:$imm),
        (BGE X:$rs1, X:$rs2, RV64IM_Btype_immAsLabel:$imm)>;


def : Pat<(brcc SETUGE, X:$rs1, X:$rs2, bb:$imm),
        (BGEU X:$rs1, X:$rs2, RV64IM_Btype_immAsLabel:$imm)>;

def : Pat<(brcc SETULE, X:$rs2, X:$rs1, bb:$imm),
        (BGEU X:$rs1, X:$rs2, RV64IM_Btype_immAsLabel:$imm)>;

def : Pat<(brcond (i64 (setuge X:$rs1, X:$rs2)), bb:$imm),
        (BGEU X:$rs1, X:$rs2, RV64IM_Btype_immAsLabel:$imm)>;

def : Pat<(brcond (i64 (setule X:$rs2, X:$rs1)), bb:$imm),
        (BGEU X:$rs1, X:$rs2, RV64IM_Btype_immAsLabel:$imm)>;


def : Pat<(brcc SETLT, X:$rs1, X:$rs2, bb:$imm),
        (BLT X:$rs1, X:$rs2, RV64IM_Btype_immAsLabel:$imm)>;

def : Pat<(brcc SETGT, X:$rs2, X:$rs1, bb:$imm),
        (BLT X:$rs1, X:$rs2, RV64IM_Btype_immAsLabel:$imm)>;

def : Pat<(brcond (i64 (setlt X:$rs1, X:$rs2)), bb:$imm),
        (BLT X:$rs1, X:$rs2, RV64IM_Btype_immAsLabel:$imm)>;

def : Pat<(brcond (i64 (setgt X:$rs2, X:$rs1)), bb:$imm),
        (BLT X:$rs1, X:$rs2, RV64IM_Btype_immAsLabel:$imm)>;


def : Pat<(brcc SETULT, X:$rs1, X:$rs2, bb:$imm),
        (BLTU X:$rs1, X:$rs2, RV64IM_Btype_immAsLabel:$imm)>;

def : Pat<(brcc SETUGT, X:$rs2, X:$rs1, bb:$imm),
        (BLTU X:$rs1, X:$rs2, RV64IM_Btype_immAsLabel:$imm)>;

def : Pat<(brcond (i64 (setult X:$rs1, X:$rs2)), bb:$imm),
        (BLTU X:$rs1, X:$rs2, RV64IM_Btype_immAsLabel:$imm)>;

def : Pat<(brcond (i64 (setugt X:$rs2, X:$rs1)), bb:$imm),
        (BLTU X:$rs1, X:$rs2, RV64IM_Btype_immAsLabel:$imm)>;


def : Pat<(brcc SETNE, X:$rs1, X:$rs2, bb:$imm),
        (BNE X:$rs1, X:$rs2, RV64IM_Btype_immAsLabel:$imm)>;

def : Pat<(brcond (i64 (setne X:$rs1, X:$rs2)), bb:$imm),
        (BNE X:$rs1, X:$rs2, RV64IM_Btype_immAsLabel:$imm)>;

def : Pat<(brcond X:$rs1, bb:$imm),
        (BNE X:$rs1, X0, bb:$imm)>;


def : Pat<(sdiv X:$rs1, X:$rs2),
        (DIV X:$rs1, X:$rs2)>;


def : Pat<(udiv X:$rs1, X:$rs2),
        (DIVU X:$rs1, X:$rs2)>;


def : Pat<(udiv X:$rs1, X:$rs2),
        (DIVUW X:$rs1, X:$rs2)>;


def : Pat<(sdiv X:$rs1, X:$rs2),
        (DIVW X:$rs1, X:$rs2)>;










def : Pat<(i64 (sextloadi8 (add X:$rs1, RV64IM_Itype_immAsInt64:$imm))),
        (LB X:$rs1, RV64IM_Itype_immAsInt64:$imm)>;

def : Pat<(i64 (sextloadi8 X:$rs1)),
        (LB X:$rs1, (i64 0))>;

def : Pat<(i64 (extloadi8 X:$rs1)),
        (LB X:$rs1, (i64 0))>;

def : Pat<(i64 (sextloadi8 (add AddrFI:$rs1, RV64IM_Itype_immAsInt64:$imm))),
        (LB AddrFI:$rs1, RV64IM_Itype_immAsInt64:$imm)>;

def : Pat<(i64 (extloadi8 (add AddrFI:$rs1, RV64IM_Itype_immAsInt64:$imm))),
        (LB AddrFI:$rs1, RV64IM_Itype_immAsInt64:$imm)>;

def : Pat<(i64 (sextloadi8 AddrFI:$rs1)),
        (LB AddrFI:$rs1, (i64 0))>;

def : Pat<(i64 (extloadi8 AddrFI:$rs1)),
        (LB AddrFI:$rs1, (i64 0))>;


def : Pat<(i64 (zextloadi8 (add X:$rs1, RV64IM_Itype_immAsInt64:$imm))),
        (LBU X:$rs1, RV64IM_Itype_immAsInt64:$imm)>;

def : Pat<(i64 (zextloadi8 X:$rs1)),
        (LBU X:$rs1, (i64 0))>;

def : Pat<(i64 (zextloadi8 (add AddrFI:$rs1, RV64IM_Itype_immAsInt64:$imm))),
        (LBU AddrFI:$rs1, RV64IM_Itype_immAsInt64:$imm)>;

def : Pat<(i64 (zextloadi8 AddrFI:$rs1)),
        (LBU AddrFI:$rs1, (i64 0))>;


def : Pat<(i64 (load (add X:$rs1, RV64IM_Itype_immAsInt64:$imm))),
        (LD X:$rs1, RV64IM_Itype_immAsInt64:$imm)>;

def : Pat<(i64 (load X:$rs1)),
        (LD X:$rs1, (i64 0))>;

def : Pat<(i64 (load (add AddrFI:$rs1, RV64IM_Itype_immAsInt64:$imm))),
        (LD AddrFI:$rs1, RV64IM_Itype_immAsInt64:$imm)>;

def : Pat<(i64 (load AddrFI:$rs1)),
        (LD AddrFI:$rs1, (i64 0))>;


def : Pat<(i64 (sextloadi16 (add X:$rs1, RV64IM_Itype_immAsInt64:$imm))),
        (LH X:$rs1, RV64IM_Itype_immAsInt64:$imm)>;

def : Pat<(i64 (sextloadi16 X:$rs1)),
        (LH X:$rs1, (i64 0))>;

def : Pat<(i64 (extloadi16 X:$rs1)),
        (LH X:$rs1, (i64 0))>;

def : Pat<(i64 (sextloadi16 (add AddrFI:$rs1, RV64IM_Itype_immAsInt64:$imm))),
        (LH AddrFI:$rs1, RV64IM_Itype_immAsInt64:$imm)>;

def : Pat<(i64 (extloadi16 (add AddrFI:$rs1, RV64IM_Itype_immAsInt64:$imm))),
        (LH AddrFI:$rs1, RV64IM_Itype_immAsInt64:$imm)>;

def : Pat<(i64 (sextloadi16 AddrFI:$rs1)),
        (LH AddrFI:$rs1, (i64 0))>;

def : Pat<(i64 (extloadi16 AddrFI:$rs1)),
        (LH AddrFI:$rs1, (i64 0))>;


def : Pat<(i64 (zextloadi16 (add X:$rs1, RV64IM_Itype_immAsInt64:$imm))),
        (LHU X:$rs1, RV64IM_Itype_immAsInt64:$imm)>;

def : Pat<(i64 (zextloadi16 X:$rs1)),
        (LHU X:$rs1, (i64 0))>;

def : Pat<(i64 (zextloadi16 (add AddrFI:$rs1, RV64IM_Itype_immAsInt64:$imm))),
        (LHU AddrFI:$rs1, RV64IM_Itype_immAsInt64:$imm)>;

def : Pat<(i64 (zextloadi16 AddrFI:$rs1)),
        (LHU AddrFI:$rs1, (i64 0))>;




def : Pat<(i64 (sextloadi32 (add X:$rs1, RV64IM_Itype_immAsInt64:$imm))),
        (LW X:$rs1, RV64IM_Itype_immAsInt64:$imm)>;

def : Pat<(i64 (sextloadi32 X:$rs1)),
        (LW X:$rs1, (i64 0))>;

def : Pat<(i64 (extloadi32 X:$rs1)),
        (LW X:$rs1, (i64 0))>;

def : Pat<(i64 (sextloadi32 (add AddrFI:$rs1, RV64IM_Itype_immAsInt64:$imm))),
        (LW AddrFI:$rs1, RV64IM_Itype_immAsInt64:$imm)>;

def : Pat<(i64 (extloadi32 (add AddrFI:$rs1, RV64IM_Itype_immAsInt64:$imm))),
        (LW AddrFI:$rs1, RV64IM_Itype_immAsInt64:$imm)>;

def : Pat<(i64 (sextloadi32 AddrFI:$rs1)),
        (LW AddrFI:$rs1, (i64 0))>;

def : Pat<(i64 (extloadi32 AddrFI:$rs1)),
        (LW AddrFI:$rs1, (i64 0))>;


def : Pat<(i64 (zextloadi32 (add X:$rs1, RV64IM_Itype_immAsInt64:$imm))),
        (LWU X:$rs1, RV64IM_Itype_immAsInt64:$imm)>;

def : Pat<(i64 (zextloadi32 X:$rs1)),
        (LWU X:$rs1, (i64 0))>;

def : Pat<(i64 (zextloadi32 (add AddrFI:$rs1, RV64IM_Itype_immAsInt64:$imm))),
        (LWU AddrFI:$rs1, RV64IM_Itype_immAsInt64:$imm)>;

def : Pat<(i64 (zextloadi32 AddrFI:$rs1)),
        (LWU AddrFI:$rs1, (i64 0))>;


def : Pat<(mul X:$rs1, X:$rs2),
        (MUL X:$rs1, X:$rs2)>;


def : Pat<(mulhs X:$rs1, X:$rs2),
        (MULH X:$rs1, X:$rs2)>;


def : Pat<(mulhs X:$rs1, X:$rs2),
        (MULHSU X:$rs1, X:$rs2)>;


def : Pat<(mulhu X:$rs1, X:$rs2),
        (MULHU X:$rs1, X:$rs2)>;


def : Pat<(mulhs X:$rs1, X:$rs2),
        (MULW X:$rs1, X:$rs2)>;


def : Pat<(or X:$rs1, X:$rs2),
        (OR X:$rs1, X:$rs2)>;


def : Pat<(or X:$rs1, RV64IM_Itype_immAsInt64:$imm),
        (ORI X:$rs1, RV64IM_Itype_immAsInt64:$imm)>;


def : Pat<(srem X:$rs1, X:$rs2),
        (REM X:$rs1, X:$rs2)>;


def : Pat<(urem X:$rs1, X:$rs2),
        (REMU X:$rs1, X:$rs2)>;


def : Pat<(urem X:$rs1, X:$rs2),
        (REMUW X:$rs1, X:$rs2)>;


def : Pat<(srem X:$rs1, X:$rs2),
        (REMW X:$rs1, X:$rs2)>;


def : Pat<(truncstorei8 X:$rs2, (add X:$rs1, RV64IM_Stype_immAsInt64:$imm)),
        (SB X:$rs1, X:$rs2, RV64IM_Stype_immAsInt64:$imm)>;

def : Pat<(truncstorei8 X:$rs2, X:$rs1),
        (SB X:$rs1, X:$rs2, (i64 0))>;

def : Pat<(truncstorei8 X:$rs2, (add AddrFI:$rs1, RV64IM_Stype_immAsInt64:$imm)),
        (SB AddrFI:$rs1, X:$rs2, RV64IM_Stype_immAsInt64:$imm)>;

def : Pat<(truncstorei8 X:$rs2, AddrFI:$rs1),
        (SB AddrFI:$rs1, X:$rs2, (i64 0))>;


def : Pat<(store X:$rs2, (add X:$rs1, RV64IM_Stype_immAsInt64:$imm)),
        (SD X:$rs1, X:$rs2, RV64IM_Stype_immAsInt64:$imm)>;

def : Pat<(store X:$rs2, X:$rs1),
        (SD X:$rs1, X:$rs2, (i64 0))>;

def : Pat<(store X:$rs2, (add AddrFI:$rs1, RV64IM_Stype_immAsInt64:$imm)),
        (SD AddrFI:$rs1, X:$rs2, RV64IM_Stype_immAsInt64:$imm)>;

def : Pat<(store X:$rs2, AddrFI:$rs1),
        (SD AddrFI:$rs1, X:$rs2, (i64 0))>;


def : Pat<(truncstorei16 X:$rs2, (add X:$rs1, RV64IM_Stype_immAsInt64:$imm)),
        (SH X:$rs1, X:$rs2, RV64IM_Stype_immAsInt64:$imm)>;

def : Pat<(truncstorei16 X:$rs2, X:$rs1),
        (SH X:$rs1, X:$rs2, (i64 0))>;

def : Pat<(truncstorei16 X:$rs2, (add AddrFI:$rs1, RV64IM_Stype_immAsInt64:$imm)),
        (SH AddrFI:$rs1, X:$rs2, RV64IM_Stype_immAsInt64:$imm)>;

def : Pat<(truncstorei16 X:$rs2, AddrFI:$rs1),
        (SH AddrFI:$rs1, X:$rs2, (i64 0))>;


def : Pat<(shl X:$rs1, X:$rs2),
        (SLL X:$rs1, X:$rs2)>;


def : Pat<(shl X:$rs1, RV64IM_Ftype_sftAsInt64:$sft),
        (SLLI X:$rs1, RV64IM_Ftype_sftAsInt64:$sft)>;


def : Pat<(shl X:$rs1, RV64IM_Rtype_rs2AsInt64:$rs2),
        (SLLIW X:$rs1, RV64IM_Rtype_rs2AsInt64:$rs2)>;


def : Pat<(shl X:$rs1, X:$rs2),
        (SLLW X:$rs1, X:$rs2)>;


def : Pat<(setcc X:$rs1, X:$rs2, SETLT),
        (SLT X:$rs1, X:$rs2)>;

def : Pat<(setcc X:$rs1, X:$rs2, SETEQ),
        (SLTIU (XOR X:$rs1, X:$rs2), 1)>;

def : Pat<(setcc X:$rs1, X:$rs2, SETGT),
        (SLT X:$rs2, X:$rs1)>;

def : Pat<(setcc X:$rs1, X:$rs2, SETLE),
        (XORI (SLT X:$rs2, X:$rs1), 1)>;


def : Pat<(setcc X:$rs1, RV64IM_Itype_immAsInt64:$imm, SETLT),
        (SLTI X:$rs1, RV64IM_Itype_immAsInt64:$imm)>;


def : Pat<(setcc X:$rs1, RV64IM_Itype_immAsInt64:$imm, SETULT),
        (SLTIU X:$rs1, RV64IM_Itype_immAsInt64:$imm)>;

def : Pat<(setcc X:$rs1, RV64IM_Itype_immAsInt64:$imm, SETNE),
        (SLTU X0, (XORI X:$rs1, RV64IM_Itype_immAsInt64:$imm))>;


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


def : Pat<(sra X:$rs1, RV64IM_Ftype_sftAsInt64:$sft),
        (SRAI X:$rs1, RV64IM_Ftype_sftAsInt64:$sft)>;


def : Pat<(sra X:$rs1, RV64IM_Rtype_rs2AsInt64:$rs2),
        (SRAIW X:$rs1, RV64IM_Rtype_rs2AsInt64:$rs2)>;


def : Pat<(sra X:$rs1, X:$rs2),
        (SRAW X:$rs1, X:$rs2)>;


def : Pat<(srl X:$rs1, X:$rs2),
        (SRL X:$rs1, X:$rs2)>;


def : Pat<(srl X:$rs1, RV64IM_Ftype_sftAsInt64:$sft),
        (SRLI X:$rs1, RV64IM_Ftype_sftAsInt64:$sft)>;


def : Pat<(srl X:$rs1, RV64IM_Rtype_rs2AsInt64:$rs2),
        (SRLIW X:$rs1, RV64IM_Rtype_rs2AsInt64:$rs2)>;


def : Pat<(srl X:$rs1, X:$rs2),
        (SRLW X:$rs1, X:$rs2)>;


def : Pat<(sub X:$rs1, X:$rs2),
        (SUB X:$rs1, X:$rs2)>;


def : Pat<(sub X:$rs1, X:$rs2),
        (SUBW X:$rs1, X:$rs2)>;


def : Pat<(truncstorei32 X:$rs2, (add X:$rs1, RV64IM_Stype_immAsInt64:$imm)),
        (SW X:$rs1, X:$rs2, RV64IM_Stype_immAsInt64:$imm)>;

def : Pat<(truncstorei32 X:$rs2, X:$rs1),
        (SW X:$rs1, X:$rs2, (i64 0))>;

def : Pat<(truncstorei32 X:$rs2, (add AddrFI:$rs1, RV64IM_Stype_immAsInt64:$imm)),
        (SW AddrFI:$rs1, X:$rs2, RV64IM_Stype_immAsInt64:$imm)>;

def : Pat<(truncstorei32 X:$rs2, AddrFI:$rs1),
        (SW AddrFI:$rs1, X:$rs2, (i64 0))>;


def : Pat<(xor X:$rs1, X:$rs2),
        (XOR X:$rs1, X:$rs2)>;


def : Pat<(xor X:$rs1, RV64IM_Itype_immAsInt64:$imm),
        (XORI X:$rs1, RV64IM_Itype_immAsInt64:$imm)>;
















def : Pat<(br bb:$imm),
        (J RV64IM_Jtype_immAsLabel:$imm)>;






def : Pat<(add X:$rs1, (i64 0)),
        (MOV X:$rs1)>;

def : Pat<(add AddrFI:$rs1, (i64 0)),
        (MOV AddrFI:$rs1)>;


def : Pat<(sub (i64 0), X:$rs1),
        (NEG X:$rs1)>;


def : Pat<(add (i64 0), (i64 0)),
        (NOP )>;


def : Pat<(xor X:$rs1, (i64 4095)),
        (NOT X:$rs1)>;




def : Pat<(setcc (i64 0), X:$rs1, SETLT),
        (SGTZ X:$rs1)>;


def : Pat<(setcc X:$rs1, (i64 0), SETLT),
        (SLTZ X:$rs1)>;


def : Pat<(setcc (i64 0), X:$rs1, SETULT),
        (SNEZ X:$rs1)>;




def : Pat<(rotl X:$rs1, X:$rs2),
        (OR (SLL X:$rs1, X:$rs2), (SRL X:$rs1, (SUB (LI (i64 64)), X:$rs2)))>;

let isCall = 1 in {
    def PseudoCALLIndirect : Pseudo<(outs ), (ins X:$rs1),
                        [(target_call X:$rs1)]>,
                 PseudoInstExpansion<(JALR X1, X:$rs1, 0)>;
}

let isCall = 1 in {
    def PseudoBRIND : Pseudo<(outs ), (ins X:$rs1),
                        [(brind X:$rs1)]>,
                 PseudoInstExpansion<(JALR X0, X:$rs1, 0)>;
}