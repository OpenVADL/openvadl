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

package vadl.rtl;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Test;
import vadl.AbstractTest;
import vadl.configuration.DumpMode;
import vadl.configuration.GeneralConfiguration;
import vadl.configuration.RtlConfiguration;
import vadl.pass.PassOrders;
import vadl.pass.exception.DuplicatedPassKeyException;

/**
 * Simple test of the MiA synthesis and emitting RTL.
 * Only keeps a few instructions to get a simpler output.
 */
public class RtlEmitMinimizedRiscVTest extends AbstractTest {

  private static final Set<String> instructions = Set.of(
      "ADD", "ADDI", "AUIPC", "LW", "SW", "BEQ", "JAL", "JALR"
  );

  @Test
  void emitMinimizedRV32() throws IOException, DuplicatedPassKeyException {
    var generalConfig =
        new GeneralConfiguration(Path.of("build/test-output"), DumpMode.NONE);

    var config = new RtlConfiguration(generalConfig);
    config.setMemory(RtlConfiguration.Memory.async);
    config.setEmitDebugPrint(false);

    var order = PassOrders.rtl(config);
    order.addAfterFirst(PassOrders.ViamCreationPass.class, new PruneIsaPass(config, instructions));

    setupPassManagerAndRunSpec("sys/risc-v/mia/rv_5stage.vadl", order);
  }

}
