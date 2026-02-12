// SPDX-FileCopyrightText : © 2025-2026 TU Wien <vadl@tuwien.ac.at>
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

package vadl.viam.passes.loopUnrolling;

import static java.util.Objects.requireNonNull;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import vadl.utils.GraphUtils;
import vadl.viam.graph.Graph;
import vadl.viam.graph.Node;
import vadl.viam.graph.control.AbstractEndNode;
import vadl.viam.graph.control.ControlNode;
import vadl.viam.graph.control.DirectionalNode;
import vadl.viam.graph.control.ForallEndNode;
import vadl.viam.graph.control.ForallNode;
import vadl.viam.graph.control.StartNode;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ForIdxNode;
import vadl.viam.graph.dependency.SideEffectNode;
import vadl.viam.passes.CfgTraverser;

/**
 * Loop unroller pass for VIAM graphs. Unrolls forall statements in the control flow graph.
 */
public class LoopUnroller implements CfgTraverser {

  private Graph graph;
  private final ArrayDeque<ForallNode> forallStack = new ArrayDeque<>();
  private Set<SideEffectNode> unrolledSideEffects = new HashSet<>();

  public LoopUnroller(Graph graph) {
    this.graph = graph;
  }

  /**
   * Starts the loop unrolling on the given graph.
   */
  public void run() {
    var start = GraphUtils.getSingleNode(graph, StartNode.class);
    var hasLoops = graph.getNodes(ForallNode.class).findAny().isPresent();
    if (!hasLoops) {
      // fast path: no loops found, nothing to do
      return;
    }
    traverseBranch(start);
    graph.deleteUnusedDependencies();
  }

  @Override
  public ControlNode onControlNode(ControlNode controlNode) {
    // we want to unroll the inner loops first, so we keep the unrolling iterations
    // as small as possible.
    // Therefore, we traverse the control flow and unroll a loop until we reach its
    // end node.
    // We keep track of visited forall start to have fast access to the end node's
    // corresponding start node.

    if (controlNode instanceof ForallNode forallNode) {
      forallStack.push(forallNode);
      return controlNode;
    }
    if (controlNode instanceof ForallEndNode forallEndNode) {
      var forallNode = forallStack.pop();
      var forallEndNext = forallEndNode.next();
      unrollLoop(forallNode, forallEndNode);
      // we want to continue after the replacement of the forallEnd.
      // therefore, we tell the traverser the new continuation, which is the
      // forallEnd replacement node.
      return (ControlNode) requireNonNull(forallEndNext.predecessor());
    }

    if (controlNode instanceof AbstractEndNode endNode) {
      // if we reached an abstract end node that is not a forall node,
      // we must collect and add all saved side effects from inner loops
      // and add them to the side effects of this node.
      unrolledSideEffects.forEach(endNode::addSideEffect);
      // reset all unrolled side effects
      unrolledSideEffects.clear();
    }

    return controlNode;
  }

