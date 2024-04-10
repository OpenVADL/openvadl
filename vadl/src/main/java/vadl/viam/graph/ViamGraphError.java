package vadl.viam.graph;

import com.google.errorprone.annotations.FormatMethod;
import javax.annotation.Nullable;
import vadl.viam.ViamError;

/**
 * A {@link ViamError} for graph related failures.
 * It contains context information of the graph and the specific
 * node.
 */
public class ViamGraphError extends ViamError {

  private @Nullable Graph graph;
  private @Nullable Node node;

  @FormatMethod
  public ViamGraphError(String message, @Nullable Object... args) {
    super(message.formatted(args));
  }

  /**
   * Adds a node to the context if it is not null.
   */
  public ViamGraphError addContext(@Nullable Node node) {
    if (node != null) {
      this.node = node;
      addContext("node", node);
    }
    return this;
  }

  /**
   * Adds a graph to the context if it is not null.
   */
  public ViamGraphError addContext(@Nullable Graph graph) {
    if (graph != null) {
      this.graph = graph;
      addContext("graph", graph);
    }
    return this;
  }

  @Nullable
  public Graph graph() {
    return graph;
  }

  @Nullable
  public Node node() {
    return node;
  }
}
