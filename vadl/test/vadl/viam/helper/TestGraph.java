package vadl.viam.helper;

import vadl.viam.graph.Graph;

/**
 * The TestGraph class extends the Graph class and represents a test graph.
 * It inherits all the properties and methods from the Graph class.
 */
public class TestGraph extends Graph {

  public TestGraph() {
    super("Test graph");
  }

  public TestGraph(String name) {
    super(name, new DummyDefinition());
  }
}
