package vadl.viam.matching.impl;

import com.google.common.collect.Streams;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Stream;
import vadl.types.BuiltInTable;
import vadl.viam.Parameter;
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

  private final BuiltInTable.BuiltIn builtIn;
  private final List<Matcher> matchers;

  public BuiltInMatcher(BuiltInTable.BuiltIn builtIn,
                        List<Matcher> matchers) {
    this.builtIn = builtIn;
    this.matchers = matchers;
  }


  @Override
  public boolean matches(Node node) {
    if (node instanceof BuiltInCall && ((BuiltInCall) node).builtIn() == builtIn) {
      return Streams.zip(node.inputs(), this.matchers.stream(),
          (inputNode, matcher) -> matcher.matches(inputNode)).allMatch(x -> x);
    }

    return false;
  }
}
