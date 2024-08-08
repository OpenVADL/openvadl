package vadl.cpp_codegen;

/**
 * This is a helper class for generating variable names for the OOP layer.
 */
public class SymbolTable {
  private int state = 0;

  /**
   * Generate a variable name. For example, "a", "ab", "xy" etc.
   */
  public String getNextVariable() {
    return getVariableBasedOnState(state++);
  }

  /**
   * Generates a variable for a given index.
   */
  public static String getVariableBasedOnState(int i) {
    return i < 0 ? "" : getVariableBasedOnState((i / 26) - 1) + (char) (97 + i % 26);
  }
}
