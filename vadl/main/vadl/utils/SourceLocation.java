package vadl.utils;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.jetbrains.annotations.NotNull;

/**
 * References a location span in source.
 *
 * @param uri   uri to concrete source file
 * @param begin the span begin with line and column
 * @param end   the span end with line and column
 */
public record SourceLocation(
    URI uri,
    SourcePosition begin,
    SourcePosition end
) {

  private static final URI INVALID_MEMORY = URI.create("memory://unknown");

  public static final SourceLocation INVALID_SOURCE_LOCATION =
      new SourceLocation(INVALID_MEMORY, 0);

  public SourceLocation(URI uri, SourcePosition begin) {
    this(uri, begin, begin);
  }

  public SourceLocation(URI uri, int lineBegin, int lineEnd) {
    this(uri, new SourcePosition(lineBegin), new SourcePosition(lineEnd));
  }

  public SourceLocation(URI uri, int line) {
    this(uri, line, line);
  }

  public boolean isValid() {
    return !this.uri.equals(INVALID_MEMORY);
  }


  /**
   * Joins this source location with another source location.
   *
   * @param other The source location to join with.
   * @return The joined source location or the invalid one, if an original one is invalid
   * @throws IllegalArgumentException if they point to different files.
   */
  public SourceLocation join(SourceLocation other) {
    if (!this.isValid() || !other.isValid()) {
      return INVALID_SOURCE_LOCATION;
    }

    if (!this.uri.equals(other.uri)) {
      throw new IllegalArgumentException(
          "Cannot join source locations that point to different files.");
    }

    SourcePosition begin = this.begin.compareTo(other.begin) < 0 ? this.begin : other.begin;
    SourcePosition end = this.end.compareTo(other.end) > 0 ? this.end : other.end;

    return new SourceLocation(this.uri, begin, end);
  }


  /**
   * Returns a new {@code SourceLocation} object representing the intersection
   * of this {@code SourceLocation} and the specified {@code SourceLocation} other.
   *
   * <p>It will return a new {@code SourceLocation} object representing the intersection
   * of this {@code SourceLocation} and the specified {@code SourceLocation} or
   * the invalid one, if one of the original source locations are invalid.</p>
   *
   * @param other the {@code SourceLocation} to intersect with this {@code SourceLocation}
   * @return a new {@code SourceLocation} object representing the intersection
   * @throws IllegalArgumentException if this and other point to different files,
   *                                  or if the source locations do not intersect
   */
  public SourceLocation meet(SourceLocation other) throws IllegalArgumentException {
    if (!this.isValid() || !other.isValid()) {
      return INVALID_SOURCE_LOCATION;
    }

    if (!this.uri.equals(other.uri)) {
      throw new IllegalArgumentException(
          "Cannot intersect source locations that point to different files.");
    }

    if (this.end.compareTo(other.begin) < 0 || other.end.compareTo(this.begin) < 0) {
      throw new IllegalArgumentException("The source locations do not intersect.");
    }

    SourcePosition begin = (this.begin.compareTo(other.begin) > 0) ? this.begin : other.begin;
    SourcePosition end = (this.end.compareTo(other.end) < 0) ? this.end : other.end;

    return new SourceLocation(this.uri, begin, end);
  }

  /**
   * Produces a concise version of a given location.
   *
   * <p>E.g.: {@code SourceLocation("/absolute/path/to/file.vadl", (1, 3), (2, 4))}
   * becomes  {@code "file.vadl:1:3..2:4"}
   * </p>
   */
  public String toConciseString() {
    var uriAsString = this.uri.toString();
    var indexOfLastSlash = uriAsString.lastIndexOf('/');
    return uriAsString.substring(indexOfLastSlash + 1)
        + ":"
        + this.begin
        + " .. "
        + this.end;
  }

  /**
   * Reads the content of the source file at this location and
   * returns it as String.
   */
  public String toSourceString() {
    if (!this.isValid()) {
      return "Invalid source location: " + this;
    }

    try (Stream<String> lines = Files.lines(Paths.get(uri))) {
      if (begin.line <= 0) {
        return "Invalid source location: " + this;
      }

      var lineDiff = end.line - begin.line;
      var sourceLines = lines.skip(begin.line - 1).limit(lineDiff + 1)
          .collect(Collectors.toCollection(ArrayList::new));

      var lineNumber = sourceLines.size();
      return IntStream.range(0, lineNumber)
          .mapToObj(i -> {
            var line = sourceLines.get(i);
            if (i == lineNumber - 1 && end.column != -1) {
              line = line.substring(0, end.column - 1);
            }

            if (i == 0 && begin.column != -1) {
              line = line.substring(begin.column - 1);
            }
            return line;
          })
          .collect(Collectors.joining("\n"));

    } catch (IOException e) {
      e.printStackTrace();
      return "Failed to load source location " + this.toConciseString() + ": " + e.getMessage();
    }
  }

  @Override
  public String toString() {
    return uri + ":" + begin + ".." + end;
  }


  /**
   * Represents a position in the source file with line and column information.
   */
  public record SourcePosition(
      int line,
      int column
  ) implements Comparable<SourcePosition> {

    public SourcePosition(int line) {
      this(line, -1);
    }

    @Override
    public String toString() {
      if (column < 0) {
        return "" + line;
      }
      return line + ":" + column;
    }

    @Override
    public int compareTo(@NotNull SourceLocation.SourcePosition other) {
      if (this.line < other.line) {
        return -1;
      } else if (this.line > other.line) {
        return 1;
      } else {
        return Integer.compare(this.column, other.column);
      }
    }
  }
}
