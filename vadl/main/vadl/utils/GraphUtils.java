package vadl.utils;

import vadl.viam.graph.Graph;
import vadl.viam.graph.Node;

public class GraphUtils {


  public static <T extends Node> T getSingleNode(Graph graph, Class<T> nodeClass) {
    var nodes = graph.getNodes(nodeClass).toList();
    graph.ensure(nodes.size() == 1, "Expected one node of type %s but found %s",
        nodeClass.getName(), nodes.size());
    return nodes.get(0);
  }

}
