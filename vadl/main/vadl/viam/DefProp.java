package vadl.viam;

import java.util.List;
import vadl.types.Type;
import vadl.viam.graph.Graph;

public interface DefProp {
  
  default Definition asDefinition() {
    return (Definition) this;
  }

  interface WithType extends DefProp {
    Type type();
  }

  /**
   * A marker interface for definitions that hold one or more behaviors.
   */
  interface WithBehavior extends DefProp {

    List<Graph> behaviors();
  }
}
