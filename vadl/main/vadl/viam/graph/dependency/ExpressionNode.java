package vadl.viam.graph.dependency;

import vadl.types.Type;

/**
 * Expression nodes produce some value and therefore also
 * hold the type of the value. This is required to maintain
 * graph consistency during graph transformation.
 */
public class ExpressionNode extends DependencyNode {

  protected Type type;

  public ExpressionNode(Type type) {
    this.type = type;
  }

  public Type type() {
    return type;
  }
}
