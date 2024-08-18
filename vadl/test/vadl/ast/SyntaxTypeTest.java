package vadl.ast;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SyntaxTypeTest {
  @Test
  void coreTypeEqualityTest() {
    Assertions.assertEquals(BasicSyntaxType.STATS, BasicSyntaxType.STATS);
  }

  @Test
  void coreTypeSubtypeItselfTest() {
    Assertions.assertTrue(BasicSyntaxType.STATS.isSubTypeOf(BasicSyntaxType.STATS));
  }

  @Test
  void coreTypeSubtypeDirectParentTest() {
    Assertions.assertTrue(BasicSyntaxType.STAT.isSubTypeOf(BasicSyntaxType.STATS));
  }

  @Test
  void coreTypeSubtypeGrandparentsTest() {
    Assertions.assertTrue(BasicSyntaxType.BIN.isSubTypeOf(BasicSyntaxType.VAL));
    Assertions.assertTrue(BasicSyntaxType.BIN.isSubTypeOf(BasicSyntaxType.LIT));
    Assertions.assertTrue(BasicSyntaxType.BIN.isSubTypeOf(BasicSyntaxType.EX));
  }
}

