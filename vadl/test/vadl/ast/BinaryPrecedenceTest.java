package vadl.ast;

import static vadl.ast.AstTestUtils.assertAstEquality;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class BinaryPrecedenceTest {

  @Test
  void twoAddTest() {
    var prog1 = "constant n = 1 + 2 + 3";
    var prog2 = "constant n = ((1 + 2) + 3)";

    assertAstEquality(VadlParser.parse(prog1), VadlParser.parse(prog2));
  }

  @Test
  void fiveAddTest() {
    var prog1 = "constant n = 1 + 2 + 3 + 4 + 5 + 6";
    var prog2 = "constant n = ((((1 + 2) + 3) + 4) + 5) + 6";

    assertAstEquality(VadlParser.parse(prog1), VadlParser.parse(prog2));
  }

  @Test
  void addMulTest() {
    var prog1 = "constant n = 40 + 4 * 8";
    var prog2 = "constant n = (40 + (4 * 8))";

    assertAstEquality(VadlParser.parse(prog1), VadlParser.parse(prog2));
  }

  @Test
  void addMulWrongOrderTest() {
    var prog1 = "constant n = 40 + 4 * 8";
    var prog2 = "constant n = ((40 + 4) * 8)";

    Assertions.assertNotEquals(VadlParser.parse(prog1), VadlParser.parse(prog2));
  }

  @Test
  void mulAddTest() {
    var prog1 = "constant n = 40 * 4 + 8";
    var prog2 = "constant n = (40 * 4) + 8";

    assertAstEquality(VadlParser.parse(prog1), VadlParser.parse(prog2));
  }

  @Test
  void mulAddWrongOrderTest() {
    var prog1 = "constant n = 40 * 4 + 8";
    var prog2 = "constant n = 40 * (4 + 8)";

    Assertions.assertNotEquals(VadlParser.parse(prog1), VadlParser.parse(prog2));
  }

  @Test
  void addAddMulTest() {
    var prog1 = "constant n = 1 + 2 + 3 * 4";
    var prog2 = "constant n = ((1 + 2) + (3 * 4))";

    assertAstEquality(VadlParser.parse(prog1), VadlParser.parse(prog2));
  }

  @Test
  void addAddAddMulTest() {
    var prog1 = "constant n = 1 + 2 + 3 + 4 * 5";
    var prog2 = "constant n = ((1 + 2) + 3) + (4 * 5)";

    assertAstEquality(VadlParser.parse(prog1), VadlParser.parse(prog2));
  }

  @Test
  void addAddMulMulTest() {
    var prog1 = "constant n = 1 + 2 + 3 * 4 * 5";
    var prog2 = "constant n = ((1 + 2) + ((3 * 4) * 5))";

    assertAstEquality(VadlParser.parse(prog1), VadlParser.parse(prog2));
  }

  @Test
  void addAddMulAddAddTest() {
    var prog1 = "constant n = 1 + 2 + 3 * 4 + 5 + 6";
    var prog2 = "constant n = ((((1 + 2) + (3 * 4)) + 5) + 6)";

    assertAstEquality(VadlParser.parse(prog1), VadlParser.parse(prog2));
  }

  @Test
  void addAddMulAddMulTest() {
    var prog1 = "constant n = 1 + 2 + 3 * 4 + 5 * 6";
    var prog2 = "constant n = (((1 + 2) + (3 * 4)) + (5 * 6))";

    assertAstEquality(VadlParser.parse(prog1), VadlParser.parse(prog2));
  }

  @Test
  void addAddMulMulMulTest() {
    var prog1 = "constant n = 1 + 2 + 3 * 4 * 5 * 6";
    var prog2 = "constant n = ((1 + 2) + (((3 * 4) * 5) * 6))";

    assertAstEquality(VadlParser.parse(prog1), VadlParser.parse(prog2));
  }

  @Test
  void addAddMulAddMullAddTest() {
    var prog1 = "constant n = 1 + 2 + 3 * 4 + 5 * 6 + 7";
    var prog2 = "constant n = ((((1 + 2) + (3 * 4)) + (5 * 6)) + 7)";

    assertAstEquality(VadlParser.parse(prog1), VadlParser.parse(prog2));
  }

  @Test
  void shiftAddMulTest() {
    var prog1 = "constant n = 2 << 3 + 4 * 5";
    var prog2 = "constant n = (2 << (3 + (4 * 5)))";

    assertAstEquality(VadlParser.parse(prog1), VadlParser.parse(prog2));
  }

  @Test
  void mulAddShiftTest() {
    var prog1 = "constant n = 2 * 3 + 4 >> 5";
    var prog2 = "constant n = (((2 * 3) + 4) >> 5)";

    assertAstEquality(VadlParser.parse(prog1), VadlParser.parse(prog2));
  }

  @Test
  void addShiftMulAddAddCompare() {
    var prog1 = "constant n = 1 + 2 << 3 * 4 + 5 + 6 < 7";
    var prog2 = "constant n = (1 + 2) << (((3 * 4) + 5) + 6) < 7";

    assertAstEquality(VadlParser.parse(prog1), VadlParser.parse(prog2));
  }

  @Test
  void compareAddMulAdd() {
    var prog1 = "constant n = 1 < 2 + 3 * 4";
    var prog2 = "constant n = (1 < (2 + (3 * 4)))";

    assertAstEquality(VadlParser.parse(prog1), VadlParser.parse(prog2));
  }

  @Test
  void containingGroupExpressionTest() {
    var prog1 = "constant n = 1 * (2 + 3) << 4";
    var prog2 = "constant n = ((1 * (2 + 3)) << 4)";

    assertAstEquality(VadlParser.parse(prog1), VadlParser.parse(prog2));
  }

  @Test
  void unaryWinsOverLessThan() {
    var prog1 = """
        constant a = 2
        constant b = -a < 2
        """;
    var prog2 = """
        constant a = 2
        constant b = (-a) < 2""";

    assertAstEquality(VadlParser.parse(prog1), VadlParser.parse(prog2));
  }
}
