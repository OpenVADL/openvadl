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
    String[] lines = fileContent.split("\n", -1);
    boolean startOutOfBounds = true;
    boolean endOutOfBounds = true;

    var start = range.getStart();
    var end = range.getEnd();

    if (start.getLine() > end.getLine()
        || (start.getLine() == end.getLine() && start.getCharacter() > end.getCharacter())) {
      return "(ERROR: start position comes after end position)";
    }

    // Put end marker first so that start marker position is not affected by it
    if (end.getLine() < lines.length
        && end.getCharacter() <= lines[end.getLine()].length()) {
      endOutOfBounds = false;
      var line = lines[end.getLine()];
      lines[end.getLine()] = line.substring(0, end.getCharacter())
          + "</>" + line.substring(end.getCharacter());
    }

    if (start.getLine() < lines.length
        && start.getCharacter() <= lines[start.getLine()].length()) {
      startOutOfBounds = false;
      var line = lines[start.getLine()];
      lines[start.getLine()] = line.substring(0, start.getCharacter())
          + "<RANGE>" + line.substring(start.getCharacter());
    }

    return SEPARATOR_LINE + "\n  " + String.join("\n  ", lines)
        + "\n" + SEPARATOR_LINE + (startOutOfBounds ? "\n(start position is out-of-bounds)" : "")
        + (endOutOfBounds ? "\n(end position is out-of-bounds)" : "");
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
