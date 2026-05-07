// SPDX-FileCopyrightText : © 2025-2026 TU Wien <vadl@tuwien.ac.at>
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
import vadl.iss.passes.common.opDecomposition.nodes.IssExprNode;
import vadl.javaannotations.viam.DataValue;
import vadl.types.Type;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * Represents a temporary expression result that is set form some branch in the control flow.
 *
 * <p>This is necessary to turn select expressions into a control flow graph.
 * This node then represents the result of the select operation and can be used by other
 * dependency nodes.
 */
public class IssTempExprNode extends IssExprNode {

  @DataValue
  private int tempId;

  public IssTempExprNode(int tmpId, Type type) {
    super(type);
    this.tempId = tmpId;
  }

  public int tempId() {
    return tempId;
  }

  @Override
  public ExpressionNode copy() {
    return new IssTempExprNode(tempId, type());
  }

  @Override
  public Node shallowCopy() {
    return new IssTempExprNode(tempId, type());
  }

  @Override
  public <T extends GraphNodeVisitor> void accept(T visitor) {
    // not used
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(tempId);
  }
}
