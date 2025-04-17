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

package vadl.viam;

import static com.google.common.collect.Lists.reverse;

import java.util.List;
import javax.annotation.Nullable;
import vadl.types.BitsType;
import vadl.types.ConcreteRelationType;
import vadl.types.DataType;
import vadl.types.Type;

/**
 * A register tensor is a generic representation of all kinds of registers.
 * It is defined by a list of {@link Dimension}s. This list defines the result types
 * and index types of registers.
 *
 * <p>The dimension list is sorted as in the VADL specification: outer dimensions are at the start,
 * inner ones are at the end.
 * The minimal accessible unit is the innermost dimension as a whole.</p>
 *
 * <p>A {@link Register} has only a single dimension.
 * E.g. {@code register X: Bits<32>} has dimensions {@code { (Bits<5>, 32) }} and its result type
 * is calculated from this dimension.
 * A {@link RegisterFile} has two dimensions.
 * E.g. {@code register X: Bits<5> -> Bits<64>} has dimensions
 * {@code { (Bits<5>, 32), (Bits<6>, 64)}}.</p>
 */
public class RegisterTensor extends Resource {

  /**
   * A dimension in a {@link RegisterTensor} consists of an index type and a size.
   * The type defines the required size to access an element of the next inner dimension;
   * the size defines how many elements are part of this dimension.
   *
   * <p>The size argument must fit in the index type.</p>
   *
   * @param indexType index type required to access an element of this dimension
   * @param size      number of elements in this dimension
   */
  public record Dimension(DataType indexType, int size) {

    /**
     * Constructs the dimension and checks whether the provided properties are
     * correct.
     */
    public Dimension {
      if (size < 1) {
        throw new IllegalArgumentException("Dimension size must be greater than 0: " + this);
      }
      if (indexType.bitWidth() < BitsType.minimalRequiredWidthFor(size - 1)) {
        throw new IllegalArgumentException("Size does not fit in dimension index type" + this);
      }
    }
  }

  private final List<Dimension> dimensions;

  public RegisterTensor(Identifier identifier, List<Dimension> dimensions) {
    super(identifier);
    this.dimensions = dimensions;
  }

  public int dimCount() {
    return dimensions.size();
  }

  public List<Dimension> dimensions() {
    return dimensions;
  }

  public Dimension outermostDim() {
    return dimensions.getFirst();
  }

  public Dimension innermostDim() {
    return dimensions.getLast();
  }

  @Override
  public List<DataType> indexTypes() {
    return dimensions.stream().map(Dimension::indexType).toList();
  }

  @Override
  public boolean hasAddress() {
    return dimensions.size() > 1;
  }

  @Nullable
  @Override
  public DataType addressType() {
    ensure(dimensions.size() == 2, "This method only works for 2-dimensional registers");
    return outermostDim().indexType();
  }

  @Override
  public DataType resultType() {
    return DataType.bits(innermostDim().size());
  }

  @Override
  public DataType resultType(int providedDimensions) {
    ensure(providedDimensions < dimensions.size(),
        "Too many dimensions provided, max is size - 1 dimensions.");
    // concatenate the reset of all returned dimensions
    var width = reverse(dimensions).stream()
        .skip(providedDimensions)
        .mapToInt(Dimension::size)
        .sum();
    return Type.bits(width);
  }

  @Override
  public ConcreteRelationType relationType() {
    // all from one.
    // e.g. Bits<4><2><32> -> args: Bits<10>, Bits<10>
    var args = dimensions.stream().skip(1)
        .map(Dimension::indexType)
        .map(Type.class::cast).toList();

    var result = resultType();
    return ConcreteRelationType.concreteRelation(args, result);
  }

  @Override
  public void accept(DefinitionVisitor visitor) {
    visitor.visit(this);
  }

}