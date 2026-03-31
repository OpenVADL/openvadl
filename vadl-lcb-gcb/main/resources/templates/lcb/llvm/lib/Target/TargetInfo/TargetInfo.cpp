#include "TargetInfo/[(${namespace})]TargetInfo.h"
#include "llvm/MC/TargetRegistry.h"

using namespace llvm;

Target &llvm::getThe[(${namespace})]Target()
{
  static Target The[(${namespace})]Target;
  return The[(${namespace})]Target;
}

extern "C" void LLVMInitialize[(${namespace})]TargetInfo()
{
  RegisterTarget<Triple::[(${namespace})], /*HasJIT=*/false> X(getThe[(${namespace})]Target(), "[(${namespace})]", "Custom vadl processor", "[(${namespace})]");
}