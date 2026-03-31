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

package vadl.cppCodeGen.model.nodes;


import java.util.List;
import vadl.cppCodeGen.CppCodeGenGraphNodeVisitor;
import vadl.javaannotations.viam.DataValue;
import vadl.javaannotations.viam.Input;
import vadl.types.Type;
import vadl.viam.Format;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * This node indicates that a value should be updated.
 * Note that the semantic of this node is that it returns not only the updated value but the
 * application of the slice.
 */
public class CppUpdateBitRangeNode extends ExpressionNode {
  // Destination value
  @Input
  public ExpressionNode value;

  // Source value
  @Input
  public ExpressionNode patch;

  // Contains the encoding to determine which bits of `value` should be overwritten by `patch`.
  @DataValue
  public Format.Field field;

  /**
   * Constructor.
   */
  public CppUpdateBitRangeNode(Type type,
                               ExpressionNode value,
                               ExpressionNode patch,
                               Format.Field field) {
    super(type);
    this.value = value;
    this.patch = patch;
    this.field = field;
  }

  @Override
  public ExpressionNode copy() {
    return new CppUpdateBitRangeNode(this.type(), value.copy(),
        patch.copy(), field);
  }

  @Override
  public Node shallowCopy() {
    return new CppUpdateBitRangeNode(this.type(), value, patch, field);
  }

  @Override
  public <T extends GraphNodeVisitor> void accept(T visitor) {
    ((CppCodeGenGraphNodeVisitor) visitor).visit(this);
  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    collection.add(value);
    collection.add(patch);
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(field);
  }

  @Override
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    value = visitor.apply(this, value, ExpressionNode.class);
    patch = visitor.apply(this, patch, ExpressionNode.class);
  }

}
