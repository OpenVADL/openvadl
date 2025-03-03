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

package vadl.gcb.passes.encodingGeneration.strategies.impl;

import javax.annotation.Nullable;
import vadl.gcb.passes.encodingGeneration.strategies.EncodingGenerationStrategy;
import vadl.types.BitsType;
import vadl.types.BuiltInTable;
import vadl.types.DataType;
import vadl.viam.Constant;
import vadl.viam.Format;
import vadl.viam.ViamError;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.FuncParamNode;
import vadl.viam.graph.dependency.SignExtendNode;
import vadl.viam.graph.dependency.SliceNode;
import vadl.viam.graph.dependency.TruncateNode;
import vadl.viam.graph.dependency.ZeroExtendNode;

/**
 * This strategy will create an encoding when the access function only contains add or sub.
 * <pre>{@code
 * format Utype : Inst =
 * { imm    : Bits<20>
 * , rd     : Index
 * , opcode : Bits7
 * , immU = ((31) as UInt<20>) - imm
 * }
 * }</pre>
 * This class should compute the following encoding function automatically:
 * <pre>{@code
 * encode {
 * imm => ((31) as UInt<20>) - immU
 * }
 * }</pre>
 */
public class ArithmeticImmediateStrategy implements EncodingGenerationStrategy {
  @Override
  public boolean checkIfApplicable(Format.FieldAccess fieldAccess) {
    var behavior = fieldAccess.accessFunction().behavior();
    return behavior.getNodes(BuiltInCall.class)
        .allMatch(x -> {
          var cast = (BuiltInCall) x;

          if (cast.builtIn() == BuiltInTable.ADD
              || cast.builtIn() == BuiltInTable.SUB) {
            return true;
          }

          return false;
        }) && behavior.getNodes(SliceNode.class).findAny().isEmpty();
  }

  @Override
  public void generateEncoding(Format.FieldAccess fieldAccess) {
    var parameter = setupEncodingForFieldAccess(fieldAccess);
    var accessFunction = fieldAccess.accessFunction();
    var copy = accessFunction.behavior().copy();
    final var returnNode = copy.getNodes(ReturnNode.class).findFirst().get();

    // Optimistic assumption: Remove all typecasts because they are not correct anymore when
    // inverted.
    copy.getNodes(SignExtendNode.class)
        .forEach(typeCastNode -> typeCastNode.replaceAndDelete(typeCastNode.value()));
    copy.getNodes(ZeroExtendNode.class)
        .forEach(typeCastNode -> typeCastNode.replaceAndDelete(typeCastNode.value()));
    copy.getNodes(TruncateNode.class)
        .forEach(typeCastNode -> typeCastNode.replaceAndDelete(typeCastNode.value()));

    // After that we need to find the field and add it to the other side.
    var fieldRefs = copy.getNodes(FieldRefNode.class).toList();
    var fieldRef = fieldRefs.get(0);
    var fieldRefBits = (BitsType) fieldRef.type();

    // Example
    // f(x) = x + 6
    // Let y = f(x)
    // y = x + 6
    // y - 6 = x
    // and
    // f(x) = x - 6
    // Let y = f(x)
    // y = x - 6
    // y + 6 = x
    // The heuristic just swaps the operators.

    var funcParam = new FuncParamNode(parameter);
    fieldRef.replaceAndDelete(funcParam);

    returnNode.applyOnInputs(new GraphVisitor.Applier<>() {
      @Nullable
      @Override
      public Node applyNullable(Node from, @Nullable Node to) {
        if (to != null) {
          to.applyOnInputs(this);
        }

        if (to instanceof BuiltInCall) {
          var cast = (BuiltInCall) to;
          if (cast.builtIn() == BuiltInTable.ADD) {
            cast.setBuiltIn(BuiltInTable.SUB);
          } else if (cast.builtIn() == BuiltInTable.SUB) {
            cast.setBuiltIn(BuiltInTable.ADD);
          }
        }

        return to;
      }
    });

    // At the end of the encoding function, the type must be exactly as the field type
    var sliceNode =
        new SliceNode(returnNode.value(), new Constant.BitSlice(new Constant.BitSlice.Part[] {
            new Constant.BitSlice.Part(fieldRefBits.bitWidth() - 1, 0)
        }), (DataType) fieldRef.type());
    var addedSliceNode = copy.add(sliceNode);
    returnNode.replaceInput(returnNode.value(), addedSliceNode);

    var encoding = fieldAccess.encoding();
    if (encoding != null) {
      encoding.setBehavior(copy);
    } else {
      throw new ViamError("An encoding must already exist");
    }
  }
}
