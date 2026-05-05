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

package vadl.gcb.passes;

import static vadl.viam.ViamError.ensurePresent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;
import vadl.configuration.GcbConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Instruction;
import vadl.viam.Specification;
import vadl.viam.graph.Graph;
import vadl.viam.graph.ReadsRegisterTensor;
import vadl.viam.graph.WritesRegisterTensor;
import vadl.viam.graph.dependency.ProcCallNode;
import vadl.viam.graph.dependency.ReadMemNode;
import vadl.viam.graph.dependency.WriteMemNode;
import vadl.viam.passes.SnapshotInstructionBehaviorPass;

/**
 * Compute the builtin attributes for an {@link Instruction}.
 */
public class DetermineBuiltinAttributesPass extends Pass {
  public DetermineBuiltinAttributesPass(GcbConfiguration gcbConfiguration) {
    super(gcbConfiguration);
  }

  @Override
  public PassName getName() {
    return new PassName("DetermineBuiltinAttributesPass");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    var snapshots =
        (Map<Instruction, Graph>) passResults.lastResultOf(SnapshotInstructionBehaviorPass.class);
    IdentityHashMap<Instruction, List<InstructionBuiltinAttributesCtx.Attribute>> map =
        new IdentityHashMap<>();

    for (var instruction : viam.isa().orElseThrow().ownInstructions()) {
      var snapshot = Objects.requireNonNull(snapshots.get(instruction));

      if (isRedFlag(viam, snapshot)) {
        continue;
      }

      var isNoMem = isNoMem(snapshot);
      var willReturn = willReturn(snapshot);
      var speculatable = speculatable(snapshot);

      var attributes = new ArrayList<InstructionBuiltinAttributesCtx.Attribute>();
      if (isNoMem) {
        attributes.add(InstructionBuiltinAttributesCtx.Attribute.NoMem);
      }
      if (willReturn) {
        attributes.add(InstructionBuiltinAttributesCtx.Attribute.WillReturn);
      } else {
        attributes.add(InstructionBuiltinAttributesCtx.Attribute.NoReturn);
      }
      if (speculatable) {
        attributes.add(InstructionBuiltinAttributesCtx.Attribute.Speculatable);
      }

      map.put(instruction, attributes);
      instruction.attachExtension(new InstructionBuiltinAttributesCtx(attributes));
    }

    return map;
  }

  private boolean isRedFlag(Specification viam, Graph snapshot) {
    var pc =
        Objects.requireNonNull(ensurePresent(viam.isa(), "must be present").pc()).registerTensor();
    // FIXME: Checking if the registers are equals is no sufficient for checking if the PC is read.
    //        A PC can be only a single register file entry -> also check indexing or use
    //        CanAccessCounter::isPcAccess
    //        See Issue #941
    var hasPc = snapshot.getNodes(ReadsRegisterTensor.class).filter(
        x -> x.registerTensor().isSingleRegister() && x.registerTensor() == pc).findAny().isEmpty();
    return !hasPc;
  }

  private boolean isMem(Graph snapshot) {
    return !snapshot.getNodes(WriteMemNode.class).toList().isEmpty()
        || !snapshot.getNodes(ReadMemNode.class).toList().isEmpty();
  }

  private boolean isNoMem(Graph snapshot) {
    return !isMem(snapshot);
  }

  private boolean willReturn(Graph snapshot) {
    return !snapshot.getNodes(WritesRegisterTensor.class).toList().isEmpty();
  }

  /**
   * Compute a special attribute for LLVM.
   * The following conditions must hold:
   * x) No side effects
   * x) Does not write memory
   * x) Does not perform I/O
   * x) Does not change global state
   * x) Cannot trap or fault
   * x) No division-by-zero traps
   */
  private boolean speculatable(Graph snapshot) {
    return isNoMem(snapshot) && snapshot.getNodes(ProcCallNode.class).toList().isEmpty();
  }
}
