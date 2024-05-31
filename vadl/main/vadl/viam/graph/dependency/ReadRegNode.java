package vadl.viam.graph.dependency;

import java.util.List;
import javax.annotation.Nullable;
import vadl.javaannotations.viam.DataValue;
import vadl.types.DataType;
import vadl.types.Type;
import vadl.viam.Register;

/**
 * The ReadRegNode class is a subclass of ReadNode that represents
 * a node that reads a value from a register location.
 */
public class ReadRegNode extends ReadNode {

  @DataValue
  protected Register register;

  /**
   * Reads a value from a register.
   *
   * @param register the register to read from
   * @param index    the optional index expression
   * @param type     the data type of the value to be read
   */
  public ReadRegNode(Register register, @Nullable ExpressionNode index, DataType type) {
    super(index, type);
    this.register = register;

    verifyState();
  }

  public ReadRegNode(Register register, DataType type) {
    this(register, null, type);
  }

  @Override
  public void verifyState() {
    super.verifyState();
    ensure(register.resultType().canBeCastTo(type()),
        "Mismatching register type. Register's result type (%s) cannot be cast to node's type.",
        register.resultType());
    ensure(hasAddress() == register.hasAddress(),
        "This hasAddress() and registers' hasAddress() returned different results.");
    if (hasAddress()) {
      ensure(((DataType) address().type()).canBeCastTo(register.addressType()),
          "Type of address node can not be cast to required register address type.");
    }
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(register);
  }

}
