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

package vadl.gcb.passes;

import java.util.List;
import java.util.Optional;
import vadl.types.BitsType;
import vadl.viam.Definition;
import vadl.viam.DefinitionExtension;
import vadl.viam.Instruction;

/**
 * An extension for the {@link Instruction}. It will be used to
 * label the instruction with a list of {@link TypedMachineInstructionLabel}.
 * The first label is seen as the primary label.
 */
public class MachineInstructionCtx extends DefinitionExtension<Instruction> {
  private final List<TypedMachineInstructionLabel> labels;

  public MachineInstructionCtx(List<TypedMachineInstructionLabel> labels) {

    this.labels = labels;
  }

  @Override
  public Class<? extends Definition> extendsDefClass() {
    return Definition.class;
  }

  public MachineInstructionLabel label() {
    return labels.getFirst().label();
  }

  public Optional<BitsType> type() {
    return labels.getFirst().type();
  }

  /**
   * Get all {@link MachineInstructionLabel} of the instruction in the order they were matched.
   */
  public List<MachineInstructionLabel> labels() {
    return labels.stream().map(TypedMachineInstructionLabel::label).toList();
  }
}
