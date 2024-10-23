#include "[(${namespace})]ELFObjectWriter.h"
#include <iostream>

#include "llvm/Support/Debug.h"
#define DEBUG_TYPE "elf-obj-writer"

using namespace llvm;

[(${namespace})]ELFObjectWriter::[(${namespace})]ELFObjectWriter(uint8_t OSABI, bool Is64Bit) : MCELFObjectTargetWriter(Is64Bit, OSABI, ELF::EM_[(${namespace})], /*HasRelocationAddend*/ true)
{
}

[(${namespace})]ELFObjectWriter::~[(${namespace})]ELFObjectWriter() {}

bool [(${namespace})]ELFObjectWriter::needsRelocateWithSymbol(const MCSymbol &Sym, unsigned Type) const
{
    // TODO: @chochrainer FIXME
    // TODO: this is very conservative, update once RISC-V psABI requirements
    //       are clarified.
    return true;
}

unsigned [(${namespace})]ELFObjectWriter::getRelocType(MCContext &Ctx, const MCValue &Target, const MCFixup &Fixup, bool IsPCRel) const
{
    // TODO: @chochrainer FIXME: deal with PCRel and builtin fixups (see MCFixupKind enum)

    // const MCExpr *Expr = Fixup.getValue(); // <-- maybe usefull in the future

    // Determine the type of the relocation
    unsigned Kind = Fixup.getTargetKind();
    switch (Kind)
    {
    default:
        Ctx.reportError(Fixup.getLoc(), "Unsupported relocation type");
        return ELF::R_[(${namespace})]_NONE;
    case FK_Data_4:
        return ELF::R_[(${namespace})]_32;
    case FK_Data_8:
        return ELF::R_[(${namespace})]_64;
    [# th:each="fx : ${fixups}" ]
    case [(${namespace})]::[(${fx.name().value()})]:
        return ELF::[(${fx.relocationLowerable().elfRelocationName().value()})];
    [/]
    }

    return 0;
}
