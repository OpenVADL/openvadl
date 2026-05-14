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

package vadl.viam;


import java.util.List;
import javax.annotation.Nullable;
import vadl.utils.Either;
import vadl.utils.WithLocation;
import vadl.viam.graph.Graph;

/**
 * Indicates that an instruction is printable because it has a behavior and assembly.
 */
public interface PrintableInstruction extends WithLocation {
  /**
   * Get the identifier of an instruction.
   */
  Identifier identifier();

  /**
   * Get the behavior of an instruction.
   */
  Graph behavior();

  /**
   * Get the {@link Assembly} of an instruction.
   */
  Assembly assembly();

  /**
   * Get the format of an instruction. If the instruction is a {@link PseudoInstruction} get
   * all formats of the instruction the machine instruction is expanded to.
   */
  List<Format> formats();

  /**
   * Find the {@link Format.Field} or {@link Format.FieldAccess} of the instruction
   * which is named like the passed operand.
   */
  @Nullable
  Either<Format.Field, Format.FieldAccess> getFieldOrAccess(String operandName);

  /**
   * Total bitwidth of the instruction.
   * Format bitwidth of {@link Instruction} or sum of bitwidth expanded instructions
   * in the case of {@link PseudoInstruction}.
   */
  default int bitWidth() {
    return formats().stream().map(f -> f.type().bitWidth()).reduce(0, Integer::sum);
  }

  ;
}
