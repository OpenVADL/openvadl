package vadl.viam.matching;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import vadl.viam.graph.Node;

/**
 * This class tries to match the given {@link Matcher} on a given {@link List} of {@link Node}.
 */
public class TreeMatcher {
  /**
   * Returns the a {@link List} of {@link Node} when {@code nodes} matches the given
   * {@link Matcher}.
   *
   * @param nodes   which will be checked by the {@code matcher}.
   * @param matcher which checks whether a tree is considered matched.
   * @return a {@link List} of {@link Node} where each node matches the {@link Matcher}.
   */
  public static List<Node> matches(Stream<Node> nodes, Matcher matcher) {
    // Because cycles are allowed, we track all visited nodes to avoid computation.
    var visited = new HashSet<Node>();
    // This arraylist stores all the nodes which were returned successfully by the matcher.
    var result = new ArrayList<Node>();

    // Iterate over all the nodes
    // 1. If the node was not visited -> if visited skip
    // 2. Check whether it matches
    // 3. Mark as visited
    // 4. Store result if it matches
    nodes
        .filter(Objects::nonNull)
        .forEach(node -> {
          if (!visited.contains(node)) {
            var matches = matcher.matches(node);
            visited.add(node);

            if (matches) {
              result.add(node);
            }
          }
        });


    return result;
  }
}
