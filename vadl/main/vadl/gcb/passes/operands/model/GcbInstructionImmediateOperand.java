// SPDX-FileCopyrightText : © 2025-2026 TU Wien <vadl@tuwien.ac.at>
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

import com.google.errorprone.annotations.concurrent.LazyInit;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenImmediateRecord;
import vadl.viam.Format;
import vadl.viam.graph.dependency.FieldAccessRefNode;

/**
 * Indicates that the operand is an immediate.
 */
public class GcbInstructionImmediateOperand extends GcbDefaultInstructionOperand {
  private final Format.FieldAccess fieldAccess;
  @LazyInit
  private TableGenImmediateRecord immediateOperand;

  /**
   * Constructor.
   */
  public GcbInstructionImmediateOperand(FieldAccessRefNode node) {
    super(node, "", node.fieldAccess().simpleName());
    this.fieldAccess = node.fieldAccess();
  }

  /**
   * Constructor.
   */
  public GcbInstructionImmediateOperand(FieldAccessRefNode origin,
                                        TableGenImmediateRecord immediateOperand, String type,
                                        String name) {
    super(origin, type, name);
    this.fieldAccess = origin.fieldAccess();
    this.immediateOperand = immediateOperand;
  }

  public Format.FieldAccess fieldAccess() {
    return fieldAccess;
  }

  public TableGenImmediateRecord immediateOperand() {
    return immediateOperand;
  }
}
