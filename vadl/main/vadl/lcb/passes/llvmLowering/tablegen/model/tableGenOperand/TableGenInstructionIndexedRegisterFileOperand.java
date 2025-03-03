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
import vadl.viam.RegisterFile;
import vadl.viam.graph.dependency.FuncParamNode;
import vadl.viam.graph.dependency.ReadRegFileNode;
import vadl.viam.graph.dependency.WriteRegFileNode;

/**
 * Indicates that the operand is a {@link RegisterFile} when the address is *not*
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
  private final RegisterFile registerFile;

  /**
   * Constructor.
   */
  public TableGenInstructionIndexedRegisterFileOperand(ReadRegFileNode node,
                                                       FuncParamNode address) {
    super(node, new TableGenParameterTypeAndName(node.registerFile().simpleName(),
        address.parameter().identifier.simpleName()));
    this.registerFile = node.registerFile();
  }

  /**
   * Constructor.
   */
  public TableGenInstructionIndexedRegisterFileOperand(WriteRegFileNode node,
                                                       FuncParamNode address) {
    super(node, new TableGenParameterTypeAndName(node.registerFile().simpleName(),
        address.parameter().identifier.simpleName()));
    this.registerFile = node.registerFile();
  }

  public RegisterFile registerFile() {
    return registerFile;
  }
}
