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

package vadl.ast;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import vadl.error.Diagnostic;

public class AsmLL1CheckerTest {
  private final String base = """
       instruction set architecture ISA = {
        register file X : Bits<5> -> Bits<32>
        
        format Rtype : Bits<1> =
        { funct7 : Bits<1> }
        
        instruction DO : Rtype =
        {
           X(0) := 1
        }
        encoding DO = { funct7 = 0b0 }
        assembly DO = (mnemonic)
        
        pseudo instruction NOP( symbol: Bits<5>) = {
        }
        assembly NOP = (mnemonic)
      }
      application binary interface ABI for ISA = {
        pseudo return instruction = NOP
        pseudo call instruction = NOP
        pseudo local address load instruction = NOP
        alias register zero = X(0)
        stack pointer = zero
        return address = zero
        global pointer = zero
        frame pointer = zero
        thread pointer = zero
        
        return value = zero
        function argument = zero
        
        caller saved = zero
        callee saved = zero
      }
      """;

  private String inputWrappedByValidAsmDescription(String input) {
    return """
          %s
                
          assembly description AD for ABI = {
            %s
          }
        """.formatted(base, input);
  }

  /**
   * A lot of tests in this file are negative tests. Therefore, we check that an
   * {@link Diagnostic} is thrown. However, the tests are worthless when the input file
   * is broken. Therefore, we have to check that they worked before and only the added
   * changes lead to an exception. That's why we test the {@code BASE} version first.
   */
  void hasToWork() {
    var ast = Assertions.assertDoesNotThrow((() -> VadlParser.parse(base)));
    var typechecker = new TypeChecker();
    Assertions.assertDoesNotThrow(() -> typechecker.verify(ast));
  }

  @Test
  void stringIsStartOfMultipleAlternatives() {
    hasToWork();
    var prog = """
          grammar = {
            RuleA : "B" | "B" ;
          }
        """;
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(inputWrappedByValidAsmDescription(prog)), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertThrows(Diagnostic.class, () -> typechecker.verify(ast));
  }

  @Test
  void ruleIsStartOfMultipleAlternatives() {
    hasToWork();
    var prog = """
          grammar = {
            RuleA : Integer | Integer ;
          }
        """;
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(inputWrappedByValidAsmDescription(prog)), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertThrows(Diagnostic.class, () -> typechecker.verify(ast));
  }

  @Test
  void ruleIsStartOfMultipleNestedAlternatives() {
    hasToWork();
    var prog = """
          grammar = {
            RuleA : (Integer | Integer) Expression;
          }
        """;
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(inputWrappedByValidAsmDescription(prog)), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertThrows(Diagnostic.class, () -> typechecker.verify(ast));
  }

  @Test
  void optionWithDeletableContent() {
    hasToWork();
    var prog = """
          grammar = {
            RuleA : [[Integer]];
          }
        """;
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(inputWrappedByValidAsmDescription(prog)), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertThrows(Diagnostic.class, () -> typechecker.verify(ast));
  }

  @Test
  void startAndSuccessorOfOption() {
    hasToWork();
    var prog = """
          grammar = {
            RuleA : [Integer] Integer;
          }
        """;
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(inputWrappedByValidAsmDescription(prog)), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertThrows(Diagnostic.class, () -> typechecker.verify(ast));
  }

  @Test
  void startAndSuccessorOfRepetition() {
    hasToWork();
    var prog = """
          grammar = {
            RuleA : {Integer} Integer;
          }
        """;
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(inputWrappedByValidAsmDescription(prog)), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertThrows(Diagnostic.class, () -> typechecker.verify(ast));
  }

  @Test
  void firstParsableElementInParamsConflict() {
    hasToWork();
    var prog = """
          instruction set architecture ISA = {}
          application binary interface ABI for ISA = {}
                
          assembly description AD for ABI = {
            function minusOne (x : SInt<64>) -> SInt<64> = x - 1
                
            grammar = {
              RuleA :
                attr = minusOne<Integer>
                | Integer
              ;
            }
          }
        """;
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(prog), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertThrows(Diagnostic.class, () -> typechecker.verify(ast));
  }

