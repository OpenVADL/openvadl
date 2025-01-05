package vadl.types.asmTypes;

/**
 * {@code @instructions} represents a set of {@code @instruction}.
 * It is used when defining a repetition block containing an instruction.
 *
 * @see InstructionAsmType
 */
public class InstructionsAsmType implements AsmType {
  private static final InstructionsAsmType INSTANCE = new InstructionsAsmType();

  private InstructionsAsmType() {
  }

  public static AsmType instance() {
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
