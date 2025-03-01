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

package vadl.viam.passes.sideeffect_condition;

import static vadl.utils.GraphUtils.getSingleNode;

import javax.annotation.Nullable;
import vadl.types.BuiltInTable;
import vadl.viam.Constant;
import vadl.viam.graph.Graph;
import vadl.viam.graph.control.AbstractBeginNode;
import vadl.viam.graph.control.AbstractEndNode;
import vadl.viam.graph.control.ControlNode;
import vadl.viam.graph.control.ControlSplitNode;
import vadl.viam.graph.control.DirectionalNode;
import vadl.viam.graph.control.IfNode;
import vadl.viam.graph.control.MergeNode;
import vadl.viam.graph.control.StartNode;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * Resolves conditions of {@link vadl.viam.graph.dependency.SideEffectNode}s on a given graph.
 * For every side effect, it finds the condition under which it is executed/affected.
 *
 * <p>It is used by the {@link SideEffectConditionResolvingPass}.</p>
 */
public class SideEffectConditionResolver {

  /**
   * Run the resolver on the given graph.
   */
  public static void run(Graph behavior) {
    new SideEffectConditionResolver()
        .resolve(behavior);
  }

  /*
  The algorithm is recursive and traverses the control flow of the given graph.
  It starts with the StartNode and uses the trivial constant `true` condition as
  base condition for every SideEffect on the way.

  See resolveBranch() for more details.
   */
  private void resolve(Graph behavior) {
    var start = getSingleNode(behavior, StartNode.class);

    resolveBranch(
        start,
        new ConstantNode(Constant.Value.of(true))
    );
  }

  /*
  The recursive resolveBranch method traverses the branch and for each side effect it
  sets the condition that must be true to be in the current branch.

  The method only calls itself in case of a control split, where a new sub-branch must be
  entered.
  Otherwise, there is always a well-defined successor that can be set as the next `current`.
  So we loop through the control nodes, until one of two things happens

    1.  We found an AbstractEndNode, which that we reached the end of the branch.
        We iterate through the set of side effects in hold by the end node.
        If the side effect node already has a condition (this might be the case as the side effect
        is a unique node), we create a condition by ORing the existing condition and the given
        branch condition.
        If there is no such condition yet, we simply set the condition to the branch condition.
        Then we find the MergeNode that uses the end node and return, so the loop is exited.

     2. We found a ControlSplitNode, which currently can only be an IfNode.
        As there is an if node, we have to resolve the side effects in both branches.
        So we call resolveBranch on the trueBranch, while the branch condition is the ANDing of
        the current branch condition and the if condition.
        We do the same for the falseBranch, but NOT the if condition before ANDing it with the
        current branch condition.

        We get the MergeNode from both sub branches, and ensure that they are equal.
        As we haven't reached the end of the current branch yet, we set `current` to the
        retrieved MergeNode and continue in the loop.
   */
  private @Nullable MergeNode resolveBranch(AbstractBeginNode beginNode,
                                            ExpressionNode branchCondition) {
    // the current control node
    ControlNode current = beginNode;

    // loop is only terminated by return of AbstractEndNode
    while (true) {

      if (current instanceof AbstractEndNode endNode) {
        // handle the end of the current branch
        var graph = endNode.graph();
        endNode.ensure(graph != null,
            "Node is not active, but control flow must be stable for SideEffectConditionResolver");

        // add the condition to all side effects
        for (var sideEffect : endNode.sideEffects()) {
          // if not already active, add the condition to the graph
          // it is important to override the branchCondition and not use a new variable
          branchCondition = branchCondition.isActive() ? branchCondition :
              graph.addWithInputs(branchCondition);

          var cond = branchCondition;
          var existingCondition = sideEffect.nullableCondition();
          if (existingCondition != null) {
            // if the side effect already has a condition, it is also available in another
            // branch, so we have ot OR it with the current branch condition
            cond = graph.addWithInputs(
                BuiltInCall.of(BuiltInTable.OR, existingCondition, cond)
            );
          }

          // set the condition
          sideEffect.setCondition(cond);
        }

        // find and return the merge node if available
        // (only in case of an InstrEndNode the MergeNode is not available)
        return endNode.usages()
            .filter(user -> user instanceof MergeNode)
            .map(MergeNode.class::cast)
            .findAny()
            .orElse(null);

      } else if (current instanceof ControlSplitNode splitNode) {
        // handle a control split

        // currently only IfNodes are supported
        splitNode.ensure(splitNode instanceof IfNode,
            "SideEffectConditionResolver not implemented for ControlSplitNode %s",
            splitNode.getClass());
        var ifNode = (IfNode) current;

        // the condition for the true branch is the given branch condition and the
        // if condition
        var trueCondition = BuiltInCall.of(BuiltInTable.AND, branchCondition, ifNode.condition());
        var trueMergeNode = resolveBranch(ifNode.trueBranch(), trueCondition);
        // the condition for the false branch is the given branch condition and the
        // NOTing of the if condition
        var falseCondition = BuiltInCall.of(BuiltInTable.AND,
            branchCondition,
            BuiltInCall.of(BuiltInTable.NOT, ifNode.condition())
        );
        var falseMergeNode = resolveBranch(ifNode.falseBranch(), falseCondition);

        // MergeNode must be the same for all branches and not null
        ifNode.ensure(trueMergeNode == falseMergeNode,
            "Branches of node don't result in the same merge node");
        ifNode.ensure(trueMergeNode != null,
            "Couldn't find merge node for true branch");

        // continue with the found mergeNode
        current = trueMergeNode;

      } else if (current instanceof DirectionalNode directionalNode) {
        // handle normal singled directed node by just skipping it and continue
        current = directionalNode.next();

      } else {
        // there should not be an other control node that was not handled yet
        //noinspection DataFlowIssue
        current.ensure(false,
            "Not an expected node in the SideEffectConditionResolver. You want to implement it.");
      }
    }
  }


}
