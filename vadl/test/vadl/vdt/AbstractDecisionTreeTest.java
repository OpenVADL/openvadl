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

package vadl.vdt;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import vadl.vdt.model.Node;
import vadl.vdt.target.common.DecisionTreeStatsCalculator;
import vadl.vdt.target.common.dto.DecisionTreeStatistics;
import vadl.vdt.utils.BitPattern;
import vadl.vdt.utils.Instruction;
import vadl.vdt.utils.PBit;
import vadl.viam.Identifier;
import vadl.viam.graph.Graph;

public class AbstractDecisionTreeTest {

  private static final Pattern QEMU_PATTERN_REGEX = Pattern.compile(
      "^([a-zA-Z]+)\\s+([01\\-.]+)\\s+(@\\w+)(\\s+\\w+=[01]+)*$");

  protected List<Instruction> parseQemuDecoding(String fileName) throws IOException {

    URL path = getClass().getResource("/testFiles/" + fileName);
    if (path == null) {
      throw new IllegalArgumentException("File not found: " + fileName);
    }

    List<String> lines = Files.readAllLines(Path.of(path.getPath()));
    Map<String, String> patterns = new HashMap<>();

    for (String line : lines) {
      if (line.isBlank() || line.startsWith("#")) {
        continue;
      }

      var matcher = QEMU_PATTERN_REGEX.matcher(line);
      if (!matcher.matches()) {
        // Something else, e.g. a field or format definition instead of an instruction pattern
        continue;
      }

      patterns.put(matcher.group(1), matcher.group(2));
    }

    return createInsns(patterns);
  }

  protected List<Instruction> createInsns(List<String> patterns) {
    Map<String, String> m = new HashMap<>();
    for (int i = 0; i < patterns.size(); i++) {
      m.put("insn_" + i, patterns.get(i));
    }
    return createInsns(m);
  }

  protected List<Instruction> createInsns(Map<String, String> patternsByName) {
    final List<Instruction> result = new ArrayList<>();

    for (Map.Entry<String, String> entry : patternsByName.entrySet()) {
      String name = entry.getKey();
      String insn = entry.getValue().replace(" ", "");

      // Prepare a dummy instruction with a unique name
      var id = Identifier.noLocation(name);
      var behaviour = new Graph("mock");
      var source = new vadl.viam.Instruction(id, behaviour, null, null) {
        @Override
        public String toString() {
          return name;
        }
      };

      // Prepare the bit pattern
      final PBit[] bits = new PBit[insn.length()];
      for (int i = 0; i < insn.length(); i++) {
        bits[i] = new PBit(insn.charAt(i) == '1' ? PBit.Value.ONE
            : (insn.charAt(i) == '0' ? PBit.Value.ZERO : PBit.Value.DONT_CARE));
      }
      var pattern = new BitPattern(bits);

      result.add(new Instruction(source, pattern.width(), pattern));
    }
    return result;
  }

  protected DecisionTreeStatistics getStats(Node tree) {
    return new DecisionTreeStatsCalculator().calculate(tree);
  }

}
