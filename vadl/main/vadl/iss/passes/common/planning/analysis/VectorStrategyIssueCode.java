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

package vadl.iss.passes.common.planning.analysis;

/**
 * Rejection reasons for the direct-gvec strategy evaluator.
 */
public enum VectorStrategyIssueCode {
  NO_FORALL,
  MULTIPLE_FORALLS,
  EXTRA_SIDE_EFFECTS,
  FORALL_WITHOUT_SINGLE_SIDE_EFFECT,
  WRITE_NOT_BASE_CHUNK,
  WRITE_HAS_CONDITION,
  DESTINATION_NOT_GVEC_CAPABLE,
  UNSUPPORTED_ELEMENT_WIDTH,
  NON_BYTE_OPERATION_SIZE,
  UNSUPPORTED_VALUE_SHAPE,
  UNSUPPORTED_OPERATION,
  OP_SIZE_NOT_FULL_RANGE,
  LAYOUT_NOT_CONTIGUOUS,
  OPERAND_NOT_VECTOR_READ,
  READ_NOT_BASE_ELEMENT,
  READ_NOT_GVEC_CAPABLE,
  READ_WIDTH_MISMATCH,
  READ_OFFSET_MISMATCH
}
