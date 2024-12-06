package vadl.lcb.passes.isaMatching.database;

import java.util.function.Predicate;
import vadl.viam.graph.Node;

/**
* Query for checking instruction's behavior for conditions.
*/
public class BehaviorQuery {
  private Class<? extends Node> applicable;
  private Predicate<Node> predicate;

  public BehaviorQuery(Class<? extends Node> applicable, Predicate<Node> predicate) {
    this.applicable = applicable;
    this.predicate = predicate;
  }

  public Class<? extends Node> applicable() {
    return applicable;
  }

  public Predicate<Node> predicate() {
    return predicate;
  }
}
