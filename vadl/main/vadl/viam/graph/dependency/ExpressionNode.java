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

  public void setType(Type type) {
    this.type = type;
  }

  public boolean isConstant() {
    return this instanceof ConstantNode;
  }


  /**
   * Creates a pretty-printed representation of the current expression node.
   *
   * @return A string containing the pretty-printed representation of the expression node.
   */
  public final String prettyPrint() {
    StringBuilder sb = new StringBuilder();
    prettyPrint(sb);
    return sb.toString();
  }

  /**
   * Appends a pretty-printed representation of the current expression node to the
   * provided StringBuilder.
   *
   * @param sb The StringBuilder to which the pretty-printed representation is appended.
   */
  public void prettyPrint(StringBuilder sb) {
    sb.append("prettyPrint(").append(getClass().getSimpleName()).append(")");
  }

}
