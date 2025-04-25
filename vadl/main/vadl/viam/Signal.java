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
import org.jetbrains.annotations.Nullable;
import vadl.types.ConcreteRelationType;
import vadl.types.DataType;
import vadl.types.Type;

/**
 * Represents a Signal in a VADL MiA specification.
 *
 * <p>It only has a single write (driver) and multiple reads.
 */
public class Signal extends Resource {

  private final DataType resultType;

  /**
   * Constructions a new signal definition.
   *
   * @param identifier the unique identifier of the definition
   * @param resultType the type of the signal
   */
  public Signal(Identifier identifier, DataType resultType) {
    super(identifier);
    this.resultType = resultType;
  }

  @Override
  public boolean hasAddress() {
    return false;
  }

  @Nullable
  @Override
  public DataType addressType() {
    return null;
  }

  @Override
  public List<DataType> indexTypes() {
    return List.of();
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
    return Type.concreteRelation(resultType);
  }

  @Override
  public void accept(DefinitionVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public String toString() {
    return identifier.simpleName() + ": " + resultType;
  }

}
