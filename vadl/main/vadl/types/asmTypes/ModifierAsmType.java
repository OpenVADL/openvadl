package vadl.types.asmTypes;

/**
 * {@code @modifier} refers to a relocation defined
 * in the modifiers section of the assembly description.
 *
 * <p>The combination of a {@code @modifier}
 * and an {@code @expression} can be cast to {@code @operand}.</p>
 */
public class ModifierAsmType implements AsmType {
  private static final ModifierAsmType INSTANCE = new ModifierAsmType();

  private ModifierAsmType() {
  }

  public static AsmType instance() {
    return INSTANCE;
  }

  @Override
  public String name() {
    return "modifier";
  }

  @Override
  public boolean canBeCastTo(AsmType to) {
    return to == this || to == VoidAsmType.instance();
  }
}
