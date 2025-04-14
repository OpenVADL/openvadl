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

package vadl.vdt.impl.regular;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import vadl.vdt.AbstractDecisionTreeTest;
import vadl.vdt.model.Node;
import vadl.vdt.utils.Instruction;

class RiscV64ITest extends AbstractDecisionTreeTest {

  @Test
  void test_generate_tree() throws IOException {

    /* GIVEN */
    final List<Instruction> riscV = parseQemuDecoding("rv64i.decode");

    /* WHEN */
    Node tree = new RegularDecodeTreeGenerator().generate(riscV);

    /* THEN */

    assertNotNull(tree);

    final var stats = getStats(tree);
    assertEquals(riscV.size(), stats.getNumberOfLeafNodes(),
        "Expected one leaf node per instruction");

    assertEquals(65, stats.getNumberOfNodes());
    assertEquals(3, stats.getMaxDepth());
    assertEquals(1, stats.getMinDepth());
    assertEquals(2.06, Math.round(stats.getAvgDepth() * 100.0) / 100.0);
  }

}
