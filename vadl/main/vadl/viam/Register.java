package vadl.viam;

import org.jetbrains.annotations.Nullable;
import vadl.types.ConcreteRelationType;
import vadl.types.DataType;
import vadl.types.Type;

/**
 * Represents a Register in a VADL specification.
 *
 * <p>It might have sub registers and it might be a sub register with a parent. Additionally,
 * registers might have a reference format, used to access sub fields by slicing.
 * If partial (sub register) or full (slicing) access is used, depends on the {@link AccessKind}.
 * </p>
 */
public class Register extends Resource {

  /**
   * Defines if a sub-register is accessed by loading the whole register and slicing the result,
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

  /**
   * Constructions a new register definition.
   *
   * @param identifier   the unique identifier of the definition
   * @param resultType   the result type of the register
   * @param readAccess   the read access of the register (see {@link AccessKind})
   * @param writeAccess  the write access of the register (see {@link AccessKind})
   * @param refFormat    the register's format, if it was used as type in the VADL specification
   * @param subRegisters the sub-registers of the register
   */
  public Register(Identifier identifier, DataType resultType, AccessKind readAccess,
                  AccessKind writeAccess, @Nullable Format refFormat, Register[] subRegisters) {
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

  @Override
  public String toString() {
    return identifier.simpleName() + ": " + resultType;
  }

  /**
   * The index register with its {@link Position} definition. E.g. the program counter
   * and the group counter definitions are both {@link Index} definitions.
   */
  public static class Index extends Register {

    /**
     * The position of the index.
     *
     * <p>{@code CURRENT} defines the counter to point to the start of the currently defined
     * instruction (group). When no annotation is given this is the default behavior.
     * This mode is the best for ARM AArch64 and RISC-V architectures. {@code NEXT} defines the
     * counter to point to the end of the currently defined instruction (group).
     * This mode is the best for Alpha and MIPS architectures. {@code NEXT_NEXT} defines the program
     * counter to point to the end of the instruction after the currently defined instruction
     * which is required to have the same size of the currently defined instruction.
     * If the sizes are different the behavior is undefined. The {@code NEXT_NEXT} is not
     * valid for group counters.
     */
    public enum Position {
      CURRENT,
      NEXT,
      NEXT_NEXT
    }

    private final Position position;

    /**
     * Constructions a new register definition.
     *
     * @param identifier the unique identifier of the definition
     * @param resultType the result type of the register
     * @param position   program pointer behavior of the PC
     */
    public Index(Identifier identifier, DataType resultType, Position position) {
      super(identifier, resultType, AccessKind.PARTIAL, AccessKind.PARTIAL, null,
          new Register[] {});
      this.position = position;
    }

    public Position position() {
      return position;
    }
  }

}
