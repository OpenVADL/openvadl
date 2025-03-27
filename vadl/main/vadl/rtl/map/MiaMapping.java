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

package vadl.rtl.map;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import vadl.rtl.ipg.InstructionProgressGraph;
import vadl.viam.Definition;
import vadl.viam.DefinitionExtension;
import vadl.viam.MicroArchitecture;
import vadl.viam.Stage;
import vadl.viam.graph.Node;
import vadl.viam.graph.ViamGraphError;
import vadl.viam.graph.dependency.SideEffectNode;

/**
 * The MiA mapping holds a context for every node in stage behaviors that maps to parts of the
 * instruction progress graph. For this it holds a set of references to the nodes in the instruction
 * progress graph.
 */
public class MiaMapping extends DefinitionExtension<MicroArchitecture> {

  private final InstructionProgressGraph ipg;

  private final IdentityHashMap<Node, NodeContext> contexts = new IdentityHashMap<>();

  public MiaMapping(InstructionProgressGraph ipg) {
    this.ipg = ipg;
  }

  public InstructionProgressGraph ipg() {
    return ipg;
  }

  public IdentityHashMap<Node, NodeContext> contexts() {
    return contexts;
  }

  public Stream<NodeContext> stageContexts(Stage stage) {
    return contexts().values().stream()
        .filter(context -> stage.equals(context.stage()));
  }

  /**
   * Create new node context for a MiA node, if not already present.
   *
   * @param stage stage the node belongs to
   * @param miaNode MiA node
   * @param inputContexts node contexts that provide instruction inputs to miaNode
   * @return node context
   */
  public NodeContext createContext(Stage stage, Node miaNode, List<NodeContext> inputContexts) {
    var context = contexts.computeIfAbsent(miaNode, node -> new NodeContext(stage, node));
    context.pred().addAll(inputContexts);
    inputContexts.forEach(input -> input.succ().add(context));
    return context;
  }

  public boolean contains(Node ipgNode) {
    return contexts.values().stream().anyMatch(context -> context.ipgNodes.contains(ipgNode));
  }

  public boolean containsInStage(Stage stage, Node ipgNode) {
    return stageContexts(stage).anyMatch(context -> context.ipgNodes.contains(ipgNode));
  }

  public Optional<NodeContext> findContext(Node ipgNode) {
    return contexts.values().stream().filter(context -> context.ipgNodes.contains(ipgNode))
        .findFirst();
  }

  public NodeContext ensureContext(Node ipgNode) {
    return findContext(ipgNode).orElseThrow(() ->
        new ViamGraphError("IPG node has no context in MiA mapping").addContext(ipgNode));
  }

  /**
   * Context for a node in a stage behavior that maps to parts of the instruction progress graph
   * as part of the MiA mapping. These contexts also track the dependencies between the map nodes
   * (predecessors, successors).
   */
  public static class NodeContext {

    private final Stage stage;

    private final Node node;

    private final Set<NodeContext> pred;

    private final Set<NodeContext> succ;

    private final Set<SideEffectNode> sideEffects;

    private final Set<Node> fixedIpgNodes;

    private final Set<Node> ipgNodes;

    NodeContext(Stage stage, Node node) {
      this.stage = stage;
      this.node = node;
      this.pred = new HashSet<>();
      this.succ = new HashSet<>();
      this.sideEffects = new HashSet<>();
      this.fixedIpgNodes = new HashSet<>();
      this.ipgNodes = new HashSet<>();
    }

    /**
     * Stage this mapping context belongs to.
     *
     * @return stage
     */
    public Stage stage() {
      return stage;
    }

    /**
     * Node in stage behavior this mapping context belongs to.
     *
     * @return node
     */
    public Node node() {
      return node;
    }

    /**
     * Set of predecessors this node has in the MiA definition.
     *
     * @return set of nodes
     */
    public Set<NodeContext> pred() {
      return pred;
    }

    /**
     * Set of successors this node has in the MiA definition.
     *
     * @return set of nodes
     */
    public Set<NodeContext> succ() {
      return succ;
    }

    /**
     * Set of side effects this node is associated with in the MiA definition.
     *
     * @return set of side effect nodes
     */
    public Set<SideEffectNode> sideEffects() {
      return sideEffects;
    }

    /**
     * Set of <b>fixed</b> IPG nodes mapped to this node in the MiA definition.
     * These nodes where matched to belong to this stage directly, not just by being an input to
     * another matched node. Fixed nodes can not be moved between stages during optimization.
     *
     * @return set of IPG nodes
     */
    public Set<Node> fixedIpgNodes() {
      return fixedIpgNodes;
    }

    /**
     * Stream IPG nodes mapped to this node that are movable, i.e., they are not fixed
     * (see {@link NodeContext#fixedIpgNodes()}).
     *
     * @return set of IPG nodes
     */
    public Stream<Node> movableIpgNodes() {
      return ipgNodes.stream().filter(n -> !fixedIpgNodes.contains(n));
    }

    /**
     * Set of IPG nodes mapped to this node in the MiA definition.
     *
     * @return set of IPG nodes
     */
    public Set<Node> ipgNodes() {
      return ipgNodes;
    }
  }

  @Override
  public Class<? extends Definition> extendsDefClass() {
    return MicroArchitecture.class;
  }

}
