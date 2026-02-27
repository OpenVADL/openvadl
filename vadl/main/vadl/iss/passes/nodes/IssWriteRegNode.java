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
import vadl.types.Type;
import vadl.viam.Constant;
import vadl.viam.Counter;
import vadl.viam.RegisterTensor;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.WriteRegTensorNode;

/**
 * Unified ISS register write node used by all ISS backends.
 *
 * <p>This node carries register/alias accessor metadata and access-window metadata.
 * {@code indices()} represent effective resource indices used for validation and hazard analysis.
 * {@code accessorIndices()} represent backend accessor call arguments and may differ for aliases.
 *
 * <p>Window metadata ({@code windowKind}, {@code bitOffset}, {@code bitWidth}) models full writes
 * and chunked writes with one node shape, so backends can share scheduling and info retrieval.
 * See {@code docs/iss/register-access-domain-map.md} for the contract across lowering and codegen.
 */
public class IssWriteRegNode extends WriteRegTensorNode {

  /**
   * Defines whether this write addresses the base tensor directly or an alias accessor.
   */
  public enum AccessKind {
    BASE,
    ALIAS
  }

  /**
   * Classifies how write guards are interpreted during lowering/codegen.
   */
  public enum WriteGuardKind {
    NONE,
    ZERO_CONSTRAINT,
    CONDITIONAL
  }

  /**
   * Defines whether the write covers the full value or a chunk window.
   */
  public enum WindowKind {
    FULL,
    CHUNK
  }

  @DataValue
  private final AccessKind accessKind;
  @DataValue
  private final WriteGuardKind writeGuardKind;
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

  /**
   * Creates a base full-window write node.
   */
  public IssWriteRegNode(RegisterTensor regTensor,
                         NodeList<ExpressionNode> resourceIndices,
                         ExpressionNode value,
                         @Nullable ExpressionNode condition) {
    this(regTensor, resourceIndices, value, null, condition,
        AccessKind.BASE, WriteGuardKind.NONE, null,
        new NodeList<>(resourceIndices), WindowKind.FULL, intConst(0),
        intConst(value.type().asDataType().bitWidth()));
  }

  /**
   * Creates a full-window write node with explicit accessor metadata.
   */
  public IssWriteRegNode(RegisterTensor regTensor,
                         NodeList<ExpressionNode> resourceIndices,
                         ExpressionNode value,
                         @Nullable ExpressionNode condition,
                         AccessKind accessKind,
                         WriteGuardKind writeGuardKind,
                         @Nullable String accessorName,
                         NodeList<ExpressionNode> accessorIndices) {
    this(regTensor, resourceIndices, value, null, condition, accessKind, writeGuardKind,
        accessorName, accessorIndices, WindowKind.FULL, intConst(0),
        intConst(value.type().asDataType().bitWidth()));
  }

  /**
   * Creates a unified ISS write node with explicit accessor and window metadata.
   */
  public IssWriteRegNode(RegisterTensor regTensor,
                         NodeList<ExpressionNode> resourceIndices,
                         ExpressionNode value,
                         @Nullable Counter staticCounterAccess,
                         @Nullable ExpressionNode condition,
                         AccessKind accessKind,
                         WriteGuardKind writeGuardKind,
                         @Nullable String accessorName,
                         NodeList<ExpressionNode> accessorIndices) {
    this(regTensor, resourceIndices, value, staticCounterAccess, condition, accessKind,
        writeGuardKind, accessorName, accessorIndices, WindowKind.FULL, intConst(0),
        intConst(value.type().asDataType().bitWidth()));
  }

  /**
   * Creates a unified ISS write node with explicit accessor and chunk/full window metadata.
   */
  public IssWriteRegNode(RegisterTensor regTensor,
                         NodeList<ExpressionNode> resourceIndices,
                         ExpressionNode value,
                         @Nullable Counter staticCounterAccess,
                         @Nullable ExpressionNode condition,
                         AccessKind accessKind,
                         WriteGuardKind writeGuardKind,
                         @Nullable String accessorName,
                         NodeList<ExpressionNode> accessorIndices,
                         WindowKind windowKind,
                         ExpressionNode bitOffset,
                         ExpressionNode bitWidth) {
    super(regTensor, resourceIndices, value, staticCounterAccess, condition);
    this.accessKind = accessKind;
    this.writeGuardKind = writeGuardKind;
    this.accessorName = accessorName;
    this.accessorIndices = accessorIndices;
    this.windowKind = windowKind;
    this.bitOffset = bitOffset;
    this.bitWidth = bitWidth;
  }

  public AccessKind accessKind() {
    return accessKind;
  }

  public WriteGuardKind writeGuardKind() {
    return writeGuardKind;
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
  public int writeBitWidth() {
    if (windowKind == WindowKind.FULL) {
      return super.writeBitWidth();
    }
    if (bitWidth instanceof ConstantNode c) {
      return c.constant().asVal().intValue();
    }
    return value().type().asDataType().bitWidth();
  }

  @Override
  public Node copy() {
    return new IssWriteRegNode(
        regTensor(),
        indices().copy(),
        value().copy(),
        staticCounterAccess(),
        nullableCondition() == null ? null : nullableCondition().copy(),
        accessKind,
        writeGuardKind,
        accessorName,
        new NodeList<>(accessorIndices),
        windowKind,
        bitOffset.copy(),
        bitWidth.copy()
    );
  }

  @Override
  public Node shallowCopy() {
    return new IssWriteRegNode(
        regTensor(),
        indices(),
        value(),
        staticCounterAccess(),
        nullableCondition(),
        accessKind,
        writeGuardKind,
        accessorName,
        accessorIndices,
        windowKind,
        bitOffset,
        bitWidth
    );
  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    collection.addAll(accessorIndices);
    collection.add(bitOffset);
    collection.add(bitWidth);
  }

  @Override
  public void applyOnInputsUnsafe(vadl.viam.graph.GraphVisitor.Applier<Node> visitor) {
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
    collection.add(writeGuardKind);
    collection.add(accessorName);
    collection.add(windowKind);
  }

  private static ExpressionNode intConst(int value) {
    return Constant.Value.of(value, Type.bits(32)).toNode();
  }
}
