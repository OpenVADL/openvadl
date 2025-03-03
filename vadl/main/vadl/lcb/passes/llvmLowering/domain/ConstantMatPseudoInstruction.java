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

package vadl.lcb.passes.llvmLowering.domain;

import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenImmediateRecord;
import vadl.viam.Assembly;
import vadl.viam.Identifier;
import vadl.viam.Parameter;
import vadl.viam.PseudoInstruction;
import vadl.viam.graph.Graph;

/**
 * A {@link ConstantMatPseudoInstruction} is like a {@link PseudoInstruction} except it is not
 * part of the assembler.
 */
public class ConstantMatPseudoInstruction extends PseudoInstruction {

  private final TableGenImmediateRecord immediateRecord;

  /**
   * Instantiates a PseudoInstruction object and verifies it.
   *
   * @param identifier     the identifier of the pseudo instruction
   * @param parameters     the list of parameters for the pseudo instruction
   * @param behavior       the behavior graph of the pseudo instruction
   */
  public ConstantMatPseudoInstruction(Identifier identifier,
                                      Parameter[] parameters,
                                      Graph behavior,
                                      Assembly assembly,
                                      TableGenImmediateRecord imm) {
    super(identifier, parameters, behavior, assembly);
    this.immediateRecord = imm;
  }

  public TableGenImmediateRecord immediateRecord() {
    return immediateRecord;
  }
}
