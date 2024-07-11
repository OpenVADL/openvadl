package vadl.test.viam.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static vadl.test.TestUtils.findDefinitionByNameIn;

import org.junit.jupiter.api.Test;
import vadl.test.AbstractTest;
import vadl.viam.Constant;
import vadl.viam.Instruction;
import vadl.viam.graph.dependency.LetNode;
import vadl.viam.graph.dependency.SliceNode;

@SuppressWarnings({"Indentation", "LocalVariableName", "VariableDeclarationUsageDistance"})
public class SliceTest extends AbstractTest {

  @Test
  public void sliceTest() {
    var spec = runAndGetViamSpecification("graph/valid_slice.vadl");

    {
      var slice_test = findDefinitionByNameIn("Test.SLICE_TEST", spec, Instruction.class);
      var behavior = slice_test.behavior();

      // first slice
      var sliceNode = behavior.getNodes(SliceNode.class).findFirst().get();
      assertEquals(
          new Constant.BitSlice(new Constant.BitSlice.Part[] {new Constant.BitSlice.Part(5, 2)}),
          sliceNode.bitSlice());

      assertEquals("a", ((LetNode) sliceNode.value()).identifier().name());
    }
  }


}
