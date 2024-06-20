#include "[(${namespace})]TargetObjectFile.h"
#include "[(${namespace})]Subtarget.h"
#include "[(${namespace})]TargetMachine.h"
#include "llvm/MC/MCContext.h"
#include "llvm/Target/TargetMachine.h"
#include "llvm/Support/Debug.h"

#define DEBUG_TYPE "[(${namespace})]TargetObjectFile"

using namespace llvm;

void [(${namespace})]TargetObjectFile::Initialize(MCContext &Ctx, const TargetMachine &TM)
{
    TargetLoweringObjectFileELF::Initialize(Ctx, TM);
    this->TM = &static_cast<const [(${namespace})]TargetMachine &>(TM);
}