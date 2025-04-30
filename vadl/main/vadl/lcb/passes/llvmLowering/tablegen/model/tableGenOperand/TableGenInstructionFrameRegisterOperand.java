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

import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmFrameIndexSD;
import vadl.viam.Format;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.FuncParamNode;

/**
 * Indicates that the operand is a register which is the frame pointer.
 */
public class TableGenInstructionFrameRegisterOperand
    extends TableGenDefaultInstructionOperand {

  /**
   * Constructor.
   */
  public TableGenInstructionFrameRegisterOperand(Node node,
                                                 Format.Field address) {
    // Note that `node` has the type `Node` and not `LlvmFrameIndex` because
    // the machine pattern requires that the node remains a register class file operand.
    super(node, LlvmFrameIndexSD.NAME, address.identifier.simpleName());
  }

  /**
   * Constructor.
   */
  public TableGenInstructionFrameRegisterOperand(Node node,
                                                 FieldRefNode address) {
    this(node, address.formatField());
  }

  /**
   * Constructor.
   */
  public TableGenInstructionFrameRegisterOperand(Node node,
                                                 FuncParamNode address) {
    // Note that `node` has the type `Node` and not `LlvmFrameIndex` because
    // the machine pattern requires that the node remains a register class file operand.
    super(node, LlvmFrameIndexSD.NAME, address.parameter().identifier.simpleName());
  }
}
