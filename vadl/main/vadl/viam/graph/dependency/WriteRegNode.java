package vadl.viam.graph.dependency;

import java.util.List;
import vadl.javaannotations.viam.DataValue;
import vadl.viam.Register;
import vadl.viam.Resource;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.UniqueNode;
import vadl.viam.graph.control.InstrCallNode;

/**
 * Represents a write to register.
 *
 * <p>Even though this is a side effect, it is both, a {@link DependencyNode}
 * and a {@link UniqueNode}. This is because of VADL's semantic constraints
 * for register writes:
 * <li>A register may only be written once per instruction</li>
 * <li>All reads must occur before all writes</li>
 * </p>
 */
public class WriteRegNode extends WriteResourceNode {

  @DataValue
  protected Register register;

  /**
   * Writes a value to a register node.
   *
   * @param register the register node to write to
   * @param value    the value to write to the register
   */
  public WriteRegNode(Register register, ExpressionNode value) {
    super(null, value);
    this.register = register;

    verifyState();
  }

  public Register register() {
    return register;
  }

  @Override
  protected Resource resourceDefinition() {
    return register;
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(register);
  }

  @Override
  public Node copy() {
    return new WriteRegNode(register, (ExpressionNode) value.copy());
  }

  @Override
  public Node shallowCopy() {
    return new WriteRegNode(register, value);
  }

  @Override
  public void accept(GraphNodeVisitor visitor) {
    visitor.visit(this);
  }
}
