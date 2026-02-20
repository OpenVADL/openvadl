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
import vadl.types.DataType;
import vadl.viam.Counter;
import vadl.viam.RegisterTensor;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.ReadRegTensorNode;

/**
 * Unified ISS register read node for base and alias accesses.
 */
public class IssReadRegNode extends ReadRegTensorNode {

  public enum AccessKind {
    BASE,
    ALIAS
  }

  public enum ReadShape {
    FULL,
    SLICE,
    EXPANSION
  }

  @DataValue
  private final AccessKind accessKind;
  @DataValue
  private final ReadShape readShape;
  @DataValue
  @Nullable
  private final String accessorName;
  @DataValue
  private final NodeList<ExpressionNode> accessorIndices;

  public IssReadRegNode(RegisterTensor regTensor,
                        NodeList<ExpressionNode> resourceIndices,
                        DataType type) {
    this(regTensor, resourceIndices, type, AccessKind.BASE, ReadShape.FULL, null,
        resourceIndices.copy());
  }

  public IssReadRegNode(RegisterTensor regTensor,
                        NodeList<ExpressionNode> resourceIndices,
                        DataType type,
                        AccessKind accessKind,
                        ReadShape readShape,
                        @Nullable String accessorName,
                        NodeList<ExpressionNode> accessorIndices) {
    this(regTensor, resourceIndices, type, null, accessKind, readShape, accessorName,
        accessorIndices);
  }

  public IssReadRegNode(RegisterTensor regTensor,
                        NodeList<ExpressionNode> resourceIndices,
                        DataType type,
                        @Nullable Counter staticCounterAccess,
                        AccessKind accessKind,
                        ReadShape readShape,
                        @Nullable String accessorName,
                        NodeList<ExpressionNode> accessorIndices) {
    super(regTensor, resourceIndices, type, staticCounterAccess);
    this.accessKind = accessKind;
    this.readShape = readShape;
    this.accessorName = accessorName;
    this.accessorIndices = accessorIndices;
  }

  public AccessKind accessKind() {
    return accessKind;
  }

  public ReadShape readShape() {
    return readShape;
  }

  public @Nullable String accessorName() {
    return accessorName;
  }

  public NodeList<ExpressionNode> accessorIndices() {
    return accessorIndices;
  }

  @Override
  public IssReadRegNode copy() {
    return new IssReadRegNode(
        regTensor(),
        indices().copy(),
        type(),
        staticCounterAccess(),
        accessKind,
        readShape,
        accessorName,
        accessorIndices.copy());
  }

  @Override
  public IssReadRegNode shallowCopy() {
    return new IssReadRegNode(
        regTensor(),
        indices(),
        type(),
        staticCounterAccess(),
        accessKind,
        readShape,
        accessorName,
        accessorIndices);
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(accessKind);
    collection.add(readShape);
    collection.add(accessorName);
    collection.addAll(accessorIndices);
  }
}
