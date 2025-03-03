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

package vadl.viam.graph.control;

import java.util.List;
import vadl.javaannotations.viam.Input;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.DependencyNode;

/**
 * A (single directed) node in the CFG that schedules some dependency.
 * This allows scheduling dependency nodes without transforming them into control nodes.
 */
public class ScheduledNode extends DirectionalNode {

  @Input
  private DependencyNode node;

  public ScheduledNode(DependencyNode node) {
    this.node = node;
  }

  public DependencyNode node() {
    return node;
  }


  @Override
  public Node copy() {
    return new ScheduledNode(node.copy(DependencyNode.class));
  }

  @Override
  public Node shallowCopy() {
    return new ScheduledNode(node);
  }

  @Override
  public <T extends GraphNodeVisitor> void accept(T visitor) {
    // Node visitor is not supported
  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    collection.add(node);
  }

  @Override
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    node = visitor.apply(this, node, DependencyNode.class);
  }
}
