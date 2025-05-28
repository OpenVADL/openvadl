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
import vadl.types.Type;

/**
 * Represents a parameter in a VADL specification.
 */
public class Parameter extends Definition implements DefProp.WithType {

  private final Type type;

  // the parent of this parameter (e.g. a function definition)
  @Nullable
  private Definition parent;

  /**
   * Constructs the parameter without parent.
   * You must add the
   * parent definition directly after construction.
   */
  public Parameter(Identifier identifier, Type type) {
    super(identifier);
    this.type = type;
  }

  /**
   * Constructs a parameter.
   */
  @SuppressWarnings("NullableProblems")
  public Parameter(Identifier identifier, Type type, Definition parent) {
    super(identifier);
    this.type = type;
    this.parent = parent;
  }

  @Override
  public void verify() {
    super.verify();
    ensure(parent != null,
        "Parent definition is null, but should always be set after creation. "
            + "Someone created a Parameter without setting the parent.");
  }

  @Override
  public Type type() {
    return type;
  }


  @Override
  public String toString() {
    return simpleName() + ": " + type;
  }


  public Definition parent() {
    ensure(parent != null, "Parent definition is null but this should not happen");
    return parent;
  }

  public void setParent(@SuppressWarnings("NullableProblems") Definition parent) {
    this.parent = parent;
  }

  @Override
  public void accept(DefinitionVisitor visitor) {
    visitor.visit(this);
  }

}
