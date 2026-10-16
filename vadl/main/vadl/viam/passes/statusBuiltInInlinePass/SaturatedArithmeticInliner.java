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

package vadl.viam.passes.statusBuiltInInlinePass;

import static vadl.utils.GraphUtils.not;
import static vadl.utils.GraphUtils.select;
import static vadl.utils.GraphUtils.testSignBit;

import javax.annotation.Nullable;
import vadl.types.BitsType;
import vadl.types.BuiltInTable;
import vadl.types.Type;
import vadl.viam.Constant;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.SelectNode;

/**
 * Contains the {@link Inliner}s for the saturated arithmetic built-ins
 * (e.g. {@link BuiltInTable#SSATADDS} or {@link BuiltInTable#USATSUBB}).
 *
 * <p>The status flags are computed using the {@link Inliner} of the corresponding
 * wrapping operation (e.g. {@link ArithmeticInliner.AddC} for {@link BuiltInTable#SSATADDC}).
 * If there is a non-status saturated built-in for the operation
 * (e.g. {@link BuiltInTable#SSATADD} for {@link BuiltInTable#SSATADDS}), it is used for the
 * result, so each backend can lower the saturation on its own.
 * Otherwise (operations with carry/borrow input), the result is the wrapping result,
 * clamped to the maximum/minimum value of the operand type if the wrapping operation
 * over- or underflows.
 * Only operations of the operand bit width are used, so the lowering also works for
 * operands that already have the size of the target.</p>
 *
 * <p>The status flags are defined as follows:
 * <ul>
 *   <li>{@code overflow} is set iff the result was saturated
 *   (like the {@code Q} flag in AArch32).</li>
 *   <li>{@code carry} is the carry/borrow of the wrapping operation, using the same
 *   convention as the non-saturated counterpart
 *   ({@code s/usatsubs} uses the carry convention of {@link BuiltInTable#SUBSC}).</li>
 *   <li>{@code zero} and {@code negative} are computed on the saturated result.</li>
 * </ul>
 */
abstract class SaturatedArithmeticInliner extends Inliner {

  /**
   * The inliner of the wrapping (non-saturated) operation.
   */
  final Inliner wrapping;

  /**
   * The non-status saturated built-in that computes the result,
   * or null if there is no such built-in.
   */
  @Nullable
  final BuiltInTable.BuiltIn equivalent;

  SaturatedArithmeticInliner(BuiltInCall builtInCall, Inliner wrapping,
                             @Nullable BuiltInTable.BuiltIn equivalent) {
    super(builtInCall);
    this.wrapping = wrapping;
    this.equivalent = equivalent;
  }

  /**
   * Replaces the built-in call by its saturated result, ignoring all status flags.
   * This is used if the status of the call is unused.
   */
  ExpressionNode replaceByResult() {
    return builtInCall.replaceAndDelete(getResult());
  }

  static SaturatedArithmeticInliner ssatAdds(BuiltInCall call) {
    return new Signed(call, new ArithmeticInliner.AddS(call), BuiltInTable.SSATADD);
  }

  static SaturatedArithmeticInliner ssatAddc(BuiltInCall call) {
    return new Signed(call, new ArithmeticInliner.AddC(call), null);
  }

  static SaturatedArithmeticInliner ssatSubs(BuiltInCall call) {
    return new Signed(call, new ArithmeticInliner.SubSC(call), BuiltInTable.SSATSUB);
  }

  static SaturatedArithmeticInliner ssatSubc(BuiltInCall call) {
    return new Signed(call, new ArithmeticInliner.SubC(call), null);
  }

  static SaturatedArithmeticInliner ssatSubb(BuiltInCall call) {
    return new Signed(call, new ArithmeticInliner.SubB(call), null);
  }

  static SaturatedArithmeticInliner usatAdds(BuiltInCall call) {
    return new UnsignedAdd(call, new ArithmeticInliner.AddS(call), BuiltInTable.USATADD);
  }

  static SaturatedArithmeticInliner usatAddc(BuiltInCall call) {
    return new UnsignedAdd(call, new ArithmeticInliner.AddC(call), null);
  }

  static SaturatedArithmeticInliner usatSubs(BuiltInCall call) {
    return new UnsignedSub(call, new ArithmeticInliner.SubSC(call), BuiltInTable.USATSUB, true);
  }

