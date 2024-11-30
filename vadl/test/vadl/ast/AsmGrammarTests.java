package vadl.ast;

import static vadl.ast.AstTestUtils.verifyPrettifiedAst;

import org.junit.jupiter.api.Test;

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
            A@typeA :
              B@typeB
            ;
          }
        """;
    verifyPrettifiedAst(VadlParser.parse(inputWrappedByValidAsmDescription(prog)));
  }

  @Test
  void assignNonTerminalToAttribute() {
    var prog = """
          grammar = {
            A@typeA :
              attributeB = B@typeB
            ;
          }
        """;
    verifyPrettifiedAst(VadlParser.parse(inputWrappedByValidAsmDescription(prog)));
  }

  @Test
  void assignStringLiteralToAttribute() {
    var prog = """
          grammar = {
            A@typeA :
              attributeB = "B" @typeB
            ;
          }
        """;
    verifyPrettifiedAst(VadlParser.parse(inputWrappedByValidAsmDescription(prog)));
  }

  @Test
  void assignMultipleAttributes() {
    var prog = """
          grammar = {
            A@typeA :
              attributeB = "B" @typeB
              attributeC = C @typeC
            ;
          }
        """;
    verifyPrettifiedAst(VadlParser.parse(inputWrappedByValidAsmDescription(prog)));
  }

  @Test
  void nonTerminalWithEmptyParameters() {
    var prog = """
          grammar = {
            A@typeA :
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
            A@typeA :
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
            A@typeA :
              B<C@typeC,D@typeD> @typeB
            ;
          }
        """;
    verifyPrettifiedAst(VadlParser.parse(inputWrappedByValidAsmDescription(prog)));
  }

  @Test
  void stringLiteralGrammarRule() {
    var prog = """
          grammar = {
            A@typeA :
              "Literal" @typeB
            ;
          }
        """;
    verifyPrettifiedAst(VadlParser.parse(inputWrappedByValidAsmDescription(prog)));
  }

  @Test
  void groupGrammarRule() {
    var prog = """
          grammar = {
            A@typeA :
              ("Literal" @typeB C@typeC)
            ;
          }
        """;
    verifyPrettifiedAst(VadlParser.parse(inputWrappedByValidAsmDescription(prog)));
  }

  @Test
  void nestedGroupGrammarRule() {
    var prog = """
          grammar = {
            A@typeA :
              (
                "Literal" @typeB C@typeC
                (D E) @ typeDE
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
            A@typeA :
              B
            ;
            C :
              D@typeD
            ;
          }
        """;
    verifyPrettifiedAst(VadlParser.parse(inputWrappedByValidAsmDescription(prog)));
  }

  @Test
  void simpleAlternativesGrammarRule() {
    var prog = """
          grammar = {
            A@typeA :
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
            A@typeA :
              B | (C@c D) | F<Int>@f
            ;
          }
        """;
    verifyPrettifiedAst(VadlParser.parse(inputWrappedByValidAsmDescription(prog)));
  }

  @Test
  void alternativesWithGroupsAndNestedAlternatives() {
    var prog = """
          grammar = {
            A@typeA :
              B | (C@c | D | G<>@g) | F<Int>@f
            ;
          }
        """;
    verifyPrettifiedAst(VadlParser.parse(inputWrappedByValidAsmDescription(prog)));
  }

  @Test
  void alternativesWithAttributesAndGroups() {
    var prog = """
          grammar = {
            A@typeA :
              attrB = B | (C@c | attrD = "D" | G<>@g) | F<Int>@f
            ;
          }
        """;
    verifyPrettifiedAst(VadlParser.parse(inputWrappedByValidAsmDescription(prog)));
  }

  @Test
  void localVarGrammarRule() {
    var prog = """
          grammar = {
            A@typeA : var tmp = null @operand
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
            A@typeA :
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
            A@typeA :
              B
              | ( ?(LaIdIn("CA","CB")) C@c | D | G<>@g)
              | F<Int>@f
            ;
          }
        """;
    verifyPrettifiedAst(VadlParser.parse(inputWrappedByValidAsmDescription(prog)));
  }

  @Test
  void repetitionGrammarRule() {
    var prog = """
          grammar = {
            A@typeA :
              B {C@c}
            ;
          }
        """;
    verifyPrettifiedAst(VadlParser.parse(inputWrappedByValidAsmDescription(prog)));
  }

  @Test
  void repetitionInNestedAlternatives() {
    var prog = """
          grammar = {
            A@typeA :
              B
              | ( ?(LaIdIn("CA","CB")) C@c | D {D} | G<>@g)
              | F<Int>@f
            ;
          }
        """;
    verifyPrettifiedAst(VadlParser.parse(inputWrappedByValidAsmDescription(prog)));
  }

  @Test
  void repetitionContainingNestedAlternatives() {
    var prog = """
          grammar = {
              A@typeA :
                {
                  B
                  | ( ?(LaIdIn("CA","CB")) C@c | D {D} | G<>@g)
                  | F<Int>@f
                }
              ;
            }
        """;
    verifyPrettifiedAst(VadlParser.parse(inputWrappedByValidAsmDescription(prog)));
  }
}
