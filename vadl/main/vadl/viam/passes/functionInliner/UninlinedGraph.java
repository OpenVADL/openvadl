package vadl.viam.passes.functionInliner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import vadl.viam.Definition;
import vadl.viam.graph.Graph;
import vadl.viam.graph.Node;

/**
 * Child class of {@link Graph} to indicate that the inlining was already done but
 * this graph has been not inlined.
 */
public class UninlinedGraph extends Graph {
  public UninlinedGraph(String name, List<Node> nodes, Definition parentDefinition) {
    super(name, new ArrayList<>(nodes));
    setParentDefinition(parentDefinition);
  }

  public UninlinedGraph(Graph graph, Definition parentDefinition) {
    this(graph.name, graph.getNodes().toList(), parentDefinition);
  }

  @Override
  protected Graph createEmptyInstance(String name, Definition parentDefinition) {
    return new UninlinedGraph(name, Collections.emptyList(), parentDefinition);
  }
}
