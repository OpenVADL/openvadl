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

package vadl.ast;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import vadl.utils.SingleFileVirtualFileSystem;

class AstAdvancedDumperTest {

  @Test
  void rendersAstIdsAndIndexesTopLevelDefinitions() {
    var source = "constant X = 1\n";
    var path = Path.of("memory");
    var fileSystem = new SingleFileVirtualFileSystem(source, path);
    var ast = VadlParser.parse(path, fileSystem);

    var dump = AstAdvancedDumper.dump(ast, fileSystem, "2026-08-27 12:00:00");

    assertTrue(dump.contains("id=\"ast-node-0\""));
    assertTrue(dump.contains("const sourceIndex = [[0,14,0,0,14]"));
    assertTrue(dump.contains("sourceElement.addEventListener(\"pointermove\""));
    assertTrue(dump.contains("highlight(nodeElement, false)"));
    assertTrue(dump.contains("<script id=\"expandedFromData\" type=\"application/json\">"));
    assertFalse(dump.contains("expanded-from-location"));
  }

  @Test
  void rendersExpandedFromLocationsAsCompactData() {
    var source = """
        model abc() : Ex = { 6 }

        model xyz(arg: Ex): Defs = {
          constant flo = $abc / $arg
        }

        $xyz(7)
        """;
    var path = Path.of("memory");
    var fileSystem = new SingleFileVirtualFileSystem(source, path);
    var ast = VadlParser.parse(path, fileSystem);

    var dump = AstAdvancedDumper.dump(ast, fileSystem, "2026-08-27 12:00:00");

    assertTrue(dump.contains("type=\"application/json\">[[\"memory\"],[["));
    assertFalse(dump.contains("expanded-from-location"));

    var dataStart = dump.indexOf("<script id=\"expandedFromData\"");
    var dataEnd = dump.indexOf("</script>", dataStart);
    var matcher = Pattern.compile("\\[(\\d+)(?:,\\d+){5,}]")
        .matcher(dump.substring(dataStart, dataEnd));
    var previousNodeId = -1;
    while (matcher.find()) {
      var nodeId = Integer.parseInt(matcher.group(1));
      assertTrue(nodeId > previousNodeId);
      previousNodeId = nodeId;
    }
    assertTrue(previousNodeId >= 0);
  }
}
