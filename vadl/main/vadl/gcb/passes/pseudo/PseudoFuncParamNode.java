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

package vadl.gcb.passes.pseudo;

import java.util.List;
import vadl.javaannotations.viam.DataValue;
import vadl.viam.Parameter;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.FuncParamNode;

/**
 * This class is used when instruction operands have to be replaced by {@link FuncParamNode}.
 * In the example below we replace {@code rs1} by {@code rs}. The pseudo expander will also require
 * the index of {@code rs} when expanding the pseudo instruction.
 * <code>
 * pseudo instruction BGEZ( rs : Index, offset : Bits<12> ) =
 * {
 * BGE{ rs1 = rs, rs2 = 0 as Bits5, imm = offset }
 * }
 * </code>
 */
public class PseudoFuncParamNode extends FuncParamNode {
  /**
   * Index of the operand in the pseudo instruction.
   */
  @DataValue
  protected int index;

  /**
   * Constructs a FuncParamNode instance with a given parameter and type.
   * The node type and parameter type must be equal.
   */
  public PseudoFuncParamNode(Parameter parameter, int index) {
    super(parameter);
    this.index = index;
  }


  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(index);
  }

  public int index() {
    return this.index;
  }


  @Override
  public ExpressionNode copy() {
    return new PseudoFuncParamNode(parameter, index);
  }

  @Override
  public Node shallowCopy() {
    return copy();
  }
}
