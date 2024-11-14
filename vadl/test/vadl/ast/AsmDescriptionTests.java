package vadl.ast;

import static vadl.ast.AstTestUtils.verifyPrettifiedAst;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import vadl.error.DiagnosticList;

public class AsmDescriptionTests {

  @Test
  void asmDescriptionWithModifier() {
    var prog = """
          assembly description AD for ABI = {
            modifiers = {
              "mod1" -> ISA::mod1
            }
        
            grammar = {
              A : B ;
            }
          }
        """;
    verifyPrettifiedAst(VadlParser.parse(prog));
  }

  @Test
  void asmDescriptionWithMultipleModifiers() {
    var prog = """
          assembly description AD for ABI = {
            modifiers = {
              "mod1" -> ISA::mod1,
              "mod2" -> ISA::mod2,
              "mod3" -> ISA::mod3
            }
        
            grammar = {
              A : B ;
            }
          }
        """;
    verifyPrettifiedAst(VadlParser.parse(prog));
  }

  @Test
  void asmDescriptionWithEmptyModifiers() {
    var prog = """
          assembly description AD for ABI = {
            modifiers = {
            }
        
            grammar = {
              A : B ;
            }
          }
        """;
    Assertions.assertThrows(DiagnosticList.class, () -> VadlParser.parse(prog));
  }

  @Test
  void asmDescriptionWithDirective() {
    var prog = """
          assembly description AD for ABI = {
            directives = {
              "dir1" -> builtinDir1
            }
        
            grammar = {
              A : B ;
            }
          }
        """;
    verifyPrettifiedAst(VadlParser.parse(prog));
  }

  @Test
  void asmDescriptionWithMultipleDirectives() {
    var prog = """
          assembly description AD for ABI = {
            directives = {
              "dir1" -> builtinDir1,
              "dir2" -> builtinDir2
            }
        
            grammar = {
              A : B ;
            }
          }
        """;
    verifyPrettifiedAst(VadlParser.parse(prog));
  }

  @Test
  void asmDescriptionWithEmptyDirectives() {
    var prog = """
          assembly description AD for ABI = {
            directives = {
            }
        
            grammar = {
              A : B ;
            }
          }
        """;
    Assertions.assertThrows(DiagnosticList.class, () -> VadlParser.parse(prog));
  }

  @Test
  void asmDescriptionWithModifiersAndDirectives() {
    var prog = """
          assembly description AD for ABI = {
            modifiers = {
              "mod1" -> ISA::mod1,
              "mod2" -> ISA::mod2,
              "mod3" -> ISA::mod3
            }
        
            directives = {
              "dir1" -> builtinDir1,
              "dir2" -> builtinDir2
            }
        
            grammar = {
              A : B ;
            }
          }
        """;
    verifyPrettifiedAst(VadlParser.parse(prog));
  }
}
