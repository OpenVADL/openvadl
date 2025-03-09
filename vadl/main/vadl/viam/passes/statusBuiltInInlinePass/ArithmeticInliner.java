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
class ArithmeticInliner {

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


}
