#ifndef LLVM_LIB_TARGET_[(${namespace})]_[(${namespace})]TARGETTRANSFORMINFO_H
#define LLVM_LIB_TARGET_[(${namespace})]_[(${namespace})]TARGETTRANSFORMINFO_H

#include "[(${namespace})]TargetMachine.h"
#include "[(${namespace})]SubTarget.h"
#include "llvm/Analysis/TargetTransformInfo.h"
#include "llvm/CodeGen/BasicTTIImpl.h"
#include "llvm/IR/Function.h"

namespace llvm {

class [(${namespace})]TTIImpl : public BasicTTIImplBase<[(${namespace})]TTIImpl> {
  using BaseT = BasicTTIImplBase<[(${namespace})]TTIImpl>;
  using TTI = TargetTransformInfo;

  friend BaseT;

  const [(${namespace})]Subtarget *ST;
  const [(${namespace})]TargetLowering *TLI;

  const [(${namespace})]Subtarget *getST() const { return ST; }
  const [(${namespace})]TargetLowering *getTLI() const { return TLI; }



public:
  explicit [(${namespace})]TTIImpl(const [(${namespace})]TargetMachine *TM, const Function &F)
      : BaseT(TM, F.getDataLayout()), ST(TM->getSubtargetImpl(F)),
        TLI(ST->getTargetLowering()) {}

  bool shouldFoldTerminatingConditionAfterLSR() const {
    return true;
    // TODO: figure out what this does, as it is only used in RISCV it seems (not using this function breaks
    // loop strength reduce functionality though).
  }

  bool isLSRCostLess(const TargetTransformInfo::LSRCost &C1,
                     const TargetTransformInfo::LSRCost &C2) {
    // specify that "instructions are number 1 priority".
    // this should be true from the view of a RISCy architecture.
    return std::tie(C1.Insns, C1.NumRegs, C1.AddRecCost, C1.NumIVMuls,
                    C1.NumBaseAdds, C1.ScaleCost, C1.ImmCost, C1.SetupCost) <
           std::tie(C2.Insns, C2.NumRegs, C2.AddRecCost, C2.NumIVMuls,
                    C2.NumBaseAdds, C2.ScaleCost, C2.ImmCost, C2.SetupCost);
  }
};

}

#endif