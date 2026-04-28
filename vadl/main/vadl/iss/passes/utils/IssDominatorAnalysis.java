// SPDX-FileCopyrightText : © 2026 TU Wien <vadl@tuwien.ac.at>
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

package vadl.iss.passes.utils;

import static java.util.Objects.requireNonNull;
import static vadl.utils.GraphUtils.getSingleNode;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import vadl.viam.graph.Graph;
import vadl.viam.graph.control.BranchEndNode;
import vadl.viam.graph.control.ControlNode;
import vadl.viam.graph.control.ControlSplitNode;
import vadl.viam.graph.control.MergeNode;
import vadl.viam.graph.control.StartNode;
import vadl.viam.passes.CfgTraverser;

/**
 * Utilities for computing and querying CFG dominator information for ISS passes.
 */
public final class IssDominatorAnalysis {

  private final Map<ControlNode, List<ControlNode>> dominatorSets;

  private IssDominatorAnalysis(Map<ControlNode, List<ControlNode>> dominatorSets) {
    this.dominatorSets = dominatorSets;
  }

  /**
   * Analyzes the given control flow graph and captures its dominator information.
   *
   * <p>Each list is ordered from entry toward the node itself, so shared-prefix scans can be used
   * to derive a latest common dominator.</p>
   */
  public static IssDominatorAnalysis analyze(Graph cfg) {
    var traversal = new Traversal();
    var start = getSingleNode(cfg, StartNode.class);
    traversal.traverseBranch(start);
    return new IssDominatorAnalysis(traversal.dominatorSets);
  }

  /**
   * Returns the ordered dominator list for the given control node.
   */
  public List<ControlNode> dominatorsOf(ControlNode node) {
    return requireNonNull(dominatorSets.get(node));
  }

  /**
   * Returns whether the given control node was part of the analyzed CFG.
   */
  public boolean contains(ControlNode node) {
    return dominatorSets.containsKey(node);
  }

  /**
   * Finds the latest control node that dominates every node in the given set.
   */
  public ControlNode latestCommonDominator(Set<ControlNode> nodes) {
    var domSets = new ArrayList<List<ControlNode>>();
    for (var node : nodes) {
      domSets.add(new ArrayList<>(dominatorsOf(node)));
    }

    // Start node is always common; the last shared prefix element is the answer.
    var lastCommon = domSets.getFirst().getFirst();
    for (var dominators : domSets) {
      dominators.removeFirst();
    }

    while (true) {
      @Nullable ControlNode nextCommon = null;
      for (var dominators : domSets) {
        if (dominators.isEmpty()) {
          return lastCommon;
        }

        var next = dominators.removeFirst();
        if (next != lastCommon) {
          return lastCommon;
        }
        nextCommon = next;
      }
      lastCommon = requireNonNull(nextCommon);
    }
  }

  /**
   * Traverses the CFG while maintaining the active dominator stack for each branch.
   */
  private static final class Traversal implements CfgTraverser {
    private final Map<ControlNode, List<ControlNode>> dominatorSets = new HashMap<>();
    private final ArrayDeque<Integer> splitDominatorIndexStack = new ArrayDeque<>();
    private List<ControlNode> dominators = new ArrayList<>();

    @Override
    public ControlNode onControlNode(ControlNode node) {
      if (node instanceof ControlSplitNode) {
        splitDominatorIndexStack.push(dominators.size() - 1);
      } else if (node instanceof BranchEndNode || node instanceof MergeNode) {
        dominators = dominators.subList(0, requireNonNull(splitDominatorIndexStack.peek()) + 1);
      }

      dominators.add(node);
      dominatorSets.put(node, new ArrayList<>(dominators));
      return node;
    }
  }
}
