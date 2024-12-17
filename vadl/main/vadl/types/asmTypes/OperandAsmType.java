package vadl.types.asmTypes;

/**
 * {@code @operand} represents an operand of an {@code @instruction}.
 * Most types can be cast to an operand. In the LCB, this corresponds to a MCOperand.
 */
public class OperandAsmType implements AsmType {

  private static final OperandAsmType INSTANCE = new OperandAsmType();

  private OperandAsmType() {
  }

  public static AsmType instance() {
    return INSTANCE;
  }

  @Override
  public String name() {
    return "operand";
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
