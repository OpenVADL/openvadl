package vadl.test.lcb.template.AsmParser;

import java.io.IOException;
import java.nio.charset.Charset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.google.common.io.Files;
import vadl.lcb.template.lib.Target.AsmParser.EmitAsmRecursiveDescentParserHeaderFilePass;
import vadl.lcb.template.lib.Target.EmitInstrInfoTableGenFilePass;
import vadl.pass.PassKey;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.template.AbstractTemplateRenderingPass;
import vadl.test.lcb.AbstractLcbTest;

public class EmitAsmRecursiveDescentParserHeaderFilePassTest extends AbstractLcbTest {
  @Test
  void testLowering() throws IOException, DuplicatedPassKeyException {
    // Given
    var testSetup = runLcb(getConfiguration(false), "sys/risc-v/rv64im.vadl",
        new PassKey(EmitAsmRecursiveDescentParserHeaderFilePass.class.getName()));

    // When
    var passResult =
        (AbstractTemplateRenderingPass.Result) testSetup.passManager().getPassResults()
            .lastResultOf(EmitAsmRecursiveDescentParserHeaderFilePass.class);

    // Then
    var resultFile = passResult.emittedFile().toFile();
    var trimmed = Files.asCharSource(resultFile, Charset.defaultCharset()).read().trim();
    var output = trimmed.lines();

    Assertions.assertLinesMatch("""
        #ifndef LLVM_LIB_TARGET_rv64im_ASMPARSER_H
        #define LLVM_LIB_TARGET_rv64im_ASMPARSER_H
                
        #include "AsmParsedOperand.h"
        #include "MCTargetDesc/rv64imMCExpr.h"
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
                
        struct mnemonic {
            ParsedValue<rv64imParsedOperand> mnemonic;
        };
                
                
        struct mnemonicrdimm {
                
          ParsedValue<rv64imParsedOperand> mnemonic;
          ParsedValue<rv64imParsedOperand> rd;
          ParsedValue<rv64imParsedOperand> imm;
                
        };
        struct mnemonicrdimmrs1 {
                
          ParsedValue<rv64imParsedOperand> mnemonic;
          ParsedValue<rv64imParsedOperand> rd;
          ParsedValue<rv64imParsedOperand> imm;
          ParsedValue<rv64imParsedOperand> rs1;
                
        };
        struct mnemonicrdrs1imm {
                
          ParsedValue<rv64imParsedOperand> mnemonic;
          ParsedValue<rv64imParsedOperand> rd;
          ParsedValue<rv64imParsedOperand> rs1;
          ParsedValue<rv64imParsedOperand> imm;
                
        };
        struct mnemonicrdrs1rs2 {
                
          ParsedValue<rv64imParsedOperand> mnemonic;
          ParsedValue<rv64imParsedOperand> rd;
          ParsedValue<rv64imParsedOperand> rs1;
          ParsedValue<rv64imParsedOperand> rs2;
                
        };
        struct mnemonicrdrs1sft {
                
          ParsedValue<rv64imParsedOperand> mnemonic;
          ParsedValue<rv64imParsedOperand> rd;
          ParsedValue<rv64imParsedOperand> rs1;
          ParsedValue<rv64imParsedOperand> sft;
                
        };
        struct mnemonicrdrs1shamt {
                
          ParsedValue<rv64imParsedOperand> mnemonic;
          ParsedValue<rv64imParsedOperand> rd;
          ParsedValue<rv64imParsedOperand> rs1;
          ParsedValue<rv64imParsedOperand> shamt;
                
        };
        struct mnemonicrs1rs2imm {
                
          ParsedValue<rv64imParsedOperand> mnemonic;
          ParsedValue<rv64imParsedOperand> rs1;
          ParsedValue<rv64imParsedOperand> rs2;
          ParsedValue<rv64imParsedOperand> imm;
                
        };
        struct mnemonicrs2immrs1 {
                
          ParsedValue<rv64imParsedOperand> mnemonic;
          ParsedValue<rv64imParsedOperand> rs2;
          ParsedValue<rv64imParsedOperand> imm;
          ParsedValue<rv64imParsedOperand> rs1;
                
        };
        struct fun67 {
                
          ParsedValue<rv64imParsedOperand> fun67;
                
        };
        struct funct3 {
                
          ParsedValue<rv64imParsedOperand> funct3;
                
        };
        struct funct7 {
                
          ParsedValue<rv64imParsedOperand> funct7;
                
        };
        struct imm {
                
          ParsedValue<rv64imParsedOperand> imm;
                
        };
        struct opcode {
                
          ParsedValue<rv64imParsedOperand> opcode;
                
        };
        struct rd {
                
          ParsedValue<rv64imParsedOperand> rd;
                
        };
        struct rs1 {
                
          ParsedValue<rv64imParsedOperand> rs1;
                
        };
        struct rs2 {
                
          ParsedValue<rv64imParsedOperand> rs2;
                
        };
        struct sft {
                
          ParsedValue<rv64imParsedOperand> sft;
                
        };
                
                
        class rv64imAsmRecursiveDescentParser {
            MCAsmLexer &Lexer;
            MCAsmParser &Parser;
            OperandVector &Operands;
                
        //private:
            /*
           \s
            RuleParsingResult<StringRef> <rv64imAsmRecursiveDescentParser::IDENTIFIER(); //\s
            RuleParsingResult<StringRef> <rv64imAsmRecursiveDescentParser::SPACE(); // \s
            RuleParsingResult<StringRef> <rv64imAsmRecursiveDescentParser::LPAREN(); // (
            RuleParsingResult<StringRef> <rv64imAsmRecursiveDescentParser::RPAREN(); // )
            RuleParsingResult<StringRef> <rv64imAsmRecursiveDescentParser::COMMA(); // ,
            RuleParsingResult<NoData> <rv64imAsmRecursiveDescentParser::MULInstruction(); // MUL
            RuleParsingResult<NoData> <rv64imAsmRecursiveDescentParser::MULHInstruction(); // MULH
            RuleParsingResult<NoData> <rv64imAsmRecursiveDescentParser::MULHSUInstruction(); // MULHSU
            RuleParsingResult<NoData> <rv64imAsmRecursiveDescentParser::MULHUInstruction(); // MULHU
            RuleParsingResult<NoData> <rv64imAsmRecursiveDescentParser::DIVInstruction(); // DIV
            RuleParsingResult<NoData> <rv64imAsmRecursiveDescentParser::DIVUInstruction(); // DIVU
            RuleParsingResult<NoData> <rv64imAsmRecursiveDescentParser::REMInstruction(); // REM
            RuleParsingResult<NoData> <rv64imAsmRecursiveDescentParser::REMUInstruction(); // REMU
            RuleParsingResult<NoData> <rv64imAsmRecursiveDescentParser::MULWInstruction(); // MULW
            RuleParsingResult<NoData> <rv64imAsmRecursiveDescentParser::DIVWInstruction(); // DIVW
            RuleParsingResult<NoData> <rv64imAsmRecursiveDescentParser::DIVUWInstruction(); // DIVUW
            RuleParsingResult<NoData> <rv64imAsmRecursiveDescentParser::REMWInstruction(); // REMW
            RuleParsingResult<NoData> <rv64imAsmRecursiveDescentParser::REMUWInstruction(); // REMUW
            RuleParsingResult<NoData> <rv64imAsmRecursiveDescentParser::ADDInstruction(); // ADD
            RuleParsingResult<NoData> <rv64imAsmRecursiveDescentParser::SUBInstruction(); // SUB
            RuleParsingResult<NoData> <rv64imAsmRecursiveDescentParser::ANDInstruction(); // AND
            RuleParsingResult<NoData> <rv64imAsmRecursiveDescentParser::ORInstruction(); // OR
            RuleParsingResult<NoData> <rv64imAsmRecursiveDescentParser::XORInstruction(); // XOR
            RuleParsingResult<NoData> <rv64imAsmRecursiveDescentParser::SLTInstruction(); // SLT
            RuleParsingResult<NoData> <rv64imAsmRecursiveDescentParser::SLTUInstruction(); // SLTU
            RuleParsingResult<NoData> <rv64imAsmRecursiveDescentParser::SLLInstruction(); // SLL
            RuleParsingResult<NoData> <rv64imAsmRecursiveDescentParser::SRLInstruction(); // SRL
            RuleParsingResult<NoData> <rv64imAsmRecursiveDescentParser::SRAInstruction(); // SRA
            RuleParsingResult<NoData> <rv64imAsmRecursiveDescentParser::ADDIInstruction(); // ADDI
            RuleParsingResult<NoData> <rv64imAsmRecursiveDescentParser::ANDIInstruction(); // ANDI
            RuleParsingResult<NoData> <rv64imAsmRecursiveDescentParser::ORIInstruction(); // ORI
            RuleParsingResult<NoData> <rv64imAsmRecursiveDescentParser::XORIInstruction(); // XORI
            RuleParsingResult<NoData> <rv64imAsmRecursiveDescentParser::SLTIInstruction(); // SLTI
            RuleParsingResult<NoData> <rv64imAsmRecursiveDescentParser::SLTIUInstruction(); // SLTIU
            RuleParsingResult<NoData> <rv64imAsmRecursiveDescentParser::AUIPCInstruction(); // AUIPC
            RuleParsingResult<NoData> <rv64imAsmRecursiveDescentParser::LUIInstruction(); // LUI
            RuleParsingResult<NoData> <rv64imAsmRecursiveDescentParser::LBInstruction(); // LB
            RuleParsingResult<NoData> <rv64imAsmRecursiveDescentParser::LBUInstruction(); // LBU
            RuleParsingResult<NoData> <rv64imAsmRecursiveDescentParser::LHInstruction(); // LH
            RuleParsingResult<NoData> <rv64imAsmRecursiveDescentParser::LHUInstruction(); // LHU
            RuleParsingResult<NoData> <rv64imAsmRecursiveDescentParser::LWInstruction(); // LW
            RuleParsingResult<NoData> <rv64imAsmRecursiveDescentParser::SBInstruction(); // SB
            RuleParsingResult<NoData> <rv64imAsmRecursiveDescentParser::SHInstruction(); // SH
            RuleParsingResult<NoData> <rv64imAsmRecursiveDescentParser::SWInstruction(); // SW
            RuleParsingResult<NoData> <rv64imAsmRecursiveDescentParser::BEQInstruction(); // BEQ
            RuleParsingResult<NoData> <rv64imAsmRecursiveDescentParser::BNEInstruction(); // BNE
            RuleParsingResult<NoData> <rv64imAsmRecursiveDescentParser::BGEInstruction(); // BGE
            RuleParsingResult<NoData> <rv64imAsmRecursiveDescentParser::BGEUInstruction(); // BGEU
            RuleParsingResult<NoData> <rv64imAsmRecursiveDescentParser::BLTInstruction(); // BLT
            RuleParsingResult<NoData> <rv64imAsmRecursiveDescentParser::BLTUInstruction(); // BLTU
            RuleParsingResult<NoData> <rv64imAsmRecursiveDescentParser::JALInstruction(); // JAL
            RuleParsingResult<NoData> <rv64imAsmRecursiveDescentParser::JALRInstruction(); // JALR
            RuleParsingResult<NoData> <rv64imAsmRecursiveDescentParser::ECALLInstruction(); // ECALL
            RuleParsingResult<NoData> <rv64imAsmRecursiveDescentParser::EBREAKInstruction(); // EBREAK
            RuleParsingResult<NoData> <rv64imAsmRecursiveDescentParser::LWUInstruction(); // LWU
            RuleParsingResult<NoData> <rv64imAsmRecursiveDescentParser::LDInstruction(); // LD
            RuleParsingResult<NoData> <rv64imAsmRecursiveDescentParser::SDInstruction(); // SD
            RuleParsingResult<NoData> <rv64imAsmRecursiveDescentParser::ADDIWInstruction(); // ADDIW
            RuleParsingResult<NoData> <rv64imAsmRecursiveDescentParser::SLLIWInstruction(); // SLLIW
            RuleParsingResult<NoData> <rv64imAsmRecursiveDescentParser::SRLIWInstruction(); // SRLIW
            RuleParsingResult<NoData> <rv64imAsmRecursiveDescentParser::SRAIWInstruction(); // SRAIW
            RuleParsingResult<NoData> <rv64imAsmRecursiveDescentParser::ADDWInstruction(); // ADDW
            RuleParsingResult<NoData> <rv64imAsmRecursiveDescentParser::SUBWInstruction(); // SUBW
            RuleParsingResult<NoData> <rv64imAsmRecursiveDescentParser::SLLWInstruction(); // SLLW
            RuleParsingResult<NoData> <rv64imAsmRecursiveDescentParser::SRLWInstruction(); // SRLW
            RuleParsingResult<NoData> <rv64imAsmRecursiveDescentParser::SRAWInstruction(); // SRAW
            RuleParsingResult<NoData> <rv64imAsmRecursiveDescentParser::SLLIInstruction(); // SLLI
            RuleParsingResult<NoData> <rv64imAsmRecursiveDescentParser::SRLIInstruction(); // SRLI
            RuleParsingResult<NoData> <rv64imAsmRecursiveDescentParser::SRAIInstruction(); // SRAI
           \s
            */
            // «visitor.resultType( AsmType.String )» Literal(std::string toParse);
            // «visitor.resultType( AsmType.Expression )» BuiltinExpression();
                
        public:
            rv64imAsmRecursiveDescentParser(MCAsmLexer &lexer, MCAsmParser &parser, OperandVector& operands)
                : Lexer(lexer), Parser(parser), Operands(operands) {
            }
                
            //RuleParsingResult<NoData> EOL();
            RuleParsingResult<NoData> ParseStatement();
            RuleParsingResult<uint64_t> ParseRegister();
        };
                
        }
                
        #endif // LLVM_LIB_TARGET_rv64im_ASMPARSER_H
        """.trim().lines(), output);
  }
}