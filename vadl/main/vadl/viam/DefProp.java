package vadl.viam;

import java.util.List;
import vadl.types.Type;
import vadl.viam.graph.Graph;

/**
 * A set of interfaces that mark definitions with certain properties.
 * This allows seraching for definitions with such properties.
 */
public interface DefProp {

  default Definition asDefinition() {
    return (Definition) this;
  }

  /**
   * A marker interface for definitions that have a type. (e.g. format, format field, ...)
   */
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
