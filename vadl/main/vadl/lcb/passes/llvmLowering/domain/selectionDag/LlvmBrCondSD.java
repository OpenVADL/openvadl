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

package vadl.lcb.passes.llvmLowering.domain.selectionDag;

import java.util.List;
import vadl.javaannotations.viam.Input;
import vadl.lcb.passes.llvmLowering.LlvmNodeLowerable;
import vadl.lcb.passes.llvmLowering.strategies.visitors.TableGenMachineInstructionVisitor;
import vadl.lcb.passes.llvmLowering.strategies.visitors.TableGenNodeVisitor;
import vadl.types.Type;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * LLVM node for conditional branch which can operate on expressions.
 */
public class LlvmBrCondSD extends ExpressionNode implements LlvmNodeLowerable {

  @Input
  private ExpressionNode condition;

  @Input
  private ExpressionNode immOffset;

  /**
   * Constructor for {@link LlvmBrCondSD}.
   */
  public LlvmBrCondSD(ExpressionNode condition,
                      ExpressionNode immOffset) {
    super(Type.dummy());
    this.condition = condition;
    this.immOffset = immOffset;
  }

  @Override
  public String lower() {
    return "brcond";
  }

  @Override
  public ExpressionNode copy() {
    return new LlvmBrCondSD((ExpressionNode) condition.copy(), (ExpressionNode) immOffset.copy());
  }

  @Override
  public Node shallowCopy() {
    return new LlvmBrCondSD(condition, immOffset);
  }

  public ExpressionNode condition() {
    return condition;
  }

  public ExpressionNode immOffset() {
    return immOffset;
  }


  @Override
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    condition = visitor.apply(this, condition, ExpressionNode.class);
    immOffset = visitor.apply(this, immOffset, ExpressionNode.class);
  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    collection.add(condition);
    collection.add(immOffset);
  }

  @Override
  public <T extends GraphNodeVisitor> void accept(T visitor) {
    if (visitor instanceof TableGenMachineInstructionVisitor v) {
      v.visit(this);
    } else if (visitor instanceof TableGenNodeVisitor v) {
      v.visit(this);
    } else {
      visitor.visit(this);
    }
  }
}

