// SPDX-FileCopyrightText : © 2025-2026 TU Wien <vadl@tuwien.ac.at>
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

import static java.util.Objects.requireNonNull;
import static vadl.utils.EditorUtils.isIntelliJIDE;

import java.nio.file.FileSystems;
import java.nio.file.Path;
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
 */
public sealed interface SourceLocation extends WithLocation, Comparable<SourceLocation>
    permits SourceLocation.DirectLocation, SourceLocation.ExpandedLocation {

  SourceLocation INVALID_SOURCE_LOCATION = new DirectLocation(null, 0);

  @Nullable
  Path path();

  Position begin();

  Position end();

  /**
   * A ergonomic constructor that will select the either {@link DirectLocation} or
   * {@link ExpandedLocation} based on the provided arguments.
   *
   * @param path of the source file
   * @param begin position
   * @param end position
   * @param expandedFrom stack of expanded locations
   * @return a source location instance
   */
  static SourceLocation of(@Nullable Path path, Position begin, Position end,
                           @Nullable List<DirectLocation> expandedFrom) {
    var primary = new DirectLocation(path, begin, end);
    if (expandedFrom == null || expandedFrom.isEmpty()) {
      return primary;
    }
    return new ExpandedLocation(primary, RopeList.of(expandedFrom));
  }

  /**
   * Return the stack of all the expandedFrom locations.
   * The stack is ordered from the most recent to outermost macro invocation.
   * The stack doesn't include the current location.
   *
   * @return the stack of all the expandedFrom locations.
   */
  default List<DirectLocation> expandedFromStack() {
    return switch (this) {
      case DirectLocation direct -> List.of();
      case ExpandedLocation expanded -> expanded.expandedFrom.toList();
    };
  }

  /**
   * Return the stack of all the expandedFrom locations.
   * The stack is ordered from the most recent to outermost macro invocation.
   * The stack doesn't include the current location.
   *
   * @return the stack of all the expandedFrom locations.
   */
  default List<DirectLocation> fullExpandedFromStack() {
    var inner = switch (this) {
      case DirectLocation direct -> direct;
      case ExpandedLocation expanded -> expanded.primaryLocation;
    };
    var list = new ArrayList<>(expandedFromStack());
    list.addFirst(inner);
    return list;
  }

  default boolean isValid() {
    return path() != null;
  }

  default SourceLocation orDefault(SourceLocation defaultLocation) {
    return isValid() ? this : defaultLocation;
  }

  /**
   * Joins multiple source location together.
   *
   * @return The joined source location or the invalid one, if an original one is invalid
   * @throws IllegalArgumentException if they point to different files.
   */
  static SourceLocation join(List<SourceLocation> others) {
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
  default SourceLocation join(SourceLocation other) {
    if (!this.isValid() || !other.isValid()) {
      return INVALID_SOURCE_LOCATION;
    }

    if (this.equals(other)) {
      return this;
    }

    if (!Objects.equals(this.path(), other.path())) {
      throw new IllegalArgumentException("Cannot join source locations from different files");
    }

    // Shortcut for direct locations
    if (this instanceof DirectLocation && other instanceof DirectLocation) {
      var begin = this.begin().compareTo(other.begin()) < 0 ? this.begin() : other.begin();
      var end = this.end().compareTo(other.end()) > 0 ? this.end() : other.end();
      return new DirectLocation(this.path(), begin, end);
    }

    // Shortcut if both have the same expansion stack
    if (this.expandedFromStack().equals(other.expandedFromStack())) {
      return new ExpandedLocation(new DirectLocation(this.path(), this.begin(), other.end()),
          RopeList.of(this.expandedFromStack()));
    }

    // The expansion stacks differ, let's find a shared substack or use the innermost invocation.
    // NOTE: If this regularly happens, investigate it further to provide better help.
    var thisStack = this.fullExpandedFromStack().reversed();
    var otherStack = other.fullExpandedFromStack().reversed();

    var firstLocation = thisStack.getLast();
    var secondLocation = otherStack.getLast();

    int minSize = Math.min(thisStack.size(), otherStack.size());
    for (int i = 0; i <= minSize; i++) {
      if (i == minSize) {
        // Reached end: use last of smaller, next of larger
        if (thisStack.size() < otherStack.size()) {
          secondLocation = otherStack.get(i);
        } else if (thisStack.size() > otherStack.size()) {
          firstLocation = thisStack.get(i);
        }
        break;
      }

      if (!thisStack.get(i).equals(otherStack.get(i))) {
        firstLocation = thisStack.get(i);
        secondLocation = otherStack.get(i);
        thisStack = thisStack.subList(i, thisStack.size());
        otherStack = otherStack.subList(i, otherStack.size());
        break;
      }
    }

    if (!Objects.equals(firstLocation.path(), secondLocation.path())) {
      throw new IllegalArgumentException("Cannot join source locations from different files.");
    }

    var begin =
        firstLocation.begin().compareTo(secondLocation.begin()) < 0 ? firstLocation.begin() :
            secondLocation.begin();
    var end = firstLocation.end().compareTo(secondLocation.end()) > 0 ? firstLocation.end() :
        secondLocation.end();
    var expanedFrom = Objects.equals(thisStack, otherStack) ? thisStack : null;

    return SourceLocation.of(firstLocation.path(), begin, end, expanedFrom);
  }

  /**
   * Create a new source location that is a copy of the current one with the provided expanded stack
   * appended to the existing stack.
   * This operation is especially performant and finishes in O(1) time.
   *
   * @param newExpandedFrom to be appended.
   * @return the new location.
   */
  default SourceLocation copyWithAppendedExpandedFrom(List<DirectLocation> newExpandedFrom) {
    return switch (this) {
      case DirectLocation direct -> new ExpandedLocation(direct, RopeList.of(newExpandedFrom));
      case ExpandedLocation original -> new ExpandedLocation(original.primaryLocation,
          original.expandedFrom.concat(RopeList.of(newExpandedFrom)));
    };
  }

  @Override
  default int compareTo(@Nonnull SourceLocation o) {
    if (Objects.equals(this.path(), o.path())) {
      return this.begin().compareTo(o.begin());
    }

    if (this.path() == null) {
      return -1;
    }
    if (o.path() == null) {
      return 1;
    }
    return this.path().compareTo(o.path());
  }

  /**
   * Modes of how the IDE should be detected or one format be forced.
   */
  enum IDEDetectionMode {
    AUTO, RELATIVE, ABSOLUTE
  }

  /**
   * Produces version that is easily understandable for IDE's.
   *
   * <p>E.g.: {@code SourceLocation("relative/path/to/file.vadl", (1, 3), (2, 4))}
   * becomes  {@code "relative/path/to/file.vadl:1:3"} for most Editors
   * becomes  {@code "file:///absolut/path/to/file.vadl:1:3"} for IntelliJ
   * </p>
   */
  default String toIDEString(VirtualFileSystem fileSystem, IDEDetectionMode mode,
                             boolean forceUnixPaths) {
    if (!this.isValid()) {
      return "Source Location was lost";
    }

    String printablePath;

    if (mode == IDEDetectionMode.ABSOLUTE || (mode == IDEDetectionMode.AUTO && isIntelliJIDE())) {
      // IntelliJ integrated terminal needs special treatment
      printablePath = "file://" + fileSystem.toAbsolutePath(requireNonNull(path()));
    } else {
      printablePath = fileSystem.toRelativePath(requireNonNull(path())).toString();
    }
    if (forceUnixPaths) {
      printablePath = printablePath.replace(FileSystems.getDefault().getSeparator(), "/");
    }

    return printablePath + ":" + this.begin();
  }

  /**
   * Produces a concise version of a given location.
   *
   * <p>E.g.: {@code SourceLocation("/absolute/path/to/file.vadl", (1, 3), (2, 4))}
   * becomes  {@code "file.vadl:1:3..2:4"}
   * </p>
   */
  default String toConciseString() {
    var uriAsString = this.path() != null ? "file://" + this.path() : "memory://invalid";
    var indexOfLastSlash = uriAsString.lastIndexOf('/');
    return uriAsString.substring(indexOfLastSlash + 1) + ":" + this.begin() + " .. " + this.end();
  }

  /**
   * Reads the content of the source file at this location and
   * returns it as String.
   */
  default String toSourceString(VirtualFileSystem fileSystem) {
    if (!this.isValid()) {
      return "Invalid source location: " + this;
    }

    try (Stream<String> lines = fileSystem.readLines(requireNonNull(this.path()))) {
      if (begin().line <= 0) {
        return "Invalid source location: " + this;
      }

      var lineDiff = end().line - begin().line;
      var sourceLines = lines.skip(begin().line - 1).limit(lineDiff + 1)
          .collect(Collectors.toCollection(ArrayList::new));

      var lineNumber = sourceLines.size();
      return IntStream.range(0, lineNumber).mapToObj(i -> {
        var line = sourceLines.get(i);
        if (i == lineNumber - 1 && end().column != -1) {
          line = line.substring(0, end().column - 1);
        }

        if (i == 0 && begin().column != -1) {
          line = line.substring(begin().column - 1);
        }
        return line;
      }).collect(Collectors.joining("\n"));

    }
  }

  /**
   * Produces a URI-based representation of this source location.
   * All used IDEs should recognize this representation as clickable in console output.
   * For example, SourceLocation("/path/file.vadl", (1, 3), (2, 4))
   * becomes "file:///path/file.vadl:1:3 .. 2:4"
   */
  default String toUriString() {
    if (path() == null) {
      return "memory://invalid";
    }
    return "file://" + path().toString() + ":" + begin() + " .. " + end();
  }

  /**
   * Same as the default equals, but ignores the expandedFrom field.
   *
   * @param other source location to compare to.
   * @return true if the source locations are equal, ignoring the expandedFrom field
   */
  @SuppressWarnings("ReferenceEquality") // I know what I'm doing.
  default boolean equalsIgnoringExpandedFrom(SourceLocation other) {
    if (this == other) {
      return true;
    }
    return Objects.equals(path(), other.path()) && Objects.equals(begin(), other.begin())
        && Objects.equals(end(), other.end());
  }

  @Override
  default SourceLocation location() {
    return this;
  }

  /**
   * A direct location that wasn't expanded from anywhere.
   *
   * @param path of the file.
   * @param begin position.
   * @param end position.
   */
  record DirectLocation(@Nullable Path path, Position begin, Position end)
      implements SourceLocation {

    public DirectLocation(@Nullable Path path, Position begin) {
      this(path, begin, begin);
    }
  }

  /**
   * A location that was expanded from another location.
   *
   * @param primaryLocation the innermost direct location.
   * @param expandedFrom    the stack of locations (like a macro backtrace) from where it was
   *                        expanded.
   */
  record ExpandedLocation(DirectLocation primaryLocation, RopeList<DirectLocation> expandedFrom)
      implements SourceLocation {

    @Nullable
    @Override
    public Path path() {
      return primaryLocation.path();
    }

    @Override
    public Position begin() {
      return primaryLocation.begin();
    }

    @Override
    public Position end() {
      return primaryLocation.end();
    }
  }

  /**
   * Represents a position in the source file with line and column information.
   *
   * @param line   starting at 1, just as displayed in an IDE.
   * @param column starting at 1, just as displayed in an IDE.
   */
  record Position(int line, int column) implements Comparable<Position> {

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
    public int compareTo(@Nonnull Position other) {
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

    /**
     * Returns true if this Position is located within the given {@code location}.
     *
     * <p>Note: This check only makes sense if this Position is known to belong to the same file
     * that {@code location} refers to.
     */
    public boolean isWithin(SourceLocation location) {
      // Both begin and end are inclusive
      return location.end().compareTo(this) >= 0 && location.begin().compareTo(this) <= 0;
    }
  }
}
