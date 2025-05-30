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
import java.util.stream.Collectors;
import vadl.javaannotations.viam.Input;
import vadl.types.Type;
import vadl.types.UIntType;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * One-hot-decode node, representing a node packing a list of boolean inputs into an integer.
 */
public class RtlOneHotDecodeNode extends ExpressionNode {

  @Input
  NodeList<ExpressionNode> values;

  /**
   * Create a new one-hot-decode node for a list of value inputs. The node's type is calculated
   * based on the input count ({@code UInt<n>} with {@code n} large enough to encode values).
   *
   * @param values value inputs (all bool)
   */
  public RtlOneHotDecodeNode(List<ExpressionNode> values) {
    super(UIntType.minimalTypeFor(values.size() - 1));
    ensure(values.stream().allMatch(value ->
        value.type().isTrivialCastTo(Type.bool())), "One-hot inputs must all be bool");
    this.values = new NodeList<>(values);
  }

  public NodeList<ExpressionNode> values() {
    return values;
  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    collection.addAll(values);
  }

  @Override
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    values = values.stream()
        .map((e) -> visitor.apply(this, e, ExpressionNode.class))
        .collect(Collectors.toCollection(NodeList::new));
  }

  @Override
  public ExpressionNode copy() {
    return new RtlOneHotDecodeNode(values.copy());
  }

  @Override
  public Node shallowCopy() {
    return new RtlOneHotDecodeNode(values);
  }

  @Override
  public <T extends GraphNodeVisitor> void accept(T visitor) {
    visitor.visit(this);
  }
}
