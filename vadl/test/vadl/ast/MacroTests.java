package vadl.ast;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import vadl.error.VadlException;

public class MacroTests {

  @Test
  void SingleExpressionTest() {
    var prog1 = """
        model example() : Ex =  {
          1 + 2
        }
                
        constant n = $example()
        """;
    var prog2 = "constant n = 1 + 2";

    Assertions.assertEquals(VadlParser.parse(prog1), VadlParser.parse(prog2));
  }

  @Test
  void BinaryOrderInMacroTest() {
    var prog1 = """
        model example() : Ex =  {
          1 + 2 * 3 = 8 && 7 + 9 > 10
        }
                
        constant n = $example()
        """;
    var prog2 = "constant n = ((1 + (2 * 3))  = 8) && ((7 + 9) > 10)";

    Assertions.assertEquals(VadlParser.parse(prog1), VadlParser.parse(prog2));
  }

  @Test
  void GroupingOutsideMacroTest() {
    var prog1 = """
        model example() : Ex =  {
          1 + 2
        }
               
        constant n = 3 * $example()
        """;
    var prog2 = "constant n = 3 * (1 + 2)";

    Assertions.assertEquals(VadlParser.parse(prog1), VadlParser.parse(prog2));
  }

  @Test
  void InvalidMacroReturnType() {
    var prog = """
        model example() : Int =  {
           1 + 2
        }
               
        constant n = 3 * $example()
        """;
    Assertions.assertThrows(VadlException.class, () -> VadlParser.parse(prog));
  }

  @Test
  void MacroWithUnusedArguments() {
    var prog1 = """
        model example(first: Int, second: Ex) : Ex =  {
          1 + 2
        }
               
        constant n = 3 * $example(3 ; 5)
        """;
    var prog2 = "constant n = 3 * (1 + 2)";

    Assertions.assertEquals(VadlParser.parse(prog1), VadlParser.parse(prog2));
  }

  @Test
  void InvalidArgumentNumber() {
    var prog = """
        model example(arg: Ex) : Ex =  {
           1 + 2
        }
               
        constant n = 3 * $example()
        """;
    Assertions.assertThrows(VadlException.class, () -> VadlParser.parse(prog));
  }

  @Test
  void InvalidProvidedArgumentType() {
    var prog = """
        model example(arg: Bool) : Ex =  {
           1 + 2
        }
               
        constant n = 3 * $example(5)
        """;
    Assertions.assertThrows(VadlException.class, () -> VadlParser.parse(prog));
  }
}
