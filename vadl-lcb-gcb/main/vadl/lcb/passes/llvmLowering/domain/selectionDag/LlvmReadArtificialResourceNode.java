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

import vadl.gcb.passes.operands.model.GcbDefaultInstructionOperand;
import vadl.gcb.passes.operands.model.GcbInstructionConcreteRegisterOperand;
import vadl.gcb.passes.operands.model.GcbInstructionIndexedRegisterFileOperand;
import vadl.gcb.passes.operands.model.GcbInstructionOperand;
import vadl.gcb.passes.operands.model.GcbInstructionRegisterFileOperand;
import vadl.lcb.passes.llvmLowering.LlvmNodeLowerable;
import vadl.lcb.passes.llvmLowering.strategies.visitors.TableGenMachineInstructionVisitor;
import vadl.lcb.passes.llvmLowering.strategies.visitors.TableGenNodeVisitor;
import vadl.types.DataType;
import vadl.viam.ArtificialResource;
import vadl.viam.RegisterTensor;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.FuncParamNode;
import vadl.viam.graph.dependency.ReadArtificialResNode;

/**
 * LLVM node for the selection dag.
 */
public class LlvmReadArtificialResourceNode extends ReadArtificialResNode
    implements LlvmNodeLowerable,
    LlvmNodeReplaceable {
  protected final ArtificialResource artificialResource;
  protected GcbDefaultInstructionOperand instructionOperand;

  /**
   * Constructor.
   */
  public LlvmReadArtificialResourceNode(ArtificialResource artificialResource,
                                        ExpressionNode address,
                                        DataType type) {
    super(artificialResource,
        new NodeList<>(address),
        type);
    this.artificialResource = artificialResource;
    if (artificialResource.readFunction().parameters().length == 0) {
      // It is a register
      instructionOperand =
          new GcbInstructionIndexedRegisterFileOperand(this, (FuncParamNode) address);
    } else if (address instanceof ConstantNode constantNode) {
      instructionOperand = new GcbInstructionConcreteRegisterOperand(
          (RegisterTensor) artificialResource.innerResourceRef(),
          constantNode.constant().asVal().intValue(),
          this);
    } else {
      instructionOperand =
          new GcbInstructionRegisterFileOperand(this, ((FieldRefNode) address).formatField());
    }
  }

  @Override
  public GcbInstructionOperand operand() {
    return instructionOperand;
  }

  @Override
  public LlvmReadArtificialResourceNode copy() {
    return new LlvmReadArtificialResourceNode(artificialResource, address().copy(), type());
  }

  @Override
  public LlvmReadArtificialResourceNode shallowCopy() {
    return new LlvmReadArtificialResourceNode(artificialResource, address(), type());
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
