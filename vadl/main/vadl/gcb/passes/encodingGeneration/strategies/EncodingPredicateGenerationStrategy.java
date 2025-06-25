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

package vadl.gcb.passes.encodingGeneration.strategies;

import java.math.BigInteger;
import vadl.gcb.passes.GenerateValueRangeImmediatePass;
import vadl.types.BuiltInTable;
import vadl.types.Type;
import vadl.utils.GraphUtils;
import vadl.viam.Constant;
import vadl.viam.Format;
import vadl.viam.Function;
import vadl.viam.Instruction;
import vadl.viam.Parameter;
import vadl.viam.graph.Graph;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.control.StartNode;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.FieldAccessRefNode;

/**
 * The implementor of this interface can generate a field access encoding / predicate functions.
 */
public interface EncodingPredicateGenerationStrategy {

  String PARAM = "param";

  /**
   * Check if the strategy can be applied. Returns {@code true} when it is applicable.
   */
  boolean checkIfApplicable(Format.FieldAccess fieldAccess);

  /**
   * Create the inverse behavior graph of a field access function.
   * It also adds the created nodes to {@code vadl.viam.Format.FieldAccess#encoding}.
   * Based on the encoding functions, it automatically detects the value range which is encodable
   * into the {@link Format.Field}.
   */
  void generateEncodingAndPredicateFunction(Instruction instruction,
                                            Format.FieldAccess fieldAccess);

  /**
   * Creates a new {@link vadl.viam.Format.FieldEncoding} for the given field
   * and the behavior graph.
   * It assumes that there is only a single field references by the field access.
   */
  default void setFieldEncoding(Instruction instruction,
                                Format.FieldAccess fieldAccess,
                                Format.Field fieldToBeEncoded,
                                Graph behavior) {
    var ident = fieldAccess.identifier.last().prepend(instruction.identifier());
    var format = fieldAccess.format();
    var encoding = new Format.FieldEncoding(ident, fieldToBeEncoded, behavior);
    format.setFieldEncoding(encoding);
  }

  /**
   * Creates a new predicate function for the given field
   * and the behavior graph.
   * It assumes that there is only a single field references by the field access.
   */
  default void setPredicate(Instruction instruction,
                            Format.FieldAccess fieldAccess,
                            Graph behavior) {
    var ident = instruction.identifier().append(fieldAccess.identifier.last()
        .parts());
    var predicate = new Function(ident, new Parameter[] {}, Type.bool(), behavior);
    fieldAccess.setPredicate(predicate);
  }

  /**
   * Template method for generation a predicate function.
   */
  default void generatePredicate(Instruction instruction, Format.FieldAccess fieldAccess) {
    var trueCase = new ConstantNode(Constant.Value.fromBoolean(true));
    var falseCase = new ConstantNode(Constant.Value.fromBoolean(false));

    var fieldRef = fieldAccess.fieldRefs().getFirst();

    var paramNode = new FieldAccessRefNode(fieldAccess, fieldAccess.type());

    var lowestValue = new ConstantNode(Constant.Value.fromInteger(
        BigInteger.valueOf(
            GenerateValueRangeImmediatePass.lowestPossibleValue(fieldRef.type().toBitsType(),
                true)),
        Type.signedInt(64)));
    var highestValue = new ConstantNode(Constant.Value.fromInteger(
        BigInteger.valueOf(
            GenerateValueRangeImmediatePass.highestPossibleValue(fieldRef.type().toBitsType(),
                true)),
        Type.signedInt(64)));

    var lowestExpr =
        GraphUtils.binaryOp(BuiltInTable.SGEQ, paramNode, lowestValue);
    var highestExpr =
        GraphUtils.binaryOp(BuiltInTable.SLEQ, paramNode, highestValue);

    var conditional =
        GraphUtils.select(GraphUtils.and(lowestExpr, highestExpr), trueCase, falseCase);
    var returnNode = new ReturnNode(conditional);
    var startNode = new StartNode(returnNode);
    var behavior = new Graph("Generated predicate of " + fieldAccess.simpleName());
    behavior.addWithInputs(returnNode);
    behavior.add(startNode);
    setPredicate(instruction, fieldAccess, behavior);
  }
}
