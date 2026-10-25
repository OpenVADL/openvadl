// SPDX-FileCopyrightText : © 2026 TU Wien <vadl@tuwien.ac.at>
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

import java.util.Set;

/**
 * Models the flow of {@link Instruction Instructions} between two components
 * of the MiA, {@link #source()} and {@link #destination()}.
 *
 * <p>The implementations limit the kinds of MiA elements between which
 * instructions may flow, serving as a sort of documentation while also
 * allowing for elegant switch statements/expressions.
 */
public sealed interface MiaInstructionFlow permits
    MiaInstructionFlow.StageToStage,
    MiaInstructionFlow.StageToReservationStation,
    MiaInstructionFlow.ReservationStationToStage {

  /**
   * The source of this instruction flow. The exact type of this depends on the
   * implementation. If you need it, that's a good sign that you might want to
   * use a {@code switch} over the {@code MiaInstructionFlow}.
   *
   * @return The source of this instruction flow
   */
  Definition source();

  /**
   * The destination of this instruction flow. The exact type of this depends
   * on the implementation. If you need it, that's a good sign that you might
   * want to use a {@code switch} over the {@code MiaInstructionFlow}.
   *
   * @return The destination of this instruction flow.
   */
  Definition destination();

  /**
   * All of the {@link Instruction Instructions} that might proceed along this
   * instruction flow. This takes into account the structure of the instruction
   * flow graph, as well as any filters defined for this flow.
   *
   * @return All of the {@code Instructions} that can proceed along this
   *         instruction flow.
   */
  Set<Instruction> allowedInstructions();

  /**
   * Models the flow of an {@link Instruction} from one {@link Stage} to
   * another.
   *
   * <p>Such instruction flows arise from reads of the source stage from the
   * destination stage's {@link Stage#behavior() behavior}. Multiple reads
   * from a given stage to another one are collapsed into a single instruction
   * flow.
   *
   * @param source      The read {@link Stage}.
   * @param destination The reading {@link Stage}.
   */
  record StageToStage(
      Stage source,
      Stage destination,
      Set<Instruction> allowedInstructions
  ) implements MiaInstructionFlow {}

  /**
   * Models the flow of an {@link Instruction} from a {@link Stage} to
   * a {@link Logic.ReservationStation ReservationStation}.
   *
   * <p>Such instruction flows arise from the {@code source} annotation on a
   * {@link Logic.ReservationStation ReservationStation}.
   *
   * @param source      The read {@link Stage}.
   * @param destination The reading
   *                    {@link Logic.ReservationStation ReservationStation}.
   */
  record StageToReservationStation(
      Stage source,
      Logic.ReservationStation destination,
      Set<Instruction> allowedInstructions
  ) implements MiaInstructionFlow {}

  /**
   * Models the flow of an {@link Instruction} from a
   * {@link Logic.ReservationStation ReservationStation} to a {@link Stage}.
   *
   * <p>Such instruction flows arise from the {@code consume} builtin being
   * called with the source {@link Logic.ReservationStation ReservationStation}
   * as its first argument from the destination stage's
   * {@link Stage#behavior() behavior}.
   *
   * @param source      The read
   *                    {@link Logic.ReservationStation ReservationStation}.
   * @param destination The reading {@link Stage}.
   */
  record ReservationStationToStage(
      Logic.ReservationStation source,
      Stage destination,
      Set<Instruction> allowedInstructions
  ) implements MiaInstructionFlow {}
}
