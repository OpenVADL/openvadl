package vadl.ast;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SyntaxTypeTest {
  @Test
  void coreTypeEqualityTest() {
    Assertions.assertEquals(BasicSyntaxType.Stats(), BasicSyntaxType.Stats());
  }

  @Test
  void coreTypeSubtypeItselfTest() {
    Assertions.assertTrue(BasicSyntaxType.Stats().isSubTypeOf(BasicSyntaxType.Stats()));
  }

  @Test
  void coreTypeSubtypeDirectParentTest() {
    Assertions.assertTrue(BasicSyntaxType.Stat().isSubTypeOf(BasicSyntaxType.Stats()));
  }

  @Test
  void coreTypeSubtypeGrandparentsTest() {
    Assertions.assertTrue(BasicSyntaxType.Bin().isSubTypeOf(BasicSyntaxType.Val()));
    Assertions.assertTrue(BasicSyntaxType.Bin().isSubTypeOf(BasicSyntaxType.Lit()));
    Assertions.assertTrue(BasicSyntaxType.Bin().isSubTypeOf(BasicSyntaxType.Ex()));
  }
}

