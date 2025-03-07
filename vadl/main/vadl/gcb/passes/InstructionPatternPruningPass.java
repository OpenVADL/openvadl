package vadl.gcb.passes;

import java.io.IOException;
import java.util.ArrayList;
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
import vadl.viam.graph.control.InstrEndNode;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.SelectNode;

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
                          var hasTrueCaseHasException = hasException(selectNode.trueCase());
                          var hasFalseCaseHasException = hasException(selectNode.falseCase());

                          if (hasTrueCaseHasException && hasFalseCaseHasException) {
                            throw Diagnostic.error(
                                "Both branches raise an exception and are pruned",
                                selectNode.trueCase().sourceLocation()
                                    .join(selectNode.falseCase().sourceLocation())).build();
                          }

                          if (hasTrueCaseHasException) {
                            // selectNode.replaceAndDelete(selectNode.falseCase());
                            workList.add(Pair.of(selectNode, selectNode.falseCase()));
                            selectNode.falseCase().applyOnInputs(this);
                            return to; // selectNode.falseCase();
                          }

                          if (hasFalseCaseHasException) {
                            // selectNode.replaceAndDelete(selectNode.trueCase());
                            workList.add(Pair.of(selectNode, selectNode.trueCase()));
                            selectNode.trueCase().applyOnInputs(this);
                            return to; //selectNode.trueCase();
                          }

                          var likelihood = determineLikelihood(selectNode.condition());
                          switch (likelihood) {
                            case TRUE_CASE -> {
                              // selectNode.replaceAndDelete(selectNode.trueCase());
                              workList.add(Pair.of(selectNode, selectNode.trueCase()));
                              selectNode.trueCase().applyOnInputs(this);
                              return to; // selectNode.trueCase();
                            }
                            case FALSE_CASE -> {
                              // selectNode.replaceAndDelete(selectNode.falseCase());
                              workList.add(Pair.of(selectNode, selectNode.falseCase()));
                              selectNode.falseCase().applyOnInputs(this);
                              return to; //selectNode.falseCase();
                            }
                            case BOTH -> {
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

    return null;
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
  private boolean hasException(ExpressionNode expressionNode) {
    // TODO: VADL cannot raise exceptions at the moment.
    return false;
  }

}