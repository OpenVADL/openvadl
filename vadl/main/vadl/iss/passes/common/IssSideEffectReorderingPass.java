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

import static java.util.Objects.requireNonNull;
import static vadl.utils.GraphUtils.getSingleNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.types.BuiltInTable;
import vadl.utils.GraphUtils;
import vadl.utils.ViamUtils;
import vadl.viam.DefProp;
import vadl.viam.Instruction;
import vadl.viam.Procedure;
import vadl.viam.Specification;
import vadl.viam.Stage;
import vadl.viam.graph.Graph;
import vadl.viam.graph.control.AbstractBeginNode;
import vadl.viam.graph.control.AbstractEndNode;
import vadl.viam.graph.control.ControlNode;
import vadl.viam.graph.control.DirectionalNode;
import vadl.viam.graph.control.ForallNode;
import vadl.viam.graph.control.IfNode;
import vadl.viam.graph.control.InstrEndNode;
import vadl.viam.graph.control.MergeNode;
import vadl.viam.graph.control.ProcEndNode;
import vadl.viam.graph.control.StageEndNode;
import vadl.viam.graph.control.StartNode;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.ProcCallNode;
import vadl.viam.graph.dependency.ReadResourceNode;
import vadl.viam.graph.dependency.SideEffectNode;
import vadl.viam.graph.dependency.WriteRegTensorNode;
import vadl.viam.passes.sideeffect_condition.SideEffectConditionResolver;

/**
 * Moves all side effects which cause an instruction exit to the end of the instruction, while
 * preserving the condition under which the moved side effects are executed. It only executes
 * on instruction and procedure behaviors.
 *
 * <p>The pass first traverses the graph and saves the conditions of the side effects. This is
 * done in a similar way as {@link SideEffectConditionResolver} does it. The conditions are not
 * combined using dis- and conjunctions however, but kept separate. This way, the original
 * control flow structure can be reconstructed exactly.
 *
 * <p>The pass then inserts nested if-blocks just before the end of the graph. Disjunct conditions
 * are translated to disjunct if-blocks. Conjunct conditions are grouped two groups: Static and
 * dynamic. Inside the groups, the conditions are then conjoined and then used to emit at most
 * two nested if-blocks (one static and one dynamic). Conjoining all conditions could hide static
 * conditions, which would then be emitted as dynamic (TCG instructions).
 *
 * <p>TODO: In the future this pass should also move non-memory side effects behind memory
 *     side effects, see Issue #1082.
 *
 * <p>TODO: Grouping conjunct conditions into static and dynamic preserves much optimization
 *     potential. A thorough condition analysis pass could expose more potential by extracting
 *     static parts out of otherwise dynamic conditions.
 */
public class IssSideEffectReorderingPass extends Pass {

  public IssSideEffectReorderingPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("Instruction Exit Scheduling Pass");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    var defs = ViamUtils.findDefinitionsByFilter(viam, def ->
        def instanceof Instruction || def instanceof Procedure);
    for (var def : defs) {
      ((DefProp.WithBehavior) def).behaviors().forEach(this::moveInstrExitSideEffectsToInstrEnd);
    }
    return null;
  }

  /**
   * Moves all side effects causing an instruction exit to the end of the instruction graph.
   * Unconditional side effects are simply added to the instruction end node. Conditional
   * side effect nodes are put in if-blocks.
   *
   * @param behaviour The graph to process.
   */
  private void moveInstrExitSideEffectsToInstrEnd(Graph behaviour) {
    var sideEffectConditions = SideEffectConditionCollector.collectConditions(behaviour);
    var instrEndNodeClass = switch (behaviour.parentDefinition()) {
      case Instruction i -> InstrEndNode.class;
      case Procedure p -> ProcEndNode.class;
      case Stage s -> StageEndNode.class;
      default -> throw new IllegalStateException("Unexpected graph parent");
    };
    var instrEnd = GraphUtils.getSingleNode(behaviour, instrEndNodeClass);
    var instrExitSideEffects = behaviour.getNodes(SideEffectNode.class).filter(
        s -> (s instanceof WriteRegTensorNode write && write.isPcAccess())
            || (s instanceof ProcCallNode procCall && procCall.exceptionRaise())
    ).toList();

    for (var instrExit : instrExitSideEffects) {
      // this is a list of all places the side effect is scheduled. for each place, it contains
      // a list of all the conditions of the if-clauses containing the side effect
      var conditions = requireNonNull(sideEffectConditions.get(instrExit));
      instrExit.usages().filter(AbstractEndNode.class::isInstance).toList()
          .forEach(user -> ((AbstractEndNode) user).removeSideEffect(instrExit));
      // go through all scheduled instances of the side effect
      for (var instanceCondition : conditions) {
        if (instanceCondition.isEmpty()) {
          instrEnd.addSideEffect(instrExit);
        } else {
          // For each, create nested if-blocks, with the conditions of the original if-blocks.
          // We could use a conjunction of all the conditions, but that makes the simulation slower:
          // Say we have a static and dynamic condition, then the conjunction is dynamic, and
          // optimization potential is lost.
          // TODO: In the future, we should make the branch lowering more intelligent: It should
          //  be able to take apart conditions into their static and dynamic parts, which would
          //  allow the translation function to evaluate parts of the condition at translation time.
          var pred = requireNonNull(instrEnd.predecessor());
          pred.unlinkNext();
          var collapsedInstanceCondition = collapseConditions(instanceCondition);
          pred.setNext(addInNestedIf(collapsedInstanceCondition, instrEnd, behaviour, instrExit));
        }
      }
    }
  }

  /**
   * Groups the conditions into static and dynamic. Creates a conjunction out of each group.
   * Returns the conjunctions in a list. May return list of size 2, 1 or empty.
   */
  private static List<ExpressionNode> collapseConditions(List<ExpressionNode> conditions) {
    return conditions.stream().collect(Collectors.partitioningBy(
        c -> GraphUtils.hasDependencies(c, dep -> dep instanceof ReadResourceNode)
    )).values().stream().flatMap(
        c -> c.stream().reduce((a, b) -> BuiltInCall.of(BuiltInTable.AND, a, b)).stream()
    ).toList();
  }

  /**
   * Puts the given side effect into nested if-blocks. For each given condition in the list,
   * an if-block is created.
   */
  private static ControlNode addInNestedIf(List<ExpressionNode> conditions, ControlNode next,
                                           Graph graph, SideEffectNode se) {
    if (conditions.isEmpty()) {
      throw new IllegalStateException("Expected non-empty list of conditions");
    }
    if (conditions.size() == 1) {
      return GraphUtils.ifElseSideEffect(
          graph,
          conditions.getFirst(),
          List.of(se),
          List.of(),
          next,
          se.location()
      );
    }
    var pair = GraphUtils.insertIfElse(
        graph,
        conditions.getFirst(),
        (g, end) -> addInNestedIf(
            conditions.subList(1, conditions.size()),
            end, graph, se
        ),
        (g, end) -> end,
        se.location()
    );
    pair.right().setNext(next);
    return pair.left();
  }
}

