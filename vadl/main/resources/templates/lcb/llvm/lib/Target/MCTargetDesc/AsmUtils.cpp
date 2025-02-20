#include "AsmUtils.h"
#include "MCTargetDesc/[(${namespace})]MCTargetDesc.h"
#include "llvm/MC/MCInst.h"
#include "llvm/MC/MCExpr.h"
#include "llvm/MC/MCSymbol.h"
#include "llvm/Support/raw_ostream.h"
#include "llvm/Support/Debug.h"
#include "llvm/MC/MCSymbol.h"
#include "[(${namespace})]MCExpr.h"
#include <string>
#include <iostream>
#include <sstream>
#include <bitset>
#include <iomanip>

#define DEBUG_TYPE "AsmUtils"

using namespace llvm;

std::string AsmUtils::formatAsmTokenKind(AsmToken::TokenKind Kind)
{
    switch (Kind) {
    case AsmToken::Error:          return "ERROR";
    case AsmToken::Identifier:     return "IDENTIFIER";
    case AsmToken::Integer:        return "INTEGER";
    case AsmToken::Real:           return "REAL";
    case AsmToken::String:         return "STRING";
    case AsmToken::Amp:            return "AMP";
    case AsmToken::AmpAmp:         return "AMPAMP";
    case AsmToken::At:             return "AT";
    case AsmToken::BackSlash:      return "BACKSLASH";
    case AsmToken::BigNum:         return "BIGNUM";
    case AsmToken::Caret:          return "CARET";
    case AsmToken::Colon:          return "COLON";
    case AsmToken::Comma:          return "COMMA";
    case AsmToken::Comment:        return "COMMENT";
    case AsmToken::Dollar:         return "DOLLAR";
    case AsmToken::Dot:            return "DOT";
    case AsmToken::EndOfStatement: return "ENDOFSTATEMENT";
    case AsmToken::Eof:            return "EOF";
    case AsmToken::Equal:          return "EQUAL";
    case AsmToken::EqualEqual:     return "EQUALEQUAL";
    case AsmToken::Exclaim:        return "EXCLAIM";
    case AsmToken::ExclaimEqual:   return "EXCLAIMEQUAL";
    case AsmToken::Greater:        return "GREATER";
    case AsmToken::GreaterEqual:   return "GREATEREQUAL";
    case AsmToken::GreaterGreater: return "GREATERGREATER";
    case AsmToken::Hash:           return "HASH";
    case AsmToken::HashDirective:  return "HASHDIRECTIVE";
    case AsmToken::LBrac:          return "LBRAC";
    case AsmToken::LCurly:         return "LCURLY";
    case AsmToken::LParen:         return "LPAREN";
    case AsmToken::Less:           return "LESS";
    case AsmToken::LessEqual:      return "LESSEQUAL";
    case AsmToken::LessGreater:    return "LESSGREATER";
    case AsmToken::LessLess:       return "LESSLESS";
    case AsmToken::Minus:          return "MINUS";
    case AsmToken::MinusGreater:   return "MINUSGREATER";
    case AsmToken::Percent:        return "PERCENT";
    case AsmToken::Pipe:           return "PIPE";
    case AsmToken::PipePipe:       return "PIPEPIPE";
    case AsmToken::Plus:           return "PLUS";
    case AsmToken::RBrac:          return "RBRAC";
    case AsmToken::RCurly:         return "RCURLY";
    case AsmToken::RParen:         return "RPAREN";
    case AsmToken::Slash:          return "SLASH";
    case AsmToken::Space:          return "SPACE";
    case AsmToken::Star:           return "STAR";
    case AsmToken::Tilde:          return "TILDE";
    default:                       return "UNKNOWN";
    }
}

std::string AsmUtils::formatImm(MCOperandWrapper Op, uint8_t Radix, const MCAsmInfo *MAI)
{
    return formatImm(Op.getMCOp(), Radix, MAI);
}

