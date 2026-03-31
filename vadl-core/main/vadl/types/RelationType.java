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

package vadl.types;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents a relation type in VADL's type system.
 * A relation type consists of a list of argument types and a return type.
 * The argument types are represented as a list of subclasses of the base class Type.
 * The return type is represented using a subclass of the base class Type.
 */
public class RelationType extends Type {

  private final List<Class<? extends Type>> argTypeClass;
  private final boolean hasVarArgs;
  private final Class<? extends Type> resultTypeClass;

  protected RelationType(List<Class<? extends Type>> argTypes, boolean hasVarArgs,
                         Class<? extends Type> resultType) {
    this.argTypeClass = argTypes;
    this.hasVarArgs = hasVarArgs;
    this.resultTypeClass = resultType;
  }

  public List<Class<? extends Type>> argTypeClasses() {
    return argTypeClass;
  }

  public boolean hasVarArgs() {
    return hasVarArgs;
  }

  public Class<? extends Type> resultTypeClass() {
    return resultTypeClass;
  }

  @Override
  public String name() {
    return "("
        + argTypeClass.stream().map(Class::getSimpleName)
        .collect(Collectors.joining(", "))
        + ") -> "
        + resultTypeClass.getSimpleName();
  }
}
