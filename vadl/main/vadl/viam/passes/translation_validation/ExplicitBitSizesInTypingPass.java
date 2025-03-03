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

package vadl.viam.passes.translation_validation;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.types.BitsType;
import vadl.viam.Instruction;
import vadl.viam.Specification;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * The {@link TranslationValidation#lower(Specification, Instruction, Instruction)} can only work
 * with explicit types. However, that is usually not required for the VIAM's happy flow since the
 * code generation works better on fewer nodes. This pass helps to verify the
 * {@link Instruction#behavior()} by inserting explicit types.
 */
public class ExplicitBitSizesInTypingPass extends Pass {
  public ExplicitBitSizesInTypingPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("ExplicitTypingPass");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam)
      throws IOException {
    viam.isa().map(isa -> isa.ownInstructions().stream())
        .orElse(Stream.empty())
        .flatMap(instruction -> instruction.behavior().getNodes(BuiltInCall.class))
        .filter(node -> !node.arguments().isEmpty())
        .filter(
            // Only relevant if all the arguments have BitsType.
            node -> node.arguments()
                .stream()
                .map(ExpressionNode::type)
                .filter(x -> x instanceof BitsType)
                .collect(Collectors.toSet())
                .size() != 1)
        .forEach(node -> {
          List<BitsType> types =
              node.arguments().stream().map(ExpressionNode::type)
                  .map(x -> x.asDataType().toBitsType()).toList();
          var join = node.arguments().get(0).type().asDataType().toBitsType()
              .join(types);

          node.arguments().forEach(arg -> arg.setType(join));
        });

    return null;
  }
}
