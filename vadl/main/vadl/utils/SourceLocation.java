// SPDX-FileCopyrightText : © 2025 TU Wien <vadl@tuwien.ac.at>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

package vadl.utils;

import static vadl.utils.EditorUtils.isIntelliJIDE;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * References a location span in source.
 *
 * @param uri          uri to concrete source file
 * @param begin        the span begin with line and column
 * @param end          the span end with line and column (this is inclusive)
 * @param expandedFrom pointing to the location of a macro instantiation from which the current ast
 *                     got expanded. This is useful to both print the code in the macro as well as
 *                     the invocation. Is null for location that weren't expanded.
 */
public record SourceLocation(
    URI uri,
    Position begin,
    Position end,
    @Nullable SourceLocation expandedFrom
) implements WithLocation {

  private static final URI INVALID_MEMORY = URI.create("memory://unknown");

  public static final SourceLocation INVALID_SOURCE_LOCATION =
      new SourceLocation(INVALID_MEMORY, 0);

  public SourceLocation(URI uri, Position begin, Position end) {
    this(uri, begin, end, null);
  }

  public SourceLocation(URI uri, Position begin) {
    this(uri, begin, begin);
  }

  public SourceLocation(URI uri, int lineBegin, int lineEnd) {
    this(uri, new Position(lineBegin), new Position(lineEnd));
  }

  public SourceLocation(URI uri, int line) {
    this(uri, line, line);
  }

  public boolean isValid() {
    return !this.uri.equals(INVALID_MEMORY);
  }

  public SourceLocation orDefault(SourceLocation defaultLocation) {
    return isValid() ? this : defaultLocation;
  }

  /**
   * Joins multiple source location together.
   *
   * @return The joined source location or the invalid one, if an original one is invalid
   * @throws IllegalArgumentException if they point to different files.
   */
  public static SourceLocation join(List<SourceLocation> others) {
    if (others.isEmpty()) {
      return SourceLocation.INVALID_SOURCE_LOCATION;
    }

    var joined = others.get(0);
    for (int i = 1; i < others.size(); i++) {
      var item = others.get(i);
      joined = joined.join(item);
    }

    return joined;
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

    Position begin = this.begin.compareTo(other.begin) < 0 ? this.begin : other.begin;
    Position end = this.end.compareTo(other.end) > 0 ? this.end : other.end;
    SourceLocation expanedFrom =
        Objects.equals(this.expandedFrom, other.expandedFrom)
            ? this.expandedFrom : null;

    return new SourceLocation(this.uri, begin, end, expanedFrom);
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

    Position begin = (this.begin.compareTo(other.begin) > 0) ? this.begin : other.begin;
    Position end = (this.end.compareTo(other.end) < 0) ? this.end : other.end;
    SourceLocation expanedFrom =
        Objects.equals(this.expandedFrom, other.expandedFrom)
            ? this.expandedFrom : null;

    return new SourceLocation(this.uri, begin, end, expanedFrom);
  }

  /**
   * Produces version that is easily understandable for IDE's.
   *
   * <p>E.g.: {@code SourceLocation("relative/path/to/file.vadl", (1, 3), (2, 4))}
   * becomes  {@code "relative/path/to/file.vadl:1:3"}
   * </p>
   */
  public String toIDEString() {
    if (!this.isValid()) {
      return "Source Location was lost";
    }

    String printablePath;

    if (isIntelliJIDE() || this.uri.getScheme().equals("memory")) {
      // IntelliJ integrated terminal needs special treatment
      printablePath = this.uri.toString();
    } else {
      var filePath = Paths.get(this.uri);
      var currentWorkingDir = Paths.get(System.getProperty("user.dir"));
      printablePath = currentWorkingDir.relativize(filePath).toFile().getPath();
    }

    return printablePath
        + ":"
        + this.begin;
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

  /**
   * Produces a URI-based representation of this source location.
   * All used IDEs should recognize this representation as clickable in console output.
   * For example, SourceLocation("/path/file.vadl", (1, 3), (2, 4))
   * becomes "file:///path/file.vadl:1:3 .. 2:4"
   */
  public String toUriString() {
    return uri.toString() + ":" + begin + " .. " + end;
  }

  @Override
  public String toString() {
    var printPath = !uri.getPath().isEmpty() ? uri.getPath() : "unknown";
    printPath += ":" + begin + ".." + end;
    return printPath;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SourceLocation that = (SourceLocation) o;
    return Objects.equals(uri, that.uri)
        && Objects.equals(begin, that.begin)
        && Objects.equals(end, that.end)
        && Objects.equals(expandedFrom, that.expandedFrom);
  }

  @Override
  public int hashCode() {
    return Objects.hash(uri, begin, end, expandedFrom);
  }

  @Override
  public SourceLocation location() {
    return this;
  }


  /**
   * Represents a position in the source file with line and column information.
   *
   * @param line   starting at 1, just as displayed in an IDE.
   * @param column starting at 1, just as displayed in an IDE.
   */
  public record Position(
      int line,
      int column
  ) implements Comparable<Position> {

    public Position(int line) {
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
    public int compareTo(@Nonnull SourceLocation.Position other) {
      if (this.line < other.line) {
        return -1;
      } else if (this.line > other.line) {
        return 1;
      } else {
        return Integer.compare(this.column, other.column);
      }
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Position position = (Position) o;
      return line == position.line && column == position.column;
    }

    @Override
    public int hashCode() {
      return Objects.hash(line, column);
    }
  }
}
