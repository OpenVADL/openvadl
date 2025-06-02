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
import vadl.types.BuiltInTable;
import vadl.utils.GraphUtils;
import vadl.utils.VadlBuiltInEmptyNoStatusDispatcher;
import vadl.vdt.passes.validate.EncodingConstraintValidator;
import vadl.viam.graph.Graph;
import vadl.viam.graph.Node;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * Transformer that converts a graph representing an encoding constraint into Negation Normal Form
 * (NNF).
 */
public class EncodingConstraintNNFTransformer
    implements VadlBuiltInEmptyNoStatusDispatcher<BuiltInCall> {

  private final Graph graph;

  /**
   * Constructs a new EncodingConstraintNNFTransformer with the given graph.
   *
   * @param graph The graph to transform. It must be quantifier-free and only contain AND, OR, NOT
   *              operators, as well as only format field references (or slices thereof) and
   *              constants. See {@link EncodingConstraintValidator} for more details on the
   *              expected format.
   */
  public EncodingConstraintNNFTransformer(Graph graph) {
    this.graph = graph.copy("Negation Normal Form");
  }

  /**
   * Transforms the given graph into Negation Normal Form (NNF).
   *
   * @return A new graph in NNF.
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
   * Recursively transform the graph into NNF.
   *
   * @param input The input AND node.
   */
  @Override
  public void handleAND(BuiltInCall input) {
    handleNode(input.arg(0));
    handleNode(input.arg(1));
  }

  /**
   * Recursively transform the graph into NNF.
   *
   * @param input The input OR node.
   */
  @Override
  public void handleOR(BuiltInCall input) {
    handleNode(input.arg(0));
    handleNode(input.arg(1));
  }

  /**
   * Recursively transform the graph into NNF.
   *
   * @param input The input NOT node.
   */
  @Override
  public void handleNOT(BuiltInCall input) {

    final ExpressionNode arg = input.arg(0);

    if (!(arg instanceof BuiltInCall bc)) {
      // If the argument is not a built-in call, we cannot transform it further.
      // This should not happen in a well-formed encoding constraint.
      throw new IllegalStateException("Unexpected argument type: " + arg.getClass());
    }

    if (NOT.equals(bc.builtIn())) {
      // Remove double negation
      final ExpressionNode innerArg = bc.arg(0);
      input.replaceAndDelete(innerArg);
      handleNode(innerArg);
      return;
    }

    // Apply De Morgan Laws
    if (AND.equals(bc.builtIn())) {
      final ExpressionNode orNode = BuiltInCall.of(OR,
          BuiltInCall.of(BuiltInTable.NOT, bc.arg(0)),
          BuiltInCall.of(BuiltInTable.NOT, bc.arg(1)));
      input.replaceAndDelete(orNode);
      handleNode(orNode);
      return;
    }

    if (OR.equals(bc.builtIn())) {
      final ExpressionNode andNode = BuiltInCall.of(AND,
          BuiltInCall.of(BuiltInTable.NOT, bc.arg(0)),
          BuiltInCall.of(BuiltInTable.NOT, bc.arg(1)));
      input.replaceAndDelete(andNode);
      handleNode(andNode);
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

      input.replaceAndDelete(replacement);
      return;
    }

    // If we reach here, we have an unsupported built-in call.
    // This should not happen in a well-formed encoding constraint.
    throw new IllegalStateException("Unsupported built-in call: " + bc.builtIn());
  }
}
