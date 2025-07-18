#include "AsmRecursiveDescentParser.h"
#include "MCTargetDesc/[(${namespace})]MCTargetDesc.h"
#include "MCTargetDesc/[(${namespace})]TargetStreamer.h"
#include "MCTargetDesc/AsmUtils.h"
#include "TargetInfo/[(${namespace})]TargetInfo.h"
#include "llvm/MC/MCParser/MCAsmLexer.h"
#include "llvm/MC/MCParser/MCAsmParser.h"
#include "llvm/MC/MCParser/MCTargetAsmParser.h"
#include "llvm/MC/TargetRegistry.h"
#include "Utils/ImmediateUtils.h"
#include "llvm/MC/MCRegister.h"


using namespace llvm;

#define DEBUG_TYPE "[(${namespace})]-asm-parser"

namespace llvm {
struct [(${namespace})]Operand;

class [(${namespace})]AsmParser : public MCTargetAsmParser {
    MCAsmParser &Parser;

[(${namespace})]TargetStreamer &getTargetStreamer() {
    MCTargetStreamer &TS = *getParser().getStreamer().getTargetStreamer();
    return static_cast<[(${namespace})]TargetStreamer &>(TS);
}

bool ModifyImmediate(unsigned OpIndex, unsigned OpCode, StringRef OpName, StringRef GrammarAttribute, [(${namespace})]ParsedOperand &Op);

bool MatchAndEmitInstruction(SMLoc IDLoc, unsigned &Opcode,
                            OperandVector &Operands, MCStreamer &Out,
                            uint64_t &ErrorInfo,
                            bool MatchingInlineAsm) override;

bool parseRegister(MCRegister &RegNo, SMLoc &StartLoc, SMLoc &EndLoc) override;

ParseStatus tryParseRegister(MCRegister &RegNo, SMLoc &StartLoc, SMLoc &EndLoc) override;

bool ParseInstruction(ParseInstructionInfo &Info,
                        StringRef Name, SMLoc NameLoc,
                        OperandVector &Operands) override;

bool ParseDirective(AsmToken DirectiveID) override;

void convertToMapAndConstraints(unsigned Kind, const OperandVector &Operands) override;

void reportError(OperandVector &Operands);

[# th:each="instruction : ${instructions}" ]
bool parse_[(${instruction.name})](MCInst &Inst, OperandVector &Operands);
[/]

public:
    [(${namespace})]AsmParser(const MCSubtargetInfo &sti, MCAsmParser &parser,
                const MCInstrInfo &MII, const MCTargetOptions &Options)
        : MCTargetAsmParser(Options, sti, MII), Parser(parser) {
        [# th:each="alias : ${aliases}" ]
            Parser.addAliasForDirective("[(${alias.alias})]", "[(${alias.target})]");
        [/]
    }

};

void [(${namespace})]AsmParser::reportError(OperandVector &Operands) {
    [(${namespace})]ParsedOperand& mnemonic = static_cast<[(${namespace})]ParsedOperand&>(*Operands[0]);
    Parser.Error(mnemonic.getStartLoc(), "Number of expected operands does not match");
}

[# th:each="instruction : ${instructions}" ]
bool [(${namespace})]AsmParser::parse_[(${instruction.name})](MCInst &Inst, OperandVector &Operands) {
    if(Operands.size() - 1 != [(${instruction.numOperands})]) {
      reportError(Operands);
    }

    // { [(${instruction.targets})] }
    std::vector<std::string> targets;
    targets =  { [(${instruction.targets})] };
    for(auto target : targets) {
      bool found = false;
      for(auto index = 1; index < [(${instruction.numOperands})] + 1; index++) {
        [(${namespace})]ParsedOperand& Op = static_cast<[(${namespace})]ParsedOperand&>(*Operands[ index ]);
        StringRef operandName = Op.getTarget();

        [# th:each="operand : ${instruction.operands}" ]
        if(operandName == "[(${operand.name})]" && target == "[(${operand.targetName})]") {
          found = true;

          if(!Op.isImm() || Op.getImm()->getKind() != MCExpr::ExprKind::Constant) {
              Op.addOperand(Inst);
          } else {
            int64_t opImm64 = dyn_cast<MCConstantExpr>(Op.getImm())->getValue();

            [# th:if="${operand.isFieldOperand}"]
            opImm64 = [(${operand.decodeMethod})]([(${operand.params})]);
            [/]

            [# th:if="${operand.requiresPredicate}" ]
            if(![(${operand.predicateMethod})](opImm64)) {
              std::string error = "Invalid immediate operand for [(${operand.name})]. The predicate does not hold.";
              Parser.Error(Op.getStartLoc(), error);
              return true;
            }
            [/]

            const MCExpr* constantExpr = MCConstantExpr::create(opImm64, Parser.getContext());
            auto operand = [(${namespace})]ParsedOperand::CreateImm(constantExpr, Op.getStartLoc(), Op.getEndLoc());
            operand.addOperand(Inst);
          }
          break;
        }

        [/]
      }
      if (!found) {
        [(${namespace})]ParsedOperand& mnemonic = static_cast<[(${namespace})]ParsedOperand&>(*Operands[0]);
        Parser.Error(mnemonic.getStartLoc(), "Could not find index for operand '" + target + "'");
        return true;
      }
    }
    return false;
}
[/]

bool [(${namespace})]AsmParser::MatchAndEmitInstruction(SMLoc IDLoc,
                                            unsigned &Opcode,
                                            OperandVector &Operands,
                                            MCStreamer &Out,
                                            uint64_t &ErrorInfo,
                                            bool MatchingInlineAsm) {
    auto mnemonic = static_cast<[(${namespace})]ParsedOperand&>(*Operands[0]).getToken();
    if(!AsmUtils::MatchOpcode(mnemonic, Opcode)){
        Parser.Error(IDLoc, "Uknown mnemonic '" + mnemonic + "'");
        return true;
    }

    MCInst Inst;
    Inst.setOpcode(Opcode);
    Inst.setLoc(IDLoc);

    std::vector<size_t> OpIndex;
    std::vector<std::string> targets;

    switch(Opcode) {
        [# th:each="instruction : ${instructions}" ]
        case [(${namespace})]::[(${instruction.name})]:
          if(parse_[(${instruction.name})](Inst, Operands)) {
            return true; // returning true signals error occurred
          }
         break;
        [/]
    }

    Out.emitInstruction(Inst, getSTI());

    return false;
}

bool [(${namespace})]AsmParser::parseRegister(MCRegister &RegNo, SMLoc &StartLoc, SMLoc &EndLoc) {
    if (!tryParseRegister(RegNo, StartLoc, EndLoc).isSuccess()) {
        return Error(StartLoc, "invalid register name");
    }

    return false;
}

ParseStatus [(${namespace})]AsmParser::tryParseRegister(MCRegister &RegNo,
                                                              SMLoc &StartLoc,
                                                              SMLoc &EndLoc) {
    SmallVector<std::unique_ptr<MCParsedAsmOperand>, 0> dummy;
    [(${namespace})]AsmRecursiveDescentParser parserGen(getLexer(), getParser(), dummy);
    auto result = parserGen.ParseRegister();

    if(!result.Success)
    {
        SMLoc loc = std::get<0>(result.getError());
        std::string msg = std::get<1>(result.getError());
        return ParseStatus::NoMatch;
    }
    RegNo = MCRegister::from(result.getParsed().Value);
    StartLoc = result.getParsed().S;
    EndLoc = result.getParsed().E;
    return ParseStatus::Success;
}

bool [(${namespace})]AsmParser::ParseDirective(AsmToken DirectiveID) {
    StringRef IDVal = DirectiveID.getString();

    // TODO @tschwarzinger how to handle this?
    if (IDVal == ".option")
    {
        Parser.eatToEndOfStatement();
        return false;
    }

    // Let LLVM handle it
    return true;
}

bool [(${namespace})]AsmParser::ParseInstruction(ParseInstructionInfo &Info,
                        StringRef Name, SMLoc NameLoc,
                        OperandVector &Operands) {
    const AsmToken mnemonicToken(AsmToken::TokenKind::Identifier, StringRef(NameLoc.getPointer(), Name.size()));
    getLexer().UnLex(mnemonicToken);

    [(${namespace})]AsmRecursiveDescentParser parserGen(getLexer(), getParser(), Operands);
    auto result = parserGen.ParseStatement();

    if(!result.Success)
    {
        SMLoc loc = std::get<0>(result.getError());
        std::string msg = std::get<1>(result.getError());
        return Error(loc, msg);
    }

    return !result.Success;
}

void [(${namespace})]AsmParser::convertToMapAndConstraints(unsigned Kind,
                                              const OperandVector &Operands) {
}
}

extern "C" LLVM_EXTERNAL_VISIBILITY void LLVMInitialize[(${namespace})]AsmParser() {
    RegisterMCAsmParser<[(${namespace})]AsmParser> X(getThe[(${namespace})]Target());
}