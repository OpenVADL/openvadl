package vadl.types;

import java.util.Arrays;

/**
 * Types of the assembly grammar elements in the assembly description definition.
 */
public enum AsmType {
  CONSTANT,
  EXPRESSION,
  INSTRUCTION,
  MODIFIER,
  OPERAND,
  REGISTER,
  STRING,
  SYMBOL,
  VOID,
  STATEMENTS,
  INSTRUCTIONS,
  OPERANDS;

  public static boolean isInputAsmType(String input) {
    return Arrays.stream(AsmType.values())
        .anyMatch(type -> type.toString().toLowerCase().equals(input));
  }
}
