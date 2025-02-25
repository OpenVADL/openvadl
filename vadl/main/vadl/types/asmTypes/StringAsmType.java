package vadl.types.asmTypes;

import javax.annotation.Nullable;
import vadl.types.Type;

/**
 * {@code @string} is the type of most terminal rules.
 *
 * @see vadl.ast.AsmGrammarDefaultRules
 */
public class StringAsmType implements AsmType {
  @Nullable
  private static StringAsmType INSTANCE;

  private StringAsmType() {
  }

  /**
   * Get the singleton instance of this AsmType.
   *
   * @return instance of the AsmType
   */
  public static AsmType instance() {
    if (INSTANCE == null) {
      INSTANCE = new StringAsmType();
    }
    return INSTANCE;
  }

  @Override
  public String name() {
    return "string";
  }

  @Override
  public Type toOperationalType() {
    return Type.string();
  }

  @Override
  public String toCppTypeString(String prefix) {
    return "StringRef";
  }

  @Override
  public boolean canBeCastTo(AsmType to) {
    return to == this || to == VoidAsmType.instance() || to == ModifierAsmType.instance()
        || to == SymbolAsmType.instance() || to == RegisterAsmType.instance()
        || to == OperandAsmType.instance();
  }

  @Override
  public String toString() {
    return "@" + name();
  }
}
