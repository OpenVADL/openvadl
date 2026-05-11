// SPDX-FileCopyrightText : © 2025-2026 TU Wien <vadl@tuwien.ac.at>
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
import java.util.Optional;
import vadl.types.DataType;
import vadl.utils.SourceLocation;

/**
 * Interface to combine common functionality for {@link RegisterTensor} and
 * {@link ArtificialResource}.
 *
 * @deprecated Use {@link RegisterResource} instead.
 */
// TODO: This is a bad abstraction. We already have the RegisterResource class that
//    combines RegisterTensor and ArtificialResource. (#945)
@Deprecated
public interface GeneratesRegisterFileName {

  /**
   * Get the name of the register file.
   */
  Identifier identifier();

  /**
   * Get the dimensions of the register file.
   */
  List<RegisterTensor.Dimension> dimensions();

  /**
   * Returns whether this register tensor represents a register file.
   * This the case if the number of dimensions is 2.
   */
  boolean isRegisterFile();

  /**
   * Generate the name from this register file with an {@code index}.
   */
  default String generateRegisterFileName(int index) {
    ViamError.ensure(isRegisterFile(), "must be registerFile");
    return identifier().simpleName() + index;
  }

  /**
   * Get the location where the object was defined.
   */
  SourceLocation location();

  /**
   * Get the address type of the object.
   */
  DataType addressType();

  /**
   * Get the result type of the object.
   */
  DataType resultType();

  /**
   * Get the constraints of the register file.
   */
  List<RegisterResource.Constraint> constraints();

  /**
   * Return the address of a zero register if it exists.
   */
  default Optional<List<Constant.Value>> zeroRegister() {
    return constraints()
        .stream()
        .filter(c -> c.value().intValue() == 0)
        .map(RegisterResource.Constraint::indices)
        .findFirst();
  }
}
