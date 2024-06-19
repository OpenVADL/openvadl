package vadl.viam.graph.dependency;

import java.util.List;
import vadl.javaannotations.viam.DataValue;
import vadl.types.DataType;
import vadl.viam.Register;
import vadl.viam.Resource;

/**
 * The ReadMemNode class is a concrete class that extends ReadNode.
 * It represents a node that reads a value from a memory location.
 */
public class ReadMemNode extends ReadResourceNode {

  @DataValue
  protected Register register;

  public ReadMemNode(Register register, ExpressionNode address, DataType type) {
    super(address, type);
    this.register = register;
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
}
