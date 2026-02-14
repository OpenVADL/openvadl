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
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.SideEffectNode;

/**
 * Helper-only register write for a fixed chunk within a register element.
 */
public class IssRegChunkWriteNode extends SideEffectNode {
  @DataValue
  private RegisterTensor regTensor;
  @DataValue
  private int chunkOffsetBits;
  @DataValue
  private int chunkWidthBits;
  @Input
  private NodeList<ExpressionNode> indices;
  @Input
  private ExpressionNode value;

  /**
   * Creates a helper-only chunked register write node.
   */
  public IssRegChunkWriteNode(RegisterTensor regTensor,
                              NodeList<ExpressionNode> indices,
                              ExpressionNode value,
                              int chunkOffsetBits,
                              int chunkWidthBits,
                              @Nullable ExpressionNode condition) {
    super(condition);
    this.regTensor = regTensor;
    this.indices = indices;
    this.value = value;
    this.chunkOffsetBits = chunkOffsetBits;
    this.chunkWidthBits = chunkWidthBits;
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

  public int chunkOffsetBits() {
    return chunkOffsetBits;
  }

  public int chunkWidthBits() {
    return chunkWidthBits;
  }

  @Override
  public void verifyState() {
    super.verifyState();
    int containerWidth = regTensor.resultType(indices.size()).bitWidth();
    ensure(chunkWidthBits > 0 && chunkWidthBits <= 64,
        "Chunk width must be in range 1..64 but is %d", chunkWidthBits);
    ensure(chunkOffsetBits >= 0 && chunkOffsetBits + chunkWidthBits <= containerWidth,
        "Chunk [%d:%d] exceeds register access width %d",
        chunkOffsetBits + chunkWidthBits - 1, chunkOffsetBits, containerWidth);
    ensure(value.type().asDataType().bitWidth() <= chunkWidthBits,
        "Chunk write value width %d exceeds chunk width %d",
        value.type().asDataType().bitWidth(), chunkWidthBits);
  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    collection.addAll(indices);
    collection.add(value);
  }

  @Override
  protected void applyOnInputsUnsafe(vadl.viam.graph.GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    indices = indices.stream().map((e) -> visitor.apply(this, e, ExpressionNode.class)).collect(
        Collectors.toCollection(NodeList::new));
    value = visitor.apply(this, value, ExpressionNode.class);
  }

  @Override
  public IssRegChunkWriteNode copy() {
    return new IssRegChunkWriteNode(
        regTensor,
        indices.copy(),
        value.copy(),
        chunkOffsetBits,
        chunkWidthBits,
        nullableCondition());
  }

  @Override
  public Node shallowCopy() {
    return new IssRegChunkWriteNode(
        regTensor, indices, value, chunkOffsetBits, chunkWidthBits, nullableCondition());
  }

  @Override
  public <T extends GraphNodeVisitor> void accept(T visitor) {
    visitor.visit(this);
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(regTensor);
    collection.add(chunkOffsetBits);
    collection.add(chunkWidthBits);
  }
}
