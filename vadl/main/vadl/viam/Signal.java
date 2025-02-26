package vadl.viam;

import org.jetbrains.annotations.Nullable;
import vadl.types.ConcreteRelationType;
import vadl.types.DataType;
import vadl.types.Type;

/**
 * Represents a Signal in a VADL MiA specification.
 *
 * <p>It only has a single write (driver) and multiple reads.
 */
public class Signal extends Resource {

  private final DataType resultType;

  /**
   * Constructions a new signal definition.
   *
   * @param identifier   the unique identifier of the definition
   * @param resultType   the type of the signal
   */
  public Signal(Identifier identifier, DataType resultType) {
    super(identifier);
    this.resultType = resultType;
  }

  @Override
  public boolean hasAddress() {
    return false;
  }

  @Nullable
  @Override
  public DataType addressType() {
    return null;
  }

  @Override
  public DataType resultType() {
    return resultType;
  }

  @Override
  public ConcreteRelationType relationType() {
    return Type.concreteRelation(resultType);
  }

  @Override
  public void accept(DefinitionVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public String toString() {
    return identifier.simpleName() + ": " + resultType;
  }

}
