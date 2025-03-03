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

import vadl.lcb.codegen.model.llvm.ValueType;
import vadl.lcb.passes.llvmLowering.strategies.visitors.TableGenMachineInstructionVisitor;
import vadl.lcb.passes.llvmLowering.strategies.visitors.TableGenNodeVisitor;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenImmediateRecord;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.tableGenParameter.TableGenParameter;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.tableGenParameter.TableGenParameterTypeAndName;
import vadl.types.Type;
import vadl.viam.Format;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.FieldAccessRefNode;

/**
 * This class represents a field access in LLVM. It extends {@link FieldAccessRefNode} because
 * it requires additional information for rendering an immediate.
 */
public class LlvmFieldAccessRefNode extends FieldAccessRefNode {
  private final ValueType llvmType;
  private final TableGenImmediateRecord immediateOperand;
  protected final TableGenParameter parameter;
  private final Usage usage;

  /**
   * Indicates how the field is used. It is {@code Immediate} when the field is directly as
   * immediate. However, it is {@code BasicBlock} if the field is a symbol to a basic block.
   * This indicates to TableGen that the type is {@code OtherVT}.
   */
  public enum Usage {
    Immediate,
    BasicBlock
  }

  /**
   * Creates an {@link LlvmFieldAccessRefNode} object that holds a reference to a format field
   * access.
   *
   * @param fieldAccess  the format immediate to be referenced
   * @param originalType of the node. This type might not be correctly sized because vadl allows
   *                     arbitrary bit sizes.
   * @param llvmType     is same as {@code originalType} when it is a valid LLVM type. Otherwise,
   *                     it is the next upcasted type.
   * @param usage        indicates how the field is used.
   */
  public LlvmFieldAccessRefNode(Format.FieldAccess fieldAccess,
                                Type originalType,
                                ValueType llvmType,
                                Usage usage) {
    super(fieldAccess, originalType);
    this.immediateOperand =
        new TableGenImmediateRecord(fieldAccess, llvmType);
    this.parameter = usage == Usage.Immediate
        ? new TableGenParameterTypeAndName(immediateOperand.fullname(),
        fieldAccess.fieldRef().identifier.simpleName()) :
        new TableGenParameterTypeAndName(immediateOperand.rawName() + TableGenParameter.AS_LABEL,
            fieldAccess.fieldRef().identifier.simpleName());
    this.llvmType = llvmType;
    this.usage = usage;
  }

  @Override
  public ExpressionNode copy() {
    return new LlvmFieldAccessRefNode(fieldAccess, type(), llvmType, usage);
  }

  @Override
  public Node shallowCopy() {
    return new LlvmFieldAccessRefNode(fieldAccess, type(), llvmType, usage);
  }

  public TableGenImmediateRecord immediateOperand() {
    return immediateOperand;
  }

  public Usage usage() {
    return usage;
  }

  public ValueType llvmType() {
    return llvmType;
  }

  @Override
  public void accept(GraphNodeVisitor visitor) {
    if (visitor instanceof TableGenMachineInstructionVisitor v) {
      v.visit(this);
    } else if (visitor instanceof TableGenNodeVisitor v) {
      v.visit(this);
    } else {
      visitor.visit(this);
    }
  }
}
