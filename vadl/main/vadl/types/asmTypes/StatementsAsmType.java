package vadl.types.asmTypes;

import javax.annotation.Nullable;

/**
 * {@code @statements} represents a set of statements.
 * A statement is any one of all available {@code @instruction}.
 */
public class StatementsAsmType implements AsmType {
  @Nullable
  private static StatementsAsmType INSTANCE;

  private StatementsAsmType() {
  }

  /**
   * Get the singleton instance of this AsmType.
   *
   * @return instance of the AsmType
   */
  public static AsmType instance() {
    if (INSTANCE == null) {
      INSTANCE = new StatementsAsmType();
    }
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

  @Override
  public String toString() {
    return "@" + name();
  }
}
