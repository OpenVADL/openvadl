package vadl.types.asmTypes;

/**
 * {@code @expression} refers to a single constant or a complex expression tree.
 * Vadl provides a built-in to parse expressions. In the LCB, this corresponds to a MCExpr.
 */
public class ExpressionAsmType implements AsmType {
  private static final ExpressionAsmType INSTANCE = new ExpressionAsmType();

  private ExpressionAsmType() {
  }

  public static AsmType instance() {
    return INSTANCE;
  }

  @Override
  public String name() {
    return "expression";
  }

  @Override
  public boolean canBeCastTo(AsmType to) {
    return to == this || to == VoidAsmType.instance() || to == OperandAsmType.instance();
  }
}
