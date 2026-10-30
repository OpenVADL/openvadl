// SPDX-FileCopyrightText : © 2026 TU Wien <vadl@tuwien.ac.at>
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

package vadl.iss.passes.common.builtInLowering;

import static vadl.utils.GraphUtils.and;
import static vadl.utils.GraphUtils.binaryOp;
import static vadl.utils.GraphUtils.select;
import static vadl.utils.GraphUtils.testSignBit;

import vadl.types.BitsType;
import vadl.types.BuiltInTable;
import vadl.types.Type;
import vadl.viam.Constant;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.SelectNode;

/**
 * Lowers the non-status saturated arithmetic built-ins ({@link BuiltInTable#SSATADD},
 * {@link BuiltInTable#USATADD}, {@link BuiltInTable#SSATSUB}, {@link BuiltInTable#USATSUB})
 * to the wrapping operation and a select of the saturation value.
 * Only operations of the operand bit width are used, so the lowering also works for
 * operands that already have the size of the target.
 *
 * <p>The saturated result type (SInt/UInt) is determined by the built-in, as the operand types
 * might have been weakened to Bits by earlier passes (e.g., by removing type casts).</p>
 */
final class SaturatedArithmeticLowering {

  private SaturatedArithmeticLowering() {
  }

  /**
   * Signed saturated addition. Saturates iff the operands have the same sign which differs
   * from the sign of the wrapping result ({@code ((a ^ r) & (b ^ r)) < 0}).
   */
  static void lowerSsatAdd(BuiltInCall call) {
    var a = call.arg(0);
    var b = call.arg(1);
    var result = binaryOp(BuiltInTable.ADD, a, b);
    var overflow = testSignBit(and(
        binaryOp(BuiltInTable.XOR, a, result),
        binaryOp(BuiltInTable.XOR, b, result)
    ));
    replace(call, Type.signedInt(bitWidth(call)), overflow, signedBound(a), result);
  }

  /**
   * Unsigned saturated addition. Saturates to the maximum value iff the wrapping
   * addition produces a carry ({@code r < a}).
   */
  static void lowerUsatAdd(BuiltInCall call) {
    var a = call.arg(0);
    var b = call.arg(1);
    var type = Type.unsignedInt(bitWidth(call));
    var result = binaryOp(BuiltInTable.ADD, a, b);
    var carry = binaryOp(BuiltInTable.ULTH, result, a);
    replace(call, type, carry, Constant.Value.maxValueOf(type).toNode(), result);
  }

  /**
   * Signed saturated subtraction. Saturates iff the operands have different signs and
   * the sign of the wrapping result differs from the first operand
   * ({@code ((a ^ b) & (a ^ r)) < 0}).
   */
  static void lowerSsatSub(BuiltInCall call) {
    var a = call.arg(0);
    var b = call.arg(1);
    var result = binaryOp(BuiltInTable.SUB, a, b);
    var overflow = testSignBit(and(
        binaryOp(BuiltInTable.XOR, a, b),
        binaryOp(BuiltInTable.XOR, a, result)
    ));
    replace(call, Type.signedInt(bitWidth(call)), overflow, signedBound(a), result);
  }

  /**
   * Unsigned saturated subtraction. Saturates to zero iff the wrapping
   * subtraction requires a borrow ({@code a < b}).
   */
  static void lowerUsatSub(BuiltInCall call) {
    var a = call.arg(0);
    var b = call.arg(1);
    var type = Type.unsignedInt(bitWidth(call));
    var result = binaryOp(BuiltInTable.SUB, a, b);
    var borrow = binaryOp(BuiltInTable.ULTH, a, b);
    replace(call, type, borrow, Constant.Value.minValueOf(type).toNode(), result);
  }

  /**
   * If a signed operation overflows, the sign of the first operand determines the direction:
   * For addition, both operands must have the same sign to overflow.
   * For subtraction, the operands must have different signs to overflow.
   * So in both cases, a negative first operand results in the minimum value and a positive
   * first operand in the maximum value.
   */
  private static ExpressionNode signedBound(ExpressionNode a) {
    var type = Type.signedInt(a.type().asDataType().bitWidth());
    return select(
        testSignBit(a),
        Constant.Value.minValueOf(type).toNode(),
        Constant.Value.maxValueOf(type).toNode()
    );
  }

  private static void replace(BuiltInCall call, BitsType type, ExpressionNode saturates,
                              ExpressionNode saturationValue, ExpressionNode result) {
    call.replaceAndDelete(new SelectNode(type, saturates, saturationValue, result));
  }

  private static int bitWidth(BuiltInCall call) {
    return call.arg(0).type().asDataType().bitWidth();
  }
}
