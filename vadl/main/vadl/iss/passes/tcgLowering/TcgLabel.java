package vadl.iss.passes.tcgLowering;

/**
 * Represents a TCG (Tiny Code Generation) label.
 *
 * <p>This class is a record that holds a single string value, which is the name of the label.
 */
public record TcgLabel(String varName) {

  @Override
  public String toString() {
    return varName;
  }
}