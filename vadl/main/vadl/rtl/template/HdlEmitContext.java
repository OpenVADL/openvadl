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
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import vadl.rtl.ipg.InstructionProgressGraph;
import vadl.viam.InstructionSetArchitecture;
import vadl.viam.MicroArchitecture;
import vadl.viam.Processor;
import vadl.viam.Signal;
import vadl.viam.Specification;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ReadResourceNode;
import vadl.viam.graph.dependency.ReadSignalNode;
import vadl.viam.graph.dependency.WriteResourceNode;
import vadl.viam.graph.dependency.WriteSignalNode;

/**
 * Emit context for HDL generation.
 *
 * @param viam        VIAM specification
 * @param isa         ISA definition
 * @param mia         MiA definition
 * @param processor   Processor definition
 * @param inlineMap   Inline map from the {@link vadl.rtl.passes.MiaMappingInlinePass}
 * @param resetVector optional external signal that provides the reset vector for pc reset
 * @param keepSignals mark signals to not be optimized or removed
 */
public record HdlEmitContext(
    Specification viam,
    InstructionSetArchitecture isa,
    MicroArchitecture mia,
    @Nullable Processor processor,
    @Nullable vadl.vdt.model.Node vdt,
    BiMap<Node, Node> inlineMap,
    @Nullable Signal resetVector,
    boolean keepSignals
) {

  public Optional<Node> ipgNode(Node inlinedNode) {
    return Optional.ofNullable(inlineMap.inverse().get(inlinedNode));
  }

  /**
   * Get the IPG node context of an inlined node.
   *
   * @param inlinedNode node inlined in a stage.
   * @return IPG node context
   */
  public Optional<InstructionProgressGraph.NodeContext> ipgContext(Node inlinedNode) {
    return ipgNode(inlinedNode).map(node -> {
      if (node.ensureGraph() instanceof InstructionProgressGraph ipg) {
        return ipg.getContext(node);
      }
      return null;
    });
  }

  public String name(Node inlinedNode, Set<String> existing, @Nullable String fallback) {
    return name(Set.of(inlinedNode), existing, fallback);
  }

  /**
   * Get a new name for a set of nodes inlined in a stage. Gets the first name we can find
   * in the IPG, return fallback otherwise.
   *
   * @param inlinedNodes set of nodes inlined in stages
   * @param existing existing names
   * @param fallback fallback name
   * @return name for inlined nodes
   */
  public String name(Collection<Node> inlinedNodes, Set<String> existing,
                     @Nullable String fallback) {

    if (inlinedNodes.isEmpty()) {
      if (fallback == null) {
        throw new IllegalArgumentException("inlinedNode and fallback is null");
      }
      return fallback;
    }
    return inlinedNodes.stream()
        .map(inlinedNode -> ipgContext(inlinedNode)
            .map(nodeContext ->
                nodeContext.shortestNameHint(existing, 30)
                    .or(() -> fallback(inlinedNode, existing))
                    .orElse("n_" + nodeContext.node().id().numericId()))
            .filter(name -> !existing.contains(name))
            .orElseGet(() -> {
              if (fallback == null || existing.contains(fallback)) {
                return fallback(inlinedNode, existing)
                    .orElse("sig_" + inlinedNode.id.numericId());
              }
              return fallback;
            }))
        .findFirst().get();
  }

  private Optional<String> fallback(Node node, Set<String> existing) {
    if (node instanceof ReadResourceNode r) {
      var prefix = (r instanceof ReadSignalNode) ? "" : "read_";
      var name = prefix + r.resourceDefinition().simpleName();
      return Optional.of(suffix(name, existing));
    }
    if (node instanceof WriteResourceNode w) {
      var prefix = (w instanceof WriteSignalNode) ? "" : "write_";
      var name = prefix + w.resourceDefinition().simpleName();
      return Optional.of(suffix(name, existing));
    }
    return Optional.empty();
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
