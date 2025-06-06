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

import javax.annotation.Nullable;
import vadl.error.Diagnostic;
import vadl.lcb.passes.llvmLowering.LlvmNodeLowerable;
import vadl.lcb.passes.llvmLowering.strategies.visitors.TableGenMachineInstructionVisitor;
import vadl.lcb.passes.llvmLowering.strategies.visitors.TableGenNodeVisitor;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.TableGenDefaultInstructionOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.TableGenInstructionIndexedRegisterFileOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.TableGenInstructionOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.TableGenInstructionRegisterFileOperand;
import vadl.types.DataType;
import vadl.viam.Counter;
import vadl.viam.RegisterTensor;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.FuncParamNode;
import vadl.viam.graph.dependency.ReadRegTensorNode;

/**
 * LLVM node for the selection dag.
 */
public class LlvmReadRegFileNode extends ReadRegTensorNode implements LlvmNodeLowerable,
    LlvmNodeReplaceable {
  protected TableGenDefaultInstructionOperand instructionOperand;

  /**
   * Constructor.
   */
  public LlvmReadRegFileNode(RegisterTensor registerFile,
                             FieldRefNode address,
                             DataType type,
                             @Nullable Counter staticCounterAccess) {
    super(registerFile, new NodeList<>(address), type, staticCounterAccess);
    instructionOperand = new TableGenInstructionRegisterFileOperand(this, address);
  }

  /**
   * Constructor.
   */
  public LlvmReadRegFileNode(RegisterTensor registerFile,
                             ExpressionNode address,
                             DataType type,
                             @Nullable Counter staticCounterAccess) {
    super(registerFile, new NodeList<>(address), type, staticCounterAccess);
    if (address instanceof FieldRefNode fieldRefNode) {
      instructionOperand = new TableGenInstructionRegisterFileOperand(this, fieldRefNode);
    } else if (address instanceof FuncParamNode funcParamNode) {
      instructionOperand = new TableGenInstructionIndexedRegisterFileOperand(this, funcParamNode);
    } else {
      throw Diagnostic.error("Cannot construct a tablegen operand from this", address.location())
          .build();
    }
  }

  @Override
  public TableGenInstructionOperand operand() {
    return instructionOperand;
  }

  @Override
  public LlvmReadRegFileNode copy() {
    return new LlvmReadRegFileNode(regTensor, address().copy(), type(),
        staticCounterAccess());
  }

  @Override
  public LlvmReadRegFileNode shallowCopy() {
    return new LlvmReadRegFileNode(regTensor, address(), type(), staticCounterAccess());
  }

  @Override
  public String lower() {
    return operand().render();
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
