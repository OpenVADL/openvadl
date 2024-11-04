package vadl.ast;

import static vadl.ast.AstTestUtils.verifyPrettifiedAst;

import org.junit.jupiter.api.Test;

public class AsmGrammarTests {

  @Test
  void nonTerminalGrammarRule() {
    var prog = """
          assembly description AD for ABI = {
            grammar = {
              A :
                B
              ;
            }
          }
        """;
    verifyPrettifiedAst(VadlParser.parse(prog));
  }

  @Test
  void nonTerminalWithTypes() {
    var prog = """
          assembly description AD for ABI = {
            grammar = {
              A@typeA :
                B@typeB
              ;
            }
          }
        """;
    verifyPrettifiedAst(VadlParser.parse(prog));
  }

  @Test
  void assignNonTerminalToAttribute() {
    var prog = """
          assembly description AD for ABI = {
            grammar = {
              A@typeA :
                attributeB = B@typeB
              ;
            }
          }
        """;
    verifyPrettifiedAst(VadlParser.parse(prog));
  }

  @Test
  void assignStringLiteralToAttribute() {
    var prog = """
          assembly description AD for ABI = {
            grammar = {
              A@typeA :
                attributeB = "B" @typeB
              ;
            }
          }
        """;
    verifyPrettifiedAst(VadlParser.parse(prog));
  }

  @Test
  void assignMultipleAttributes() {
    var prog = """
          assembly description AD for ABI = {
            grammar = {
              A@typeA :
                attributeB = "B" @typeB
                attributeC = C @typeC
              ;
            }
          }
        """;
    verifyPrettifiedAst(VadlParser.parse(prog));
  }

  @Test
  void nonTerminalWithEmptyParameters() {
    var prog = """
          assembly description AD for ABI = {
            grammar = {
              A@typeA :
                B<>
              ;
            }
          }
        """;
    verifyPrettifiedAst(VadlParser.parse(prog));
  }

  @Test
  void nonTerminalWithParameters() {
    var prog = """
          assembly description AD for ABI = {
            grammar = {
              A@typeA :
                B<C,D>
              ;
            }
          }
        """;
    verifyPrettifiedAst(VadlParser.parse(prog));
  }

  @Test
  void nonTerminalParametersWithTypecast() {
    var prog = """
          assembly description AD for ABI = {
            grammar = {
              A@typeA :
                B<C@typeC,D@typeD> @typeB
              ;
            }
          }
        """;
    verifyPrettifiedAst(VadlParser.parse(prog));
  }

  @Test
  void stringLiteralGrammarRule() {
    var prog = """
          assembly description AD for ABI = {
            grammar = {
              A@typeA :
                "Literal" @typeB
              ;
            }
          }
        """;
    verifyPrettifiedAst(VadlParser.parse(prog));
  }

  @Test
  void groupGrammarRule() {
    var prog = """
          assembly description AD for ABI = {
            grammar = {
              A@typeA :
                ("Literal" @typeB C@typeC)
              ;
            }
          }
        """;
    verifyPrettifiedAst(VadlParser.parse(prog));
  }

  @Test
  void nestedGroupGrammarRule() {
    var prog = """
          assembly description AD for ABI = {
            grammar = {
              A@typeA :
                (
                  "Literal" @typeB C@typeC
                  (D E) @ typeDE
                )
              ;
            }
          }
        """;
    verifyPrettifiedAst(VadlParser.parse(prog));
  }

  @Test
  void multipleGrammarRules() {
    var prog = """
          assembly description AD for ABI = {
            grammar = {
              A@typeA :
                B
              ;
              C :
                D@typeD
              ;
            }
          }
        """;
    verifyPrettifiedAst(VadlParser.parse(prog));
  }
}
