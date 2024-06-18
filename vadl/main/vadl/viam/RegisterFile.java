package vadl.viam;

import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;
import vadl.types.ConcreteRelationType;
import vadl.types.DataType;
import vadl.types.Type;

/**
 * The register file is related to the {@link Register} but takes an address/index when accessing
 * it. It may also have constraints that restricts the possible values for statically defined
 * addresses.
 */
public class RegisterFile extends Resource {

  private final DataType accessType;
  private final DataType resultType;
  private final List<Constraint> constraints;

  /**
   * Constructs a new RegisterFile object.
   *
   * @param identifier The identifier of the RegisterFile.
   * @param accessType The data type of the file address/index.
   * @param resultType The data type of the result value.
   */
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

  @Override
  public String toString() {
    return identifier.simpleName() + ": " + accessType + " -> " + resultType;
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

    /**
     * Constructs the constraint of a given register file.
     *
     * @param address      the address constant
     * @param value        the value constant that is always returned when using the address
     * @param registerFile the register file to which this condition belongs
     */
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
