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

package vadl.lcb.passes.isaMatching.database;

import java.util.function.Predicate;
import vadl.viam.graph.Node;

/**
* Query for checking instruction's behavior for conditions.
*/
public class BehaviorQuery {
  private Class<? extends Node> applicable;
  private Predicate<Node> predicate;

  public BehaviorQuery(Class<? extends Node> applicable, Predicate<Node> predicate) {
    this.applicable = applicable;
    this.predicate = predicate;
  }

  public Class<? extends Node> applicable() {
    return applicable;
  }

  public Predicate<Node> predicate() {
    return predicate;
  }
}
