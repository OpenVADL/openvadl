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
import vadl.lcb.passes.llvmLowering.LlvmNodeLowerable;
import vadl.lcb.passes.llvmLowering.strategies.visitors.TableGenMachineInstructionVisitor;
import vadl.lcb.passes.llvmLowering.strategies.visitors.TableGenNodeVisitor;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenImmediateRecord;
import vadl.types.Type;
import vadl.viam.Format;
import vadl.viam.PrintableInstruction;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.FieldAccessRefNode;

/**
 * LLVM node which represents the basic block as selection dag node.
 */
public class LlvmBasicBlockSD extends FieldAccessRefNode implements LlvmNodeLowerable {
  private final PrintableInstruction instruction;
  private final ValueType llvmType;
  private final TableGenImmediateRecord immediateOperand;


  /**
   * Creates an {@link LlvmBasicBlockSD} object that holds a reference to a format field
   * access. But in the selection dag, the immediate is a reference to a basic block.
   *
   * @param instruction  which scopes the {@link vadl.viam.Format.FieldAccess}.
   * @param fieldAccess  the format immediate to be referenced
   * @param originalType of the node. This type might not be correctly sized because vadl allows
   *                     arbitrary bit sizes.
   * @param llvmType     is same as {@code originalType} when it is a valid LLVM type. Otherwise,
   *                     it is the next upcasted type.
   */
  public LlvmBasicBlockSD(PrintableInstruction instruction,
                          Format.FieldAccess fieldAccess,
                          Type originalType,
                          ValueType llvmType) {
    super(fieldAccess, originalType);
    this.instruction = instruction;
    this.immediateOperand =
        new TableGenImmediateRecord(instruction, fieldAccess, llvmType);
    this.llvmType = llvmType;
  }

  public TableGenImmediateRecord immediateOperand() {
    return immediateOperand;
  }

  @Override
  public ExpressionNode copy() {
    return new LlvmBasicBlockSD(instruction, fieldAccess, type(), llvmType);
  }

  @Override
  public Node shallowCopy() {
    return new LlvmBasicBlockSD(instruction, fieldAccess, type(), llvmType);
  }

  @Override
  public void accept(GraphNodeVisitor visitor) {
    if (visitor instanceof TableGenMachineInstructionVisitor v) {
      v.visit(this);
    }
    if (visitor instanceof TableGenNodeVisitor v) {
      v.visit(this);
    }
  }

  @Override
  public String lower() {
    return "bb:$" + immediateOperand.fieldAccessRef().fieldRef().simpleName();
  }
}
