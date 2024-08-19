#ifndef LLVM_LIB_TARGET_[(${namespace})]_[(${namespace})]_H
#define LLVM_LIB_TARGET_[(${namespace})]_[(${namespace})]_H

#include "llvm/Target/TargetMachine.h"

namespace llvm
{
    class FunctionPass;
    class [(${namespace})]TargetMachine;
    class PassRegistry;

    FunctionPass *create[(${namespace})]ISelDag( [(${namespace})]TargetMachine &TM, CodeGenOpt::Level OptLevel );

    FunctionPass *create[(${namespace})]ExpandPseudoPass();
    void initialize[(${namespace})]ExpandPseudoPass(PassRegistry &);
}

#endif // LLVM_LIB_TARGET_[(${namespace})]_[(${namespace})]_H
