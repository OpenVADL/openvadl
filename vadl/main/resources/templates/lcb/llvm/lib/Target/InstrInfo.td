/*
 * Node representing the frame index.
 * The method CPUDAGToDAGISel::SelectAddrFI is used to determine
 * if the specific register is a frame pointer.
 */
def AddrFI : ComplexPattern<iPTR, 1, "SelectAddrFI", [frameindex], []>;

def SDT_CallSeqStart : SDCallSeqStart<[SDTCisVT<0, [(${stackPointerType})]>, SDTCisVT<1, [(${stackPointerType})]>]>;
def SDT_CallSeqEnd   : SDCallSeqEnd<[SDTCisVT<0, [(${stackPointerType})]>, SDTCisVT<1, [(${stackPointerType})]>]>;

// Target-dependent type requirements
def SDT_CPU_Call : SDTypeProfile<0, -1, [SDTCisVT<0, [(${stackPointerType})]>]>;

// Target-independent nodes, but with target-specific formats
def callseq_start : SDNode<"ISD::CALLSEQ_START", SDT_CallSeqStart, [SDNPHasChain, SDNPOutGlue]>;
def callseq_end   : SDNode<"ISD::CALLSEQ_END", SDT_CallSeqEnd, [SDNPHasChain, SDNPOptInGlue, SDNPOutGlue]>;

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
    let Defs = [ X2 ]; // stack pointer
    let Uses = [ X2 ]; // stack pointer
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
    let Defs = [ X2 ]; // stack pointer
    let Uses = [ X2 ]; // stack pointer
}

def RESERVERD_PSEUDO_RET : Instruction
{
    let Namespace = "[(${namespace})]";
    /* let Size = 4; // ( Bits<32> )
    let CodeSize = 4; // ( Bits<32> ), used for ISEL cost */
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
def PSEUDO_CALL : Instruction
{
    let Namespace = "[(${namespace})]";
    //let Size = 8; // ( Bits<32>, Bits<32> )
    //let CodeSize = 8; // ( Bits<32>, Bits<32> ), used for ISEL cost
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
def : Pat<(target_call tglobaladdr:$func), (PSEUDO_CALL tglobaladdr:$func)>;
def : Pat<(target_call texternalsym:$func), (PSEUDO_CALL texternalsym:$func)>;

[# th:each="imm : ${immediates}" ]
[(${imm})]
[/]

[# th:each="m : ${constMats}" ]
[(${m})]
[/]

[# th:each="instruction : ${instructions}" ]
[(${instruction})]
[/]

[# th:each="pseudo : ${pseudos}" ]
[(${pseudo})]
[/]