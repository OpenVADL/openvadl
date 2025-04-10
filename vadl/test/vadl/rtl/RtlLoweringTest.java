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
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vadl.AbstractTest;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassOrders;
import vadl.pass.PassResults;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.viam.Specification;

public class RtlLoweringTest extends AbstractTest {

  private static final Logger log = LoggerFactory.getLogger(RtlLoweringTest.class);

  private static final Set<String> instructions = Set.of(
      "ADD", "ADDI", "SUB",
      "LW", "SW",
      "JAL", "JALR", "BEQ"
  );

  // TODO remove, not really a test
  @Test
  void rtlLoweringTest() throws IOException, DuplicatedPassKeyException {
    var config =
        new GeneralConfiguration(Path.of("build/test-output"), true);

    var order = PassOrders.rtl(config);
    order.addAfterFirst(PassOrders.ViamCreationPass.class, new PruneIsaPass(config, instructions));

    setupPassManagerAndRunSpec("sys/risc-v/rv64i.vadl",
        order
    );
  }

  /**
   * Pass for testing that removes VIAM elements to get a simple test case.
   */
  public static class PruneIsaPass extends Pass {

    private final Set<String> instructions;

    /**
     * New prune ISA pass that removes all instructions, but the ones referenced by a set of names.
     *
     * @param config configuration
     * @param instructions set of instruction names
     */
    public PruneIsaPass(GeneralConfiguration config, Set<String> instructions) {
      super(config);
      this.instructions = instructions;
    }

    @Override
    public PassName getName() {
      return PassName.of("Prune ISA");
    }

    @Nullable
    @Override
    public Object execute(PassResults passResults, Specification viam) throws IOException {
      viam.isa().ifPresent(isa -> {
        isa.ownInstructions().removeIf(ins -> !instructions.contains(ins.simpleName()));
      });
      return null;
    }

  }
}