std::string AsmUtils::formatImm(MCOperand Op, uint8_t Radix, const MCAsmInfo *MAI)
{
    if(Op.isExpr())
    {
        return formatExpr(Op.getExpr(), Radix, MAI);
    }

    int64_t value = Op.getImm();
    return formatImm(value, Radix, MAI);
}

std::string AsmUtils::formatImm(int64_t Value, uint8_t Radix, const MCAsmInfo *MAI)
{
    std::stringstream stream;
    switch(Radix)
    {
        case(2):
        {
            std::string bits = std::bitset<64>(Value).to_string();
            bits.erase(0, bits.find_first_not_of('0'));
            if(bits.empty())
            {
                bits += "0";
            }
            return "0b" + bits;
        }
        case(8):
            stream << "o" << std::oct << Value;
            return stream.str();
        case(10):
            stream << Value;
            return stream.str();
        case(16):
            stream << "0x" << std::hex << Value;
            return stream.str();
        default:
            return std::string("Usupported radix ") + std::to_string(Radix);
    }
}

std::string AsmUtils::formatExpr(const MCExpr *Expr, uint8_t Radix, const MCAsmInfo *MAI)
{
    std::string s = "";
    raw_string_ostream O(s);

    switch (Expr->getKind()) {
    case MCExpr::Target:
        return cast<[(${namespace})]MCExpr>(Expr)->format(Radix, MAI);
    case MCExpr::Constant: {
        auto Value = cast<MCConstantExpr>(*Expr).getValue();
        return formatImm(Value, Radix, MAI);
    }
    case MCExpr::SymbolRef: {
        const MCSymbolRefExpr &SRE = cast<MCSymbolRefExpr>(*Expr);
        const MCSymbol &Sym = SRE.getSymbol();
        // Parenthesize names that start with $ so that they don't look like
        // absolute names.
        bool UseParens = !Sym.getName().empty() && Sym.getName()[0] == '$';
        if (UseParens) {
            O << '(';
            Sym.print(O, MAI);
            O << ')';
        } else
            Sym.print(O, MAI);

        if(SRE.getKind() != MCSymbolRefExpr::VK_None)
        O << SRE.getKind();


        return O.str();
    }

    case MCExpr::Unary: {
        const MCUnaryExpr &UE = cast<MCUnaryExpr>(*Expr);
        switch(UE.getOpcode()) {
        case MCUnaryExpr::LNot:  O << '!'; break;
        case MCUnaryExpr::Minus: O << '-'; break;
        case MCUnaryExpr::Not:   O << '~'; break;
        case MCUnaryExpr::Plus:  O << '+'; break;
        }
        bool Binary = UE.getSubExpr()->getKind() == MCExpr::Binary;
        if (Binary) O << "(";
        O << formatExpr(UE.getSubExpr(), Radix, MAI);
        if (Binary) O << ")";
        return O.str();
    }

    case MCExpr::Binary: {
        const MCBinaryExpr &BE = cast<MCBinaryExpr>(*Expr);

        // Only print parens around the LHS if it is non-trivial.
        if(isa<MCConstantExpr>(BE.getLHS()) || isa<MCSymbolRefExpr>(BE.getLHS())) {
            O << formatExpr(BE.getLHS(), Radix, MAI);
        } else {
            O << '(';
            O << formatExpr(BE.getLHS(), Radix, MAI);
            O << ')';
        }

        switch(BE.getOpcode()) {
        case MCBinaryExpr::Add:
        // Print "X-42" instead of "X+-42".
        if (const MCConstantExpr *RHSC = dyn_cast<MCConstantExpr>(BE.getRHS())) {
            if (RHSC->getValue() < 0) {
                O << formatImm(RHSC->getValue(), Radix, MAI);
                return O.str();
            }
        }

        O <<  '+';
        break;
        case MCBinaryExpr::AShr: O << ">>"; break;
        case MCBinaryExpr::And:  O <<  '&'; break;
        case MCBinaryExpr::Div:  O <<  '/'; break;
        case MCBinaryExpr::EQ:   O << "=="; break;
        case MCBinaryExpr::GT:   O <<  '>'; break;
        case MCBinaryExpr::GTE:  O << ">="; break;
        case MCBinaryExpr::LAnd: O << "&&"; break;
        case MCBinaryExpr::LOr:  O << "||"; break;
        case MCBinaryExpr::LShr: O << ">>"; break;
        case MCBinaryExpr::LT:   O <<  '<'; break;
        case MCBinaryExpr::LTE:  O << "<="; break;
        case MCBinaryExpr::Mod:  O <<  '%'; break;
        case MCBinaryExpr::Mul:  O <<  '*'; break;
        case MCBinaryExpr::NE:   O << "!="; break;
        case MCBinaryExpr::Or:   O <<  '|'; break;
        case MCBinaryExpr::Shl:  O << "<<"; break;
        case MCBinaryExpr::Sub:  O <<  '-'; break;
        case MCBinaryExpr::Xor:  O <<  '^'; break;
        }

        // Only print parens around the LHS if it is non-trivial.
        if(isa<MCConstantExpr>(BE.getRHS()) || isa<MCSymbolRefExpr>(BE.getRHS())) {
            O << formatExpr(BE.getRHS(), Radix, MAI);
        } else {
            O << '(';
            O << formatExpr(BE.getRHS(), Radix, MAI);
            O << ')';
        }
        return O.str();
    }
    }

    llvm_unreachable("Invalid expression kind!");
}

