#include "[(${namespace})]AsmPrinter.h"
#include "llvm/MC/TargetRegistry.h"
#include "MCTargetDesc/[(${namespace})]MCExpr.h"
#include "MCTargetDesc/[(${namespace})]InstPrinter.h"
#include "MCTargetDesc/[(${namespace})]MCTargetDesc.h"
#include "Utils/ImmediateUtils.h"
#include "MCTargetDesc/AsmUtils.h"
#include "TargetInfo/[(${namespace})]TargetInfo.h"
#include "llvm/Support/raw_ostream.h"
#include "llvm/Support/Debug.h"
#include "llvm/MC/MCInst.h"
#include "llvm/MC/MCInstBuilder.h"
#include "llvm/MC/MCExpr.h"

#define DEBUG_TYPE "[(${namespace})]AsmPrinter"

using namespace llvm;

void [(${namespace})]AsmPrinter::emitToStreamer(MCStreamer &S, const MCInst &Inst)
{
    AsmPrinter::EmitToStreamer(*OutStreamer, Inst);
}

StringRef [(${namespace})]AsmPrinter::getPassName() const
{
    return "[(${namespace})] Assembly Printer";
}

#include "[(${namespace})]GenMCPseudoLowering.inc"

static MCOperand lowerSymbolOperand(const MachineOperand &MO, MCSymbol *Sym,
                            const AsmPrinter &AP) {
    MCContext &Ctx = AP.OutContext;
    const MCExpr *ME =
    MCSymbolRefExpr::create(Sym, MCSymbolRefExpr::VK_None, Ctx);
    return MCOperand::createExpr(ME);
}

bool [(${namespace})]AsmPrinter::lowerOperand(const MachineOperand &MO,
                                             MCOperand &MCOp) const {
    switch (MO.getType()) {
    default:
        report_fatal_error("lowerOperand: unknown operand type");
    case MachineOperand::MO_Register:
        // Ignore all implicit register operands.
        if (MO.isImplicit())
        return false;
        MCOp = MCOperand::createReg(MO.getReg());
        break;
    case MachineOperand::MO_RegisterMask:
        // Regmasks are like implicit defs.
        return false;
    case MachineOperand::MO_Immediate:
        MCOp = MCOperand::createImm(MO.getImm());
        break;
    case MachineOperand::MO_MachineBasicBlock:
        MCOp = lowerSymbolOperand(MO, MO.getMBB()->getSymbol(), *this);
        break;
    case MachineOperand::MO_GlobalAddress:
        MCOp = lowerSymbolOperand(MO, getSymbolPreferLocal(*MO.getGlobal()), *this);
        break;
    case MachineOperand::MO_BlockAddress:
        MCOp = lowerSymbolOperand(MO, GetBlockAddressSymbol(MO.getBlockAddress()),
                                *this);
        break;
    case MachineOperand::MO_ExternalSymbol:
        MCOp = lowerSymbolOperand(MO, GetExternalSymbolSymbol(MO.getSymbolName()),
                                *this);
        break;
    case MachineOperand::MO_ConstantPoolIndex:
        MCOp = lowerSymbolOperand(MO, GetCPISymbol(MO.getIndex()), *this);
        break;
    case MachineOperand::MO_JumpTableIndex:
        MCOp = lowerSymbolOperand(MO, GetJTISymbol(MO.getIndex()), *this);
        break;
    case MachineOperand::MO_MCSymbol:
        MCOp = lowerSymbolOperand(MO, MO.getMCSymbol(), *this);
        break;
    }
    return true;
}

void [(${namespace})]AsmPrinter::emitInstruction( const MachineInstr *MI )
{
    // this is triggered if 'PseudoExpansion' is used in tablegen
    if (emitPseudoExpansionLowering(*OutStreamer, MI))
    {
        return;
    }

    MCInst TmpInst;
    MCInstLowering.Lower(MI, TmpInst);

    if( MCInstExpander.isExpandableForAssembly( TmpInst ) )
    {
        MCInstExpander.expand( TmpInst, [&](const MCInst &Inst) {
          emitToStreamer(*OutStreamer, Inst);
        },
        [&](MCSymbol* Symbol) {
          OutStreamer->emitLabel(Symbol);
        });
    }
    else
    {
        emitToStreamer( *OutStreamer, TmpInst );
    }
}

extern "C" LLVM_EXTERNAL_VISIBILITY void LLVMInitialize[(${namespace})]AsmPrinter()
{
    RegisterAsmPrinter<[(${namespace})]AsmPrinter> X(getThe[(${namespace})]Target());
}

void [(${namespace})]AsmPrinter::emitStartOfAsmFile( Module& module )
{
    const char* vadlAsmVersion = std::getenv( "VADL_ASM_VERSION" );
    if( vadlAsmVersion )
    {
        OutStreamer->emitRawComment( "", false );
        OutStreamer->emitRawComment( "   Vienna Architecture Description Language (VADL)", false );
        OutStreamer->emitRawComment( "", false );
    }

    AsmPrinter::emitStartOfAsmFile( module );
}