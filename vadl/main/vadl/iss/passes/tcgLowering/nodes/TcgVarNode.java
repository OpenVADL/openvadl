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
import vadl.iss.passes.nodes.TcgVRefNode;
import vadl.iss.passes.tcgLowering.TcgV;
import vadl.javaannotations.viam.Input;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;

/**
 * An abstract base class for TCG nodes that are associated with a {@link TcgV} variable.
 * This class provides common functionality for nodes that work
 * with TCG variables in the TCG lowering process.
 */
public abstract class TcgVarNode extends TcgNode {

  /**
   * The TCG variable associated with this node.
   */
  @Input
  private TcgVRefNode variable;

  public TcgVarNode(TcgVRefNode variable) {
    this.variable = variable;
  }

  /**
   * Returns the {@link TcgV} variable associated with this node.
   *
   * @return The TCG variable of this node.
   */
  public TcgVRefNode variable() {
    return variable;
  }

  @Override
  public Set<TcgVRefNode> usedVars() {
    return Set.of();
  }

  @Override
  public List<TcgVRefNode> definedVars() {
    return List.of();
  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    collection.add(variable);
  }

  @Override
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    variable = visitor.apply(this, variable, TcgVRefNode.class);
  }
}