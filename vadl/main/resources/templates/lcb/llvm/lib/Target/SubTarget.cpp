#include "«processorName»Subtarget.h"
#include "llvm/Support/Debug.h"

#define DEBUG_TYPE "«processorName»Subtarget"

using namespace llvm;

#define GET_SUBTARGETINFO_TARGET_DESC
#define GET_SUBTARGETINFO_CTOR
#include "«processorName»GenSubtargetInfo.inc"

void «processorName»Subtarget::anchor() {}

«processorName»Subtarget::«processorName»Subtarget(const Triple &TargetTriple, StringRef Cpu, StringRef TuneCPU, StringRef FeatureString, const TargetMachine &TM, const TargetOptions &Options, CodeModel::Model CodeModel, CodeGenOpt::Level OptLevel)
    : «processorName»GenSubtargetInfo(TargetTriple, Cpu, TuneCPU, FeatureString), InstrInfo(*this) // «processorName»InstrInfo
      ,
      FrameLowering(*this) // «processorName»FrameLowering
      ,
      TLInfo(TM, *this) // «processorName»TargetLowering
      ,
      TSInfo() // SelectionDAGTargetInfo
      ,
      RegInfo() // «processorName»RegisterInfo
{
}