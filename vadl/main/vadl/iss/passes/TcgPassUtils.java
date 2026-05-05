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

package vadl.iss.passes;

import static vadl.viam.ViamError.ensure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.function.BiConsumer;
import javax.annotation.Nullable;
import vadl.iss.passes.extensions.InstrInfo;
import vadl.iss.passes.extensions.RegInfo;
import vadl.iss.passes.tcgLowering.TcgCondition;
import vadl.types.BuiltInTable;
import vadl.utils.GraphUtils;
import vadl.viam.Instruction;
import vadl.viam.RegisterTensor;
import vadl.viam.graph.control.ControlNode;
import vadl.viam.graph.control.DirectionalNode;
import vadl.viam.graph.control.ScheduledNode;
import vadl.viam.graph.control.StartNode;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.DependencyNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.LetNode;
import vadl.viam.graph.dependency.ReadResourceNode;

/**
 * Contains utility methods for TCG passes.
 */
public class TcgPassUtils {

  /**
   * Returns a variable name for the given expression that is easy to read and understand
   * in the generated source code.
   */
  public static String exprVarName(ExpressionNode expr) {
    if (expr instanceof LetNode letNode) {
      return letNode.letName().name();
    } else if (expr instanceof FieldRefNode fieldRefNode) {
      return fieldRefNode.formatField().simpleName();
    } else if (expr instanceof FieldAccessRefNode fieldAccessRefNode) {
      return fieldAccessRefNode.fieldAccess().simpleName();
    } else if (expr instanceof ConstantNode constantNode) {
      return constantNode.constant().asVal().asString("0x", 16, false);
    } else {
      return "n" + expr.id;
    }
  }

  public static boolean isTcg(DependencyNode node) {
    return node.usages().anyMatch(u -> u instanceof ScheduledNode);
  }

  /**
   * Determines whether a given dependency node in the data dependency
   * graph must be scheduled based on its dependencies. A node is considered
   * to require scheduling if it has dependencies that are {@link ReadResourceNode}
   * objects with a resource definition different from the provided tensor.
   *
   * @param node the {@link DependencyNode} to be checked
   * @return {@code true} if the node must be scheduled, {@code false} otherwise
   */
  public static boolean mustBeScheduled(DependencyNode node) {
    // pc reads can be done at translation time and are not translated to TCG
    // pc reads are translated to IssStaticPcRegNode at this point -> no need to check
    return GraphUtils.hasDependencies(node, dep -> dep instanceof ReadResourceNode);
  }

  public static RegInfo regInfo(RegisterTensor reg) {
    return reg.expectExtension(RegInfo.class);
  }

  public static InstrInfo instrInfo(Instruction instr) {
    return instr.expectExtension(InstrInfo.class);
  }


  /**
   * Returns a {@link TcgCondition} for a given {@link vadl.types.BuiltInTable.BuiltIn}, if
   * there exists one, otherwise it returns null.
   * E.g., on the SLTH built-in it returns LT.
   */
  public static @Nullable TcgCondition conditionOf(BuiltInTable.BuiltIn builtIn) {
    if (builtIn == BuiltInTable.EQU) {
      return TcgCondition.EQ;
    } else if (builtIn == BuiltInTable.NEQ) {
      return TcgCondition.NE;
    } else if (builtIn == BuiltInTable.SLTH) {
      return TcgCondition.LT;
    } else if (builtIn == BuiltInTable.SLEQ) {
      return TcgCondition.LE;
    } else if (builtIn == BuiltInTable.ULTH) {
      return TcgCondition.LTU;
    } else if (builtIn == BuiltInTable.ULEQ) {
      return TcgCondition.LEU;
    } else if (builtIn == BuiltInTable.SGTH) {
      return TcgCondition.GT;
    } else if (builtIn == BuiltInTable.SGEQ) {
      return TcgCondition.GE;
    } else if (builtIn == BuiltInTable.UGTH) {
      return TcgCondition.GTU;
    } else if (builtIn == BuiltInTable.UGEQ) {
      return TcgCondition.GEU;
    } else if (builtIn == BuiltInTable.AND) {
      return TcgCondition.TSTNE;
    } else {
      return null;
    }
  }

  /**
   * Returns a {@link BuiltInTable.BuiltIn} for a given {@link TcgCondition}.
   * E.g., on LT it returns the SLTH built-in.
   */
  public static BuiltInTable.BuiltIn builtInOf(TcgCondition condition) {
    return switch (condition) {
      case EQ -> BuiltInTable.EQU;
      case NE -> BuiltInTable.NEQ;
      case LT -> BuiltInTable.SLTH;
      case GE -> BuiltInTable.SGEQ;
      case LE -> BuiltInTable.SLEQ;
      case GT -> BuiltInTable.SGTH;
      case LTU -> BuiltInTable.ULTH;
      case GEU -> BuiltInTable.UGEQ;
      case LEU -> BuiltInTable.ULEQ;
      case GTU -> BuiltInTable.UGTH;
      case TSTNE -> BuiltInTable.AND;
      case TSTEQ -> throw new IllegalArgumentException("No built-in for TSTEQ");
    };
  }

