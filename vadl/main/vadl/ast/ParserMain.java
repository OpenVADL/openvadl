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

package vadl.ast;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Map;

/**
 * A simple VADL parser entry point to collect timings etc.
 */
public class ParserMain {
  /**
   * A simple VADL parser entry point to collect timings etc.
   *
   * @param args A single-element array containing the path to the VADL file.
   */
  public static void main(String[] args) throws IOException {
    if (args.length != 1) {
      System.out.println("Arguments: <path.to.vadl>");
      System.exit(1);
    }
    var file = args[0];
    VadlParser.parse(Paths.get(file), Map.of());
  }
}
