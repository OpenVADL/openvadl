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

package vadl.viam.passes.statusBuiltInInlinePass;

import static vadl.utils.GraphUtils.binaryOp;
import static vadl.utils.GraphUtils.bits;
import static vadl.utils.GraphUtils.or;
import static vadl.utils.GraphUtils.testSignBit;
import static vadl.utils.GraphUtils.zeroExtend;

import com.google.errorprone.annotations.concurrent.LazyInit;
import vadl.types.BuiltInTable;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * Contains the status built-in {@link Inliner}s for all arithmetic operations.
 */
abstract class ArithmeticInliner {

  static class AddS extends Inliner {

    AddS(BuiltInCall builtInCall) {
      super(builtInCall);
    }

    @Override
    ExpressionNode createResult() {
      return binaryOf(BuiltInTable.ADD);
    }

    @Override
    ExpressionNode checkOverflow() {
      // the signed overflow (or just overflow) happens, if the sign of the result is unexpected.
      // so if we add two positive numbers and get a negative or if we add two negative numbers and
      // get a positive, an overflow occurs.

      // https://godbolt.org/z/nh5TKzGWb
      // when using the clang built-in __builtin_add_overflow(a, b, result); which returns
      // if there was an overflow, it compiles to
      // add     a3, a0, a1  -- add operands
      // slt     a0, a3, a0  -- check if result < operand1 (signed)
      // slti    a1, a1, 0   -- check if operand2 < 0 (testSignBit)
      // xor     a0, a0, a1  -- check if either result < operand1 or operand2 < 0

      var result = getResult();
      var a = firstArg();
      var b = secondArg();

      // check: result < a ^ b < 0
      return binaryOp(BuiltInTable.XOR,
          binaryOp(BuiltInTable.SLTH,
              result,
              a
          ),
          testSignBit(b)
      );
    }

    @Override
    ExpressionNode checkCarry() {
      // there was an unsigned overflow (carry) iff a operand is less than the result (unsigned).
      // this is because in case of a carry, the result will always be samller than the smaller
      // operand.
      // e.g.: 11 + 01 = 00 -> 00 is smaller than 01

      // this is also how clang compiles __builtin_add_overflow to machine code:
      // https://godbolt.org/z/Kec7xs9h5

      return binaryOp(BuiltInTable.ULTH,
          getResult(),
          firstArg()
      );
    }
  }

  static class AddC extends Inliner {

    AddC(BuiltInCall builtInCall) {
      super(builtInCall);
    }

    @LazyInit
    private ExpressionNode partialResult;

    @Override
    ExpressionNode createResult() {
      var a = firstArg();
      var b = secondArg();
      var carryIn = thirdArg();

      partialResult = binaryOp(BuiltInTable.ADD, a, b);
      // a + b + carry
      return binaryOp(
          BuiltInTable.ADD,
          partialResult,
          zeroExtend(carryIn, a.type().asDataType())
      );
    }

    @Override
    ExpressionNode checkOverflow() {
      // as for ADDS, if the operands have the same sign which differs from the
      // result sign, a signed overflow occurs.

      // the naive approach would be
      // (a >= 0 && b >= 0 && result < 0)|| (a < 0 && b < 0 && result >= 0).
      // however, we can simplify this to
      // (a ^ result) & (b ^ result) < 0

      var result = getResult();
      var a = firstArg();
      var b = secondArg();

      // check: (a ^ result) & (b ^ result) < 0
      return testSignBit(
          binaryOp(BuiltInTable.AND,
              binaryOp(BuiltInTable.XOR,
                  a,
                  result
              ),
              binaryOp(BuiltInTable.XOR,
                  b,
                  result
              )
          )
      );
    }

    @Override
    ExpressionNode checkCarry() {
      // for ADDC the carry is a bit different,
      // because the result might "catch up" with the smallest operand.
      // e.g., 11 + 01 = 00 -> carry out.
      // however, if the carry in is 1 we get 11 + 01 + 1 = 01,
      // so the condition of ADD doesn't hold any longer.

      // the new condition to check the carry-out is
      // p = a + b
      // result = a + b + carry_in
      // partialCarry = (p < a)
      // finalCarry = (result < p)
      // carry_out = partialCarry || finalCarry

      // you can see this on godbolt: https://godbolt.org/z/457KP9s3z

      var a = firstArg();

      var result = getResult();
      var p = partialResult;

      var partialCarry = BuiltInCall.of(
          BuiltInTable.ULTH,
          p,
          a
      );
      var finalCarry = BuiltInCall.of(
          BuiltInTable.ULTH,
          result,
          p
      );
      return or(partialCarry, finalCarry);
    }
  }

