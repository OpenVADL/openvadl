#include "llvm/ADT/SmallVector.h"
#include "llvm/MC/MCRegister.h"
#include "llvm/MC/MCSubtargetInfo.h"
#include <cstdint>
#include <bitset>
#include "[(${namespace})]ConstMatInt.h"
#include "[(${namespace})]DAGToDAGISel.h"
#include "[(${namespace})]RegisterInfo.h"
#include "[(${namespace})]SubTarget.h"

using namespace llvm;

namespace llvm::[(${namespace})]MatInt {
  InstSeq generateInstSeqImpl(int64_t Val, [(${namespace})]MatInt::InstSeq &Res ) {
    uint64_t uVal = Val;

    [# th:each="cons : ${constantSequences}" ]
    [# th:if="${cons.isSigned == false}" ]
    if(uVal >= [(${cons.lowestValue})] && uVal <= [(${cons.highestValue})]) {
      Res.emplace_back([(${namespace})]::[(${cons.instruction})], uVal);
      return Res;
    }
    [/]
    [# th:if="${cons.isSigned == true}" ]
    if(Val >= [(${cons.lowestValue})] && Val <= [(${cons.highestValue})]) {
      Res.emplace_back([(${namespace})]::[(${cons.instruction})], Val);
      return Res;
    }
    [/]
    [/]

    llvm_unreachable("not supported immediate");
  }

  InstSeq generateInstSeq(int64_t Val) {
    [(${namespace})]MatInt::InstSeq Res;
    generateInstSeqImpl(Val, Res);
    return Res;
  }
}