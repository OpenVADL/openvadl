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
 * Represents a write operation to (parts of) a register that is part of the translation block
 * state. Whether the written value is statically known at TCG translation time is determined
 * by the field {@link #isStatic}.
 *
 * <p>Not really a TCG operation, but a call to a helper function that modifies the
 * {@code IsJmpState}. This node does <strong>not</strong> replace an actual write,
 * it just sets some flags needed during TCG translation time to decide how to exit/chain
 * translation blocks.
 */
public class TcgIsJmpTbStateWrite extends TcgNode {

  @DataValue
  protected boolean isStatic;

  public TcgIsJmpTbStateWrite(boolean isStatic) {
    this.isStatic = isStatic;
  }

  @Override
  public String cCode(Function<Node, String> nodeToCCode) {
    return "is_jmp_" + (isStatic ? "static" : "dynamic") + "_tb_state_write(&s);";
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
    return new TcgIsJmpTbStateWrite(isStatic);
  }

  @Override
  public Node shallowCopy() {
    return new TcgIsJmpTbStateWrite(isStatic);
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(isStatic);
  }
}
