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
import vadl.types.Type;
import vadl.utils.GraphUtils;
import vadl.viam.Constant;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;

/**
 * Represents the index of a {@code forall} VADL construct.
 * It is the dependency of all {@code forall} related constructs and holds its value range.
 */
public class ForIdxNode extends ExpressionNode {

  @DataValue
  private int fromIdx;
  @DataValue
  private int toIdx;

  /**
   * Construct the index node.
   */
  public ForIdxNode(Type type, int fromIdx, int toIdx) {
    super(type);
    this.fromIdx = fromIdx;
    this.toIdx = toIdx;
  }

  public int fromIdx() {
    return fromIdx;
  }

  public int toIdx() {
    return toIdx;
  }

  @Override
  public DataType type() {
    return (DataType) super.type();
  }

  public Constant.Value fromVal() {
    return GraphUtils.intU(fromIdx(), type().bitWidth());
  }

  public Constant.Value toVal() {
    return GraphUtils.intU(toIdx(), type().bitWidth());
  }

  @Override
  public ForIdxNode copy() {
    return new ForIdxNode(type(), fromIdx, toIdx);
  }

  @Override
  public Node shallowCopy() {
    return new ForIdxNode(type(), fromIdx, toIdx);
  }

  @Override
  public <T extends GraphNodeVisitor> void accept(T visitor) {

  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(fromIdx);
    collection.add(toIdx);
  }
}
