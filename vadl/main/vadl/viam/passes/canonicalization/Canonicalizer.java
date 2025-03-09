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

package vadl.viam.passes.canonicalization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vadl.viam.graph.Canonicalizable;
import vadl.viam.graph.Graph;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.passes.GraphProcessor;

/**
 * Applies the canonical form of nodes to the graph.
 * Either it is applied to the whole graph, so the canonical form of all nodes is evaluated
 * and applied. Or it is applied to a subgraph (a node and all its inputs).
 */
public class Canonicalizer extends GraphProcessor<Node> {

  private static final Logger log = LoggerFactory.getLogger(Canonicalizer.class);

  /**
   * Applies the canonicalization on the whole graph.
   * It traverses all nodes (from leaves to roots) and replaces them by their
   * canonical form.
   */
  public static void canonicalize(Graph graph) {
    new Canonicalizer()
        .processGraph(graph,
            // only get nodes that are not used (root nodes)
            node -> node.usageCount() == 0);
  }

  /**
   * Applies the canonicalization on a subgraph.
   * It traverses all inputs (from leaves to the given node) and replaces them by their
   * canonical form.
   *
   * @return The replacement of the subgraph root / node argument
   */
  public static Node canonicalizeSubGraph(Node node) {
    node.ensure(node.isActive(), "cannot canonicalize in active node");
    return new Canonicalizer()
        .processNode(node);
  }


  @Override
  protected Node processUnprocessedNode(Node toProcess) {
    // first visit all inputs to receive their canonical form
    toProcess.visitInputs(this);

    if (toProcess instanceof Canonicalizable) {
      // retrieve the canonical form of node
      var canonicalNode = ((Canonicalizable) toProcess).canonical();

      if (canonicalNode != toProcess) {

        if (canonicalNode instanceof ConstantNode node) {
          log.info("{}:\t{}", toProcess.id, node.constant());
        }

        // replace original one by new one
        toProcess.replaceAndDelete(canonicalNode);
      }
      return canonicalNode;
    }

    // by default return original node
    return toProcess;
  }

}
