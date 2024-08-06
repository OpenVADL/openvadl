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

  private final Optional<Type> type;
  private final Matcher matcher;

  public TypcastMatcher(Type type, Matcher matcher) {
    this.type = Optional.ofNullable(type);
    this.matcher = matcher;
  }

  /**
   * Match any typecast when {@link Matcher} matches the value.
   */
  protected TypcastMatcher(Matcher matcher) {
    this.type = Optional.empty();
    this.matcher = matcher;
  }

  @Override
  public boolean matches(Node node) {
    if (node instanceof TypeCastNode && type.isEmpty()) {
      return matcher.matches(node);
    } else if (node instanceof TypeCastNode && ((TypeCastNode) node).type() == this.type.get()) {
      return this.matches(((TypeCastNode) node).value());
    }

    return false;
  }
}
