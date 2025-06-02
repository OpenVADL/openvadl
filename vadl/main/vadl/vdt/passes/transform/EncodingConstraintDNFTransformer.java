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

package vadl.vdt.passes.transform;

import static vadl.types.BuiltInTable.OR;

import java.util.concurrent.atomic.AtomicBoolean;
import vadl.types.BuiltInTable;
import vadl.utils.GraphUtils;
import vadl.viam.graph.Graph;
import vadl.viam.graph.dependency.BuiltInCall;

/**
 * Transformer that converts a graph representing an encoding constraint into Disjunctive Normal
 * Form (DNF).
 */
public class EncodingConstraintDNFTransformer {

  private EncodingConstraintDNFTransformer() {
    // Utility class
  }

  /**
   * Transforms the given graph into Disjunctive Normal Form (DNF).
   * The graph must be quantifier-free and should be already in Negation Normal Form (NNF).
   * Atomic formulas must be EQ or NEQ.
   *
   * @param graph The graph to transform.
   * @return A new graph in NNF.
   */
  public static Graph transform(Graph graph) {

    final Graph dnfGraph = graph.copy("Disjunctive Normal Form");

    boolean stable;
    do {
      stable = distribute(dnfGraph);
    } while (!stable);

    return dnfGraph;
  }

  /**
   * Apply the distribution rules for equivalence-preserving DNF transformation.
   *
   * @param graph The graph to transform.
   * @return True if the graph is stable (no further changes), false otherwise.
   */
  private static boolean distribute(Graph graph) {

    final AtomicBoolean stable = new AtomicBoolean(true);

    GraphUtils.getNodes(graph, BuiltInCall.class).stream()
        .filter(n -> BuiltInTable.AND.equals(n.builtIn()))
        .forEach(andNode -> {

          var args = andNode.arguments();
          if (args.size() < 2) {
            return; // AND must have at least two arguments
          }

          var leftArg = args.getFirst();
          var rightArg = args.getLast();

          // Distribute the right over the left (AND over OR)
          if (leftArg instanceof BuiltInCall left && OR.equals(left.builtIn())) {

            andNode.replaceAndDelete(
                BuiltInCall.of(OR, left.arguments().stream()
                    .map(arg -> BuiltInCall.of(BuiltInTable.AND, arg, rightArg))
                    .toArray(BuiltInCall[]::new))
            );

            stable.set(false);
            return;
          }

          if (!(rightArg instanceof BuiltInCall right && OR.equals(right.builtIn()))) {
            return; // Nothing to distribute
          }

          // Distribute the left over the right (AND over OR)
          andNode.replaceAndDelete(
              BuiltInCall.of(OR, right.arguments().stream()
                  .map(arg -> BuiltInCall.of(BuiltInTable.AND, leftArg, arg))
                  .toArray(BuiltInCall[]::new))
          );
          stable.set(false);

        });

    return stable.get();
  }
}
