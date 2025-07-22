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

package vadl.viam.passes;

import java.io.IOException;
import java.util.Collections;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.gcb.passes.operands.GenerateInstructionOperandsPass;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.InstructionSetArchitecture;
import vadl.viam.Specification;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.SignExtendNode;
import vadl.viam.graph.dependency.ZeroExtendNode;

/**
 * The compiler generator needs to have builtins where
 * all the arguments' is the same. Otherwise, LLVM will create type errors
 * during lowering. Optimally, this pass would be in the LCB. However,
 * we need to update the type before we calculate the operands
 * in {@link GenerateInstructionOperandsPass}.
 */
public class InsertZeroOrSignExtensionPass extends Pass {
  public InsertZeroOrSignExtensionPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("InsertZeroOrSignExtensionPass");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    for (var instruction : viam.isa().map(InstructionSetArchitecture::ownInstructions).orElse(
        Collections.emptyList())) {
      for (var behavior : instruction.behaviors()) {
        var builtins = behavior.getNodes(BuiltInCall.class).toList();

        for (var builtin : builtins) {
          if (builtin.arguments().isEmpty()) {
            continue;
          }

          // We have to make sure that all the arguments have the same type.
          var firstArg = builtin.arg(0);

          for (int i = 1; i < builtin.arguments().size(); i++) {
            var arg = builtin.arg(i);

            if (!arg.type().equals(firstArg.type())) {
              var hasToBeSignExtended = arg.type().asDataType().isSigned();

              if (hasToBeSignExtended) {
                var node = new SignExtendNode(arg, firstArg.type().asDataType());
                arg.replace(node);
                builtin.arguments().set(i, node);
              } else {
                // has to be zero extended
                var node = new ZeroExtendNode(arg, firstArg.type().asDataType());
                arg.replace(node);
                builtin.arguments().set(i, node);
              }
            }
          }
        }
      }
    }

    return null;
  }
}
