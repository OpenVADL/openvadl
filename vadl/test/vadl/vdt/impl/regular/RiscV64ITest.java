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
import static vadl.vdt.target.common.DecisionTreeStatsCalculator.statistics;

import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import vadl.AbstractTest;
import vadl.configuration.DecoderOptions;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.PassManager;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.vdt.model.Node;
import vadl.vdt.passes.VdtConstraintSynthesisPass;
import vadl.vdt.passes.VdtInputPreparationPass;
import vadl.vdt.passes.VdtLoweringPass;

class RiscV64ITest extends AbstractTest {

  @Test
  void test_generate_tree() throws IOException, DuplicatedPassKeyException {

    /* GIVEN */
    var config = new GeneralConfiguration(Path.of("build/test-output"), false);
    config.getDecoderOptions().setGenerator(DecoderOptions.Generator.REGULAR);

    var spec = runAndGetViamSpecification("sys/risc-v/rv64i.vadl");

    var manager = new PassManager();
    manager.add(new VdtInputPreparationPass(config));
    manager.add(new VdtConstraintSynthesisPass(config));
    manager.add(new VdtLoweringPass(config));

    /* WHEN */
    manager.run(spec);

    /* THEN */
    var tree = manager.getPassResults().lastResultOf(VdtLoweringPass.class, Node.class);

    assertNotNull(tree);

    final var stats = statistics(tree);

    assertEquals(65, stats.getNumberOfNodes());
    assertEquals(3, stats.getMaxDepth());
    assertEquals(1, stats.getMinDepth());
    assertEquals(2.06, Math.round(stats.getAvgDepth() * 100.0) / 100.0);
  }

}
