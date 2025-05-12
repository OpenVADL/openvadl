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
import vadl.types.DataType;
import vadl.viam.ArtificialResource;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;

/**
 * A read of an {@link ArtificialResource}.
 */
public class ReadArtificialResNode extends ReadResourceNode {

  @DataValue
  private final ArtificialResource resource;

  public ReadArtificialResNode(ArtificialResource artificialResource,
                               NodeList<ExpressionNode> indices,
                               DataType type) {
    super(indices, type);
    this.resource = artificialResource;
  }

  @Override
  public ArtificialResource resourceDefinition() {
    return resource;
  }

  @Override
  public ExpressionNode copy() {
    return new ReadArtificialResNode(resource, indices, type());
  }

  @Override
  public Node shallowCopy() {
    return new ReadArtificialResNode(resource, indices, type());
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(resource);
  }

  @Override
  public <T extends GraphNodeVisitor> void accept(T visitor) {

  }
}
