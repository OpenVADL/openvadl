package vadl.viam.matching.impl;

import com.google.common.collect.Streams;
import java.util.List;
import java.util.Optional;
import vadl.types.BuiltInTable;
import vadl.types.Type;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.TypeCastNode;
import vadl.viam.matching.Matcher;

/**
 * Matches a {@link TypeCastNode}.
 */
public class TypcastMatcher implements Matcher {

  private final Type type;
  private final Optional<Matcher> matcher;

  public TypcastMatcher(Type type, Matcher matcher) {
    this.type = type;
    this.matcher = Optional.of(matcher);
  }

  public TypcastMatcher(Type type) {
    this.type = type;
    this.matcher = Optional.empty();
  }


  @Override
  public boolean matches(Node node) {
    if (node instanceof TypeCastNode && ((TypeCastNode) node).type() == this.type) {
      if (this.matcher.isPresent()) {
        return this.matches(((TypeCastNode) node).value());
      } else {
        return true;
      }
    }

    return false;
  }
}
