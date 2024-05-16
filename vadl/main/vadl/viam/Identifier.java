package vadl.viam;

import java.util.Arrays;
import java.util.List;
import vadl.utils.SourceLocation;

/**
 * Source level identifier class.
 */
public record Identifier(
    String[] parts,
    SourceLocation sourceLocation
) {

  public Identifier(String name, SourceLocation sourceLocation) {
    this(new String[] {name}, sourceLocation);
  }

  public Identifier(List<String> parts, SourceLocation sourceLocation) {
    this(parts.toArray(String[]::new), sourceLocation);
  }


  @Override
  public String toString() {
    return String.join("::", this.parts);
  }

  public String name() {
    return String.join("::", this.parts);
  }

  public String simpleName() {
    return this.parts[this.parts.length - 1];
  }

}
