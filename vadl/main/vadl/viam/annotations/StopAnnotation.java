// SPDX-FileCopyrightText : © 2026 TU Wien <vadl@tuwien.ac.at>
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

package vadl.viam.annotations;

import vadl.viam.Annotation;
import vadl.viam.Group;
import vadl.viam.graph.Graph;

/**
 * Annotation to attach stop expression to a {@link Group} definition.
 *
 * <p>Example:<pre>{@code
 *  [stop : forall i in {O1, O2} then i.x = 0b1 ]
 *  group VLIW = (O1 | O2)
 * }</pre></p>
 */
public class StopAnnotation extends Annotation<Group> {

  private final Graph expression;

  public StopAnnotation(Graph expression) {
    this.expression = expression;
  }

  public Graph expr() {
    return expression;
  }

  @Override
  public Class<Group> parentDefinitionClass() {
    return Group.class;
  }
}
