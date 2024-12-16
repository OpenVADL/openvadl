package vadl.types.asmTypes;

/**
 * {@code @string} is the type of most terminal rules.
 *
 * @see vadl.ast.AsmGrammarDefaultRules
 */
public class StringAsmType implements AsmType {
  private static final StringAsmType INSTANCE = new StringAsmType();

  private StringAsmType() {
  }

  public static AsmType instance() {
    return INSTANCE;
  }

  @Override
  public String name() {
    return "string";
  }

  @Override
  public boolean canBeCastTo(AsmType to) {
    return to == this || to == VoidAsmType.instance() || to == ModifierAsmType.instance()
        || to == SymbolAsmType.instance() || to == RegisterAsmType.instance()
        || to == OperandAsmType.instance();
  }
}
