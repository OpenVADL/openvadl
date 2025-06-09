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

package vadl.ast;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import vadl.types.BitsType;
import vadl.types.Type;

/**
 * A type for multidimensional bit vectors. For example {@code Bits<4><8>} in which case
 * innerType would be {@code Bits<8>} and dimensions {@code List.of(4)}.
 */
public class TensorType extends Type {

  private final List<Integer> dimensions;
  private final BitsType innerType;

  /**
   * Create a new multidimensional vector from a base and all additional dimensions.
   *
   * @param dimensions is a list of additional dimensions.
   * @param innerType  to be made multidimensional.
   */
  public TensorType(List<Integer> dimensions, BitsType innerType) {
    this.dimensions = dimensions;
    this.innerType = innerType;
  }

  /**
   * Create a new multidimensional vector by extending an existing one.
   *
   * @param dimensions to be added.
   * @param extending  to be extended.
   */
  public TensorType(List<Integer> dimensions, TensorType extending) {
    this.dimensions = new ArrayList<>(dimensions);
    this.dimensions.addAll(extending.dimensions);
    this.innerType = extending.innerType;
  }

  /**
   * Returns the type if the outermost dimension is removed.
   *
   * @return the original type without one dimension.
   */
  Type pop() {
    if (dimensions.size() <= 1) {
      return innerType;
    }

    return new TensorType(dimensions.subList(1, dimensions.size()), innerType);
  }

  int outerMostDimension() {
    return dimensions.getFirst();
  }

  BitsType flattenBitsType() {
    var bitWidth = dimensions.stream().reduce(1, (a, b) -> a * b) * innerType.bitWidth();
    return innerType.withBitWidth(bitWidth);
  }

  @Override
  public String name() {
    var dimensionString = dimensions.stream().map("<%d>"::formatted).collect(Collectors.joining());

    // Inject the dimension string into the middle of the base.
    // Eg: Bits<8> and dimension <32><16> => Bits<32><16><8>

    var innerString = innerType.toString();
    var insertPos = innerString.indexOf('<');
    if (insertPos < 0) {
      insertPos = innerString.length();
    }

    return innerString.substring(0, insertPos)
        + dimensionString
        + innerString.substring(insertPos);
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    TensorType that = (TensorType) o;
    return dimensions.equals(that.dimensions) && innerType.equals(that.innerType);
  }

  @Override
  public int hashCode() {
    int result = dimensions.hashCode();
    result = 31 * result + innerType.hashCode();
    return result;
  }
}
