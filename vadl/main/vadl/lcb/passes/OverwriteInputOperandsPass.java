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

package vadl.lcb.passes;

import java.io.IOException;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.lcb.graph.DefinedImmediateSideEffectNode;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Specification;
import vadl.viam.annotations.DefineOperandAnnotation;
import vadl.viam.graph.dependency.FieldRefNode;

/**
 * Reads all the {@link DefineOperandAnnotation} and adds them as
 * {@link DefinedImmediateSideEffectNode}.
 */
public class OverwriteInputOperandsPass extends Pass {
  public OverwriteInputOperandsPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("OverwriteInputOperandsPass");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    var machineInstructions = viam.isa().stream()
        .flatMap(isa -> isa.ownInstructions().stream())
        .filter(x -> x.hasAnnotation(DefineOperandAnnotation.class));

    machineInstructions.forEach(machineInstruction -> {
      var annotation = machineInstruction.expectAnnotation(DefineOperandAnnotation.class);

      for (var field : annotation.inputs()) {
        machineInstruction.behavior().addWithInputs(
            new DefinedImmediateSideEffectNode(null, new FieldRefNode(field, field.type())));
      }
    });

    return null;
  }
}
