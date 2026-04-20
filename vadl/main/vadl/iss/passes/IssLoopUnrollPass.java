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

package vadl.iss.passes;

import java.io.IOException;
import javax.annotation.CheckForNull;
import vadl.configuration.IssConfiguration;
import vadl.iss.passes.safeResourceRead.IssSafeResourceReadAnalysis;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Counter;
import vadl.viam.Instruction;
import vadl.viam.Specification;
import vadl.viam.graph.control.ForallNode;
import vadl.viam.passes.loopUnrolling.LoopUnroller;
import vadl.viam.passes.sideEffectScheduling.SideEffectSchedulingPass;

/**
 * Pass to unroll forall statements in the ISS.
 */
public class IssLoopUnrollPass extends AbstractIssPass {

  public IssLoopUnrollPass(IssConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("ISS Loop Unroll Pass");
  }

  @CheckForNull
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    tcgInstrs(viam).forEach(i -> new LoopUnroller(i.behavior()).run());

    var pc = viam.isa().get().pc();
    helperInstrs(viam)
        .filter(i -> i.behavior().getNodes(ForallNode.class).findAny().isPresent())
        .filter(i -> helperLoopMustBeUnrolled(i, pc))
        .forEach(i -> new LoopUnroller(i.behavior()).run());

    return null;
  }

  private boolean helperLoopMustBeUnrolled(Instruction instruction, @CheckForNull Counter pc) {
    var unrolled = instruction.copy();
    new LoopUnroller(unrolled.behavior()).run();

    var scheduledProbe = unrolled.copy();
    SideEffectSchedulingPass.schedule(scheduledProbe.behavior(), pc);
    return IssSafeResourceReadAnalysis.requiresReadSave(scheduledProbe);
  }
}
