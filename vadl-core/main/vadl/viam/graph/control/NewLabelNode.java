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
import vadl.viam.PseudoInstruction;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.LabelNode;

/**
 * A node that indicates that a new temporary label has to be created. This is useful for
 * {@link PseudoInstruction} where you need to implement addressing for relative addressing.
 */
public class NewLabelNode extends DirectionalNode {
  @Input
  private LabelNode label;

  public NewLabelNode(LabelNode label) {
    this.label = label;
  }

  @Override
  public void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    collection.add(label);
  }

  @Override
  public void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    label = visitor.apply(this, label, LabelNode.class);
  }

  @Override
  public Node copy() {
    return new NewLabelNode((LabelNode) label.copy());
  }

  @Override
  public Node shallowCopy() {
    return new NewLabelNode(label);
  }

  @Override
  public <T extends GraphNodeVisitor> void accept(T visitor) {
    visitor.visit(this);
  }
}