  /**
   * Implements subtract with carry, using the two's complement identity:
   * <code>-x = not(x) + 1</code>. It computes <code>a - b</code> as
   * <code>a + not(b) + 1</code>.
   *
   * <p>In subtract-without-carry operations (subsc), the instruction acts as if the carry
   * flag was set (i.e. <code>C = 1</code>).
   *
   * <p>This convention sets the carry flag if <code>a ≥ b</code> (indicating that no borrow occurred)
   * and clears it if <code>a &lt; b</code>.
   * This behavior is used in several instruction set architectures,
   * including System/360, 6502, MSP430, COP8, ARM, and PowerPC.
   *
   * <p>Notably, the 6502 does not have an explicit subtract-without-carry instruction,
   * so programmers must ensure that the carry flag is set before subtract operations where a borrow is not desired.
   *
   * @see <a href="https://arc.net/l/quote/fmdsnowl">Wikipedia: Carry vs. Borrow Flag</a>
   * @see <a href="https://developer.arm.com/documentation/ddi0602/2024-12/Base-Instructions/SUBS--extended-register---Subtract-extended-and-scaled-register--setting-flags-?lang=en">
   *     ARM64 SUBS Instruction</a>
   */
  static class SubSC extends Inliner {

    public SubSC(BuiltInCall builtInCall) {
      super(builtInCall);
    }

    @Override
    ExpressionNode createResult() {
      // for us the most optimized way is to just use SUB for the result
      return binaryOp(BuiltInTable.SUB, firstArg(), secondArg());
    }

    /**
     * As described in the class documentation, the nvzc flags are typically taken
     * from the ADDC result using {@code a+(not(b)+1)}.
     * Below the overflow check is inlined from the {@link AddS#checkOverflow()} implementation.
     * <pre>{@code
     * result < a ^ b > 0
     * }
     * </pre>
     */
    @Override
    ExpressionNode checkOverflow() {
      var result = getResult();
      var a = firstArg();
      var b = secondArg();

      return binaryOp(BuiltInTable.XOR,
          binaryOp(BuiltInTable.SLTH,
              result,
              a
          ),
          binaryOp(
              BuiltInTable.SGTH,
              b, bits(0, b.type().asDataType().bitWidth()).toNode()
          )
      );
    }

    /**
     * For the subtract with carry operation, the carry flag (no borrow) is set iff
     * {@code a >= b}, so when no underflow happens.
     */
    @Override
    ExpressionNode checkCarry() {
      return binaryOp(BuiltInTable.UGEQ, firstArg(), secondArg());
    }
  }

  /**
   * Implements subtract with borrow, which uses the bit as a borrow flag,
   * setting it if {@code a < b} when computing {@code a - b}, and a
   * borrow must be performed.
   * The {@link BuiltInTable#SUBSB} built-in acts as if the carry flag is not set.
   *
   * @see <a href="https://arc.net/l/quote/fmdsnowl">Wikipedia: Carry vs. Borrow Flag</a>
   */
  static class SubSB extends Inliner {

    public SubSB(BuiltInCall builtInCall) {
      super(builtInCall);
    }

    @Override
    ExpressionNode createResult() {
      // for us the most optimized way is to just use SUB for the result
      return binaryOp(BuiltInTable.SUB, firstArg(), secondArg());
    }

    /**
     * As described in the class documentation, the nvzc flags are typically taken
     * from the ADDC result using {@code a+(not(b)+1)}.
     * Below the overflow check is inlined from the {@link AddS#checkOverflow()} implementation.
     * <pre>{@code
     * result < a ^ b > 0
     * }
     * </pre>
     */
    @Override
    ExpressionNode checkOverflow() {
      var result = getResult();
      var a = firstArg();
      var b = secondArg();

      return binaryOp(BuiltInTable.XOR,
          binaryOp(BuiltInTable.SLTH,
              result,
              a
          ),
          binaryOp(
              BuiltInTable.SGTH,
              b, bits(0, b.type().asDataType().bitWidth()).toNode()
          )
      );
    }

    /**
     * For the subtract with borrow operation, the carry flag (borrow) is set iff
     * {@code a < b}, so when an underflow happens.
     */
    @Override
    ExpressionNode checkCarry() {
      return binaryOp(BuiltInTable.ULTH, firstArg(), secondArg());
    }
  }


}
