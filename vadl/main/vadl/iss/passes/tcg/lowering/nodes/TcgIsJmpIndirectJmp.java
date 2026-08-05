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
import vadl.viam.graph.Node;

/**
 * Represents a jump to an address that may not be statically known (at TCG translation time).
 *
 * <p>Is either emitted as a call to {@code tcg_gen_lookup_and_goto_ptr} or a call to a helper
 * function that calls {@code tcg_gen_lookup_and_goto_ptr} and modifies the {@code IsJmpState}.
 *
 * <p>Note: The helper function does <strong>not</strong> generate the PC update. The PC change must
 * be done beforehand by another operation (e.g. a {@link vadl.iss.passes.nodes.IssWriteRegNode}).
 */
public class TcgIsJmpIndirectJmp extends TcgNode {

  @DataValue
  protected final boolean writeIsJmpState;

  /**
   * Constructs a new TcgIndirectJmp node.
   *
   * @param writeIsJmpState Whether {@link TcgIsJmpStateProlog} exists and
   *                        {@code IsJmpState} can be written to.
   */
  public TcgIsJmpIndirectJmp(boolean writeIsJmpState) {
    this.writeIsJmpState = writeIsJmpState;
  }

  @Override
  public String cCode(Function<Node, String> nodeToCCode) {
    return writeIsJmpState
        ? "is_jmp_indirect_jmp(&s);"
        : "tcg_gen_lookup_and_goto_ptr();";
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
    return new TcgIsJmpIndirectJmp(writeIsJmpState);
  }

  @Override
  public Node shallowCopy() {
    return new TcgIsJmpIndirectJmp(writeIsJmpState);
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(writeIsJmpState);
  }
}
