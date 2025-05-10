#ifndef LLVM_LIB_TARGET_[(${namespace})]_[(${namespace})]_H
#define LLVM_LIB_TARGET_[(${namespace})]_[(${namespace})]_H

#include "llvm/Target/TargetMachine.h"

namespace llvm
{
    class FunctionPass;
    class [(${namespace})]TargetMachine;
    class PassRegistry;

    FunctionPass *create[(${namespace})]ISelDag( [(${namespace})]TargetMachine &TM, CodeGenOptLevel OptLevel );

    FunctionPass *create[(${namespace})]ExpandPseudoPass();
    void initialize[(${namespace})]ExpandPseudoPass(PassRegistry &);
    void initialize[(${namespace})]DAGToDAGISelLegacyPass(PassRegistry &);
}

#endif // LLVM_LIB_TARGET_[(${namespace})]_[(${namespace})]_H
