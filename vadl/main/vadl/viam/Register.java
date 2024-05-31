package vadl.viam;

import vadl.types.ConcreteRelationType;
import vadl.types.DataType;
import vadl.types.Type;

/**
 * Represents a register definition in VADL.
 *
 * <p>A register always has a resultType of {@link DataType} and an optional addressType of
 * {@link DataType} if it is a register file. Thus the type is a {@link ConcreteRelationType}
 * with at most one argument type and exactly one result type.</p>
 */
public class Register extends Definition {

  private final ConcreteRelationType type;

  /**
   * Creates a new instance of a {@link Register} definition.
   *
   * @param identifier The identifier of the register.
   * @param type       The type of the register.
   */
  public Register(Identifier identifier, ConcreteRelationType type) {
    super(identifier);
    this.type = type;

    verify();
  }

  public Register(Identifier identifier, DataType type) {
    this(identifier, Type.concreteRelation(type));
  }

  public boolean hasAddress() {
    return !type.argTypes().isEmpty();
  }

  public DataType addressType() {
    ensure(hasAddress(), "Register has no address");
    return (DataType) type.argTypes().get(0);
  }

  public DataType resultType() {
    return (DataType) type.returnType();
  }

  public ConcreteRelationType relationType() {
    return type;
  }

  @Override
  public void verify() {
    ensure(type.returnType() instanceof DataType,
        "Invalid register type. Must result in DataType, was: %s", type);
    ensure(type.argTypes().size() <= 1, "Type must have at most one argument, was: %s", type);
    if (hasAddress()) {
      ensure(type.argTypes().get(0) instanceof DataType,
          "Address type must be a DataType, was: %s", type.argTypes().get(0));
    }
  }

  @Override
  public void accept(DefinitionVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public String toString() {
    return "register " + (hasAddress() ? "file " : "") + identifier + ": " + type;
  }
}
