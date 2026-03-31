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

package vadl.gcb.passes.assembly;

import vadl.gcb.passes.assembly.visitors.AssemblyVisitor;
import vadl.types.BuiltInTable;
import vadl.types.Type;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.FieldRefNode;

/**
 * Register Node in assembly.
 */
public class AssemblyRegisterNode extends BuiltInCall {


  public AssemblyRegisterNode(NodeList<ExpressionNode> args,
                              Type type) {
    super(BuiltInTable.REGISTER, args, type);
  }


  public AssemblyRegisterNode(FieldRefNode fieldRefNode,
                              Type type) {
    super(BuiltInTable.REGISTER, new NodeList<>(fieldRefNode), type);
  }

  public FieldRefNode field() {
    return (FieldRefNode) args.get(0);
  }

  @Override
  public ExpressionNode copy() {
    return new AssemblyRegisterNode(
        new NodeList<>(this.arguments().stream().map(x -> (ExpressionNode) x.copy()).toList()),
        this.type());
  }

  @Override
  public Node shallowCopy() {
    return new AssemblyRegisterNode(args, type());
  }

  @Override
  public void accept(GraphNodeVisitor visitor) {
    if (visitor instanceof AssemblyVisitor v) {
      v.visit(this);
    } else {
      visitor.visit(this);
    }
  }
}
