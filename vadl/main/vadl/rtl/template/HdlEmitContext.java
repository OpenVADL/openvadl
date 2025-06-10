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

package vadl.rtl.template;

import com.google.common.collect.BiMap;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import vadl.rtl.ipg.InstructionProgressGraph;
import vadl.viam.InstructionSetArchitecture;
import vadl.viam.MicroArchitecture;
import vadl.viam.Specification;
import vadl.viam.graph.Node;

public record HdlEmitContext(
    Specification viam,
    InstructionSetArchitecture isa,
    MicroArchitecture mia,
    BiMap<Node, Node> inlineMap
) {

  public Optional<Node> ipgNode(Node inlinedNode) {
    return Optional.ofNullable(inlineMap.inverse().get(inlinedNode));
  }

  public Optional<InstructionProgressGraph.NodeContext> ipgContext(Node inlinedNode) {
    return ipgNode(inlinedNode).map(node -> {
      if (node.ensureGraph() instanceof InstructionProgressGraph ipg) {
        return ipg.getContext(node);
      }
      return null;
    });
  }

  public String name(Node inlinedNode, Set<String> existing, @Nullable String fallback) {
    return ipgContext(inlinedNode)
        .flatMap(nodeContext -> nodeContext.shortestNameHint(existing, 20))
        .orElseGet(() -> {
          if (fallback == null || existing.contains(fallback)) {
            return "n_" + inlinedNode.id.numericId();
          }
          return fallback;
        });
  }

}
