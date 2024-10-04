package vadl.viam.graph.dependency;

import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import vadl.javaannotations.viam.DataValue;
import vadl.types.DataType;
import vadl.viam.Counter;
import vadl.viam.RegisterFile;
import vadl.viam.Resource;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.HasRegisterFile;
import vadl.viam.graph.Node;

/**
 * A read of a register file in the behaviour graph. It takes one expression node as input
 * that represents the address/index value.
 */
public class ReadRegFileNode extends ReadResourceNode implements HasRegisterFile {

  @DataValue
  protected RegisterFile registerFile;

  // a register-file-read might read from a counter.
  // if this can be inferred, the counter is set.
  // however, not all counter-accesses are statically known, as if the register file
  // is known, but the concrete index isn't,
  // it could be a counter written, but doesn't have to be.
  // it is generally set during the `StaticCounterAccessResolvingPass`
  @DataValue
  @Nullable
  private Counter.RegisterFileCounter staticCounterAccess;

  /**
   * Constructs the node, which represents a read from a register file at some specific index.
   *
   * @param registerFile        the register-file definition to be read from
   * @param address             the index of the specific register in the register-file
   * @param type                the type this node should be result in
   * @param staticCounterAccess the {@link Counter} this node reads from, or null if
   *                            it is not known
   */
  public ReadRegFileNode(RegisterFile registerFile, ExpressionNode address,
                         DataType type, @Nullable Counter.RegisterFileCounter staticCounterAccess) {
    super(address, type);
    this.registerFile = registerFile;
    this.staticCounterAccess = staticCounterAccess;
  }

  public RegisterFile registerFile() {
    return registerFile;
  }

  @Nullable
  public Counter.RegisterFileCounter staticCounterAccess() {
    return staticCounterAccess;
  }

  public void setStaticCounterAccess(@Nonnull Counter.RegisterFileCounter staticCounterAccess) {
    this.staticCounterAccess = staticCounterAccess;
  }

  @Override
  protected Resource resourceDefinition() {
    return registerFile;
  }

  @Override
  public void verifyState() {
    super.verifyState();

    ensure(registerFile.resultType().isTrivialCastTo(type()),
        "Mismatching register file type. Register file's result type (%s) "
            + "cannot be trivially cast to node's type (%s).",
        registerFile.resultType(), type());
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(registerFile);
    collection.add(staticCounterAccess);
  }

  @Override
  public Node copy() {
    return new ReadRegFileNode(registerFile, (ExpressionNode) address().copy(), type(),
        staticCounterAccess());
  }

  @Override
  public Node shallowCopy() {
    return new ReadRegFileNode(registerFile, address(), type(), staticCounterAccess());
  }

  @Override
  public void accept(GraphNodeVisitor visitor) {
    visitor.visit(this);
  }
}
