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
import java.util.Objects;
import javax.annotation.Nullable;
import vadl.javaannotations.viam.DataValue;
import vadl.viam.ArtificialResource;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;

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
                                @Nullable ExpressionNode address,
                                ExpressionNode value) {
    super(address, value);
    this.resource = resource;
  }

  /**
   * Writes a value to a {@link ArtificialResource}.
   */
  public WriteArtificialResNode(ArtificialResource resource, ExpressionNode address,
                                ExpressionNode value,
                                @Nullable ExpressionNode condition) {
    super(address, value);
    this.resource = resource;
    this.condition = condition;
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
        address().copy(),
        value.copy(),
        (condition != null ? condition.copy() : null));
  }

  @Override
  public Node shallowCopy() {
    return new WriteArtificialResNode(resource,
        Objects.requireNonNull(address),
        value,
        condition);
  }

  @Override
  public void accept(GraphNodeVisitor visitor) {
    visitor.visit(this);
  }
}
