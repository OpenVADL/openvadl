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

import vadl.gcb.passes.encodingGeneration.strategies.EncodingGenerationStrategy;
import vadl.types.BuiltInTable;
import vadl.viam.Constant;
import vadl.viam.Format;
import vadl.viam.PrintableInstruction;
import vadl.viam.ViamError;
import vadl.viam.graph.Graph;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.control.StartNode;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.SliceNode;

/**
 * This strategy will create an encoding when the immediate is shifted.
 * <pre>{@code
 * format Utype : Inst =
 * {     imm    : Bits<20>
 * , rd     : Index
 * , opcode : Bits7
 * , ImmediateU = ( imm, 0 as Bits<12> ) as UInt
 * }
 * }</pre>
 * This class should compute the following encoding function automatically:
 * <pre>{@code
 * encode {
 * imm => ImmediateU(31..12)
 * }
 * }</pre>
 */
public class ShiftedImmediateStrategy implements EncodingGenerationStrategy {
  @Override
  public boolean checkIfApplicable(Format.FieldAccess fieldAccess) {
    // Checks whether the behavior only contains (logical or arithmetic) left or right shift.
    // But only one logical operation is allowed.
    var behavior = fieldAccess.accessFunction().behavior();
    return behavior.getNodes(BuiltInCall.class)
        .allMatch(x -> {
          var cast = (BuiltInCall) x;

          if (cast.builtIn() == BuiltInTable.LSL) {
            return true;
          }

          return false;
        }) && behavior.getNodes(BuiltInCall.class).count() == 1;
  }

  @Override
  public void generateEncoding(PrintableInstruction printableInstruction,
                               Format.FieldAccess fieldAccess) {
    var accessFunction = fieldAccess.accessFunction();
    var fieldRef = fieldAccess.fieldRef();

    var originalShift =
        accessFunction.behavior().getNodes(BuiltInCall.class).findFirst().get();
    var shiftValue =
        ((Constant.Value) ((ConstantNode) originalShift.arguments()
            .get(1)).constant()).integer();

    ExpressionNode invertedSliceNode;
    if (originalShift.builtIn() == BuiltInTable.LSL) {
      // If the decode function has a left shift,
      // then we need to extract the original shifted value.
      // We compute an upper bound which is the shift value plus the size of the field
      // and a lower bound which is the shifted value.
      var upperBound = shiftValue.intValue() + fieldRef.size() - 1;
      var lowerBound = shiftValue.intValue();
      var slice = new Constant.BitSlice(
          Constant.BitSlice.Part.of(upperBound, lowerBound));
      invertedSliceNode =
          new SliceNode(new FieldAccessRefNode(fieldAccess, fieldAccess.type()), slice,
              fieldRef.type());
    } else {
      throw new ViamError("Inverting builtin is not supported");
    }

    var returnNode = new ReturnNode(invertedSliceNode);
    var startNode = new StartNode(returnNode);

    var behavior = new Graph("Generated encoding of " + fieldAccess.simpleName());
    behavior.addWithInputs(returnNode);
    behavior.add(startNode);
    setFieldEncoding(printableInstruction, fieldAccess, behavior);
  }
}
