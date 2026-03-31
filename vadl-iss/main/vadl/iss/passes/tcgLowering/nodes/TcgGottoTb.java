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
import java.util.Set;
import java.util.function.Function;
import vadl.iss.passes.nodes.TcgVRefNode;
import vadl.javaannotations.viam.DataValue;
import vadl.javaannotations.viam.Input;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * Representing a TCG (Tiny Code Generation)
 * operation node that models an absolute jump to a target program counter (PC) value.
 */
public class TcgGottoTb extends TcgNode {

  /**
   * Indicates the jump slot for the branch optimization.
   *
   * <p>QEMU allows two jump slots per instruction.
   * If we assign some jump slot, the address to jump to must stay the same.
   * <ul>
   * <li>The NEXT_INSTR jump slot reserved for the jump to the next instruction (default case),
   * except if we always jump away.</li>
   * <li>The BRANCH_OUT jump slot is used for one of the jumps away.</li>
   * </ul>
   * E.g. the jump to the address in the BEQ RISC-V instruction in case of EQ would be assigned
   * to slot BRANCH_OUT (1), while the default case, the jump to the next instruction
   * would get NEXT_INSTR (0).
   * However, the branch to the next instruction is implicitly handled via the DISAS_CHAIN
   * jump status (take a look at translate.c:arch_tr_tb_stop).</p>
   *
   * <p>The LOOK_UP (-1) is used to indicate that we don't assign any jump slot.
   * This is the case if there are two or more jumps in one instruction, then only one jump may
   * get a slot, and the other one must always be looked up.</p>
   */
  public enum JmpSlot {
    NEXT_INSTR(0), // translates to 0
    BRANCH_OUT(1), // translate to 1
    LOOK_UP(-1);    // -1: no jump slot, we must make a lookup

    // used in C code
    public final int code;

    JmpSlot(int code) {
      this.code = code;
    }
  }

  @Input
  private ExpressionNode targetPc;

  @DataValue
  private JmpSlot jmpSlot;


  /**
   * Constructs a TcgGottoTbAbs node for TCG IR generation.
   *
   * @param targetPc The expression node representing the target program counter (PC) value.
   */
  public TcgGottoTb(ExpressionNode targetPc, JmpSlot jmpSlot) {
    this.targetPc = targetPc;
    this.jmpSlot = jmpSlot;
  }

  public ExpressionNode targetPc() {
    return targetPc;
  }

  public JmpSlot jmpSlot() {
    return jmpSlot;
  }

  @Override
  public String cCode(Function<Node, String> nodeToCCode) {
    return "gen_goto_tb(ctx, " + jmpSlot.code + ", " + nodeToCCode.apply(targetPc) + ");";
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
  public Node copy() {
    return new TcgGottoTb(targetPc.copy(ExpressionNode.class), jmpSlot);
  }

  @Override
  public Node shallowCopy() {
    return new TcgGottoTb(targetPc, jmpSlot);
  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    collection.add(targetPc);
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(jmpSlot);
  }

  @Override
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    targetPc = visitor.apply(this, targetPc, ExpressionNode.class);
  }
}
