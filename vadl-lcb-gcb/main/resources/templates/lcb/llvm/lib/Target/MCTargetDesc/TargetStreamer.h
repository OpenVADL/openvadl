#ifndef LLVM_LIB_TARGET_[(${namespace})]_MCTARGETDESC_[(${namespace})]STREAM_H
#define LLVM_LIB_TARGET_[(${namespace})]_MCTARGETDESC_[(${namespace})]STREAM_H

#include "llvm/MC/MCStreamer.h"

namespace llvm
{
    class [(${namespace})]TargetStreamer : public MCTargetStreamer
    {
    public:
        [(${namespace})]TargetStreamer(MCStreamer & S);

        virtual void emitDirectiveOptionPush() = 0;
        virtual void emitDirectiveOptionPop() = 0;
        virtual void emitDirectiveOptionRVC() = 0;
        virtual void emitDirectiveOptionNoRVC() = 0;
        virtual void emitDirectiveOptionRelax() = 0;
        virtual void emitDirectiveOptionNoRelax() = 0;
    };
}

#endif