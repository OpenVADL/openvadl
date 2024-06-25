//===--------------------------------------------------------------------------===//
// This file specifies register calling conventions for [(${namespace})] target.
//===--------------------------------------------------------------------------===//

def CSR_[(${namespace})] : CalleeSavedRegs<(add «FOR register : calleeSaved SEPARATOR ", "»«emit(register)»«ENDFOR»)>;

def CC_[(${namespace})] : CallingConv<[

    «emitAssignToReg( functionArgument )»

    «emitAssignToMem( functionArgument )»

]>;

def RetCC_[(${namespace})] : CallingConv<[
    «emitAssignToReg( returnValue )»
]>;

// no preserved mask
def CSR_NoRegs : CalleeSavedRegs<(add)>;