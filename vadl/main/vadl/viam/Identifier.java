package vadl.viam;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import vadl.utils.SourceLocation;

/**
 * Source level identifier class.
 */
public record Identifier(
    String[] parts,
    SourceLocation sourceLocation
) {


  /**
   * Normalize the parts of the identifier by removing leading and trailing dots.
   */
  public Identifier {
    for (int i = 0; i < parts.length; i++) {
      parts[i] = normalizePart(parts[i]);
    }
  }

  public Identifier(String name, SourceLocation sourceLocation) {
    this(List.of(name), sourceLocation);
  }

  public Identifier(List<String> parts, SourceLocation sourceLocation) {
    this(parts.toArray(String[]::new),
        sourceLocation);
  }

  public static Identifier noLocation(String name) {
    return new Identifier(name, SourceLocation.INVALID_SOURCE_LOCATION);
  }

  /**
   * Prepend the given identifier scope to the current identifier.
   *
   * <p>The sourcelocation of the returned identifier is the same as of
   * {@code this} identifier.</p>
   *
   * @param scope The identifier scope to prepend.
   * @return The new identifier with the scope prepended.
   */
  public Identifier prepend(Identifier scope) {
    return new Identifier(
        Stream.concat(Arrays.stream(scope.parts), Arrays.stream(this.parts)).toArray(String[]::new),
        this.sourceLocation
    );
  }

  /**
   * Appends the given parts to the existing parts of the Identifier and
   * returns a new Identifier object.
   *
   * <p>The sourcelocation of the returned identifier is the same as of
   * {@code this} identifier.</p>
   *
   * @param parts The parts to be appended to the existing parts of the Identifier.
   * @return A new Identifier object with the appended parts.
   */
  public Identifier append(String... parts) {
    return new Identifier(
        Stream.concat(Arrays.stream(this.parts), Arrays.stream(parts)).toArray(String[]::new),
        this.sourceLocation
    );
  }

  /**
   * Creates a new identifier by copy with an extended simple name {@code suffix}.
   */
  public Identifier extendSimpleName(String suffix) {
    return new Identifier(
        Arrays.copyOf(this.parts, this.parts.length - 1),
        this.sourceLocation
    ).append(this.parts[this.parts.length - 1] + suffix);
  }

  /**
   * Returns a new Identifier object with the given source location.
   *
   * @param sourceLocation The source location to be set for the identifier.
   * @return A new Identifier object with the given source location.
   */
  public Identifier withSourceLocation(SourceLocation sourceLocation) {
    return new Identifier(
        Arrays.stream(this.parts).toArray(String[]::new),
        sourceLocation
    );
  }

  @Override
  public String toString() {
    return name();
  }

  public String name() {
    return String.join("::", this.parts);
  }

  public String simpleName() {
    return this.parts[this.parts.length - 1];
  }

  private static String normalizePart(String part) {
    // remove leading and trailing dots
    part = part.trim();
    if (part.startsWith(".")) {
      part = part.substring(1);
    }
    if (part.endsWith(".")) {
      part = part.substring(0, part.length() - 1);
    }
    return part;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Identifier that = (Identifier) o;
    return Arrays.equals(parts, that.parts);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(parts);
  }
}
