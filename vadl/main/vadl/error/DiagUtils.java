package vadl.error;

import vadl.utils.SourceLocation;
import vadl.viam.graph.Node;

/**
 * A utility collection for {@link Diagnostic}.
 */
public class DiagUtils {

  /**
   * Throws a {@code <something> is not allowed here} diagnostic exception.
   * The source location will be the node, if it is not an invalid source location.
   * Otherwise, it will be the node's parent definition's source location.
   *
   * @param node The node that caused the diagnostic error.
   * @param what The plural phrase that prefixes {@code are not allowed here}.
   */
  public static void throwNotAllowed(Node node, String what) {
    SourceLocation loc = !node.sourceLocation().equals(SourceLocation.INVALID_SOURCE_LOCATION)
        ? node.sourceLocation()
        : node.ensureGraph().parentDefinition().sourceLocation();
    throw Diagnostic.error(what + " are not allowed here", loc)
        .build();
  }

}
