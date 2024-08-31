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
    var configuration = getConfiguration(false);
    var testSetup = runLcb(configuration, "examples/rv3264im.vadl",
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
        #ifndef LLVM_LIB_TARGET_rv3264im_ASMPARSER_H
        #define LLVM_LIB_TARGET_rv3264im_ASMPARSER_H
                
        #include "rv3264imParsedOperand.h"
        #include "MCTargetDesc/rv3264imMCExpr.h"
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
            ParsedValue<rv3264imParsedOperand> mnemonic;
        };
                
                
        struct mnemonicrdimm {
                
          ParsedValue<rv3264imParsedOperand> mnemonic;
          ParsedValue<rv3264imParsedOperand> rd;
          ParsedValue<rv3264imParsedOperand> imm;
                
        };
        struct mnemonicrdimmrs1 {
                
          ParsedValue<rv3264imParsedOperand> mnemonic;
          ParsedValue<rv3264imParsedOperand> rd;
          ParsedValue<rv3264imParsedOperand> imm;
          ParsedValue<rv3264imParsedOperand> rs1;
                
        };
        struct mnemonicrdrs1imm {
                
          ParsedValue<rv3264imParsedOperand> mnemonic;
          ParsedValue<rv3264imParsedOperand> rd;
          ParsedValue<rv3264imParsedOperand> rs1;
          ParsedValue<rv3264imParsedOperand> imm;
                
        };
        struct mnemonicrdrs1rs2 {
                
          ParsedValue<rv3264imParsedOperand> mnemonic;
          ParsedValue<rv3264imParsedOperand> rd;
          ParsedValue<rv3264imParsedOperand> rs1;
          ParsedValue<rv3264imParsedOperand> rs2;
                
        };
        struct mnemonicrdrs1sft {
                
          ParsedValue<rv3264imParsedOperand> mnemonic;
          ParsedValue<rv3264imParsedOperand> rd;
          ParsedValue<rv3264imParsedOperand> rs1;
          ParsedValue<rv3264imParsedOperand> sft;
                
        };
        struct mnemonicrdrs1shamt {
                
          ParsedValue<rv3264imParsedOperand> mnemonic;
          ParsedValue<rv3264imParsedOperand> rd;
          ParsedValue<rv3264imParsedOperand> rs1;
          ParsedValue<rv3264imParsedOperand> shamt;
                
        };
        struct mnemonicrs1rs2imm {
                
          ParsedValue<rv3264imParsedOperand> mnemonic;
          ParsedValue<rv3264imParsedOperand> rs1;
          ParsedValue<rv3264imParsedOperand> rs2;
          ParsedValue<rv3264imParsedOperand> imm;
                
        };
        struct mnemonicrs2immrs1 {
                
          ParsedValue<rv3264imParsedOperand> mnemonic;
          ParsedValue<rv3264imParsedOperand> rs2;
          ParsedValue<rv3264imParsedOperand> imm;
          ParsedValue<rv3264imParsedOperand> rs1;
                
        };
        struct fun67 {
                
          ParsedValue<rv3264imParsedOperand> fun67;
                
        };
        struct funct3 {
                
          ParsedValue<rv3264imParsedOperand> funct3;
                
        };
        struct funct7 {
                
          ParsedValue<rv3264imParsedOperand> funct7;
                
        };
        struct imm {
                
          ParsedValue<rv3264imParsedOperand> imm;
                
        };
        struct opcode {
                
          ParsedValue<rv3264imParsedOperand> opcode;
                
        };
        struct rd {
                
          ParsedValue<rv3264imParsedOperand> rd;
                
        };
        struct rs1 {
                
          ParsedValue<rv3264imParsedOperand> rs1;
                
        };
        struct rs2 {
                
          ParsedValue<rv3264imParsedOperand> rs2;
                
        };
        struct sft {
                
          ParsedValue<rv3264imParsedOperand> sft;
                
        };
                
                
        class rv3264imAsmRecursiveDescentParser {
            MCAsmLexer &Lexer;
            MCAsmParser &Parser;
            OperandVector &Operands;
                
        private:
           \s
            // «visitor.resultType( AsmType.String )» Literal(std::string toParse);
            // «visitor.resultType( AsmType.Expression )» BuiltinExpression();
                
        public:
            rv3264imAsmRecursiveDescentParser(MCAsmLexer &lexer, MCAsmParser &parser, OperandVector& operands)
                : Lexer(lexer), Parser(parser), Operands(operands) {
            }
                
            RuleParsingResult<StringRef> IDENTIFIER();
            RuleParsingResult<NoData> ParseStatement();
            RuleParsingResult<uint64> ParseRegister();
        };
                
        }
                
        #endif // LLVM_LIB_TARGET_rv3264im_ASMPARSER_H
                """.trim().lines(), output);
  }
}