  /**
   * Finds the latest possible insertion point for the given expression to be scheduled.
   * This is done by using the {@link #findCommonInsertionPoint(List)}
   * applied on all usages that are scheduled with a {@link ScheduledNode}.
   */
  public static DirectionalNode findLatestSafeInsertionPoint(ExpressionNode node) {
    var scheduledUsers =
        GraphUtils.getTransientUsages(node)
            .filter(u -> u instanceof ControlNode)
            .map(u -> (ControlNode) u)
            .toList();
    return findCommonInsertionPoint(scheduledUsers);
  }

  /**
   * Finds the control-flow insertion point for scheduling a dependency so it
   * executes on every path that reaches all given user branch ends.
   *
   * <p>
   * Used when turning a data dependency into a {@link ScheduledNode} in the CFG.
   * Given branch ends (the users of the dependency), this method returns the
   * nearest/common {@link DirectionalNode} that lies on the backward CFG of
   * every provided end. Inserting a scheduling node <em>after</em> the returned
   * {@code DirectionalNode} guarantees the dependency is evaluated on all
   * relevant execution paths.
   * </p>
   *
   * <h4>Assumptions and behavior</h4>
   * <ul>
   *   <li>{@code userBranches} must be non-empty; otherwise a
   *       {@link vadl.viam.graph.ViamGraphError} is thrown.</li>
   *   <li>For a single user branch end, the insertion point is the
   *       {@linkplain GraphUtils#predecessorSkippingMerges(ControlNode) merge-skipping}
   *       predecessor of that end; it must be a {@link DirectionalNode}.</li>
   *   <li>For multiple ends, the algorithm walks each end’s backward CFG using
   *       {@code predecessorSkippingMerges}, collecting only {@link DirectionalNode}s
   *       until the {@link StartNode} is reached. It then picks the nearest (deepest)
   *       {@code DirectionalNode} that appears on all backward paths.</li>
   *   <li>If no common node exists, the method fails with a descriptive error
   *       (malformed/inconsistent CFG).</li>
   * </ul>
   *
   * <h4>Algorithm (informal)</h4>
   * <ol>
   *   <li>From the first end’s merge-skipping predecessor, collect the backward
   *       path of {@link DirectionalNode}s up to (but not including) the {@link StartNode}.</li>
   *   <li>For each remaining end, collect the set of its backward {@code DirectionalNode}s
   *       (also merge-skipping) up to the start.</li>
   *   <li>Iterate the first path from nearest to farthest and return the first node
   *       contained in all other sets.</li>
   * </ol>
   * Complexity is O(k · L), where k is the number of branch ends and L the maximum
   * inspected backward-path length.
   *
   * @param userBranches the control-flow branch ends that must have the dependency executed
   *                     on every reaching path
   * @return the nearest/common {@link DirectionalNode} after which the dependency can be
   *     safely scheduled for all provided ends
   * @throws vadl.viam.graph.ViamGraphError if {@code userBranches} is empty or no common
   *                                        insertion point can be found
   */
  public static DirectionalNode findCommonInsertionPoint(List<ControlNode> userBranches) {
    ensure(!userBranches.isEmpty(), "userBranches must not be empty");

    if (userBranches.size() == 1) {
      var pred = userBranches.getFirst().predecessor();
      ensure(pred instanceof DirectionalNode, "Predecessor is not a directional node");
      return (DirectionalNode) pred;
    }

    // lambda to collect all directional predecessor nodes
    BiConsumer<ControlNode, Collection<DirectionalNode>> collectDirPreds = (curr, collection) -> {
      ControlNode prev = GraphUtils.predecessorSkippingMerges(curr);
      while (!(prev instanceof StartNode startNode)) {
        ensure(prev != null, "Found node without predecessor during backwards traversal");
        if (prev instanceof DirectionalNode dir) {
          collection.add(dir);
        }
        prev = GraphUtils.predecessorSkippingMerges(prev);
      }
      // finally, add the start node
      collection.add(startNode);
    };

    // Collect the backward path (DirectionalNodes) from the first branch end to the start
    ArrayList<DirectionalNode> firstPath = new ArrayList<>();
    collectDirPreds.accept(userBranches.getFirst(), firstPath);

    // Build sets for the remaining branch ends for quick membership checks
    ArrayList<HashSet<DirectionalNode>> otherSets = new ArrayList<>();
    for (int i = 1; i < userBranches.size(); i++) {
      var set = new HashSet<DirectionalNode>();
      var curr = userBranches.get(i);
      collectDirPreds.accept(curr, set);
      otherSets.add(set);
    }

    // Pick the deepest common directional node: iterate along firstPath (from end backwards)
    for (var dir : firstPath) {
      boolean common = true;
      for (var set : otherSets) {
        if (!set.contains(dir)) {
          common = false;
          break;
        }
      }
      if (common) {
        return dir;
      }
    }

    // If no common node was found, this indicates a malformed CFG
    throw new IllegalStateException(
        "Failed to find common insertion point for scheduled dependency");
  }
}
