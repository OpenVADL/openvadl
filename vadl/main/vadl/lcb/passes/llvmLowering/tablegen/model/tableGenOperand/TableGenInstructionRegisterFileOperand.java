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

import vadl.lcb.passes.llvmLowering.tablegen.model.ReferencesFormatField;
import vadl.viam.Format;
import vadl.viam.RegisterTensor;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.FuncParamNode;
import vadl.viam.graph.dependency.ReadRegTensorNode;
import vadl.viam.graph.dependency.WriteRegTensorNode;

/**
 * Indicates that the operand is a register file when the address is a {@link Format.Field}.
 */
public class TableGenInstructionRegisterFileOperand
    extends TableGenDefaultInstructionOperand
    implements ReferencesFormatField {
  private final RegisterTensor registerFile;
  private final Format.Field formatField;

  /**
   * Constructor.
   */
  public TableGenInstructionRegisterFileOperand(ReadRegTensorNode node, Format.Field address) {
    super(node, node.regTensor().simpleName(), address.identifier.simpleName());
    this.registerFile = node.regTensor();
    this.registerFile.ensure(registerFile.isRegisterFile(), "must be registerfile");
    this.formatField = address;
    node.regTensor().ensure(registerFile.isRegisterFile(), "must be registerfile");
  }

  /**
   * Constructor.
   */
  public TableGenInstructionRegisterFileOperand(ReadRegTensorNode node, FieldRefNode address) {
    this(node, address.formatField());
  }

  /**
   * Constructor.
   */
  public TableGenInstructionRegisterFileOperand(WriteRegTensorNode node, FieldRefNode address) {
    super(node, node.regTensor().simpleName(), address.formatField().identifier.simpleName());
    this.registerFile = node.regTensor();
    this.registerFile.ensure(registerFile.isRegisterFile(), "must be registerfile");
    this.formatField = address.formatField();
  }

  /**
   * Constructor for pseudo instructions. Pseudo instructions will have a {@link ReadRegTensorNode}
   * or {@link WriteRegTensorNode} because they are constructed over {@link FuncParamNode}.
   */
  public TableGenInstructionRegisterFileOperand(RegisterTensor registerFile,
                                                Format.Field field,
                                                FuncParamNode funcParamNode) {
    super(funcParamNode, registerFile.simpleName(),
        funcParamNode.parameter().identifier.simpleName());
    this.registerFile = registerFile;
    this.formatField = field;
    this.registerFile.ensure(registerFile.isRegisterFile(), "must be registerfile");
  }

  public RegisterTensor registerFile() {
    return registerFile;
  }

  @Override
  public Format.Field formatField() {
    return formatField;
  }
}
