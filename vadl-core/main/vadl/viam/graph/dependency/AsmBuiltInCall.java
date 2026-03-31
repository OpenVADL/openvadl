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
import vadl.types.BuiltInTable.BuiltIn;
import vadl.types.Type;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;

/**
 * Represents a call to a built-in function of the asm parser.
 * It holds a {@link BuiltIn} from the {@link vadl.types.BuiltInTable#ASM_PARSER_BUILT_INS}.
 *
 * @see vadl.types.BuiltInTable
 * @see AbstractFunctionCallNode
 */
public class AsmBuiltInCall extends AbstractFunctionCallNode {

  @DataValue
  protected BuiltIn asmBuiltIn;

  public AsmBuiltInCall(BuiltIn asmBuiltIn, NodeList<ExpressionNode> args, Type type) {
    super(args, type);
    this.asmBuiltIn = asmBuiltIn;
  }

  /**
   * Update the asm builtin by the given value.
   */
  public void setAsmBuiltIn(BuiltIn asmBuiltIn) {
    this.asmBuiltIn = asmBuiltIn;
  }

  /**
   * Gets the asm {@link BuiltIn}.
   */
  public BuiltIn asmBuiltIn() {
    return this.asmBuiltIn;
  }

  @Override
  public ExpressionNode copy() {
    return new AsmBuiltInCall(asmBuiltIn,
        new NodeList<>(this.arguments().stream().map(x -> (ExpressionNode) x.copy()).toList()),
        this.type());
  }

  @Override
  public Node shallowCopy() {
    return new AsmBuiltInCall(asmBuiltIn, args, type());
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(asmBuiltIn);
  }

  @Override
  public <T extends GraphNodeVisitor> void accept(T visitor) {
    visitor.visit(this);
  }
}
