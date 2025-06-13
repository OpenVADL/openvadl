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

package vadl.viam.passes;

import static java.util.Objects.requireNonNull;
import static vadl.utils.GraphUtils.getSingleNode;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.utils.ViamUtils;
import vadl.viam.DefProp;
import vadl.viam.Specification;
import vadl.viam.graph.Graph;
import vadl.viam.graph.control.AbstractBeginNode;
import vadl.viam.graph.control.AbstractEndNode;
import vadl.viam.graph.control.BeginNode;
import vadl.viam.graph.control.BranchEndNode;
import vadl.viam.graph.control.ControlNode;
import vadl.viam.graph.control.ControlSplitNode;
import vadl.viam.graph.control.IfNode;
import vadl.viam.graph.control.StartNode;
import vadl.viam.graph.dependency.ConstantNode;

/**
 * Optimizes the control flow by inlining the if-else control flow where the constant
 * is constant.
 */
public class ControlFlowOptimizationPass extends Pass {

  public ControlFlowOptimizationPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("Control Flow Optimization");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) {

    ViamUtils.findDefinitionsByFilter(viam,
            definition -> definition instanceof DefProp.WithBehavior)
        .stream()
        .flatMap(d -> ((DefProp.WithBehavior) d).behaviors().stream())
        .forEach(d -> new ControlFlowOptimizer(d).run());

    return null;
  }
}

class ControlFlowOptimizer implements CfgTraverser {

  Graph graph;

  public ControlFlowOptimizer(Graph graph) {
    this.graph = graph;
  }

  void run() {
    var start = getSingleNode(graph, StartNode.class);
    this.traverseBranch(start);
    graph.deleteUnusedDependencies();
  }

  @Override
  public ControlNode onControlSplit(ControlSplitNode controlSplit) {
    // the general idea:
    // if we come across a control split (outer are traversed first)
    // we check if the condition is constant.
    // if it is constant we can extract the taken branch by linking
    // the node before the control split with the node after the branch start node,
    // and link the node before the branch end node with the node after the merge node.
    // then remove the whole control node construct with the not taken branch.
    // note that we have to preserve the sideeffects attached to the taken branch end node.

    if (!(controlSplit instanceof IfNode ifNode)) {
      throw controlSplit.error("This control split type is not yet implemented by the pass.");
    }

    if (!(ifNode.condition() instanceof ConstantNode constantNode)) {
      return controlSplit;
    }

    var condition = constantNode.constant().asVal();
    var mergeNode = ifNode.findCorrespondingMergeNode();

    BeginNode branchStart;
    BranchEndNode branchEnd;
    BeginNode removedBranch;
    if (condition.bool()) {
      // extract true branch
      branchStart = ifNode.trueBranch();
      branchEnd = mergeNode.trueBranchEnd();
      removedBranch = ifNode.falseBranch();
    } else {
      // extract false branch
      branchStart = ifNode.falseBranch();
      branchEnd = mergeNode.falseBranchEnd();
      removedBranch = ifNode.trueBranch();
    }

    // add side effect to end node of this scope
    var scopeEnd = findEndOfScope(mergeNode);
    for (var sideEffectNode : branchEnd.sideEffects()) {
      scopeEnd.addSideEffect(sideEffectNode);
    }

    // unlink predecessor from ifNode
    var pred = requireNonNull(ifNode.predecessor());
    pred.unlinkNext();

    // unlink branch start from actual branch
    var branchNext = branchStart.unlinkNext();
    // link the node after the true branch start with the node before the if
    pred.setNext(branchNext);

    // unlink merge node and get the node
    var afterMerge = mergeNode.unlinkNext();
    // connect the end of the branch with the merges' successor
    var endPred = requireNonNull(branchEnd.predecessor());
    endPred.setNext(afterMerge);

    // delete the whole control split subflow
    ifNode.safeDelete(false);
    mergeNode.safeDelete();
    branchStart.safeDelete();
    new SubGraphRemover(removedBranch).run();

    // we return the predecessor to handle the branchNext node in the next iteration
    return pred;
  }

  private AbstractEndNode findEndOfScope(ControlNode controlNode) {
    return new CfgTraverser() {
    }.traverseBranch(controlNode);
  }
}

class SubGraphRemover implements CfgTraverser {

  private List<ControlNode> toRemove = new ArrayList<>();
  private AbstractBeginNode begin;

  SubGraphRemover(AbstractBeginNode begin) {
    this.begin = begin;
  }

  void run() {
    begin.ensure(begin.predecessor() == null, "Only unlinked sub graphs can be removed.");
    traverseBranch(begin);

    for (var node : toRemove) {
      if (node instanceof BranchEndNode branchEnd) {
        // remove merge node first
        branchEnd.usages().findFirst().ifPresent(n -> n.safeDelete(false));
      }
      if (!node.isDeleted()) {
        node.safeDelete(false);
      }
    }
  }

  @Override
  public ControlNode onControlNode(ControlNode controlNode) {
    toRemove.add(controlNode);
    return controlNode;
  }
}
