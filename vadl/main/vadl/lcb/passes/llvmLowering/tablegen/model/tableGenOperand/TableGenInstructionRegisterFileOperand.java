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
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.tableGenParameter.TableGenParameterTypeAndName;
import vadl.viam.Format;
import vadl.viam.RegisterFile;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.ReadRegFileNode;
import vadl.viam.graph.dependency.WriteRegFileNode;

/**
 * Indicates that the operand is a {@link RegisterFile} when the address is a {@link Format.Field}.
 */
public class TableGenInstructionRegisterFileOperand extends TableGenInstructionOperand
    implements ReferencesFormatField {
  private final RegisterFile registerFile;
  private final Format.Field formatField;
  private final Node reference;

  /**
   * Constructor.
   */
  public TableGenInstructionRegisterFileOperand(ReadRegFileNode node, FieldRefNode address) {
    super(node, new TableGenParameterTypeAndName(node.registerFile().simpleName(),
        address.formatField().identifier.simpleName()));
    this.registerFile = node.registerFile();
    this.formatField = address.formatField();
    this.reference = node;
  }

  /**
   * Constructor.
   */
  public TableGenInstructionRegisterFileOperand(WriteRegFileNode node, FieldRefNode address) {
    super(node, new TableGenParameterTypeAndName(node.registerFile().simpleName(),
        address.formatField().identifier.simpleName()));
    this.registerFile = node.registerFile();
    this.formatField = address.formatField();
    this.reference = node;
  }

  public RegisterFile registerFile() {
    return registerFile;
  }

  @Override
  public Format.Field formatField() {
    return formatField;
  }

  public Node reference() {
    return reference;
  }
}
