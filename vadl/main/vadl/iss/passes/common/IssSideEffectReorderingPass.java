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
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.error.Diagnostic;
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
import vadl.viam.graph.control.ControlSplitNode;
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
import vadl.viam.graph.dependency.WriteMemNode;
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
 *     This could be done with some form of shannon expansion, but must be done carefully to
 *     avoid an exponential blowup in code size.
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
    // FIXME: first find all instr exit side effects that must be moved
    //        then move them to instr end (or instr start)
    //        finally clean up CFG
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
    var sideEffectInstances = SideEffectInstanceCollector.collectInstances(behaviour);
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
      var instances = requireNonNull(sideEffectInstances.get(instrExit));
      instrExit.ensure(instances.stream()
          .anyMatch(SideEffectInstanceCollector.SideEffectInstance::insideForall),
          "Side effects causing an instruction exit are not supported inside forall statements"
      );
      // go through all scheduled instances of the side effect
      for (var instance : instances) {
        if (!instance.hasIfSibling()) {
          // The side effect must not be moved into a new if-construct, since ordering
          // the side effects inside the containing if-constructs is sufficient to ensure
          // a correct order (this is done by SideEffectSchedulingPass).
          continue;
        }
        requireNonNull(instance.location()).removeSideEffect(instrExit);
        // TODO: remove the if-construct if it is now empty
        if (instance.conditions().isEmpty()) {
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
          var collapsedInstanceCondition = collapseConditions(instance.conditions());
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

class SideEffectReorderer {

  enum SideEffectType {
    MEM, SE, PC
  }

  enum SideEffectTypeTransition {
    MEM_SE, SE_PC
  }

  static class SideEffectCount {
    public int memCnt = 0;
    public int seCnt = 0;
    public int pcCnt = 0;

    public SideEffectCount() {}

    private SideEffectCount(int memCnt, int seCnt, int pcCnt) {
      this.memCnt = memCnt;
      this.seCnt = seCnt;
      this.pcCnt = pcCnt;
    }

    public static SideEffectCount add(SideEffectCount first, SideEffectCount second) {
      return new SideEffectCount(
          first.memCnt + second.memCnt,
          first.seCnt + second.seCnt,
          first.pcCnt + second.pcCnt
      );
    }

    public void add(SideEffectCount other) {
      memCnt += other.memCnt;
      seCnt  += other.seCnt;
      pcCnt  += other.pcCnt;
    }

    public void inc(SideEffectType type) {
      switch (type) {
        case MEM -> memCnt++;
        case SE  -> seCnt++;
        case PC  -> pcCnt++;
      }
    }

    public int total() {
      return memCnt + seCnt + pcCnt;
    }

    public SideEffectType mostCommonType() {
      if (memCnt > seCnt) {
        if (memCnt > pcCnt) {
          return SideEffectType.MEM;
        }
        return SideEffectType.PC;
      }
      if (seCnt > pcCnt) {
        return SideEffectType.SE;
      }
      return SideEffectType.PC;
    }
  }

  record SideEffectTypes(Set<SideEffectType> types) {
    public static SideEffectTypes of(Set<SideEffectType> types) {
      return new SideEffectTypes(types);
    }

    public SideEffectTypes combine(SideEffectTypes other) {
      return new SideEffectTypes(
          Stream.concat(other.types.stream(), types.stream()).collect(Collectors.toSet()));
    }

    public boolean contains(SideEffectType type) {
      return types.contains(type);
    }

    private int min() {
      return types.stream().mapToInt(Enum::ordinal).min()
          .orElseThrow(() -> new IllegalStateException("Statement without side effects"));
    }

    private int max() {
      return types.stream().mapToInt(Enum::ordinal).max()
          .orElseThrow(() -> new IllegalStateException("Statement without side effects"));
    }

    private int range() {
      return max() - min();
    }

    private boolean isMemSe() {
      return min() == SideEffectType.MEM.ordinal() && max() == SideEffectType.SE.ordinal();
    }

    private boolean isSePc() {
      return min() == SideEffectType.SE.ordinal() && max() == SideEffectType.PC.ordinal();
    }

    private boolean isMemPc() {
      return min() == SideEffectType.MEM.ordinal() && max() == SideEffectType.PC.ordinal();
    }

    boolean conflictsWith(SideEffectTypes other) {
      if (types.isEmpty() || other.types.isEmpty()) {
        return true;
      }
      return max() > other.min() && min() < other.max();
    }
  }

  private final Graph behavior;
  private final HashMap<ControlSplitNode, SideEffectTypes> sideEffectTypes;
  private final HashMap<ControlSplitNode, SideEffectCount> sideEffectCount;
  private final HashMap<SideEffectNode, List<List<ExpressionNode>>> sideEffectConditions;

  SideEffectReorderer(Graph behavior) {
    this.behavior = behavior;
    this.sideEffectTypes = new HashMap<>();
    this.sideEffectCount = new HashMap<>();
    this.sideEffectConditions = new HashMap<>();
  }

  public static void run(Graph behavior) {
    new SideEffectReorderer(behavior).reorder();
  }

  private void reorder() {
    new SideEffectInfoCollector().run();
  }

  /**
   * Collects conditions for all side effects.
   * Collects types and amount of side effects for each control flow split (so if-else and forall).
   */
  class SideEffectInfoCollector {

    record Result(@Nullable MergeNode mergeNode, SideEffectTypes seTypes, SideEffectCount seCount) {
    }

    public void run() {
      var start = getSingleNode(behavior, StartNode.class);
      resolveBranch(start, new ArrayList<>());
    }

    private Result resolveBranch(AbstractBeginNode beginNode,
                                 List<ExpressionNode> conditions) {
      ControlNode current = beginNode;
      var seTypes = new SideEffectTypes(Set.of());
      var seCount = new SideEffectCount();
      while (true) {
        switch (current) {
          case AbstractEndNode endNode -> {
            var sideEffects = endNode.sideEffects();
            for (var se : sideEffects) {
              sideEffectConditions.computeIfAbsent(se,
                  s -> new ArrayList<>()).add(List.copyOf(conditions));
              seCount.inc(classify(se));
            }
            var mergeNode = endNode.usages()
                .filter(user -> user instanceof MergeNode)
                .map(MergeNode.class::cast)
                .findAny()
                .orElse(null);
            seTypes = seTypes.combine(new SideEffectTypes(
                sideEffects.stream().map(this::classify).collect(Collectors.toSet())));
            return new Result(mergeNode, seTypes, seCount);
          }
          case IfNode ifNode -> {
            var result = handleIf(ifNode, conditions);
            sideEffectTypes.put(ifNode, result.seTypes);
            sideEffectCount.put(ifNode, result.seCount);
            seTypes = seTypes.combine(result.seTypes);
            seCount.add(result.seCount);
            current = requireNonNull(result.mergeNode);
          }
          case ForallNode forallNode -> {
            var result = handleForall(forallNode, conditions);
            sideEffectTypes.put(forallNode, result.seTypes);
            sideEffectCount.put(forallNode, result.seCount);
            seTypes = seTypes.combine(result.seTypes);
            seCount.add(result.seCount);
            current = requireNonNull(result.mergeNode);
          }
          case DirectionalNode directionalNode -> current = directionalNode.next();
          default -> //noinspection DataFlowIssue
              current.ensure(false,
                  "Not an expected node in the SideEffectReorderer. "
                      + "You want to implement it.");
        }
      }
    }

    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    private Result handleIf(IfNode ifNode, List<ExpressionNode> conditions) {

      conditions.addLast(ifNode.condition());
      var trueResult = resolveBranch(ifNode.trueBranch(), conditions);
      conditions.removeLast();

      conditions.addLast(BuiltInCall.of(BuiltInTable.NOT, ifNode.condition()));
      var falseResult = resolveBranch(ifNode.falseBranch(), conditions);
      conditions.removeLast();

      // MergeNode must be the same for all branches and not null
      ifNode.ensure(trueResult.mergeNode == falseResult.mergeNode,
          "Branches of node don't result in the same merge node");
      ifNode.ensure(trueResult.mergeNode != null,
          "Couldn't find merge node for true branch");

      return new Result(
          trueResult.mergeNode,
          trueResult.seTypes.combine(falseResult.seTypes),
          SideEffectCount.add(trueResult.seCount, falseResult.seCount)
      );
    }

    private Result handleForall(ForallNode forallNode, List<ExpressionNode> conditions) {
      // forall nodes must be handled in its own resolveBranch call to ensure
      // it is correctly stepped out of
      var result = resolveBranch(forallNode.beginNode(), conditions);
      forallNode.ensure(result.mergeNode != null, "Couldn't find merge node for forall branch");
      return result;
    }

    private SideEffectType classify(SideEffectNode node) {
      if (node instanceof WriteMemNode) {
        return SideEffectType.MEM;
      } else if (node instanceof WriteRegTensorNode write && write.isPcAccess()
          || node instanceof ProcCallNode procCall && procCall.exceptionRaise()) {
        return SideEffectType.PC;
      }
      return SideEffectType.SE;
    }
  }

  class Reorderer {

    public void run() {
      var start = getSingleNode(behavior, StartNode.class);
      handleBranch(start);
    }

    private void handleBranch(AbstractBeginNode beginNode) {
      ControlNode current = beginNode;

      var ifNodes = new ArrayList<IfNode>();
      var forallNodes = new ArrayList<ForallNode>();

      loop: while (true) {
        switch (current) {
          case AbstractEndNode endNode -> {
            break loop;
          }
          case IfNode ifNode -> {
            ifNodes.add(ifNode);
            current = ifNode.mergeNode(); // TODO: this is kinda slow
          }
          case ForallNode forallNode -> {
            forallNodes.add(forallNode);
            current = forallNode.mergeNode(); // TODO: same here
          }
          case DirectionalNode directionalNode -> current = directionalNode.next();
          default -> //noinspection DataFlowIssue
              current.ensure(false,
                  "Not an expected node in the SideEffectReorderer. "
                      + "You want to implement it.");
        }
      }

      // ensure than no two forall blocks have conflicting side effects, since we cannot
      // extract from forall blocks
      // TODO: or maybe we can? e.g. if in a forall both MEM and some register are written,
      //       we could split that into two forall constructs...
      for (int i = 0; i < forallNodes.size(); i++) {
        for (int j = 0; j < i; j++) {
          var forall0 = forallNodes.get(i);
          var forall1 = forallNodes.get(j);
          var seTypes0 = sideEffectTypes.get(forall0);
          var seTypes1 = sideEffectTypes.get(forall1);
          if (seTypes0.conflictsWith(seTypes1)) {
            throw Diagnostic.error(
                "Side-effects in two forall constructs cannot be ordered", forall0
            ).locationNote(forall1, "Conflicting forall construct").build();
          }
        }
      }

      // there can only be a few constructs with multiple side effect types:
      // either both MEM -> SE and SE -> PC
      // or just MEM -> PC
      // lets call those "slots"
      boolean memSeTransitionUsed = false;
      boolean sePcTransitionUsed = false;

      // forall nodes cannot be extracted out of, so those are fixed
      for (var forall : forallNodes) {
        var seTypes = sideEffectTypes.get(forall);
        if (seTypes.min() <= SideEffectType.MEM.ordinal()
            && seTypes.max() >= SideEffectType.SE.ordinal()) {
          memSeTransitionUsed = true;
        }
        if (seTypes.min() <= SideEffectType.SE.ordinal()
            && seTypes.max() >= SideEffectType.PC.ordinal()) {
          sePcTransitionUsed = true;
        }
      }

      // sort by side effect count (largest to smallest), such that if-constructs with many
      // side effects are more likely to be frozen
      ifNodes.sort((n0, n1) -> sideEffectCount.get(n1).total() - sideEffectCount.get(n0).total());

      // we try to find if-constructs to fit into the slots
      // these are "frozen", meaning we will not extract side effects out of them
      var frozenIfNodes = new ArrayList<>();

      if (!memSeTransitionUsed) { // MEM -> SE slot
        var candidate = ifNodes.stream().filter(n -> sideEffectTypes.get(n).isMemSe()).findFirst();
        if (candidate.isPresent()) {
          frozenIfNodes.add(candidate.get());
          memSeTransitionUsed = true;
        }
      }
      if (!sePcTransitionUsed) { // SE -> PC slot
        var candidate = ifNodes.stream().filter(n -> sideEffectTypes.get(n).isSePc()).findFirst();
        if (candidate.isPresent()) {
          frozenIfNodes.add(candidate.get());
          sePcTransitionUsed = true;
        }
      }
      if (!memSeTransitionUsed && !sePcTransitionUsed) { // MEM -> PC slot
        var candidate = ifNodes.stream().filter(n -> sideEffectTypes.get(n).isMemPc()).findFirst();
        if (candidate.isPresent()) {
          frozenIfNodes.add(candidate.get());
          memSeTransitionUsed = sePcTransitionUsed = true;
        }
      }

      // extract out of remaining if-constructs, such that they each only contain one se type
      ifNodes.forEach(node -> {
        var isolatedType = handleIf(node, !frozenIfNodes.contains(node));
        if (isolatedType != null) {
          // update the type, such that the reordering later knows that only one type remains
          sideEffectTypes.put(node, SideEffectTypes.of(Set.of(isolatedType)));
        }
      });
      forallNodes.forEach(this::handleForall);

      // now it is possible to reorder everything
      // TODO: reorder the if- and forall-constructs
      //       the side-effects themselves also need to be scheduled, which is done by the
      //       SideEffectSchedulingPass, but it must be adapted to handle three types now
    }

    /**
     * Reorders the side effects inside the if-construct. If necessary, extracts side effects
     * until only one type of side effect remains.
     *
     * @param ifNode The node to handle.
     * @param extract Whether to extract side effects.
     * @return If extracting, the side effect type remaining inside, otherwise null.
     */
    @Nullable
    private SideEffectType handleIf(IfNode ifNode, boolean extract) {
      if (extract) {
        var seType = sideEffectCount.get(ifNode).mostCommonType();
        // TODO: extract everything that is different
        return seType;
      }
      // only if we do not extract and multiple se types remain do we need to reorder
      handleBranch(ifNode.trueBranch());
      handleBranch(ifNode.falseBranch());
      return null;
    }

    /**
     * Reorders the side effects inside the forall-construct.
     *
     * @param forallNode The node to handle.
     */
    private void handleForall(ForallNode forallNode) {
      handleBranch(forallNode.beginNode());
    }

  }





}

class SideEffectInstanceCollector {

  /**
   * Stores the condition under which a side effect is executed. Represents the conditions
   * for a single instance of the side effect (so if e.g. a register write is scheduled
   * in multiple locations in the CFG, each location is represented by a separate instance of
   * this record).
   *
   * @param conditions The conjunctive conditions of the side effect instance.
   * @param location The control node at which the side effect is scheduled.
   * @param insideForall Whether the side effect is inside a forall construct.
   * @param hasIfSibling Whether any of the surrounding if-constructs has a sibling.
   */
  public record SideEffectInstance(List<ExpressionNode> conditions,
                                   @Nullable AbstractEndNode location,
                                   boolean insideForall, boolean hasIfSibling) {

    public void push(ExpressionNode condition) {
      conditions.addLast(condition);
    }

    public void pop() {
      conditions.removeLast();
    }

    public SideEffectInstance withInsideForall() {
      return new SideEffectInstance(conditions, location, true, hasIfSibling);
    }

    public SideEffectInstance withIfSibling() {
      return new SideEffectInstance(conditions, location, insideForall, true);
    }

    public SideEffectInstance createInstance(AbstractEndNode location) {
      return new SideEffectInstance(List.copyOf(conditions), location, insideForall, hasIfSibling);
    }

    static SideEffectInstance empty() {
      return new SideEffectInstance(new ArrayList<>(), null, false, false);
    }
  }

  private final HashMap<SideEffectNode, List<SideEffectInstance>> conditions = new HashMap<>();

  /**
   * Collect all side effect instances. Each side effect gets a list of all instances, in
   * which the side effect node is scheduled. Each instance contains a list of all the conditions
   * which must be met, such that that instance is reached.
   *
   * <p>For example:
   * <pre>{@code
   * if (a) { if (b) {} else { se_0 } }
   * if (c) { se_0 }
   * }</pre>
   * yields:
   * <pre>{@code
   * { se_0: [ { [a, -b], ...}, { [c], ... } ] }
   * }</pre>
   */
  public static Map<SideEffectNode, List<SideEffectInstance>> collectInstances(Graph behavior) {
    var collector = new SideEffectInstanceCollector();
    collector.collect(behavior);
    return collector.conditions;
  }

  private void collect(Graph behavior) {
    var start = getSingleNode(behavior, StartNode.class);
    resolveBranch(start, SideEffectInstance.empty());
  }

  /**
   * Recursively traverses the CFG. Jumps over regular directional nodes. When an if-block
   * is encountered, its condition is added to branchCondition while traversing the true-branch
   * and removed afterward. The same is done for the negation of the condition for the false-branch.
   */
  private @Nullable MergeNode resolveBranch(AbstractBeginNode beginNode,
                                            SideEffectInstance branchCondition) {
    // the current control node
    ControlNode current = beginNode;
    var ifNodes = new ArrayList<IfNode>();
    var forallNodes = new ArrayList<ForallNode>();

    // loop is only terminated by return of AbstractEndNode
    while (true) {

      switch (current) {
        case AbstractEndNode endNode -> {
          var ifNodeCond = ifNodes.size() > 1 ? branchCondition.withIfSibling() : branchCondition;
          ifNodes.forEach(node -> handleIf(node, ifNodeCond));
          forallNodes.forEach(node -> handleForall(node, branchCondition));
          return handleEndNode(endNode, branchCondition);
        }
        case IfNode ifNode -> {
          current = ifNode.mergeNode();
          ifNodes.add(ifNode);
        }
        case ForallNode forallNode -> {
          current = forallNode.mergeNode();
          forallNodes.add(forallNode);
        }
        // handle normal singled directed node by just skipping it and continue
        case DirectionalNode directionalNode -> current = directionalNode.next();
        // there should not be an other control node that was not handled yet
        default -> //noinspection DataFlowIssue
            current.ensure(false,
                "Not an expected node in the SideEffectInstanceCollector. "
                    + "You want to implement it.");
      }
    }
  }

  @Nullable
  private MergeNode handleEndNode(AbstractEndNode endNode, SideEffectInstance branchCondition) {
    // handle the end of the current branch
    var graph = endNode.graph();
    endNode.ensure(graph != null,
        "Node is not active, but control flow must be stable for IssSideEffectReorderingPass");

    // add the condition to all side effects
    for (var sideEffect : endNode.sideEffects()) {
      conditions.computeIfAbsent(sideEffect,
          s -> new ArrayList<>()).add(branchCondition.createInstance(endNode));
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
  private MergeNode handleIf(IfNode ifNode, SideEffectInstance branchCondition) {

    branchCondition.push(ifNode.condition());
    var trueMergeNode = resolveBranch(ifNode.trueBranch(), branchCondition);
    branchCondition.pop();

    branchCondition.push(BuiltInCall.of(BuiltInTable.NOT, ifNode.condition()));
    var falseMergeNode = resolveBranch(ifNode.falseBranch(), branchCondition);
    branchCondition.pop();

    // MergeNode must be the same for all branches and not null
    ifNode.ensure(trueMergeNode == falseMergeNode,
        "Branches of node don't result in the same merge node");
    ifNode.ensure(trueMergeNode != null,
        "Couldn't find merge node for true branch");

    // continue with the found mergeNode
    return trueMergeNode;
  }

  private MergeNode handleForall(ForallNode forallNode, SideEffectInstance branchCondition) {
    // forall nodes must be handled in its own resolveBranch call to ensure
    // it is correctly stepped out of
    var mergeNode = resolveBranch(forallNode.beginNode(), branchCondition.withInsideForall());
    forallNode.ensure(mergeNode != null, "Couldn't find merge node for forall branch");
    return mergeNode;
  }

}
