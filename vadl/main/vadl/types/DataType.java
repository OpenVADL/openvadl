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

package vadl.types;

import javax.annotation.Nullable;

/**
 * A type that represents actual data that can be stored in a continues
 * array of bits. All data types have a bit-width in memory.
 *
 * @see BitsType
 * @see BoolType
 */
public abstract class DataType extends Type {

  public abstract int bitWidth();

  /**
   * Bitwidth but without sign bit.
   */
  public abstract int useableBitWidth();

  /**
   * Checks if this type can be trivially cast to another type,
   * such that the bit representation must not be changed in any way.
   *
   * @param other the type to potentially cast to.
   * @return true if it is possible, false otherwise
   */
  @Override
  public final boolean isTrivialCastTo(Type other) {
    if (this == other) {
      return true;
    }
    if (other instanceof DataType otherDataType) {
      var sameLength = otherDataType.bitWidth() == bitWidth();
      return sameLength && (other instanceof BitsType || other instanceof BoolType);
    }
    return false;
  }

  public boolean isSigned() {
    return false;
  }

  @Nullable
  public abstract DataType fittingCppType();

  public BitsType toBitsType() {
    return Type.bits(bitWidth());
  }

}
