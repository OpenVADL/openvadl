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

import vadl.types.BuiltInTable;
import vadl.types.Type;
import vadl.utils.GraphUtils;
import vadl.viam.Constant;
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
      // from
      // https://stackoverflow.com/questions/199333/how-do-i-detect-unsigned-integer-overflow
      // return (x > 0 && a > MAX - x) || (x < 0 && a < MIN - x)

      var result = getResult();
      var resultType = result.type().asDataType();
      var width = resultType.bitWidth();
      var min = Constant.Value.minValueOf(Type.signedInt(width)).toNode();
      var max = Constant.Value.maxValueOf(Type.signedInt(width)).toNode();

      // left side
      var xGt0 =
          BuiltInCall.of(BuiltInTable.SGTH,
              result, Constant.Value.of(0, resultType).toNode());
      var maxSx = BuiltInCall.of(BuiltInTable.SUB, max, result);
      var aGtmaxX = BuiltInCall.of(BuiltInTable.SGTH, firstArg(), maxSx);
      var leftAnd = BuiltInCall.of(BuiltInTable.AND, xGt0, aGtmaxX);

      // right side
      var xLt0 = GraphUtils.testSignBit(result);
      var minSx = BuiltInCall.of(BuiltInTable.SUB, min, result);
      var aLtminX = BuiltInCall.of(BuiltInTable.SLTH, firstArg(), minSx);
      var rightAnd = BuiltInCall.of(BuiltInTable.AND, xLt0, aLtminX);

      return BuiltInCall.of(BuiltInTable.OR, leftAnd, rightAnd);
    }

    @Override
    ExpressionNode checkCarry() {
      return BuiltInCall.of(
          BuiltInTable.ULTH,
          getResult(),
          firstArg()
      );
    }
  }


}
