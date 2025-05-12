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
import javax.annotation.Nullable;
import vadl.javaannotations.viam.DataValue;
import vadl.viam.ArtificialResource;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;

/**
 * A write to an {@link ArtificialResource}.
 */
public class WriteArtificialResNode extends WriteResourceNode {

  @DataValue
  protected ArtificialResource resource;

  /**
   * Writes a value to a {@link ArtificialResource}.
   */
  public WriteArtificialResNode(ArtificialResource resource,
                                NodeList<ExpressionNode> indices,
                                ExpressionNode value) {
    super(indices, value, null);
    this.resource = resource;
  }

  /**
   * Writes a value to a {@link ArtificialResource}.
   */
  public WriteArtificialResNode(ArtificialResource resource, NodeList<ExpressionNode> indices,
                                ExpressionNode value,
                                @Nullable ExpressionNode condition) {
    super(indices, value, condition);
    this.resource = resource;
  }

  @Override
  public ArtificialResource resourceDefinition() {
    return resource;
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(resource);
  }

  @Override
  public Node copy() {
    return new WriteArtificialResNode(resource,
        indices().copy(),
        value.copy(),
        (condition != null ? condition.copy() : null));
  }

  @Override
  public Node shallowCopy() {
    return new WriteArtificialResNode(resource,
        indices().copy(),
        value,
        condition);
  }

  @Override
  public void accept(GraphNodeVisitor visitor) {
    visitor.visit(this);
  }
}
