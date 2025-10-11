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

package vadl.rtl.ipg.nodes;

import java.util.List;
import vadl.javaannotations.viam.DataValue;
import vadl.types.Type;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.ReadResourceNode;
import vadl.viam.graph.dependency.WriteResourceNode;

/**
 * Represents a stage signal that checks if a memory read/write is finished and the result is valid.
 */
public class RtlValidSignalNode extends ExpressionNode {

  @DataValue
  RtlConditionalMemNode validNode;

  public RtlValidSignalNode(RtlConditionalMemNode validNode) {
    super(Type.bool());
    this.validNode = validNode;
  }

  public RtlConditionalMemNode validNode() {
    return validNode;
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(validNode);
  }

  @Override
  public ExpressionNode copy() {
    return new RtlValidSignalNode(validNode);
  }

  @Override
  public Node shallowCopy() {
    return new RtlValidSignalNode(validNode);
  }

  @Override
  public <T extends GraphNodeVisitor> void accept(T visitor) {
    visitor.visit(this);
  }
}
