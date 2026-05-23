// SPDX-FileCopyrightText : © 2026 TU Wien <vadl@tuwien.ac.at>
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

package vadl.iss.passes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static vadl.TestUtils.findDefinitionByNameIn;

import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import vadl.AbstractTest;
import vadl.configuration.DumpMode;
import vadl.configuration.GeneralConfiguration;
import vadl.configuration.IssConfiguration;
import vadl.iss.passes.tcg.lowering.nodes.TcgGvecOpNode;
import vadl.iss.passes.vector.IssDirectGvecLoweringPass;
import vadl.pass.PassOrders;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.viam.Instruction;
import vadl.viam.Specification;
import vadl.viam.graph.control.ForallNode;

public class IssDirectGvecLoweringPassTest extends AbstractTest {

  @Test
  void lowersRecognizedVectorLoopIntoBackendGvecNode()
      throws IOException, DuplicatedPassKeyException {
    var viam = analyze("sys/risc-v/rv64v.vadl");
    var instr = findInstruction(viam, "RV64IMV::VADD_VV");

    assertFalse(instr.behavior().getNodes(ForallNode.class).findAny().isPresent());
    assertEquals(1, instr.behavior().getNodes(TcgGvecOpNode.class).count());
  }

  @Test
  void leavesFallbackVectorInstructionWithoutBackendGvecNode()
      throws IOException, DuplicatedPassKeyException {
    var viam = analyze("sys/risc-v/rv64v.vadl");
    var instr = findInstruction(viam, "RV64IMV::VADD_VX");

    assertTrue(instr.behavior().getNodes(ForallNode.class).findAny().isPresent());
    assertEquals(0, instr.behavior().getNodes(TcgGvecOpNode.class).count());
  }

  @Test
  void lowersAliasVectorBenchLoopIntoBackendGvecNode()
      throws IOException, DuplicatedPassKeyException {
    var viam = analyze("sys/vectorbench/vectorbench64.vadl");
    var instr = findInstruction(viam, "VectorBench64::VADD_DO_VV");

    assertFalse(instr.behavior().getNodes(ForallNode.class).findAny().isPresent());
    assertEquals(1, instr.behavior().getNodes(TcgGvecOpNode.class).count());
  }

  private Specification analyze(String specPath) throws IOException, DuplicatedPassKeyException {
    return setupPassManagerAndRunSpec(specPath,
        PassOrders.iss(config()).untilFirst(IssDirectGvecLoweringPass.class)
    ).specification();
  }

  private IssConfiguration config() {
    return new IssConfiguration(
        new GeneralConfiguration(Path.of("build/test-output"), DumpMode.NONE)
    );
  }

  private Instruction findInstruction(Specification viam, String name) {
    return findDefinitionByNameIn(name, viam, Instruction.class);
  }
}
