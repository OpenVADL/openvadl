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

  /**
   * Check if the input string is a valid assembly grammar type.
   * The input string is valid if it is equal to the lowercase string representation
   * of any assembly grammar type.
   *
   * @param input the input string to check
   * @return true if the input string is a valid assembly type, false otherwise
   */
  public static boolean isInputAsmType(String input) {
    return Arrays.stream(AsmType.values())
        .anyMatch(type -> type.toString().toLowerCase().equals(input));
  }
}
