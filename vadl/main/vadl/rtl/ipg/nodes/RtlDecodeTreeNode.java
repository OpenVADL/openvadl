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
import javax.annotation.Nullable;
import vadl.javaannotations.viam.Input;
import vadl.rtl.passes.InstructionProgressGraphLowerPass;
import vadl.types.Type;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * Node that represents the instruction decoder in the RTL behaviour.
 */
public class RtlDecodeTreeNode extends ExpressionNode {

  @Input
  @Nullable
  protected ExpressionNode instructionWord;

  /**
   * Initial constructor of RtlDecodeTreeNode. Initially the type is 'void' but will be extended
   * by appending result 'signals' to it.
   */
  public RtlDecodeTreeNode() {
    super(Type.void_()); // Type will be extended once signals are added
  }

  private RtlDecodeTreeNode(Type type, @Nullable ExpressionNode instructionWord) {
    super(type);
    this.instructionWord = instructionWord;
  }

  /**
   * Add a new control signal decided by this decode tree node.
   *
   * @param signal The signal to add (either an is-instruction or one-hot node)
   */
  public void addSignal(ExpressionNode signal) {
    final int existingWidth = type().isDataType() ? type().asDataType().bitWidth() : 0;
    final int newWidth = signal.type().asDataType().bitWidth();
    setType(Type.bits(existingWidth + newWidth));
  }

  /**
   * Instruction word input, set by {@link InstructionProgressGraphLowerPass}.
   *
   * @return instruction word input
   */
  @Nullable
  public ExpressionNode instructionWord() {
    return instructionWord;
  }

  public void setInstructionWord(@Nullable ExpressionNode instructionWord) {
    updateUsageOf(this.instructionWord, instructionWord);
    this.instructionWord = instructionWord;
  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    if (this.instructionWord != null) {
      collection.add(instructionWord);
    }
  }

  @Override
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    instructionWord = visitor.applyNullable(this, instructionWord, ExpressionNode.class);
  }

  @Override
  public ExpressionNode copy() {
    return new RtlDecodeTreeNode(type(), instructionWord != null ? instructionWord.copy() : null);
  }

  @Override
  public Node shallowCopy() {
    return new RtlDecodeTreeNode(type(), instructionWord);
  }

  @Override
  public <T extends GraphNodeVisitor> void accept(T visitor) {
    visitor.visit(this);
  }

  @Override
  public String toString() {
    final int width = type().isDataType() ? type().asDataType().bitWidth() : 0;
    return "(" + id + ") DecodeTree<" + width + ">";
  }
}
