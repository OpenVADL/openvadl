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

package vadl.iss.passes.nodes;

import java.util.List;
import vadl.iss.passes.opDecomposition.nodes.IssExprNode;
import vadl.javaannotations.viam.Input;
import vadl.types.DataType;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * A node that acts as type information transport between the input and the user.
 * The {@link vadl.iss.passes.IssExtractOptimizationPass} removes all extracts that
 * are not required for correctness when operating on the TCG target size.
 * However, the {@code vadl_builtins.h} library requires correct input width for each argument.
 * Let's consider the example {@code (f as Bits<64>) << 10} where f is a format field
 * {@code f: Bits<16>}.
 * The optimization pass would remove the zero extension because it is unnecessary to compute
 * the shift result correctly.
 * However, when calling the shift C built-in: {@code VADL_lsl(f, 16, 10, 5)} we can see
 * that it takes 16 as argument width of f. This causes a truncated value of the result to
 * 16 bits, which is wrong as it was zero extended 64 bit in the VADL source code.
 *
 * <p>To fix this, the optimization pass replaces the zero extend by this ghost cast,
 * which is ignored by the TCG lowerer, but provides the correct type for the C built-in.
 * So in the upper example, this cast would have the type {@code Bits<64>}.</p>
 */
public class IssGhostCastNode extends IssExprNode {

  @Input
  private ExpressionNode value;

  public IssGhostCastNode(ExpressionNode value, DataType castType) {
    super(castType);
    this.value = value;
  }

  public ExpressionNode value() {
    return value;
  }

  @Override
  public DataType type() {
    return (DataType) super.type();
  }

  @Override
  public ExpressionNode copy() {
    return new IssGhostCastNode(value.copy(), type());
  }

  @Override
  public Node shallowCopy() {
    return new IssGhostCastNode(value, type());
  }

  @Override
  public <T extends GraphNodeVisitor> void accept(T visitor) {

  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    collection.add(value);
  }

  @Override
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    value = visitor.apply(this, value, ExpressionNode.class);
  }
}
