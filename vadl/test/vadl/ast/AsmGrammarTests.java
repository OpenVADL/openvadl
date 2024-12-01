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
                B
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
              B@operand
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
              B
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
              B @invalidType
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
              (B C) @invalidType
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
              attributeB = B
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
              attributeC = C
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
              B<>
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
              B<C,D>
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
              B<C@string,D@string>
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
              ("Literal" C@operand)
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
                "Literal" C @expression
                (D E) @operand
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
              B
            ;
            C :
              D
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
              B | C
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
              B | (C@expression D) | F<Int>@operand
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
              B | (C@operand | D | G<>@operand) | F<Int>@operand
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
              attrB = B | (C | attrD = "D" | G<>) | F<Int>
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
              attrB = B
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
              attrB = B
              ( var tmp = null @operand
                C
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
              [B C]
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
              (B [C D])
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
              [ ?( LaIdEq<1>("C") )
                B C
              ]
              B
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
              B
              | ( ?(LaIdIn("CA","CB")) C | D | G<>@expression)
              | F<Int>@operand
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
              B {C@string}
            ;
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
              | ( ?(LaIdIn("CA","CB")) C | D {D} | G<>)
              | F<Int>
            ;
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
                B
                | ( ?(LaIdIn("CA","CB")) C | D {D} | G<>)
                | F<Int>
              }
            ;
          }
        """;
    verifyPrettifiedAst(VadlParser.parse(inputWrappedByValidAsmDescription(prog)));
  }

  @Test
  void doubleDefinitionOfRuleName() {
    var prog = """
          grammar = {
            A : B ;
            A : C ;
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
                C
              )
            ;
          }
        """;
    verifyPrettifiedAst(VadlParser.parse(inputWrappedByValidAsmDescription(prog)));
  }
}
