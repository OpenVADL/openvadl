#ifndef LLVM_LIB_TARGET_[(${namespace})]_MCTARGETDESC_[(${namespace})]MCTARGETDESC_H
#define LLVM_LIB_TARGET_[(${namespace})]_MCTARGETDESC_[(${namespace})]MCTARGETDESC_H

#include <cstdint>

namespace llvm
{
    class MCInstrInfo;
    class MCSubtargetInfo;
}

#define GET_REGINFO_ENUM
#include "[(${namespace})]GenRegisterInfo.inc"

#define GET_SUBTARGETINFO_ENUM
#include "[(${namespace})]GenSubtargetInfo.inc"

#define GET_INSTRINFO_ENUM
#include "[(${namespace})]GenInstrInfo.inc"

#endif