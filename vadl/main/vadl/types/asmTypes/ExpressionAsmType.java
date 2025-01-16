package vadl.types.asmTypes;

import javax.annotation.Nullable;

/**
 * {@code @expression} refers to a single constant or a complex expression tree.
 * Vadl provides a built-in to parse expressions. In the LCB, this corresponds to a MCExpr.
 */
public class ExpressionAsmType implements AsmType {
  @Nullable
  private static ExpressionAsmType INSTANCE;

  private ExpressionAsmType() {
  }

  /**
   * Get the singleton instance of this AsmType.
   *
   * @return instance of the AsmType
   */
  public static AsmType instance() {
    if (INSTANCE == null) {
      INSTANCE = new ExpressionAsmType();
    }
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

  @Override
  public String toString() {
    return "@" + name();
  }
}
