package vadl.viam.graph;

import com.google.errorprone.annotations.FormatMethod;
import javax.annotation.Nullable;
import org.jetbrains.annotations.Contract;
import vadl.utils.SourceLocation;
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

  public ViamGraphError addLocation(SourceLocation location) {
    super.addLocation(location);
    return this;
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


  //// STATIC HELPER

  @FormatMethod
  private static void ensureInternal(boolean condition, @Nullable Graph graph, @Nullable Node node1,
                                     @Nullable Node node2,
                                     String fmt,
                                     Object... args) {
    if (!condition) {
      throw new ViamGraphError(fmt, args)
          .addContext(graph)
          .addContext(node1)
          .addContext(node2)
          .shrinkStacktrace(2)
          ;
    }
  }

  @FormatMethod
  public static void ensure(boolean condition, @Nullable Graph graph, @Nullable Node node1,
                            @Nullable Node node2,
                            String fmt,
                            Object... args) {
    ensureInternal(condition, graph, node1, node2, fmt, args);
  }

  @FormatMethod
  public static void ensure(boolean condition, @Nullable Graph graph, @Nullable Node node1,
                            String fmt,
                            Object... args) {
    ViamGraphError.ensureInternal(condition, graph, node1, null, fmt, args);
  }

  @FormatMethod
  public static void ensure(boolean condition, @Nullable Graph graph, String fmt,
                            Object... args) {
    ViamGraphError.ensureInternal(condition, graph, null, null, fmt, args);
  }

  @FormatMethod
  public static void ensure(boolean condition, String fmt,
                            Object... args) {
    ViamGraphError.ensureInternal(condition, null, null, null, fmt, args);
  }

  @Contract("null, _, _, _, _  -> fail")
  @FormatMethod
  public static void ensureNonNull(@Nullable Object o, @Nullable Graph graph, @Nullable Node node1,
                                   @Nullable Node node2, String msg) {
    ViamGraphError.ensureInternal(o != null, graph, node1, node2, msg);
  }

}
