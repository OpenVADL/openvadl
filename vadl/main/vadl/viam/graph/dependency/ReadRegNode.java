package vadl.viam.graph.dependency;

import java.util.List;
import vadl.javaannotations.viam.DataValue;
import vadl.types.DataType;
import vadl.viam.Register;
import vadl.viam.Resource;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;

/**
 * The ReadRegNode class is a subclass of ReadNode that represents
 * a node that reads a value from a register location.
 */
public class ReadRegNode extends ReadResourceNode {

  @DataValue
  protected Register register;

  /**
   * Reads a value from a register.
   *
   * @param register the register to read from
   * @param type     the data type of the value to be read
   */
  public ReadRegNode(Register register, DataType type) {
    super(null, type);
    this.register = register;

    verifyState();
  }

  public Register register() {
    return register;
  }

  @Override
  public boolean hasAddress() {
    return false;
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
    return new ReadRegNode(register, type());
  }

  @Override
  public Node shallowCopy() {
    return copy();
  }

  @Override
  public void accept(GraphNodeVisitor visitor) {
    visitor.visit(this);
  }
}
