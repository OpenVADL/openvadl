package vadl.viam;

import org.jetbrains.annotations.Nullable;
import vadl.types.ConcreteRelationType;
import vadl.types.DataType;
import vadl.types.Type;

public class RegisterFile extends Resource {

  private final DataType accessType;
  private final DataType resultType;

  public RegisterFile(Identifier identifier, DataType accessType, DataType resultType) {
    super(identifier);
    this.accessType = accessType;
    this.resultType = resultType;
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

  @Override
  public void accept(DefinitionVisitor visitor) {
    visitor.visit(this);
  }
}
