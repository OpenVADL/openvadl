package vadl.types.asmTypes;

/**
 * {@code @constant} is a 64-bit signed integer.
 * The default rules Integer and Natural produce this type.
 *
 * @see vadl.ast.AsmGrammarDefaultRules
 */
public class ConstantAsmType implements AsmType {

  private static final ConstantAsmType INSTANCE = new ConstantAsmType();

  private ConstantAsmType() {
  }

  public static AsmType instance() {
    return INSTANCE;
  }

  @Override
  public String name() {
    return "constant";
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