  @Test
  void firstParsableElementInLastElementOfGroup() {
    hasToWork();
    var prog = """
          instruction set architecture ISA = {}
          application binary interface ABI for ISA = {}
                
          assembly description AD for ABI = {
            function one -> SInt<64> = 1
            function add (a: SInt<64>, b: SInt<64>) -> SInt<64> = a + b
                
            grammar = {
              RuleA :
                (
                  attr = one<>
                  attr2 = add<one,Integer>
                )
                | attr2 = Integer attr4 = Integer
              ;
            }
          }
        """;
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(prog), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertThrows(Diagnostic.class, () -> typechecker.verify(ast));
  }

  @Test
  void equalStartOfTerminalAndNonTerminal() {
    hasToWork();
    var prog = """
          grammar = {
            RuleA : Integer@void | MINUS@void;
          }
        """;
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(inputWrappedByValidAsmDescription(prog)), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertThrows(Diagnostic.class, () -> typechecker.verify(ast));
  }

  @Test
  void ambiguousStringAndTerminal() {
    hasToWork();
    var prog = """
          grammar = {
            RuleA : IDENTIFIER@void | Token;
            Token : "some"@void;
          }
        """;
    var x = inputWrappedByValidAsmDescription(prog);
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(x), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertThrows(Diagnostic.class, () -> typechecker.verify(ast));
  }

  @Test
  void validStringAlternatives() {
    hasToWork();
    var prog = """
          grammar = {
            RuleA : "A" | "B";
          }
        """;
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(inputWrappedByValidAsmDescription(prog)), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertDoesNotThrow(() -> typechecker.verify(ast));
  }

  @Test
  void semanticPredicateToSolveAlternativeConflict() {
    hasToWork();
    var prog = """
          grammar = {
            RuleA : ?(VADL::equ(1 as Bits<2>,2 as Bits<2>)) "A" | "A";
          }
        """;
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(inputWrappedByValidAsmDescription(prog)), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertDoesNotThrow(() -> typechecker.verify(ast));
  }

  @Test
  void semanticPredicateToSolveOptionConflict() {
    hasToWork();
    var prog = """
          grammar = {
            RuleA : [?(VADL::equ(1 as Bits<2>,2 as Bits<2>)) "A"] "A";
          }
        """;
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(inputWrappedByValidAsmDescription(prog)), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertDoesNotThrow(() -> typechecker.verify(ast));
  }

  @Test
  void semanticPredicateToSolveRepetitionConflict() {
    hasToWork();
    var prog = """
          grammar = {
            RuleA : {?(VADL::equ(1 as Bits<2>,2 as Bits<2>)) "A"} "A";
          }
        """;
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(inputWrappedByValidAsmDescription(prog)), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertDoesNotThrow(() -> typechecker.verify(ast));
  }

  @Test
  void semanticPredicateFollowedByAlternatives() {
    hasToWork();
    var prog = """
          grammar = {
            RuleA : [ ?(VADL::equ(1 as Bits<2>,2 as Bits<2>))
                        (?(VADL::equ(1 as Bits<2>,2 as Bits<2>)) "A"
                        | "A")
                    ] "A";
          }
        """;
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(inputWrappedByValidAsmDescription(prog)), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertDoesNotThrow(() -> typechecker.verify(ast));
  }

  @Test
  void missingSemanticPredicateFollowedByAlternatives() {
    hasToWork();
    var prog = """
          grammar = {
            RuleA : [ ?(VADL::equ(1 as Bits<2>,2 as Bits<2>)) "A" | "A"] "A";
          }
        """;
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(inputWrappedByValidAsmDescription(prog)), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertThrows(Diagnostic.class, () -> typechecker.verify(ast));
  }

  @Test
  void unnecessarySemanticPredicateInAlternatives() {
    hasToWork();
    var prog = """
          grammar = {
            RuleA : ?(VADL::equ(1 as Bits<2>,2 as Bits<2>)) "A" | "B";
          }
        """;
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(inputWrappedByValidAsmDescription(prog)), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertThrows(Diagnostic.class, () -> typechecker.verify(ast));
  }

