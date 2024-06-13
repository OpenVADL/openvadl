package vadl.viam;

import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;
import vadl.types.ConcreteRelationType;
import vadl.types.DataType;
import vadl.types.Type;

public class Register extends Resource {

  private final DataType resultType;

  @Nullable
  private final Register parent;
  private final List<Register> subRegisters;

  public Register(Identifier identifier, DataType resultType, @Nullable Register parent) {
    super(identifier);
    this.resultType = resultType;
    this.subRegisters = new ArrayList<Register>();
    this.parent = parent;
  }

  public Register(Identifier identifier, DataType resultType) {
    this(identifier, resultType, null);
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

  public boolean isSubRegister() {
    return this.parent != null;
  }

  public @Nullable Register parent() {
    return this.parent;
  }

  public List<Register> subRegisters() {
    return subRegisters;
  }

  @Override
  public void accept(DefinitionVisitor visitor) {
    visitor.visit(this);
  }
}