class SideEffectConditionCollector {

  private final HashMap<SideEffectNode, List<List<ExpressionNode>>> conditions = new HashMap<>();

  /**
   * Collect all side effect conditions. Each side effect gets a list of all instances, in
   * which the side effect node is scheduled. Each instance is a list of all the conditions
   * which must be met, such that that instance is reached.
   *
   * <p>For example:
   * <pre>{@code
   * if (a) { if (b) {} else { se_0 } }
   * if (c) { se_0 }
   * }</pre>
   * yields:
   * <pre>{@code
   * { se_0: [ [a, -b], [c] ] }
   * }</pre>
   */
  public static Map<SideEffectNode, List<List<ExpressionNode>>> collectConditions(
      Graph behavior) {
    var collector = new SideEffectConditionCollector();
    collector.collect(behavior);
    return collector.conditions;
  }

  private void collect(Graph behavior) {
    var start = getSingleNode(behavior, StartNode.class);
    resolveBranch(start, new ArrayList<>());
  }

  /**
   * Recursively traverses the CFG. Jumps over regular directional nodes. When an if-block
   * is encountered, its condition is added to branchCondition while traversing the true-branch
   * and removed afterward. The same is done for the negation of the condition for the false-branch.
   */
  private @Nullable MergeNode resolveBranch(AbstractBeginNode beginNode,
                                            List<ExpressionNode> branchCondition) {
    // the current control node
    ControlNode current = beginNode;

    // loop is only terminated by return of AbstractEndNode
    while (true) {

      switch (current) {
        case AbstractEndNode endNode -> {
          return handleEndNode(endNode, branchCondition);
        }
        case IfNode ifNode -> current = handleIf(ifNode, branchCondition);
        // forall as no special handling required, as it doesn't influence the condition
        case ForallNode forallNode -> current = forallNode.beginNode();
        // handle normal singled directed node by just skipping it and continue
        case DirectionalNode directionalNode -> current = directionalNode.next();
        // there should not be an other control node that was not handled yet
        default -> //noinspection DataFlowIssue
            current.ensure(false,
                "Not an expected node in the SideEffectConditionCollector. "
                    + "You want to implement it.");
      }
    }
  }

  @Nullable
  private MergeNode handleEndNode(AbstractEndNode endNode, List<ExpressionNode> branchCondition) {
    // handle the end of the current branch
    var graph = endNode.graph();
    endNode.ensure(graph != null,
        "Node is not active, but control flow must be stable for SideEffectConditionResolver");

    // add the condition to all side effects
    for (var sideEffect : endNode.sideEffects()) {
      conditions.computeIfAbsent(sideEffect, s -> new ArrayList<>())
          .add(List.copyOf(branchCondition));
    }

    // find and return the merge node if available
    // (only in case of an InstrEndNode the MergeNode is not available)
    return endNode.usages()
        .filter(user -> user instanceof MergeNode)
        .map(MergeNode.class::cast)
        .findAny()
        .orElse(null);
  }

  @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
  private MergeNode handleIf(IfNode ifNode, List<ExpressionNode> branchCondition) {

    branchCondition.addLast(ifNode.condition());
    var trueMergeNode = resolveBranch(ifNode.trueBranch(), branchCondition);
    branchCondition.removeLast();

    branchCondition.addLast(BuiltInCall.of(BuiltInTable.NOT, ifNode.condition()));
    var falseMergeNode = resolveBranch(ifNode.falseBranch(), branchCondition);
    branchCondition.removeLast();

    // MergeNode must be the same for all branches and not null
    ifNode.ensure(trueMergeNode == falseMergeNode,
        "Branches of node don't result in the same merge node");
    ifNode.ensure(trueMergeNode != null,
        "Couldn't find merge node for true branch");

    // continue with the found mergeNode
    return trueMergeNode;
  }

}
