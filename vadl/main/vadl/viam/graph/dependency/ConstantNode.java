package vadl.viam.graph.dependency;

import java.util.List;
import vadl.javaannotations.viam.DataValue;
import vadl.oop.SymbolTable;
import vadl.viam.Constant;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;

/**
 * The constant node represents a compile time constant value in the
 * data dependency graph.
 */
public class ConstantNode extends ExpressionNode {

  @DataValue
  private Constant constant;

  public ConstantNode(Constant constant) {
    super(constant.type());
    this.constant = constant;
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(constant);
  }

  /**
   * Set the {@link Constant}.
   */
  public void setConstant(Constant constant) {
    this.constant = constant;
  }

  /**
   * Return the {@link Constant}.
   */
  public Constant constant() {
    return this.constant;
  }

  @Override
  public Node copy() {
    return new ConstantNode(constant);
  }

  @Override
  public Node shallowCopy() {
    return new ConstantNode(constant);
  }

  @Override
  public void accept(GraphNodeVisitor visitor) {
    visitor.visit(this);
  }
}
