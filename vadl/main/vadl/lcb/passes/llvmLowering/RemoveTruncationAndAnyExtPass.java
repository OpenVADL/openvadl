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

package vadl.lcb.passes.llvmLowering;

import java.io.IOException;
import java.util.Collections;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.InstructionSetArchitecture;
import vadl.viam.Specification;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.SignExtendNode;
import vadl.viam.graph.dependency.TruncateNode;
import vadl.viam.graph.dependency.ZeroExtendNode;

/**
 * Removes {@link TruncateNode} or {@link SignExtendNode} or {@link ZeroExtendNode} if they
 * are not required.
 */
public class RemoveTruncationAndAnyExtPass extends Pass {

  /**
   * Constructor.
   */
  public RemoveTruncationAndAnyExtPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("RemoveTruncationAndAnyExtPass");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    for (var instruction : viam.isa().map(InstructionSetArchitecture::ownInstructions)
        .orElse(Collections.emptyList())) {
      for (var behavior : instruction.behaviors()) {
        var candidates = behavior.getNodes(BuiltInCall.class)
            .filter(x -> !x.builtIn().hasSameBitWidth())
            .toList();

        if (behavior.getNodes(SignExtendNode.class).findAny().isPresent()
            || behavior.getNodes(ZeroExtendNode.class).findAny().isPresent()) {
          continue;
        }

        for (var candidate : candidates) {
          for (var arg : candidate.arguments()) {
            if (arg instanceof TruncateNode truncateNode) {
              truncateNode.replaceAndDelete(truncateNode.value());
            }
          }
        }
      }
    }

    return null;
  }
}
