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
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import vadl.javaannotations.viam.DataValue;
import vadl.javaannotations.viam.Input;
import vadl.types.DataType;
import vadl.types.Type;
import vadl.viam.Constant;
import vadl.viam.Counter;
import vadl.viam.RegisterTensor;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.ReadRegTensorNode;

/**
 * Unified ISS register read node used by all ISS backends.
 *
 * <p>This node represents both base register and alias register reads. The same metadata is used
 * by TCG translation paths and helper/cpu-side code generation:
 * <ul>
 *   <li>{@code indices()} describe the effective resource access and must match the referenced
 *   register tensor rank expected by validators and conflict analysis.</li>
 *   <li>{@code accessorIndices()} describe emitted accessor call arguments and may differ from
 *   resource indices for aliases.</li>
 *   <li>Window metadata ({@code windowKind}, {@code bitOffset}, {@code bitWidth}) describes
 *   full-width accesses and chunked sub-accesses in a backend-neutral form.</li>
 * </ul>
 *
 * <p>See {@code docs/iss/register-access-domain-map.md} for the cross-domain contract.
 */
public class IssReadRegNode extends ReadRegTensorNode {

  /**
   * Defines whether this read addresses the base tensor directly or an alias accessor.
   */
  public enum AccessKind {
    BASE,
    ALIAS
  }

  /**
   * Describes alias read shaping from the lowered semantics perspective.
   */
  public enum ReadShape {
    FULL,
    SLICE,
    EXPANSION
  }

  /**
   * Defines whether the access covers the full value or a chunk window.
   */
  public enum WindowKind {
    FULL,
    CHUNK
  }

  @DataValue
  private final AccessKind accessKind;
  @DataValue
  private final ReadShape readShape;
  @DataValue
  @Nullable
  private final String accessorName;
  @Input
  private NodeList<ExpressionNode> accessorIndices;
  @DataValue
  private final WindowKind windowKind;
  @Input
  private ExpressionNode bitOffset;
  @Input
  private ExpressionNode bitWidth;

  public IssReadRegNode(RegisterTensor regTensor,
                        NodeList<ExpressionNode> resourceIndices,
                        DataType type) {
    this(regTensor, resourceIndices, type, null, AccessKind.BASE, ReadShape.FULL, null,
        new NodeList<>(resourceIndices), WindowKind.FULL, intConst(0), intConst(type.bitWidth()));
  }

  /**
   * Creates a unified ISS read node with explicit accessor and window metadata.
   */
  public IssReadRegNode(RegisterTensor regTensor,
                        NodeList<ExpressionNode> resourceIndices,
                        DataType type,
                        AccessKind accessKind,
                        ReadShape readShape,
                        @Nullable String accessorName,
                        NodeList<ExpressionNode> accessorIndices) {
    this(regTensor, resourceIndices, type, null, accessKind, readShape, accessorName,
        accessorIndices, WindowKind.FULL, intConst(0), intConst(type.bitWidth()));
  }

  /**
   * Creates a unified ISS read node with explicit accessor and window metadata.
   */
  public IssReadRegNode(RegisterTensor regTensor,
                        NodeList<ExpressionNode> resourceIndices,
                        DataType type,
                        @Nullable Counter staticCounterAccess,
                        AccessKind accessKind,
                        ReadShape readShape,
                        @Nullable String accessorName,
                        NodeList<ExpressionNode> accessorIndices) {
    this(regTensor, resourceIndices, type, staticCounterAccess, accessKind, readShape,
        accessorName, accessorIndices, WindowKind.FULL, intConst(0), intConst(type.bitWidth()));
  }

  /**
   * Creates a unified ISS read node with explicit accessor and chunk/full window metadata.
   */
  public IssReadRegNode(RegisterTensor regTensor,
                        NodeList<ExpressionNode> resourceIndices,
                        DataType type,
                        @Nullable Counter staticCounterAccess,
                        AccessKind accessKind,
                        ReadShape readShape,
                        @Nullable String accessorName,
                        NodeList<ExpressionNode> accessorIndices,
                        WindowKind windowKind,
                        ExpressionNode bitOffset,
                        ExpressionNode bitWidth) {
    super(regTensor, resourceIndices, type, staticCounterAccess);
    this.accessKind = accessKind;
    this.readShape = readShape;
    this.accessorName = accessorName;
    this.accessorIndices = accessorIndices;
    this.windowKind = windowKind;
    this.bitOffset = bitOffset;
    this.bitWidth = bitWidth;
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

  public WindowKind windowKind() {
    return windowKind;
  }

  public ExpressionNode bitOffset() {
    return bitOffset;
  }

  public ExpressionNode bitWidth() {
    return bitWidth;
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
        new NodeList<>(accessorIndices),
        windowKind,
        bitOffset.copy(),
        bitWidth.copy());
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
        accessorIndices,
        windowKind,
        bitOffset,
        bitWidth);
  }

  @Override
  protected void collectInputs(List<vadl.viam.graph.Node> collection) {
    super.collectInputs(collection);
    collection.addAll(accessorIndices);
    collection.add(bitOffset);
    collection.add(bitWidth);
  }

  @Override
  public void applyOnInputsUnsafe(
      vadl.viam.graph.GraphVisitor.Applier<vadl.viam.graph.Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    accessorIndices = accessorIndices.stream()
        .map(e -> visitor.apply(this, e, ExpressionNode.class))
        .collect(Collectors.toCollection(NodeList::new));
    bitOffset = visitor.apply(this, bitOffset, ExpressionNode.class);
    bitWidth = visitor.apply(this, bitWidth, ExpressionNode.class);
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(accessKind);
    collection.add(readShape);
    collection.add(accessorName);
    collection.add(windowKind);
  }

  private static ExpressionNode intConst(int value) {
    return Constant.Value.of(value, Type.bits(32)).toNode();
  }
}
