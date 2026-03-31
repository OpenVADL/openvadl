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
import vadl.types.Type;
import vadl.viam.graph.Graph;

/**
 * A set of interfaces that mark definitions with certain properties.
 * This allows seraching for definitions with such properties.
 */
public interface DefProp {

  default Definition asDefinition() {
    return (Definition) this;
  }

  /**
   * A marker interface for definitions that have a type. (e.g. format, format field, ...)
   */
  interface WithType extends DefProp {
    Type type();
  }

  /**
   * A marker interface for definitions that hold one or more behaviors.
   */
  interface WithBehavior extends DefProp {

    List<Graph> behaviors();
  }
}
