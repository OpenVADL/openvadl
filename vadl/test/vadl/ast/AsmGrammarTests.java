package vadl.ast;

import static vadl.ast.AstTestUtils.verifyPrettifiedAst;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import vadl.error.DiagnosticList;

public class AsmGrammarTests {

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
  void nonTerminalGrammarRule() {
    var prog = """
          grammar = {
              A :
                Register
              ;
          }
        """;
    verifyPrettifiedAst(VadlParser.parse(inputWrappedByValidAsmDescription(prog)));
  }

  @Test
  void nonTerminalWithTypes() {
    var prog = """
          grammar = {
            A@instruction :
              ImmediateOperand@operand
            ;
          }
        """;
    verifyPrettifiedAst(VadlParser.parse(inputWrappedByValidAsmDescription(prog)));
  }

  @Test
  void ruleWithInvalidType() {
    var prog = """
          grammar = {
            A@invalidType :
              ImmediateOperand
            ;
          }
        """;
    Assertions.assertThrows(DiagnosticList.class,
        () -> VadlParser.parse(inputWrappedByValidAsmDescription(prog)));
  }

  @Test
  void operandWithInvalidType() {
    var prog = """
          grammar = {
            A :
              ImmediateOperand @invalidType
            ;
          }
        """;
    Assertions.assertThrows(DiagnosticList.class,
        () -> VadlParser.parse(inputWrappedByValidAsmDescription(prog)));
  }

  @Test
  void stringLiteralWithInvalidType() {
    var prog = """
          grammar = {
            A :
              "string" @invalidType
            ;
          }
        """;
    Assertions.assertThrows(DiagnosticList.class,
        () -> VadlParser.parse(inputWrappedByValidAsmDescription(prog)));
  }

  @Test
  void groupWithInvalidType() {
    var prog = """
          grammar = {
            A :
              (Register ImmediateOperand) @invalidType
            ;
          }
        """;
    Assertions.assertThrows(DiagnosticList.class,
        () -> VadlParser.parse(inputWrappedByValidAsmDescription(prog)));
  }

  @Test
  void assignNonTerminalToAttribute() {
    var prog = """
          grammar = {
            A@instruction :
              attributeB = Expression
            ;
          }
        """;
    verifyPrettifiedAst(VadlParser.parse(inputWrappedByValidAsmDescription(prog)));
  }

  @Test
  void assignStringLiteralToAttribute() {
    var prog = """
          grammar = {
            A:
              attributeB = "B"
            ;
          }
        """;
    verifyPrettifiedAst(VadlParser.parse(inputWrappedByValidAsmDescription(prog)));
  }

  @Test
  void assignMultipleAttributes() {
    var prog = """
          grammar = {
            A@instruction :
              attributeB = "B" @string
              attributeC = Register
            ;
          }
        """;
    verifyPrettifiedAst(VadlParser.parse(inputWrappedByValidAsmDescription(prog)));
  }

  @Test
  void nonTerminalWithEmptyParameters() {
    var prog = """
          grammar = {
            A@instruction :
              ImmediateOperand<>
            ;
          }
        """;
    verifyPrettifiedAst(VadlParser.parse(inputWrappedByValidAsmDescription(prog)));
  }

  @Test
  void nonTerminalWithParameters() {
    var prog = """
          grammar = {
            A@instruction :
              IDENTIFIER<Integer,Integer>
            ;
          }
        """;
    verifyPrettifiedAst(VadlParser.parse(inputWrappedByValidAsmDescription(prog)));
  }

  @Test
  void nonTerminalParametersWithTypecast() {
    var prog = """
          grammar = {
            A@instruction :
              IDENTIFIER<STRING@string,STRING@string>
            ;
          }
        """;
    verifyPrettifiedAst(VadlParser.parse(inputWrappedByValidAsmDescription(prog)));
  }

  @Test
  void stringLiteralGrammarRule() {
    var prog = """
          grammar = {
            A :
              "Literal" @string
            ;
          }
        """;
    verifyPrettifiedAst(VadlParser.parse(inputWrappedByValidAsmDescription(prog)));
  }

  @Test
  void groupGrammarRule() {
    var prog = """
          grammar = {
            A :
              ("Literal" STRING@operand)
            ;
          }
        """;
    verifyPrettifiedAst(VadlParser.parse(inputWrappedByValidAsmDescription(prog)));
  }

  @Test
  void nestedGroupGrammarRule() {
    var prog = """
          grammar = {
            A@instruction:
              (
                "Literal" Expression @expression
                op = (Register Register) @operand
              )
            ;
          }
        """;
    verifyPrettifiedAst(VadlParser.parse(inputWrappedByValidAsmDescription(prog)));
  }

  @Test
  void multipleGrammarRules() {
    var prog = """
          grammar = {
            A :
              IDENTIFIER
            ;
            C :
              A
            ;
          }
        """;
    verifyPrettifiedAst(VadlParser.parse(inputWrappedByValidAsmDescription(prog)));
  }

  @Test
  void simpleAlternativesGrammarRule() {
    var prog = """
          grammar = {
            A :
              STRING | ImmediateOperand
            ;
          }
        """;
    verifyPrettifiedAst(VadlParser.parse(inputWrappedByValidAsmDescription(prog)));
  }