  private void unrollLoop(ForallNode start, ForallEndNode end) {
    // 1. we collect all nodes we want to duplicate
    // (e.i. all control nodes between the start and end node).
    // 2. we create a shallow copy of all the collected nodes and store the relation
    // between the new and the old one in a cache map.
    // 3. we replace all control node references in the cache map with the new nodes.
    // 4. we repeat step 2 and 3 until we unrolled all iterations.

    // the collection of nodes that are influenced by the loop unroll
    var affectedNodes = collectNodesBetween(start, end);

    var idxNode = start.idx();
    var idxDirectUsages = idxNode.usages().collect(Collectors.toSet());

    DirectionalNode currBeforeStart = requireNonNull(start.predecessor());

    var firstInLoop = start.beginNode().next();
    var lastInLoop = end.endNode().predecessor();
    if (lastInLoop == start.beginNode()) {
      lastInLoop = null;
    }


    // for each iteration of the loop we unroll the body
    for (var i = idxNode.fromIdx(); i <= idxNode.toIdx(); i++) {
      var idxInstance = GraphUtils.intU(i, idxNode.type().bitWidth()).toNode();

      // create a cache map for the iteration where each original node affected node
      // is associated with a copied new (uninitialized) node.
      // all idx nodes are replaced by the idxInstance of this iteration.
      var cache = createIterationSpecificCopy(affectedNodes, idxNode, idxInstance, idxDirectUsages);

      // Now, we have a shallow copy of all original nodes.
      // However, the new nodes are still linked to the original nodes.
      // In this step we replace all inputs and successors with the corresponding new nodes.
      graph.linkAndAddCopies(cache);

      // we must cache all unrolled side effects of this loop iteration, so it can
      // be added to the outer scoped abstract end node.
      saveUnrolledSideEffects(end.endNode(), cache);

      // we link the forall node's predecessor to the iteration's forall node successor
      var firstNode = cache.get(firstInLoop);
      DirectionalNode lastNode = null;
      if (lastInLoop != null) {
        lastNode = (DirectionalNode) requireNonNull(cache.get(lastInLoop));
        lastNode.unlinkNext();
      }
      currBeforeStart.setNext((ControlNode) firstNode);
      currBeforeStart = lastNode != null
          ? lastNode
          : currBeforeStart;
    }

    // link the currBeforeStart node of the last iteration to the end node's successor
    var endNext = end.next();
    end.unlinkNext();
    currBeforeStart.setNext(endNext);

    removeOriginalNodes(start, end);
  }

  private void removeOriginalNodes(ForallNode forallNode, ForallEndNode forallEndNode) {
    GraphUtils.deleteAllBetween(forallNode, forallEndNode);
  }

  private Map<Node, Node> createIterationSpecificCopy(Set<Node> affectedNodes,
                                                      ForIdxNode idxNode,
                                                      ConstantNode idxInstance,
                                                      Set<Node> idxDirectUsages
  ) {
    Map<Node, Node> cache = new LinkedHashMap<>();
    // we copy all affected nodes and store them in the cache map.
    for (var node : affectedNodes) {
      var newNode = node.shallowCopy();

      if (idxDirectUsages.contains(node)) {
        // if the node uses the index, we replace the index by the idxInstance of this iteration
        newNode.replaceInput(idxNode, idxInstance);
      }
      // store the relation between the old and the new node
      cache.put(node, newNode);
    }
    return cache;
  }

  private void saveUnrolledSideEffects(AbstractEndNode endNode, Map<Node, Node> copiedCache) {
    for (var sideEffect : endNode.sideEffects()) {
      var cachedSideEffect = copiedCache.get(sideEffect);
      if (cachedSideEffect != null) {
        unrolledSideEffects.add((SideEffectNode) cachedSideEffect);
      } else {
        unrolledSideEffects.add(sideEffect);
      }
    }
  }

  private Set<Node> collectNodesBetween(ForallNode start, ForallEndNode end) {
    var collection = new HashSet<Node>();

    // first we traverse the control flow graph between start and end node.
    var branchStart = start.beginNode();
    var branchEnd = end.endNode();

    try {
      new CfgTraverser() {
        @Override
        public ControlNode onControlNode(ControlNode controlNode) {
          if (controlNode == branchEnd) {
            // we abort the traversal if we reach the end node.
            throw new StopTraversal();
          }
          collection.add(controlNode);
          return controlNode;
        }
      }.traverseBranch(branchStart.next());
    } catch (StopTraversal e) {
      // do nothing
    }

    // then we have to add all dependency nodes that use the loop's idx node
    var idxNode = start.idx();
    // add all transient usages of the idx node to the collection.
    // we only need those dependencies, as the others remain the same across iterations.
    GraphUtils.getTransientUsages(idxNode)
        // we already added all control nodes in-between
        .filter(usage -> !(usage instanceof ControlNode))
        .forEach(collection::add);

    return collection;
  }

}


class StopTraversal extends RuntimeException {
}