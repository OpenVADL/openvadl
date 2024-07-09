package vadl.ast;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SyntaxTypeTest {
  @Test
  void coreTypeEqualityTest() {
    Assertions.assertEquals(CoreType.Stats(), CoreType.Stats());
  }

  @Test
  void coreTypeSubtypeItselfTest() {
    Assertions.assertTrue(CoreType.Stats().isSubTypeOf(CoreType.Stats()));
  }

  @Test
  void coreTypeSubtypeDirectParentTest() {
    Assertions.assertTrue(CoreType.Stat().isSubTypeOf(CoreType.Stats()));
  }

  @Test
  void coreTypeSubtypeGrandparentsTest() {
    Assertions.assertTrue(CoreType.Bin().isSubTypeOf(CoreType.Val()));
    Assertions.assertTrue(CoreType.Bin().isSubTypeOf(CoreType.Lit()));
    Assertions.assertTrue(CoreType.Bin().isSubTypeOf(CoreType.Ex()));
  }
}

