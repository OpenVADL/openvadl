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

import java.math.BigInteger;
import vadl.error.Diagnostic;
import vadl.gcb.passes.encodingGeneration.strategies.EncodingPredicateGenerationStrategy;
import vadl.types.BuiltInTable;
import vadl.types.Type;
import vadl.utils.GraphUtils;
import vadl.utils.SourceLocation;
import vadl.viam.Constant;
import vadl.viam.Format;
import vadl.viam.Identifier;
import vadl.viam.Instruction;
import vadl.viam.Parameter;
import vadl.viam.ViamError;
import vadl.viam.graph.Graph;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.control.StartNode;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.FuncParamNode;
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
public class ShiftedImmediateStrategyPredicate implements EncodingPredicateGenerationStrategy {
  @Override
  public boolean checkIfApplicable(Format.FieldAccess fieldAccess) {
    // Check if only one field
    if (fieldAccess.fieldRefs().size() > 1) {
      return false;
    }

    // Checks whether the behavior only contains (logical or arithmetic) left or right shift.
    // But only one logical operation is allowed.
    var behavior = fieldAccess.accessFunction().behavior();
    return behavior.getNodes(BuiltInCall.class)
        .allMatch(x ->
            x.builtIn() == BuiltInTable.LSL)
        && behavior.getNodes(BuiltInCall.class).count() == 1
        && behavior.getNodes(ConstantNode.class).count() == 1;
  }

  @Override
  public void generateEncodingAndPredicateFunction(Instruction printableInstruction,
                                                   Format.FieldAccess fieldAccess) {
    generateEncoding(printableInstruction, fieldAccess);
    generatePredicate(printableInstruction, fieldAccess);
  }

  @Override
  public void generatePredicate(Instruction instruction, Format.FieldAccess fieldAccess) {
    var trueCase = new ConstantNode(Constant.Value.fromBoolean(true));
    var falseCase = new ConstantNode(Constant.Value.fromBoolean(false));
    var constantNodes =
        fieldAccess.accessFunction().behavior().getNodes(ConstantNode.class).toList();
    ViamError.ensure(constantNodes.size() == 1,
        () -> Diagnostic.error("Expected one constant node We found zero or multiple",
            fieldAccess.location()));
    var constantNode = constantNodes.getFirst();
    var shiftValue = constantNode.constant().asVal().intValue();
    var shiftValueNode = new ConstantNode(
        Constant.Value.fromInteger(BigInteger.valueOf((long) Math.pow(2, shiftValue - 1)),
            Type.signedInt(64)));

    // Check if the lowest "shiftValue" bits are zero.
    var paramNode = new FuncParamNode(
        new Parameter(new Identifier(PARAM, SourceLocation.INVALID_SOURCE_LOCATION),
            fieldAccess.type()));
    var and = GraphUtils.binaryOp(BuiltInTable.AND, paramNode, shiftValueNode);
    var conditional = GraphUtils.select(GraphUtils.binaryOp(BuiltInTable.EQU, and,
            new ConstantNode(Constant.Value.fromInteger(BigInteger.ZERO, Type.unsignedInt(64)))),
        trueCase, falseCase);
    var returnNode = new ReturnNode(conditional);
    var startNode = new StartNode(returnNode);
    var behavior = new Graph("Generated predicate of " + fieldAccess.simpleName());
    behavior.addWithInputs(returnNode);
    behavior.add(startNode);
    setPredicate(instruction, fieldAccess, behavior);
  }

  private void generateEncoding(Instruction printableInstruction, Format.FieldAccess fieldAccess) {
    var accessFunction = fieldAccess.accessFunction();
    var fieldRef = fieldAccess.fieldRefs().getFirst();

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
    setFieldEncoding(printableInstruction, fieldAccess, fieldRef, behavior);
  }
}
