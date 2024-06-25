package vadl.viam;

import vadl.types.ConcreteRelationType;
import vadl.types.DataType;
import vadl.types.Type;

public class Memory extends Resource {

  private final DataType addressType;
  private final DataType resultType;

  public Memory(Identifier identifier, DataType accessType, DataType resultType) {
    super(identifier);
    this.addressType = accessType;
    this.resultType = resultType;
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
}
