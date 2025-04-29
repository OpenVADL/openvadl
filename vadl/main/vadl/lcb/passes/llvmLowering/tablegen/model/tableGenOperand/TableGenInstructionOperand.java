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

import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstruction;
import vadl.viam.graph.Node;

/**
 * An {@link TableGenInstruction} has list of operands for inputs and outputs.
 * This class represent one element of the inputs or outputs.
 * A parameter represents {@code X:$rs1} in
 * <pre>
 * {@code
 * def : Pat<(xor X:$rs1, RV3264I_Itype_immAsInt64:$imm),
 * (XORI X:$rs1, RV3264I_Itype_immAsInt64:$imm)>;
 * }</pre>
 */
public abstract class TableGenInstructionOperand implements InstructionOperandPrintable {
  protected final Node origin;

  public TableGenInstructionOperand(Node origin) {
    this.origin = origin;
  }

  public Node origin() {
    return origin;
  }
}
