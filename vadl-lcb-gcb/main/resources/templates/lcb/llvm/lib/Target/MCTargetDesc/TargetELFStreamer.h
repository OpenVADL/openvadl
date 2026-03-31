#ifndef LLVM_LIB_TARGET_[(${namespace})]_MCTARGETDESC_[(${namespace})]ELFSTREAM_H
#define LLVM_LIB_TARGET_[(${namespace})]_MCTARGETDESC_[(${namespace})]ELFSTREAM_H

#include "llvm/MC/MCELFStreamer.h"
#include "[(${namespace})]TargetStreamer.h"

namespace llvm
{
    class [(${namespace})]ELFStreamer : public [(${namespace})]TargetStreamer
    {
    public:
        MCELFStreamer &getStreamer();
        [(${namespace})]ELFStreamer(MCStreamer & S, const MCSubtargetInfo &STI);

        virtual void emitDirectiveOptionPush();
        virtual void emitDirectiveOptionPop();
        virtual void emitDirectiveOptionRVC();
        virtual void emitDirectiveOptionNoRVC();
        virtual void emitDirectiveOptionRelax();
        virtual void emitDirectiveOptionNoRelax();
    };
}

#endif