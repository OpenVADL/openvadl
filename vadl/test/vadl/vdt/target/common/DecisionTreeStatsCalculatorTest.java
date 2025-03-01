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

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import vadl.vdt.AbstractDecisionTreeTest;
import vadl.vdt.impl.theiling.TheilingDecodeTreeGenerator;
import vadl.vdt.model.Node;
import vadl.vdt.target.common.dto.DecisionTreeStatistics;

class DecisionTreeStatsCalculatorTest extends AbstractDecisionTreeTest {

  @Test
  void testGenerate_statistics_1() {

    /* GIVEN */
    final var instructions = createInsns(List.of("1--", "01-", "00-"));

    /* WHEN */
    final Node dt = new TheilingDecodeTreeGenerator().generate(instructions);

    /* THEN */
    final DecisionTreeStatsCalculator calculator = new DecisionTreeStatsCalculator();
    final DecisionTreeStatistics stats = calculator.calculate(dt);

    Assertions.assertEquals(5, stats.getNumberOfNodes());
    Assertions.assertEquals(3, stats.getNumberOfLeafNodes());
    Assertions.assertEquals(1, stats.getMinDepth());
    Assertions.assertEquals(2, stats.getMaxDepth());
    Assertions.assertEquals(1.67, Math.round(stats.getAvgDepth() * 100) / 100.0);
  }

  @Test
  void testGenerate_statistics_2() {

    /* GIVEN */
    final var instructions = createInsns(List.of("1--", "01-", "000", "001"));

    /* WHEN */
    final Node dt = new TheilingDecodeTreeGenerator().generate(instructions);

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
    final Node dt = new TheilingDecodeTreeGenerator().generate(instructions);

    /* THEN */
    final DecisionTreeStatsCalculator calculator = new DecisionTreeStatsCalculator();
    final DecisionTreeStatistics stats = calculator.calculate(dt);

    Assertions.assertEquals(9, stats.getNumberOfNodes());
    Assertions.assertEquals(8, stats.getNumberOfLeafNodes());
    Assertions.assertEquals(1, stats.getMinDepth());
    Assertions.assertEquals(1, stats.getMaxDepth());
    Assertions.assertEquals(1, stats.getAvgDepth());
  }

}