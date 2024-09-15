#include "AsmParsedOperand.h"
#include "llvm/Support/Casting.h"
#include "llvm/Support/raw_ostream.h"

#define DEBUG_TYPE "[(${namespace})]-parsed-operand"

using namespace llvm;

namespace llvm {
    bool [(${namespace})]ParsedOperand::isToken() const { return Kind == k_Token; }
    bool [(${namespace})]ParsedOperand::isReg() const { return Kind == k_Register; }
    bool [(${namespace})]ParsedOperand::isImm() const { return Kind == k_Immediate; }
    bool [(${namespace})]ParsedOperand::isMem() const { return false; } // currently not supported

    bool [(${namespace})]ParsedOperand::isIntReg() const {
      return (Kind == k_Register && Reg.Kind == rk_IntReg);
    }

    StringRef [(${namespace})]ParsedOperand::getToken() const {
      assert(Kind == k_Token && "Invalid access!");
      return StringRef(Tok.Data, Tok.Length);
    }

    unsigned [(${namespace})]ParsedOperand::getReg() const {
      assert((Kind == k_Register) && "Invalid access!");
      return Reg.RegNum;
    }

    const MCExpr *[(${namespace})]ParsedOperand::getImm() const {
      assert((Kind == k_Immediate) && "Invalid access!");
      return Imm.Val;
    }

    SMLoc [(${namespace})]ParsedOperand::getStartLoc() const {
      return StartLoc;
    }

    SMLoc [(${namespace})]ParsedOperand::getEndLoc() const {
      return EndLoc;
    }

    StringRef [(${namespace})]ParsedOperand::getTarget() const {
      return Target;
    }

    void [(${namespace})]ParsedOperand::setTarget(StringRef Target) {
      this->Target = Target;
    }

    void [(${namespace})]ParsedOperand::print(raw_ostream &OS) const {
      switch (Kind) {
      case k_Token:     OS << "Token: " << getToken() << "\n"; break;
      case k_Register:  OS << "Reg: #" << getReg() << "\n"; break;
      case k_Immediate: OS << "Imm: " << *getImm() << "\n"; break;
      }
    }

    void [(${namespace})]ParsedOperand::addOperand(MCInst &Inst) const {
      if(isReg()) {
        Inst.addOperand(MCOperand::createReg(getReg()));
      } else if(isImm()) {
        const MCExpr *Expr = getImm();
        addExpr(Inst, Expr);
      } else {
        llvm_unreachable("Unknown operand type.");
      }
    }

    void [(${namespace})]ParsedOperand::addExpr(MCInst &Inst, const MCExpr *Expr) const{
      // Add as immediate when possible.  Null MCExpr = 0.
      if (!Expr)
        Inst.addOperand(MCOperand::createImm(0));
      else if (const MCConstantExpr *CE = dyn_cast<MCConstantExpr>(Expr))
        Inst.addOperand(MCOperand::createImm(CE->getValue()));
      else
        Inst.addOperand(MCOperand::createExpr(Expr));
    }
}