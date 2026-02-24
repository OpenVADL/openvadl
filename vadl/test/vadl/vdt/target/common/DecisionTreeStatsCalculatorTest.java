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

package vadl.vdt.target.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import vadl.types.BitsType;
import vadl.utils.Triple;
import vadl.vdt.AbstractDecisionTreeTest;
import vadl.vdt.impl.irregular.IrregularDecodeTreeGenerator;
import vadl.vdt.impl.irregular.model.DecodeEntry;
import vadl.vdt.impl.regular.RegularDecodeTreeGenerator;
import vadl.vdt.model.Node;
import vadl.vdt.target.common.dto.DecisionTreeStatistics;
import vadl.vdt.utils.BitPattern;
import vadl.viam.Encoding;
import vadl.viam.Format;
import vadl.viam.Identifier;
import vadl.viam.graph.Graph;

class DecisionTreeStatsCalculatorTest extends AbstractDecisionTreeTest {

  @Test
  void testGenerate_statistics_1() {

    /* GIVEN */
    final var instructions = createInsns(List.of("1--", "01-", "00-"));

    /* WHEN */
    final Node dt = new RegularDecodeTreeGenerator().generate(instructions);

    /* THEN */
    final DecisionTreeStatsCalculator calculator = new DecisionTreeStatsCalculator();
    final DecisionTreeStatistics stats = calculator.calculate(dt);

    Assertions.assertEquals(5, stats.getNumberOfNodes());
    Assertions.assertEquals(3, stats.getNumberOfLeafNodes());
    Assertions.assertEquals(1, stats.getMinDepth());
    Assertions.assertEquals(2, stats.getMaxDepth());
    Assertions.assertEquals(1.67, Math.round(stats.getAvgDepth() * 100) / 100.0);

    // No occurrence stats available
    Assertions.assertEquals(0, stats.getWeightedAvgDepth());
    Assertions.assertEquals(0, stats.getOccurrenceProbability());
  }

  @Test
  void testGenerate_statistics_2() {

    /* GIVEN */
    final var instructions = createInsns(List.of("1--", "01-", "000", "001"));

    /* WHEN */
    final Node dt = new RegularDecodeTreeGenerator().generate(instructions);

    /* THEN */
    final DecisionTreeStatsCalculator calculator = new DecisionTreeStatsCalculator();
    final DecisionTreeStatistics stats = calculator.calculate(dt);

    Assertions.assertEquals(7, stats.getNumberOfNodes());
    Assertions.assertEquals(4, stats.getNumberOfLeafNodes());
    Assertions.assertEquals(1, stats.getMinDepth());
    Assertions.assertEquals(3, stats.getMaxDepth());
    Assertions.assertEquals(2.25, stats.getAvgDepth());
  }

  @Test
  void testGenerate_statistics_3() {

    /* GIVEN */
    final var instructions = createInsns(
        List.of("100", "101", "110", "111", "010", "011", "000", "001"));

    /* WHEN */
    final Node dt = new RegularDecodeTreeGenerator().generate(instructions);

    /* THEN */
    final DecisionTreeStatsCalculator calculator = new DecisionTreeStatsCalculator();
    final DecisionTreeStatistics stats = calculator.calculate(dt);

    Assertions.assertEquals(9, stats.getNumberOfNodes());
    Assertions.assertEquals(8, stats.getNumberOfLeafNodes());
    Assertions.assertEquals(1, stats.getMinDepth());
    Assertions.assertEquals(1, stats.getMaxDepth());
    Assertions.assertEquals(1, stats.getAvgDepth());
  }

