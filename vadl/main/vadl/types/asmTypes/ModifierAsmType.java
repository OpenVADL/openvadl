package vadl.types.asmTypes;

import javax.annotation.Nullable;

/**
 * {@code @modifier} refers to a relocation defined
 * in the modifiers section of the assembly description.
 *
 * <p>The combination of a {@code @modifier}
 * and an {@code @expression} can be cast to {@code @operand}.</p>
 */
public class ModifierAsmType implements AsmType {
  @Nullable
  private static ModifierAsmType INSTANCE;

  private ModifierAsmType() {
  }

  /**
   * Get the singleton instance of this AsmType.
   *
   * @return instance of the AsmType
   */
  public static AsmType instance() {
    if (INSTANCE == null) {
      INSTANCE = new ModifierAsmType();
    }
    return INSTANCE;
  }

  @Override
  public String name() {
    return "modifier";
  }

  @Override
  public String toCppTypeString(String prefix) {
    return prefix + "MCExpr::VariantKind";
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
