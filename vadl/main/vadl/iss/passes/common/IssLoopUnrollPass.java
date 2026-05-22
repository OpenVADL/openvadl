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

package vadl.iss.passes.common;

import java.io.IOException;
import javax.annotation.Nullable;
import vadl.configuration.IssConfiguration;
import vadl.iss.passes.AbstractIssPass;
import vadl.iss.passes.common.safeResourceRead.IssSafeResourceReadAnalysis;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Instruction;
import vadl.viam.Specification;
import vadl.viam.graph.control.ForallNode;
import vadl.viam.passes.canonicalization.Canonicalizer;
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

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    scalarTcgInstrs(viam).forEach(i -> new LoopUnroller(i.behavior()).run());

    wholeHelperInstrs(viam)
        .filter(i -> i.behavior().getNodes(ForallNode.class).findAny().isPresent())
        .filter(this::helperLoopMustBeUnrolled)
        .forEach(i -> new LoopUnroller(i.behavior()).run());

    // Direct-gvec candidates keep their normalized vector loop structure for a future
    // graph-based vector lowering pass.
    return null;
  }

  private boolean helperLoopMustBeUnrolled(Instruction instruction) {
    // Probe the unrolled form: unrolling removes iteration boundaries, so the scheduled graph
    // exposes cross-iteration read/write conflicts that the loop form hides (the loop form
    // has only a single scheduled write per body, which always passes the conflict check).
    // IssSafeResourceReadAnalysis knows how to treat CHUNK accesses with disjoint constant
    // (bitOffset, bitWidth) windows as independent, so same-chunk reuse patterns (e.g. a
    // per-element tensor write where iteration i reads and writes the same chunk) do not
    // trigger a false-positive unroll decision.
    var unrolled = instruction.copy();
    new LoopUnroller(unrolled.behavior()).run();
    // Fold the constants that unrolling produced (e.g. `VADL::mul(const(i), const(w))` → const)
    // so that IssSafeResourceReadAnalysis can see literal CHUNK (bitOffset, bitWidth) windows
    // and partition accesses by disjoint constant windows.
    Canonicalizer.canonicalize(unrolled.behavior());

    var scheduledProbe = unrolled.copy();
    SideEffectSchedulingPass.schedule(scheduledProbe.behavior());
    return IssSafeResourceReadAnalysis.requiresReadSave(scheduledProbe);
  }
}
