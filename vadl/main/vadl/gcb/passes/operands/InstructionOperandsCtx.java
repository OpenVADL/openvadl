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

package vadl.gcb.passes.operands;

import java.util.ArrayList;
import java.util.List;
import vadl.gcb.passes.operands.model.GcbInstructionOperand;
import vadl.viam.Definition;
import vadl.viam.DefinitionExtension;
import vadl.viam.Instruction;

/**
 * An extension for {@link Instruction} to store input and output operands.
 */
public class InstructionOperandsCtx extends DefinitionExtension<Instruction> {
  private final List<GcbInstructionOperand> inputs;
  private final List<GcbInstructionOperand> outputs;

  /**
   * Constructor.
   */
  public InstructionOperandsCtx(List<GcbInstructionOperand> inputs,
                                List<GcbInstructionOperand> outputs) {
    this.inputs = new ArrayList<>(inputs);
    this.outputs = new ArrayList<>(outputs);
  }

  public List<GcbInstructionOperand> inputs() {
    return inputs;
  }

  public List<GcbInstructionOperand> outputs() {
    return outputs;
  }

  @Override
  public Class<? extends Definition> extendsDefClass() {
    return Definition.class;
  }
}
