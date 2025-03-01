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

import javax.annotation.Nullable;
import vadl.types.ConcreteRelationType;
import vadl.types.DataType;
import vadl.types.Type;

/**
 * Represents a resource in the VADL specification.
 *
 * <p>Such resources are registers and memory definitions.</p>
 */
public abstract class Resource extends Definition implements DefProp.WithType {

  public Resource(Identifier identifier) {
    super(identifier);
  }

  /**
   * Returns if this resource takes an address in order to return
   * a value.
   */
  public abstract boolean hasAddress();

  /**
   * Returns the address type of this resource.
   */
  public abstract @Nullable DataType addressType();

  /**
   * Returns the result type of this resource.
   */
  public abstract DataType resultType();

  /**
   * Returns the type of this resource as a relation type.
   * E.g. a register {@code Z: Bits<3>} has the relation type {@code () -> Bits<3>}
   * while the register file {@code X: Bits<2> -> Bits<3>} has the relation type
   * {@code (Bits<2>) -> Bits<3>},
   */
  public abstract ConcreteRelationType relationType();

  @Override
  public Type type() {
    return resultType();
  }
}
