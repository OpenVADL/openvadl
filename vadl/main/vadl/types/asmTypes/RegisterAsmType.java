package vadl.types.asmTypes;

import javax.annotation.Nullable;

/**
 * {@code @register} refers to a register in the assembly language.
 * {@code @constant} and {@code @string} can be cast to {@code @register}.
 *
 * @see ConstantAsmType
 * @see StringAsmType
 */
public class RegisterAsmType implements AsmType {
  @Nullable
  private static RegisterAsmType INSTANCE;

  private RegisterAsmType() {
  }

  /**
   * Get the singleton instance of this AsmType.
   *
   * @return instance of the AsmType
   */
  public static AsmType instance() {
    if (INSTANCE == null) {
      INSTANCE = new RegisterAsmType();
    }
    return INSTANCE;
  }

  @Override
  public String name() {
    return "register";
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
