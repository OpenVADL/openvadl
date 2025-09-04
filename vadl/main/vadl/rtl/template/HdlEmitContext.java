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
import vadl.viam.Signal;
import vadl.viam.Specification;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ReadResourceNode;
import vadl.viam.graph.dependency.ReadSignalNode;
import vadl.viam.graph.dependency.WriteResourceNode;
import vadl.viam.graph.dependency.WriteSignalNode;

public record HdlEmitContext(
    Specification viam,
    InstructionSetArchitecture isa,
    MicroArchitecture mia,
    BiMap<Node, Node> inlineMap,
    Signal resetVector
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

  public String name(@Nullable Node inlinedNode, Set<String> existing, @Nullable String fallback) {
    if (inlinedNode == null) {
      if (fallback == null) {
        throw new IllegalArgumentException("inlinedNode and fallback is null");
      }
      return fallback;
    }
    return ipgContext(inlinedNode)
        .flatMap(nodeContext -> nodeContext.shortestNameHint(existing, 30))
        .orElseGet(() -> {
          if (fallback == null || existing.contains(fallback)) {
            return fallback(inlinedNode, existing);
          }
          return fallback;
        });
  }

  public String fallback(Node node, Set<String> existing) {
    if (node instanceof ReadResourceNode r) {
      var prefix = (r instanceof ReadSignalNode) ? "" : "read_";
      var name = prefix + r.resourceDefinition().simpleName();
      return suffix(name, existing);
    }
    if (node instanceof WriteResourceNode w) {
      var prefix = (w instanceof WriteSignalNode) ? "" : "write_";
      var name = prefix + w.resourceDefinition().simpleName();
      return suffix(name, existing);
    }
    return "_n_" + node.id.numericId();
  }

  private String suffix(String name, Set<String> existing) {
    if (!existing.contains(name)) {
      return name;
    }
    int i = 0;
    while (existing.contains(name + "_" + i)) {
      i++;
    }
    return name + "_" + i;
  }

}
