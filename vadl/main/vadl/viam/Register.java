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
import vadl.types.BitsType;
import vadl.types.DataType;
import vadl.types.Type;

/**
 * Represents a Register in a VADL specification.
 */
public class Register extends RegisterTensor {

  /**
   * Constructions a new register definition.
   *
   * @param identifier the unique identifier of the definition
   * @param resultType the result type of the register
   */
  public Register(Identifier identifier, DataType resultType) {
    // construct dimensions
    // e.g. Bits<16> -> { Bits<4>, 16 }
    super(identifier, List.of(new Dimension(
            0,
            Type.bits(BitsType.minimalRequiredWidthFor(resultType.bitWidth())),
            resultType.toBitsType().bitWidth())
        ),
        new Constraint[] {}
    );
  }

  @Override
  public void accept(DefinitionVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public String toString() {
    return identifier.simpleName() + ": " + resultType();
  }

}
