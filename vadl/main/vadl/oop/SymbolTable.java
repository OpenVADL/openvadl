package vadl.oop;

public class SymbolTable {
  private int i = 0;

  public String getNextVariable() {
    return str(i++);
  }

  private static String str(int i) {
    return i < 0 ? "" : str((i / 26) - 1) + (char)(97 + i % 26);
  }
}
