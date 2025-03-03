// SPDX-FileCopyrightText : © 2025 TU Wien <vadl@tuwien.ac.at>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

package vadl.lcb.template.AsmParser;

import java.io.IOException;
import java.nio.charset.Charset;
import org.junit.jupiter.api.Assertions;
import org.testcontainers.shaded.com.google.common.io.Files;
import vadl.lcb.AbstractLcbTest;
import vadl.lcb.template.lib.Target.AsmParser.EmitAsmRecursiveDescentParserHeaderFilePass;
import vadl.pass.PassKey;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.template.AbstractTemplateRenderingPass;

public class EmitAsmRecursiveDescentParserHeaderFilePassTest extends AbstractLcbTest {
  // FIXME: re-enable when asm parser is finished
  // @Test
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
    var output = trimmed.lines().skip(4); // skip copyright notice;

    Assertions.assertLinesMatch("""
        #ifndef LLVM_LIB_TARGET_processornamevalue_ASMPARSER_H
        #define LLVM_LIB_TARGET_processornamevalue_ASMPARSER_H
        
        #include "AsmParsedOperand.h"
        #include "MCTargetDesc/processornamevalueMCExpr.h"
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
            ParsedValue<processornamevalueParsedOperand> mnemonic;
        };
        
        
        struct mnemonicrdimm {
        
          ParsedValue<processornamevalueParsedOperand> mnemonic;
          ParsedValue<processornamevalueParsedOperand> rd;
          ParsedValue<processornamevalueParsedOperand> imm;
        
        };
        struct mnemonicrdimmrs1 {
        
          ParsedValue<processornamevalueParsedOperand> mnemonic;
          ParsedValue<processornamevalueParsedOperand> rd;
          ParsedValue<processornamevalueParsedOperand> imm;
          ParsedValue<processornamevalueParsedOperand> rs1;
        
        };
        struct mnemonicrdrs1imm {
        
          ParsedValue<processornamevalueParsedOperand> mnemonic;
          ParsedValue<processornamevalueParsedOperand> rd;
          ParsedValue<processornamevalueParsedOperand> rs1;
          ParsedValue<processornamevalueParsedOperand> imm;
        
        };
        struct mnemonicrdrs1rs2 {
        
          ParsedValue<processornamevalueParsedOperand> mnemonic;
          ParsedValue<processornamevalueParsedOperand> rd;
          ParsedValue<processornamevalueParsedOperand> rs1;
          ParsedValue<processornamevalueParsedOperand> rs2;
        
        };
        struct mnemonicrdrs1sft {
        
          ParsedValue<processornamevalueParsedOperand> mnemonic;
          ParsedValue<processornamevalueParsedOperand> rd;
          ParsedValue<processornamevalueParsedOperand> rs1;
          ParsedValue<processornamevalueParsedOperand> sft;
        
        };
        struct mnemonicrdrs1shamt {
        
          ParsedValue<processornamevalueParsedOperand> mnemonic;
          ParsedValue<processornamevalueParsedOperand> rd;
          ParsedValue<processornamevalueParsedOperand> rs1;
          ParsedValue<processornamevalueParsedOperand> shamt;
        
        };
        struct mnemonicrs1rs2imm {
        
          ParsedValue<processornamevalueParsedOperand> mnemonic;
          ParsedValue<processornamevalueParsedOperand> rs1;
          ParsedValue<processornamevalueParsedOperand> rs2;
          ParsedValue<processornamevalueParsedOperand> imm;
        
        };
        struct mnemonicrs2immrs1 {
        
          ParsedValue<processornamevalueParsedOperand> mnemonic;
          ParsedValue<processornamevalueParsedOperand> rs2;
          ParsedValue<processornamevalueParsedOperand> imm;
          ParsedValue<processornamevalueParsedOperand> rs1;
        
        };
        struct funct2 {
        
          ParsedValue<processornamevalueParsedOperand> funct2;
        
        };
        struct funct3 {
        
          ParsedValue<processornamevalueParsedOperand> funct3;
        
        };
        struct funct7 {
        
          ParsedValue<processornamevalueParsedOperand> funct7;
        
        };
        struct imm {
        
          ParsedValue<processornamevalueParsedOperand> imm;
        
        };
        struct opcode {
        
          ParsedValue<processornamevalueParsedOperand> opcode;
        
        };
        struct rd {
        
          ParsedValue<processornamevalueParsedOperand> rd;
        
        };
        struct rs1 {
        
          ParsedValue<processornamevalueParsedOperand> rs1;
        
        };
        struct rs2 {
        
          ParsedValue<processornamevalueParsedOperand> rs2;
        
        };
        struct sft {
        
          ParsedValue<processornamevalueParsedOperand> sft;
        
        };
        struct zero {
        
          ParsedValue<processornamevalueParsedOperand> zero;
        
        };
        
        
        class processornamevalueAsmRecursiveDescentParser {
            MCAsmLexer &Lexer;
            MCAsmParser &Parser;
            OperandVector &Operands;
        
        //private:
            /*
           \s
            RuleParsingResult<StringRef> <processornamevalueAsmRecursiveDescentParser::IDENTIFIER(); //\s
            RuleParsingResult<StringRef> <processornamevalueAsmRecursiveDescentParser::SPACE(); // \s
            RuleParsingResult<StringRef> <processornamevalueAsmRecursiveDescentParser::LPAREN(); // (
            RuleParsingResult<StringRef> <processornamevalueAsmRecursiveDescentParser::RPAREN(); // )
            RuleParsingResult<StringRef> <processornamevalueAsmRecursiveDescentParser::COMMA(); // ,
            RuleParsingResult<NoData> <processornamevalueAsmRecursiveDescentParser::ADDInstruction(); // ADD
            RuleParsingResult<NoData> <processornamevalueAsmRecursiveDescentParser::SUBInstruction(); // SUB
            RuleParsingResult<NoData> <processornamevalueAsmRecursiveDescentParser::ANDInstruction(); // AND
            RuleParsingResult<NoData> <processornamevalueAsmRecursiveDescentParser::ORInstruction(); // OR
            RuleParsingResult<NoData> <processornamevalueAsmRecursiveDescentParser::XORInstruction(); // XOR
            RuleParsingResult<NoData> <processornamevalueAsmRecursiveDescentParser::SLTInstruction(); // SLT
            RuleParsingResult<NoData> <processornamevalueAsmRecursiveDescentParser::SLTUInstruction(); // SLTU
            RuleParsingResult<NoData> <processornamevalueAsmRecursiveDescentParser::SLLInstruction(); // SLL
            RuleParsingResult<NoData> <processornamevalueAsmRecursiveDescentParser::SRLInstruction(); // SRL
            RuleParsingResult<NoData> <processornamevalueAsmRecursiveDescentParser::SRAInstruction(); // SRA
            RuleParsingResult<NoData> <processornamevalueAsmRecursiveDescentParser::ADDIInstruction(); // ADDI
            RuleParsingResult<NoData> <processornamevalueAsmRecursiveDescentParser::ANDIInstruction(); // ANDI
            RuleParsingResult<NoData> <processornamevalueAsmRecursiveDescentParser::ORIInstruction(); // ORI
            RuleParsingResult<NoData> <processornamevalueAsmRecursiveDescentParser::XORIInstruction(); // XORI
            RuleParsingResult<NoData> <processornamevalueAsmRecursiveDescentParser::SLTIInstruction(); // SLTI
            RuleParsingResult<NoData> <processornamevalueAsmRecursiveDescentParser::SLTIUInstruction(); // SLTIU
            RuleParsingResult<NoData> <processornamevalueAsmRecursiveDescentParser::AUIPCInstruction(); // AUIPC
            RuleParsingResult<NoData> <processornamevalueAsmRecursiveDescentParser::LUIInstruction(); // LUI
            RuleParsingResult<NoData> <processornamevalueAsmRecursiveDescentParser::LBInstruction(); // LB
            RuleParsingResult<NoData> <processornamevalueAsmRecursiveDescentParser::LBUInstruction(); // LBU
            RuleParsingResult<NoData> <processornamevalueAsmRecursiveDescentParser::LHInstruction(); // LH
            RuleParsingResult<NoData> <processornamevalueAsmRecursiveDescentParser::LHUInstruction(); // LHU
            RuleParsingResult<NoData> <processornamevalueAsmRecursiveDescentParser::LWInstruction(); // LW
            RuleParsingResult<NoData> <processornamevalueAsmRecursiveDescentParser::SBInstruction(); // SB
            RuleParsingResult<NoData> <processornamevalueAsmRecursiveDescentParser::SHInstruction(); // SH
            RuleParsingResult<NoData> <processornamevalueAsmRecursiveDescentParser::SWInstruction(); // SW
            RuleParsingResult<NoData> <processornamevalueAsmRecursiveDescentParser::BEQInstruction(); // BEQ
            RuleParsingResult<NoData> <processornamevalueAsmRecursiveDescentParser::BNEInstruction(); // BNE
            RuleParsingResult<NoData> <processornamevalueAsmRecursiveDescentParser::BGEInstruction(); // BGE
            RuleParsingResult<NoData> <processornamevalueAsmRecursiveDescentParser::BGEUInstruction(); // BGEU
            RuleParsingResult<NoData> <processornamevalueAsmRecursiveDescentParser::BLTInstruction(); // BLT
            RuleParsingResult<NoData> <processornamevalueAsmRecursiveDescentParser::BLTUInstruction(); // BLTU
            RuleParsingResult<NoData> <processornamevalueAsmRecursiveDescentParser::JALInstruction(); // JAL
            RuleParsingResult<NoData> <processornamevalueAsmRecursiveDescentParser::JALRInstruction(); // JALR
            RuleParsingResult<NoData> <processornamevalueAsmRecursiveDescentParser::ECALLInstruction(); // ECALL
            RuleParsingResult<NoData> <processornamevalueAsmRecursiveDescentParser::EBREAKInstruction(); // EBREAK
            RuleParsingResult<NoData> <processornamevalueAsmRecursiveDescentParser::LWUInstruction(); // LWU
            RuleParsingResult<NoData> <processornamevalueAsmRecursiveDescentParser::LDInstruction(); // LD
            RuleParsingResult<NoData> <processornamevalueAsmRecursiveDescentParser::SDInstruction(); // SD
            RuleParsingResult<NoData> <processornamevalueAsmRecursiveDescentParser::ADDIWInstruction(); // ADDIW
            RuleParsingResult<NoData> <processornamevalueAsmRecursiveDescentParser::SLLIWInstruction(); // SLLIW
            RuleParsingResult<NoData> <processornamevalueAsmRecursiveDescentParser::SRLIWInstruction(); // SRLIW
            RuleParsingResult<NoData> <processornamevalueAsmRecursiveDescentParser::SRAIWInstruction(); // SRAIW
            RuleParsingResult<NoData> <processornamevalueAsmRecursiveDescentParser::ADDWInstruction(); // ADDW
            RuleParsingResult<NoData> <processornamevalueAsmRecursiveDescentParser::SUBWInstruction(); // SUBW
            RuleParsingResult<NoData> <processornamevalueAsmRecursiveDescentParser::SLLWInstruction(); // SLLW
            RuleParsingResult<NoData> <processornamevalueAsmRecursiveDescentParser::SRLWInstruction(); // SRLW
            RuleParsingResult<NoData> <processornamevalueAsmRecursiveDescentParser::SRAWInstruction(); // SRAW
            RuleParsingResult<NoData> <processornamevalueAsmRecursiveDescentParser::SLLIInstruction(); // SLLI
            RuleParsingResult<NoData> <processornamevalueAsmRecursiveDescentParser::SRLIInstruction(); // SRLI
            RuleParsingResult<NoData> <processornamevalueAsmRecursiveDescentParser::SRAIInstruction(); // SRAI
            RuleParsingResult<NoData> <processornamevalueAsmRecursiveDescentParser::MULInstruction(); // MUL
            RuleParsingResult<NoData> <processornamevalueAsmRecursiveDescentParser::MULHInstruction(); // MULH
            RuleParsingResult<NoData> <processornamevalueAsmRecursiveDescentParser::MULHSUInstruction(); // MULHSU
            RuleParsingResult<NoData> <processornamevalueAsmRecursiveDescentParser::MULHUInstruction(); // MULHU
            RuleParsingResult<NoData> <processornamevalueAsmRecursiveDescentParser::DIVInstruction(); // DIV
            RuleParsingResult<NoData> <processornamevalueAsmRecursiveDescentParser::DIVUInstruction(); // DIVU
            RuleParsingResult<NoData> <processornamevalueAsmRecursiveDescentParser::REMInstruction(); // REM
            RuleParsingResult<NoData> <processornamevalueAsmRecursiveDescentParser::REMUInstruction(); // REMU
            RuleParsingResult<NoData> <processornamevalueAsmRecursiveDescentParser::MULWInstruction(); // MULW
            RuleParsingResult<NoData> <processornamevalueAsmRecursiveDescentParser::DIVWInstruction(); // DIVW
            RuleParsingResult<NoData> <processornamevalueAsmRecursiveDescentParser::DIVUWInstruction(); // DIVUW
            RuleParsingResult<NoData> <processornamevalueAsmRecursiveDescentParser::REMWInstruction(); // REMW
            RuleParsingResult<NoData> <processornamevalueAsmRecursiveDescentParser::REMUWInstruction(); // REMUW
           \s
            */
            // «visitor.resultType( AsmType.String )» Literal(std::string toParse);
            // «visitor.resultType( AsmType.Expression )» BuiltinExpression();
        
        public:
            processornamevalueAsmRecursiveDescentParser(MCAsmLexer &lexer, MCAsmParser &parser, OperandVector& operands)
                : Lexer(lexer), Parser(parser), Operands(operands) {
            }
        
            //RuleParsingResult<NoData> EOL();
            RuleParsingResult<NoData> ParseStatement();
            RuleParsingResult<uint64_t> ParseRegister();
        };
        
        }
        
        #endif // LLVM_LIB_TARGET_processornamevalue_ASMPARSER_H
        """.trim().lines(), output);
  }
}