package vadl.viam;

import vadl.utils.SourceLocation;

/**
 * Source level identifier class.
 */
public record Identifier(
    String[] parts,
    SourceLocation sourceLocation
) {

  Identifier(String name, SourceLocation sourceLocation) {
    this(new String[] {name}, sourceLocation);
  }

  Identifier(SourceLocation sourceLocation, String... parts) {
    this(parts, sourceLocation);
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
