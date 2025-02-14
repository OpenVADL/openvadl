#ifndef LLVM_LIB_TARGET_[(${namespace})]_ASMPARSER_H
#define LLVM_LIB_TARGET_[(${namespace})]_ASMPARSER_H

#include "AsmParsedOperand.h"
#include "MCTargetDesc/[(${namespace})]MCExpr.h"
#include "llvm/MC/MCParser/MCAsmLexer.h"
#include "llvm/MC/MCParser/MCAsmParser.h"
#include "llvm/MC/MCParser/MCTargetAsmParser.h"
#include <map>

using namespace llvm;

namespace llvm {

template <typename T> struct ParsedValue {
    T Value;
    SMLoc S, E;

    ParsedValue(T Value): Value(Value), S(SMLoc()), E(SMLoc()) {}
    ParsedValue(T Value, SMLoc S, SMLoc E): Value(Value), S(S), E(E) {}
};

template <typename T> class RuleParsingResult {
private:
    std::optional<std::tuple<SMLoc, std::string>> Error;
    std::optional<ParsedValue<T>> Parsed;

public:
    bool Success;

    RuleParsingResult(ParsedValue<T> Parsed) : Parsed(Parsed), Success(true) {
    }

    RuleParsingResult(std::tuple<SMLoc, std::string> Error) : Error(Error), Success(false) {
    }

    RuleParsingResult(SMLoc Location, Twine Msg) : Error(std::make_tuple (Location, Msg.str())), Success(false) {
    }

    std::tuple<SMLoc, std::string> getError() {
    return Error.value();
    }

    ParsedValue<T> getParsed() {
    return Parsed.value();
    }
};

/*
struct NoData{};

struct mnemonic {
    ParsedValue<[(${namespace})]ParsedOperand> mnemonic;
};
*/

[# th:each="struct : ${parsedValueStructs}" ]
struct [(${struct.getName()})] {
  [# th:each="field : ${struct.getFields()}" ]
  ParsedValue<[(${field.cppTypeString()})]> [(${field.name()})];
  [/]
};

[/]

class [(${namespace})]AsmRecursiveDescentParser {
    MCAsmLexer &Lexer;
    MCAsmParser &Parser;
    OperandVector &Operands;

private:

    [# th:each="pr : ${parsingResults}" ]
    RuleParsingResult<[(${pr.type})]> [(${pr.functionName})]();
    [/]

    RuleParsingResult<StringRef> Literal(std::string toParse);
    RuleParsingResult<const MCExpr*> BuiltinExpression();

    bool builtin_asm_laidin(uint64_t lookahead, const std::vector<string>& compareStrings);
    bool builtin_asm_laideq(uint64_t lookahead, const string compareString);

public:
    [(${namespace})]AsmRecursiveDescentParser(MCAsmLexer &lexer, MCAsmParser &parser, OperandVector& operands)
        : Lexer(lexer), Parser(parser), Operands(operands) {
    }

    //RuleParsingResult<NoData> EOL();
    RuleParsingResult<NoData> ParseStatement();
    RuleParsingResult<uint64_t> ParseRegister();
};

}

#endif // LLVM_LIB_TARGET_[(${namespace})]_ASMPARSER_H