// SPDX-FileCopyrightText : © 2026 TU Wien <vadl@tuwien.ac.at>
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

package vadl.iss.passes.tcg.lowering.nodes;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import vadl.iss.passes.nodes.TcgVRefNode;
import vadl.javaannotations.viam.DataValue;
import vadl.javaannotations.viam.Input;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * Represents a jump to a statically (at TCG translation time) known address.
 *
 * <p>Not really a TCG operation, but a call to a helper function that calls {@code gen_goto_tb}
 * and uses/modifies the {@code IsJmpState} to determine if we can chain to the next
 * translation block.
 *
 * <p>Note: The helper function also generates the PC update.
 */
public class TcgIsJmpDirectJmp extends TcgNode {

  @Input
  protected ExpressionNode targetPc;
  @DataValue
  private final boolean useJmpSlot;

  /**
   * Constructs a new TcgIsJmpDirectJmp node.
   *
   * @param targetPc The PC to jump to.
   * @param useJmpSlot Whether to use jump slots to chain to the next translation block.
   */
  public TcgIsJmpDirectJmp(ExpressionNode targetPc, boolean useJmpSlot) {
    this.targetPc = targetPc;
    this.useJmpSlot = useJmpSlot;
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
  public String cCode(Function<Node, String> nodeToCCode) {
    return "is_jmp_direct_jmp(ctx, &s, " + useJmpSlot + ", " + nodeToCCode.apply(targetPc) + ");";
  }

  @Override
  public Node copy() {
    return new TcgIsJmpDirectJmp(targetPc.copy(), useJmpSlot);
  }

  @Override
  public Node shallowCopy() {
    return new TcgIsJmpDirectJmp(targetPc, useJmpSlot);
  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    collection.add(targetPc);
  }

  @Override
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    targetPc = visitor.apply(this, targetPc, ExpressionNode.class);
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(useJmpSlot);
  }
}
