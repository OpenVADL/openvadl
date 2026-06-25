// SPDX-FileCopyrightText : © 2025-2026 TU Wien <vadl@tuwien.ac.at>
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

import java.util.function.Consumer;
import vadl.javaannotations.viam.Input;
import vadl.types.Type;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * Node that represents matching none of the instructions in the instruction set.
 */
public class RtlInvalidInstructionNode extends ExpressionNode {

  @Input
  protected RtlDecodeTreeNode decodeTree;

  /**
   * Create a new is-invalid-instruction node.
   *
   * @param decodeTree decode tree input
   */
  public RtlInvalidInstructionNode(RtlDecodeTreeNode decodeTree) {
    super(Type.bool());
    this.decodeTree = decodeTree;
  }

  /**
   * Decode tree deciding this node.
   *
   * @return the decoder deciding this node
   */
  public RtlDecodeTreeNode decodeTree() {
    return decodeTree;
  }

  @Override
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    decodeTree = visitor.apply(this, decodeTree, RtlDecodeTreeNode.class);
  }

  @Override
  protected void forEachInput(Consumer<Node> consumer) {
    super.forEachInput(consumer);
    consumer.accept(decodeTree);
  }

  @Override
  public ExpressionNode copy() {
    return new RtlInvalidInstructionNode(decodeTree.copy(RtlDecodeTreeNode.class));
  }

  @Override
  public Node shallowCopy() {
    return new RtlInvalidInstructionNode(decodeTree);
  }

  @Override
  public <T extends GraphNodeVisitor> void accept(T visitor) {
    visitor.visit(this);
  }

  @Override
  public String toString() {
    return "(" + id + ") InvalidInstruction";
  }
}
