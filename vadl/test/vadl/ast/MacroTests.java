package vadl.ast;

import static vadl.ast.AstTestUtils.assertAstEquality;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import vadl.error.VadlException;

public class MacroTests {

  @Test
  void singleExpressionTest() {
    var prog1 = """
        model example() : Ex =  {
          1 + 2
        }
                
        constant n = $example()
        """;
    var prog2 = "constant n = 1 + 2";

    assertAstEquality(VadlParser.parse(prog1), VadlParser.parse(prog2));
  }

  @Test
  void binaryOrderInMacroTest() {
    var prog1 = """
        model example() : Ex =  {
          1 + 2 * 3 = 8 && 7 + 9 > 10
        }
                
        constant n = $example()
        """;
    var prog2 = "constant n = ((1 + (2 * 3))  = 8) && ((7 + 9) > 10)";

    assertAstEquality(VadlParser.parse(prog1), VadlParser.parse(prog2));
  }

  @Test
  void groupingOutsideMacroTest() {
    var prog1 = """
        model example() : Ex =  {
          1 + 2
        }
               
        constant n = 3 * $example()
        """;
    var prog2 = "constant n = 3 * (1 + 2)";

    assertAstEquality(VadlParser.parse(prog1), VadlParser.parse(prog2));
  }

  @Test
  void invalidMacroReturnType() {
    var prog = """
        model example() : Int =  {
           1 + 2
        }
               
        constant n = 3 * $example()
        """;
    Assertions.assertThrows(VadlException.class, () -> VadlParser.parse(prog));
  }

  @Test
  void macroWithUnusedArguments() {
    var prog1 = """
        model example(first: Int, second: Ex) : Ex =  {
          1 + 2
        }
               
        constant n = 3 * $example(3 ; 5)
        """;
    var prog2 = "constant n = 3 * (1 + 2)";

    assertAstEquality(VadlParser.parse(prog1), VadlParser.parse(prog2));
  }

  @Test
  void invalidArgumentNumber() {
    var prog = """
        model example(arg: Ex) : Ex =  {
           1 + 2
        }
               
        constant n = 3 * $example()
        """;
    Assertions.assertThrows(VadlException.class, () -> VadlParser.parse(prog));
  }

  @Test
  void invalidProvidedArgumentType() {
    var prog = """
        model example(arg: Bool) : Ex =  {
           1 + 2
        }
               
        constant n = 3 * $example(5)
        """;
    Assertions.assertThrows(VadlException.class, () -> VadlParser.parse(prog));
  }

  @Test
  void passIdAsParameter() {
    var prog1 = """
        instruction set architecture Test = {
          format F : Bits<32> = { bits [31..0] }
          register A : Bits<32>
          model test(opName: Id, instrFormat : Id) : IsaDefs = {
            instruction $opName : $instrFormat = {
              A := bits
            }
          }
        
          $test(SET ; F)
        }
        """;

    var prog2 = """
        instruction set architecture Test = {
          format F : Bits<32> = { bits [31..0] }
          register A : Bits<32>
          instruction SET : F = {
            A := bits
          }
        }
        """;

    assertAstEquality(VadlParser.parse(prog1), VadlParser.parse(prog2));
  }

  @Test
  void validatesSymbolAvailabilityAtCallSite() {
    var prog = """
        instruction set architecture Test = {
          format F : Bits<32> = { bits [31..0] }
          register A : Bits<32>
          model test(opName: Id, instrFormat : Id) : IsaDefs = {
            instruction $opName : $instrFormat = {
              A := bots // Typo!
            }
          }
        
          $test(SET ; F)
        }
        """;

    Assertions.assertThrows(VadlException.class, () -> VadlParser.parse(prog));
  }

  @Test
  void macroProducingStatements() {
    var prog1 = """
        instruction set architecture Test = {
          format F : Bits<32> = { bits [31..0] }
          register A : Bits<32>
          register B : Bits<32>
          register C : Bits<32>
          model test(targetReg: Id, sourceReg1: Id, sourceReg2: Id) : Stats = {
            $targetReg := $sourceReg1 + $sourceReg2
          }
          instruction ADD : F = {
            $test(A ; B ; C)
          }
        }
        """;

    var prog2 = """
        instruction set architecture Test = {
          format F : Bits<32> = { bits [31..0] }
          register A : Bits<32>
          register B : Bits<32>
          register C : Bits<32>
          instruction ADD : F = {
            A := B + C
          }
        }
        """;

    assertAstEquality(VadlParser.parse(prog1), VadlParser.parse(prog2));
  }

  @Test
  void canHandleMultipleInvocations() {
    var prog1 = """
        instruction set architecture Test = {
          format F : Bits<32> = { bits [31..0] }
          format G : Bits<32> = { bits [31..0] }
          register A : Bits<32>
          model test(opName: Id, instrFormat : Id) : IsaDefs = {
            instruction $opName : $instrFormat = {
              A := bits
            }
          }
        
          $test(SET_F ; F)
          $test(SET_G ; G)
        }
        """;

    var prog2 = """
        instruction set architecture Test = {
          format F : Bits<32> = { bits [31..0] }
          format G : Bits<32> = { bits [31..0] }
          register A : Bits<32>
          instruction SET_F : F = {
            A := bits
          }
          instruction SET_G : G = {
            A := bits
          }
        }
        """;

    assertAstEquality(VadlParser.parse(prog1), VadlParser.parse(prog2));
  }
}
