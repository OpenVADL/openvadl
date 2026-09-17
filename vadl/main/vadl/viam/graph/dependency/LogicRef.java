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

package vadl.viam.graph.dependency;

import vadl.types.Type;
import vadl.viam.Logic;
import vadl.viam.graph.Node;

/**
 * Reference to a logic element. Should only appear in MiA stages.
 */
public class LogicRef extends ExpressionNode {

  private final Logic logic;

  public LogicRef(Type type, Logic logic) {
    super(type);
    this.logic = logic;
  }

  @Override
  public ExpressionNode copy() {
    return new LogicRef(type(), logic);
  }

  @Override
  public Node shallowCopy() {
    return new LogicRef(type(), logic);
  }

  public Logic logic() {
    return logic;
  }
}
