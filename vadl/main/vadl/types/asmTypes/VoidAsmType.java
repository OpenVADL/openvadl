package vadl.types.asmTypes;

import javax.annotation.Nullable;
import vadl.types.Type;

/**
 * {@code @void} represents grammar elements without a value.
 */
public class VoidAsmType implements AsmType {
  @Nullable
  private static VoidAsmType INSTANCE;

  private VoidAsmType() {
  }

  /**
   * Get the singleton instance of this AsmType.
   *
   * @return instance of the AsmType
   */
  public static AsmType instance() {
    if (INSTANCE == null) {
      INSTANCE = new VoidAsmType();
    }
    return INSTANCE;
  }

  @Override
  public String name() {
    return "void";
  }

  @Override
  public Type toOperationalType() {
    return Type.void_();
  }

  @Override
  public String toCppTypeString(String prefix) {
    return "NoData";
  }

  @Override
  public boolean canBeCastTo(AsmType to) {
    return to == this;
  }

  @Override
  public String toString() {
    return "@" + name();
  }
}
