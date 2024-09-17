#ifndef LLVM_LIB_TARGET_[(${namespace})]_MCTARGETDESC_[(${namespace})]MCCODEEMITTER_H
#define LLVM_LIB_TARGET_[(${namespace})]_MCTARGETDESC_[(${namespace})]MCCODEEMITTER_H

#include "MCTargetDesc/[(${namespace})]FixupKinds.h"
#include "AsmUtils.h"
#include "[(${namespace})]MCExpr.h"
#include "[(${namespace})]MCTargetDesc.h"
#include "[(${namespace})]MCInstExpander.h"
#include "llvm/ADT/SmallVector.h"
#include "llvm/ADT/Statistic.h"
#include "llvm/MC/MCAsmInfo.h"
#include "llvm/MC/MCCodeEmitter.h"
#include "llvm/MC/MCContext.h"
#include "llvm/MC/MCExpr.h"
#include "llvm/MC/MCFixup.h"
#include "llvm/MC/MCInst.h"
#include "llvm/MC/MCInstrInfo.h"
#include "llvm/MC/MCRegisterInfo.h"
#include "llvm/MC/MCSubtargetInfo.h"
#include "llvm/MC/MCSymbol.h"
#include "llvm/TargetParser/SubtargetFeature.h"
#include "llvm/Support/Casting.h"
#include "llvm/Support/Endian.h"
#include "llvm/Support/EndianStream.h"
#include "llvm/Support/ErrorHandling.h"
#include "llvm/Support/raw_ostream.h"
#include <cassert>
#include <cstdint>

namespace llvm
{
    class [(${namespace})]MCCodeEmitter : public MCCodeEmitter
    {
        const MCInstrInfo &MCII;
        MCContext & Ctx;

        [(${namespace})]MCCodeEmitter(const [(${namespace})]MCCodeEmitter &) = delete;
        void operator=(const [(${namespace})]MCCodeEmitter &) = delete;

    public:
        [(${namespace})]MCCodeEmitter(const MCInstrInfo &mcii, MCContext &ctx, bool IsBigEndian);

        ~[(${namespace})]MCCodeEmitter() override = default;

        void encodeInstruction(const MCInst &MI, raw_ostream &OS, SmallVectorImpl<MCFixup> &Fixups, const MCSubtargetInfo &STI) const override;

        void encodeNonPseudoInstruction(const MCInst &MI, raw_ostream &OS, SmallVectorImpl<MCFixup> &Fixups, const MCSubtargetInfo &STI) const;

        // getBinaryCodeForInstr - TableGen'erated function for getting the
        // binary encoding for an instruction.
        uint64_t getBinaryCodeForInstr(const MCInst &MI, SmallVectorImpl<MCFixup> &Fixups, const MCSubtargetInfo &STI) const;

        /// getMachineOpValue - Return binary encoding of operand. If the machine
        /// operand requires relocation, record the relocation and return zero.
        unsigned getMachineOpValue(const MCInst &MI, const MCOperand &MO, SmallVectorImpl<MCFixup> &Fixups, const MCSubtargetInfo &STI) const;

    protected:
        support::endianness EndianEncoding;

    private:
        [(${namespace})]MCInstExpander MCInstExpander;
        mutable unsigned Offset = 0;

        void emitFixups(const MCInst MI, unsigned OpNo, const MCExpr *Expr, SmallVectorImpl<MCFixup> &Fixups) const;

        [# th:each="imm : ${immediates}" ]
        unsigned [(${imm.encodeWrapper})](const MCInst &MI, unsigned OpNo, SmallVectorImpl<MCFixup> &Fixups, const MCSubtargetInfo &STI) const;
        [/]
    };
} // end llvm namespace

#endif