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

package vadl.viam.passes.functionInliner;

import static vadl.utils.GraphUtils.getSingleNode;
import static vadl.utils.ViamUtils.findDefinitionsByFilter;

import java.io.IOException;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Instruction;
import vadl.viam.Specification;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.dependency.FieldAccessRefNode;

/**
 * FieldAccessInlinerPass is a transformation pass that inlines field access operations
 * in the VADL specification. It is responsible for identifying and replacing field
 * accesses with field access function body.
 */
public class FieldAccessInlinerPass extends Pass {
  public FieldAccessInlinerPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("Field Access Inliner Pass");
  }

  @Override
  public @Nullable Object execute(PassResults passResults, Specification viam)
      throws IOException {

    findDefinitionsByFilter(viam, d -> d instanceof Instruction)
        .stream().map(Instruction.class::cast)
        .forEach(instruction -> {
          var fieldAccesses = instruction.behavior().getNodes(FieldAccessRefNode.class)
              .toList();

          fieldAccesses.forEach(fieldAccessRefNode -> {
            var behavior = fieldAccessRefNode.fieldAccess().accessFunction().behavior();
            var returnNode = getSingleNode(behavior, ReturnNode.class);
            fieldAccessRefNode.replaceAndDelete(returnNode.value().copy());
          });
        });
    return null;
  }
}
