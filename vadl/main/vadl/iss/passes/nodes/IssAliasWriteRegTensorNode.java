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

package vadl.iss.passes.nodes;

import java.util.List;
import javax.annotation.Nullable;
import vadl.javaannotations.viam.DataValue;
import vadl.viam.RegisterTensor;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.WriteRegTensorNode;

/**
 * Register write node that should use alias accessor names in ISS translate generation.
 */
public class IssAliasWriteRegTensorNode extends WriteRegTensorNode {

  @DataValue
  private final String aliasAccessorName;
  @DataValue
  private final NodeList<ExpressionNode> accessorIndices;

  public IssAliasWriteRegTensorNode(RegisterTensor regTensor,
                                    NodeList<ExpressionNode> indices,
                                    ExpressionNode value,
                                    String aliasAccessorName) {
    this(regTensor, indices, value, aliasAccessorName, indices.copy(), null);
  }

  public IssAliasWriteRegTensorNode(RegisterTensor regTensor,
                                    NodeList<ExpressionNode> indices,
                                    ExpressionNode value,
                                    String aliasAccessorName,
                                    NodeList<ExpressionNode> accessorIndices,
                                    @Nullable ExpressionNode condition) {
    super(regTensor, indices, value, null, condition);
    this.aliasAccessorName = aliasAccessorName;
    this.accessorIndices = accessorIndices;
  }

  public String aliasAccessorName() {
    return aliasAccessorName;
  }

  public NodeList<ExpressionNode> accessorIndices() {
    return accessorIndices;
  }

  @Override
  public Node copy() {
    return new IssAliasWriteRegTensorNode(
        regTensor(),
        indices().copy(),
        value().copy(),
        aliasAccessorName,
        accessorIndices.copy(),
        nullableCondition() == null ? null : nullableCondition().copy()
    );
  }

  @Override
  public Node shallowCopy() {
    return new IssAliasWriteRegTensorNode(
        regTensor(),
        indices(),
        value(),
        aliasAccessorName,
        accessorIndices,
        nullableCondition()
    );
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(aliasAccessorName);
    collection.addAll(accessorIndices);
  }
}
