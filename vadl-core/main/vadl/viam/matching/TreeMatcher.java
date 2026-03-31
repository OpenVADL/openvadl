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

package vadl.viam.matching;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;
import vadl.viam.graph.Node;

/**
 * This class tries to match the given {@link Matcher} on a given {@link List} of {@link Node}.
 */
public class TreeMatcher {
  /**
   * Returns the a {@link List} of {@link Node} when {@code nodes} matches the given
   * {@link Matcher}. The method will run every matcher and not stop until the first was found.
   *
   * @param supplier gives the {@link TreeMatcher} a fresh stream for every run.
   * @param matchers a set of {@link Matcher} which is checked for every {@link Node} given by
   *                 the {@code supplier}.
   * @return a {@link List} of {@link Node} where each node matches the {@link Matcher}.
   */
  public static List<Node> matches(Supplier<Stream<Node>> supplier, Set<Matcher> matchers) {
    // This arraylist stores all the nodes which were returned successfully by the matcher.
    var result = new ArrayList<Node>();

    for (var matcher : matchers) {
      result.addAll(matches(supplier.get(), matcher));
    }

    return result;
  }

  /**
   * Returns the a {@link List} of {@link Node} when {@code nodes} matches the given
   * {@link Matcher}.
   *
   * @param nodes   which will be checked by the {@code matcher}.
   * @param matcher which checks whether a tree is considered matched.
   * @return a {@link List} of {@link Node} where each node matches the {@link Matcher}.
   */
  public static List<Node> matches(Stream<Node> nodes, Matcher matcher) {
    // Because cycles are allowed, we track all visited nodes to avoid computation.
    var visited = new HashSet<Node>();
    // This arraylist stores all the nodes which were returned successfully by the matcher.
    var result = new ArrayList<Node>();

    // Iterate over all the nodes
    // 1. If the node was not visited -> if visited skip
    // 2. Check whether it matches
    // 3. Mark as visited
    // 4. Store result if it matches
    nodes
        .filter(Objects::nonNull)
        .forEach(node -> {
          if (!visited.contains(node)) {
            var matches = matcher.matches(node);
            visited.add(node);

            if (matches) {
              result.add(node);
            }
          }
        });


    return result;
  }
}
