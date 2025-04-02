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
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * One-hot-decode node, representing a node packing a list of boolean inputs into an integer.
 */
public class OneHotDecodeNode extends ExpressionNode {

  @Input
  NodeList<ExpressionNode> values;

  public OneHotDecodeNode(List<ExpressionNode> values) {
    super(Type.unsignedInt(32 - Integer.numberOfLeadingZeros(values.size() - 1)));
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
    return new OneHotDecodeNode(values.copy());
  }

  @Override
  public Node shallowCopy() {
    return new OneHotDecodeNode(values);
  }

  @Override
  public <T extends GraphNodeVisitor> void accept(T visitor) {
    visitor.visit(this);
  }
}
