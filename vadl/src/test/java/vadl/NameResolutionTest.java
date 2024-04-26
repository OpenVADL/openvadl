package vadl;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import vadl.ast.VadlParser;
import vadl.error.VadlException;

public class NameResolutionTest {
  @Test
  void resolveSingleConstant() {
    var prog = "constant a = 13";
    Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
  }

  @Test
  void resolveTwoConstant() {
    var prog = """
      constant a = 13
      constant b = 13
    """;
    Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
  }

  @Test
  void resolveTwoOverlappingConstant() {
    var prog = """
      constant a = 13
      constant a = 13
    """;
    var thrown = Assertions.assertThrows(VadlException.class, () -> VadlParser.parse(prog), "Expected to throw name conflict");
    Assertions.assertEquals(thrown.errors.size(), 1);
  }

  @Test
  void resolveUndefinedVariable() {
    var prog = """
      constant a = b
    """;
    var thrown = Assertions.assertThrows(VadlException.class, () -> VadlParser.parse(prog), "Expected to throw unresolved variable");
    Assertions.assertEquals(thrown.errors.size(), 1);
  }

  @Test
  void resolvePreviouslyDefinedVariable() {
    var prog = """
      constant a = 13
      constant b = a
    """;
    Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
  }

  @Test
  void resolveInTheFutureDefinedVariable() {
    var prog = """
      constant b = a
      constant a = 13
    """;
    Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
  }

  // FIXME how should we solve this
  /*@Test
  void resolveCyclicDefinedVariable() {
    var prog = """
      constant a = a
    """;
    var thrown = Assertions.assertThrows(VadlException.class, () -> VadlParser.parse(prog), "Expected to throw unresolved variable");
    Assertions.assertEquals(thrown.errors.size(), 1);
  }
   */

  // FIXME how should we solve this
  /*@Test
  void resolveTwoCyclicDefinedVariables() {
    var prog = """
      constant a = b
      constant b = a
    """;
    var thrown = Assertions.assertThrows(VadlException.class, () -> VadlParser.parse(prog), "Expected to throw unresolved variable");
    Assertions.assertEquals(thrown.errors.size(), 1);
  }
   */
}
