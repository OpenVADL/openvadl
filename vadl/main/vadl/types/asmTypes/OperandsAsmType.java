package vadl.types.asmTypes;

import javax.annotation.Nullable;

/**
 * {@code @operands} represents a set of {@code @operand}.
 * It is used when defining a repetition block containing an operand.
 *
 * @see OperandAsmType
 */
public class OperandsAsmType implements AsmType {
  @Nullable
  private static OperandsAsmType INSTANCE;

  private OperandsAsmType() {
  }

  /**
   * Get the singleton instance of this AsmType.
   *
   * @return instance of the AsmType
   */
  public static AsmType instance() {
    if (INSTANCE == null) {
      INSTANCE = new OperandsAsmType();
    }
    return INSTANCE;
  }

  @Override
  public String name() {
    return "operands";
  }

  @Override
  public boolean canBeCastTo(AsmType to) {
    return to == this || to == VoidAsmType.instance();
  }

  @Override
  public String toString() {
    return "@" + name();
  }
}
