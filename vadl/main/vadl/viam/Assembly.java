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

import vadl.types.Type;

/**
 * The Assembly definition of an instruction in a VADL specification.
 */
public class Assembly extends Definition {

  private final Function function;

  /**
   * Creates an Assembly object with the specified identifier and arguments.
   *
   * @param identifier the identifier of the Assembly definition
   * @param function   the function to create an assembly string
   */
  public Assembly(Identifier identifier, Function function) {
    super(identifier);

    this.function = function;

    verify();
  }

  public Function function() {
    return function;
  }

  @Override
  public void verify() {
    super.verify();
    ensure(function.returnType().equals(Type.string()),
        "Assembly function does not return a String, but %s", function.returnType());
  }

  @Override
  public void accept(DefinitionVisitor visitor) {
    visitor.visit(this);
  }

}
