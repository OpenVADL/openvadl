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
#include <sstream>
#include <set>

using namespace llvm;

#define DEBUG_TYPE "[(${namespace})]-asm-parser"

namespace llvm {
[# th:each="pr : ${lexParsingResults}" ]
RuleParsingResult<StringRef> <[(${namespace})]AsmRecursiveDescentParser::[(${pr.functionName})]() {
  auto tok = Lexer.getTok();
  if(tok.getKind() != AsmToken::[(${pr.kind.name()})])
  {
      return RuleParsingResult<StringRef>(tok.getLoc(), "Expected [(${pr.kind.name()})], but got '" + tok.getString() + "'");
  } else {
      Lexer.Lex();
      return RuleParsingResult<StringRef>(ParsedValue<StringRef>(tok.getString(), tok.getLoc(), tok.getEndLoc()));
  }
}
[/]

RuleParsingResult<StringRef> [(${namespace})]AsmRecursiveDescentParser::IDENTIFIER() {
    auto tok = Lexer.getTok();
    if(tok.getKind() != AsmToken::Identifier)
    {
        return RuleParsingResult<StringRef>(tok.getLoc(), "Expected IDENTIFIER, but got '" + tok.getString() + "'");
    } else {
        Lexer.Lex();
        return RuleParsingResult<StringRef>(ParsedValue<StringRef>(tok.getString(), tok.getLoc(), tok.getEndLoc()));
    }
}

RuleParsingResult<StringRef> [(${namespace})]AsmRecursiveDescentParser::STRING() {
    auto tok = Lexer.getTok();
    if(tok.getKind() != AsmToken::String)
    {
        return RuleParsingResult<StringRef>(tok.getLoc(), "Expected STRING, but got '" + tok.getString() + "'");
    } else {
        Lexer.Lex();
        return RuleParsingResult<StringRef>(ParsedValue<StringRef>(tok.getString(), tok.getLoc(), tok.getEndLoc()));
    }
}

RuleParsingResult<int64_t /* SInt<64> */> [(${namespace})]AsmRecursiveDescentParser::INTEGER() {
    auto tok = Lexer.getTok();
    if(tok.getKind() != AsmToken::Integer)
    {
        return RuleParsingResult<int64_t /* SInt<64> */>(tok.getLoc(), "Expected INTEGER, but got '" + tok.getString() + "'");
    } else {
        Lexer.Lex();
        return RuleParsingResult<int64_t /* SInt<64> */>(ParsedValue<int64_t /* SInt<64> */>(tok.getIntVal(), tok.getLoc(), tok.getEndLoc()));
    }
}

RuleParsingResult<NoData> [(${namespace})]AsmRecursiveDescentParser::EOL() {
    auto tok = Lexer.getTok();
    if(tok.getKind() != AsmToken::EndOfStatement)
    {
        return RuleParsingResult<NoData>(tok.getLoc(), "Expected EOL, but got '" + tok.getString() + "'");
    } else {
        Lexer.Lex();
        return RuleParsingResult<NoData>(ParsedValue<NoData>(NoData(), tok.getLoc(), tok.getEndLoc()));
    }
}

RuleParsingResult<uint64_t /* UInt<64> */> [(${namespace})]AsmRecursiveDescentParser::Register()
{
    RuleParsingResult<StringRef> temp_20 = IDENTIFIER();
    if(!temp_20.Success) {
        return RuleParsingResult<uint64_t /* UInt<64> */>(temp_20.getError());
    }
    ParsedValue<StringRef> parsed = temp_20.getParsed();
    unsigned temp_21;
    if(!AsmUtils::MatchRegNo(parsed.Value, temp_21)) {
        return RuleParsingResult<uint64_t /* UInt<64> */>(parsed.S, "Could not convert data into register because '" + parsed.Value + "' is not a valid register");
    }
    ParsedValue<uint64_t /* UInt<64> */> casted(temp_21, parsed.S, parsed.E);
    return RuleParsingResult<uint64_t /* UInt<64> */>(casted);
}

RuleParsingResult<StringRef> [(${namespace})]AsmRecursiveDescentParser::Identifier()
{
    RuleParsingResult<StringRef> temp_22 = IDENTIFIER();
    if(!temp_22.Success) {
        return RuleParsingResult<StringRef>(temp_22.getError());
    }
    ParsedValue<StringRef> RuleReference_g2504 = temp_22.getParsed();
    return RuleParsingResult<StringRef>(RuleReference_g2504);
}

[# th:each="pr : ${instructionResults}" ]
RuleParsingResult<NoData> [(${namespace})]AsmRecursiveDescentParser::[(${pr.functionName})]() {
[(${pr.body})]
}
[/]

}
