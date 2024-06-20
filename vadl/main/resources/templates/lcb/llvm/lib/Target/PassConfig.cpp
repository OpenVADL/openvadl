#include "[(${namespace})].h"
#include "[(${namespace})]PassConfig.h"
#include "llvm/Support/Debug.h"

#define DEBUG_TYPE "[(${namespace})]PassConfig"

using namespace llvm;

bool [(${namespace})]PassConfig::addInstSelector()
{
    auto iSelDagPass = create[(${namespace})]ISelDag(get[(${namespace})]TargetMachine(), getOptLevel());
    addPass(iSelDagPass);
    return false;
}

void [(${namespace})]PassConfig::addPreRegAlloc()
{
    auto expandPseudoPass = create[(${namespace})]ExpandPseudoPass();
    addPass(expandPseudoPass);
}