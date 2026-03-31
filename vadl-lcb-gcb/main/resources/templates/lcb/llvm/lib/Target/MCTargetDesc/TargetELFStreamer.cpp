#include "[(${namespace})]ELFStreamer.h"
#include "llvm/BinaryFormat/ELF.h"
#include "llvm/MC/MCSubtargetInfo.h"
#include <iostream>

#include "llvm/Support/Debug.h"
#define DEBUG_TYPE "asm-backend"

using namespace llvm;

// This part is for ELF object output.
[(${namespace})]ELFStreamer::[(${namespace})]ELFStreamer(MCStreamer &S, const MCSubtargetInfo &STI) : [(${namespace})]TargetStreamer(S)
{
    // TODO: @chochrainer set ELF flags for ELF header
}

MCELFStreamer &[(${namespace})]ELFStreamer::getStreamer()
{
    return static_cast<MCELFStreamer &>(Streamer);
}

void [(${namespace})]ELFStreamer::emitDirectiveOptionPush() {}
void [(${namespace})]ELFStreamer::emitDirectiveOptionPop() {}
void [(${namespace})]ELFStreamer::emitDirectiveOptionRVC() {}
void [(${namespace})]ELFStreamer::emitDirectiveOptionNoRVC() {}
void [(${namespace})]ELFStreamer::emitDirectiveOptionRelax() {}
void [(${namespace})]ELFStreamer::emitDirectiveOptionNoRelax() {}