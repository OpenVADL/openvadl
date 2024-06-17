package vadl.viam;

import org.jetbrains.annotations.Nullable;
import vadl.types.ConcreteRelationType;
import vadl.types.DataType;
import vadl.types.Type;

public class Register extends Resource {

  /**
   * Defines if a sub-register is access by loading the whole register and slicing the result,
   * or by directly accessing the partial result. Same for writing a register.
   */
  public enum AccessKind {
    PARTIAL,
    FULL
  }

  private final DataType resultType;

  @Nullable
  private Register parent;
  private final Register[] subRegisters;

  private final AccessKind readAccess;
  private final AccessKind writeAccess;

  @Nullable
  private final Format refFormat;

  public Register(Identifier identifier, DataType resultType, AccessKind readAccess,
                  AccessKind writeAccess, Format refFormat, Register[] subRegisters) {
    super(identifier);
    this.resultType = resultType;
    this.subRegisters = subRegisters;
    this.readAccess = readAccess;
    this.writeAccess = writeAccess;
    this.refFormat = refFormat;
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

  public AccessKind readAccess() {
    return readAccess;
  }

  public AccessKind writeAccess() {
    return writeAccess;
  }

  public @Nullable Register parent() {
    return this.parent;
  }

  @SuppressWarnings("NullableProblems")
  public void setParent(Register parent) {
    this.parent = parent;
  }

  public Register[] subRegisters() {
    return subRegisters;
  }

  public @Nullable Format refFormat() {
    return refFormat;
  }

  @Override
  public void accept(DefinitionVisitor visitor) {
    visitor.visit(this);
  }

}
