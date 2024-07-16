package vadl.oop;

import vadl.types.BoolType;
import vadl.types.SIntType;
import vadl.types.Type;
import vadl.types.UIntType;

public class CppTypeMap {
  public static String getCppTypeNameByVadlType(Type type) {
    if (type instanceof BoolType) {
      return "bool";
    } else if (type instanceof SIntType && ((SIntType) type).bitWidth() == 8) {
      return "char";
    } else if (type instanceof SIntType && ((SIntType) type).bitWidth() == 16) {
      return "short int";
    } else if (type instanceof SIntType && ((SIntType) type).bitWidth() == 32) {
      return "long int";
    } else if (type instanceof SIntType && ((SIntType) type).bitWidth() == 64) {
      return "long long int";
    } else if (type instanceof UIntType && ((UIntType) type).bitWidth() == 8) {
      return "unsigned char";
    } else if (type instanceof UIntType && ((UIntType) type).bitWidth() == 16) {
      return "unsigned short int";
    } else if (type instanceof UIntType && ((UIntType) type).bitWidth() == 32) {
      return "unsigned long int";
    } else if (type instanceof UIntType && ((UIntType) type).bitWidth() == 64) {
      return "unsigned long long int";
    }

    throw new RuntimeException("not implemented");
  }
}
