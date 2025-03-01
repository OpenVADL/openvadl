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
import vadl.iss.passes.tcgLowering.TcgCondition;
import vadl.javaannotations.viam.DataValue;
import vadl.javaannotations.viam.Input;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;

/**
 * Represents the {@code tcg_gen_movcond} TCG operation.
 * If the condition {@code c1 cond c2} is satisfied, {@code dest} is assigned
 * to {@code v1}, otherwise to {@code v2}.
 */
public class TcgMovCondNode extends TcgOpNode {

  @Input
  TcgVRefNode c1;

  @Input
  TcgVRefNode c2;

  @Input
  TcgVRefNode v1;

  @Input
  TcgVRefNode v2;

  @DataValue
  TcgCondition cond;

  /**
   * Constructs the {@link TcgMovCondNode}.
   */
  public TcgMovCondNode(TcgVRefNode dest, TcgVRefNode c1, TcgVRefNode c2, TcgVRefNode v1,
                        TcgVRefNode v2,
                        TcgCondition cond) {
    super(dest, dest.width());
    this.c1 = c1;
    this.c2 = c2;
    this.v1 = v1;
    this.v2 = v2;
    this.cond = cond;
  }

  @Override
  public Set<TcgVRefNode> usedVars() {
    var sup = super.usedVars();
    sup.addAll(Set.of(c1, c2, v1, v2));
    return sup;
  }

  @Override
  public String cCode(Function<Node, String> nodeToCCode) {
    return "tcg_gen_movcond_" + width() + "("
        + cond.cCode() + ","
        + firstDest().cCode() + ","
        + c1.cCode() + ","
        + c2.cCode() + ","
        + v1.cCode() + ","
        + v2.cCode() + ");";
  }

  @Override
  public Node copy() {
    return new TcgMovCondNode(firstDest().copy(), c1.copy(), c2.copy(), v1.copy(), v2.copy(), cond);
  }

  @Override
  public Node shallowCopy() {
    return new TcgMovCondNode(firstDest(), c1, c2, v1, v2, cond);
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(cond);
  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    collection.add(c1);
    collection.add(c2);
    collection.add(v1);
    collection.add(v2);
  }

  @Override
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    c1 = visitor.apply(this, c1, TcgVRefNode.class);
    c2 = visitor.apply(this, c2, TcgVRefNode.class);
    v1 = visitor.apply(this, v1, TcgVRefNode.class);
    v2 = visitor.apply(this, v2, TcgVRefNode.class);
  }
}
