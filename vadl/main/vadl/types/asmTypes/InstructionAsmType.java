package vadl.types.asmTypes;

import javax.annotation.Nullable;

/**
 * {@code @instruction} is a whole machine or pseudo instruction.
 * It contains one or more {@code @operand}.
 * In the LCB, this corresponds to a MCInst.
 *
 * @see OperandAsmType
 */
public class InstructionAsmType implements AsmType {
  @Nullable
  private static InstructionAsmType INSTANCE;

  private InstructionAsmType() {
  }

  /**
   * Get the singleton instance of this AsmType.
   *
   * @return instance of the AsmType
   */
  public static AsmType instance() {
    if (INSTANCE == null) {
      INSTANCE = new InstructionAsmType();
    }
    return INSTANCE;
  }

  @Override
  public String name() {
    return "instruction";
  }

  @Override
  public boolean canBeCastTo(AsmType to) {
    return to == this || to == VoidAsmType.instance() || to == StatementsAsmType.instance();
  }

  @Override
  public String toString() {
    return "@" + name();
  }
}
