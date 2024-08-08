package vadl.cpp_codegen;

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
    }

    throw new RuntimeException(String.format("not implemented: type %s", type.toString()));
  }
}
