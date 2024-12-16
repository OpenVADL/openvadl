package vadl.types.asmTypes;

/**
 * {@code @void} represents grammar elements without a value.
 */
public class VoidAsmType implements AsmType {
  private static final VoidAsmType INSTANCE = new VoidAsmType();

  private VoidAsmType() {
  }

  public static AsmType instance() {
    return INSTANCE;
  }

  @Override
  public String name() {
    return "void";
  }

  @Override
  public boolean canBeCastTo(AsmType to) {
    return to == this;
  }
}
