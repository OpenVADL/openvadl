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

package vadl.iss.passes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static vadl.TestUtils.findDefinitionByNameIn;
import static vadl.utils.GraphUtils.getSingleNode;

import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import vadl.AbstractTest;
import vadl.configuration.GeneralConfiguration;
import vadl.configuration.IssConfiguration;
import vadl.pass.PassOrders;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.types.BuiltInTable;
import vadl.viam.Instruction;
import vadl.viam.graph.control.IfNode;
import vadl.viam.graph.control.ScheduledNode;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.passes.sideEffectScheduling.SideEffectSchedulingPass;
import vadl.viam.passes.sideEffectScheduling.nodes.InstrExitNode;

/**
 * We need this tests as there is no occurrence of the tested problems in the RISC-V specification.
 */
public class IssTcgSchedulingPassTest extends AbstractTest {

  @Test
  public void test_schedule_in_both_branches() throws IOException, DuplicatedPassKeyException {
    var config =
        new IssConfiguration(new GeneralConfiguration(Path.of("build/test-output"), false));

    var setup = setupPassManagerAndRunSpec("passes/issTcgScheduling/valid_branch_1.vadl",
        PassOrders.iss(config)
            .untilFirst(SideEffectSchedulingPass.class)
    );
    var viam = setup.specification();

    var test1 = findDefinitionByNameIn("ValidBranch::TEST1", viam, Instruction.class);

    var builtInCalls = test1.behavior().getNodes(BuiltInCall.class)
        .filter(b -> b.builtIn() == BuiltInTable.ADD)
        .toList();
    assertEquals(1, builtInCalls.size());

    var instrExits = test1.behavior().getNodes(InstrExitNode.class).toList();

    // no scheduled nodes as all are instruction exits
    assertEquals(0, test1.behavior().getNodes(ScheduledNode.class).count());
    assertEquals(2, instrExits.size());

    var branchBegins = getSingleNode(test1.behavior(), IfNode.class)
        .branches();

    for (var begin : branchBegins) {
      var beginSucc = begin.next();
      assertTrue(instrExits.contains(beginSucc));
    }

  }

}
