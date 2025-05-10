#ifndef LLVM_LIB_TARGET_[(${namespace})]_MCTARGETDESC_[(${namespace})]ELFOBJECTWRITER_H
#define LLVM_LIB_TARGET_[(${namespace})]_MCTARGETDESC_[(${namespace})]ELFOBJECTWRITER_H

#include "MCTargetDesc/[(${namespace})]FixupKinds.h"
#include "MCTargetDesc/[(${namespace})]MCExpr.h"
#include "MCTargetDesc/[(${namespace})]MCTargetDesc.h"
#include "llvm/MC/MCContext.h"
#include "llvm/MC/MCELFObjectWriter.h"
#include "llvm/MC/MCFixup.h"
#include "llvm/MC/MCObjectWriter.h"
#include "llvm/Support/ErrorHandling.h"
#include "llvm/MC/MCValue.h"

namespace llvm
{
    class [(${namespace})]ELFObjectWriter : public MCELFObjectTargetWriter
    {
    public:
        [(${namespace})]ELFObjectWriter(uint8_t OSABI, bool Is64Bit);

        ~[(${namespace})]ELFObjectWriter() override;

        // Return true if the given relocation must be with a symbol rather than
        // section plus offset.
        bool needsRelocateWithSymbol(const MCValue &Val, const MCSymbol &Sym, unsigned Type) const override;

    protected:
        unsigned getRelocType(MCContext & Ctx, const MCValue &Target, const MCFixup &Fixup, bool IsPCRel) const override;
    };
};

#endif