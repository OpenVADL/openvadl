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
import vadl.types.Type;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * Represents an ISS intermediate multiplication node that returns only
 * the upper half of the multiplication result.
 */
public class IssMulhNode extends IssBinaryNode {

  @DataValue
  private IssMulKind kind;

  public IssMulhNode(ExpressionNode arg1, ExpressionNode arg2, IssMulKind kind, Type type) {
    super(arg1, arg2, type);
    this.kind = kind;
  }

  public IssMulKind kind() {
    return kind;
  }

  @Override
  public ExpressionNode copy() {
    return new IssMulhNode(arg1().copy(), arg2().copy(), kind, type());
  }

  @Override
  public Node shallowCopy() {
    return new IssMulhNode(arg1(), arg2(), kind, type());
  }

  @Override
  public <T extends GraphNodeVisitor> void accept(T visitor) {

  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(kind);
  }
}
