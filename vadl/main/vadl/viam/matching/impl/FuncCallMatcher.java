package vadl.viam.matching.impl;

import vadl.types.Type;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.FuncCallNode;
import vadl.viam.matching.Matcher;

/**
 * Checks if the node has the given {@link FuncCallNode} with a return {@link Type}.
 */
public class FuncCallMatcher implements Matcher {

  private final Type returnType;

  public FuncCallMatcher(Type type) {
    this.returnType = type;
  }


  @Override
  public boolean matches(Node node) {
    return (node instanceof FuncCallNode call
        && call.function().returnType() == this.returnType);
  }
}
