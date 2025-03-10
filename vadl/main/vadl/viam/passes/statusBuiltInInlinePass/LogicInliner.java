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
import static vadl.utils.GraphUtils.bool;
import static vadl.utils.GraphUtils.or;

import vadl.types.BuiltInTable;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * Contains the status built-in inliners for logic operations.
 */
abstract class LogicInliner extends Inliner {

  LogicInliner(BuiltInCall builtInCall) {
    super(builtInCall);
  }

  @Override
  ExpressionNode checkOverflow() {
    // always false for and
    return bool(false).toNode();
  }

  @Override
  ExpressionNode checkCarry() {
    // always false for and
    return bool(false).toNode();
  }

  static class AndS extends LogicInliner {

    AndS(BuiltInCall builtInCall) {
      super(builtInCall);
    }

    @Override
    ExpressionNode createResult() {
      return and(arg0(), arg1());
    }

  }

  static class OrS extends LogicInliner {

    OrS(BuiltInCall builtInCall) {
      super(builtInCall);
    }

    @Override
    ExpressionNode createResult() {
      return or(arg0(), arg1());
    }
  }

  static class XorS extends LogicInliner {

    XorS(BuiltInCall builtInCall) {
      super(builtInCall);
    }

    @Override
    ExpressionNode createResult() {
      return binaryOp(BuiltInTable.XOR, arg0(), arg1());
    }
  }

}
