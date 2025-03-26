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

package vadl.lcb.passes.llvmLowering.tablegen.model;

import java.util.Collections;
import java.util.List;
import vadl.lcb.passes.llvmLowering.LlvmLoweringPass;
import vadl.lcb.passes.llvmLowering.domain.RegisterRef;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.TableGenInstructionOperand;
import vadl.viam.CompilerInstruction;
import vadl.viam.PseudoInstruction;

/**
 * Represents a record in tablegen for {@link CompilerInstruction}.
 */
public class TableGenCompilerInstruction extends TableGenInstruction {
  private final CompilerInstruction compilerInstruction;

  public TableGenCompilerInstruction(
      CompilerInstruction compilerInstruction,
      String name,
      String namespace,
      LlvmLoweringPass.Flags flags,
      List<TableGenInstructionOperand> inOperands,
      List<TableGenInstructionOperand> outOperands,
      List<RegisterRef> uses,
      List<RegisterRef> defs) {
    super(name, namespace, flags, inOperands, outOperands, uses, defs, Collections.emptyList());
    this.compilerInstruction = compilerInstruction;
  }

  public CompilerInstruction compilerInstruction() {
    return compilerInstruction;
  }
}
