package vadl.viam.graph.dependency;

import vadl.types.Type;

/**
 * Expression nodes produce some value and therefore also
 * hold the type of the value. This is required to maintain
 * graph consistency during graph transformation.
 */
public abstract class ExpressionNode extends DependencyNode {

  //TODO: Should this be DataType in any case?
  private Type type;

  public ExpressionNode(Type type) {
    this.type = type;
  }

  //  public abstract Type type();
  public Type type() {
    return type;
  }

  protected void setType(Type type) {
    this.type = type;
  }
}
