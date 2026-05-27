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

package vadl.iss.passes.common;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nullable;
import vadl.configuration.IssConfiguration;
import vadl.iss.passes.AbstractIssPass;
import vadl.iss.passes.common.safeResourceRead.nodes.ExprSaveNode;
import vadl.iss.passes.utils.IssDominatorAnalysis;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.utils.GraphUtils;
import vadl.viam.Instruction;
import vadl.viam.Specification;
import vadl.viam.graph.Node;
import vadl.viam.graph.control.AbstractBeginNode;
import vadl.viam.graph.control.ControlNode;
import vadl.viam.graph.control.DirectionalNode;
import vadl.viam.graph.control.ScheduledNode;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.ForIdxNode;
import vadl.viam.graph.dependency.FuncCallNode;
import vadl.viam.graph.dependency.LetNode;
import vadl.viam.graph.dependency.ParamNode;
import vadl.viam.graph.dependency.ReadMemNode;
import vadl.viam.graph.dependency.ReadRegTensorNode;

/**
 * Materializes helper-side common scalar subexpressions as {@link ExprSaveNode}s.
 *
 * <p>The helper path emits expression trees directly as C code. After ISS decomposition,
 * normalization, and resource-read securing, expensive helper expressions often appear as repeated
 * scalar subtrees. This pass stores such subtrees once at the latest control location that
 * dominates all uses, so helper code generation can reuse the existing {@link ExprSaveNode}
 * support without introducing codegen-local placement logic.</p>
 */
public class IssCommonExprSavePass extends AbstractIssPass {

  public IssCommonExprSavePass(IssConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("ISS Common Expr Save Pass");
  }

  @Override
  public @Nullable Object execute(PassResults passResults, Specification viam) throws IOException {
    if (viam.isa().isEmpty()) {
      return null;
    }

    wholeHelperInstrs(viam).forEach(this::saveCommonExpressions);
    return null;
  }

  private void saveCommonExpressions(Instruction instruction) {
    var graph = instruction.behavior();
    var dominatorAnalysis = IssDominatorAnalysis.analyze(graph);

    // Process from consumers back toward leaves so once a parent expression is materialized,
    // its children can see that reduced fanout and avoid redundant saves.
    var topo = new java.util.ArrayList<>(GraphUtils.topologyOrderOfDependencyNodes(graph));
    java.util.Collections.reverse(topo);

    topo.stream()
        .filter(ExpressionNode.class::isInstance)
        .map(ExpressionNode.class::cast)
        .filter(this::isCandidate)
        .forEach(expr -> materialize(expr, dominatorAnalysis));
  }

  private boolean isCandidate(ExpressionNode expr) {
    if (expr instanceof ExprSaveNode) {
      return false;
    }
    if (!expr.type().isDataType()) {
      return false;
    }
    if (expr.type().asDataType().bitWidth() > 64) {
      return false;
    }
    if (expr.inputs().findAny().isEmpty()) {
      return false;
    }
    if (expr.usages().count() <= 1) {
      return false;
    }
    if (dependsOnForallIndex(expr)) {
      return false;
    }
    if (hasPreloadedReadUsage(expr, new HashSet<>())) {
      return false;
    }
    if (isTrivialAlias(expr)) {
      return false;
    }
    return !containsDisallowedSubtree(expr, new HashSet<>());
  }

  private void materialize(ExpressionNode expr,
                           IssDominatorAnalysis dominatorAnalysis) {
    // Map all transitively reachable control users back onto stable CFG nodes that participate
    // in the dominator analysis, then save once at their latest common dominator.
    var controlUsages = GraphUtils.getTransientUsages(expr)
        .filter(ControlNode.class::isInstance)
        .map(ControlNode.class::cast)
        .map(node -> normalizeControlUsage(node, dominatorAnalysis))
        .filter(java.util.Objects::nonNull)
        .collect(java.util.stream.Collectors.toSet());
    if (controlUsages.isEmpty()) {
      return;
    }

    var saveLocation = dominatorAnalysis.latestCommonDominator(controlUsages);
    var saveNode = new ExprSaveNode(expr);
    saveNode = expr.replace(saveNode);
    insertScheduledSave(saveNode, saveLocation);
  }

  private void insertScheduledSave(ExprSaveNode saveNode, ControlNode location) {
    // Expr saves behave like other scheduled dependency nodes: they must be inserted into the
    // directional CFG edge that dominates all later uses.
    var scheduledSaveNode = new ScheduledNode(saveNode);
    if (location instanceof AbstractBeginNode beginNode) {
      beginNode.addAfter(scheduledSaveNode);
      return;
    }

    var predecessor = location.predecessor();
    location.ensure(predecessor instanceof DirectionalNode,
        "Expected control save location predecessor to be directional, got %s",
        location.predecessor());
    ((DirectionalNode) predecessor).addAfter(scheduledSaveNode);
  }

  private boolean dependsOnForallIndex(Node node) {
    return GraphUtils.isOrHasDependencies(node, ForIdxNode.class::isInstance);
  }

  private boolean containsDisallowedSubtree(Node node, Set<Node> visited) {
    // Keep the pass conservative: CPU-vector repeated scalar arithmetic is fine to save,
    // but memory reads and function calls may imply extra evaluation constraints or side effects.
    if (!visited.add(node)) {
      return false;
    }
    if (node instanceof ReadMemNode || node instanceof FuncCallNode) {
      return true;
    }
    return node.inputs().anyMatch(input -> containsDisallowedSubtree(input, visited));
  }

  private boolean hasPreloadedReadUsage(Node node, Set<Node> visited) {
    // Helper register-read preloads are emitted before scheduled ExprSave statements, so any
    // expression feeding such a preload must remain directly printable and cannot depend on a save.
    if (!visited.add(node)) {
      return false;
    }
    return node.usages().anyMatch(usage -> {
      if (usage instanceof ReadRegTensorNode read) {
        return isPreloadedHelperRead(read);
      }
      return hasPreloadedReadUsage(usage, visited);
    });
  }

  private boolean isPreloadedHelperRead(ReadRegTensorNode read) {
    return !dependsOnForallIndex(read) && read.type().isDataType()
        && read.type().asDataType().bitWidth() <= 64;
  }

  private boolean isTrivialAlias(ExpressionNode expr) {
    // Do not create save temporaries that merely rename an already variable-like value.
    if (!(expr instanceof LetNode letNode)) {
      return false;
    }
    var value = letNode.expression();
    return value instanceof ConstantNode || value instanceof ParamNode
        || value instanceof ReadRegTensorNode || value instanceof ExprSaveNode;
  }

  private static @Nullable ControlNode normalizeControlUsage(
      ControlNode node,
      IssDominatorAnalysis dominatorAnalysis) {
    // Newly inserted ScheduledNodes are not part of the precomputed dominator map; use their
    // successor as the stable CFG location they conceptually belong to.
    if (dominatorAnalysis.contains(node)) {
      return node;
    }
    if (node instanceof DirectionalNode directionalNode && directionalNode.next() != null) {
      var next = directionalNode.next();
      if (dominatorAnalysis.contains(next)) {
        return next;
      }
    }
    return null;
  }

}
