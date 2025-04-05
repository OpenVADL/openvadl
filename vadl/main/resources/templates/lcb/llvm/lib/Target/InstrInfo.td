/*
 * Node representing the frame index.
 * The method SelectAddrFI is used to determine
 * if the specific register is a frame pointer.
 */
def AddrFI : ComplexPattern<iPTR, 1, "SelectAddrFI", [frameindex], []>;

// symbol operand used for matching load and call sequences.
// the size is determined by the program counter size.
def bare_symbol : Operand<[(${stackPointerType})]>;

def SDT_CallSeqStart : SDCallSeqStart<[SDTCisVT<0, [(${stackPointerType})]>, SDTCisVT<1, [(${stackPointerType})]>]>;
def SDT_CallSeqEnd   : SDCallSeqEnd<[SDTCisVT<0, [(${stackPointerType})]>, SDTCisVT<1, [(${stackPointerType})]>]>;

// Target-dependent type requirements
def SDT_CPU_Call : SDTypeProfile<0, -1, [SDTCisVT<0, [(${stackPointerType})]>]>;

class InstFormat<bits<5> val> {
    bits<5> Value = val;
}

class GenericInstCommon<dag outs, dag ins, string opcodestr, string argstr,
           list<dag> pattern, InstFormat format> : Instruction {
    let Namespace = "[(${namespace})]";

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

def target_call : SDNode<"[(${namespace})]ISD::CALL", SDT_CPU_Call,
                           [SDNPHasChain, SDNPOptInGlue, SDNPOutGlue, SDNPVariadic]>;

def target_ret_flag : SDNode<"[(${namespace})]ISD::RET_FLAG", SDTNone,
                           [SDNPHasChain, SDNPOptInGlue, SDNPVariadic]>;

/*
 * ADJCALLSTACKDOWN is a pseudo instruction used to represent the
 * 'CFSetupOpcode', which is needed for the call frame setup
 */
def ADJCALLSTACKDOWN : Instruction
{
    let InOperandList = (ins [(${stackPointerType})]imm:$amt1, [(${stackPointerType})]imm:$amt2); /* i32imm : Operand<i32> */
    let OutOperandList = (outs);
    let Pattern = [ (callseq_start timm:$amt1, timm:$amt2) ];
    let Namespace = "[(${namespace})]";
    let isPseudo = 1;
    let isCodeGenOnly = 1;
    let Defs = [ [(${stackPointerRegister})] ]; // stack pointer
    let Uses = [ [(${stackPointerRegister})] ]; // stack pointer
}

/*
 * ADJCALLSTACKUP is a pseudo instruction used to represent the
 * 'CFDestroyOpcode', which is needed for the call frame setup
 */
def ADJCALLSTACKUP : Instruction
{
    let InOperandList = (ins [(${stackPointerType})]imm:$amt1, [(${stackPointerType})]imm:$amt2);
    let OutOperandList = (outs);
    let Pattern = [ (callseq_end timm:$amt1, timm:$amt2) ];
    let Namespace = "[(${namespace})]";
    let isPseudo = 1;
    let isCodeGenOnly = 1;
    let Defs = [ [(${stackPointerRegister})] ]; // stack pointer
    let Uses = [ [(${stackPointerRegister})] ]; // stack pointer
}

def [(${returnInstruction})] : Instruction
{
    let Namespace = "[(${namespace})]";
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
def [(${callInstruction})] : Instruction
{
    let Namespace = "[(${namespace})]";
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
    let Defs = [ [(${returnAddress})] ];
    let Uses = [];
}

/* Match the call sequence for global and external symbols */
def : Pat<(target_call tglobaladdr:$func), ([(${callInstruction})] tglobaladdr:$func)>;
def : Pat<(target_call texternalsym:$func), ([(${callInstruction})] texternalsym:$func)>;

def SDT_[(${namespace})]SelectCC     : SDTypeProfile<1, 5, [SDTCisSameAs<1, 2>,
                                         SDTCisSameAs<0, 4>,
                                         SDTCisSameAs<4, 5>]>;
def target_selectcc  : SDNode<"[(${namespace})]ISD::SELECT_CC", SDT_[(${namespace})]SelectCC,
                     [SDNPInGlue]>;

def vadl_imm32 : ImmLeaf<i32, [{ return true; }]>, Operand<i32>
{

}

def vadl_imm64 : ImmLeaf<i64, [{ return true; }]>, Operand<i64>
{

}

[#th:block th:each="registerFile : ${registerFiles}" ]
[#th:block th:if="${registerFile.resultWidth == 32}"]
def SelectCC_[(${registerFile.name})]: Instruction {
    field bits<32> Inst;
    // SoftFail is a field the disassembler can use to provide a way for
    // instructions to not match without killing the whole decode process. It is
    // mainly used for ARM, but Tablegen expects this field to exist or it fails
    // to build the decode table.
    field bits<32> SoftFail = 0;

    bits<7> Opcode = 0;

    let Inst{6-0} = Opcode;

    let Namespace = "[(${namespace})]";

    dag OutOperandList = (outs [(${registerFile.name})]:$dst);
    dag InOperandList = (ins [(${registerFile.name})]:$lhs, [(${registerFile.name})]:$rhs, vadl_imm32:$imm, [(${registerFile.name})]:$truev, [(${registerFile.name})]:$falsev);
    let Pattern = [ (set [(${registerFile.name})]:$dst, (target_selectcc [(${registerFile.name})]:$lhs, [(${registerFile.name})]:$rhs,
                (i32 vadl_imm32:$imm), [(${registerFile.name})]:$truev, [(${registerFile.name})]:$falsev)) ];

    let TSFlags{4-0} = 0;
    let usesCustomInserter = 1;
    let isPseudo = 1;
    let isCodeGenOnly = 1;
}
[/th:block]
[#th:block th:if="${registerFile.resultWidth == 64}"]
def SelectCC_[(${registerFile.name})]: Instruction {
    field bits<64> Inst;
    // SoftFail is a field the disassembler can use to provide a way for
    // instructions to not match without killing the whole decode process. It is
    // mainly used for ARM, but Tablegen expects this field to exist or it fails
    // to build the decode table.
    field bits<32> SoftFail = 0;

    bits<7> Opcode = 0;

    let Inst{6-0} = Opcode;

    let Namespace = "[(${namespace})]";

    dag OutOperandList = (outs [(${registerFile.name})]:$dst);
    dag InOperandList = (ins [(${registerFile.name})]:$lhs, [(${registerFile.name})]:$rhs, vadl_imm64:$imm, [(${registerFile.name})]:$truev, [(${registerFile.name})]:$falsev);
    let Pattern = [ (set [(${registerFile.name})]:$dst, (target_selectcc [(${registerFile.name})]:$lhs, [(${registerFile.name})]:$rhs,
                (i64 vadl_imm64:$imm), [(${registerFile.name})]:$truev, [(${registerFile.name})]:$falsev)) ];

    let TSFlags{4-0} = 0;
    let usesCustomInserter = 1;
    let isPseudo = 1;
    let isCodeGenOnly = 1;
}
[/th:block]
[/th:block]

[# th:each="imm : ${immediates}" ]
[(${imm})]
[/]

[# th:each="instruction : ${instructions}" ]
[(${instruction})]
[/]

def : Pat<([(${stackPointerType})] AddrFI:$rs1),
  ([(${addi})] AddrFI:$rs1, ([(${stackPointerType})] 0))>;

[# th:each="pseudo : ${pseudos}" ]
[(${pseudo})]
[/]

[# th:each="comp : ${compiler}" ]
[(${comp})]
[/]

[# th:each="instAlias : ${instAliases}" ]
[(${instAlias})]
[/]

[# th:each="pattern : ${patterns}" ]
[(${pattern})]
[/]