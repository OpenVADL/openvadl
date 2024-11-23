package vadl.ast;

import static vadl.ast.AstTestUtils.verifyPrettifiedAst;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import vadl.error.DiagnosticList;

public class AsmDescriptionTests {

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
  void asmDescriptionWithModifier() {
    var prog = """
          instruction set architecture ISA = {}
          application binary interface ABI for ISA = {}
        
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
          instruction set architecture ISA = {}
          application binary interface ABI for ISA = {}
        
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
          modifiers = {
          }
        
          grammar = {
            A : B ;
          }
        """;
    Assertions.assertThrows(DiagnosticList.class, () -> VadlParser.parse(
        inputWrappedByValidAsmDescription(prog)));
  }

  @Test
  void asmDescriptionWithDirective() {
    var prog = """
          directives = {
            "dir1" -> builtinDir1
          }
        
          grammar = {
            A : B ;
          }
        """;
    verifyPrettifiedAst(VadlParser.parse(inputWrappedByValidAsmDescription(prog)));
  }

  @Test
  void asmDescriptionWithMultipleDirectives() {
    var prog = """
          directives = {
            "dir1" -> builtinDir1,
            "dir2" -> builtinDir2
          }
        
          grammar = {
            A : B ;
          }
        """;
    verifyPrettifiedAst(VadlParser.parse(inputWrappedByValidAsmDescription(prog)));
  }

  @Test
  void asmDescriptionWithEmptyDirectives() {
    var prog = """
          directives = {
          }
        
          grammar = {
            A : B ;
          }
        """;
    Assertions.assertThrows(DiagnosticList.class, () -> VadlParser.parse(
        inputWrappedByValidAsmDescription(prog)));
  }

  @Test
  void asmDescriptionWithModifiersAndDirectives() {
    var prog = """
          instruction set architecture ISA = {}
          application binary interface ABI for ISA = {}
        
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

  @Test
  void asmDescriptionReferencesUnknownAbi() {
    var prog = """
          assembly description AD for ABI = {}
        """;
    Assertions.assertThrows(DiagnosticList.class, () -> VadlParser.parse(prog));
  }
}
