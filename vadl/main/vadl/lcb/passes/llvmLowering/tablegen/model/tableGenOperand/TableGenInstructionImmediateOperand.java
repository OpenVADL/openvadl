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

import java.util.Objects;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmFieldAccessRefNode;
import vadl.lcb.passes.llvmLowering.tablegen.model.ReferencesFormatField;
import vadl.lcb.passes.llvmLowering.tablegen.model.ReferencesImmediateOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenImmediateRecord;
import vadl.viam.Format;

/**
 * Indicates that the operand is an immediate.
 */
public class TableGenInstructionImmediateOperand extends TableGenDefaultInstructionOperand
    implements ReferencesFormatField, ReferencesImmediateOperand {
  private final TableGenImmediateRecord immediateOperand;

  /**
   * Constructor.
   */
  public TableGenInstructionImmediateOperand(LlvmFieldAccessRefNode node) {
    super(node, node.immediateOperand().fullname(),
        node.fieldAccess().fieldRef().identifier.simpleName());
    this.immediateOperand = node.immediateOperand();
  }

  @Override
  public TableGenImmediateRecord immediateOperand() {
    return immediateOperand;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!immediateOperand.equals(((TableGenInstructionImmediateOperand) o).immediateOperand)) {
      return false;
    }
    TableGenInstructionImmediateOperand that = (TableGenInstructionImmediateOperand) o;
    return Objects.equals(immediateOperand, that.immediateOperand);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), immediateOperand);
  }

  @Override
  public Format.Field formatField() {
    return immediateOperand.fieldAccessRef().fieldRef();
  }
}
