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

package vadl.rtl.ipg.nodes;

import java.util.List;
import vadl.javaannotations.viam.Input;
import vadl.types.Type;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * One-hot-decode node, representing a node packing a list of boolean inputs into an integer.
 */
public class RtlOneHotDecodeNode extends ExpressionNode {

  @Input
  RtlDecodeTreeNode decodeTree;

  /**
   * Create a new one-hot-decode node.
   */
  // TODO: Don't make this decoder specific
  public RtlOneHotDecodeNode(Type type, RtlDecodeTreeNode decodeTree) {
    super(type);
    this.decodeTree = decodeTree;
  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    collection.add(decodeTree);
  }

  @Override
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    decodeTree = visitor.apply(this, decodeTree, RtlDecodeTreeNode.class);
  }

  @Override
  public ExpressionNode copy() {
    return new RtlOneHotDecodeNode(type(), decodeTree.copy(RtlDecodeTreeNode.class));
  }

  @Override
  public Node shallowCopy() {
    return new RtlOneHotDecodeNode(type(), decodeTree);
  }

  @Override
  public <T extends GraphNodeVisitor> void accept(T visitor) {
    visitor.visit(this);
  }
}
