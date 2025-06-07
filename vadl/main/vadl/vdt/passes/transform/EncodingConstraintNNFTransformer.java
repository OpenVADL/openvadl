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

import static vadl.types.BuiltInTable.AND;
import static vadl.types.BuiltInTable.EQU;
import static vadl.types.BuiltInTable.NEQ;
import static vadl.types.BuiltInTable.NOT;
import static vadl.types.BuiltInTable.OR;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import vadl.types.BuiltInTable;
import vadl.utils.GraphUtils;
import vadl.viam.graph.Graph;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * Transformer that converts a graph representing an encoding constraint into Negation Normal Form
 * (NNF).
 */
public class EncodingConstraintNNFTransformer {

  private EncodingConstraintNNFTransformer() {
    // Utility class
  }

  /**
   * Transforms the given graph into Negation Normal Form (NNF).
   * The graph must be quantifier-free and only contain AND, OR, NOT operators.
   * Atomic formulas must be EQ or NEQ.
   *
   * @param graph The graph to transform.
   * @return A new graph in NNF.
   */
  public static Graph transform(Graph graph) {

    final Graph nnfGraph = graph.copy("Negation Normal Form");

    boolean stable;
    do {
      stable = replaceNot(nnfGraph);
    } while (!stable);

    return nnfGraph;
  }

  /**
   * Apply the replacement rules for NOT operators in the graph.
   *
   * @param graph The graph to transform.
   * @return True if the graph is stable (no further changes), false otherwise.
   */
  private static boolean replaceNot(Graph graph) {

    final AtomicBoolean stable = new AtomicBoolean(true);

    GraphUtils.getNodes(graph, BuiltInCall.class).stream()
        .filter(n -> BuiltInTable.NOT.equals(n.builtIn()))
        .forEach(notNode -> {

          final ExpressionNode arg = notNode.arg(0);

          if (!(arg instanceof BuiltInCall bc)) {
            // If the argument is not a built-in call, we cannot transform it further.
            // This should not happen in a well-formed encoding constraint.
            throw new IllegalStateException("Unexpected argument type: " + arg.getClass());
          }

          if (NOT.equals(bc.builtIn())) {
            // Will be resolved in a future iteration
            stable.set(false);
            return;
          }

          // Push NOT down to the predicates
          if (Set.of(EQU, NEQ).contains(bc.builtIn())) {
            final Node replacement;
            if (EQU.equals(bc.builtIn())) {
              replacement = BuiltInCall.of(NEQ, bc.arg(0), bc.arg(1));
            } else {
              replacement = BuiltInCall.of(EQU, bc.arg(0), bc.arg(1));
            }

            notNode.replaceAndDelete(replacement);
            return;
          }

          // Apply De Morgan Laws
          if (AND.equals(bc.builtIn())) {
            final Node orNode = BuiltInCall.of(OR,
                BuiltInCall.of(BuiltInTable.NOT, bc.arg(0)),
                BuiltInCall.of(BuiltInTable.NOT, bc.arg(1)));
            notNode.replaceAndDelete(orNode);
            stable.set(false);
            return;
          }

          if (OR.equals(bc.builtIn())) {
            final Node orNode = BuiltInCall.of(AND,
                BuiltInCall.of(BuiltInTable.NOT, bc.arg(0)),
                BuiltInCall.of(BuiltInTable.NOT, bc.arg(1)));
            notNode.replaceAndDelete(orNode);
            stable.set(false);
            return;
          }

          // If we reach here, we have an unsupported built-in call.
          throw new IllegalStateException("Unsupported built-in call: " + bc.builtIn());

        });

    return stable.get();
  }
}
