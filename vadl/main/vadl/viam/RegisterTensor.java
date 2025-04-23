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

import com.google.common.collect.Streams;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
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
 * The minimal accessible unit is the innermost dimension as a whole.
 * If users access more specific areas of a register by using formats, slices, or an index on the
 * innermost dimension, these accesses are resolved by the frontend by using slice
 * nodes in the graph.</p>
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
   * @param index     the index of this dimension (0 is the outermost dimension)
   * @param indexType index type required to access an element of this dimension
   * @param size      number of elements in this dimension
   */
  public record Dimension(int index, DataType indexType, int size) {

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
  private final Constraint[] constraints;

  /**
   * Constructs the register tensor.
   */
  public RegisterTensor(Identifier identifier, List<Dimension> dimensions,
                        Constraint[] constraints) {
    super(identifier);
    this.dimensions = dimensions;
    this.constraints = constraints;
  }

  public int dimCount() {
    return dimensions.size();
  }

  /**
   * The dimensions of this register tensor.
   * The outermost dimension is at the start, the innermost at the end of the list.
   */
  public List<Dimension> dimensions() {
    return dimensions;
  }

  public List<Dimension> indexDimensions() {
    return dimensions.subList(0, maxNumberOfAccessIndices());
  }

  public int maxNumberOfAccessIndices() {
    return dimCount() - 1;
  }

  /**
   * Returns whether this register tensor represents a single register.
   * This the case if the number of dimensions is 1.
   */
  public boolean isSingleRegister() {
    return dimCount() == 1;
  }

  /**
   * Returns whether this register tensor represents a register file.
   * This the case if the number of dimensions is 2.
   */
  public boolean isRegisterFile() {
    return dimCount() == 2;
  }

  public Dimension outermostDim() {
    return dimensions.getFirst();
  }

  public Dimension innermostDim() {
    return dimensions.getLast();
  }

  public Constraint[] constraints() {
    return constraints;
  }

  @Override
  public List<DataType> indexTypes() {
    return dimensions.stream().limit(maxNumberOfAccessIndices()).map(Dimension::indexType).toList();
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

  /**
   * The Bits type of the innermost dimension.
   * It assumes that dimCount - 1 dimensions are accessed.
   */
  @Override
  public DataType resultType() {
    return DataType.bits(innermostDim().size());
  }

  /**
   * The concatenated bits type of all dimensions that were not accessed.
   * E.g. given {@code register X: Bits<8><4><32>} then the result type of
   * {@code X(2)} would be {@code Bits<4 * 32>}, as the provided number of dimensions
   * was {@code 1}.
   *
   * @param accessedDimensions number of accessed dimensions (indexes when accessing register)
   * @return a Bits type with the concatenation of all dimensions that were not accessed.
   */
  @Override
  public DataType resultType(int accessedDimensions) {
    ensure(accessedDimensions <= maxNumberOfAccessIndices(),
        "Too many dimensions provided, max is %s, got %s.", maxNumberOfAccessIndices(),
        accessedDimensions);
    // concatenate the reset of all returned dimensions by multiply the entries per
    // dimension
    var width = dimensions.stream()
        .skip(accessedDimensions)
        .mapToInt(Dimension::size)
        .reduce(1, (a, b) -> a * b);
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

  /**
   * Return the address of a zero register if it exists.
   */
  public Optional<List<Constant.Value>> zeroRegister() {
    return Arrays.stream(constraints())
        .filter(c -> c.value().intValue() == 0)
        .map(c -> c.indices)
        .findFirst();
  }

  /**
   * Generate the name from this register file with an {@code index}.
   */
  public String generateRegisterFileName(int index) {
    ensure(isRegisterFile(), "must be registerFile");
    return identifier.simpleName() + index;
  }

  @Override
  public void accept(DefinitionVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public void verify() {
    super.verify();
    for (Constraint constraint : constraints) {
      ensureMatchingIndexTypes(constraint.indices.stream().map(Constant.Value::type).toList());
      ensure(constraint.value.type().isTrivialCastTo(resultType(constraint.indices().size())),
          "Type mismatch: Can't cast constraint value type %s to register tensor result type %s.",
          constraint.value.type(), this.resultType());
    }
  }

  /**
   * Ensures that the types of the given indices match the types of the register tensor's
   * dimension types.
   */
  public void ensureMatchingIndexTypes(List<DataType> indexTypes) {
    ensure(indexTypes.size() <= maxNumberOfAccessIndices(),
        "Too may indices provided, max is %s, got %s", maxNumberOfAccessIndices(),
        indexTypes.size());
    var dims = dimensions().stream().limit(indexTypes.size()).map(Dimension::indexType);
    Streams.forEachPair(indexTypes.stream(), dims, (provided, actual) -> {
      ensure(provided.isTrivialCastTo(actual),
          "Provided index type does not match respective tensor image type: %s != %s",
          provided, actual);
    });
  }

  /**
   * A register file constraint that statically defines the result value for a specific
   * index.
   *
   * <p>For example<pre>
   *  {@code
   * [X(0) = 0]
   * register file X: Index -> Regs
   * }
   * </pre>
   * defines that the address 0 always results in 0 on register file X.
   * </p>
   *
   * @param indices of constraint
   * @param value   of constraint
   */
  public record Constraint(
      List<Constant.Value> indices,
      Constant.Value value
  ) {
  }
}