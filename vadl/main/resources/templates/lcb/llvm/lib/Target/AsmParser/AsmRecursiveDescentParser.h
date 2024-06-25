#ifndef LLVM_LIB_TARGET_[(${namespace})]_ASMPARSER_H
#define LLVM_LIB_TARGET_[(${namespace})]_ASMPARSER_H

#include "[(${namespace})]ParsedOperand.h"
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

struct NoData{};

«structVisitor.generateStructs( grammar )»

class [(${namespace})]AsmRecursiveDescentParser {
    MCAsmLexer &Lexer;
    MCAsmParser &Parser;
    OperandVector &Operands;

private:
    «FOR rule : grammar.rules»
        «visitor.resultType( rule )» «rule.name»();
    «ENDFOR»
    «visitor.resultType( AsmType.String )» Literal(std::string toParse);
    «visitor.resultType( AsmType.Expression )» BuiltinExpression();

public:
    [(${namespace})]AsmRecursiveDescentParser(MCAsmLexer &lexer, MCAsmParser &parser, OperandVector& operands)
        : Lexer(lexer), Parser(parser), Operands(operands) {
    }

    «visitor.resultType( grammar.get( "Statement" ).get )» ParseStatement();
    «visitor.resultType( grammar.get( "Register" ).get )» ParseRegister();
};

}

#endif // LLVM_LIB_TARGET_«processorName»_ASMPARSER_H