#ifndef LLVM_LIB_TARGET_[(${namespace})]_[(${namespace})]TARGETOBJECTFILE_H
#define LLVM_LIB_TARGET_[(${namespace})]_[(${namespace})]TARGETOBJECTFILE_H

#include "llvm/CodeGen/TargetLoweringObjectFileImpl.h"

namespace llvm
{
    class [(${namespace})]TargetMachine;

    class [(${namespace})]TargetObjectFile : public TargetLoweringObjectFileELF
    {
        const [(${namespace})]TargetMachine *TM;

    public:
        void Initialize(MCContext & Ctx, const TargetMachine &TM) override;
    };
}

#endif