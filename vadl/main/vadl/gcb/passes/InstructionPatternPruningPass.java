package vadl.gcb.passes;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.error.Diagnostic;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.types.BuiltInTable;
import vadl.utils.Pair;
import vadl.viam.Specification;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.control.AbstractEndNode;
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
import vadl.viam.passes.CfgTraverser;

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

  enum Likelihood {
    TRUE_CASE,
    FALSE_CASE,
    BOTH
  }

  @Override
  public PassName getName() {
    return new PassName("InstructionPatternPruningPass");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    var workList = new ArrayList<Pair<Node, Node>>();

    do {
      workList.clear();
      viam.isa()
          .map(isa -> isa.ownInstructions().stream())
          .orElse(Stream.empty())
          .forEach(instruction -> {
            instruction.behavior().getNodes(InstrEndNode.class)
                .flatMap(instrEndNode -> instrEndNode.sideEffects().stream())
                .forEach(root ->
                    root.applyOnInputs(new GraphVisitor.Applier<>() {
                      @Nullable
                      @Override
                      public Node applyNullable(Node from, @Nullable Node to) {
                        if (to instanceof SelectNode selectNode) {
                          /* Here we determine the default case.
                             Is the true case the default case
                             or the false case?
                             Because you can write a condition in two ways:
                             (1) X(rs1) == X(rs2) (narrow)
                             (2) X(rs1) != x(rs2) (wide)
                           */
                          var hasTrueCaseHasException = hasException();
                          var hasFalseCaseHasException = hasException();

                          if (hasTrueCaseHasException && hasFalseCaseHasException) {
                            throw Diagnostic.error(
                                "Both branches raise an exception and are pruned",
                                selectNode.trueCase().location()
                                    .join(selectNode.falseCase().location())).build();
                          }

                          if (hasTrueCaseHasException) {
                            workList.add(Pair.of(selectNode, selectNode.falseCase()));
                            selectNode.falseCase().applyOnInputs(this);
                            return to;
                          }

                          if (hasFalseCaseHasException) {
                            workList.add(Pair.of(selectNode, selectNode.trueCase()));
                            selectNode.trueCase().applyOnInputs(this);
                            return to;
                          }

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

    viam.isa()
        .map(isa -> isa.ownInstructions().stream())
        .orElse(Stream.empty())
        .forEach(instruction -> {
          var startNodes = instruction.behavior().getNodes(StartNode.class).toList();
          for (var startNode : startNodes) {
            var ifNodeTraversal = new ExceptionBranchElimination();
            ifNodeTraversal.traverseBranch(startNode);
          }
        });

    return null;
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
        var dir = (DirectionalNode) ifNode.predecessor();
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

  /**
   * Returns {@code true} when an exception is raised on *ALL* execution paths.
   */
  private boolean hasException() {
    // TODO: VADL cannot raise exceptions in the dataflow at the moment.
    return false;
  }
}