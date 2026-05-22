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

package vadl.types;

/**
 * An array type, as used in group annotations.
 * E.g: <pre>
 * [assert : VLIW.length <= 2]
 * group VLIW = O1.O2
 * </pre>
 */
public class ArrayType extends Type {

  private final Type elementType;
  private final UIntType lengthType;

  /**
   * Construct an array type.
   *
   * @param elementType the type of the elements of the array.
   * @param lengthType  the type of the length of the array. Must be an unsigned integer type.
   */
  public ArrayType(Type elementType, UIntType lengthType) {
    this.elementType = elementType;
    this.lengthType = lengthType;
  }

  public Type elementType() {
    return elementType;
  }

  public UIntType lengthType() {
    return lengthType;
  }

  @Override
  public String name() {
    return "%s[%s]".formatted(elementType.name(), lengthType.name());
  }
}
