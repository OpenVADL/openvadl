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
import vadl.iss.passes.tcgLowering.TcgExtend;
import vadl.javaannotations.viam.DataValue;
import vadl.javaannotations.viam.Input;
import vadl.types.Type;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * The expression node equivalent to {@link vadl.iss.passes.tcgLowering.nodes.TcgExtractNode}.
 */
public class IssValExtractNode extends IssExprNode {

  @Input
  private ExpressionNode value;
  @Input
  private ExpressionNode ofs;
  @Input
  private ExpressionNode len;

  @DataValue
  private final TcgExtend extendMode;

  /**
   * Constructs the extract node.
   *
   * @param extendMode sign or zero extending
   * @param value      the value to extract
   * @param ofs        the initial offset of extraction
   * @param len        how many bits should be extracted
   * @param type       the result type
   */
  public IssValExtractNode(TcgExtend extendMode, ExpressionNode value, ExpressionNode ofs,
                           ExpressionNode len,
                           Type type) {
    super(type);
    this.extendMode = extendMode;
    this.len = len;
    this.ofs = ofs;
    this.value = value;
  }

  public ExpressionNode value() {
    return value;
  }

  public ExpressionNode ofs() {
    return ofs;
  }

  public ExpressionNode len() {
    return len;
  }

  public TcgExtend extendMode() {
    return extendMode;
  }

  @Override
  public ExpressionNode copy() {
    return new IssValExtractNode(extendMode, value.copy(), ofs.copy(), len.copy(), type());
  }

  @Override
  public Node shallowCopy() {
    return new IssValExtractNode(extendMode, value, ofs, len, type());
  }

  @Override
  public <T extends GraphNodeVisitor> void accept(T visitor) {

  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(extendMode);
  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    collection.add(value);
    collection.add(ofs);
    collection.add(len);
  }

  @Override
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    value = visitor.apply(this, value, ExpressionNode.class);
    ofs = visitor.apply(this, ofs, ExpressionNode.class);
    len = visitor.apply(this, len, ExpressionNode.class);
  }

}
