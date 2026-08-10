// SPDX-FileCopyrightText : © 2026 TU Wien <vadl@tuwien.ac.at>
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

package vadl.lsp;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

public class TestUtils {
  private static final String SEPARATOR_LINE = "================";

  /**
   * Formats given (vadl) file content in a way that makes it clearer where the file starts and
   * ends when used in a snapshot.
   */
  public static String formatFileContent(String fileContent) {
    return SEPARATOR_LINE + "\n  " + fileContent.replace("\n", "\n  ")
        + "\n" + SEPARATOR_LINE;
  }

  /**
   * Displays a position marker within the given file content.
   *
   * @param position the lsp position
   * @param fileContent the entire content of the file which should contain position
   * @return given fileContent with position marked, formatted for use in a snapshot
   */
  public static String showPositionInFile(Position position, String fileContent) {
    String[] lines = fileContent.split("\n", -1);
    boolean outOfBounds = true;

    if (position.getLine() < lines.length
        && position.getCharacter() <= lines[position.getLine()].length()) {
      outOfBounds = false;
      var line = lines[position.getLine()];
      lines[position.getLine()] = line.substring(0, position.getCharacter())
          + "<POS>" + line.substring(position.getCharacter());
    }

    return SEPARATOR_LINE + "\n  " + String.join("\n  ", lines)
        + "\n" + SEPARATOR_LINE + (outOfBounds ? "\n(position is out-of-bounds)" : "");
  }

  /**
   * Displays a range marker within the given file content.
   *
   * @param range the lsp range (with start and end position)
   * @param fileContent the entire content of the file which should contain range
   * @return given fileContent with range marked, formatted for use in a snapshot
   */
  public static String showRangeInFile(Range range, String fileContent) {
    return showRangeInFile(range, fileContent, "RANGE");
  }

  /**
   * Displays a range marker within the given file content.
   *
   * @param range the lsp range (with start and end position)
   * @param fileContent the entire content of the file which should contain range
   * @param name the name of this range, used as part of the marker. Should be uppercase for
   *             aesthetic reasons.
   * @return given fileContent with range marked, formatted for use in a snapshot
   */
  public static String showRangeInFile(Range range, String fileContent, String name) {
    return showRangesInFile(List.of(new NamedRange(range, name)), fileContent);
  }

  public record NamedRange(Range range, String name) {}

  /**
   * Displays several range markers within the given file content.
   *
   * @param ranges all ranges to display, with lsp range and a name (used as part of the marker;
   *               should be uppercase for aesthetic reasons)
   * @param fileContent the entire content of the file which should contain given ranges
   * @return given fileContent with ranges marked, formatted for use in a snapshot
   */
  public static String showRangesInFile(List<NamedRange> ranges, String fileContent) {
    // Create list of all insertions sorted by position
    record PositionedString(Position position, String string) {}

    List<PositionedString> positionedStrings = new ArrayList<>(ranges.size() * 2);
    for (var nr : ranges) {
      var start = nr.range.getStart();
      var end = nr.range.getEnd();
      if (start.getLine() > end.getLine()
          || (start.getLine() == end.getLine() && start.getCharacter() > end.getCharacter())) {
        return "(ERROR: start position comes after end position)";
      }
      positionedStrings.add(new PositionedString(nr.range.getStart(), "<" + nr.name + ">"));
      positionedStrings.add(new PositionedString(nr.range.getEnd(), "</" + nr.name + ">"));
    }
    positionedStrings.sort((ps1, ps2) -> {
      var pos1 = ps1.position();
      var pos2 = ps2.position();
      if (pos1.getLine() != pos2.getLine()) {
        return pos1.getLine() - pos2.getLine();
      }
      if (pos1.getCharacter() != pos2.getCharacter()) {
        return pos1.getCharacter() - pos2.getCharacter();
      }
      return 0;
    });

    String[] lines = fileContent.split("\n", -1);
    List<String> outOfBoundsStrings = new ArrayList<>();
    // Do insertions in reverse order so that positions do not have to be adjusted
    for (var ps : positionedStrings.reversed()) {
      var pos = ps.position();
      if (pos.getLine() >= lines.length || pos.getCharacter() > lines[pos.getLine()].length()) {
        outOfBoundsStrings.add(ps.string);
        continue;
      }
      var line = lines[pos.getLine()];
      lines[pos.getLine()] = line.substring(0, pos.getCharacter())
          + ps.string() + line.substring(pos.getCharacter());
    }

    return SEPARATOR_LINE + "\n  " + String.join("\n  ", lines)
        + "\n" + SEPARATOR_LINE + (!outOfBoundsStrings.isEmpty()
        ? "\n(out-of-bounds: " + String.join(", ", outOfBoundsStrings) + ")" : "");
  }

  /**
   * Normalizes the given lsp uri string so that tests don't depend on absolute system-dependent
   * paths.
   *
   * @param uri as present in some lsp4j data structure
   * @return shortened variant of uri, which only contains the last two path components
   */
  public static String normalizeUri(String uri) {
    var parts = uri.split("/");
    String result = ".../";
    if (parts.length >= 2) {
      result += parts[parts.length - 2] + "/";
    }
    result += parts[parts.length - 1];
    return result;
  }
}
