//===--------------------------------------------------------------------------===//
// This file specifies register calling conventions for [(${namespace})] target.
//===--------------------------------------------------------------------------===//

def CSR_[(${namespace})] : CalleeSavedRegs<(add calleeSavedRegs)>;

def CC_[(${namespace})] : CallingConv<[
    CCIfType
    <
    [(${functionRegisters.type})]
    CCAssignToReg< [ [(${functionRegisters.registerRefs})] ] >
    >,

    // Otherwise the values are assigned to the stack in specific aligned units.
    // the first parameter indicates the size of the slot, and
    // the second parameter indicates the stack alignment.
    // if size is zero, then the ABI size is used;
    // if alignment is zero, then the ABI alignment is used.
    CCAssignToStack<0, 0>
]>;

def RetCC_[(${namespace})] : CallingConv<[
    CCIfType
    <
    [(${returnRegisters.type})]
    CCAssignToReg< [ [(${returnRegisters.registerRefs})] ] >
    >
]>;

// no preserved mask
def CSR_NoRegs : CalleeSavedRegs<(add)>;