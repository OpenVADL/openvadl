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

package vadl.gcb.passes.operands.model;

import vadl.viam.Format;
import vadl.viam.RegisterTensor;
import vadl.viam.graph.dependency.ReadRegTensorNode;
import vadl.viam.graph.dependency.WriteRegTensorNode;

/**
 * Indicates that the operand is a register file when the address is a {@link Format.Field}.
 */
public final class GcbRegisterFileOperand extends GcbDefaultInstructionOperand {
  private final RegisterTensor registerFile;
  private final Format.Field formatField;

  /**
   * Constructor.
   */
  private GcbRegisterFileOperand(ReadRegTensorNode node, Format.Field address) {
    super(node);
    this.registerFile = node.regTensor();
    this.formatField = address;
  }

  /**
   * Constructor.
   */
  private GcbRegisterFileOperand(WriteRegTensorNode node, Format.Field address) {
    super(node);
    this.registerFile = node.regTensor();
    this.formatField = address;
  }

  public RegisterTensor registerFile() {
    return registerFile;
  }

  public Format.Field formatField() {
    return formatField;
  }
}
