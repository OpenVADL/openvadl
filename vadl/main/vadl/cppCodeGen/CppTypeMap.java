package vadl.cppCodeGen;

import vadl.cppCodeGen.model.CppType;
import vadl.types.BitsType;
import vadl.types.BoolType;
import vadl.types.SIntType;
import vadl.types.Type;
import vadl.types.UIntType;

/**
 * This class is a mapping layer for {@link Type} and
 * the corresponding cpp type.
 */
public class CppTypeMap {
  /**
   * Returns the cpp type given the {@link Type}.
   */
  public static String getCppTypeNameByVadlType(Type type) {
    if (type instanceof BoolType) {
      return "bool";
    } else if (type instanceof SIntType && ((SIntType) type).bitWidth() == 1) {
      return "bool";
    } else if (type instanceof SIntType && ((SIntType) type).bitWidth() == 8) {
      return "int8_t";
    } else if (type instanceof SIntType && ((SIntType) type).bitWidth() == 16) {
      return "int16_t";
    } else if (type instanceof SIntType && ((SIntType) type).bitWidth() == 32) {
      return "int32_t";
    } else if (type instanceof SIntType && ((SIntType) type).bitWidth() == 64) {
      return "int64_t";
    } else if (type instanceof SIntType && ((SIntType) type).bitWidth() == 128) {
      return "int128_t";
    } else if (type instanceof UIntType && ((UIntType) type).bitWidth() == 1) {
      return "bool";
    } else if (type instanceof UIntType && ((UIntType) type).bitWidth() == 8) {
      return "uint8_t";
    } else if (type instanceof UIntType && ((UIntType) type).bitWidth() == 16) {
      return "uint16_t";
    } else if (type instanceof UIntType && ((UIntType) type).bitWidth() == 32) {
      return "uint32_t";
    } else if (type instanceof UIntType && ((UIntType) type).bitWidth() == 64) {
      return "uint64_t";
    } else if (type instanceof UIntType && ((UIntType) type).bitWidth() == 128) {
      return "uint128_t";
    } else if (type instanceof BitsType && ((BitsType) type).bitWidth() == 8) {
      return "uint8_t";
    } else if (type instanceof BitsType && ((BitsType) type).bitWidth() == 16) {
      return "uint16_t";
    } else if (type instanceof BitsType && ((BitsType) type).bitWidth() == 32) {
      return "uint32_t";
    } else if (type instanceof BitsType && ((BitsType) type).bitWidth() == 64) {
      return "uint64_t";
    } else if (type instanceof BitsType && ((BitsType) type).bitWidth() == 128) {
      return "uint128_t";
    } else if (type instanceof CppType cppType) {
      return cppType.lower();
    }

    throw new RuntimeException(String.format("not implemented: type %s", type.toString()));
  }

  /**
   * Determines the C++ unsigned integer type corresponding to a specified bit width.
   *
   * @param bitWidth the bit width of the desired C++ unsigned integer type
   * @return the corresponding C++ unsigned integer type as a String
   * @throws RuntimeException if the bit width does not match a known C++ unsigned integer type
   */
  public static String cppUintType(int bitWidth) {
    switch (bitWidth) {
      case 1:
        return "bool";
      case 8:
        return "uint8_t";
      case 16:
        return "uint16_t";
      case 32:
        return "uint32_t";
      case 64:
        return "uint64_t";
      default:
        throw new RuntimeException(String.format("not implemented: type %s", bitWidth));
    }

  }

  /**
   * Determines the C++ signed integer type corresponding to a specified bit width.
   *
   * @param bitWidth the bit width of the desired C++ signed integer type
   * @return the corresponding C++ signed integer type as a String
   * @throws RuntimeException if the bit width does not match a known C++ signed integer type
   */
  public static String cppSintType(int bitWidth) {
    switch (bitWidth) {
      case 8:
        return "int8_t";
      case 16:
        return "int16_t";
      case 32:
        return "int32_t";
      case 64:
        return "int64_t";
      default:
        throw new RuntimeException(String.format("not implemented: type %s", bitWidth));
    }
  }

  /**
   * Upcast the given type to the next fitting bit size.
   */
  public static BitsType upcast(Type type) {
    if (type instanceof BitsType cast) {
      return cast.withBitWidth(nextFittingBitSize(cast.bitWidth()));
    } else {
      throw new RuntimeException("Non bits type are not supported");
    }
  }

  private static int nextFittingBitSize(int old) {
    if (old == 1) {
      return 1;
    } else if (old > 1 && old <= 8) {
      return 8;
    } else if (old > 8 && old <= 16) {
      return 16;
    } else if (old > 16 && old <= 32) {
      return 32;
    } else if (old > 32 && old <= 64) {
      return 64;
    } else if (old > 64 && old <= 128) {
      return 128;
    }

    throw new RuntimeException("Types with more than 128 bits are not supported");
  }
}
