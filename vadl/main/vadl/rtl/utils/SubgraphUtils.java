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

package vadl.rtl.utils;

import com.google.common.collect.Streams;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import vadl.viam.graph.Graph;
import vadl.viam.graph.Node;

/**
 * Utils for copying subgraphs.
 */
public class SubgraphUtils {

  /**
   * Supplier for missing nodes (inputs or usages) during subgraph copy.
   */
  public interface MissingSupplier {
    /**
     * Supplier for a missing input or usage during subgraph copy.
     * If null is supplied for an input, a copy of the original target is kept in the destination
     * graph. If null is supplied for a usage, the destination graph will not include this usage.
     *
     * @param originalFrom source node in the original graph
     * @param originalTo target of input or usage in the original graph
     * @param copyFrom source node in the destination graph
     * @return node that should serve as input to/usage of copyFrom in the destination graph
     */
    @Nullable
    Node supply(Node originalFrom, Node originalTo, Node copyFrom);
  }

  /**
   * Copy a subset of nodes into a destination graph.
   *
   * @param dest destination graph
   * @param set subset of nodes (all from the same graph)
   * @param missingInput supplier for input nodes missing from the set
   * @param missingUsage supplier for usage nodes missing from the set
   * @return map from nodes in the set to the corresponding nodes in the destination graph
   */
  public static Map<Node, Node> copy(Graph dest, Set<Node> set, MissingSupplier missingInput,
                                     MissingSupplier missingUsage) {
    var cache = new HashMap<Node, Node>();

    set.stream().filter(n -> n.usages().noneMatch(set::contains))
        .forEach(root -> {
          var copy = dest.addWithInputs(root.copy());
          patchInputs(root, copy, set, missingInput, cache);
        });

    set.forEach(node -> {
      node.usages().filter(u -> !set.contains(u))
          .forEach(usage -> {
            if (usage != null) {
              var newUsage = missingUsage.supply(
                  node, usage, Objects.requireNonNull(cache.get(node)));
              if (newUsage != null) {
                dest.addWithInputs(newUsage);
              }
            }
          });
    });

    return cache;
  }

  private static void patchInputs(Node original, Node copy, Set<Node> set,
                                  MissingSupplier missingInput, Map<Node, Node> cache) {
    cache.put(original, copy);

    Streams.forEachPair(original.inputs(), copy.inputs(), (originalInput, copyInput) -> {
      if (set.contains(originalInput)) {
        patchInputs(originalInput, copyInput, set, missingInput, cache);
      } else {
        var newInput = missingInput.supply(original, originalInput, copy);
        if (newInput != null) {
          newInput = copy.ensureGraph().addWithInputs(newInput);
          copy.replaceInput(copyInput, newInput);
          if (copyInput.isActive() && !copyInput.hasUsages()) {
            copyInput.safeDelete();
          }
        }
      }
    });
  }

}
