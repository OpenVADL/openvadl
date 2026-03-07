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

package vadl.lsp;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Tracks dependencies between items. You can set an item's dependencies and then request another
 * item's dependents (i.e. every item that is dependening on it).
 *
 * <p>Note: This is effectively a bidirectional multimap. Custom terms are used to avoid
 * misunderstandings in the usage context.
 *
 * @param <T> Item type to track dependencies for
 */
public class DependencyMap<T> {
  /**
   * Maps items to their dependencies.
   */
  private final Map<T, Set<T>> dependencies = new HashMap<>();
  /**
   * Maps items to their dependents - the inverse of {@code dependencies}.
   */
  private final Map<T, Set<T>> dependents = new HashMap<>();

  /**
   * Overwrites an item's dependencies.
   *
   * @param newDependencies All existing dependencies for {@code item} that are not contained herein
   *                        are removed. If {@code item} is contained herein it is ignored - i.e.
   *                        an item never depends on itself.
   */
  public synchronized void setDependencies(T item, Set<T> newDependencies) {
    Set<T> itemSet = dependencies.get(item);
    if (itemSet == null) {
      itemSet = new HashSet<>();
      dependencies.put(item, itemSet);

    } else {
      // Remove all outdated dependencies
      var iter = itemSet.iterator();
      while (iter.hasNext()) {
        T d = iter.next();
        if (!newDependencies.contains(d)) {
          iter.remove();
          Set<T> ds = dependents.get(d);
          if (ds != null) {
            ds.remove(item);
          }
        }
      }
    }

    // Add new dependencies
    for (T d : newDependencies) {
      if (item.equals(d)) {
        // Ignore self-dependencies
        continue;
      }
      if (itemSet.add(d)) {
        Set<T> ds = dependents.computeIfAbsent(d, k -> new HashSet<>());
        ds.add(item);
      }
    }
  }

  /**
   * Returns {@code item}'s current dependents (i.e. other items that depend on it).
   */
  public synchronized Set<T> getDependents(T item) {
    var d = dependents.get(item);
    if (d == null) {
      return Set.of();
    }
    return Set.copyOf(d);
  }
}
