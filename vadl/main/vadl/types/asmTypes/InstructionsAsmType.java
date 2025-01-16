package vadl.types.asmTypes;

import javax.annotation.Nullable;

/**
 * {@code @instructions} represents a set of {@code @instruction}.
 * It is used when defining a repetition block containing an instruction.
 *
 * @see InstructionAsmType
 */
public class InstructionsAsmType implements AsmType {
  @Nullable
  private static InstructionsAsmType INSTANCE;

  private InstructionsAsmType() {
  }

  /**
   * Get the singleton instance of this AsmType.
   *
   * @return instance of the AsmType
   */
  public static AsmType instance() {
    if (INSTANCE == null) {
      INSTANCE = new InstructionsAsmType();
    }
    return INSTANCE;
  }

  @Override
  public String name() {
    return "instructions";
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
