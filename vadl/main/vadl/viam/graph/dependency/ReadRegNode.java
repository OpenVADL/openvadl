package vadl.viam.graph.dependency;

import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import vadl.javaannotations.viam.DataValue;
import vadl.types.DataType;
import vadl.viam.Counter;
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

  // a register-file-read might read from a counter.
  // if this can be inferred, the counter is set.
  // it is generally set during the `StaticCounterAccessResolvingPass`
  @DataValue
  @Nullable
  private Counter.RegisterCounter staticCounterAccess;

  /**
   * Reads a value from a register.
   *
   * @param register            the register to read from
   * @param type                the data type of the value to be read
   * @param staticCounterAccess the {@link Counter} that is read,
   *                            or null if no counter is read
   */
  public ReadRegNode(Register register, DataType type,
                     @Nullable Counter.RegisterCounter staticCounterAccess) {
    super(null, type);
    this.register = register;
    this.staticCounterAccess = staticCounterAccess;
  }

  public Register register() {
    return register;
  }

  @Nullable
  public Counter.RegisterCounter staticCounterAccess() {
    return staticCounterAccess;
  }

  public void setStaticCounterAccess(@Nonnull Counter.RegisterCounter staticCounterAccess) {
    this.staticCounterAccess = staticCounterAccess;
  }

  @Override
  public boolean hasAddress() {
    return false;
  }

  @Override
  public Resource resourceDefinition() {
    return register;
  }

  @Override
  public void verifyState() {
    super.verifyState();

    ensure(register.resultType().isTrivialCastTo(type()),
        "Mismatching register type. Register's result type (%s) "
            + "cannot be trivially cast to node's type (%s).",
        register.resultType(), type());
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(register);
    collection.add(staticCounterAccess);
  }

  @Override
  public Node copy() {
    return new ReadRegNode(register, type(), staticCounterAccess);
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