  static SaturatedArithmeticInliner usatSubc(BuiltInCall call) {
    return new UnsignedSub(call, new ArithmeticInliner.SubC(call), null, true);
  }

  static SaturatedArithmeticInliner usatSubb(BuiltInCall call) {
    return new UnsignedSub(call, new ArithmeticInliner.SubB(call), null, false);
  }

  /**
   * Returns the value that is used as result if the wrapping operation over-/underflows.
   */
  abstract ExpressionNode saturationValue();

  /**
   * Returns the condition that is true iff the wrapping operation over-/underflows.
   */
  abstract ExpressionNode checkSaturation();

  @Override
  ExpressionNode createResult() {
    if (equivalent != null) {
      return binaryOf(equivalent);
    }
    // the wrapping result is of type Bits<N>, but the saturated one is of type SInt/UInt
    return new SelectNode(resultType(), getOverflow(), saturationValue(), wrapping.getResult());
  }

  @Override
  ExpressionNode checkOverflow() {
    return checkSaturation();
  }

  @Override
  ExpressionNode checkCarry() {
    return wrapping.getCarry();
  }

  /**
   * Returns the SInt/UInt type of the saturated result.
   * The signedness is determined by the built-in, as the operand types might
   * have been weakened to Bits by earlier passes (e.g., by removing type casts).
   */
  abstract BitsType resultType();

  int bitWidth() {
    return arg0().type().asDataType().bitWidth();
  }

  /**
   * Signed saturation. The wrapping operation saturates iff it has a signed overflow.
   * If it saturates, the sign of the first operand determines the direction:
   * For addition, both operands must have the same sign to overflow.
   * For subtraction, the operands must have different signs to overflow.
   * So in both cases, a negative first operand results in the minimum value and a positive
   * first operand in the maximum value.
   */
  static class Signed extends SaturatedArithmeticInliner {

    Signed(BuiltInCall builtInCall, Inliner wrapping,
           @Nullable BuiltInTable.BuiltIn equivalent) {
      super(builtInCall, wrapping, equivalent);
    }

    @Override
    BitsType resultType() {
      return Type.signedInt(bitWidth());
    }

    @Override
    ExpressionNode saturationValue() {
      var type = resultType();
      return select(
          testSignBit(arg0()),
          Constant.Value.minValueOf(type).toNode(),
          Constant.Value.maxValueOf(type).toNode()
      );
    }

    @Override
    ExpressionNode checkSaturation() {
      return wrapping.getOverflow();
    }
  }

  /**
   * Unsigned saturated addition. The wrapping operation saturates iff it produces a carry.
   * The result is then clamped to the maximum value.
   */
  static class UnsignedAdd extends SaturatedArithmeticInliner {

    UnsignedAdd(BuiltInCall builtInCall, Inliner wrapping,
                @Nullable BuiltInTable.BuiltIn equivalent) {
      super(builtInCall, wrapping, equivalent);
    }

    @Override
    BitsType resultType() {
      return Type.unsignedInt(bitWidth());
    }

    @Override
    ExpressionNode saturationValue() {
      return Constant.Value.maxValueOf(resultType()).toNode();
    }

    @Override
    ExpressionNode checkSaturation() {
      return wrapping.getCarry();
    }
  }

  /**
   * Unsigned saturated subtraction. The wrapping operation saturates iff it requires a borrow.
   * The result is then clamped to zero.
   */
  static class UnsignedSub extends SaturatedArithmeticInliner {

    /**
     * True if the wrapping operation uses the carry convention, where the carry flag
     * is the inverse of the borrow.
     */
    private final boolean carryConvention;

    UnsignedSub(BuiltInCall builtInCall, Inliner wrapping,
                @Nullable BuiltInTable.BuiltIn equivalent, boolean carryConvention) {
      super(builtInCall, wrapping, equivalent);
      this.carryConvention = carryConvention;
    }

    @Override
    BitsType resultType() {
      return Type.unsignedInt(bitWidth());
    }

    @Override
    ExpressionNode saturationValue() {
      return Constant.Value.minValueOf(resultType()).toNode();
    }

    @Override
    ExpressionNode checkSaturation() {
      var carry = wrapping.getCarry();
      return carryConvention ? not(carry) : carry;
    }
  }
}
