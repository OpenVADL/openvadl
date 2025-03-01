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
 * LLVM node for unconditional jump.
 */
public class LlvmBrSD extends ExpressionNode implements LlvmNodeLowerable {

  @Input
  private ExpressionNode bb;

  /**
   * Constructor for the LLVM node "brcc".
   */
  public LlvmBrSD(ExpressionNode bb) {
    super(Type.dummy());
    this.bb = bb;
  }

  @Override
  public String lower() {
    return "br";
  }

  @Override
  public ExpressionNode copy() {
    return new LlvmBrSD(
        (ExpressionNode) bb.copy());
  }

  @Override
  public Node shallowCopy() {
    return new LlvmBrSD(bb);
  }

  @Override
  public void accept(GraphNodeVisitor visitor) {
    if (visitor instanceof TableGenMachineInstructionVisitor v) {
      v.visit(this);
    } else if (visitor instanceof TableGenNodeVisitor v) {
      v.visit(this);
    }
  }

  public ExpressionNode bb() {
    return bb;
  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    collection.add(bb);
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
  }

  @Override
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    bb = visitor.apply(this, bb, ExpressionNode.class);
  }
}
