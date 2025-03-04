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

import vadl.viam.Assembly;
import vadl.viam.Instruction;
import vadl.viam.PseudoInstruction;
import vadl.viam.graph.Graph;

/**
 * Defines an instruction alias in LLVM. This is used for {@link PseudoInstruction} when they
 * only have one {@link Instruction}.
 */
public class TableGenInstAlias {
  private final PseudoInstruction pseudoInstruction;
  private final Assembly assembly;
  private final Graph output;

  /**
   * Constructor.
   */
  public TableGenInstAlias(PseudoInstruction pseudoInstruction, Assembly assembly, Graph output) {
    this.pseudoInstruction = pseudoInstruction;
    this.assembly = assembly;
    this.output = output;
  }

  public Assembly assembly() {
    return assembly;
  }

  public Graph output() {
    return output;
  }

  public PseudoInstruction pseudoInstruction() {
    return pseudoInstruction;
  }
}
