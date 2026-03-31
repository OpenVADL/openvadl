#include "[(${namespace})]MCAsmInfo.h"
#include "llvm/TargetParser/Triple.h"

using namespace llvm;

void [(${namespace})]MCAsmInfo::anchor() {}

[(${namespace})]MCAsmInfo::[(${namespace})]MCAsmInfo(const Triple &TheTriple)
{
    CommentString = "[(${assemblyDescription.commentString})]";
    AlignmentIsInBytes = [(${assemblyDescription.alignmentInBytes})];
}