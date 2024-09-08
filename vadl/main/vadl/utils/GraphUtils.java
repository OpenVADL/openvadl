package vadl.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import vadl.viam.graph.Graph;
import vadl.viam.graph.Node;

/**
 * A collection of useful utility methods on graphs.
 */
public class GraphUtils {


  /**
   * Searches for a node of the nodeClass and ensures that there is exactly one such
   * node.
   * The found node is returned.
   *
   * @param graph     The graph to search in.
   * @param nodeClass The node types class.
   * @param <T>       The type of node.
   * @return The found node of type T.
   */
  public static <T extends Node> T getSingleNode(Graph graph, Class<T> nodeClass) {
    var nodes = graph.getNodes(nodeClass).toList();
    graph.ensure(nodes.size() == 1, "Expected one node of type %s but found %s",
        nodeClass.getName(), nodes.size());
    return nodes.get(0);
  }

  public static <T extends Node> T getSingleLeafNode(Node node, Class<T> nodeClass) {
    var leaves = getLeafNodes(node).toList();
    node.ensure(leaves.size() == 1, "Expected exactly 1 leave node, but got %s", leaves.size());
    var result = leaves.get(0);
    result.ensure(nodeClass.isInstance(result), "Expected to be of type %s", nodeClass.getName());
    return nodeClass.cast(result);
  }

  public static Stream<Node> getLeafNodes(Node node) {
    var inputs = node.inputs().toList();
    if (inputs.isEmpty()) {
      return Stream.of(node);
    }
    return inputs.stream()
        .flatMap(GraphUtils::getLeafNodes);
  }

}
