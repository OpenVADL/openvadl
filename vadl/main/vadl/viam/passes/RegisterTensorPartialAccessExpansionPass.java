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

package vadl.viam.passes;

import java.io.IOException;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.utils.ViamUtils;
import vadl.viam.Specification;
import vadl.viam.graph.Graph;
import vadl.viam.graph.control.AbstractEndNode;
import vadl.viam.graph.dependency.ReadRegTensorNode;
import vadl.viam.graph.dependency.WriteRegTensorNode;

/**
 * Expands partial base-register accesses into the concrete concatenated/sliced access shape.
 *
 * <p>The frontend now preserves partial register-tensor accesses such as {@code V(vd)} as one
 * {@link ReadRegTensorNode} / {@link WriteRegTensorNode}. This pass restores the old VIAM shape by
 * enumerating the missing register dimensions and lowering them to the exact read/write sequence
 * that downstream passes already understand.</p>
 */
public class RegisterTensorPartialAccessExpansionPass extends Pass {

  public RegisterTensorPartialAccessExpansionPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("Register Tensor Partial Access Expansion");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    ViamUtils.findAllBehaviors(viam).forEach(this::expandInBehavior);
    return null;
  }

  private void expandInBehavior(Graph behavior) {
    behavior.getNodes(ReadRegTensorNode.class)
        .filter(this::isPartialAccess)
        .toList()
        .forEach(this::expandRead);
    behavior.getNodes(WriteRegTensorNode.class)
        .filter(this::isPartialAccess)
        .toList()
        .forEach(this::expandWrite);
  }

  private boolean isPartialAccess(ReadRegTensorNode read) {
    return read.indices().size() < read.resourceDefinition().indexDimensions().size();
  }

  private boolean isPartialAccess(WriteRegTensorNode write) {
    return write.indices().size() < write.resourceDefinition().indexDimensions().size();
  }

  private void expandRead(ReadRegTensorNode read) {
    var replacement = PartialAccessExpansionSupport.expandReadValue(
        read.resourceDefinition(), read.indices(), read.staticCounterAccess());
    replacement.setSourceLocationIfNotSet(read.location());
    read.replaceAndDelete(replacement);
  }

  private void expandWrite(WriteRegTensorNode write) {
    var ends = write.usages()
        .filter(AbstractEndNode.class::isInstance)
        .map(AbstractEndNode.class::cast)
        .toList();
    var replacementByEnd = ends.stream()
        .map(end -> PartialAccessExpansionSupport.expandWriteEffects(write))
        .toList();

    for (int i = 0; i < ends.size(); i++) {
      var end = ends.get(i);
      var replacements = replacementByEnd.get(i);
      replacements.forEach(effect -> write.ensureGraph().addWithInputs(effect));
      end.replaceSideEffect(write, replacements);
    }

    if (write.usageCount() == 0) {
      write.safeDelete();
    }
  }
}
