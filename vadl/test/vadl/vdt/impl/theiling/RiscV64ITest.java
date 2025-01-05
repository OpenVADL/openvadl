package vadl.vdt.impl.theiling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import vadl.vdt.AbstractDecisionTreeTest;
import vadl.vdt.model.Node;
import vadl.vdt.utils.Instruction;

class RiscV64ITest extends AbstractDecisionTreeTest {

  @Test
  void test_generate_tree() throws IOException {

    /* GIVEN */
    final List<Instruction> riscV = parseQemuDecoding("rv64i.decode");

    /* WHEN */
    Node tree = new TheilingDecodeTreeGenerator().generate(riscV);

    /* THEN */

    assertNotNull(tree);

    final var stats = getStats(tree);
    assertEquals(riscV.size(), stats.getNumberOfLeafNodes(),
        "Expected one leaf node per instruction");

    assertEquals(65, stats.getNumberOfNodes());
    assertEquals(3, stats.getMaxDepth());
    assertEquals(1, stats.getMinDepth());
    assertEquals(2.06, Math.round(stats.getAvgDepth() * 100.0) / 100.0);
  }

}
