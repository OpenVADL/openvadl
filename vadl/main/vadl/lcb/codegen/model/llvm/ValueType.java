package vadl.lcb.codegen.model.llvm;

import java.util.Optional;
import vadl.types.BitsType;
import vadl.types.SIntType;
import vadl.types.Type;
import vadl.types.UIntType;

/**
 * LLVM types which can be used.
 */
public enum ValueType {
  I8("i8", "Int8", "Int8"),
  I16("i16", "Int16", "Int16"),
  I32("i32", "Int32", "Int32"),

  I64("i64", "Int64", "Int64"),

  // LLVM has no concept of unsigned integers because it's all interpretation.
  // That's why sometimes have to use signed names.
  U8("u8", "Uint8", "Int8"),
  U16("u16", "Uint16", "Int16"),
  U32("u32", "Uint32", "Int32"),
  U64("u64", "Uint64", "Int64");

  private final String llvmType;
  private final String fancyName;
  private final String tableGen;

  ValueType(String llvmType, String fancyName, String tableGen) {
    this.llvmType = llvmType;
    this.fancyName = fancyName;
    this.tableGen = tableGen;
  }

  /**
   * Get the bit width of the type.
   */
  public int getBitwidth() {
    return switch (this) {
      case I8 -> 8;
      case I16 -> 16;
      case I32 -> 32;
      case I64 -> 64;
      case U8 -> 8;
      case U16 -> 16;
      case U32 -> 32;
      case U64 -> 64;
    };
  }

  /**
   * Map {@link Type} into a {@link ValueType}.
   */
  public static Optional<ValueType> from(Type type) {
    if (type instanceof SIntType sint) {
      if (sint.bitWidth() == 8) {
        return Optional.of(ValueType.I8);
      } else if (sint.bitWidth() == 16) {
        return Optional.of(ValueType.I16);
      } else if (sint.bitWidth() == 32) {
        return Optional.of(ValueType.I32);
      } else if (sint.bitWidth() == 64) {
        return Optional.of(ValueType.I64);
      }
    } else if (type instanceof UIntType uint) {
      if (uint.bitWidth() == 8) {
        return Optional.of(ValueType.U8);
      } else if (uint.bitWidth() == 16) {
        return Optional.of(ValueType.U16);
      } else if (uint.bitWidth() == 32) {
        return Optional.of(ValueType.U32);
      } else if (uint.bitWidth() == 64) {
        return Optional.of(ValueType.U64);
      }
    } else if (type instanceof BitsType bitsType) {
      if (bitsType.bitWidth() == 8) {
        return Optional.of(ValueType.I8);
      } else if (bitsType.bitWidth() == 16) {
        return Optional.of(ValueType.I16);
      } else if (bitsType.bitWidth() == 32) {
        return Optional.of(ValueType.I32);
      } else if (bitsType.bitWidth() == 64) {
        return Optional.of(ValueType.I64);
      }
    }

    return Optional.empty();
  }

  public String getFancyName() {
    return fancyName;
  }

  public String getLlvmType() {
    return llvmType;
  }

  public String getTableGen() {
    return tableGen;
  }

  /**
   * Check whether the type is signed.
   */
  public boolean isSigned() {
    return switch (this) {
      case I8 -> true;
      case I16 -> true;
      case I32 -> true;
      case I64 -> true;
      default -> false;
    };
  }

  /**
   * Make the type signed.
   */
  public ValueType makeSigned() {
    int bitwith = getBitwidth();
    Type type = Type.signedInt(bitwith);
    return ValueType.from(type).get();
  }
}
