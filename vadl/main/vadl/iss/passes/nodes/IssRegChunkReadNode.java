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
import vadl.types.Type;
import vadl.viam.Counter;
import vadl.viam.RegisterTensor;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.ReadResourceNode;

/**
 * Helper-only register read for a fixed chunk within a register element.
 */
public class IssRegChunkReadNode extends ReadResourceNode {
  @DataValue
  private RegisterTensor regTensor;
  @DataValue
  private int chunkOffsetBits;
  @DataValue
  private int chunkWidthBits;
  @DataValue
  @Nullable
  private Counter staticCounterAccess;

  public IssRegChunkReadNode(RegisterTensor regTensor,
                             NodeList<ExpressionNode> indices,
                             int chunkOffsetBits,
                             int chunkWidthBits,
                             @Nullable Counter staticCounterAccess) {
    super(indices, Type.bits(chunkWidthBits));
    this.regTensor = regTensor;
    this.chunkOffsetBits = chunkOffsetBits;
    this.chunkWidthBits = chunkWidthBits;
    this.staticCounterAccess = staticCounterAccess;
  }

  public RegisterTensor regTensor() {
    return regTensor;
  }

  public int chunkOffsetBits() {
    return chunkOffsetBits;
  }

  public int chunkWidthBits() {
    return chunkWidthBits;
  }

  public @Nullable Counter staticCounterAccess() {
    return staticCounterAccess;
  }

  @Override
  public RegisterTensor resourceDefinition() {
    return regTensor;
  }

  @Override
  public DataType type() {
    return (DataType) super.type();
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
  }

  @Override
  public IssRegChunkReadNode copy() {
    return new IssRegChunkReadNode(
        regTensor, indices.copy(), chunkOffsetBits, chunkWidthBits, staticCounterAccess);
  }

  @Override
  public Node shallowCopy() {
    return new IssRegChunkReadNode(
        regTensor, indices, chunkOffsetBits, chunkWidthBits, staticCounterAccess);
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
    collection.add(staticCounterAccess);
  }
}
