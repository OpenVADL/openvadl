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

package vadl.vdt.impl.regular;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static vadl.vdt.target.common.DecisionTreeStatsCalculator.statistics;

import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vadl.AbstractTest;
import vadl.configuration.DecoderOptions;
import vadl.configuration.DumpMode;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.PassManager;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.vdt.model.Node;
import vadl.vdt.passes.VdtConstraintSynthesisPass;
import vadl.vdt.passes.VdtInputPreparationPass;
import vadl.vdt.passes.VdtLoweringPass;
import vadl.vdt.target.common.DecisionTreeStatsCalculator;
import vadl.vdt.target.dump.TextGraphGenerator;

class RiscVTest extends AbstractTest {

  private static final Logger log = LoggerFactory.getLogger(RiscVTest.class);

  @Test
  void testGenerateVDT() throws IOException, DuplicatedPassKeyException {

    /* GIVEN */

    var config = new GeneralConfiguration(Path.of("build/test-output"), DumpMode.NONE);
    config.getDecoderOptions().setGenerator(DecoderOptions.Generator.REGULAR);

    var spec = runAndGetViamSpecification("sys/risc-v/rv64im.vadl");

    var manager = new PassManager();
    manager.add(new VdtInputPreparationPass(config));
    manager.add(new VdtConstraintSynthesisPass(config));
    manager.add(new VdtLoweringPass(config));

    /* WHEN */
    manager.run(spec);

    /* THEN */

    var decodeTree = manager.getPassResults().lastResultOf(VdtLoweringPass.class, Node.class);

    assertNotNull(decodeTree);

    log.info("VDT:\n{}", new TextGraphGenerator(decodeTree).generate());

    final var stats = statistics(decodeTree);

    log.info("Statistics: {}", DecisionTreeStatsCalculator.statistics(decodeTree));

    assertEquals(78, stats.getNumberOfNodes());
    assertEquals(3, stats.getMaxDepth());
    assertEquals(1, stats.getMinDepth());
    assertEquals(2.05, Math.round(stats.getAvgDepth() * 100.0) / 100.0);
  }

}
