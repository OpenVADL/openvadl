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

package vadl.viam.annotations;

import java.util.List;
import vadl.viam.Annotation;
import vadl.viam.Format;
import vadl.viam.Instruction;

/**
 * Overwrite the input operands for the instruction.
 */
public class DefineOperandAnnotation extends Annotation<Instruction> {
  private final List<Format.Field> inputs;

  /**
   * Create a constructor with a list of fields which are the inputs for the instruction.
   */
  public DefineOperandAnnotation(List<Format.Field> inputs) {
    this.inputs = inputs;
  }

  public List<Format.Field> inputs() {
    return inputs;
  }

  @Override
  public Class<Instruction> parentDefinitionClass() {
    return Instruction.class;
  }
}
