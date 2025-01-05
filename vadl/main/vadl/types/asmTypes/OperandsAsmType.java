package vadl.types.asmTypes;

/**
 * {@code @operands} represents a set of {@code @operand}.
 * It is used when defining a repetition block containing an operand.
 *
 * @see OperandAsmType
 */
public class OperandsAsmType implements AsmType {
  private static final OperandsAsmType INSTANCE = new OperandsAsmType();

  private OperandsAsmType() {
  }

  public static AsmType instance() {
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
