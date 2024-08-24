package vadl.lcb.codegen.model.llvm;

import vadl.types.BitsType;
import vadl.types.SIntType;
import vadl.types.Type;
import vadl.types.UIntType;

/**
 * LLVM types which can be used.
 */
public enum ValueType {
  I8("i8", "Int8"),
  I16("i16", "Int16"),
  I32("i32", "Int32"),

  I64("i64", "Int64"),
  U8("u8", "Uint8"),
  U16("u16", "Uint16"),

  U32("u32", "Uint32"),

  U64("u64", "Uint64");

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
      if (sint.bitWidth() == 8) {
        return ValueType.I8;
      } else if (sint.bitWidth() == 16) {
        return ValueType.I16;
      } else if (sint.bitWidth() == 32) {
        return ValueType.I32;
      } else if (sint.bitWidth() == 64) {
        return ValueType.I64;
      }
    } else if (type instanceof UIntType uint) {
      if (uint.bitWidth() == 8) {
        return ValueType.U8;
      } else if (uint.bitWidth() == 16) {
        return ValueType.U16;
      } else if (uint.bitWidth() == 32) {
        return ValueType.U32;
      } else if (uint.bitWidth() == 64) {
        return ValueType.U64;
      }
    } else if (type instanceof BitsType bitsType) {
      if (bitsType.bitWidth() == 8) {
        return ValueType.I8;
      } else if (bitsType.bitWidth() == 16) {
        return ValueType.I16;
      } else if (bitsType.bitWidth() == 32) {
        return ValueType.I32;
      } else if (bitsType.bitWidth() == 64) {
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
