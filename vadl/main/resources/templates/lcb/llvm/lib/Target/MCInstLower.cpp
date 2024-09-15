#include "Utils/[(${namespace})]BaseInfo.h"
#include "[(${namespace})]MCInstLower.h"
#include "MCTargetDesc/[(${namespace})]MCExpr.h"
#include "llvm/CodeGen/AsmPrinter.h"
#include "llvm/CodeGen/MachineFunction.h"
#include "llvm/CodeGen/MachineInstr.h"
#include "llvm/CodeGen/MachineOperand.h"
#include "llvm/IR/Mangler.h"
#include "llvm/MC/MCContext.h"
#include "llvm/MC/MCInst.h"

#define DEBUG_TYPE "[(${namespace})]MCInstLower"

using namespace llvm;

[(${namespace})]MCInstLower::[(${namespace})]MCInstLower(class AsmPrinter &asmprinter)
    : Printer(asmprinter) {}

const MCExpr *[(${namespace})]MCInstLower::OperandToMCExpr(const MachineOperand &MO) const
{
    /* NOTE: registers are not supported for this action as it should not be needed */

    MCContext &Ctx = Printer.OutContext;
    MachineOperandType MOTy = MO.getType();

    switch (MOTy)
    {
    case MachineOperand::MO_MachineBasicBlock:
    case MachineOperand::MO_GlobalAddress:
    case MachineOperand::MO_BlockAddress:
    case MachineOperand::MO_ExternalSymbol:
    case MachineOperand::MO_JumpTableIndex:
    case MachineOperand::MO_ConstantPoolIndex:
        return SymbolOperandToMCSymbolRefExpr(MO);
    case MachineOperand::MO_Immediate:
        return MCConstantExpr::create(MO.getImm(), Ctx, false /* = print as hex */);
    default:
        llvm_unreachable("<unsupported operand type>");
    }
}

const MCSymbolRefExpr *[(${namespace})]MCInstLower::SymbolOperandToMCSymbolRefExpr(const MachineOperand &MO) const
{
    MCContext &Ctx = Printer.OutContext;
    const MCSymbol *Symbol;
    MachineOperandType MOTy = MO.getType();

    switch (MOTy)
    {
    case MachineOperand::MO_MachineBasicBlock:
        Symbol = MO.getMBB()->getSymbol();
        break;
    case MachineOperand::MO_GlobalAddress:
        Symbol = Printer.getSymbol(MO.getGlobal());
        break;
    case MachineOperand::MO_BlockAddress:
        Symbol = Printer.GetBlockAddressSymbol(MO.getBlockAddress());
        break;
    case MachineOperand::MO_ExternalSymbol:
        Symbol = Printer.GetExternalSymbolSymbol(MO.getSymbolName());
        break;
    case MachineOperand::MO_JumpTableIndex:
        Symbol = Printer.GetJTISymbol(MO.getIndex());
        break;
    case MachineOperand::MO_ConstantPoolIndex:
        Symbol = Printer.GetCPISymbol(MO.getIndex());
        break;
    default:
        llvm_unreachable("<unsupported operand type>");
    }

    return MCSymbolRefExpr::create(Symbol, MCSymbolRefExpr::VK_None, Ctx);
}

MCOperand [(${namespace})]MCInstLower::LowerSymbolOperand(const MachineOperand &MO, MachineOperandType MOTy) const
{
    MCContext &Ctx = Printer.OutContext;

    [(${namespace})]MCExpr::VariantKind Kind;
    switch (MO.getTargetFlags())
    {
    case [(${namespace})]BaseInfo::MO_None:
        Kind = [(${namespace})]MCExpr::VariantKind::VK_[(${namespace})]_None;
        break;
        /*
    «FOR relocation : relocations» case [(${namespace})]BaseInfo::«relocation.moAnnotationIdentifier»:
        Kind = [(${namespace})]MCExpr::VariantKind::«relocation.variantKindIdentifier»;
        break;
        «ENDFOR» */
    default : llvm_unreachable("<unsupported target flag on operand>");
    }

    const MCExpr *expr = SymbolOperandToMCSymbolRefExpr(MO);

    if (MO.isJTI() == false && MO.isMBB() == false && MO.getOffset() != 0)
    {
        unsigned Offset = MO.getOffset();
        assert(Offset > 0);
        const MCConstantExpr *OffsetExpr = MCConstantExpr::create(Offset, Ctx);
        expr = MCBinaryExpr::createAdd(expr, OffsetExpr, Ctx);
    }

    if (Kind != [(${namespace})]MCExpr::VariantKind::VK_[(${namespace})]_None)
    {
        expr = [(${namespace})]MCExpr::create(expr, Kind, Ctx);
    }

    return MCOperand::createExpr(expr);
}

MCOperand [(${namespace})]MCInstLower::LowerOperand(const MachineOperand &MO) const
{
    MachineOperandType MOTy = MO.getType();
    switch (MOTy)
    {
    default:
        llvm_unreachable("<unsupported operand type>");
    case MachineOperand::MO_Register:
        // Ignore all implicit register operands.
        if (MO.isImplicit())
        {
            break;
        }
        // TODO: @chochrainer deal with sub register flags!!!
        return MCOperand::createReg(MO.getReg());
    case MachineOperand::MO_Immediate:
        // TODO: @chochrainer assert that there are no flags set!!!
        return MCOperand::createImm(MO.getImm());
    case MachineOperand::MO_MachineBasicBlock:
    case MachineOperand::MO_GlobalAddress:
    case MachineOperand::MO_ExternalSymbol:
    case MachineOperand::MO_JumpTableIndex:
    case MachineOperand::MO_ConstantPoolIndex:
    case MachineOperand::MO_BlockAddress:
        return LowerSymbolOperand(MO, MOTy);
    case MachineOperand::MO_RegisterMask:
        break;
    }
    return MCOperand();
}

void [(${namespace})]MCInstLower::Lower(const MachineInstr *MI, MCInst &OutMI) const
{
    OutMI.setOpcode(MI->getOpcode());
    for (auto &MO : MI->operands())
    {
        const MCOperand MCOp = LowerOperand(MO);
        if (MCOp.isValid())
        {
            OutMI.addOperand(MCOp);
        }
    }
}