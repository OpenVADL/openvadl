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
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.passes.algebraic_simplication.AlgebraicSimplifier;
import vadl.viam.passes.algebraic_simplication.rules.AlgebraicSimplificationRule;

public class RtlSimplifier extends AlgebraicSimplifier {

  public RtlSimplifier(List<AlgebraicSimplificationRule> rules) {
    super(rules);
  }

  /**
   * Run simplification rules on the instruction progress graph. Keep the MiA mapping updated
   * when nodes are removed/replaced.
   *
   * @param ipg instruction progress graph
   * @param mapping MiA mapping
   */
  public void run(InstructionProgressGraph ipg, MiaMapping mapping) {
    super.run(ipg);

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
}
