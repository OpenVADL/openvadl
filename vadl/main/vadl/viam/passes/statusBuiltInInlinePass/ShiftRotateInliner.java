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

import static vadl.utils.GraphUtils.binaryOp;
import static vadl.utils.GraphUtils.bitsNode;
import static vadl.utils.GraphUtils.testSignBit;
import static vadl.utils.GraphUtils.truncate;

import vadl.types.BuiltInTable;
import vadl.types.Type;
import vadl.viam.Constant;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * Contains the status built-in {@link Inliner}s for all shift/rotate operations.
 */
public abstract class ShiftRotateInliner {

  /**
   * Implements {@link #checkOverflow()}. Shift/rotate operations do not set
   * the overflow flag, so {@code false} is returned by default.
   */
  abstract static class ShiftRotate extends Inliner {

    ShiftRotate(BuiltInCall builtInCall) {
      super(builtInCall);
    }

    @Override
    ExpressionNode checkOverflow() {
      return Constant.Value.fromBoolean(false).toNode();
    }

  }

  static class LSLS extends ShiftRotate {

    LSLS(BuiltInCall builtInCall) {
      super(builtInCall);
    }

    @Override
    ExpressionNode createResult() {
      return binaryOf(BuiltInTable.LSL);
    }

    @Override
    ExpressionNode checkCarry() {
      var a = arg0();
      var b = arg1();

      // if b is 0, carry is 0

      // check: (a <<> b) & (b != 0)
      return binaryOp(BuiltInTable.AND,
          truncate(binaryOp(BuiltInTable.ROL, a, b), Type.bits(1)),
          binaryOp(BuiltInTable.NEQ, b, bitsNode(0, b.type().asDataType().bitWidth()))
      );
    }
  }

  static class RORS extends ShiftRotate {

    RORS(BuiltInCall builtInCall) {
      super(builtInCall);
    }

    @Override
    ExpressionNode createResult() {
      return binaryOf(BuiltInTable.ROR);
    }

    @Override
    ExpressionNode checkCarry() {
      var result = getResult();
      var b = arg1();

      // the last bit rotated off the right side is the
      // msb of the result
      // if b is 0, carry is 0

      // check: msb(result) & (b != 0)
      return binaryOp(BuiltInTable.AND,
          testSignBit(result),
          binaryOp(BuiltInTable.NEQ, b, bitsNode(0, b.type().asDataType().bitWidth()))
      );
    }
  }

}
