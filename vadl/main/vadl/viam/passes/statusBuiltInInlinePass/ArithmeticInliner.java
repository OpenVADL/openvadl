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
import static vadl.utils.GraphUtils.equ;
import static vadl.utils.GraphUtils.not;
import static vadl.utils.GraphUtils.or;
import static vadl.utils.GraphUtils.testSignBit;
import static vadl.utils.GraphUtils.zeroExtend;

import vadl.types.BuiltInTable;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ExpressionNode;

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
      // we can check this by taking the condition described above.
      var result = getResult();
      var a = firstArg();
      var b = secondArg();

      // check: x < 0 && a > 0 && b > 0 || x > 0 && a < 0 && b < 0
      return or(
          // x < 0 && a > 0 && b > 0
          and(
              testSignBit(result),
              not(testSignBit(a)),
              not(testSignBit(b))
          ),
          // x > 0 && a < 0 && b < 0
          and(
              not(testSignBit(result)),
              testSignBit(a),
              testSignBit(b)
          )
      );
    }

    @Override
    ExpressionNode checkCarry() {
      // there was an unsigned overflow (carry) iff a operand is less than the result (unsigned).
      // this is because in case of a carry, the result will always be samller than the smaller
      // operand.
      // e.g.: 11 + 01 = 00 -> 00 is smaller than 01
      return BuiltInCall.of(
          BuiltInTable.ULTH,
          getResult(),
          firstArg()
      );
    }
  }

  // 10 + 11 = 01
  // 11 + 11 = 10

  // the overflow implementation stays the same for ADDC,
  // as the carry will never cause a change of the sign bit.
  static class AddC extends AddS {

    AddC(BuiltInCall builtInCall) {
      super(builtInCall);
    }

    @Override
    ExpressionNode createResult() {
      var a = firstArg();
      var b = secondArg();
      var carryIn = thirdArg();
      // a + b + carry
      return binaryOp(
          BuiltInTable.ADD,
          binaryOp(BuiltInTable.ADD, a, b),
          zeroExtend(carryIn, a.type().asDataType())
      );
    }

    @Override
    ExpressionNode checkCarry() {
      // for ADDC the carry is a bit different,
      // because the result might "catch" the smallest operand.
      // e.g. 11 + 01 = 00 -> carry out.
      // however, if the carry in is 1 we get 11 + 01 + 1 = 01
      // so the condition of ADD doesn't hold any longer.
      // the new condition to check the carry out is
      // x < a || (carry && (x == a || x == b))
      var carryIn = thirdArg();
      return or(
          BuiltInCall.of(BuiltInTable.ULTH, getResult(), firstArg()),
          and(
              carryIn,
              or(
                  equ(getResult(), firstArg()),
                  equ(getResult(), firstArg())
              )
          )
      );
    }
  }


}
