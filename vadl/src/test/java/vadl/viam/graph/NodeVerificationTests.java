package vadl.viam.graph;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vadl.viam.graph.helper.TestGraph;
import vadl.viam.graph.helper.TestNodePlain;

public class NodeVerificationTests {


  TestGraph testGraph;

  @BeforeEach
  public void setUp() {
    testGraph = new TestGraph();
  }

  @Test
  void verifyUnregisteredNode_shouldFail() {
    var node = new TestNodePlain();
    var exc = assertThrows(ViamGraphError.class, node::verify);
    assert (exc.getMessage().contains("not active"));
  }

  
}
