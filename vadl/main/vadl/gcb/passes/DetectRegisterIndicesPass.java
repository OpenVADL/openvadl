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

import java.io.IOException;
import java.util.HashSet;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Specification;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.ReadRegTensorNode;
import vadl.viam.graph.dependency.WriteRegTensorNode;

/**
 * Detect whether the {@link FieldRefNode} is used as a register index.
 * It is detected as register index when a usage is {@link ReadRegTensorNode}
 * or {@link WriteRegTensorNode}.
 */
public class DetectRegisterIndicesPass extends Pass {
  public DetectRegisterIndicesPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("DetectRegisterIndicesPass");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    var result = new HashSet<FieldRefNode>();

    viam.isa()
        .get()
        .ownInstructions()
        .stream()
        .flatMap(x -> x.behaviors().stream())
        .forEach(behavior -> {
          var fields = behavior.getNodes(FieldRefNode.class).toList();

          for (var field : fields) {
            var hasRead = field.usages().anyMatch(u -> u instanceof ReadRegTensorNode);
            var hasWrite = field.usages().anyMatch(u -> u instanceof WriteRegTensorNode);

            if (hasRead || hasWrite) {
              result.add(field);
            }
          }
        });

    return result;
  }
}
