#ifndef LLVM_LIB_TARGET_[(${namespace})]_MCTARGETDESC_[(${namespace})]_ASM_STREAM_H
#define LLVM_LIB_TARGET_[(${namespace})]_MCTARGETDESC_[(${namespace})]_ASM_STREAM_H

#include "[(${namespace})]TargetStreamer.h"
#include "llvm/MC/MCStreamer.h"
#include "llvm/MC/MCSubtargetInfo.h"

namespace llvm
{
    class formatted_raw_ostream;

    // This part is for ascii assembly output
    class [(${namespace})]AsmStreamer : public [(${namespace})]TargetStreamer
    {
        formatted_raw_ostream & OS;

    public:
        [(${namespace})]AsmStreamer(MCStreamer & S, formatted_raw_ostream & OS);

        void emitDirectiveOptionPush() override;
        void emitDirectiveOptionPop() override;
        void emitDirectiveOptionRVC() override;
        void emitDirectiveOptionNoRVC() override;
        void emitDirectiveOptionRelax() override;
        void emitDirectiveOptionNoRelax() override;
    };
}

#endif