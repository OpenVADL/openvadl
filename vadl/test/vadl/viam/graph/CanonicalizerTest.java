package vadl.viam.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static vadl.viam.helper.TestGraphUtils.binaryOp;
import static vadl.viam.helper.TestGraphUtils.bits;
import static vadl.viam.helper.TestGraphUtils.cast;
import static vadl.viam.helper.TestGraphUtils.intS;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vadl.types.BuiltInTable;
import vadl.types.Type;
import vadl.viam.Constant;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.visualize.DotGraphVisualizer;
import vadl.viam.helper.TestGraph;

public class CanonicalizerTest {

  TestGraph testGraph;

  @BeforeEach
  public void setUp() {
    testGraph = new TestGraph("VerificationTestGraph");
  }

  @AfterEach
  public void tearDown() {
    System.out.println(
        new DotGraphVisualizer()
            .load(testGraph)
            .visualize()
    );
  }

  @Test
  void constantEvaluation() {
    testGraph.addWithInputs(
        binaryOp(BuiltInTable.ADD,
            bits(5, 10),
            bits(10, 10))
    );


    testGraph.verify();

    assertEquals(3, testGraph.getNodes().count());

    Canonicalizer.canonicalize(testGraph);

    assertEquals(1, testGraph.getNodes().count());
    assertEquals(15,
        ((Constant.Value) ((ConstantNode) testGraph.getNodes().findFirst()
            .get()).constant()).value().intValue());
  }

  @Test
  void constantEvaluationWithCast() {

    var addition = testGraph.addWithInputs(
        binaryOp(BuiltInTable.ADD,
            bits(5, 10),
            bits(-10, 10))
    );
    testGraph.add(cast(addition, Type.unsignedInt(5)));

    testGraph.verify();

    assertEquals(4, testGraph.getNodes().count());

    Canonicalizer.canonicalize(testGraph);

    assertEquals(1, testGraph.getNodes().count());
    assertEquals(27,
        ((Constant.Value) ((ConstantNode) testGraph.getNodes().findFirst()
            .get()).constant()).integer().intValue());

    testGraph.verify();
  }

  @Test
  void constantEvaluationSubGraph() {

    var addition = testGraph.addWithInputs(
        binaryOp(BuiltInTable.ADD,
            bits(5, 10),
            bits(-10, 10))
    );
    testGraph.add(cast(addition, Type.unsignedInt(5)));

    testGraph.verify();

    assertEquals(4, testGraph.getNodes().count());

    Canonicalizer.canonicalizeSubGraph(addition);

    assertEquals(2, testGraph.getNodes().count());
    assertEquals(-5,
        ((Constant.Value) testGraph.getNodes(ConstantNode.class).findFirst()
            .get().constant()).integer().intValue());

    testGraph.verify();
  }


}
