package vadl.ast;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SyntaxTypeTest {
  @Test
  void CoreTypeEqualityTest() {
    Assertions.assertEquals(CoreType.Stats(), CoreType.Stats());
  }

  @Test
  void CoreTypeSubtypeItselfTest() {
    Assertions.assertTrue(CoreType.Stats().isSubTypeOf(CoreType.Stats()));
  }

  @Test
  void CoreTypeSubtypeDirectParentTest() {
    Assertions.assertTrue(CoreType.Stat().isSubTypeOf(CoreType.Stats()));
  }

  @Test
  void CoreTypeSubtypeGrandparentsTest() {
    Assertions.assertTrue(CoreType.Bin().isSubTypeOf(CoreType.Val()));
    Assertions.assertTrue(CoreType.Bin().isSubTypeOf(CoreType.Lit()));
    Assertions.assertTrue(CoreType.Bin().isSubTypeOf(CoreType.Ex()));
  }
}

