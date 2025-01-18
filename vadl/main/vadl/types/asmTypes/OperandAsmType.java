package vadl.types.asmTypes;

import javax.annotation.Nullable;

/**
 * {@code @operand} represents an operand of an {@code @instruction}.
 * Most types can be cast to an operand. In the LCB, this corresponds to a MCOperand.
 */
public class OperandAsmType implements AsmType {
  @Nullable
  private static OperandAsmType INSTANCE;

  private OperandAsmType() {
  }

  /**
   * Get the singleton instance of this AsmType.
   *
   * @return instance of the AsmType
   */
  public static AsmType instance() {
    if (INSTANCE == null) {
      INSTANCE = new OperandAsmType();
    }
    return INSTANCE;
  }

  @Override
  public String name() {
    return "operand";
  }

  @Override
  public boolean canBeCastTo(AsmType to) {
    return to == this || to == VoidAsmType.instance() || to == InstructionAsmType.instance()
        || to == OperandsAsmType.instance();
  }

  @Override
  public String toString() {
    return "@" + name();
  }
}
