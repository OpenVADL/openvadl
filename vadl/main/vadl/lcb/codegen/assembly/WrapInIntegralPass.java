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

package vadl.lcb.codegen.assembly;

import java.io.IOException;
import java.util.List;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.gcb.passes.IdentifyFieldUsagePass;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.types.BuiltInTable;
import vadl.viam.InstructionSetArchitecture;
import vadl.viam.Specification;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.FieldRefNode;

/**
 * Wraps the register indices in an {@code integral} function when
 * register usage is not used with the {@code register} builtin.
 */
public class WrapInIntegralPass extends Pass {
  /**
   * Constructor.
   */
  public WrapInIntegralPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("WrapInIntegralPass");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    var fieldUsages =
        (IdentifyFieldUsagePass.ImmediateDetectionContainer) passResults.lastResultOf(
            IdentifyFieldUsagePass.class);

    viam.isa().map(InstructionSetArchitecture::ownInstructions).orElse(List.of()).forEach(
        instruction -> {
          var fields =
              instruction.assembly().function().behavior().getNodes(FieldRefNode.class).toList();

          for (var field : fields) {
            var usages = fieldUsages.getFieldUsages(instruction, field.formatField());
            // If the field is a register
            if (usages.stream().anyMatch(x -> x == IdentifyFieldUsagePass.FieldUsage.REGISTER)) {
              // and the number value is used
              if (field.usages().anyMatch(x -> x instanceof BuiltInCall builtInCall
                  && (builtInCall.builtIn() == BuiltInTable.HEX
                  || builtInCall.builtIn() == BuiltInTable.SDEC
                  || builtInCall.builtIn() == BuiltInTable.UDEC))) {
                // then wrap the node with the integral function.
                field.replace(
                    new BuiltInCall(BuiltInTable.INTEGRAL, new NodeList<>(field), field.type()));
              }
            }
          }
        });

    return null;
  }
}
