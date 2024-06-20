#ifndef LLVM_LIB_TARGET_[(${namespace})]_[(${namespace})]ParsedOperand_H
#define LLVM_LIB_TARGET_[(${namespace})]_[(${namespace})]ParsedOperand_H

#include "llvm/MC/MCExpr.h"
#include "llvm/MC/MCInst.h"
#include "llvm/MC/MCParser/MCAsmLexer.h"
#include "llvm/MC/MCParser/MCParsedAsmOperand.h"

using namespace llvm;

namespace llvm {

class [(${namespace})]ParsedOperand : public MCParsedAsmOperand {
public:
  enum RegisterKind {
    rk_None,
    rk_IntReg,
  };

private:
  enum KindTy {
    k_Token,
    k_Register,
    k_Immediate,
  } Kind;

  SMLoc StartLoc, EndLoc;
  StringRef Target;

  struct Token {
    const char *Data;
    unsigned Length;
  };

  struct RegOp {
    unsigned RegNum;
    RegisterKind Kind;
  };

  struct ImmOp {
    const MCExpr *Val;
  };

  struct MemOp {
    unsigned Base;
    unsigned OffsetReg;
    const MCExpr *Off;
  };

  union {
    struct Token Tok;
    struct RegOp Reg;
    struct ImmOp Imm;
    struct MemOp Mem;
  };

public:
  [(${namespace})]ParsedOperand(KindTy K) : MCParsedAsmOperand(), Kind(K) {}

  bool isToken() const override;
  bool isReg() const override;
  bool isImm() const override;
  bool isMem() const override;
  bool isIntReg() const;
  StringRef getToken() const;
  unsigned getReg() const override;
  const MCExpr *getImm() const;
  SMLoc getStartLoc() const override;
  SMLoc getEndLoc() const override;
  StringRef getTarget() const;
  void setTarget(StringRef Target);
  void print(raw_ostream &OS) const override;
  void addOperand(MCInst &Inst) const;
  void addExpr(MCInst &Inst, const MCExpr *Expr) const;

  static [(${namespace})]ParsedOperand CreateToken(StringRef Str, SMLoc S, SMLoc E) {
    auto Op = [(${namespace})]ParsedOperand(k_Token);
    Op.Tok.Data = Str.data();
    Op.Tok.Length = Str.size();
    Op.StartLoc = S;
    Op.EndLoc = E;
    return Op;
  }

  static [(${namespace})]ParsedOperand CreateReg(unsigned RegNum, unsigned Kind, SMLoc S, SMLoc E) {
    auto Op = [(${namespace})]ParsedOperand(k_Register);
    Op.Reg.RegNum = RegNum;
    Op.Reg.Kind   = ([(${namespace})]ParsedOperand::RegisterKind)Kind;
    Op.StartLoc = S;
    Op.EndLoc = E;
    return Op;
  }

  static [(${namespace})]ParsedOperand CreateImm(const MCExpr *Val, SMLoc S, SMLoc E) {
    auto Op = [(${namespace})]ParsedOperand(k_Immediate);
    Op.Imm.Val = Val;
    Op.StartLoc = S;
    Op.EndLoc = E;
    return Op;
  }

};

}

#endif // LLVM_LIB_TARGET_[(${namespace})]_[(${namespace})]ParsedOperand_H