package vadl.viam.matching.impl;

import com.google.common.collect.Streams;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import vadl.types.BuiltInTable;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.matching.Matcher;

/**
 * Checks if the node has the given {@link BuiltInTable.BuiltIn} and
 * all the inputs from the node also match the given {@link Matcher}.
 * Note that if the node has more inputs then matchers given then the {@code matches}
 * method will return {@code true}.
 */
public class BuiltInMatcher implements Matcher {

  private final Set<BuiltInTable.BuiltIn> builtIns;
  private final List<Matcher> matchers;

  public BuiltInMatcher(BuiltInTable.BuiltIn builtIn,
                        List<Matcher> matchers) {
    this.builtIns = Set.of(builtIn);
    this.matchers = matchers;
  }

  /**
   * Constructor for matcher.
   *
   * @param builtIns is a list of accepted builtins.
   * @param matchers for children nodes.
   */
  public BuiltInMatcher(List<BuiltInTable.BuiltIn> builtIns, List<Matcher> matchers) {
    this.builtIns = new HashSet<>(builtIns);
    this.matchers = matchers;
  }

  public BuiltInMatcher(BuiltInTable.BuiltIn builtIn,
                        Matcher matcher) {
    this.builtIns = Set.of(builtIn);
    this.matchers = List.of(matcher);
  }

  @Override
  public boolean matches(Node node) {
    if (node instanceof BuiltInCall && builtIns.contains(((BuiltInCall) node).builtIn())) {
      if (this.matchers.isEmpty()) {
        // Edge case: when no matchers exist and the builtIn is matched then return true.
        return true;
      } else {
        // The matchers must perfectly fit because the inputs cannot be rearranged.
        return Streams.zip(node.inputs(), this.matchers.stream(),
            (inputNode, matcher) -> matcher.matches(inputNode)).allMatch(x -> x);
      }
    }

    return false;
  }
}
