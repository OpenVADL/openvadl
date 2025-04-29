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

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import vadl.types.BitsType;
import vadl.types.DataType;
import vadl.types.Type;
import vadl.utils.Pair;

/**
 * A frontend type that represents a potentially multidimensional {@link DataType}.
 * E.g. {@code Bits<32><32>} and the equivalent {@code Bits<5> -> Bits<32>} are such tensor types.
 *
 * <p>It has exactly one base type, which defines the innermost type that can be accessed without
 * slicing.
 * Additionally, it holds a list of {@link Index} which defines addressable indices/dimensions
 * with their respective size and type.</p>
 *
 * @see Index
 */
// FIXME: Is this a Bits Subtype?
class TensorType extends BitsType {

  /**
   * An index dimension of a {@link TensorType}.
   * E.g., for the mapping type {@code Bits<5> -> Bits<32>} the argument type is an index of
   * {@code (size: 32, type: Bits<5>)}.
   *
   * <p>The dimension type {@code Bits<12><32>} also has one index (outermost dimension)
   * of {@code (size: 12, type: null)}.
   * As the user does not define the access type in the second example (dimension notation),
   * it must not be checked and is therefore null.
   *
   * @param size size of the index dimension
   * @param type optional type of the index dimension (only used for mapping types)
   */
  record Index(int size, @Nullable BitsType type) {
    /**
     * Constructs an index from a given mapping type argument.
     * The index size is given from the type size.
     * E.g. the index of {@code Bits<5> -> ...} has a size of {@code 32} (power of 2).
     *
     * @param bitsType the type of the mapping argument
     * @return an index with the size being {@code 2^bitsType.width}.
     */
    static Index ofMappingArg(BitsType bitsType) {
      return new Index((int) Math.pow(2, bitsType.bitWidth()), bitsType);
    }

    /**
     * Returns a type that fits the given index size.
     * If no type for the index is given, a bits type with the minimal required size is constructed.
     *
     * @return a type that may hold at least the size of the index.
     */
    BitsType viamType() {
      return type == null ? Type.bits(BitsType.minimalRequiredWidthFor(size - 1)) : type;
    }
  }

  // hashmap that contains all global occurrences of the tensor type
  static final HashMap<Pair<List<Index>, DataType>, TensorType> cache = new HashMap<>();

  // the size of the n > 0th indices. e.g., in Bits<2><32> this would be List.of(2)
  final List<Index> indices;
  // the 0th dimension. e.g., in Bits<2><32> this would be Bits<32>
  final DataType baseType;

  // private to prevent multiple instances of the same type
  private TensorType(List<Index> indices, DataType baseType) {
    super(indices.stream().map(Index::size).reduce(baseType.bitWidth(), (a, b) -> a * b));
    this.indices = indices;
    this.baseType = baseType;
  }

  /**
   * Construct a tensor type.
   * If there already exists such a tensor type, no new one is constructed.
   *
   * @param indexDims index dimensions. {@code Bits<5><4><32>} would have two such dimensions.
   * @param baseType  the innermost dimension type. {@code Bits<5><32>} has a base type
   *                  of {@code Bits<32>}
   */
  static TensorType of(List<Index> indexDims, DataType baseType) {
    return cache.computeIfAbsent(new Pair<>(indexDims, baseType),
        k -> new TensorType(indexDims, baseType));
  }

  /**
   * Construct a tensor type with no index dimensions.
   */
  static TensorType of(DataType baseType) {
    return of(List.of(), baseType);
  }

  /**
   * Returns the width if the tensor was accessed as a whole.
   */
  int mergedWidth() {
    return indices.stream().map(Index::size).reduce(baseType.bitWidth(), (a, b) -> a * b);
  }

  /**
   * Returns the type if the tensor was accessed as a whole.
   * If the tensor has no indices, this is equivalent to the base type.
   * Otherwise, it is a Bits type of size {@link #mergedWidth()}.
   */
  DataType mergedType() {
    if (indices.isEmpty()) {
      return baseType;
    }
    return Type.bits(mergedWidth());
  }

  /**
   * Tries to unpack to base type.
   *
   * @return the base type if no indices are given, this type otherwise.
   */
  Type unpack() {
    if (indices.isEmpty()) {
      return baseType;
    }
    return this;
  }

  /**
   * The type when accessing some value of this tensor type with a certain number of indices.
   * E.g. given {@code X: Bits<5><4><32>}, when calling this method on this type with
   * {@code 1}, the return type would be {@code Bits<4><32>}, which would correspond to an access
   * such as {@code X(2)}.
   *
   * @param accessedIndices the number of indices that are "accessed". This must be <= the number
   *                        of indices, otherwise a runtime exception is thrown.
   * @return the tensor type of the partial access. If {@code accessedIndices == indices.size()}
   *     the return tensor type will have no indices, but only the base type set.
   */
  TensorType typeAfterNIndices(int accessedIndices) {
    if (accessedIndices > indices.size()) {
      throw new IllegalStateException(
          "More indices accessed than available: %s vs %s".formatted(accessedIndices,
              indices.size()));
    }
    return of(indices.subList(accessedIndices, indices.size()), baseType);
  }


  @Override
  public String name() {
    var baseTypeName = baseType.getClass().getSimpleName().replace("Type", "");
    var dims = this.indices.stream().map(d -> "<" + d + ">").collect(Collectors.joining());
    return baseTypeName + dims + "<" + baseType.bitWidth() + ">";
  }
}
