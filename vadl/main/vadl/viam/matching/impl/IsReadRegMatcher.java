package vadl.viam.matching.impl;

import vadl.viam.Register;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ReadRegNode;
import vadl.viam.matching.Matcher;

/**
 * Matches any {@link vadl.viam.graph.dependency.ReadRegNode}.
 */
public class IsReadRegMatcher implements Matcher {

  private final Register register;

  public IsReadRegMatcher(Register register) {
    this.register = register;
  }

  @Override
  public boolean matches(Node node) {
    return node instanceof ReadRegNode n && n.register().equals(register);
  }
}
