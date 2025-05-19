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

import java.util.Collection;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import vadl.rtl.ipg.InstructionProgressGraph;
import vadl.types.BuiltInTable;
import vadl.viam.Definition;
import vadl.viam.DefinitionExtension;
import vadl.viam.MicroArchitecture;
import vadl.viam.Stage;
import vadl.viam.ViamError;
import vadl.viam.graph.Node;
import vadl.viam.graph.ViamGraphError;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.MiaBuiltInCall;
import vadl.viam.graph.dependency.SideEffectNode;

/**
 * The MiA mapping holds a context for every node in stage behaviors that maps to parts of the
 * instruction progress graph. For this it holds a set of references to the nodes in the instruction
 * progress graph.
 */
public class MiaMapping extends DefinitionExtension<MicroArchitecture> {

  private final MicroArchitecture mia;

  private final InstructionProgressGraph ipg;

  private final IdentityHashMap<Node, NodeContext> contexts = new IdentityHashMap<>();

  public MiaMapping(MicroArchitecture mia, InstructionProgressGraph ipg) {
    this.mia = mia;
    this.ipg = ipg;
  }

  public MicroArchitecture mia() {
    return mia;
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

  /**
   * Get stage IPG nodes.
   *
   * @param stage stage to filter for
   * @return stream of nodes that are mapped to the stage
   */
  public Stream<Node> stageIpgNodes(Stage stage) {
    return stageContexts(stage).flatMap(context -> context.ipgNodes.stream());
  }

  /**
   * Get stage inputs.
   *
   * @param stage stage to filter for
   * @return stream of expression nodes that are inputs to the stage
   */
  public Stream<ExpressionNode> stageInputs(Stage stage) {
    return stageIpgNodes(stage)
        .flatMap(Node::inputs)
        .filter(ExpressionNode.class::isInstance).map(ExpressionNode.class::cast)
        .filter(node -> !containsInStage(stage, node))
        .distinct();
  }

  /**
   * Get stage outputs.
   *
   * @param stage stage to filter for
   * @return stream of expression nodes that are outputs from the stage
   */
  public Stream<ExpressionNode> stageOutputs(Stage stage) {
    return stageIpgNodes(stage)
        .filter(ExpressionNode.class::isInstance).map(ExpressionNode.class::cast)
        .filter(node -> node.usages().anyMatch(u -> !containsInStage(stage, u)));
  }

  /**
   * Find all node contexts for a given IPG node.
   *
   * @param ipgNode IPG node
   * @return stream of all node contexts
   */
  public Stream<NodeContext> findContexts(Node ipgNode) {
    return contexts.values().stream().filter(context -> context.ipgNodes.contains(ipgNode));
  }

  /**
   * Find any node context for a given IPG node.
   *
   * @param ipgNode IPG node
   * @return node context, if any
   */
  public Optional<NodeContext> findContext(Node ipgNode) {
    return findContexts(ipgNode).findFirst();
  }

  /**
   * Find a unique context for a given IPG node.
   *
   * @param ipgNode IPG node
   * @return unique context, or {@link Optional#empty()} if none or not unique
   */
  public Optional<Stage> findStageUnique(Node ipgNode) {
    var list = contexts.values().stream()
        .filter(context -> context.ipgNodes.contains(ipgNode))
        .map(NodeContext::stage).distinct().toList();
    if (list.size() == 1) {
      return Optional.of(list.get(0));
    }
    return Optional.empty();
  }

  /**
   * Find any node context for a given IPG node, fail if not present.
   *
   * @param ipgNode IPG node
   * @return node context
   */
  public NodeContext ensureContext(Node ipgNode) {
    return findContext(ipgNode).orElseThrow(() ->
        new ViamGraphError("IPG node has no context in MiA mapping").addContext(ipgNode));
  }

  /**
   * Remove an IPG node from all node contexts.
   *
   * @param ipgNode IPG node to remove
   */
  public void removeNode(Node ipgNode) {
    contexts().forEach((miaNode, context) -> {
      context.fixedIpgNodes().remove(ipgNode);
      context.ipgNodes().remove(ipgNode);
    });
  }

  /**
   * Get node context belonging to the decode mapping.
   *
   * @return context of decode mapping, if any
   */
  public Optional<NodeContext> decode() {
    return contexts.values().stream().filter(NodeContext::isDecode).findFirst();
  }

  /**
   * Get node context belonging to the decode mapping. Fail if not present.
   *
   * @return context of decode mapping
   */
  public NodeContext ensureDecode() {
    return decode().orElseThrow(
        () -> new ViamError("Missing decode builtin call in micro architecture"));
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

    /**
     * Check if this node context belongs to a MiA builtin call to {@link BuiltInTable#DECODE}.
     *
     * @return true if decode context
     */
    public boolean isDecode() {
      return (node instanceof MiaBuiltInCall b && b.builtIn() == BuiltInTable.DECODE);
    }
  }

  @Override
  public Class<? extends Definition> extendsDefClass() {
    return MicroArchitecture.class;
  }

}
