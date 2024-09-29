#ifndef LLVM_LIB_TARGET_[(${namespace})]_[(${namespace})]TARGETMACHINE_H
#define LLVM_LIB_TARGET_[(${namespace})]_[(${namespace})]TARGETMACHINE_H

#include "[(${namespace})]SubTarget.h"

#include "llvm/CodeGen/Passes.h"
#include "llvm/CodeGen/TargetPassConfig.h"
#include "llvm/Target/TargetMachine.h"

namespace llvm
{
    class [(${namespace})]TargetMachine : public LLVMTargetMachine
    {
        // virtual anchor method to decrease link time as the vtable
        virtual void anchor();

    public:
        [(${namespace})]TargetMachine(const Target &T, const Triple &TT, StringRef CPU, StringRef FS, const TargetOptions &Options, std::optional<Reloc::Model> RM, std::optional<CodeModel::Model> CM, CodeGenOpt::Level OL, bool JIT);

        ~[(${namespace})]TargetMachine() override;

        const [(${namespace})]Subtarget *getSubtargetImpl(const llvm::Function & /*Fn*/) const override
        {
            return &Subtarget; // Default (and only) Subtarget
        }

        TargetPassConfig *createPassConfig(PassManagerBase & PassManager);

        TargetLoweringObjectFile *getObjFileLowering() const override
        {
            // TODO: @chochrainer this needs to be init for asm printer
            // does nothing now and should not be used yet
            return TLOF.get();
        }

        MachineFunctionInfo *createMachineFunctionInfo(BumpPtrAllocator & Allocator, const Function &F,
                                                       const TargetSubtargetInfo *STI) const override;

    private:
        [(${namespace})]Subtarget Subtarget;
        std::unique_ptr<TargetLoweringObjectFile> TLOF;
    };
}

#endif