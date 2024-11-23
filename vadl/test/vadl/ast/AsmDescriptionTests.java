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
          instruction set architecture ISA = {
            relocation HI ( symbol : Bits <32> ) -> Bits <16> = ( symbol >> 16 ) & 0xFFFF
          }
          application binary interface ABI for ISA = {}
        
          assembly description AD for ABI = {
            modifiers = {
              "hi" -> ISA::HI
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
          instruction set architecture ISA = {
            relocation HI ( symbol : Bits <32> ) -> Bits <16> = ( symbol >> 16 ) & 0xFFFF
            relocation LO ( symbol : Bits <32> ) -> Bits <12> =   symbol         & 0xFFF
          }
          application binary interface ABI for ISA = {}
        
          assembly description AD for ABI = {
            modifiers = {
              "hi" -> ISA::HI,
              "lo" -> ISA::LO
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
          instruction set architecture ISA = {
            relocation HI ( symbol : Bits <32> ) -> Bits <16> = ( symbol >> 16 ) & 0xFFFF
            relocation LO ( symbol : Bits <32> ) -> Bits <12> =   symbol         & 0xFFF
          }
          application binary interface ABI for ISA = {}
        
          assembly description AD for ABI = {
            modifiers = {
              "hi" -> ISA::HI,
              "lo" -> ISA::LO
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

  @Test
  void asmModifiersReferenceUnknownRelocations() {
    var prog = """
          instruction set architecture ISA = {}
          application binary interface ABI for ISA = {}
          assembly description AD for ABI = {
            modifiers = {
              "hi" -> ISA::HI
            }
          }
        """;
    Assertions.assertThrows(DiagnosticList.class, () -> VadlParser.parse(prog));
  }
}
