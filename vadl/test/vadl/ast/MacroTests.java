package vadl.ast;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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
          1 + 2 * 3  == 8 && 7 + 9 > 10
        }
                
        constant n = $example()
        """;
    var prog2 = "constant n = ((1 + (2 * 3))  == 8) && ((7 + 9) > 10)";

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
}
