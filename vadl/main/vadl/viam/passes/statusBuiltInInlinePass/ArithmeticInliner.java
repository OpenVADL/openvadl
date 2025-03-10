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

import static vadl.utils.GraphUtils.and;
import static vadl.utils.GraphUtils.binaryOp;
import static vadl.utils.GraphUtils.bits;
import static vadl.utils.GraphUtils.not;
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

    /**
     * The signed overflow (or just overflow) happens if the sign of the result is unexpected.
     * So if we add two positive numbers and get a negative, or if we add two negative numbers and
     * get a positive, an overflow occurs.
     *
     * <p>When using the clang built-in __builtin_add_overflow(a, b, result); which returns
     * if there was an overflow, it compiles to
     * <pre>{@code
     * add     a3, a0, a1  -- add operands
     * slt     a0, a3, a0  -- check if result < operand1 (signed)
     * slti    a1, a1, 0   -- check if operand2 < 0 (testSignBit)
     * xor     a0, a0, a1  -- check if either result < operand1 or operand2 < 0
     * }</pre>
     *
     * @see <a href="https://godbolt.org/z/nh5TKzGWb">Godbolt Clang __builtin_add_overflow</a>
     */
    @Override
    ExpressionNode checkOverflow() {
      var result = getResult();
      var a = arg0();
      var b = arg1();

      // check: result < a ^ b < 0
      return binaryOp(BuiltInTable.XOR,
          binaryOp(BuiltInTable.SLTH,
              result,
              a
          ),
          testSignBit(b)
      );
    }

    /**
     * There was an unsigned overflow (carry) iff an operand is less than the result (unsigned)
     * this is because in the case of a carry,
     * the result will always be smaller than the smaller
     * operand.
     * E.g.: {@code 11 + 01 = 00 -> 00} is smaller than 01
     *
     * @see <a href="https://godbolt.org/z/Kec7xs9h5">Godbolt Clang __builtin_add_overflow</a>
     */
    @Override
    ExpressionNode checkCarry() {
      return binaryOp(BuiltInTable.ULTH,
          getResult(),
          arg0()
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
      var a = arg0();
      var b = arg1();
      var carryIn = arg2();

      partialResult = binaryOp(BuiltInTable.ADD, a, b);
      // a + b + carry
      return binaryOp(
          BuiltInTable.ADD,
          partialResult,
          zeroExtend(carryIn, a.type().asDataType())
      );
    }

    /**
     * As for ADDS, if the operands have the same sign which differs from the
     * result sign, a signed overflow occurs.
     * However, we now have a variable carry bit.
     *
     * <p>The naive approach would be
     * <pre>{@code
     * (a >= 0 && b >= 0 && result < 0)|| (a < 0 && b < 0 && result >= 0).
     * }</pre>
     * However, we can simplify this to
     * <pre>{@code
     * ((a ^ result) & (b ^ result)) < 0
     * }</pre>
     */
    @Override
    ExpressionNode checkOverflow() {
      var result = getResult();
      var a = arg0();
      var b = arg1();

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

    /**
     * For ADDC the carry is a bit different
     * because the result might "catch up" with the smallest operand.
     * E.g., {@code 11 + 01 = 00} -> carry out.
     *
     * <p>However, if the carry in is 1 we get {@code 11 + 01 + 1 = 01},
     * so the condition of ADD doesn't hold any longer.
     * The new condition to check the carry-out is
     * <pre>{@code
     * p = a + b
     * result = a + b + carry_in
     * partialCarry = (p < a)
     * finalCarry = (result < p)
     * carry_out = partialCarry || finalCarry
     * }</pre>
     *
     * @see <a href="https://godbolt.org/z/457KP9s3z">Godbolt Clang __builtin_addcl</a>
     */
    @Override
    @SuppressWarnings("DuplicatedCode")
    ExpressionNode checkCarry() {
      var a = arg0();

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
   * Implements subtract without carry (no-borrow).
   * Because of the way how this kind of subtract works ({@code a - b = a + ~b + 1}, where
   * the last {@code 1} is the carry flag for {@link BuiltInTable#SUBC}, the carry
   * is checked by {@code a >= b}.
   *
   * @see <a href="https://en.wikipedia.org/wiki/Carry_flag#:~:text=signed%20integer%20values.-,Vs.%20borrow%20flag,-%5Bedit%5D">Wikipedia: Carry vs. Borrow Flag</a>
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
      return binaryOp(BuiltInTable.SUB, arg0(), arg1());
    }

    /**
     * From the clang compiler we get the most optimized check.
     * <pre>{@code
     * result < a ^ b > 0
     * } </pre>
     *
     * @see <a href="https://godbolt.org/z/vjrvjKKqz">Godbolt Sub Overflow</a>
     */
    @Override
    ExpressionNode checkOverflow() {
      var result = getResult();
      var a = arg0();
      var b = arg1();

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
      return binaryOp(BuiltInTable.UGEQ, arg0(), arg1());
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
      return binaryOp(BuiltInTable.SUB, arg0(), arg1());
    }

    /**
     * From the clang compiler we get the most optimized check.
     * <pre>{@code
     * result < a ^ b > 0
     * }
     * </pre>
     *
     * @see <a href="https://godbolt.org/z/vjrvjKKqz">Godbolt Sub Overflow</a>
     */
    @Override
    ExpressionNode checkOverflow() {
      var result = getResult();
      var a = arg0();
      var b = arg1();

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
      return binaryOp(BuiltInTable.ULTH, arg0(), arg1());
    }
  }

  /**
   * Implements subtract with carry.
   * This is implemented using {@code a - b - borrow = a + ~b + c }, which means that
   * {@code borrow = !c}.
   *
   * @see <a href="https://en.wikipedia.org/wiki/Carry_flag#:~:text=signed%20integer%20values.-,Vs.%20borrow%20flag,-%5Bedit%5D">Wikipedia: Carry vs. Borrow Flag</a>
   * @see <a href="https://developer.arm.com/documentation/ddi0602/2024-12/Base-Instructions/SUBS--extended-register---Subtract-extended-and-scaled-register--setting-flags-?lang=en">
   *     ARM64 SUBS Instruction</a>
   */
  static class SubC extends Inliner {

    @LazyInit
    private ExpressionNode partialResult;

    public SubC(BuiltInCall builtInCall) {
      super(builtInCall);
    }

    @Override
    ExpressionNode createResult() {
      var a = arg0();
      var b = arg1();
      var carryIn = arg2();
      // borrow = !carryIn
      var borrow = zeroExtend(not(carryIn), a.type().asDataType());
      // a - b
      partialResult = binaryOp(BuiltInTable.SUB, a, b);
      // a - b - borrow
      return binaryOp(BuiltInTable.SUB, partialResult, borrow);
    }

    /**
     * This check is done by checking if “the
     * carry into the sign position is not equal to the carry out of the sign position.”
     * (from Hacker's Delight book).
     * This is done with the following expression:
     * <pre>{@code
     * ((a ^ b) & (a ^ result)) < 0
     * }
     * </pre>
     */
    @Override
    ExpressionNode checkOverflow() {
      var result = getResult();
      var a = arg0();
      var b = arg1();

      return testSignBit(
          and(
              binaryOp(BuiltInTable.XOR,
                  a,
                  b
              ),
              binaryOp(
                  BuiltInTable.XOR,
                  a,
                  result
              )
          )
      );
    }

    /**
     * Can be computed with:
     * <pre>{@code
     * a >= partial && partial >= result
     * }</pre>
     * It is a negation of the {@link SubB#checkCarry()} expression, which comes
     * from the clang built-in implementation.
     */
    @Override
    ExpressionNode checkCarry() {
      var a = arg0();

      var result = getResult();
      var p = partialResult;

      var partialCarry = BuiltInCall.of(
          BuiltInTable.UGEQ,
          a,
          p
      );
      var finalCarry = BuiltInCall.of(
          BuiltInTable.UGEQ,
          p,
          result
      );
      return and(partialCarry, finalCarry);
    }
  }

  /**
   * Implements subtract with borrow, which uses the bit as a borrow flag,
   * setting it if {@code a < b} when computing {@code a - b}, and a
   * borrow must be performed.
   *
   * @see <a href="https://arc.net/l/quote/fmdsnowl">Wikipedia: Carry vs. Borrow Flag</a>
   */
  static class SubB extends Inliner {

    @LazyInit
    private ExpressionNode partialResult;

    public SubB(BuiltInCall builtInCall) {
      super(builtInCall);
    }

    @Override
    ExpressionNode createResult() {
      var a = arg0();
      var b = arg1();
      var carryIn = arg2();
      partialResult = binaryOp(BuiltInTable.SUB, a, b);
      // for us the most optimized way is to just use SUB for the result
      return binaryOp(BuiltInTable.SUB, partialResult,
          zeroExtend(carryIn, a.type().asDataType()));
    }

    /**
     * This check is done by checking if “the
     * carry into the sign position is not equal to the carry out of the sign position.”
     * (from Hacker's Delight book).
     * This is done with the following expression:
     * <pre>{@code
     * ((a ^ b) & (a ^ result)) < 0
     * }
     * </pre>
     */
    @Override
    ExpressionNode checkOverflow() {
      var result = getResult();
      var a = arg0();
      var b = arg1();

      return testSignBit(
          and(
              binaryOp(BuiltInTable.XOR,
                  a,
                  b
              ),
              binaryOp(
                  BuiltInTable.XOR,
                  a,
                  result
              )
          )
      );
    }

    /**
     * Can be computed with:
     * <pre>{@code
     * a < partial || partial < result
     * }</pre>
     * This is from the clang {@code __builtin_subcl} translation.
     *
     * @see <a href="https://godbolt.org/z/q65hqMhr6">Godbolt Sub Carry</a>
     */
    @Override
    @SuppressWarnings("DuplicatedCode")
    ExpressionNode checkCarry() {
      var a = arg0();

      var result = getResult();
      var p = partialResult;

      var partialCarry = BuiltInCall.of(
          BuiltInTable.ULTH,
          a,
          p
      );
      var finalCarry = BuiltInCall.of(
          BuiltInTable.ULTH,
          p,
          result
      );
      return or(partialCarry, finalCarry);
    }
  }

}
