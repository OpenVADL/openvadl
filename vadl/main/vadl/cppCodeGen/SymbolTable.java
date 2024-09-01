package vadl.cppCodeGen;

/**
 * This is a helper class for generating variable names for the OOP layer.
 */
public class SymbolTable {
  private final String prefix;
  private int state = 0;

  public SymbolTable() {
    prefix = "";
  }

  /**
   * Sets a prefix for the variables.
   */
  public SymbolTable(String prefix) {
    this.prefix = prefix;
  }

  /**
   * Offset the SymbolTable so it starts not with "a".
   */
  public SymbolTable(int offset) {
    this.state = offset;
    this.prefix = "";
  }

  /**
   * Get a symbol without modifying the state.
   */
  public String getLastVariable() {
    return prefix + getVariableBasedOnState(state);
  }

  /**
   * Generate a variable name. For example, "a", "ab", "xy" etc.
   */
  public String getNextVariable() {
    return prefix + getVariableBasedOnState(state++);
  }

  /**
   * Generates a variable for a given index without any prefix.
   */
  public static String getVariableBasedOnState(int i) {
    return i < 0 ? "" : getVariableBasedOnState((i / 26) - 1) + (char) (97 + i % 26);
  }
}
