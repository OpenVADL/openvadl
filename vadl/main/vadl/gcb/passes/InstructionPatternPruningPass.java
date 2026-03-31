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

package vadl.gcb.passes;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.gcb.annotations.SkipPruningAnnotation;
import vadl.gcb.annotations.StatusRegisterAnnotation;
import vadl.lcb.passes.isaMatching.IsaMachineInstructionMatchingPass;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.types.BuiltInTable;
import vadl.utils.Pair;
import vadl.viam.Instruction;
import vadl.viam.Specification;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.ReadsRegisterTensor;
import vadl.viam.graph.control.AbstractEndNode;
import vadl.viam.graph.control.CfgTraverser;
import vadl.viam.graph.control.ControlNode;
import vadl.viam.graph.control.ControlSplitNode;
import vadl.viam.graph.control.DirectionalNode;
import vadl.viam.graph.control.IfNode;
import vadl.viam.graph.control.InstrEndNode;
import vadl.viam.graph.control.MergeNode;
import vadl.viam.graph.control.StartNode;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.ProcCallNode;
import vadl.viam.graph.dependency.SelectNode;
import vadl.viam.graph.dependency.SideEffectNode;

/**
 * The problem is that when a user writes an instruction behavior where
 * he handles an edge case then this destroys the entire classification
 * in {@link IsaMachineInstructionMatchingPass}. Our solution is to
 * detect the default flow and removing the branches which are not on the default flow.
 * A non default flow is detected when the condition contains a check with a specific value
 * or if any branch raises an exception.
 */
public class InstructionPatternPruningPass extends Pass {
  public InstructionPatternPruningPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  /**
   * Structure to determine which branch is the default case.
   */
  enum Likelihood {
    TRUE_CASE, // the true case is the default case.
    FALSE_CASE, // the false case is the default case.
    BOTH // both are equally likely.
  }

  @Override
  public PassName getName() {
    return new PassName("InstructionPatternPruningPass");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    var workList = new ArrayList<Pair<Node, Node>>();
    var isNotPrunable = new HashSet<Instruction>();

    do {
      workList.clear();
      viam.isa().stream().flatMap(isa -> isa.ownInstructions().stream())
          .filter(instruction -> !instruction.hasAnnotation(SkipPruningAnnotation.class))
          .forEach(instruction -> {
            instruction.behavior().getNodes(InstrEndNode.class)
                .flatMap(instrEndNode -> instrEndNode.sideEffects().stream())
                .forEach(root ->
                    root.applyOnInputs(new GraphVisitor.Applier<>() {
                      @Nullable
                      @Override
                      public Node applyNullable(Node from, @Nullable Node to) {
                        if (to instanceof SelectNode selectNode) {
                          // If the condition contains a register which is a status
                          // register then we do not prune.
                          if (!checkIfConditionHasNoStatusRegister(selectNode)) {
                            return to;
                          }

                          /* Here we determine the default case.
                             Is the true case the default case
                             or the false case?
                             Because you can write a condition in two ways:
                             (1) X(rs1) == X(rs2) (narrow)
                             (2) X(rs1) != x(rs2) (wide)
                           f*/

                          var likelihood = determineLikelihood(selectNode.condition());
                          switch (likelihood) {
                            case TRUE_CASE -> {
                              workList.add(Pair.of(selectNode, selectNode.trueCase()));
                              selectNode.trueCase().applyOnInputs(this);
                              return to;
                            }
                            case FALSE_CASE -> {
                              workList.add(Pair.of(selectNode, selectNode.falseCase()));
                              selectNode.falseCase().applyOnInputs(this);
                              return to;
                            }
                            default -> {
                              // We can't do anything.
                              isNotPrunable.add(instruction);
                              return to;
                            }
                          }
                        } else if (to != null) {
                          to.applyOnInputs(this);
                        }

                        return to;
                      }
                    }));

            for (var item : workList) {
              if (!item.left().isDeleted()) {
                item.left().replaceAndDelete(item.right());
              }
            }
          });
    } while (!workList.isEmpty());

    viam.isa().stream().flatMap(isa -> isa.ownInstructions().stream())
        .filter(instruction -> !instruction.hasAnnotation(SkipPruningAnnotation.class))
        // Only prune instructions which can be entirely pruned.
        .filter(instruction -> !isNotPrunable.contains(instruction))
        .forEach(instruction -> {
          var startNodes = instruction.behavior().getNodes(StartNode.class).toList();
          for (var startNode : startNodes) {
            var ifNodeTraversal = new ExceptionBranchElimination();
            ifNodeTraversal.traverseBranch(startNode);
          }
        });

    return null;
  }

  /**
   * If the selectNode's condition has no {@link ReadsRegisterTensor} that has the
   * {@link StatusRegisterAnnotation}. If all {@link ReadsRegisterTensor} are not status registers
   * then return {@code true}.
   */
  private boolean checkIfConditionHasNoStatusRegister(SelectNode selectNode) {
    var children = new ArrayList<ReadsRegisterTensor>();
    selectNode.condition().collectInputsWithChildren(children, ReadsRegisterTensor.class);

    return children
        .stream()
        .filter(x -> x.registerTensor().isSingleRegister())
        .noneMatch(x -> x.registerTensor().hasAnnotation(StatusRegisterAnnotation.class));
  }

