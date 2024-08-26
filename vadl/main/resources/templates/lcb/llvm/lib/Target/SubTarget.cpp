#include "[(${namespace})]Subtarget.h"
#include "llvm/Support/Debug.h"

#define DEBUG_TYPE "[(${namespace})]Subtarget"

using namespace llvm;

#define GET_SUBTARGETINFO_TARGET_DESC
#define GET_SUBTARGETINFO_CTOR
#include "[(${namespace})]GenSubtargetInfo.inc"

void [(${namespace})]Subtarget::anchor() {}

[(${namespace})]Subtarget::[(${namespace})]Subtarget(const Triple &TargetTriple, StringRef Cpu, StringRef TuneCPU, StringRef FeatureString, const TargetMachine &TM, const TargetOptions &Options, CodeModel::Model CodeModel, CodeGenOpt::Level OptLevel)
    : [(${namespace})]GenSubtargetInfo(TargetTriple, Cpu, TuneCPU, FeatureString), InstrInfo(*this) // [(${namespace})]InstrInfo
      ,
      FrameLowering(*this) // [(${namespace})]FrameLowering
      ,
      TLInfo(TM, *this) // [(${namespace})]TargetLowering
      ,
      TSInfo() // SelectionDAGTargetInfo
      ,
      RegInfo() // [(${namespace})]RegisterInfo
{
}