  @Test
  void testGenerate_statistics_with_cost_uniform() {

    /* GIVEN */
    // List.of("1--", "01-", "00-")
    final var instructions = createDecodeEntry(List.of(
        Triple.of("insn_0", BitPattern.fromString("1--", 3), 1 / 3.0),
        Triple.of("insn_1", BitPattern.fromString("01-", 3), 1 / 3.0),
        Triple.of("insn_2", BitPattern.fromString("00-", 3), 1 / 3.0)
    ));

    // insn & 0x4
    //  |- 0x0
    //  |  insn & 0x2
    //  |    |- 0x2 -> insn_1   Depth: 2 * 0.33
    //  |    |- 0x0 -> insn_2   Depth: 2 * 0.33
    //  |- 0x4 -> insn_0        Depth: 1 * 0.33

    /* WHEN */
    final Node dt = new IrregularDecodeTreeGenerator().generate(instructions);

    /* THEN */
    final DecisionTreeStatsCalculator calculator = new DecisionTreeStatsCalculator();
    final DecisionTreeStatistics stats = calculator.calculate(dt);

    Assertions.assertEquals(5, stats.getNumberOfNodes());
    Assertions.assertEquals(3, stats.getNumberOfLeafNodes());
    Assertions.assertEquals(1, stats.getMinDepth());
    Assertions.assertEquals(2, stats.getMaxDepth());
    Assertions.assertEquals(1.67, Math.round(stats.getAvgDepth() * 100) / 100.0);
    Assertions.assertEquals(1.67, Math.round(stats.getWeightedAvgDepth() * 100) / 100.0);
    Assertions.assertEquals(1.0, stats.getOccurrenceProbability());
  }

  @Test
  void testGenerate_statistics_with_cost_weighted() {

    /* GIVEN */
    // List.of("1--", "01-", "000", "001")
    final var instructions = createDecodeEntry(List.of(
        Triple.of("insn_0", BitPattern.fromString("1--", 3), .5),
        Triple.of("insn_1", BitPattern.fromString("01-", 3), .2),
        Triple.of("insn_2", BitPattern.fromString("000", 3), .1),
        Triple.of("insn_3", BitPattern.fromString("001", 3), .2)
    ));

    // insn & 0x4
    //  |- 0x4 -> insn_0            1 * .5
    //  |- 0x0
    //  |  insn & 0x2
    //  |    |- 0x0
    //  |    |  insn & 0x1
    //  |    |    |- 0x0 -> insn_2  3 * .1
    //  |    |    |- 0x1 -> insn_3  3 * .2
    //  |    |- 0x2 -> insn_1       2 * .2

    /* WHEN */
    final Node dt = new IrregularDecodeTreeGenerator().generate(instructions);

    /* THEN */
    final DecisionTreeStatsCalculator calculator = new DecisionTreeStatsCalculator();
    final DecisionTreeStatistics stats = calculator.calculate(dt);

    Assertions.assertEquals(7, stats.getNumberOfNodes());
    Assertions.assertEquals(4, stats.getNumberOfLeafNodes());
    Assertions.assertEquals(1, stats.getMinDepth());
    Assertions.assertEquals(3, stats.getMaxDepth());
    Assertions.assertEquals(2.25, stats.getAvgDepth());
    Assertions.assertEquals(1.8, stats.getWeightedAvgDepth());
    Assertions.assertEquals(1.0, stats.getOccurrenceProbability());
  }

  protected List<DecodeEntry> createDecodeEntry(List<Triple<String, BitPattern, Double>> insns) {
    final List<DecodeEntry> result = new ArrayList<>();

    for (Triple<String, BitPattern, Double> entry : insns) {
      String name = entry.left();
      BitPattern pattern = entry.middle();

      // Prepare a dummy instruction with a unique name
      var id = Identifier.noLocation(name);
      var behaviour = new Graph("mock");
      var encoding = new Encoding(Identifier.noLocation("enc"),
          new Format(Identifier.noLocation("format"), BitsType.unsignedInt(pattern.width())),
          new Encoding.Field[0]);
      var source = new vadl.viam.Instruction(id, behaviour, null, encoding) {
        @Override
        public String toString() {
          return name;
        }
      };

      result.add(new DecodeEntry(source, pattern.width(), pattern, Set.of(), entry.right()));
    }
    return result;
  }

}