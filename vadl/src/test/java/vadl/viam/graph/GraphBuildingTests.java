package vadl.viam.graph;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static vadl.viam.graph.GraphMatchers.activeIn;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vadl.viam.graph.helper.TestGraph;
import vadl.viam.graph.helper.TestNodes.Plain;
import vadl.viam.graph.helper.TestNodes.PlainUnique;
import vadl.viam.graph.helper.TestNodes.WithDataUnique;
import vadl.viam.graph.helper.TestNodes.WithTwoInputs;
import vadl.viam.graph.helper.TestNodes.WithTwoInputsUnique;


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
    d1 = testGraph.add(d1);
    var n1 = new WithTwoInputsUnique(d1, d2);
    n1 = testGraph.add(n1);


    var n2 = new WithTwoInputsUnique(
        new WithDataUnique(1),
        new WithDataUnique(2)
    );
    var n2return = testGraph.add(n2);


    assertNotEquals(n2, n2return);
    assertEquals(n1, n2);
    assertEquals(n1.inputs().toList(), n2.inputs().toList());
  }

  @Test
  void add_WithUniqueInputsNested2_Success() {
    var d1 = new WithDataUnique(1);
    var d2 = new WithDataUnique(2);
    d1 = testGraph.add(d1);
    d1 = testGraph.add(d1);
    var n1 = new WithTwoInputsUnique(d1, d2);
    n1 = testGraph.add(n1);


    var n2 = new WithTwoInputs(
        new WithDataUnique(1),
        new WithDataUnique(2)
    );
    var n2return = testGraph.add(n2);


    assertEquals(n2, n2return);
    assertEquals(n1.inputs().toList(), n2.inputs().toList());
  }


}
