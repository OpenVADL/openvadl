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
import vadl.javaannotations.viam.DataValue;
import vadl.types.DataType;
import vadl.viam.RegisterTensor;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.ReadRegTensorNode;

/**
 * Register read node that should use alias accessor names in ISS translate generation.
 */
public class IssAliasReadRegTensorNode extends ReadRegTensorNode {

  @DataValue
  private final String aliasAccessorName;
  @DataValue
  private final NodeList<ExpressionNode> accessorIndices;

  public IssAliasReadRegTensorNode(RegisterTensor regTensor,
                                   NodeList<ExpressionNode> indices,
                                   DataType type,
                                   String aliasAccessorName) {
    this(regTensor, indices, type, aliasAccessorName, indices.copy());
  }

  public IssAliasReadRegTensorNode(RegisterTensor regTensor,
                                   NodeList<ExpressionNode> indices,
                                   DataType type,
                                   String aliasAccessorName,
                                   NodeList<ExpressionNode> accessorIndices) {
    super(regTensor, indices, type, null);
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
  public IssAliasReadRegTensorNode copy() {
    return new IssAliasReadRegTensorNode(
        regTensor(),
        indices().copy(),
        type(),
        aliasAccessorName,
        accessorIndices.copy());
  }

  @Override
  public IssAliasReadRegTensorNode shallowCopy() {
    return new IssAliasReadRegTensorNode(
        regTensor(),
        indices(),
        type(),
        aliasAccessorName,
        accessorIndices);
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(aliasAccessorName);
    collection.addAll(accessorIndices);
  }
}
