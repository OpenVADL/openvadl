package vadl.ast;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static vadl.ast.AstTestUtils.assertAstEquality;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class BinaryPrecedenceTest {

  @Test
  void twoAddTest() {
    var prog1 = "constant n = 1 + 2 + 3";
    var prog2 = "constant n = ((1 + 2) + 3)";

    assertAstEquality(VadlParser.parse(prog1), VadlParser.parse(prog2));
    assertHasBeenReordered(VadlParser.parse(prog1), VadlParser.parse(prog2));
  }

  @Test
  void fiveAddTest() {
    var prog1 = "constant n = 1 + 2 + 3 + 4 + 5 + 6";
    var prog2 = "constant n = ((((1 + 2) + 3) + 4) + 5) + 6";

    assertAstEquality(VadlParser.parse(prog1), VadlParser.parse(prog2));
    assertHasBeenReordered(VadlParser.parse(prog1), VadlParser.parse(prog2));
  }

  @Test
  void addMulTest() {
    var prog1 = "constant n = 40 + 4 * 8";
    var prog2 = "constant n = (40 + (4 * 8))";

    assertAstEquality(VadlParser.parse(prog1), VadlParser.parse(prog2));
    assertHasBeenReordered(VadlParser.parse(prog1), VadlParser.parse(prog2));
  }

  @Test
  void addMulWrongOrderTest() {
    var prog1 = "constant n = 40 + 4 * 8";
    var prog2 = "constant n = ((40 + 4) * 8)";

    Assertions.assertNotEquals(VadlParser.parse(prog1), VadlParser.parse(prog2));
    assertHasBeenReordered(VadlParser.parse(prog1), VadlParser.parse(prog2));
  }

  @Test
  void mulAddTest() {
    var prog1 = "constant n = 40 * 4 + 8";
    var prog2 = "constant n = (40 * 4) + 8";

    assertAstEquality(VadlParser.parse(prog1), VadlParser.parse(prog2));
    assertHasBeenReordered(VadlParser.parse(prog1), VadlParser.parse(prog2));
  }

  @Test
  void mulAddWrongOrderTest() {
    var prog1 = "constant n = 40 * 4 + 8";
    var prog2 = "constant n = 40 * (4 + 8)";

    Assertions.assertNotEquals(VadlParser.parse(prog1), VadlParser.parse(prog2));
    assertHasBeenReordered(VadlParser.parse(prog1), VadlParser.parse(prog2));
  }

  @Test
  void addAddMulTest() {
    var prog1 = "constant n = 1 + 2 + 3 * 4";
    var prog2 = "constant n = ((1 + 2) + (3 * 4))";

    assertAstEquality(VadlParser.parse(prog1), VadlParser.parse(prog2));
    assertHasBeenReordered(VadlParser.parse(prog1), VadlParser.parse(prog2));
  }

  @Test
  void addAddAddMulTest() {
    var prog1 = "constant n = 1 + 2 + 3 + 4 * 5";
    var prog2 = "constant n = ((1 + 2) + 3) + (4 * 5)";

    assertAstEquality(VadlParser.parse(prog1), VadlParser.parse(prog2));
    assertHasBeenReordered(VadlParser.parse(prog1), VadlParser.parse(prog2));
  }

  @Test
  void addAddMulMulTest() {
    var prog1 = "constant n = 1 + 2 + 3 * 4 * 5";
    var prog2 = "constant n = ((1 + 2) + ((3 * 4) * 5))";

    assertAstEquality(VadlParser.parse(prog1), VadlParser.parse(prog2));
    assertHasBeenReordered(VadlParser.parse(prog1), VadlParser.parse(prog2));
  }

  @Test
  void addAddMulAddAddTest() {
    var prog1 = "constant n = 1 + 2 + 3 * 4 + 5 + 6";
    var prog2 = "constant n = ((((1 + 2) + (3 * 4)) + 5) + 6)";

    assertAstEquality(VadlParser.parse(prog1), VadlParser.parse(prog2));
    assertHasBeenReordered(VadlParser.parse(prog1), VadlParser.parse(prog2));
  }

  @Test
  void addAddMulAddMulTest() {
    var prog1 = "constant n = 1 + 2 + 3 * 4 + 5 * 6";
    var prog2 = "constant n = (((1 + 2) + (3 * 4)) + (5 * 6))";

    assertAstEquality(VadlParser.parse(prog1), VadlParser.parse(prog2));
    assertHasBeenReordered(VadlParser.parse(prog1), VadlParser.parse(prog2));
  }

  @Test
  void addAddMulMulMulTest() {
    var prog1 = "constant n = 1 + 2 + 3 * 4 * 5 * 6";
    var prog2 = "constant n = ((1 + 2) + (((3 * 4) * 5) * 6))";

    assertAstEquality(VadlParser.parse(prog1), VadlParser.parse(prog2));
    assertHasBeenReordered(VadlParser.parse(prog1), VadlParser.parse(prog2));
  }

  @Test
  void addAddMulAddMullAddTest() {
    var prog1 = "constant n = 1 + 2 + 3 * 4 + 5 * 6 + 7";
    var prog2 = "constant n = ((((1 + 2) + (3 * 4)) + (5 * 6)) + 7)";

    assertAstEquality(VadlParser.parse(prog1), VadlParser.parse(prog2));
    assertHasBeenReordered(VadlParser.parse(prog1), VadlParser.parse(prog2));
  }

  @Test
  void shiftAddMulTest() {
    var prog1 = "constant n = 2 << 3 + 4 * 5";
    var prog2 = "constant n = (2 << (3 + (4 * 5)))";

    assertAstEquality(VadlParser.parse(prog1), VadlParser.parse(prog2));
    assertHasBeenReordered(VadlParser.parse(prog1), VadlParser.parse(prog2));
  }

  @Test
  void mulAddShiftTest() {
    var prog1 = "constant n = 2 * 3 + 4 >> 5";
    var prog2 = "constant n = (((2 * 3) + 4) >> 5)";

    assertAstEquality(VadlParser.parse(prog1), VadlParser.parse(prog2));
    assertHasBeenReordered(VadlParser.parse(prog1), VadlParser.parse(prog2));
  }

  @Test
  void addShiftMulAddAddCompare() {
    var prog1 = "constant n = 1 + 2 << 3 * 4 + 5 + 6 < 7";
    var prog2 = "constant n = (1 + 2) << (((3 * 4) + 5) + 6) < 7";

    assertAstEquality(VadlParser.parse(prog1), VadlParser.parse(prog2));
    assertHasBeenReordered(VadlParser.parse(prog1), VadlParser.parse(prog2));
  }

  @Test
  void compareAddMulAdd() {
    var prog1 = "constant n = 1 < 2 + 3 * 4";
    var prog2 = "constant n = (1 < (2 + (3 * 4)))";

    assertAstEquality(VadlParser.parse(prog1), VadlParser.parse(prog2));
    assertHasBeenReordered(VadlParser.parse(prog1), VadlParser.parse(prog2));
  }

  @Test
  void containingGroupExpressionTest() {
    var prog1 = "constant n = 1 * (2 + 3) << 4";
    var prog2 = "constant n = ((1 * (2 + 3)) << 4)";

    assertAstEquality(VadlParser.parse(prog1), VadlParser.parse(prog2));
    assertHasBeenReordered(VadlParser.parse(prog1), VadlParser.parse(prog2));
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
    assertHasBeenReordered(VadlParser.parse(prog1), VadlParser.parse(prog2));
  }

  @Test
  void saturatedOpsBehaveAsNormal() {
    var prog1 = """
        constant a = 2 +| 3 *# 4 -| 5 + 6
        """;
    var prog2 = """
        constant a = ((2 +| (3 *# 4)) -| 5) + 6
        """;

    assertAstEquality(VadlParser.parse(prog1), VadlParser.parse(prog2));
    assertHasBeenReordered(VadlParser.parse(prog1), VadlParser.parse(prog2));
  }

  @Test
  void repeatedInvocationDoesNotChangeResult() {
    var prog = "constant n = 1 + 2 << 3 * 4 + 5 + 6 < 7";
    var binExpr =
        (BinaryExpr) ((ConstantDefinition) VadlParser.parse(prog).definitions.get(0)).value;
    var reorderedOnce = prettyPrint(BinaryExpr.reorder(binExpr));
    var reorderedTwice = prettyPrint(BinaryExpr.reorder(binExpr));
    var reorderedThrice = prettyPrint(BinaryExpr.reorder(binExpr));
    var expected = "(((1 + 2) << (((3 * 4) + 5) + 6)) < 7)";

    assertThat(reorderedOnce, equalTo(expected));
    assertThat(reorderedTwice, equalTo(expected));
    assertThat(reorderedThrice, equalTo(expected));
  }

  private void assertHasBeenReordered(Ast... asts) {
    for (Ast ast : asts) {
      for (Definition definition : ast.definitions) {
        var constant = (ConstantDefinition) definition;
        assertHasBeenReordered(constant.value);
      }
    }
  }

  private void assertHasBeenReordered(Expr expr) {
    if (expr instanceof BinaryExpr binExpr) {
      Assertions.assertTrue(binExpr.hasBeenReordered, () -> {
        var sb = new StringBuilder();
        sb.append("Not reordered: ");
        binExpr.prettyPrint(0, sb);
        return sb.toString();
      });
      assertHasBeenReordered(binExpr.left);
      assertHasBeenReordered(binExpr.right);
    }
  }

  private String prettyPrint(Node node) {
    StringBuilder sb = new StringBuilder();
    node.prettyPrint(0, sb);
    return sb.toString();
  }
}