  @Test
  void semanticPredicateShouldBeInPreviousAlternatives() {
    hasToWork();
    var prog = """
          grammar = {
            RuleA : "B" | ?(VADL::equ(1 as Bits<2>,2 as Bits<2>)) "B";
          }
        """;
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(inputWrappedByValidAsmDescription(prog)), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertThrows(Diagnostic.class, () -> typechecker.verify(ast));
  }

  @Test
  void unnecessarySemanticPredicateInOption() {
    hasToWork();
    var prog = """
          grammar = {
            RuleA : [ ?(VADL::equ(1 as Bits<2>,2 as Bits<2>)) "A" ] "B";
          }
        """;
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(inputWrappedByValidAsmDescription(prog)), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertThrows(Diagnostic.class, () -> typechecker.verify(ast));
  }

  @Test
  void unnecessarySemanticPredicateInRepetition() {
    hasToWork();
    var prog = """
          grammar = {
            RuleA : { ?(VADL::equ(1 as Bits<2>,2 as Bits<2>)) "A" } "B";
          }
        """;
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(inputWrappedByValidAsmDescription(prog)), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertThrows(Diagnostic.class, () -> typechecker.verify(ast));
  }

  @Test
  void misplacedSemanticPredicateInGroup() {
    hasToWork();
    var prog = """
          grammar = {
            RuleA : ?(VADL::equ(1 as Bits<2>,2 as Bits<2>)) "A" ?(VADL::equ(1 as Bits<2>,2 as Bits<2>)) | "A";
          }
        """;
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(inputWrappedByValidAsmDescription(prog)), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertThrows(Diagnostic.class, () -> typechecker.verify(ast));
  }

  @Test
  void conflictInOptionCausedByFollowSet() {
    hasToWork();
    var prog = """
          grammar = {
            A : B;            // Identifier in follow(A), therefore in follow(B)
            B : [Identifier]; // Identifier in follow(B) --> LL(1) conflict
            C : A Identifier; // Identifier in follow(A)
          }
        """;
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(inputWrappedByValidAsmDescription(prog)), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertThrows(Diagnostic.class, () -> typechecker.verify(ast));
  }

  @Test
  void conflictByChainedFollowSet() {
    hasToWork();
    var prog = """
          grammar = {
            A : D;
            B : [Identifier];
            C : A Identifier;
            D : E;
            E : B;
          }
        """;
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(inputWrappedByValidAsmDescription(prog)), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertThrows(Diagnostic.class, () -> typechecker.verify(ast));
  }

  @Test
  void conflictWithTerminal() {
    hasToWork();
    var prog = """
          grammar = {
            A : "+" | PLUS;
          }
        """;
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(inputWrappedByValidAsmDescription(prog)), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertThrows(Diagnostic.class, () -> typechecker.verify(ast));
  }

  // FIXME: re-enable when parameters of asm built-in functions are correctly casted
  // @Test
  void asmBuiltInUsage() {
    var prog = """
          grammar = {
            RuleA : ?( laidin(2,"A","B","C") ) "A" | "A";
          }
        """;
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(inputWrappedByValidAsmDescription(prog)), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertDoesNotThrow(() -> typechecker.verify(ast));
  }

  @Test
  void conflictInExpandedInstructionRule() {
    hasToWork();
    var prog = """
          grammar = {
            A : inst = (Register @operand) @instruction;
            B : inst = (Register @operand) @instruction;
          }
        """;
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(inputWrappedByValidAsmDescription(prog)), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertThrows(Diagnostic.class, () -> typechecker.verify(ast));
  }

  // FIXME: re-enable when parameters of asm built-in functions are correctly casted
  // @Test
  void conflictInExpandedInstructionRuleResolvedByRewriting() {
    var prog = """
          grammar = {
            Inst : inst = (
              ?(laideq(0,"r1")) A
              | B
            ) @instruction ;
                
            A : Register @operand ;
            B : Register @operand ;
          }
        """;
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(inputWrappedByValidAsmDescription(prog)), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertDoesNotThrow(() -> typechecker.verify(ast));
  }
}
