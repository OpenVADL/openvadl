package vadl.types.asmTypes;

/**
 * {@code @statements} represents a set of statements.
 * A statement is any one of all available {@code @instruction}.
 */
public class StatementsAsmType implements AsmType {
  private static final StatementsAsmType INSTANCE = new StatementsAsmType();

  private StatementsAsmType() {
  }

  public static AsmType instance() {
    return INSTANCE;
  }

  @Override
  public String name() {
    return "statements";
  }

  @Override
  public boolean canBeCastTo(AsmType to) {
    return to == this || to == VoidAsmType.instance();
  }
}
