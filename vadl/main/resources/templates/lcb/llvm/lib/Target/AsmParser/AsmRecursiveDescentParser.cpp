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

  RuleParsingResult<NoData> [(${namespace})]AsmRecursiveDescentParser::ParseStatement() {
    //return Statement();
    return RuleParsingResult<NoData>(ParsedValue<NoData>(NoData {}));
  }

  RuleParsingResult<uint64_t /* UInt<64> */> [(${namespace})]AsmRecursiveDescentParser::ParseRegister() {
    //return Register();
    return RuleParsingResult<uint64_t>(ParsedValue<uint64_t>(0));
  }

[# th:each="rule : ${grammarRules}" ]
  [(${rule})]
[/]
}
