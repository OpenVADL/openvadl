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

package vadl.iss.passes.tcgLowering.nodes;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import vadl.iss.passes.nodes.TcgVRefNode;
import vadl.javaannotations.viam.Input;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;

/**
 * Represents an abstract unary operation node within the Tiny Code Generator (TCG) framework.
 * This class serves as a base for specific unary operations by handling
 * common functionality such as argument management and data collection.
 */
public abstract class TcgUnaryOpNode extends TcgOpNode {

  @Input
  TcgVRefNode arg;

  public TcgUnaryOpNode(TcgVRefNode dest, TcgVRefNode arg) {
    super(dest, dest.width());
    this.arg = arg;
  }

  public TcgVRefNode arg() {
    return arg;
  }

  public abstract String tcgFunctionName();

  @Override
  public Set<TcgVRefNode> usedVars() {
    var sup = super.usedVars();
    sup.add(arg);
    return sup;
  }

  @Override
  public String cCode(Function<Node, String> nodeToCCode) {
    return tcgFunctionName() + "(" + firstDest().varName() + ", " + arg.varName() + ");";
  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    collection.add(arg);
  }

  @Override
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    arg = visitor.apply(this, arg, TcgVRefNode.class);
  }
}
