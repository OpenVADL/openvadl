package vadl.ast;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import vadl.error.Diagnostic;

public class AsmLL1CheckerTest {
  private String inputWrappedByValidAsmDescription(String input) {
    return """
          instruction set architecture ISA = {}
          application binary interface ABI for ISA = {}
        
          assembly description AD for ABI = {
            %s
          }
        """.formatted(input);
  }

  @Test
  void stringIsStartOfMultipleAlternatives() {
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
    var prog = """
          grammar = {
            RuleA : IDENTIFIER@void | Token;
            Token : "some"@void;
          }
        """;
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(inputWrappedByValidAsmDescription(prog)), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertThrows(Diagnostic.class, () -> typechecker.verify(ast));
  }

  @Test
  void validStringAlternatives() {
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

  @Test
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
}
