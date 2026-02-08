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

package vadl.vdt.impl.irregular.model;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import vadl.vdt.utils.BitPattern;

public record ExclusionConditions(Set<ExclusionCondition> conditions) {

  public ExclusionConditions(Set<ExclusionCondition> conditions) {
    this.conditions = new LinkedHashSet<>(conditions);
  }

  public ExclusionConditions() {
    this(new LinkedHashSet<>());
  }

  /**
   * Combine the exclusion conditions by keeping only a single tautological matching pattern.
   *
   * @param newConditions The exclusion conditions to add
   */
  public void add(Collection<ExclusionCondition> newConditions) {

    newConditions = new LinkedHashSet<>(newConditions);

    final ExclusionCondition newDoesMatchAll = newConditions.stream()
        .filter(c -> c.matching().doesMatchAll())
        .findFirst().orElse(null);

    if (newDoesMatchAll == null) {
      conditions.addAll(newConditions);
      return;
    }

    final ExclusionCondition existingMatchAll = conditions.stream()
        .filter(c -> c.matching().doesMatchAll())
        .findFirst().orElse(null);

    if (existingMatchAll == null) {
      conditions.addAll(newConditions);
      return;
    }

    final Set<BitPattern> union = new LinkedHashSet<>(existingMatchAll.unmatching());
    union.addAll(newDoesMatchAll.unmatching());

    final ExclusionCondition combined =
        new ExclusionCondition(existingMatchAll.matching(), union);

    conditions.removeIf(c -> c.matching().doesMatchAll());
    conditions.add(combined);

    newConditions.removeIf(c -> c.matching().doesMatchAll());
    conditions.addAll(newConditions);
  }
}
