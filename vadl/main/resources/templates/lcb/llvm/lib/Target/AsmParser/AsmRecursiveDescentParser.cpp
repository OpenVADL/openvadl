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
#include "vadl-builtins.hpp"
#include <sstream>
#include <set>
#include <vector>
#include <string>

using namespace llvm;

#define DEBUG_TYPE "[(${namespace})]-asm-parser"

namespace llvm {

  RuleParsingResult<NoData> [(${namespace})]AsmRecursiveDescentParser::ParseStatement() {
[# th:if="${asmDescriptionExists}"]
    return Statement();
[/]
[# th:unless="${asmDescriptionExists}"]
    return RuleParsingResult<NoData>(ParsedValue<NoData>(NoData {}));
[/]
  }

  RuleParsingResult<uint64_t> [(${namespace})]AsmRecursiveDescentParser::ParseRegister() {
[# th:if="${asmDescriptionExists}"]
    return Register();
[/]
[# th:unless="${asmDescriptionExists}"]
    return RuleParsingResult<uint64_t>(ParsedValue<uint64_t>(0));
[/]
  }

  RuleParsingResult<StringRef> [(${namespace})]AsmRecursiveDescentParser::Literal(std::string toParse) {
    auto tok = Lexer.getTok();
    if(!tok.getString().[(${compareFunction})](toParse))
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

  bool [(${namespace})]AsmRecursiveDescentParser::VADL_asmparser_lakindin(uint64_t lookahead, const std::vector<std::string>& compareTokenKinds) {
    std::optional<AsmToken> tok = VADL_asmparser_lookahead_token(lookahead);
    if (!tok.has_value()) {
      return false;
    }

    const AsmToken::TokenKind actualKind = tok->getKind();
    for (const std::string &kindStr : compareTokenKinds) {
      std::optional<AsmToken::TokenKind> expectedKind = AsmUtils::stringToAsmTokenKind(kindStr);
      if (expectedKind.has_value() && actualKind == *expectedKind) {
        return true;
      }
    }
    return false;
  }

  bool [(${namespace})]AsmRecursiveDescentParser::VADL_asmparser_laidin(uint64_t lookahead, const std::vector<std::string>& compareStrings) {
    std::optional<AsmToken> tok = VADL_asmparser_lookahead_token(lookahead);
    if (!tok.has_value()) {
      return false;
    }

    StringRef s = tok->getString();
    for (size_t i = 0; i < compareStrings.size(); ++i) {
      if (s.[(${compareFunction})](compareStrings[i])) {
        return true;
      }
    }
    return false;
  }

  bool [(${namespace})]AsmRecursiveDescentParser::VADL_asmparser_laideq(uint64_t lookahead, const std::string compareString) {
    std::optional<AsmToken> tok = VADL_asmparser_lookahead_token(lookahead);
    if (!tok.has_value()) {
      return false;
    }

    return tok->getString().[(${compareFunction})](compareString);
  }

  std::optional<AsmToken> [(${namespace})]AsmRecursiveDescentParser::VADL_asmparser_lookahead_token(uint64_t lookahead) {
    if (lookahead == 0) {
      return Lexer.getTok();
    }

    AsmToken current = Lexer.getTok();
    AsmToken firstLookahead = Lexer.Lex();

    std::optional<AsmToken> result;
    if (lookahead == 1) {
      result = firstLookahead;
    } else {
      std::vector<AsmToken> rest(lookahead - 1);
      MutableArrayRef<AsmToken> buf(rest.data(), lookahead - 1);
      size_t readCount = Lexer.peekTokens(buf, true);

      size_t idx = lookahead - 2;
      if (readCount > idx) {
        result = rest[idx];
      }
    }

    Lexer.UnLex(current);
    return result;
  }

[(${grammarRules})]
}
