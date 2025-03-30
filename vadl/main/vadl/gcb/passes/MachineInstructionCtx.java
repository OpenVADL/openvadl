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

import java.util.Optional;
import vadl.types.BitsType;
import vadl.types.DataType;
import vadl.viam.Definition;
import vadl.viam.DefinitionExtension;
import vadl.viam.Instruction;

/**
 * An extension for the {@link Instruction}. It will be used
 * label the instruction with a {@link MachineInstructionLabel}. The {@code type} indicates on
 * what type the instruction operates. It can be {@link Optional#empty()} when they are
 * multiple writes or reads with different sizes.
 */
public class MachineInstructionCtx extends DefinitionExtension<Instruction> {
  private final MachineInstructionLabel label;
  private final Optional<BitsType> type;

  public MachineInstructionCtx(MachineInstructionLabel label,
                               Optional<BitsType> type) {
    this.label = label;
    this.type = type;
  }

  @Override
  public Class<? extends Definition> extendsDefClass() {
    return Definition.class;
  }

  public MachineInstructionLabel label() {
    return label;
  }

  public Optional<BitsType> type() {
    return type;
  }
}
