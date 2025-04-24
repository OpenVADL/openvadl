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

package vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand;

import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.tableGenParameter.TableGenParameterTypeAndName;
import vadl.viam.Format;
import vadl.viam.RegisterTensor;
import vadl.viam.graph.dependency.FuncParamNode;
import vadl.viam.graph.dependency.ReadRegTensorNode;
import vadl.viam.graph.dependency.WriteRegTensorNode;

/**
 * Indicates that the operand is a registerFile when the address is *not*
 * a {@link Format.Field} but is indexed by a function. This is useful when we have to generate
 * tablegen instruction operands from operands.
 * In the example below we have {@code rd} and {@code rs1} which are both indexes and have no
 * {@link Format.Field}.
 * <code>
 * pseudo instruction MOV( rd : Index, rs1 : Index ) =
 * {
 * ADDI{ rd = rd, rs1 = rs1, imm = 0 as Bits12 }
 * }
 * </code>
 */
public class TableGenInstructionIndexedRegisterFileOperand extends TableGenInstructionOperand {
  private final RegisterTensor registerFile;

  /**
   * Constructor.
   */
  public TableGenInstructionIndexedRegisterFileOperand(ReadRegTensorNode node,
                                                       FuncParamNode address) {
    super(node, new TableGenParameterTypeAndName(node.regTensor().simpleName(),
        address.parameter().identifier.simpleName()));
    this.registerFile = node.regTensor();
    node.regTensor().ensure(node.regTensor().isRegisterFile(), "must be a register file");
  }

  /**
   * Constructor.
   */
  public TableGenInstructionIndexedRegisterFileOperand(WriteRegTensorNode node,
                                                       FuncParamNode address) {
    super(node, new TableGenParameterTypeAndName(node.regTensor().simpleName(),
        address.parameter().identifier.simpleName()));
    this.registerFile = node.regTensor();
    node.regTensor().ensure(node.regTensor().isRegisterFile(), "must be a register file");
  }

  public RegisterTensor registerFile() {
    return registerFile;
  }
}
