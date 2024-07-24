package vadl.viam.matching.impl;

import java.util.Optional;
import vadl.types.Type;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ReadRegFileNode;
import vadl.viam.matching.Matcher;

/**
 * Matches any {@link ReadRegFileNode}.
 */
public class AnyReadRegFileMatcher implements Matcher {

  @Override
  public boolean matches(Node node) {
    return node instanceof ReadRegFileNode;
  }
}
