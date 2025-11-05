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
import vadl.gcb.passes.GenerateValueRangeImmediatePass;
import vadl.gcb.passes.encodingGeneration.strategies.EncodingPredicateGenerationStrategy;
import vadl.types.BitsType;
import vadl.types.BuiltInTable;
import vadl.types.Type;
import vadl.utils.GraphUtils;
import vadl.viam.Constant;
import vadl.viam.Format;
import vadl.viam.Instruction;
import vadl.viam.ViamError;
import vadl.viam.graph.Graph;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.control.StartNode;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.FieldRefNode;
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
public class ShiftedImmediateEncodingPredicateStrategy
    implements EncodingPredicateGenerationStrategy {
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
    var paramNode = new FieldAccessRefNode(fieldAccess, fieldAccess.type());
    var bitwise = GraphUtils.binaryOp(BuiltInTable.AND, paramNode, shiftValueNode);
    var cond = GraphUtils.and(GraphUtils.binaryOp(BuiltInTable.EQU, bitwise,
            new ConstantNode(Constant.Value.fromInteger(BigInteger.ZERO, Type.unsignedInt(64)))),
        checkIfValueInRange(fieldAccess, shiftValue, paramNode)
    );
    var select = GraphUtils.select(cond,
        trueCase, falseCase);
    var returnNode = new ReturnNode(select);
    var startNode = new StartNode(returnNode);
    var behavior = new Graph("Generated predicate of " + fieldAccess.simpleName());
    behavior.addWithInputs(returnNode);
    behavior.add(startNode);
    setPredicate(instruction, fieldAccess, behavior);
  }

  private ExpressionNode checkIfValueInRange(Format.FieldAccess fieldAccess,
                                             int shiftBits,
                                             FieldAccessRefNode param) {
    var ty =
        fieldAccess.accessFunction().behavior().getNodes(FieldRefNode.class).toList().getFirst()
            .formatField().type();
    var isSigned = fieldAccess.accessFunction().returnType().asDataType().isSigned();
    var bitWidth = ty.bitWidth();
    var newTy = BitsType.bits(bitWidth + shiftBits);
    var maxValue = new ConstantNode(
        Constant.Value.of(
            GenerateValueRangeImmediatePass.highestPossibleValue(
                newTy, isSigned),
            fieldAccess.accessFunction().returnType().asDataType()));
    var minValue = new ConstantNode(
        Constant.Value.of(
            GenerateValueRangeImmediatePass.lowestPossibleValue(
                newTy, isSigned),
            fieldAccess.accessFunction().returnType().asDataType()));

    if (!isSigned) {
      return GraphUtils.and(GraphUtils.binaryOp(BuiltInTable.ULEQ, param, maxValue),
          GraphUtils.binaryOp(BuiltInTable.UGEQ, param, minValue));
    } else {
      return GraphUtils.and(GraphUtils.binaryOp(BuiltInTable.SLEQ, param, maxValue),
          GraphUtils.binaryOp(BuiltInTable.SGEQ, param, minValue));
    }
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
