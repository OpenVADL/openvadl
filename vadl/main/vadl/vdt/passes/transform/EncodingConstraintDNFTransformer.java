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

import vadl.types.BuiltInTable;
import vadl.utils.GraphUtils;
import vadl.utils.VadlBuiltInEmptyNoStatusDispatcher;
import vadl.viam.graph.Graph;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * Transformer that converts a graph representing an encoding constraint into Disjunctive Normal
 * Form (DNF).
 */
public class EncodingConstraintDNFTransformer implements
    VadlBuiltInEmptyNoStatusDispatcher<BuiltInCall> {

  private final Graph graph;

  /**
   * Constructs a new EncodingConstraintDNFTransformer with the given graph.
   *
   * @param graph The graph to transform. It must be quantifier-free and in Negation Normal Form
   *              (NNF).
   */
  public EncodingConstraintDNFTransformer(Graph graph) {
    this.graph = graph.copy("Disjunctive Normal Form");
  }

  /**
   * Transforms the given graph into Disjunctive Normal Form (DNF).
   *
   * @return A new graph in DNF.
   */
  public Graph transform() {
    var root = GraphUtils.getSingleNode(this.graph, ReturnNode.class).value();
    handleNode(root);
    return graph;
  }

  private void handleNode(ExpressionNode node) {

    if (node instanceof BuiltInCall bc) {
      dispatch(bc, bc.builtIn());
    }

    // No transformation needed for other node types
  }

  /**
   * Distribute AND over OR to convert the graph into Disjunctive Normal Form (DNF).
   *
   * @param input The input AND node.
   */
  @Override
  public void handleAND(BuiltInCall input) {

    var args = input.arguments();
    if (args.size() < 2) {
      return; // AND must have at least two arguments
    }

    var leftArg = args.getFirst();
    var rightArg = args.getLast();

    // Distribute the right over the left (AND over OR)
    if (leftArg instanceof BuiltInCall left && OR.equals(left.builtIn())) {

      var replacement = BuiltInCall.of(OR, left.arguments().stream()
          .map(arg -> BuiltInCall.of(BuiltInTable.AND, arg, rightArg))
          .toArray(BuiltInCall[]::new));

      input.replaceAndDelete(replacement);
      handleNode(replacement);
      return;
    }

    if (!(rightArg instanceof BuiltInCall right && OR.equals(right.builtIn()))) {
      // Nothing to distribute, just recursively handle arguments
      handleNode(leftArg);
      handleNode(rightArg);
      return;
    }

    // Distribute the left over the right (AND over OR)
    var replacement = BuiltInCall.of(OR, right.arguments().stream()
        .map(arg -> BuiltInCall.of(BuiltInTable.AND, leftArg, arg))
        .toArray(BuiltInCall[]::new));
    input.replaceAndDelete(replacement);
    handleNode(replacement);
  }

  /**
   * Recursively transform the graph into DNF.
   *
   * @param input The input OR node.
   */
  @Override
  public void handleOR(BuiltInCall input) {
    handleNode(input.arg(0));
    handleNode(input.arg(1));
  }
}
