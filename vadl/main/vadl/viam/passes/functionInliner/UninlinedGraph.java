package vadl.viam.passes.functionInliner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import vadl.viam.graph.Graph;
import vadl.viam.graph.Node;

/**
 * Child class of {@link Graph} to indicate that the inlining was already done but
 * this graph has been not inlined.
 */
public class UninlinedGraph extends Graph {
  public UninlinedGraph(String name, List<Node> nodes) {
    super(name, new ArrayList<>(nodes));
  }

  public UninlinedGraph(Graph graph) {
    this(graph.name, graph.getNodes().toList());
  }

  @Override
  protected Graph createEmptyInstance(String name) {
    return new UninlinedGraph(name, Collections.emptyList());
  }
}
