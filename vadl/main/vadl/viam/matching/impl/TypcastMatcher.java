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

package vadl.viam.matching.impl;

import java.util.Optional;
import vadl.types.Type;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.TypeCastNode;
import vadl.viam.matching.Matcher;

/**
 * Matches a {@link TypeCastNode}.
 */
public class TypcastMatcher implements Matcher {

  private final Optional<Type> type;
  private final Matcher matcher;

  public TypcastMatcher(Type type, Matcher matcher) {
    this.type = Optional.ofNullable(type);
    this.matcher = matcher;
  }

  /**
   * Match any typecast when {@link Matcher} matches the value.
   */
  protected TypcastMatcher(Matcher matcher) {
    this.type = Optional.empty();
    this.matcher = matcher;
  }

  @Override
  public boolean matches(Node node) {
    if (node instanceof TypeCastNode && type.isEmpty()) {
      return matcher.matches(node);
    } else if (node instanceof TypeCastNode && ((TypeCastNode) node).type() == this.type.get()) {
      return this.matches(((TypeCastNode) node).value());
    }

    return false;
  }
}
