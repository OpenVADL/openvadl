package vadl.types.asmTypes;

/**
 * {@code @instruction} is a whole machine or pseudo instruction.
 * It contains one or more {@code @operand}.
 * In the LCB, this corresponds to a MCInst.
 *
 * @see OperandAsmType
 */
public class InstructionAsmType implements AsmType {
  private static final InstructionAsmType INSTANCE = new InstructionAsmType();

  private InstructionAsmType() {
  }

  public static AsmType instance() {
    return INSTANCE;
  }

  @Override
  public String name() {
    return "instruction";
  }

  @Override
  public boolean canBeCastTo(AsmType to) {
    return to == this || to == VoidAsmType.instance();
  }
}
