#ifndef LLVM_LIB_TARGET_[(${namespace})]_[(${namespace})]PASSCONFIG_H
#define LLVM_LIB_TARGET_[(${namespace})]_[(${namespace})]PASSCONFIG_H

#include "[(${namespace})]TargetMachine.h"
#include "llvm/CodeGen/Passes.h"
#include "llvm/CodeGen/TargetPassConfig.h"

namespace llvm
{
    class [(${namespace})]PassConfig : public TargetPassConfig
    {
    public:
        [(${namespace})]PassConfig( [(${namespace})]TargetMachine& TM, PassManagerBase* PassManager)
            : TargetPassConfig(TM, *PassManager)
        {
        }

        [(${namespace})]TargetMachine &get[(${namespace})]TargetMachine() const
        {
            return getTM<[(${namespace})]TargetMachine>();
        }

        bool addInstSelector() override;
        void addPreRegAlloc() override;
        void addPreEmitPass() override;
    };
}

#endif // LLVM_LIB_TARGET_[(${namespace})]_[(${namespace})]PASSCONFIG_H