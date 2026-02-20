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
import vadl.viam.RegisterTensor;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.SideEffectNode;

/**
 * Writes a bitfield into a register tensor element:
 * {@code reg = deposit(base, value, offset, width)}.
 *
 * <p>This node is used when a write updates only a sub-window of a scalar register container.
 * It keeps bit-window information as graph inputs so translation-time constants can be propagated
 * through lowering and directly mapped to TCG deposit operations.
 *
 * <p>See {@code docs/iss/register-access-domain-map.md} for the relation to unified
 * {@link IssWriteRegNode} accesses and backend lowering.
 */
public class IssRegBitfieldWriteNode extends SideEffectNode {

  @DataValue
  private RegisterTensor regTensor;
  @Input
  private NodeList<ExpressionNode> indices;
  @Input
  private ExpressionNode value;
  @Input
  private ExpressionNode bitOffset;
  @Input
  private ExpressionNode bitWidth;
  @DataValue
  @Nullable
  private String aliasAccessorName;

  /**
   * Creates a bitfield write for a register element with explicit destination window.
   */
  public IssRegBitfieldWriteNode(RegisterTensor regTensor,
                                 NodeList<ExpressionNode> indices,
                                 ExpressionNode value,
                                 ExpressionNode bitOffset,
                                 ExpressionNode bitWidth,
                                 @Nullable String aliasAccessorName,
                                 @Nullable ExpressionNode condition) {
    super(condition);
    this.regTensor = regTensor;
    this.indices = indices;
    this.value = value;
    this.bitOffset = bitOffset;
    this.bitWidth = bitWidth;
    this.aliasAccessorName = aliasAccessorName;
  }

  public RegisterTensor regTensor() {
    return regTensor;
  }

  public NodeList<ExpressionNode> indices() {
    return indices;
  }

  public ExpressionNode value() {
    return value;
  }

  public ExpressionNode bitOffset() {
    return bitOffset;
  }

  public ExpressionNode bitWidth() {
    return bitWidth;
  }

  public @Nullable String aliasAccessorName() {
    return aliasAccessorName;
  }

  @Override
  public void verifyState() {
    super.verifyState();
    var containerWidth = regTensor.resultType(indices.size()).bitWidth();
    if (bitOffset instanceof vadl.viam.graph.dependency.ConstantNode bitOffsetConst
        && bitWidth instanceof vadl.viam.graph.dependency.ConstantNode bitWidthConst) {
      var bitOffsetVal = bitOffsetConst.constant().asVal().intValue();
      var bitWidthVal = bitWidthConst.constant().asVal().intValue();
      ensure(bitWidthVal > 0, "Bitfield width must be > 0");
      ensure(bitOffsetVal >= 0, "Bitfield offset must be >= 0");
      ensure(bitOffsetVal + bitWidthVal <= containerWidth,
          "Bitfield [%d:%d] exceeds register width %d",
          bitOffsetVal + bitWidthVal - 1, bitOffsetVal, containerWidth);
      ensure(value.type().asDataType().bitWidth() <= bitWidthVal,
          "Bitfield value width (%d) exceeds field width (%d)",
          value.type().asDataType().bitWidth(), bitWidthVal);
    }
  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    collection.addAll(indices);
    collection.add(value);
    collection.add(bitOffset);
    collection.add(bitWidth);
  }

  @Override
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    indices = indices.stream()
        .map(e -> visitor.apply(this, e, ExpressionNode.class))
        .collect(Collectors.toCollection(NodeList::new));
    value = visitor.apply(this, value, ExpressionNode.class);
    bitOffset = visitor.apply(this, bitOffset, ExpressionNode.class);
    bitWidth = visitor.apply(this, bitWidth, ExpressionNode.class);
  }

  @Override
  public IssRegBitfieldWriteNode copy() {
    return new IssRegBitfieldWriteNode(
        regTensor,
        indices.copy(),
        value.copy(),
        bitOffset.copy(),
        bitWidth.copy(),
        aliasAccessorName,
        nullableCondition()
    );
  }

  @Override
  public Node shallowCopy() {
    return new IssRegBitfieldWriteNode(
        regTensor,
        indices,
        value,
        bitOffset,
        bitWidth,
        aliasAccessorName,
        nullableCondition()
    );
  }

  @Override
  public <T extends GraphNodeVisitor> void accept(T visitor) {
    visitor.visit(this);
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(regTensor);
    collection.add(aliasAccessorName);
  }
}
