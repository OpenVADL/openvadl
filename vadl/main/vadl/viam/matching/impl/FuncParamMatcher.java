package vadl.viam.matching.impl;

import com.google.common.collect.Streams;
import java.util.List;
import vadl.types.BuiltInTable;
import vadl.types.Type;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.FuncParamNode;
import vadl.viam.matching.Matcher;

/**
 * Checks if the node has the given {@link FuncParamNode} and a certain {@link Type}.
 */
public class FuncParamMatcher implements Matcher {

  private final Type type;

  public FuncParamMatcher(Type type) {
    this.type = type;
  }


  @Override
  public boolean matches(Node node) {
    return (node instanceof FuncParamNode &&
        ((FuncParamNode) node).parameter().type() == this.type);
  }
}