  static class ExceptionBranchElimination implements CfgTraverser {
    private final ArrayDeque<IfNode> ifNodes = new ArrayDeque<>();
    private final List<SideEffectNode> collection = new ArrayList<>();
    private final Set<AbstractEndNode> markedForDeletion = new HashSet<>();

    @Override
    public ControlNode onControlSplit(ControlSplitNode controlNode) {
      if (controlNode instanceof IfNode ifNode) {
        ifNodes.add(ifNode);
      }
      return controlNode;
    }

    @Override
    public ControlNode onEnd(AbstractEndNode endNode) {
      if (endNode.sideEffects().stream().anyMatch(x -> x instanceof ProcCallNode procCallNode
          && procCallNode.exceptionRaise())) {
        markedForDeletion.add(endNode);
      }
      return endNode;
    }

    @Override
    public ControlNode traverseControlSplit(ControlSplitNode splitNode) {
      @Nullable AbstractEndNode someEnd = null;
      var afterControlSplit = splitNode.mergeNode().next();
      for (var branch : splitNode.branches()) {
        someEnd = traverseBranch(branch);
      }
      splitNode.ensure(someEnd != null, "Control split has no branches.");

      // Get the merge node from the end of the branch
      return someEnd.usages().findFirst().map(x -> (ControlNode) x).orElse(afterControlSplit);
    }

    @Override
    public ControlNode onDirectional(DirectionalNode directionalNode) {
      if (!(directionalNode instanceof MergeNode mergeNode)) {
        return directionalNode;
      }

      var ifNode = ifNodes.pop();
      var oldNext = Objects.requireNonNull(ifNode.predecessor());

      if (markedForDeletion.contains(mergeNode.trueBranchEnd())) {
        if (mergeNode.next() instanceof AbstractEndNode abstractEndNode) {
          // We have to delete the true branch, therefore we store the side effects of the
          // false branch in the next end node.
          eliminate(mergeNode, mergeNode.falseBranchEnd(), abstractEndNode, ifNode);
          return (ControlNode) oldNext;
        } else {
          // We have to delete the true branch, but the next node has no side effects.
          // Because it might be an IfNode.
          collection.addAll(mergeNode.falseBranchEnd().sideEffects());
        }
      } else if (markedForDeletion.contains(mergeNode.falseBranchEnd())) {
        if (mergeNode.next() instanceof AbstractEndNode abstractEndNode) {
          // We have to delete the false branch, therefore we store the side effects of the
          // true branch in the next end node.
          eliminate(mergeNode, mergeNode.trueBranchEnd(), abstractEndNode, ifNode);
          return (ControlNode) oldNext;
        } else {
          // We have to delete the false branch, but the next node has no side effects.
          // Because it might be an IfNode.
          collection.addAll(mergeNode.trueBranchEnd().sideEffects());
        }
      }

      return mergeNode;
    }

    private void eliminate(MergeNode mergeNode,
                           AbstractEndNode branchNodeToEliminate,
                           AbstractEndNode abstractEndNode,
                           IfNode ifNode) {

      branchNodeToEliminate.sideEffects().forEach(abstractEndNode::addSideEffect);
      collection.forEach(abstractEndNode::addSideEffect);
      collection.clear();

      if (ifNode.predecessor() != null) {
        var dir = ifNode.predecessor();
        var mergeNext = mergeNode.next();
        mergeNode.replaceSuccessor(mergeNode.next(), null);
        dir.setNext(mergeNext);
        ifNode.clearPredecessor();
      }
      ifNode.safeDelete();
      mergeNode.safeDelete();
      markedForDeletion.remove(branchNodeToEliminate);
    }
  }

  /**
   * Determines which case of the {@code condition} is more likely and is therefore the default
   * flow.
   *
   * @return the {@link Likelihood} that determines which branch is the default case.
   */
  private Likelihood determineLikelihood(ExpressionNode condition) {
    if (condition instanceof BuiltInCall builtInCall) {
      // An equality is always an edge case.
      if (builtInCall.builtIn() == BuiltInTable.EQU) {
        return Likelihood.FALSE_CASE;
      } else if (builtInCall.builtIn() == BuiltInTable.NEQ) {
        return Likelihood.TRUE_CASE;
      } else if (builtInCall.builtIn() == BuiltInTable.AND) {
        Likelihood result = null;

        // Iterate over all the "and expressions" and see if the likelihood is the same.
        for (var arg : builtInCall.arguments()) {
          var subLikelihood = determineLikelihood(arg);

          var combined = meet(result, subLikelihood);
          if (combined == Likelihood.BOTH) {
            return Likelihood.BOTH;
          } else {
            // They are the same so continue
            result = combined;
          }
        }
        return result == null ? Likelihood.BOTH : result;
      }
    }

    return Likelihood.BOTH;
  }

  /**
   * Calculates the common {@link Likelihood} of both input parameters.
   *
   * @param result    is the {@link Likelihood} that the branch is the default case based on the
   *                  previous conditions.
   * @param subResult is the {@link Likelihood} that the branch is the default case based on the
   *                  current subcondition.
   * @return the combined {@link Likelihood} based on the previous result and the new information.
   */
  private Likelihood meet(@Nullable Likelihood result, Likelihood subResult) {
    // The result is not set because it is the first expression.
    if (result == null) {
      return subResult;
    } else {
      if (result == subResult) {
        return result;
      } else {
        return Likelihood.BOTH;
      }
    }
  }
}
