package vadl.types.asmTypes;

/**
 * {@code @register} refers to a register in the assembly language.
 * {@code @constant} and {@code @string} can be cast to {@code @register}.
 *
 * @see ConstantAsmType
 * @see StringAsmType
 */
public class RegisterAsmType implements AsmType {
  private static final RegisterAsmType INSTANCE = new RegisterAsmType();

  private RegisterAsmType() {
  }

  public static AsmType instance() {
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
