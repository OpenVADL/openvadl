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
public class GroupType extends Type {

  private final Type elementType;

  private final UIntType lengthType;
  private final UIntType bitLengthType;

  /**
   * Construct a group expression type.
   *
   * @param elementType   the type of the elements of the group.
   * @param lengthType    the type of the length of the group. Must be an unsigned integer type.
   * @param bitLengthType the type of the bitlength of the group.
   */
  public GroupType(Type elementType, UIntType lengthType, UIntType bitLengthType) {
    this.elementType = elementType;
    this.lengthType = lengthType;
    this.bitLengthType = bitLengthType;
  }

  public Type elementType() {
    return elementType;
  }

  public UIntType lengthType() {
    return lengthType;
  }

  public UIntType bitLengthType() {
    return bitLengthType;
  }

  @Override
  public String name() {
    return "%s[%s]".formatted(elementType.name(), lengthType.name());
  }
}
