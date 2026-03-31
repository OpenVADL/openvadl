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

import static vadl.types.BuiltInTable.SMULL;
import static vadl.types.BuiltInTable.SUMULL;
import static vadl.types.BuiltInTable.UMULL;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.types.BuiltInTable;
import vadl.types.DataType;
import vadl.types.Type;
import vadl.utils.Pair;
import vadl.utils.Triple;
import vadl.viam.Specification;
import vadl.viam.ViamError;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.SignExtendNode;
import vadl.viam.graph.dependency.ZeroExtendNode;

/**
 * Z3 has not the semantic for multiplication.
 * This passes replaces the built-ins with the extended implementation.
 */
public class ExtendMultiplicationPass extends Pass {
  public ExtendMultiplicationPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("ExtendMultiplicationPass");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam)
      throws IOException {
    ArrayList<Triple<BuiltInCall, ExpressionNode, Node>> worklist = new ArrayList<>();
    viam.isa().map(isa -> isa.ownInstructions().stream())
        .orElse(Stream.empty())
        .flatMap(instruction -> instruction.behavior().getNodes(BuiltInCall.class))
        .forEach(builtinCall -> {
          if (builtinCall.builtIn() == SMULL
              || builtinCall.builtIn() == UMULL
              || builtinCall.builtIn() == BuiltInTable.SUMULL) {

            var argExtendNodes = doubleExtendArguments(builtinCall);
            worklist.add(new Triple<>(
                builtinCall,
                builtinCall.arguments().get(0),
                argExtendNodes.left())
            );
            worklist.add(new Triple<>(
                builtinCall,
                builtinCall.arguments().get(1),
                argExtendNodes.right())
            );
          }
        });

    for (var item : worklist) {
      var builtInCall = item.left();
      var originalArg = item.middle();
      var replacementArg = item.right();
      var addedNode = Objects.requireNonNull(builtInCall.graph()).add(replacementArg);
      builtInCall.replaceInput(originalArg, addedNode);
    }

    return null;
  }

  private static Pair<ExpressionNode, ExpressionNode> doubleExtendArguments(BuiltInCall mulCall) {

    var builtin = mulCall.builtIn();
    var leftArg = mulCall.arguments().get(0);
    var rightArg = mulCall.arguments().get(1);
    var width = ((DataType) leftArg.type()).bitWidth();
    var targetWidth = width * 2;

    if (builtin == SMULL) {
      return Pair.of(
          new SignExtendNode(leftArg, Type.signedInt(targetWidth)),
          new SignExtendNode(rightArg, Type.signedInt(targetWidth))
      );
    } else if (builtin == UMULL) {
      return Pair.of(
          new ZeroExtendNode(leftArg, Type.unsignedInt(targetWidth)),
          new ZeroExtendNode(rightArg, Type.unsignedInt(targetWidth))
      );
    } else if (builtin == SUMULL) {
      return Pair.of(
          new SignExtendNode(leftArg, Type.signedInt(targetWidth)),
          new ZeroExtendNode(rightArg, Type.unsignedInt(targetWidth))
      );
    } else {
      throw new ViamError("Unexpected builtin found: %s".formatted(builtin))
          .addContext("leftArg", leftArg)
          .addContext("rightArg", rightArg);
    }
  }
}
