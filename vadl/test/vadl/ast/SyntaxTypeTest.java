package vadl.ast;

import java.util.List;
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

  @Test
  void recordTypeParentTest() {
    var recordA = new RecordType("A", List.of(
        new RecordType.Entry("hello", BasicSyntaxType.ID),
        new RecordType.Entry("world", BasicSyntaxType.STAT)
    ));
    var recordB = new RecordType("B", List.of(
        new RecordType.Entry("names", BasicSyntaxType.EX),
        new RecordType.Entry("dontmatter", BasicSyntaxType.STATS)
    ));

    Assertions.assertTrue(recordA.isSubTypeOf(recordA), "A <: A");
    Assertions.assertTrue(recordB.isSubTypeOf(recordB), "B <: B");
    Assertions.assertTrue(recordA.isSubTypeOf(recordB), "A <: B");
    Assertions.assertFalse(recordB.isSubTypeOf(recordA), "B !<: A");
  }
}
