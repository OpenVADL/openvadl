package vadl.viam.passes;

import java.util.HashMap;
import java.util.function.Function;
import org.jetbrains.annotations.Nullable;
import vadl.viam.ViamError;
import vadl.viam.graph.Graph;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.ViamGraphError;

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
public abstract class GraphProcessor<T> implements GraphVisitor<Object> {
  protected final HashMap<Node, T> processedNodes = new HashMap<>();

  /**
   * Process the nodes in the graph that pass the given filter.
   *
   * @param graph  The graph containing the nodes to process
   * @param filter The filter function used to determine which nodes to process
   */
  protected void processGraph(Graph graph, Function<Node, Boolean> filter) {
    graph.getNodes()
        .filter(filter::apply)
        .forEach(this::processNode);
  }

  /**
   * Processes a given node in a graph. It will call {@link #processUnprocessedNode(Node)} if
   * the node was not already processed.
   * If the node was already processed, it will return the found result.
   *
   * @param toProcess The node to be processed
   * @return The processed node
   */
  protected T processNode(Node toProcess) {
    var resultNode = processedNodes.get(toProcess);
    if (resultNode != null) {
      return resultNode;
    }
    try {
      resultNode = processUnprocessedNode(toProcess);
      processedNodes.put(toProcess, resultNode);
      return resultNode;
    } catch (Exception e) {
      // wrap exceptions in viam error
      if (e instanceof ViamError) {
        throw e;
      }
      throw new ViamGraphError("Exception during graph processing: " + e.getMessage(),
          e)
          .addContext(toProcess)
          .addContext(toProcess.graph());
    }
  }

  protected abstract T processUnprocessedNode(Node toProcess);

  @Nullable
  @Override
  public T visit(Node from, @Nullable Node to) {
    if (to != null) {
      processNode(to);
    }
    // result is ignored anyway
    return null;
  }


  protected <R extends T> R getResultOf(Node processedNode, Class<R> clazz) {
    if (!processedNodes.containsKey(processedNode)) {
      processNode(processedNode);
    }
    var result = processedNodes.get(processedNode);
    processedNode.ensure(result != null,
        "node processing implementation is faulty. no result found");
    ViamError.ensure(clazz.isInstance(result),
        "expected result to be instance of %s, but was %s", clazz, result);
    return clazz.cast(result);
  }
}
