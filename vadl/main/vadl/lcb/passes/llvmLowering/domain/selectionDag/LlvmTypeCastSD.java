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

package vadl.lcb.passes.llvmLowering.domain.selectionDag;

import java.util.List;
import vadl.javaannotations.viam.DataValue;
import vadl.lcb.codegen.model.llvm.ValueType;
import vadl.lcb.passes.llvmLowering.LlvmNodeLowerable;
import vadl.lcb.passes.llvmLowering.strategies.visitors.TableGenMachineInstructionVisitor;
import vadl.lcb.passes.llvmLowering.strategies.visitors.TableGenNodeVisitor;
import vadl.types.Type;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.UnaryNode;

/**
 * LLVM's node for type casts.
 */
public class LlvmTypeCastSD extends UnaryNode implements LlvmNodeLowerable {
  @DataValue
  private ValueType valueType;

  public LlvmTypeCastSD(ExpressionNode value, Type type) {
    super(value, type);
    this.valueType = ValueType.from(type).get();
  }

  @Override
  public ExpressionNode copy() {
    return new LlvmTypeCastSD((ExpressionNode) value.copy(), type());
  }

  @Override
  public Node shallowCopy() {
    return new LlvmTypeCastSD(value, type());
  }

  @Override
  public <T extends GraphNodeVisitor> void accept(T visitor) {
    if (visitor instanceof TableGenMachineInstructionVisitor v) {
      v.visit(this);
    } else if (visitor instanceof TableGenNodeVisitor v) {
      v.visit(this);
    } else {
      visitor.visit(this);
    }
  }

  @Override
  public String lower() {
    return valueType.getLlvmType();
  }


  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(valueType);
  }
}
