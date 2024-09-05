package vadl.viam;

import javax.annotation.Nonnull;
import vadl.types.ConcreteRelationType;
import vadl.types.DataType;
import vadl.types.Type;

/**
 * The register file is related to the {@link Register} but takes an address/index when accessing
 * it. It may also have constraints that restricts the possible values for statically defined
 * addresses.
 */
public class RegisterFile extends Resource {

  private final DataType addressType;
  private final DataType resultType;
  private final Constraint[] constraints;

  /**
   * Constructs a new RegisterFile object.
   *
   * @param identifier  The identifier of the RegisterFile.
   * @param addressType The data type of the file address/index.
   * @param resultType  The data type of the result value.
   */
  public RegisterFile(Identifier identifier, DataType addressType, DataType resultType,
                      Constraint[] constraints) {
    super(identifier);
    this.addressType = addressType;
    this.resultType = resultType;
    this.constraints = constraints;
  }


  @Override
  public boolean hasAddress() {
    return true;
  }

  @Override
  @Nonnull
  public DataType addressType() {
    return addressType;
  }

  @Override
  public DataType resultType() {
    return resultType;
  }

  @Override
  public ConcreteRelationType relationType() {
    return Type.concreteRelation(addressType, resultType);
  }

  public Constraint[] constraints() {
    return constraints;
  }

  @Override
  public void verify() {
    super.verify();

    for (Constraint constraint : constraints) {
      ensure(constraint.value.type().isTrivialCastTo(resultType),
          "Type missmatch: Can't cast value type %s to register file result type %s.",
          constraint.value.type(), this.resultType);

      ensure(constraint.address.type().isTrivialCastTo(addressType),
          "Type missmatch: Can't cast address type %s to register file address type %s.",
          constraint.address.type(), this.resultType);
    }
  }

  @Override
  public void accept(DefinitionVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public String toString() {
    return identifier.simpleName() + ": " + addressType + " -> " + resultType;
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
   * @param address of constraint
   * @param value   of constraint
   */
  public record Constraint(
      Constant.Value address,
      Constant.Value value
  ) {
  }
}
