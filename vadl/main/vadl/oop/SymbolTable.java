package vadl.oop;

/**
 * This is a helper class for generating variable names for the OOP layer.
 */
public class SymbolTable {
  private int state = 0;

  /**
   * Generate a variable name. For example, "a", "ab", "xy" etc.
   */
  public String getNextVariable() {
    return str(state++);
  }

  private static String str(int i) {
    return i < 0 ? "" : str((i / 26) - 1) + (char) (97 + i % 26);
  }
}
