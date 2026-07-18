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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import vadl.javaannotations.viam.DataValue;
import vadl.javaannotations.viam.Input;
import vadl.types.Type;
import vadl.viam.Instruction;
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

  @DataValue
  List<Set<Instruction>> instructions;

  /**
   * Create a new one-hot-decode node.
   */
  public RtlOneHotDecodeNode(Type type, Collection<Set<Instruction>> instructions,
                             RtlDecodeTreeNode decodeTree) {
    super(type);
    this.instructions = new ArrayList<>(instructions);
    this.decodeTree = decodeTree;
  }

  /**
   * Get the list of instruction sets this one-hot-decode node matches.
   *
   * @return list of instruction sets
   */
  public List<Set<Instruction>> instructions() {
    return instructions;
  }

  @Override
  protected void forEachInput(Consumer<Node> consumer) {
    super.forEachInput(consumer);
    consumer.accept(decodeTree);
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(instructions);
  }

  @Override
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    decodeTree = visitor.apply(this, decodeTree, RtlDecodeTreeNode.class);
  }

  @Override
  public ExpressionNode copy() {
    return new RtlOneHotDecodeNode(type(), instructions, decodeTree.copy(RtlDecodeTreeNode.class));
  }

  @Override
  public Node shallowCopy() {
    return new RtlOneHotDecodeNode(type(), instructions, decodeTree);
  }

  @Override
  public <T extends GraphNodeVisitor> void accept(T visitor) {
    visitor.visit(this);
  }

  @Override
  public String toString() {
    return "(" + id + ") OneHot<" + instructions.size() + " sets, " + instructions.stream()
        .reduce(0, (a, b) -> a + b.size(), Integer::sum)
        + " total instructions>";
  }
}
