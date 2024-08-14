package vadl.viam.graph.dependency;

import vadl.types.Type;
import vadl.viam.Definition;

/**
 * Represents an abstract parameter node, like an instruction or function
 * parameter.
 */
public abstract class ParamNode extends ExpressionNode {

  public ParamNode(Type type) {
    super(type);
  }

  public abstract Definition definition();

}
