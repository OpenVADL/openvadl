package vadl.ast;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class BinaryPrecedenceTest {

  @Test
  void TwoAddTest() {
    var prog1 = "constant n = 1 + 2 + 3";
    var prog2 = "constant n = ((1 + 2) + 3)";

    Assertions.assertEquals(VadlParser.parse(prog1), VadlParser.parse(prog2));
  }

  @Test
  void FiveAddTest() {
    var prog1 = "constant n = 1 + 2 + 3 + 4 + 5 + 6";
    var prog2 = "constant n = ((((1 + 2) + 3) + 4) + 5) + 6";

    Assertions.assertEquals(VadlParser.parse(prog1), VadlParser.parse(prog2));
  }

  @Test
  void AddMulTest() {
    var prog1 = "constant n = 40 + 4 * 8";
    var prog2 = "constant n = (40 + (4 * 8))";

    Assertions.assertEquals(VadlParser.parse(prog1), VadlParser.parse(prog2));
  }

  @Test
  void AddMulWrongOrderTest() {
    var prog1 = "constant n = 40 + 4 * 8";
    var prog2 = "constant n = ((40 + 4) * 8)";

    Assertions.assertNotEquals(VadlParser.parse(prog1), VadlParser.parse(prog2));
  }

  @Test
  void MulAddTest() {
    var prog1 = "constant n = 40 * 4 + 8";
    var prog2 = "constant n = (40 * 4) + 8";

    Assertions.assertEquals(VadlParser.parse(prog1), VadlParser.parse(prog2));
  }

  @Test
  void MulAddWrongOrderTest() {
    var prog1 = "constant n = 40 * 4 + 8";
    var prog2 = "constant n = 40 * (4 + 8)";

    Assertions.assertNotEquals(VadlParser.parse(prog1), VadlParser.parse(prog2));
  }

  @Test
  void AddAddMulTest() {
    var prog1 = "constant n = 1 + 2 + 3 * 4";
    var prog2 = "constant n = ((1 + 2) + (3 * 4))";

    Assertions.assertEquals(VadlParser.parse(prog1), VadlParser.parse(prog2));
  }

  @Test
  void AddAddAddMulTest() {
    var prog1 = "constant n = 1 + 2 + 3 + 4 * 5";
    var prog2 = "constant n = ((1 + 2) + 3) + (4 * 5)";

    Assertions.assertEquals(VadlParser.parse(prog1), VadlParser.parse(prog2));
  }

  @Test
  void AddAddMulMulTest() {
    var prog1 = "constant n = 1 + 2 + 3 * 4 * 5";
    var prog2 = "constant n = ((1 + 2) + ((3 * 4) * 5))";

    Assertions.assertEquals(VadlParser.parse(prog1), VadlParser.parse(prog2));
  }

  @Test
  void AddAddMulAddAddTest() {
    var prog1 = "constant n = 1 + 2 + 3 * 4 + 5 + 6";
    var prog2 = "constant n = ((((1 + 2) + (3 * 4)) + 5) + 6)";

    Assertions.assertEquals(VadlParser.parse(prog1), VadlParser.parse(prog2));
  }

  @Test
  void AddAddMulAddMulTest() {
    var prog1 = "constant n = 1 + 2 + 3 * 4 + 5 * 6";
    var prog2 = "constant n = (((1 + 2) + (3 * 4)) + (5 * 6))";

    Assertions.assertEquals(VadlParser.parse(prog1), VadlParser.parse(prog2));
  }

  @Test
  void AddAddMulMulMulTest() {
    var prog1 = "constant n = 1 + 2 + 3 * 4 * 5 * 6";
    var prog2 = "constant n = ((1 + 2) + (((3 * 4) * 5) * 6))";

    Assertions.assertEquals(VadlParser.parse(prog1), VadlParser.parse(prog2));
  }

  @Test
  void AddAddMulAddMullAddTest() {
    var prog1 = "constant n = 1 + 2 + 3 * 4 + 5 * 6 + 7";
    var prog2 = "constant n = ((((1 + 2) + (3 * 4)) + (5 * 6)) + 7)";

    Assertions.assertEquals(VadlParser.parse(prog1), VadlParser.parse(prog2));
  }

  @Test
  void ShiftAddMulTest() {
    var prog1 = "constant n = 2 << 3 + 4 * 5";
    var prog2 = "constant n = (2 << (3 + (4 * 5)))";

    Assertions.assertEquals(VadlParser.parse(prog1), VadlParser.parse(prog2));
  }

  @Test
  void MulAddShiftTest() {
    var prog1 = "constant n = 2 * 3 + 4 >> 5";
    var prog2 = "constant n = (((2 * 3) + 4) >> 5)";

    Assertions.assertEquals(VadlParser.parse(prog1), VadlParser.parse(prog2));
  }

  @Test
  void AddShiftMulAddAddCompare() {
    var prog1 = "constant n = 1 + 2 << 3 * 4 + 5 + 6 < 7";
    var prog2 = "constant n = (1 + 2) << (((3 * 4) + 5) + 6) < 7";

    Assertions.assertEquals(VadlParser.parse(prog1), VadlParser.parse(prog2));
  }

  @Test
  void CompareAddMulAdd() {
    var prog1 = "constant n = 1 < 2 + 3 * 4";
    var prog2 = "constant n = (1 < (2 + (3 * 4)))";

    Assertions.assertEquals(VadlParser.parse(prog1), VadlParser.parse(prog2));
  }


  @Test
  void ContainingGroupExpressionTest() {
    var prog1 = "constant n = 1 * (2 + 3) << 4";
    var prog2 = "constant n = ((1 * (2 + 3)) << 4)";

    Assertions.assertEquals(VadlParser.parse(prog1), VadlParser.parse(prog2));
  }
}
