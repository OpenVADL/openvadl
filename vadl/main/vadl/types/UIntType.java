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

/**
 * An arbitrary sized unsigned integer.
 */
public class UIntType extends BitsType {

  protected UIntType(int bitWidth) {
    super(bitWidth);
  }

  @Override
  public String name() {
    return "UInt<%s>".formatted(bitWidth);
  }

  /**
   * Returns a signed integer with the same {@code bitWidth}.
   */
  public SIntType makeSigned() {
    return new SIntType(bitWidth);
  }

  @Override
  public boolean isSigned() {
    return false;
  }

  @Override
  public boolean equals(Object obj) {
    return this.getClass() == obj.getClass()
        && super.equals(obj);
  }

  @Override
  public BitsType withBitWidth(int bitWidth) {
    return Type.unsignedInt(bitWidth);
  }

  public static UIntType minimalTypeFor(long value) {
    if (value < 0) {
      throw new IllegalArgumentException("value must be >= 0");
    }
    return Type.unsignedInt(minimalRequiredWidthFor(value));
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
