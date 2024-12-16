package vadl.types.asmTypes;

/**
 * {@code @symbol} refers to an identifier in the assembly language.
 */
public class SymbolAsmType implements AsmType {
  private static final SymbolAsmType INSTANCE = new SymbolAsmType();

  private SymbolAsmType() {
  }

  public static AsmType instance() {
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
}
