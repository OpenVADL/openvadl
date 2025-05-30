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

package vadl.vdt.impl.irregular;

import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vadl.AbstractTest;
import vadl.configuration.GeneralConfiguration;
import vadl.configuration.IssConfiguration;
import vadl.pass.PassManager;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.vdt.model.Node;
import vadl.vdt.passes.VdtInputPreparationPass;
import vadl.vdt.passes.VdtLoweringPass;
import vadl.vdt.target.common.DecisionTreeStatsCalculator;

class Aarch64Test extends AbstractTest {

  private static final Logger log = LoggerFactory.getLogger(Aarch64Test.class);

  @Test
  void testGenerateVDT() throws IOException, DuplicatedPassKeyException {

    /* GIVEN */

    var config =
        new IssConfiguration(new GeneralConfiguration(Path.of("build/test-output"), false));

    var spec = runAndGetViamSpecification("sys/aarch64/virt.vadl");

    var manager = new PassManager();
    manager.add(new VdtInputPreparationPass(config));
    manager.add(new VdtLoweringPass(config));

    /* WHEN */
    manager.run(spec);

    /* THEN */

    var decodeTree = manager.getPassResults().lastResultOf(VdtLoweringPass.class, Node.class);

    Assertions.assertNotNull(decodeTree);

    log.info("VDT: {}", DecisionTreeStatsCalculator.statistics(decodeTree));
  }
}
