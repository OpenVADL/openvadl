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

import java.util.List;
import vadl.gcb.passes.operands.ReferencesFormatField;
import vadl.viam.Format;
import vadl.viam.GeneratesRegisterFileName;
import vadl.viam.RegisterTensor;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.ReadArtificialResNode;
import vadl.viam.graph.dependency.ReadRegTensorNode;
import vadl.viam.graph.dependency.WriteArtificialResNode;
import vadl.viam.graph.dependency.WriteRegTensorNode;

/**
 * Indicates that the operand is a register file when the address is a {@link Format.Field}.
 */
public class GcbInstructionRegisterFileOperand
    extends GcbDefaultInstructionOperand
    implements ReferencesFormatField {
  private final GeneratesRegisterFileName registerFile;
  private final Format.Field formatField;

  /**
   * Constructor.
   */
  public GcbInstructionRegisterFileOperand(ReadRegTensorNode node, Format.Field address) {
    super(node, node.regTensor().simpleName(), address.identifier.simpleName());
    this.registerFile = node.regTensor();
    this.formatField = address;
    node.regTensor().ensure(registerFile.isRegisterFile(), "must be registerfile");
  }

  /**
   * Constructor.
   */
  public GcbInstructionRegisterFileOperand(ReadArtificialResNode node, Format.Field address) {
    super(node, node.resourceDefinition().simpleName(),
        address.identifier.simpleName());
    this.registerFile = node.resourceDefinition();
    this.formatField = address;
    node.resourceDefinition().innerResourceRef()
        .ensure(registerFile.isRegisterFile(), "must be registerfile");
  }

  /**
   * Constructor.
   */
  public GcbInstructionRegisterFileOperand(WriteRegTensorNode node, FieldRefNode address) {
    super(node, node.regTensor().simpleName(), address.formatField().identifier.simpleName());
    this.registerFile = node.regTensor();
    this.formatField = address.formatField();
  }

  /**
   * Constructor.
   */
  public GcbInstructionRegisterFileOperand(WriteArtificialResNode node, Format.Field address) {
    super(node, node.resourceDefinition().simpleName(),
        address.identifier.simpleName());
    this.registerFile = node.resourceDefinition();
    this.formatField = address;
    node.resourceDefinition().innerResourceRef()
        .ensure(registerFile.isRegisterFile(), "must be registerfile");
  }

  public GeneratesRegisterFileName registerFile() {
    return registerFile;
  }

  @Override
  public List<Format.Field> formatFields() {
    return List.of(formatField);
  }

  public Format.Field formatField() {
    return formatField;
  }
}
