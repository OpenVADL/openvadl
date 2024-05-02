package vadl.viam.graph;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vadl.viam.helper.TestGraph;
import vadl.viam.helper.TestNodes.Plain;
import vadl.viam.helper.TestNodes.WithInput;
import vadl.viam.helper.TestNodes.WithSuccessor;

/**
 * The NodeVerificationTests class is a test class that verifies the behavior of the Node class.
 * It contains test methods for different scenarios.
 *
 * @see Node
 */
public class NodeVerificationTests {


  TestGraph testGraph;

  @BeforeEach
  public void setUp() {
    testGraph = new TestGraph("VerificationTestGraph");
  }

  @Test
  void verify_UnregisteredNode_Failure() {
    var node = new Plain();
    var exc = assertThrows(ViamGraphError.class, node::verify);
    assertThat(exc.getMessage(), containsString("not active"));
  }

  @Test
  void verify_PlainNodeInGraph_Success() {
    var node = new Plain();
    testGraph.add(node);
    node.verify();
  }


  @Test
  void verify_NodeInputNotActive_Failure() {
    var input = new Plain();
    testGraph.add(input);
    var node = new WithInput(input);
    node.initialize(testGraph);

    // remove input again (without checks)
    testGraph.remove(input);

    var exc = assertThrows(ViamGraphError.class, node::verify);
    assert (exc.node() == node);
    assertThat(exc.getMessage(), containsString("input is not active"));
  }

  @Test
  void verify_NodeInputActive_Success() {
    var input = new Plain();
    testGraph.add(input);
    var node = new WithInput(input);
    testGraph.add(node);

    node.verify();
  }

  @Test
  void verify_NodeSuccessorNotActive_Failure() {
    var successor = new Plain();
    testGraph.add(successor);
    var node = new WithSuccessor(successor);
    node.initialize(testGraph);

    // remove successor again (without checks)
    testGraph.remove(successor);

    var exc = assertThrows(ViamGraphError.class, node::verify);
    assert (exc.node() == node);
    assertThat(exc.getMessage(), containsString("successor is not active"));
  }

  @Test
  void verify_NodeSuccessorActive_Success() {
    var successor = new Plain();
    testGraph.add(successor);
    var node = new WithSuccessor(successor);
    node.initialize(testGraph);

    node.verify();
  }

  @Test
  void verify_NodeUserNotActive_Failure() {
    var input = new Plain();
    testGraph.add(input);
    var user = new WithInput(input);
    testGraph.add(user);
    input.addUsage(user);

    // remove user from graph
    testGraph.remove(user);

    var exc = assertThrows(ViamGraphError.class, input::verify);
    assert (exc.node() == input);
    assertThat(exc.getMessage(), containsString("usage is not active"));
  }

  @Test
  void verify_NodeInputDoesNotKnowUser_Success() {
    var input = new Plain();
    testGraph.add(input);
    var user = new WithInput(input);
    testGraph.add(user);

    input.removeUsage(user);

    var exc = assertThrows(ViamGraphError.class, user::verify);
    assert (exc.node() == user);
    assertThat(exc.getMessage(), containsString("node is not a user of input"));
  }

  @Test
  void verify_NodeUserDoesNotKnowInput_Failure() {
    var input = new Plain();
    testGraph.add(input);
    var user = new WithInput(input);
    testGraph.add(user);

    // dumb override of input
    user.applyOnInputsUnsafe((s, t) -> new Plain());

    var exc = assertThrows(ViamGraphError.class, input::verify);
    assert (exc.node() == input);
    assertThat(exc.getMessage(), containsString("user does not contain this node as input"));
  }

  @Test
  void verify_NodeUserApplyOnInput_Success() {
    var input = new Plain();
    testGraph.add(input);
    var user = new WithInput(input);
    testGraph.add(user);

    var inputNew = new Plain();
    testGraph.add(inputNew);
    // dumb override of input
    user.applyOnInputs((s, t) -> inputNew);

    input.verify();
    user.verify();
    inputNew.verify();

    assertThat(user.inputs().count(), equalTo(1L));
  }


}
