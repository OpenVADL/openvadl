package vadl.lcb.template.lib.Target.AsmParser;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalToIgnoringWhiteSpace;

import java.io.IOException;
import java.io.StringWriter;
import org.junit.jupiter.api.Test;
import vadl.gcb.valuetypes.ProcessorName;
import vadl.lcb.config.LcbConfiguration;
import vadl.utils.SourceLocation;
import vadl.viam.Identifier;
import vadl.viam.Specification;

class EmitAsmParserCppFilePassTest {

  private static LcbConfiguration createLcbConfiguration() {
    return new LcbConfiguration("");
  }

  @Test
  void shouldRenderTemplate() throws IOException {
    // Given
    var specification = new Specification(
        new Identifier("specificationValue", SourceLocation.INVALID_SOURCE_LOCATION));

    var template =
        new EmitAsmParserCppFilePass(createLcbConfiguration(),
            new ProcessorName("processorNameValue"));
    var writer = new StringWriter();

    // When
    template.renderToString(null, specification, writer);
    var output = writer.toString();

    // Then
    assertThat(output, equalToIgnoringWhiteSpace("""
        #include "specificationValueAsmRecursiveDescentParser.h"
        #include "MCTargetDesc/specificationValueMCTargetDesc.h"
        #include "MCTargetDesc/specificationValueTargetStreamer.h"
        #include "MCTargetDesc/AsmUtils.h"
        #include "TargetInfo/specificationValueTargetInfo.h"
        #include "llvm/MC/MCParser/MCAsmLexer.h"
        #include "llvm/MC/MCParser/MCAsmParser.h"
        #include "llvm/MC/MCParser/MCTargetAsmParser.h"
        #include "llvm/MC/MCParser/MCTargetAsmParser.h"
        #include "llvm/MC/TargetRegistry.h"
        #include "Utils/ImmediateUtils.h"
        #include "llvm/MC/MCRegister.h"
               
               
        using namespace llvm;
               
        #define DEBUG_TYPE "specificationValue-asm-parser"
               
        namespace llvm {
        struct specificationValueOperand;
               
        class specificationValueAsmParser : public MCTargetAsmParser {
            MCAsmParser &Parser;
               
        specificationValueTargetStreamer &getTargetStreamer() {
            MCTargetStreamer &TS = *getParser().getStreamer().getTargetStreamer();
            return static_cast<specificationValueTargetStreamer &>(TS);
        }
               
        bool ModifyImmediate(unsigned OpCode, unsigned OpIndex, specificationValueParsedOperand &Op);
               
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
            specificationValueAsmParser(const MCSubtargetInfo &sti, MCAsmParser &parser,
                        const MCInstrInfo &MII, const MCTargetOptions &Options)
                : MCTargetAsmParser(Options, sti, MII), Parser(parser) {
               \s
            }
               
        };
               
        bool specificationValueAsmParser::MatchAndEmitInstruction(SMLoc IDLoc, unsigned &Opcode,
                                                    OperandVector &Operands,
                                                    MCStreamer &Out,
                                                    uint64_t &ErrorInfo,
                                                    bool MatchingInlineAsm) {
            auto mnemonic = static_cast<specificationValueParsedOperand&>(*Operands[0]).getToken();
            if(!AsmUtils::MatchOpcode(mnemonic, Opcode)){
                Parser.Error(IDLoc, "Uknown mnemonic '" + mnemonic + "'");
                return true;
            }
               
            // std::string msg = "Matching Instruction (" + std::to_string(Opcode) + "):\\n";
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
               \s
            }
               
            for( unsigned i = 0; i < targets.size(); i++ )
            {
                auto searchTarget = targets[i];
                bool targetMatched = false;
               
                unsigned j = 1;
                while( j < Operands.size() && targetMatched == false )
                {
                    specificationValueParsedOperand& op = static_cast<specificationValueParsedOperand&>(*Operands[j]);
                    auto parsedTarget = op.getTarget();
                    if( parsedTarget == searchTarget )
                    {
                        if(!ModifyImmediate(Opcode, j, op))
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
                    specificationValueParsedOperand& mnemonic = static_cast<specificationValueParsedOperand&>(*Operands[0]);
                    Parser.Error(mnemonic.getStartLoc(), "Could not find index for operand '" + searchTarget + "'");
                    return true;
                }
            }
               
            Out.emitInstruction(Inst, getSTI());
               
            return false;
        }
               
        bool specificationValueAsmParser::parseRegister(MCRegister &RegNo, SMLoc &StartLoc, SMLoc &EndLoc) {
            if (tryParseRegister(RegNo, StartLoc, EndLoc) != MatchOperand_Success) {
                return Error(StartLoc, "invalid register name");
            }
               
            return false;
        }
               
        OperandMatchResultTy specificationValueAsmParser::tryParseRegister(MCRegister &RegNo,
                                                                      SMLoc &StartLoc,
                                                                      SMLoc &EndLoc) {
            SmallVector<std::unique_ptr<MCParsedAsmOperand>, 0> dummy;
            specificationValueAsmRecursiveDescentParser parserGen(getLexer(), getParser(), dummy);
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
               
        bool specificationValueAsmParser::ParseDirective(AsmToken DirectiveID) {
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
               
        bool specificationValueAsmParser::ParseInstruction(ParseInstructionInfo &Info,
                                StringRef Name, SMLoc NameLoc,
                                OperandVector &Operands) {
            const AsmToken mnemonicToken(AsmToken::TokenKind::Identifier, StringRef(NameLoc.getPointer(), Name.size()));
            getLexer().UnLex(mnemonicToken);
               
            specificationValueAsmRecursiveDescentParser parserGen(getLexer(), getParser(), Operands);
            auto result = parserGen.ParseStatement();
               
            if(!result.Success)
            {
                SMLoc loc = std::get<0>(result.getError());
                std::string msg = std::get<1>(result.getError());
                return Error(loc, msg);
            }
               
            return !result.Success;
        }
               
        void specificationValueAsmParser::convertToMapAndConstraints(unsigned Kind,
                                                      const OperandVector &Operands) {
        }
               
        bool specificationValueAsmParser::ModifyImmediate(unsigned Opcode, unsigned OpIndex, specificationValueParsedOperand &Op)
        {
            if(!Op.isImm() || Op.getImm()->getKind() != MCExpr::ExprKind::Constant)
                return true;
               
            auto opImm64 = dyn_cast<MCConstantExpr>(Op.getImm())->getValue();
            switch(Opcode)
            {
               
            }
            return true;
        }
               
        }
               
        extern "C" LLVM_EXTERNAL_VISIBILITY void LLVMInitializespecificationValueAsmParser() {
            RegisterMCAsmParser<specificationValueAsmParser> X(getThespecificationValueTarget());
        }
        """));
  }

}