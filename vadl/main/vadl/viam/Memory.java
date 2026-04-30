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

import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import vadl.types.ConcreteRelationType;
import vadl.types.DataType;
import vadl.types.Type;
import vadl.viam.graph.Graph;

/**
 * The Memory class represents a VADL memory definition.
 *
 * <p>It always takes an address type and result type. The result type
 * specifies the word size of the memory.</p>
 */
public class Memory extends Resource implements DefProp.WithOptionalBehaviour {

  private final DataType addressType;
  private final DataType resultType;

  private Endianness endianness = Endianness.LITTLE;

  /**
   * If this condition is not {@code null} and evaluates to {@code false}, then
   * the opposite endianness as returned by {@link #endianness()} is used.
   */
  @Nullable
  private Graph biEndianCondition;

  /**
   * Constructs a new Memory object.
   *
   * @param identifier the identifier of the memory
   * @param accessType the address type of the memory
   * @param resultType the result type of the memory
   */
  public Memory(Identifier identifier, DataType accessType, DataType resultType) {
    super(identifier);
    this.addressType = accessType;
    this.resultType = resultType;
  }

  /**
   * Returns the word size of the memory.
   */
  public int wordSize() {
    return resultType.bitWidth();
  }

  @Override
  public boolean hasAddress() {
    return true;
  }

  @Override
  @Nonnull
  public DataType addressType() {
    return addressType;
  }

  @Override
  public List<DataType> indexTypes() {
    return List.of(addressType());
  }

  @Override
  public DataType resultType() {
    return resultType;
  }

  @Override
  public DataType resultType(int providedDimensions) {
    return resultType();
  }

  @Override
  public ConcreteRelationType relationType() {
    return Type.concreteRelation(addressType, resultType);
  }

  /**
   * Returns whether the endianness of this memory depends on a condition
   * (meaning it can change). The condition, if present, is returned by
   * {@link #biEndianCondition()}.
   *
   * @return whether this memory is bi-endian
   */
  public boolean isBiEndian() {
    return biEndianCondition != null;
  }

  /**
   * The endiannes of this memory. See also {@link #isBiEndian()} and
   * {@link #biEndianCondition()}.
   *
   * @return the endianness of this memory
   */
  public Endianness endianness() {
    return endianness;
  }

  public void setEndianness(Endianness endianness) {
    this.endianness = endianness;
  }

  /**
   * If this condition is not {@code null} and evaluates to {@code false}, then
   * the opposite endianness as returned by {@link #endianness()} is used.
   *
   * @return a function graph without params containing the condition
   */
  @Nullable
  public Graph biEndianCondition() {
    return biEndianCondition;
  }

  /**
   * Sets the bi-endian condition (see {@link #biEndianCondition()}).
   *
   * @param biEndianCondition a function graph without params containing the condition
   */
  public void setBiEndianCondition(@Nullable Graph biEndianCondition) {
    this.biEndianCondition = biEndianCondition;
  }

  @Override
  public List<Graph> behaviors() {
    return biEndianCondition == null
        ? List.of()
        : List.of(biEndianCondition);
  }

  @Override
  public void accept(DefinitionVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public String toString() {
    return identifier.simpleName() + ": " + addressType + " -> " + resultType;
  }
}
