package vadl.utils;

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

}