  @Test
  void alternativesWithGroups() {
    var prog = """
          grammar = {
            A@instruction :
              Register | (Expression@expression Register) | IDENTIFIER<Integer>@operand
            ;
          }
        """;
    verifyPrettifiedAst(VadlParser.parse(inputWrappedByValidAsmDescription(prog)));
  }

  @Test
  void alternativesWithGroupsAndNestedAlternatives() {
    var prog = """
          grammar = {
            A@instruction :
              "B" | (ImmediateOperand@operand | Expression | ImmediateOperand<>@operand) | IDENTIFIER<Integer>@operand
            ;
          }
        """;
    verifyPrettifiedAst(VadlParser.parse(inputWrappedByValidAsmDescription(prog)));
  }

  @Test
  void alternativesWithAttributesAndGroups() {
    var prog = """
          grammar = {
            A:
              attrB = "B" | (STRING | attrD = "D" | STRING)
            ;
          }
        """;
    verifyPrettifiedAst(VadlParser.parse(inputWrappedByValidAsmDescription(prog)));
  }

  @Test
  void localVarGrammarRule() {
    var prog = """
          grammar = {
            A : var tmp = null @operand
              attrB = "B"
            ;
          }
        """;
    verifyPrettifiedAst(VadlParser.parse(inputWrappedByValidAsmDescription(prog)));
  }

  @Test
  void localVarInGroupGrammarRule() {
    var prog = """
          grammar = {
            A :
              attrB = "B"
              ( var tmp = null @operand
                "C"
              )
            ;
          }
        """;
    verifyPrettifiedAst(VadlParser.parse(inputWrappedByValidAsmDescription(prog)));
  }

  @Test
  void optionalGrammarRule() {
    var prog = """
          grammar = {
            A :
              ["B" "C"]
            ;
          }
        """;
    verifyPrettifiedAst(VadlParser.parse(inputWrappedByValidAsmDescription(prog)));
  }

  @Test
  void optionalInGroupGrammarRule() {
    var prog = """
          grammar = {
            A :
              (Register ["B" "C"])
            ;
          }
        """;
    verifyPrettifiedAst(VadlParser.parse(inputWrappedByValidAsmDescription(prog)));
  }

  @Test
  void semanticPredicateGrammarRule() {
    var prog = """
          grammar = {
            A :
              [ ?(VADL::equ(1 as Bits<2>, 2 as Bits<2>))
                Register "C"
              ]
              Register
            ;
          }
        """;
    verifyPrettifiedAst(VadlParser.parse(inputWrappedByValidAsmDescription(prog)));
  }

  @Test
  void semanticPredicateInNestedAlternatives() {
    var prog = """
          grammar = {
            A@instruction :
              ImmediateOperand
              | ( ?(VADL::equ(1 as Bits<2>, 2 as Bits<2>)) STRING | ImmediateOperand | Expression<>@expression)
              | IDENTIFIER<Integer>@operand
            ;
          }
        """;
    verifyPrettifiedAst(VadlParser.parse(inputWrappedByValidAsmDescription(prog)));
  }

  @Test
  void repetitionGrammarRule() {
    var prog = """
          grammar = {
            A :
              "B" {C@string}
            ;
            C : STRING;
          }
        """;
    verifyPrettifiedAst(VadlParser.parse(inputWrappedByValidAsmDescription(prog)));
  }

  @Test
  void repetitionInNestedAlternatives() {
    var prog = """
          grammar = {
            A :
              B
              | ( ?(VADL::equ(1 as Bits<2>, 2 as Bits<2>)) "C" | B  {B} | IDENTIFIER<>)
              | IDENTIFIER<Integer>
            ;
            B : "B" STRING;
          }
        """;
    verifyPrettifiedAst(VadlParser.parse(inputWrappedByValidAsmDescription(prog)));
  }

  @Test
  void repetitionContainingNestedAlternatives() {
    var prog = """
          grammar = {
            A :
              {
                "B"
                | ( ?(VADL::equ(1 as Bits<2>, 2 as Bits<2>)) "C" | B  {B} | IDENTIFIER<>)
                | IDENTIFIER<Integer>
              }
            ;
            B : "B" STRING;
          }
        """;
    verifyPrettifiedAst(VadlParser.parse(inputWrappedByValidAsmDescription(prog)));
  }

  @Test
  void doubleDefinitionOfRuleName() {
    var prog = """
          grammar = {
            A : "B" ;
            A : "C" ;
          }
        """;
    Assertions.assertThrows(DiagnosticList.class,
        () -> VadlParser.parse(inputWrappedByValidAsmDescription(prog)));
  }

  @Test
  void doubleDefinitionOfLocalVar() {
    var prog = """
          grammar = {
            A :
              var tmp = "a"
              var tmp = "b"
              C
            ;
          }
        """;
    Assertions.assertThrows(DiagnosticList.class,
        () -> VadlParser.parse(inputWrappedByValidAsmDescription(prog)));
  }

  @Test
  void innerLocalVarHidingOuterLocalVar() {
    var prog = """
          grammar = {
            A : var tmp = "a"
              ( var tmp = "b"
                "C"
              )
            ;
          }
        """;
    verifyPrettifiedAst(VadlParser.parse(inputWrappedByValidAsmDescription(prog)));
  }

  @Test
  void grammarRuleWithInvalidSymbol() {
    var prog = """
          grammar = {
            A :
              B
            ;
          }
        """;
    Assertions.assertThrows(DiagnosticList.class,
        () -> VadlParser.parse(inputWrappedByValidAsmDescription(prog)));
  }
}
