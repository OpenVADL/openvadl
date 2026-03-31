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

package vadl.iss.passes.opDecomposition.nodes;

import java.util.List;
import vadl.javaannotations.viam.DataValue;
import vadl.types.TupleType;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * The ISS Mul2 node represents a long multiplication that returns a tuple of two integers.
 * The first one holds the lower half of the multiplication, while the second one is the upper
 * half.
 * We need this as long multiplication tends to exceed the maximum supported result size of
 * 64bit. So we have to split it into two smaller results.
 */
public class IssMul2Node extends IssBinaryNode {

  @DataValue
  private IssMulKind kind;


  public IssMul2Node(ExpressionNode arg1, ExpressionNode arg2, IssMulKind kind,
                     TupleType resultType) {
    super(arg1, arg2, resultType);
    this.kind = kind;
  }

  public IssMulKind kind() {
    return kind;
  }

  @Override
  public TupleType type() {
    return (TupleType) super.type();
  }

  @Override
  public ExpressionNode copy() {
    return new IssMul2Node(arg1().copy(), arg2().copy(), kind, type());
  }

  @Override
  public ExpressionNode shallowCopy() {
    return new IssMul2Node(arg1(), arg2(), kind, type());
  }

  @Override
  public <T extends GraphNodeVisitor> void accept(T visitor) {
    // not used
  }


  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(kind);
  }
}
