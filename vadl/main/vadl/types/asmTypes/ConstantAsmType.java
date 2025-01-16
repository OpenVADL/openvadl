package vadl.types.asmTypes;

import javax.annotation.Nullable;
import vadl.types.Type;

/**
 * {@code @constant} is a 64-bit signed integer.
 * The default rules Integer and Natural produce this type.
 *
 * @see vadl.ast.AsmGrammarDefaultRules
 */
public class ConstantAsmType implements AsmType {
  @Nullable
  private static ConstantAsmType INSTANCE;

  private ConstantAsmType() {
  }

  /**
   * Get the singleton instance of this AsmType.
   *
   * @return instance of the AsmType
   */
  public static AsmType instance() {
    if (INSTANCE == null) {
      INSTANCE = new ConstantAsmType();
    }
    return INSTANCE;
  }

  @Override
  public String name() {
    return "constant";
  }

  @Override
  public Type toOperationalType() {
    return Type.signedInt(64);
  }

  @Override
  public boolean canBeCastTo(AsmType to) {
    return to == this || to == VoidAsmType.instance() || to == OperandAsmType.instance()
        || to == RegisterAsmType.instance();
  }

  @Override
  public String toString() {
    return "@" + name();
  }
}
