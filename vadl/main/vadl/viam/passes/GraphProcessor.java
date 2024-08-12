package vadl.viam.passes;

import java.util.HashMap;
import java.util.function.Function;
import org.jetbrains.annotations.Nullable;
import vadl.viam.graph.Graph;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;

/**
 * The GraphProcessor class is an abstract class that provides functionality for
 * processing nodes in a graph.
 *
 * <p>Extending classes should implement the processUnprocessedNode method to define the processing
 * logic for nodes.
 * The GraphProcessor class maintains a list of already processed nodes and only calls the
 * {@link #processUnprocessedNode(Node)} if the node was not yet processed.
 * To trigger processing of a node, the node should {@code accept(this)}.
 */
public abstract class GraphProcessor implements GraphVisitor<Object> {
  protected final HashMap<Node, Node> processedNodes = new HashMap<Node, Node>();

  /**
   * Process the nodes in the graph that pass the given filter.
   *
   * @param graph  The graph containing the nodes to process
   * @param filter The filter function used to determine which nodes to process
   */
  protected void processGraph(Graph graph, Function<Node, Boolean> filter) {
    for (var n : graph.getNodes()
        .filter(filter::apply)
        .toList()) {
      processNode(n);
    }
  }

  /**
   * Processes a given node in a graph. It will call {@link #processUnprocessedNode(Node)} if
   * the node was not already processed.
   * If the node was already processed, it will return the found result.
   *
   * @param toProcess The node to be processed
   * @return The processed node
   */
  protected Node processNode(Node toProcess) {
    var resultNode = processedNodes.get(toProcess);
    if (resultNode != null) {
      return resultNode;
    }
    resultNode = processUnprocessedNode(toProcess);
    processedNodes.put(toProcess, resultNode);
    return resultNode;
  }

  protected abstract Node processUnprocessedNode(Node toProcess);

  @Nullable
  @Override
  public Object visit(Node from, @Nullable Node to) {
    if (to != null) {
      processNode(to);
    }
    // result is ignored anyway
    return null;
  }
}
