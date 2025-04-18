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

package vadl.viam.graph.dependency;

import java.util.List;
import vadl.javaannotations.viam.DataValue;
import vadl.viam.RegisterTensor;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;

/**
 * Writes to a {@link RegisterTensor}.
 * TODO: Add more documentation
 */
public class WriteRegTensorNode extends WriteResourceNode {

  @DataValue
  protected RegisterTensor regTensor;

  public WriteRegTensorNode(RegisterTensor regTensor, NodeList<ExpressionNode> indices,
                            ExpressionNode value) {
    super(indices, value);
    this.regTensor = regTensor;
  }

  @Override
  public RegisterTensor resourceDefinition() {
    return regTensor;
  }

  @Override
  public void verifyState() {
    super.verifyState();
    ensure(indices().size() <= regTensor.maxNumberOfAccessIndices(),
        "Too many indices for tensor access. Write uses %d indices, tensor has %d indices",
        indices().size(), regTensor.maxNumberOfAccessIndices());
    // TODO: Check index types
  }

  @Override
  public Node copy() {
    return new WriteRegTensorNode(regTensor, indices.copy(), value.copy());
  }

  @Override
  public Node shallowCopy() {
    return new WriteRegTensorNode(regTensor, indices, value);
  }

  @Override
  public <T extends GraphNodeVisitor> void accept(T visitor) {

  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(regTensor);
  }
}
