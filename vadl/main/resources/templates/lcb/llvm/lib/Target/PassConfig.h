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
        [(${namespace})]PassConfig( «processorName»TargetMachine& TM, PassManagerBase* PassManager)
            : TargetPassConfig(TM, *PassManager)
        {
        }

        [(${namespace})]TargetMachine &get«processorName»TargetMachine() const
        {
            return getTM<«processorName»TargetMachine>();
        }

        bool addInstSelector() override;
        void addPreRegAlloc() override;
    };
}

#endif // LLVM_LIB_TARGET_[(${namespace})]_[(${namespace})]PASSCONFIG_H