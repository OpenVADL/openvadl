package vadl.types.asmTypes;

import javax.annotation.Nullable;

/**
 * {@code @symbol} refers to an identifier in the assembly language.
 */
public class SymbolAsmType implements AsmType {
  @Nullable
  private static SymbolAsmType INSTANCE;

  private SymbolAsmType() {
  }

  /**
   * Get the singleton instance of this AsmType.
   *
   * @return instance of the AsmType
   */
  public static AsmType instance() {
    if (INSTANCE == null) {
      INSTANCE = new SymbolAsmType();
    }
    return INSTANCE;
  }

  @Override
  public String name() {
    return "symbol";
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
