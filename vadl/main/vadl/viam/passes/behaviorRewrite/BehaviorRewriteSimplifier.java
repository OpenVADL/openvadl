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

package vadl.viam.passes.behaviorRewrite;

import java.util.List;
import java.util.Optional;
import vadl.utils.Pair;
import vadl.viam.graph.Graph;
import vadl.viam.graph.Node;
import vadl.viam.passes.algebraic_simplication.AlgebraicSimplifier;
import vadl.viam.passes.behaviorRewrite.rules.BehaviorRewriteSimplificationRule;

/**
 * This class contains the main driver logic to simplify behavior expressions which
 * are not covered by {@link AlgebraicSimplifier}.
 * When instantiating a new object, the user can give a list of rules which should be applied on
 * each node.
 * Usually, this list will be a static list in the {@link BehaviorRewritePass}. However,
 * it might be the case that the lcb (or others) requires special nodes. These rules with
 * non VIAM nodes should not be applied when running the {@link BehaviorRewritePass}.
 * The passes with special requirements can individually define which
 * {@link BehaviorRewritePass} applies.
 */
public class BehaviorRewriteSimplifier {
  private final List<BehaviorRewriteSimplificationRule> rules;

  public BehaviorRewriteSimplifier(List<BehaviorRewriteSimplificationRule> rules) {
    this.rules = rules;
  }

  /**
   * Apply algebraic simplification as long as something changes on the given {@link Graph}.
   *
   * @param graph where the simplification should be applied on.
   */
  public void run(Graph graph) {
    rules.forEach(rule -> {
      boolean hasChanged;

      do {
        hasChanged = false;

        var result = graph.getNodes()
            .filter(Node::isActive)
            // When `normalize` returns an Optional
            // then create a `Pair`
            .map(node -> rule.simplify(node).map(y -> new Pair<>(node, y)))
            .flatMap(Optional::stream)
            .toList();

        for (var pair : result) {
          var oldNode = pair.left();
          var newNode = pair.right();

          oldNode.replaceAndDelete(newNode);
          hasChanged = true;
        }
      } while (hasChanged);
    });
  }
}
