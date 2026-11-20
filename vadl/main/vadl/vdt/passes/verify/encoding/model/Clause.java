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

package vadl.vdt.passes.verify.encoding.model;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Clause in an CNF encoding.
 *
 * @param literals the literals.
 */
public record Clause(Set<Integer> literals) {

  /**
   * The constructor.
   */
  public Clause(int... literals) {
    this(new LinkedHashSet<>(Arrays.stream(literals).boxed().toList()));
  }

  /**
   * Extend the clause with some literals.
   */
  public Clause extend(int... add) {
    final Set<Integer> l = new LinkedHashSet<>(literals);
    Arrays.stream(add).boxed().forEach(l::add);
    return new Clause(l);
  }
}
