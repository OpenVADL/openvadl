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

package vadl.viam.helper;

import vadl.types.BuiltInTable;
import vadl.types.Type;
import vadl.viam.Constant;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ExpressionNode;

public class TestGraphUtils {

  public static BuiltInCall binaryOp(BuiltInTable.BuiltIn op, Constant.Value a, Constant.Value b) {
    return new BuiltInCall(
        op,
        new NodeList<>(
            new ConstantNode(a),
            new ConstantNode(b)
        ),
        a.type()
    );
  }

  public static BuiltInCall binaryOp(BuiltInTable.BuiltIn op, Type resultType, ExpressionNode a,
                                     ExpressionNode b) {
    return new BuiltInCall(
        op,
        new NodeList<>(
            a,
            b
        ),
        resultType
    );
  }

  public static ConstantNode intSNode(long val, int width) {
    return new ConstantNode(intS(val, width));
  }

  public static ConstantNode intUNode(long val, int width) {
    return new ConstantNode(intU(val, width));
  }

  public static ConstantNode bitsNode(long val, int width) {
    return new ConstantNode(bits(val, width));
  }

  // constant value construction
  public static Constant.Value intS(long val, int width) {
    return Constant.Value.of(val, Type.signedInt(width));
  }

  public static Constant.Value intU(long val, int width) {
    return Constant.Value.of(val, Type.unsignedInt(width));
  }

  public static Constant.Value bits(long val, int width) {
    return Constant.Value.of(val, Type.bits(width));
  }


  public static Constant.Value bool(boolean val) {
    return Constant.Value.of(val);
  }

  public static Constant.Tuple.Status status(boolean negative, boolean zero, boolean carry,
                                             boolean overflow) {
    return new Constant.Tuple.Status(negative, zero, carry, overflow);
  }


}
