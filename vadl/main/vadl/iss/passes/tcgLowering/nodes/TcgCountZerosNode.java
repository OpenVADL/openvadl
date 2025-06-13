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
import vadl.iss.passes.nodes.TcgVRefNode;
import vadl.javaannotations.viam.DataValue;
import vadl.viam.graph.Node;

/**
 * Represents the {@code tcg_gen_clz/ctz} TCG instruction in the TCG VIAM lowering.
 * The semantics are: {@code t0 = t1 ? clz/ctz(t1) : t2}, where t2 represents the fallback
 * if t1 is 0. In VADL t2 would be the size of t1.
 */
public class TcgCountZerosNode extends TcgBinaryOpNode {

  /**
   * Kind of count zero bits operation.
   * Either clz or ctz.
   */
  public enum Kind {
    LEADING,
    TRAILING,
  }

  @DataValue
  private Kind kind;

  public TcgCountZerosNode(Kind kind, TcgVRefNode t0, TcgVRefNode t1, TcgVRefNode t2) {
    super(t0, t1, t2, t0.width());
    this.kind = kind;
  }

  public Kind kind() {
    return kind;
  }

  @Override
  public String tcgFunctionName() {
    var name = kind == Kind.LEADING ? "clz" : "ctz";
    return "tcg_gen_" + name;
  }

  @Override
  public Node copy() {
    return new TcgCountZerosNode(kind, firstDest().copy(TcgVRefNode.class),
        arg1.copy(TcgVRefNode.class),
        arg2.copy(TcgVRefNode.class));
  }

  @Override
  public Node shallowCopy() {
    return new TcgCountZerosNode(kind, firstDest(), arg1, arg2);
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(kind);
  }
}
