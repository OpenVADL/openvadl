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

package vadl.lcb.passes.llvmLowering.domain.machineDag;

import java.util.List;
import vadl.javaannotations.viam.DataValue;
import vadl.lcb.codegen.model.llvm.ValueType;
import vadl.lcb.passes.llvmLowering.strategies.visitors.TableGenMachineInstructionVisitor;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenPattern;
import vadl.types.Type;
import vadl.viam.Constant;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * Nodes in the machine graph for {@link TableGenPattern}. This is special because
 * it represents only a value.
 */
public class LcbMachineInstructionValueNode extends ExpressionNode {
  @DataValue
  private ValueType valueType;
  @DataValue
  private Constant constant;

  /**
   * Constructor.
   */
  public LcbMachineInstructionValueNode(ValueType valueType, Constant constant) {
    super(Type.dummy());
    this.valueType = valueType;
    this.constant = constant;
  }

  public ValueType valueType() {
    return valueType;
  }

  public Constant constant() {
    return constant;
  }

  @Override
  public ExpressionNode copy() {
    return new LcbMachineInstructionValueNode(valueType, constant);
  }

  @Override
  public Node shallowCopy() {
    return new LcbMachineInstructionValueNode(valueType, constant);
  }

  @Override
  public void accept(GraphNodeVisitor visitor) {
    ((TableGenMachineInstructionVisitor) visitor).visit(this);
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(valueType);
    collection.add(constant);
  }
}
