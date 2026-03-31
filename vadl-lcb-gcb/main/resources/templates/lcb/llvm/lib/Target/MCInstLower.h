#ifndef LLVM_LIB_TARGET_[(${namespace})]_[(${namespace})]MCINSTLOWER_H
#define LLVM_LIB_TARGET_[(${namespace})]_[(${namespace})]MCINSTLOWER_H

#include "llvm/CodeGen/MachineOperand.h"
#include "llvm/Support/Compiler.h"
#include "llvm/MC/MCExpr.h"

namespace llvm
{
    class MCInst;
    class MCOperand;
    class MachineInstr;
    class AsmPrinter;

    class [(${namespace})]MCInstLower
    {
        typedef MachineOperand::MachineOperandType MachineOperandType;
        AsmPrinter & Printer;

    public:
        [(${namespace})]MCInstLower(class AsmPrinter & asmprinter);
        void Lower(const MachineInstr *MI, MCInst &OutMI) const;
        MCOperand LowerOperand(const MachineOperand &MO) const;

        const MCExpr *OperandToMCExpr(const MachineOperand &MO) const;
        const MCSymbolRefExpr *SymbolOperandToMCSymbolRefExpr(const MachineOperand &MO) const;

        MCOperand LowerSymbolOperand(const MachineOperand &MO, MachineOperandType MOTy) const;
    };
}

#endif // LLVM_LIB_TARGET_[(${namespace})]_[(${namespace})]MCINSTLOWER_H