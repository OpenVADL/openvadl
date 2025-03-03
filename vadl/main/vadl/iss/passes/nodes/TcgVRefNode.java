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

package vadl.iss.passes.nodes;

import java.util.List;
import javax.annotation.Nullable;
import vadl.iss.passes.tcgLowering.TcgV;
import vadl.iss.passes.tcgLowering.Tcg_32_64;
import vadl.javaannotations.viam.DataValue;
import vadl.javaannotations.viam.Input;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.DependencyNode;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * A dependency node that holds the TcgV variable.
 * This is a node, as it makes it easier to optimize variable usages during allocation.
 */
public class TcgVRefNode extends DependencyNode {

  @DataValue
  private TcgV var;

  @Input
  @Nullable
  // the immediate(constant) value or register file index.
  private ExpressionNode dependency;

  public TcgVRefNode(TcgV var, @Nullable ExpressionNode dependency) {
    this.var = var;
    this.dependency = dependency;
  }

  public TcgV var() {
    return var;
  }

  @Nullable
  public ExpressionNode dependency() {
    return dependency;
  }

  public void setVar(TcgV var) {
    this.var = var;
  }

  public Tcg_32_64 width() {
    return var.width();
  }

  public String varName() {
    return var.varName();
  }

  @SuppressWarnings("MethodName")
  public String cCode() {
    return var.varName();
  }

  @Override
  public TcgVRefNode copy() {
    return new TcgVRefNode(var, dependency == null ? null : dependency.copy());
  }

  @Override
  public Node shallowCopy() {
    return new TcgVRefNode(var, dependency);
  }

  @Override
  public <T extends GraphNodeVisitor> void accept(T visitor) {

  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(var);
  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    if (this.dependency != null) {
      collection.add(dependency);
    }
  }

  @Override
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    dependency = visitor.applyNullable(this, dependency, ExpressionNode.class);
  }
}
