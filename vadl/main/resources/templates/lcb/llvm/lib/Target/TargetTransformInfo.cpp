#include "[(${namespace})]TargetTransformInfo.h"
#include "llvm/Analysis/TargetTransformInfo.h"

using namespace llvm;

// should be always the correct comparison order
bool [(${namespace})]TTIImpl::isLSRCostLess(const TargetTransformInfo::LSRCost &C1,
                                 const TargetTransformInfo::LSRCost &C2) {

  unsigned C1NumRegs = C1.NumRegs + (C1.NumBaseAdds != 0);
  unsigned C2NumRegs = C2.NumRegs + (C2.NumBaseAdds != 0);
  return std::tie(C1.Insns, C1NumRegs, C1.AddRecCost,
                  C1.NumIVMuls, C1.NumBaseAdds,
                  C1.ScaleCost, C1.ImmCost, C1.SetupCost) <
         std::tie(C2.Insns, C2NumRegs, C2.AddRecCost,
                  C2.NumIVMuls, C2.NumBaseAdds,
                  C2.ScaleCost, C2.ImmCost, C2.SetupCost);
}
