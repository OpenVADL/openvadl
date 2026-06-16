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

package vadl.iss.passes.common.planning.evaluators;

import static vadl.iss.passes.TcgPassUtils.regInfo;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import vadl.iss.passes.common.planning.analysis.VectorRegion;
import vadl.iss.passes.extensions.InstrExecPlan.DirectGvecSupport;
import vadl.iss.passes.extensions.RegInfo;
import vadl.iss.passes.nodes.IssReadRegNode;
import vadl.iss.passes.nodes.IssWriteRegNode;
import vadl.utils.GraphUtils;
import vadl.viam.Instruction;
import vadl.viam.graph.Node;
import vadl.viam.graph.control.ControlNode;
import vadl.viam.graph.control.ForallNode;
import vadl.viam.graph.dependency.FoldNode;
import vadl.viam.graph.dependency.TensorNode;
import vadl.viam.passes.CfgTraverser;

/**
 * Checks whether an instruction can stay on the shared non-helper path without any dedicated
 * vector-region optimization.
 */
public final class NormalTcgPathEvaluator {

  /**
   * Returns whether the instruction can remain on the shared non-helper path without a dedicated
   * vector-region rewrite.
   */
  public boolean isViable(Instruction instruction) {
    return isViable(instruction, List.of());
  }

  /**
   * Returns whether the instruction can remain on the shared non-helper path after subtracting the
   * accepted vector-region rewrites from the residual graph.
   */
  public boolean isViable(Instruction instruction, List<DirectGvecSupport> directGvecRegions) {
    var behavior = instruction.behavior();
    var coveredNodes = coveredNodesOf(directGvecRegions);

    // This is the baseline shared non-helper path before any vector-specific region rewriting.
    // It stays valid only while the residual graph does not require vector/tensor semantics that
    // the default lowering still does not model directly.
    var hasCpuVectorReads = behavior.getNodes(IssReadRegNode.class)
        .filter(node -> !coveredNodes.contains(node))
        .anyMatch(node -> regInfo(node.regTensor()).execClass() == RegInfo.ExecClass.CPU_VECTOR);
    var hasCpuVectorWrites = behavior.getNodes(IssWriteRegNode.class)
        .filter(node -> !coveredNodes.contains(node))
        .anyMatch(node -> regInfo(node.regTensor()).execClass() == RegInfo.ExecClass.CPU_VECTOR);
    var hasForall = behavior.getNodes(ForallNode.class)
        .anyMatch(node -> !coveredNodes.contains(node));
    var hasTensor = behavior.getNodes(TensorNode.class)
        .anyMatch(node -> !coveredNodes.contains(node));
    var hasFold = behavior.getNodes(FoldNode.class)
        .anyMatch(node -> !coveredNodes.contains(node));

    return !(hasCpuVectorReads || hasCpuVectorWrites || hasForall || hasTensor || hasFold);
  }

  private Set<Node> coveredNodesOf(List<DirectGvecSupport> directGvecRegions) {
    var coveredNodes = new HashSet<Node>();
    directGvecRegions.stream()
        .filter(DirectGvecSupport::isViable)
        .map(DirectGvecSupport::region)
        .forEach(region -> collectCoveredNodes(region, coveredNodes));
    return Set.copyOf(coveredNodes);
  }

  private void collectCoveredNodes(VectorRegion region, Set<Node> coveredNodes) {
    coveredNodes.add(region.forall());
    coveredNodes.add(region.forallEnd());
    coveredNodes.add(region.write());
    coveredNodes.add(region.valueExpression());

    collectLoopBodyControlNodes(region, coveredNodes);
    GraphUtils.getTransientUsages(region.idx())
        .filter(usage -> !(usage instanceof ControlNode))
        .forEach(coveredNodes::add);
  }

  private void collectLoopBodyControlNodes(VectorRegion region, Set<Node> coveredNodes) {
    var branchEnd = region.forallEnd().endNode();
    try {
      new CfgTraverser() {
        @Override
        public ControlNode onControlNode(ControlNode controlNode) {
          if (controlNode == branchEnd) {
            throw new RegionTraversalStop();
          }
          coveredNodes.add(controlNode);
          return controlNode;
        }
      }.traverseBranch(region.forall().beginNode().next());
    } catch (RegionTraversalStop e) {
      // reached the end of the covered loop body
    }
    coveredNodes.add(branchEnd);
  }

  private static final class RegionTraversalStop extends RuntimeException {
  }
}
