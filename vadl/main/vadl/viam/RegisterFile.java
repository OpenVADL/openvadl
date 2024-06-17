package vadl.viam;

import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;
import vadl.types.ConcreteRelationType;
import vadl.types.DataType;
import vadl.types.Type;


public class RegisterFile extends Resource {

  private final DataType accessType;
  private final DataType resultType;
  private final List<Constraint> constraints;

  public RegisterFile(Identifier identifier, DataType accessType, DataType resultType) {
    super(identifier);
    this.accessType = accessType;
    this.resultType = resultType;
    this.constraints = new ArrayList<>();
  }


  @Override
  public boolean hasAddress() {
    return true;
  }

  @Override
  public DataType addressType() {
    return accessType;
  }

  @Override
  public DataType resultType() {
    return resultType;
  }

  @Override
  public ConcreteRelationType relationType() {
    return Type.concreteRelation(accessType, resultType);
  }

  public void addConstraint(Constant.Value address, Constant.Value value) {
    constraints.add(new Constraint(address, value, this));
  }

  public List<Constraint> constraints() {
    return constraints;
  }

  @Override
  public void accept(DefinitionVisitor visitor) {
    visitor.visit(this);
  }


  /**
   * A register file constraint that statically defines the result value for a specific
   * address.
   *
   * <p>For example<pre>
   *  {@code
   * [X(0) = 0]
   * register file X: Index -> Regs
   * }
   * </pre>
   * defines that the address 0 always results in 0 on register file X.
   * </p>
   *
   * @param address      of constraint
   * @param value        of constraint
   * @param registerFile associated to constraint
   */
  public record Constraint(
      Constant.Value address,
      Constant.Value value,
      RegisterFile registerFile
  ) {

    public Constraint(
        Constant.Value address,
        Constant.Value value,
        RegisterFile registerFile
    ) {
      this.address = address;
      this.value = value;
      this.registerFile = registerFile;

      registerFile.ensure(value.type().canBeCastTo(registerFile.resultType),
          "Type missmatch: Can't cast value type %s to register file result type %s.",
          value.type(), registerFile);

      registerFile.ensure(address.type().canBeCastTo(registerFile.accessType),
          "Type missmatch: Can't cast address type %s to register file address type %s.",
          address.type(), registerFile);
    }

  }
}
