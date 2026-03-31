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

package vadl.lcb.passes.llvmLowering;

import java.io.IOException;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.gcb.passes.DetermineRegisterUsesAndDefsPass;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Specification;
import vadl.viam.graph.control.InstrEndNode;
import vadl.viam.graph.dependency.WriteRegTensorNode;

/**
 * Remove all register writes which are not PC writes. We already determined in
 * {@link DetermineRegisterUsesAndDefsPass} the uses and defs.
 */
public class RemoveRegisterWritesPass extends Pass {
  public RemoveRegisterWritesPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("RemoveRegisterReadsAndWritesPass");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    for (var instruction : viam.isa().orElseThrow().ownInstructions()) {
      var affectedWrites = instruction.behavior().getNodes(WriteRegTensorNode.class)
          .filter(x -> x.registerTensor().isSingleRegister() && !x.isPcAccess())
          .toList();

      for (var element : affectedWrites) {
        if (element.usages().count() == 1
            && element.usages().toList().getFirst() instanceof InstrEndNode instrEndNode) {
          instrEndNode.sideEffects().remove(element);
          element.clearUsages();
          element.safeDelete();
        }
      }
    }

    return null;
  }
}
