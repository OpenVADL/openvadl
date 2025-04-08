#include "[(${namespace})]MCCodeEmitter.h"

#include "MCTargetDesc/[(${namespace})]FixupKinds.h"
#include "[(${namespace})]MCExpr.h"
#include "[(${namespace})]MCTargetDesc.h"
#include "llvm/ADT/SmallVector.h"
#include "llvm/ADT/Statistic.h"
#include "llvm/MC/MCAsmInfo.h"
#include "llvm/MC/MCCodeEmitter.h"
#include "llvm/MC/MCContext.h"
#include "llvm/MC/MCExpr.h"
#include "llvm/MC/MCFixup.h"
#include "llvm/MC/MCInst.h"
#include "llvm/MC/MCInstBuilder.h"
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
#include <iostream>
#include <vector>
#include "Utils/ImmediateUtils.h"

#include "llvm/Support/Debug.h"
#define DEBUG_TYPE "mc-code-emitter"

using namespace llvm;

[(${namespace})]MCCodeEmitter::[(${namespace})]MCCodeEmitter(const MCInstrInfo &mcii, MCContext &ctx, bool IsBigEndian) : MCII(mcii), Ctx(ctx), EndianEncoding(IsBigEndian ? support::big : support::little), MCInstExpander(ctx)
{
}

MCFixupKind getFixupKind(unsigned Opcode, unsigned OpNo, const MCExpr* Expr) {
  MCExpr::ExprKind kind = Expr->getKind();
   if(kind == MCExpr::ExprKind::Target) {
      const [(${namespace})]MCExpr* targetExpr = cast<[(${namespace})]MCExpr>(Expr);
      [(${namespace})]MCExpr::VariantKind targetKind = targetExpr->getKind();

      switch(targetKind) {
          // TODO: case for each relocation variant kind, if statement for each inst and for each immediate operand
          default:
          {
              // This is an immediate variant kind. Emit fixup for sub expression.
              return getFixupKind(Opcode, OpNo, targetExpr->getSubExpr());
              break;
          }
      }

   } else if (kind == MCExpr::ExprKind::SymbolRef) {
      switch(Opcode)
      {
        [# th:each="symbolRefFixup : ${symbolRefFixups}" ]
        case([(${namespace})]::[(${symbolRefFixup.instruction})]):
            [# th:each="operand : ${symbolRefFixup.immediateOperands}" ]
            if (OpNo == [(${operand.opIndex})]) {
                return MCFixupKind([(${namespace})]::[(${operand.fixup})]);
            }
            [/]
            assert(false && "No immediate operand found.");
            return MCFixupKind::FK_NONE;
        [/]
      }
   } else if (kind == MCExpr::ExprKind::Binary) {
        const MCBinaryExpr *Bin = static_cast<const MCBinaryExpr *>(Expr);
        MCFixupKind lhs = getFixupKind(Opcode, OpNo, Bin->getLHS());
        MCFixupKind rhs = getFixupKind(Opcode, OpNo, Bin->getRHS());
        if(lhs == MCFixupKind::FK_NONE) {
            return rhs;
        } else if(rhs == MCFixupKind::FK_NONE) {
            return lhs;
        } else if(lhs == rhs) {
            return lhs;
        } else {
            report_fatal_error("Binary expression is too complex.");
        }
   }
   return MCFixupKind::FK_NONE;
}

void [(${namespace})]MCCodeEmitter::emitFixups
    (const MCInst MI
    , unsigned OpNo
    , const MCExpr *Expr
    , SmallVectorImpl<MCFixup> &Fixups) const
{
    MCFixupKind fixupKind = getFixupKind(MI.getOpcode(), OpNo, Expr);
    if(fixupKind != MCFixupKind::FK_NONE) {
        Fixups.push_back(
            MCFixup::create(Offset, Expr, fixupKind, MI.getLoc()));
    }
}

[# th:each="imm : ${immediates}" ]
unsigned [(${namespace})]MCCodeEmitter::[(${imm.encodeWrapper})](const MCInst &MI, unsigned OpNo, SmallVectorImpl<MCFixup> &Fixups, const MCSubtargetInfo &STI) const
{
    const MCOperand &MO = MI.getOperand(OpNo);

    if (MO.isImm())
        return [(${imm.encode})](MO.getImm());

    int64_t imm;
    if (AsmUtils::evaluateConstantImm(&MO, imm))
        return [(${imm.encode})](imm);

    assert(MO.isExpr() && "[(${imm.encodeWrapper})] expects only expressions or immediates");

    emitFixups(MI, OpNo, MO.getExpr(), Fixups);

    return 0;
}
[/]

void [(${namespace})]MCCodeEmitter::encodeInstruction(const MCInst &MCI, raw_ostream &OS, SmallVectorImpl<MCFixup> &Fixups, const MCSubtargetInfo &STI) const
{
    Offset = 0;
    std::vector<MCInst> resultVec;

    if (MCInstExpander.isExpandable(MCI))
    {
        MCInstExpander.expand(MCI, resultVec);
    }
    else
    {
        resultVec.push_back(MCI);
    }

    for (auto it = std::begin(resultVec); it != std::end(resultVec); ++it)
    {
        MCInst MI = *it;
        encodeNonPseudoInstruction(MI, OS, Fixups, STI);
        const MCInstrDesc &Desc = MCII.get(MI.getOpcode());
        unsigned Size = Desc.getSize();
        Offset += Size;
    }
}

void [(${namespace})]MCCodeEmitter::encodeNonPseudoInstruction(const MCInst &MI, raw_ostream &OS, SmallVectorImpl<MCFixup> &Fixups, const MCSubtargetInfo &STI) const
{
    // Get byte count of instruction.
    const MCInstrDesc &Desc = MCII.get(MI.getOpcode());
    unsigned Size = Desc.getSize();

    switch (Size)
    {
    case 1:
    {
        uint8_t Bits = getBinaryCodeForInstr(MI, Fixups, STI);
        support::endian::write<uint8_t>(OS, Bits, EndianEncoding);
        break;
    }
    case 2:
    {
        uint16_t Bits = getBinaryCodeForInstr(MI, Fixups, STI);
        support::endian::write<uint16_t>(OS, Bits, EndianEncoding);
        break;
    }
    case 4:
    {
        uint32_t Bits = getBinaryCodeForInstr(MI, Fixups, STI);
        support::endian::write<uint32_t>(OS, Bits, EndianEncoding);
        break;
    }
    case 8:
    {
        uint64_t Bits = getBinaryCodeForInstr(MI, Fixups, STI);
        support::endian::write<uint64_t>(OS, Bits, EndianEncoding);
        break;
    }
    default:
    {
        llvm_unreachable("encodeInstruction() unimplemented byte size");
    }
    }
}

unsigned [(${namespace})]MCCodeEmitter::getMachineOpValue(const MCInst &MI, const MCOperand &MO, SmallVectorImpl<MCFixup> &Fixups, const MCSubtargetInfo &STI) const
{
    // TODO: @chochrainer FIXME: emit all expressions

    if (MO.isReg())
        return Ctx.getRegisterInfo()->getEncodingValue(MO.getReg());

    if (MO.isImm())
        return MO.getImm();

    // expressions should have been handled with separate method and tablegen registering
    llvm_unreachable("Unhandled expression!");
    return 0;
}

#include "[(${namespace})]GenMCCodeEmitter.inc"