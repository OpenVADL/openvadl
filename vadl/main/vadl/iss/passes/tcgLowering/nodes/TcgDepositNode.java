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
import java.util.function.Function;
import vadl.iss.passes.nodes.TcgVRefNode;
import vadl.javaannotations.viam.Input;
import vadl.utils.GraphUtils;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * Deposit t2 as a bitfield into t1, placing the result in dest().
 *
 * <p>The bitfield is described by pos/len, which are immediate values:
 * <ul>
 * <li>
 *   len - the length of the bitfield
 * </li>
 * <li>
 * pos - the position of the first bit, counting from the LSB
 * </li>
 * </ul>
 * For example, {@code deposit_i32 dest, t1, t2, 8, 4} indicates a 4-bit field at bit 8.
 * This operation would be equivalent to
 * {@code dest = (t1 & ~0x0f00) | ((t2 << 8) & 0x0f00)}
 */
public class TcgDepositNode extends TcgBinaryOpNode {

  @Input
  private ExpressionNode pos;

  @Input
  private ExpressionNode len;

  /**
   * Constructs a TCG deposit node.
   */
  public TcgDepositNode(TcgVRefNode dest,
                        TcgVRefNode t1, TcgVRefNode t2, ExpressionNode pos, ExpressionNode len) {
    super(dest, t1, t2);
    this.pos = pos;
    this.len = len;
  }

  /**
   * Constructs a TCG deposit node.
   */
  public TcgDepositNode(TcgVRefNode dest,
                        TcgVRefNode t1, TcgVRefNode t2, int pos, int len) {
    this(dest, t1, t2, GraphUtils.intU(pos, 32).toNode(), GraphUtils.intU(len, 32).toNode());
  }

  public ExpressionNode pos() {
    return pos;
  }

  public ExpressionNode len() {
    return len;
  }

  @Override
  public String tcgFunctionName() {
    return "tcg_gen_deposit_" + width();
  }

  @Override
  public String cCode(Function<Node, String> nodeToCCode) {
    return tcgFunctionName() + "(" + firstDest().varName() + ", " + arg1.varName() + ", "
        + arg2.varName() + ", " + nodeToCCode.apply(pos) + ", " + nodeToCCode.apply(len) + ");";
  }

  @Override
  public TcgDepositNode copy() {
    return new TcgDepositNode(firstDest().copy(), arg1.copy(), arg2.copy(), pos, len);
  }

  @Override
  public Node shallowCopy() {
    return new TcgDepositNode(firstDest(), arg1, arg2, pos, len);
  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    collection.add(pos);
    collection.add(len);
  }

  @Override
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    pos = visitor.apply(this, pos, ExpressionNode.class);
    len = visitor.apply(this, len, ExpressionNode.class);
  }
}
