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

package vadl.ast;

import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import vadl.viam.passes.verification.ViamVerifier;

public class FrontendIntegrationTest {

  @ParameterizedTest
  @ValueSource(strings = {
      "../sys/aarch64/aarch64.vadl",
      "../sys/aarch64/aarch64-abi.vadl",
      "../sys/aarch64/sve.vadl",
      "../sys/aarch64/virt.vadl",
      "../sys/aarch64/vprocessor.vadl",
      "../sys/ppc64/ppc64.vadl",
      "../sys/risc-v/rv32i.vadl",
      "../sys/risc-v/rv32im.vadl",
      "../sys/risc-v/rv64i.vadl",
      "../sys/risc-v/rv64im.vadl",
      "../sys/risc-v/rv64v.vadl",
      "../sys/risc-v/rvcsr.vadl",
      "../sys/risc-v/mia/rv_1stage.vadl",
      "../sys/risc-v/mia/rv_3stage.vadl",
      "../sys/risc-v/mia/rv_5stage.vadl",
      "../sys/v-risc/ABI.vadl"
  })
  public void testFrontendPassingOnSysSpecs(String filename) {
    var path = Path.of(filename);
    Assertions.assertTrue(path.toFile().exists());
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(path),
        "Cannot parse input");
    new Ungrouper().ungroup(ast);
    new ModelRemover().removeModels(ast);
    var typechecker = new TypeChecker();
    Assertions.assertDoesNotThrow(() -> typechecker.verify(ast), "Program isn't typesafe");
    var lowering = new ViamLowering();
    var spec = Assertions.assertDoesNotThrow(() -> lowering.generate(ast), "Cannot generate VIAM");
    ViamVerifier.verifyAllIn(spec);
  }

}
