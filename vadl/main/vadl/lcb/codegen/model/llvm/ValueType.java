package vadl.lcb.codegen.model.llvm;

import vadl.types.SIntType;
import vadl.types.Type;

/**
 * LLVM types which can be used.
 */
public enum ValueType {
  I32("i32", "Int32"),

  I64("i64", "Int64");

  private final String llvmType;
  private final String fancyName;

  ValueType(String llvmType, String fancyName) {
    this.llvmType = llvmType;
    this.fancyName = fancyName;
  }

  /**
   * Map {@link Type} into a {@link ValueType}.
   */
  public static ValueType from(Type type) {
    if (type instanceof SIntType sint) {
      if (sint.bitWidth() == 32) {
        return ValueType.I32;
      } else if (sint.bitWidth() == 64) {
        return ValueType.I64;
      }
    }

    throw new RuntimeException("not implemented");
  }

  public String getFancyName() {
    return fancyName;
  }

  public String getLlvmType() {
    return llvmType;
  }
}
