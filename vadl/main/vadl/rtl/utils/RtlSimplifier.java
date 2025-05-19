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

import java.util.List;
import vadl.rtl.ipg.InstructionProgressGraph;
import vadl.rtl.map.MiaMapping;
import vadl.viam.graph.Graph;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.passes.algebraic_simplication.AlgebraicSimplifier;
import vadl.viam.passes.algebraic_simplication.rules.AlgebraicSimplificationRule;
import vadl.viam.passes.canonicalization.Canonicalizer;

/**
 * Simplifier for RTL generation, extending the {@link AlgebraicSimplifier}.
 *
 * <p>The simplifier applies the simplification rules in a loop with the following steps:
 * <li>Canonicalization
 * <li>Apply simplification rules
 * <li>Deduplicate nodes (some RTL simplification rules can introduce duplicates)
 */
public class RtlSimplifier extends AlgebraicSimplifier {

  public RtlSimplifier(List<AlgebraicSimplificationRule> rules) {
    super(rules);
  }

  @Override
  public int run(Graph graph) {
    var changes = 1;
    var total = 0;
    while (changes > 0) {
      Canonicalizer.canonicalize(graph);
      changes = super.run(graph);
      changes += dedupNodes(graph);
      total += changes;
    }
    return total;
  }

  /**
   * Run simplification rules on the instruction progress graph. Keep the MiA mapping updated
   * when nodes are removed/replaced.
   *
   * @param ipg instruction progress graph
   * @param mapping MiA mapping
   */
  public void run(InstructionProgressGraph ipg, MiaMapping mapping) {
    run(ipg);

    // clean up mapping (deleted nodes, mapping of constant nodes)
    for (MiaMapping.NodeContext context : mapping.contexts().values()) {

      // remove deleted nodes (from optimization)
      context.ipgNodes().removeIf(Node::isDeleted);
      context.fixedIpgNodes().removeIf(Node::isDeleted);

      // add constant nodes only to mapping with usage (remove otherwise)
      ipg.getNodes(ConstantNode.class).forEach(constantNode -> {
        if (constantNode.usages().anyMatch(context.ipgNodes()::contains)) {
          context.ipgNodes().add(constantNode);
        } else {
          context.ipgNodes().remove(constantNode);
          context.fixedIpgNodes().remove(constantNode);
        }
      });
    }
  }

  private int dedupNodes(Graph graph) {
    var changes = 1;
    var total = 0;
    while (changes > 0) {
      changes = 0;
      for (Node node : graph.getNodes().toList()) {
        if (node.isDeleted() || node.inputs().findAny().isEmpty()) {
          continue;
        }
        var dup = graph.findDuplicate(node);
        if (dup != null) {
          dup.replaceAndDelete(node);
          changes++;
        }
      }
      total += changes;
    }
    return total;
  }
}
