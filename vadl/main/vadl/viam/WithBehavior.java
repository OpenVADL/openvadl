package vadl.viam;

import java.util.List;
import vadl.viam.graph.Graph;

/**
 * A marker interface for definitions that hold one or more behaviors.
 */
public interface WithBehavior {

  List<Graph> behaviors();

  default Definition asDefinition() {
    return (Definition) this;
  }

}
