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

import static vadl.viam.ViamError.ensure;

import vadl.error.Diagnostic;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmBasicBlockSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmFieldAccessRefNode;
import vadl.lcb.passes.llvmLowering.tablegen.model.ReferencesFormatField;
import vadl.lcb.passes.llvmLowering.tablegen.model.ReferencesImmediateOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenImmediateRecord;
import vadl.viam.Format;
import vadl.viam.graph.Node;

/**
 * TableGen operand which references a label.
 */
public class TableGenInstructionLabelOperand extends TableGenDefaultInstructionOperand
    implements ReferencesFormatField, ReferencesImmediateOperand {
  private static final String AS_LABEL = "AsLabel";

  private final TableGenImmediateRecord immediate;

  private TableGenInstructionLabelOperand(Node origin,
                                          TableGenImmediateRecord immediateRecord,
                                          Format.FieldAccess fieldAccess) {
    super(origin, immediateRecord.rawName() + AS_LABEL, fieldAccess.fieldRef().simpleName());
    this.immediate = immediateRecord;
  }

  /**
   * Constructor.
   */
  public TableGenInstructionLabelOperand(LlvmBasicBlockSD node) {
    this(node, node.immediateOperand(), node.fieldAccess());
  }

  /**
   * Constructor.
   */
  public TableGenInstructionLabelOperand(LlvmFieldAccessRefNode node) {
    this(node, node.immediateOperand(), node.fieldAccess());
    ensure(node.usage() == LlvmFieldAccessRefNode.Usage.BasicBlock,
        () -> Diagnostic.error(
            "Field reference has wrong type. It is expected to be basic block but it is not.",
            node.location()));
  }

  @Override
  public Format.Field formatField() {
    return immediate.fieldAccessRef().fieldRef();
  }

  @Override
  public TableGenImmediateRecord immediateOperand() {
    return immediate;
  }
}