std::string AsmUtils::FormatModifier(const [(${namespace})]MCExpr::VariantKind VariantKind)
{
    [# th:each="fm : ${formatModifiers}" ]
    if(VariantKind == [(${namespace})]MCExpr::VariantKind::[(${fm.variantKind.value})]) {
      return "[(${fm.relocation.name})]";
    }
    [/]

    return "unknown";
}

std::string AsmUtils::getRegisterName( unsigned RegNo )
{
  switch(RegNo)
  {
  [# th:each="rg : ${registers}" ]
  case [(${namespace})]::[(${rg.name})]:
      return "[(${rg.getAsmName})]";
  [/]
  }
}

bool AsmUtils::MatchRegNo(StringRef Reg, unsigned &RegNo)
{
    return false;
}

bool AsmUtils::MatchOpcode(StringRef Mnemonic, unsigned &Opcode)
{
    return false;
}

bool AsmUtils::MatchCustomModifier(StringRef String, [(${namespace})]MCExpr::VariantKind &VariantKind)
{
    return false;
}

bool AsmUtils::evaluateConstantImm(const MCExpr *Expr, int64_t &Imm)
{
    if (auto *targetExpression = dyn_cast<[(${namespace})]MCExpr>(Expr))
    {
        return targetExpression->evaluateAsConstant(Imm);
    }

    if (auto constantExpression = dyn_cast<MCConstantExpr>(Expr))
    {
        Imm = constantExpression->getValue();
        return true;
    }

    return Expr->evaluateAsAbsolute(Imm);
}

bool AsmUtils::evaluateConstantImm(const MCOperand *MCOp, int64_t &Imm)
{
    if(MCOp->isImm())
    {
        Imm = MCOp->getImm();
        return true;
    }

    if(MCOp->isExpr())
    {
        const MCExpr* expr = MCOp->getExpr();
        return evaluateConstantImm( expr, Imm );
    }

    return false;
}


[# th:each="rg : ${registerClasses}" ]
std::string AsmUtils::getRegisterNameFrom[(${rg.registerFile.name})]ByIndex( unsigned RegIndex ) {
    const int registers[] =
    {
        [# th:each="reg, iterStat : ${rg.registers}" ]
        [(${namespace})]::[(${reg.name})][#th:block th:if="${!iterStat.last}"],[/th:block]
      [/]
    };

    assert([(${rg.registers.size()})] - 1 >= RegIndex && "Register index was out of bounds for 'X'" );
    assert(registers[ RegIndex ] != -1 && "Register index was not allowed for 'X'" );

    unsigned regNo = registers[ RegIndex ];
    return AsmUtils::getRegisterName( regNo );
}
[/]