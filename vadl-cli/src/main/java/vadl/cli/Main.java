package vadl.cli;

import javax.annotation.Nullable;

/**
 * The VADL CLI entry class
 */
public class Main {
  public static void main(String[] args) {
    System.out.println("Hello world!");
  }

  public boolean test(@Nullable String t) {
    return t.contains("s");
  }
}