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

package vadl.vdt.target.common.dto;

import java.math.BigInteger;
import java.nio.ByteOrder;
import vadl.utils.FieldExtractionUtils;
import vadl.vdt.utils.Instruction;
import vadl.viam.Constant.Value;
import vadl.viam.Format.Field;
import vadl.viam.Format.FieldAccess;

/**
 * Encapsulate a successfully decoded instruction, enabling access to the format fields of the
 * instruction.
 *
 * @param entry     The decode tree entry.
 * @param encoding  The instruction encoding.
 * @param byteOrder The byte order to interpret the encoding as.
 */
public record DecodedInstruction(Instruction entry, BigInteger encoding, ByteOrder byteOrder) {

  /**
   * Get the VIAM instruction definition of this instruction.
   *
   * @return The instruction definition
   */
  public vadl.viam.Instruction source() {
    return entry.source();
  }

  /**
   * Extracts the requested format field from this instruction.
   *
   * @param field The field to extract.
   * @return The field's value.
   */
  public Value get(Field field) {
    return FieldExtractionUtils.extract(encoding, byteOrder, field);
  }

  /**
   * Extracts the requested field access value from this instruction.
   *
   * @param fieldAccess The field access to extract.
   * @return The field access' value.
   */
  public Value get(FieldAccess fieldAccess) {
    return FieldExtractionUtils.extract(encoding, byteOrder, fieldAccess);
  }

}
