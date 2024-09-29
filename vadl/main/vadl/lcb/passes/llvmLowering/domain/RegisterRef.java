package vadl.lcb.passes.llvmLowering.domain;

import org.jetbrains.annotations.Nullable;
import vadl.types.ConcreteRelationType;
import vadl.types.DataType;
import vadl.viam.Constant;
import vadl.viam.DefinitionVisitor;
import vadl.viam.Format;
import vadl.viam.Register;
import vadl.viam.RegisterFile;
import vadl.viam.Resource;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.ReadRegFileNode;
import vadl.viam.graph.dependency.ReadResourceNode;
import vadl.viam.graph.dependency.WriteRegFileNode;
import vadl.viam.graph.dependency.WriteResourceNode;

/**
 * A {@link RegisterRef} can be a {@link Register} which comes from {@link ReadResourceNode} or
 * {@link WriteResourceNode}. But it can also come from {@link ReadRegFileNode} or
 * {@link WriteRegFileNode} when the address is constant. Since, we have no way to reduce a
 * {@link RegisterFile} to a {@link Register}, we use {@link RegisterRef} as joined type for
 * both "worlds".
 */
public class RegisterRef extends Resource {
  private final DataType resultType;
  private final ConcreteRelationType relationType;

  @Nullable
  private Format refFormat;
  @Nullable
  private Constant address;

  public RegisterRef(Register register) {
    super(register.identifier);
    this.resultType = register.resultType();
    this.refFormat = register.refFormat();
    this.relationType = register.relationType();
    this.address = null;
  }

  public RegisterRef(RegisterFile registerFile, Constant address) {
    super(registerFile.identifier);
    this.resultType = registerFile.resultType();
    this.relationType = registerFile.relationType();
    this.refFormat = null;
    this.address = address;
  }

  @Override
  public boolean hasAddress() {
    return address != null;
  }

  @Nullable
  @Override
  public DataType addressType() {
    if (hasAddress()) {
      ensure(address != null, "Address must no be null");
      return (DataType) address.type();
    }

    return null;
  }

  @Override
  public DataType resultType() {
    return resultType;
  }

  @Override
  public ConcreteRelationType relationType() {
    return relationType;
  }

  @Override
  public void accept(DefinitionVisitor visitor) {

  }

  /**
   * Get the name of the register or the name of register file with index.
   */
  public String lowerName() {
    if (hasAddress()) {
      ensure(address != null, "address must not be null");
      return name() + address.asVal().decimal();
    } else {
      return name();
    }
  }
}
