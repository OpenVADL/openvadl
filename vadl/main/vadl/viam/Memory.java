package vadl.viam;

import java.util.Objects;
import vadl.types.ConcreteRelationType;
import vadl.types.DataType;
import vadl.types.Type;

/**
 * The Memory class represents a VADL memory definition.
 *
 * <p>It always takes an address type and result type. The result type
 * specifies the word size of the memory.</p>
 */
public class Memory extends Resource {

  private final DataType addressType;
  private final DataType resultType;

  /**
   * Constructs a new Memory object.
   *
   * @param identifier the identifier of the memory
   * @param accessType the address type of the memory
   * @param resultType the result type of the memory
   */
  public Memory(Identifier identifier, DataType accessType, DataType resultType) {
    super(identifier);
    this.addressType = accessType;
    this.resultType = resultType;
  }

  /**
   * Returns the word size of the memory.
   */
  public int wordSize() {
    return resultType.bitWidth();
  }

  @Override
  public boolean hasAddress() {
    return true;
  }

  @Override
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

  @Override
  public void accept(DefinitionVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public String toString() {
    return identifier.simpleName() + ": " + addressType + " -> " + resultType;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Memory mem) {
      return mem.identifier.equals(this.identifier);
    }

    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(identifier);
  }
}
