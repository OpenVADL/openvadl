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

package vadl.ast.nodes;

import java.util.List;
import java.util.stream.Collectors;

/**
 * A projection type describes an operation that converts multiple arguments to a new value.
 * A projection type is considered a subtype of another projection type, iif the result type of
 * the projection type is a subtype of the other projection type AND all argument types of the
 * projection types are a subtype of the other projection type's respective argument.
 */
@SuppressWarnings("MissingJavadocMethod")
public class ProjectionType implements SyntaxType {
  public List<SyntaxType> arguments;
  public SyntaxType resultType;

  public ProjectionType(List<SyntaxType> arguments, SyntaxType resultType) {
    this.arguments = arguments;
    this.resultType = resultType;
  }

  @Override
  public String toString() {
    return arguments.stream().map(Object::toString).collect(Collectors.joining(", ", "(", ")"))
        + " -> " + resultType;
  }

  @Override
  public boolean isSubTypeOf(SyntaxType other) {
    if (!(other instanceof ProjectionType otherProjection)) {
      return false;
    }
    if (!resultType.isSubTypeOf(otherProjection.resultType)) {
      return false;
    }
    if (arguments.size() != otherProjection.arguments.size()) {
      return false;
    }
    for (int i = 0; i < arguments.size(); i++) {
      if (!otherProjection.arguments.get(i).isSubTypeOf(arguments.get(i))) {
        return false;
      }
    }
    return true;
  }

  @Override
  public String print() {
    return arguments.stream().map(SyntaxType::print).collect(Collectors.joining(",", "(", ")"))
        + " -> " + resultType.print();
  }
}
