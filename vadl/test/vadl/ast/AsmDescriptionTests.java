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
    Assertions.assertThrows(DiagnosticList.class, () -> VadlParser.parse(prog),
        "Empty modifiers definition");
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
    Assertions.assertThrows(DiagnosticList.class, () -> VadlParser.parse(prog),
        "Empty directives definition");
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

  @Test
  void asmDescriptionWithFunctions() {
    var prog = """
          assembly description AD for ABI = {
            function minus (x : SInt<64>) -> SInt<64> = -x
            function minus32 (x : SInt<32>) -> SInt<32> = -x
        
            grammar = {
              A : a = minus32<Integer> ;
            }
          }
        """;
    verifyPrettifiedAst(VadlParser.parse(prog));
  }

  @Test
  void asmDescriptionWithInvalidFunctionDefinition() {
    var prog = """
          assembly description AD for ABI = {
            function minus (x) -> SInt<64> = -x   // argument without type
        
            grammar = {
              A : a = minus<Integer> ;
            }
          }
        """;
    Assertions.assertThrows(DiagnosticList.class, () -> VadlParser.parse(prog),
        "Invalid function definition");
  }

  @Test
  void asmDescriptionWithConstantAndFunction() {
    var prog = """
          assembly description AD for ABI = {
            constant one = 1
        
            function minusOne (x : SInt<64>) -> SInt<64> = x - one
            function minusOne32 (x : SInt<32>) -> SInt<32> = x - one
        
            grammar = {
              A : a = minusOne32<Integer> ;
            }
          }
        """;
    verifyPrettifiedAst(VadlParser.parse(prog));
  }

  @Test
  void asmDescriptionWithUsingAndFunction() {
    var prog = """
          assembly description AD for ABI = {
            using char = Bits<8>
        
            function minusOne (x : char) -> char = -x
        
            grammar = {
              A : a = minusOne32<Integer> ;
            }
          }
        """;
    verifyPrettifiedAst(VadlParser.parse(prog));
  }

  @Test
  void asmDescriptionNotAllowedFormatDefinition() {
    var prog = """
          assembly description AD for ABI = {
            format F : Bits<16> =
             { rs2 : RegIndex
             , rs1 : RegIndex
             , rd : RegIndex
             , op : Bits<4>
             }
          }
        """;
    Assertions.assertThrows(DiagnosticList.class, () -> VadlParser.parse(prog),
        "Format definition not allowed in assembly description");
  }

  @Test
  void asmDescriptionNotAllowedModelDefinition() {
    var prog = """
          assembly description AD for ABI = {
            model CreateInstr ( instr : Id , fmt: Id , behavior : Stats) : IsaDefs = {
              instruction $instr : $fmt = {
                $behavior
              }
            }
          }
        """;
    Assertions.assertThrows(DiagnosticList.class, () -> VadlParser.parse(prog),
        "Model definition not allowed in assembly description");
  }
}
