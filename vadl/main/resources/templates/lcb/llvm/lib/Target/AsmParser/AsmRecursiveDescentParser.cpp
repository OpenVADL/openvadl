#include "AsmParser/AsmRecursiveDescentParser.h"
#include "MCTargetDesc/[(${namespace})]MCTargetDesc.h"
#include "MCTargetDesc/[(${namespace})]TargetStreamer.h"
#include "MCTargetDesc/AsmUtils.h"
#include "TargetInfo/[(${namespace})]TargetInfo.h"
#include "llvm/MC/MCContext.h"
#include "llvm/MC/MCParser/MCAsmLexer.h"
#include "llvm/MC/MCParser/MCAsmParser.h"
#include "llvm/MC/MCParser/MCTargetAsmParser.h"
#include "llvm/MC/MCParser/MCTargetAsmParser.h"
#include "llvm/MC/TargetRegistry.h"
#include "vadl-builtins.h"
#include <sstream>
#include <set>
#include <vector>
#include <string>

using namespace llvm;

#define DEBUG_TYPE "[(${namespace})]-asm-parser"

namespace llvm {

  RuleParsingResult<NoData> [(${namespace})]AsmRecursiveDescentParser::ParseStatement() {
    return Statement();
  }

  RuleParsingResult<uint64_t> [(${namespace})]AsmRecursiveDescentParser::ParseRegister() {
    return Register();
  }

  RuleParsingResult<StringRef> [(${namespace})]AsmRecursiveDescentParser::Literal(std::string toParse) {
    auto tok = Lexer.getTok();
    if(!tok.getString().«compareFunction»(toParse))
    {
      return RuleParsingResult<StringRef>(tok.getLoc(), "Expected '" + toParse + "', but got '" + tok.getString() + "'");
    } else {
      Lexer.Lex();
      return RuleParsingResult<StringRef>(ParsedValue<StringRef>(tok.getString(), tok.getLoc(), tok.getEndLoc()));
    }
  }

  RuleParsingResult<const MCExpr*> [(${namespace})]AsmRecursiveDescentParser::BuiltinExpression() {
    const MCExpr* expr;
    if (Parser.parseExpression(expr)) {
      return RuleParsingResult<const MCExpr*>(Lexer.getTok().getLoc(), "Invalid expression.");
    } else {
      return RuleParsingResult<const MCExpr*>(ParsedValue<const MCExpr*>(expr, expr->getLoc(), expr->getLoc()));
    }
  }

  bool [(${namespace})]AsmRecursiveDescentParser::builtin_asm_laidin(uint64_t lookahead, const std::vector<string>& compareStrings) {
    AsmToken* tok;
    MutableArrayRef<AsmToken> Buf(tok, lookahead);
    size_t ReadCount = peekTokens(Buf, true);

    for (size_t i = 0; i < compareStrings.size(); i++) {
      if (tok[lookahead].getString().[(${compareFunction})](compareStrings[i])) {
          return true;
      }
    }
    return false;
  }

  bool [(${namespace})]AsmRecursiveDescentParser::builtin_asm_laideq(uint64_t lookahead, const string compareString) {
      AsmToken* tok;
      MutableArrayRef<AsmToken> Buf(tok ,lookahead);
      size_t ReadCount = peekTokens(Buf, true);

      return tok[lookahead].getString().[(${compareFunction})](compareString);
  }

[(${grammarRules})]
}
