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

package vadl.lcb.passes.operands;

import java.util.List;
import java.util.Objects;
import vadl.gcb.passes.operands.ReferencesFormatField;
import vadl.gcb.passes.operands.model.GcbDefaultInstructionOperand;
import vadl.gcb.passes.operands.model.GcbInstructionImmediateOperand;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmFieldAccessRefNode;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenImmediateRecord;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.ReferencesImmediateOperand;
import vadl.viam.Format;

/**
 * Indicates that the operand is an immediate. This is a lowered variant
 * of {@link GcbInstructionImmediateOperand} because we need to reference
 * {@link TableGenImmediateRecord}. However, we can only do that after the LLVM lowering.
 */
public class TableGenInstructionImmediateOperand extends GcbInstructionImmediateOperand
    implements ReferencesFormatField, ReferencesImmediateOperand {
  private final TableGenImmediateRecord immediateOperand;

  /**
   * Constructor.
   */
  public TableGenInstructionImmediateOperand(LlvmFieldAccessRefNode node) {
    this(node.fieldAccess().identifier.simpleName(), node);
  }

  /**
   * Constructor.
   */
  public TableGenInstructionImmediateOperand(String variableName, LlvmFieldAccessRefNode node) {
    super(node, node.immediateOperand().fullname(), variableName);
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
  public List<Format.Field> formatFields() {
    return immediateOperand.fieldAccessRef().fieldRefs();
  }
}
