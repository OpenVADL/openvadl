package vadl.viam.graph;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static vadl.viam.graph.GraphMatchers.activeIn;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vadl.types.Type;
import vadl.viam.ConstantValue;
import vadl.viam.graph.control.EndNode;
import vadl.viam.graph.control.StartNode;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.InstrParamNode;
import vadl.viam.graph.dependency.SideEffectNode;
import vadl.viam.graph.dependency.WriteRegNode;
import vadl.viam.helper.TestGraph;
import vadl.viam.helper.TestNodes.Plain;
import vadl.viam.helper.TestNodes.PlainUnique;
import vadl.viam.helper.TestNodes.WithDataUnique;
import vadl.viam.helper.TestNodes.WithTwoInputs;
import vadl.viam.helper.TestNodes.WithTwoInputsUnique;


/**
 * The GraphBuildingTests class is a test class that contains several test
 * methods for validating the functionality of the Graph class.
 * It performs tests related to adding nodes to the graph and checking node duplication.
 *
 * @see Graph
 * @see Node
 */
public class GraphBuildingTests {

  TestGraph testGraph;

  @BeforeEach
  public void setUp() {
    testGraph = new TestGraph("VerificationTestGraph");
  }

  @Test
  void add_SingleNode_Success() {
    var node = testGraph.add(new Plain());
    assertNotNull(node);
    assertThat(testGraph.getNodes().count(), equalTo(1L));
  }

  @Test
  void add_MultipleNodes_Success() {
    testGraph.add(new Plain());
    testGraph.add(new Plain());
    testGraph.add(new Plain());
    assertThat(testGraph.getNodes().count(), equalTo(3L));

    assertThat(testGraph.getNodes().toList(), everyItem(activeIn(testGraph)));
  }

  @Test
  void add_MultipleUniqueNodes_Success() {
    testGraph.add(new PlainUnique());
    testGraph.add(new PlainUnique());
    testGraph.add(new PlainUnique());
    assertThat(testGraph.getNodes().count(), equalTo(1L));

    assertThat(testGraph.getNodes().toList(), everyItem(activeIn(testGraph)));
  }

  @Test
  void add_WithUniqueInputsNested_Success() {
    var d1 = new WithDataUnique(1);
    var d2 = new WithDataUnique(2);
    d1 = testGraph.add(d1);
    d2 = testGraph.add(d2);
    var n1 = new WithTwoInputsUnique(d1, d2);
    n1 = testGraph.add(n1);


    var n2 = new WithTwoInputsUnique(
        new WithDataUnique(1),
        new WithDataUnique(2)
    );
    var n2return = testGraph.addWithInputs(n2);


    assertNotEquals(n2, n2return);
    assertEquals(n1, n2return);
    assertEquals(n1.inputs().toList(), n2.inputs().toList());
  }

  @Test
  void add_WithUniqueInputsNested2_Success() {
    var d1 = new WithDataUnique(1);
    var d2 = new WithDataUnique(2);
    d1 = testGraph.add(d1);
    d2 = testGraph.add(d2);
    var n1 = new WithTwoInputsUnique(d1, d2);
    n1 = testGraph.add(n1);


    var n2 = new WithTwoInputs(
        new WithDataUnique(1),
        new WithDataUnique(2)
    );
    var n2return = testGraph.addWithInputs(n2);


    assertEquals(n2, n2return);
    assertEquals(n1.inputs().toList(), n2.inputs().toList());
  }

  @Test
  void add_twoTimes_Failure() {
    var n = testGraph.add(new Plain());
    var exc = assertThrows(ViamGraphError.class, () -> testGraph.add(n));
    assertThat(exc.getMessage(), containsString("node is not uninitialized"));
  }

  @Test
  void add_twoTimesUnique_Failure() {
    var n = testGraph.add(new PlainUnique());
    var exc = assertThrows(ViamGraphError.class, () -> testGraph.add(n));
    assertThat(exc.getMessage(), containsString("node is not uninitialized"));
  }

  @Test
  void add_withNodeListInput_Failure() {
    var sideEffects = new NodeList<SideEffectNode>(
        new WriteRegNode(
            new InstrParamNode("testReg"),
            new ConstantNode(ConstantValue.of(2, Type.sInt(32)))
        ),
        new WriteRegNode(
            new InstrParamNode("testReg"),
            new ConstantNode(ConstantValue.of(2, Type.sInt(32)))
        )
    );
    var end = testGraph.addWithInputs(new EndNode(sideEffects));
    testGraph.add(new StartNode(end));

    assertEquals(testGraph.getNodes().count(), 5);
  }


}
