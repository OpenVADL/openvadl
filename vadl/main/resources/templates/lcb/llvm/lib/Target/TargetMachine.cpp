#include "[(${namespace})]TargetObjectFile.h"
#include "[(${namespace})]TargetMachine.h"
#include "TargetInfo/[(${namespace})]TargetInfo.h"
#include "[(${namespace})]PassConfig.h"
#include "llvm/MC/TargetRegistry.h"
#include "llvm/Support/Debug.h"
#include <string>
#include "[(${namespace})]MachineFunctionInfo.h"

#define DEBUG_TYPE "[(${namespace})]"

using namespace llvm;

extern "C" void LLVMInitialize[(${namespace})]Target()
{
    RegisterTargetMachine<[(${namespace})]TargetMachine> X(getThe[(${namespace})]Target());
}

static std::string getDataLayout()
{
    return "[(${dataLayout})]";
}

static Reloc::Model get[(${namespace})]EffectiveRelocModel(std::optional<Reloc::Model> RM)
{
    if (!RM.has_value())
    {
        return Reloc::Static;
    }

    return *RM;
}

static CodeModel::Model get[(${namespace})]EffectiveCodeModel(std::optional<CodeModel::Model> CM)
{
    // if (CM)
    // {
    //     // By default, targets do not support the tiny and kernel models.
    //     if (*CM == CodeModel::Tiny)
    //         report_fatal_error("Target does not support the tiny CodeModel", false);

    //     if (*CM == CodeModel::Kernel)
    //         report_fatal_error("Target does not support the kernel CodeModel", false);

    //     return *CM;
    // }

    // for now only use the small code model!
    return CodeModel::Small;
}

void [(${namespace})]TargetMachine::anchor() {}

[(${namespace})]TargetMachine::[(${namespace})]TargetMachine(const Target &T, const Triple &TT, StringRef CPU, StringRef FS, const TargetOptions &Options, std::optional<Reloc::Model> RM, std::optional<CodeModel::Model> CM, CodeGenOpt::Level OL, bool JIT)
    : LLVMTargetMachine(T, getDataLayout(), TT, CPU, FS, Options, get[(${namespace})]EffectiveRelocModel(RM) // has to do with addresses loading
                        ,
                        get[(${namespace})]EffectiveCodeModel(CM) // has to do with addresses loading
                        ,
                        OL),
      Subtarget(TT, CPU, "", FS, *this, Options, getCodeModel(), OL), TLOF(std::make_unique<[(${namespace})]TargetObjectFile>())
{
    initAsmInfo();
}

[(${namespace})]TargetMachine::~[(${namespace})]TargetMachine() {}

TargetPassConfig *[(${namespace})]TargetMachine::createPassConfig(PassManagerBase &PassManager)
{
    return new [(${namespace})]PassConfig(*this, &PassManager);
}

MachineFunctionInfo *[(${namespace})]TargetMachine::createMachineFunctionInfo(
    BumpPtrAllocator &Allocator, const Function &F,
    const TargetSubtargetInfo *STI) const
{
    return [(${namespace})]MachineFunctionInfo::create<[(${namespace})]MachineFunctionInfo>(Allocator,
                                                                                          F, STI);
}