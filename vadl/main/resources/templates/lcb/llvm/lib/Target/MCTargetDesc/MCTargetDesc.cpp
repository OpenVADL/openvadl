#include "[(${namespace})]InstPrinter.h"
#include "[(${namespace})]MCTargetDesc.h"
#include "[(${namespace})]ELFStreamer.h"
#include "[(${namespace})]AsmStreamer.h"
#include "[(${namespace})]MCCodeEmitter.h"
#include "[(${namespace})]AsmBackend.h"
#include "Disassembler/[(${namespace})]Disassembler.h"
#include "TargetInfo/[(${namespace})]TargetInfo.h"
#include "[(${namespace})]MCAsmInfo.h"
#include "llvm/MC/MCInstrInfo.h"
#include "llvm/MC/MCSubtargetInfo.h"
#include "llvm/MC/TargetRegistry.h"
#include "llvm/Support/Debug.h"
#include <string>
#include <iostream>

#define DEBUG_TYPE "MCTargetDesc"

using namespace llvm;

#define GET_INSTRINFO_MC_DESC
#include "[(${namespace})]GenInstrInfo.inc"

#define GET_REGINFO_MC_DESC
#include "[(${namespace})]GenRegisterInfo.inc"

#define GET_SUBTARGETINFO_MC_DESC
#include "[(${namespace})]GenSubtargetInfo.inc"

static MCAsmInfo *create[(${namespace})]MCAsmInfo(const MCRegisterInfo &MRI, const Triple &TT, const MCTargetOptions &Options)
{
    return new [(${namespace})]MCAsmInfo(TT);
}

static MCSubtargetInfo *create[(${namespace})]MCSubtargetInfo(const Triple &TT, StringRef CPU, StringRef FS)
{
    std::string CPUName = std::string(CPU);
    if (CPUName.empty())
    {
        CPUName = "generic-[(${namespace})]";
    }
    return create[(${namespace})]MCSubtargetInfoImpl(TT, CPUName, CPUName, FS);
}

static MCInstrInfo *create[(${namespace})]MCInstrInfo()
{
    MCInstrInfo *X = new MCInstrInfo();
    Init[(${namespace})]MCInstrInfo(X);
    return X;
}

static MCRegisterInfo *create[(${namespace})]MCRegisterInfo(const Triple & /*TT*/)
{
    MCRegisterInfo *X = new MCRegisterInfo();
    Init[(${namespace})]MCRegisterInfo(X, «emit(returnAddressRegister)» /* = return address register */);
    return X;
}

static MCInstPrinter *create[(${namespace})]InstPrinter(const Triple &T, unsigned SyntaxVariant, const MCAsmInfo &MAI, const MCInstrInfo &MII, const MCRegisterInfo &MRI)
{
    return new [(${namespace})]InstPrinter(MAI, MII, MRI);
}

static MCTargetStreamer *create[(${namespace})]ObjectTargetStreamer(MCStreamer &S, const MCSubtargetInfo &STI)
{
    // NOTE: only supports elf object format
    const Triple &TT = STI.getTargetTriple();
    if (TT.isOSBinFormatELF()) // check for elf bin support
    {
        return new [(${namespace})]ELFStreamer(S, STI);
    }
    return nullptr; // defaults to no registering
}

static MCAsmBackend *create[(${namespace})]AsmBackend(const Target &T, const MCSubtargetInfo &STI, const MCRegisterInfo &MRI, const MCTargetOptions &Options)
{
    // NOTE: only supports elf object format
    const Triple &TT = STI.getTargetTriple();
    if (TT.isOSBinFormatELF()) // check for elf bin support
    {
        return new [(${namespace})]ELFAsmBackend(T, TT.getOS(), [(${namespace})]BaseInfo::IsBigEndian());
    }
    return nullptr; // defaults to no registering
}

static MCCodeEmitter *create[(${namespace})]MCCodeEmitter(const MCInstrInfo &MCII, MCContext &Ctx)
{
    return new [(${namespace})]MCCodeEmitter(MCII, Ctx, [(${namespace})]BaseInfo::IsBigEndian());
}

static MCTargetStreamer *create[(${namespace})]AsmTargetStreamer(MCStreamer &S, formatted_raw_ostream &OS, MCInstPrinter *InstPrint, bool isVerboseAsm)
{
    return new [(${namespace})]AsmStreamer(S, OS);
}

extern "C" void LLVMInitialize[(${namespace})]TargetMC()
{
    // Register the MC asm info.
    TargetRegistry::RegisterMCAsmInfo(getThe[(${namespace})]Target(), create[(${namespace})]MCAsmInfo);

    // Register the MC instruction info.
    TargetRegistry::RegisterMCInstrInfo(getThe[(${namespace})]Target(), create[(${namespace})]MCInstrInfo);

    // Register the MC register info.
    TargetRegistry::RegisterMCRegInfo(getThe[(${namespace})]Target(), create[(${namespace})]MCRegisterInfo);

    // Register the Target Backend for ELF Support
    TargetRegistry::RegisterMCAsmBackend(getThe[(${namespace})]Target(), create[(${namespace})]AsmBackend);

    // Register the Target MCCode emitter
    TargetRegistry::RegisterMCCodeEmitter(getThe[(${namespace})]Target(), create[(${namespace})]MCCodeEmitter);

    // Register the MCInstPrinter
    TargetRegistry::RegisterMCInstPrinter(getThe[(${namespace})]Target(), create[(${namespace})]InstPrinter);

    // Register the MC subtarget info.
    TargetRegistry::RegisterMCSubtargetInfo(getThe[(${namespace})]Target(), create[(${namespace})]MCSubtargetInfo);

    // Register the Object Target Streamer for ELF Support
    TargetRegistry::RegisterObjectTargetStreamer(getThe[(${namespace})]Target(), create[(${namespace})]ObjectTargetStreamer);

    // Register the Asm Target Streamer for asm ascii output
    TargetRegistry::RegisterAsmTargetStreamer(getThe[(${namespace})]Target(), create[(${namespace})]AsmTargetStreamer);
}