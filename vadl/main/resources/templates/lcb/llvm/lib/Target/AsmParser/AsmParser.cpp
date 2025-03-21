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

bool ModifyImmediate(unsigned OpIndex, unsigned OpCode, StringRef OpName, [(${namespace})]ParsedOperand &Op);

bool MatchAndEmitInstruction(SMLoc IDLoc, unsigned &Opcode,
                            OperandVector &Operands, MCStreamer &Out,
                            uint64_t &ErrorInfo,
                            bool MatchingInlineAsm) override;

bool parseRegister(MCRegister &RegNo, SMLoc &StartLoc, SMLoc &EndLoc) override;

OperandMatchResultTy tryParseRegister(MCRegister &RegNo,
                                                      SMLoc &StartLoc,
                                                      SMLoc &EndLoc) override;

bool ParseInstruction(ParseInstructionInfo &Info,
                        StringRef Name, SMLoc NameLoc,
                        OperandVector &Operands) override;

bool ParseDirective(AsmToken DirectiveID) override;

void convertToMapAndConstraints(unsigned Kind, const OperandVector &Operands) override;

public:
    [(${namespace})]AsmParser(const MCSubtargetInfo &sti, MCAsmParser &parser,
                const MCInstrInfo &MII, const MCTargetOptions &Options)
        : MCTargetAsmParser(Options, sti, MII), Parser(parser) {
        [# th:each="alias : ${aliases}" ]
            Parser.addAliasForDirective("[(${alias.alias})]", "[(${alias.target})]");
        [/]
    }

};

bool [(${namespace})]AsmParser::MatchAndEmitInstruction(SMLoc IDLoc, unsigned &Opcode,
                                            OperandVector &Operands,
                                            MCStreamer &Out,
                                            uint64_t &ErrorInfo,
                                            bool MatchingInlineAsm) {
    auto mnemonic = static_cast<[(${namespace})]ParsedOperand&>(*Operands[0]).getToken();
    if(!AsmUtils::MatchOpcode(mnemonic, Opcode)){
        Parser.Error(IDLoc, "Uknown mnemonic '" + mnemonic + "'");
        return true;
    }

    // std::string msg = "Matching Instruction (" + std::to_string(Opcode) + "):\n";
    // for(auto it = Operands.begin(); it != Operands.end(); ++it)
    // {
    //     std::string s = "";
    //     raw_string_ostream O(s);
    //     it->get()->print(O);
    //     msg += O.str();
    // }
    // Note(IDLoc, msg);

    MCInst Inst;
    Inst.setOpcode(Opcode);
    Inst.setLoc(IDLoc);

    std::vector<size_t> OpIndex;
    std::vector<std::string> targets;

    switch(Opcode) {
        [# th:each="instruction : ${instructions}" ]
        case [(${namespace})]::[(${instruction.name})]: targets = { [(${instruction.operands})] }; break;
        [/]
    }

    for( unsigned i = 0; i < targets.size(); i++ )
    {
        auto searchTarget = targets[i];
        bool targetMatched = false;

        unsigned j = 1; // start at 1 because 0 is the mnemonic
        while( j < Operands.size() && targetMatched == false )
        {
            [(${namespace})]ParsedOperand& op = static_cast<[(${namespace})]ParsedOperand&>(*Operands[j]);
            auto parsedTarget = op.getTarget();
            if( parsedTarget == searchTarget )
            {
                if(!ModifyImmediate(j, Opcode, parsedTarget, op))
                {
                    return true;
                }
                op.addOperand(Inst);
                targetMatched = true;
            }
            j++;
        }

        if( targetMatched == false )
        {
            [(${namespace})]ParsedOperand& mnemonic = static_cast<[(${namespace})]ParsedOperand&>(*Operands[0]);
            Parser.Error(mnemonic.getStartLoc(), "Could not find index for operand '" + searchTarget + "'");
            return true;
        }
    }

    Out.emitInstruction(Inst, getSTI());

    return false;
}

bool [(${namespace})]AsmParser::parseRegister(MCRegister &RegNo, SMLoc &StartLoc, SMLoc &EndLoc) {
    if (tryParseRegister(RegNo, StartLoc, EndLoc) != MatchOperand_Success) {
        return Error(StartLoc, "invalid register name");
    }

    return false;
}

OperandMatchResultTy [(${namespace})]AsmParser::tryParseRegister(MCRegister &RegNo,
                                                              SMLoc &StartLoc,
                                                              SMLoc &EndLoc) {
    SmallVector<std::unique_ptr<MCParsedAsmOperand>, 0> dummy;
    [(${namespace})]AsmRecursiveDescentParser parserGen(getLexer(), getParser(), dummy);
    auto result = parserGen.ParseRegister();

    if(!result.Success)
    {
        SMLoc loc = std::get<0>(result.getError());
        std::string msg = std::get<1>(result.getError());
        return MatchOperand_NoMatch;
    }
    RegNo = MCRegister::from(result.getParsed().Value);
    StartLoc = result.getParsed().S;
    EndLoc = result.getParsed().E;
    return MatchOperand_Success;
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

bool [(${namespace})]AsmParser::ModifyImmediate(unsigned OpIndex, unsigned Opcode, StringRef OpName, [(${namespace})]ParsedOperand &Op)
{
    if(!Op.isImm() || Op.getImm()->getKind() != MCExpr::ExprKind::Constant)
        return true;

    int64_t opImm64 = dyn_cast<MCConstantExpr>(Op.getImm())->getValue();

    [# th:each="conversion : ${immediateConversions}" ]
    if(Opcode == [(${namespace})]::[(${conversion.insnName})] && OpIndex == [(${conversion.opIndex})])
    {
        if (OpName.equals_insensitive("[(${conversion.operandName})]")) {
            // check if immediate is in ([(${conversion.lowestValue})],[(${conversion.highestValue})])
            if (opImm64 < [(${conversion.lowestValue})] || opImm64 > [(${conversion.highestValue})]) {
                std::string error = "Invalid immediate operand for [(${conversion.insnName})].[(${conversion.operandName})]. Value "
                 + std::to_string(opImm64) + " is out of the valid range ([(${conversion.lowestValue})],[(${conversion.highestValue})]).";
                Parser.Error(Op.getStartLoc(), error);
                return false;
            }
            [# th:if="${conversion.needsDecode}" ]
            opImm64 = [(${conversion.decodeMethod})](opImm64);
            [/]
            // check if immediate fits the provided predicate for the instruction
            if(![(${conversion.predicateMethod})](opImm64))
            {
                std::string error = "Invalid immediate operand for [(${conversion.insnName})].[(${conversion.operandName})]. The predicate does not hold.";
                Parser.Error(Op.getStartLoc(), error);
                return false;
            }

            const MCExpr* constantExpr = MCConstantExpr::create(opImm64, Parser.getContext());
            Op = [(${namespace})]ParsedOperand::CreateImm(constantExpr, Op.getStartLoc(), Op.getEndLoc());
            return true;
        } else {
            const MCExpr* constantExpr = MCConstantExpr::create(opImm64, Parser.getContext());
            Op = [(${namespace})]ParsedOperand::CreateImm(constantExpr, Op.getStartLoc(), Op.getEndLoc());
            return true;
        }
    }[/]
    return true;
}

}

extern "C" LLVM_EXTERNAL_VISIBILITY void LLVMInitialize[(${namespace})]AsmParser() {
    RegisterMCAsmParser<[(${namespace})]AsmParser> X(getThe[(${namespace})]Target());
}