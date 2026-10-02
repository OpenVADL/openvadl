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
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.iss.SideEffectUtils;
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
import vadl.viam.graph.Graph;
import vadl.viam.graph.control.AbstractBeginNode;
import vadl.viam.graph.control.AbstractEndNode;
import vadl.viam.graph.control.BranchEndNode;
import vadl.viam.graph.control.ControlNode;
import vadl.viam.graph.control.ControlSplitNode;
import vadl.viam.graph.control.DirectionalNode;
import vadl.viam.graph.control.ForallNode;
import vadl.viam.graph.control.IfNode;
import vadl.viam.graph.control.MergeNode;
import vadl.viam.graph.control.StartNode;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.ForIdxNode;
import vadl.viam.graph.dependency.SideEffectNode;
import vadl.viam.passes.CfgTraverser;
import vadl.viam.passes.sideEffectScheduling.SideEffectSchedulingPass;

/**
 * Reorders side effects such that memory writes come before all other side effects
 * and instruction exiting side effects (such as PC changes and exception raises) come last.
 *
 * <p>Runs two passes over each graph: The first collects information about the CFG structure,
 * conditions and side effects. The second uses that information to reorder side effects only
 * where necessary and uses a heuristical approach to make minimal changes.
 *
 * <p>The algorithm to reorder the side effects and their containing control flow constructs
 * works as follows:
 * <ul>
 *   <li>Find and classify all side effects in the graph. The three classes are
 *   {@code MEM}, {@code SE} and {@code PC}. See {@link SideEffectUtils.SideEffectType}.</li>
 *   <li>Traverse the CFG and find sibling control flow blocks.</li>
 *   <li>Extract side effects out of blocks into their own blocks at the same level until
 *   all blocks can be ordered such that the ordering of all side effects is guaranteed.</li>
 *   <li>Reorder the blocks.</li>
 * </ul>
 *
 * <p>The hard part of the algorithm is finding the smallest set of side effects to extract,
 * such that all sibling blocks at a level can be ordered. For each block, we first find out
 * what range of side effect types it covers (e.g. {@code MEM..SE}). We realize that (excluding all
 * blocks covering only one type) only the following two cases are allowed:
 *
 * <ul>
 *   <li>A single block covering all three types {@code MEM..PC}.</li>
 *   <li>Two blocks covering {@code MEM..SE} and {@code SE..PC}.</li>
 * </ul>
 *
 * <p>All other blocks may at most cover one side effect type. We prefer to keep blocks with
 * the many side effects and also prefer the second case. We choose up to two blocks to keep
 * (let's call them "frozen"), and must extract side effects out of all other blocks until only
 * one type remains in each block. We keep track of the number of each side effect type in each
 * block, and keep the type with the highest count.
 *
 * <p>Example:
 * <pre>{@code
 * if (a) { PC, SE, SE }
 * if (b) { PC, SE, SE, SE }
 * }</pre>
 *
 * <p>Here the two blocks cannot be reordered (they are conflicting). We freeze the second block,
 * since it has the most side effects. We choose to extract all {@code PC} side effects out of
 * the first block, because that type has the least occurrences in the block. The final, reordered
 * code looks like this:
 *
 * <pre>{@code
 * if (a) { PC }
 * if (b) { PC, SE, SE, SE }
 * if (a) { SE, SE }
 * }</pre>
 *
 * <p>The scheduling of side effects inside blocks in the correct order is handled by
 * {@link SideEffectSchedulingPass}.
 *
 * <p>TODO: memory loads are a different story. They are not side effects and cannot be handled
 *          here. I think the IssSafeResourceReadPass could be adapted to ensure that all
 *          memory loads happen before all side effects. Also, using reg dest TCGvs as temp
 *          storage before memory loads is a source of errors.
 */
public class IssSideEffectReorderingPass extends Pass {

  public IssSideEffectReorderingPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("Instruction Side Effect Reordering Pass");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    var defs = ViamUtils.findDefinitionsByFilter(viam, def ->
        def instanceof Instruction || def instanceof Procedure);
    for (var def : defs) {
      ((DefProp.WithBehavior) def).behaviors().forEach(SideEffectReorderer::run);
    }
    return null;
  }
}

class SideEffectReorderer {

  /**
   * Stores a count for each of the side effect types (see {@link SideEffectUtils.SideEffectType}).
   */
  static class SideEffectCount {
    public int memCnt = 0;
    public int seCnt = 0;
    public int pcCnt = 0;

    public SideEffectCount() {}

    public SideEffectCount add(SideEffectCount other) {
      memCnt += other.memCnt;
      seCnt  += other.seCnt;
      pcCnt  += other.pcCnt;
      return this;
    }

    public SideEffectCount inc(SideEffectUtils.SideEffectType type) {
      switch (type) {
        case MEM -> memCnt++;
        case SE  -> seCnt++;
        case PC  -> pcCnt++;
      }
      return this;
    }

    public int total() {
      return memCnt + seCnt + pcCnt;
    }

    public SideEffectUtils.SideEffectType mostCommonType() {
      if (memCnt > seCnt) {
        if (memCnt > pcCnt) {
          return SideEffectUtils.SideEffectType.MEM;
        }
        return SideEffectUtils.SideEffectType.PC;
      }
      if (seCnt > pcCnt) {
        return SideEffectUtils.SideEffectType.SE;
      }
      return SideEffectUtils.SideEffectType.PC;
    }
  }

  private final Graph behavior;

  /**
   * Maps each control flow block to the set of side effect types, which are present inside.
   */
  private final IdentityHashMap<ControlSplitNode, SideEffectUtils.SideEffectTypes> sideEffectTypes;

  /**
   * Maps each control flow block to the number of each side effect type, which are present inside.
   */
  private final IdentityHashMap<ControlSplitNode, SideEffectCount> sideEffectCount;

  /**
   * Maps each side effect to a map, which maps each occurrence to the list of expressions,
   * which represent the conditions/indices of all if-/forall-blocks surrounding the instance
   * (from outer to inner).
   */
  private final IdentityHashMap<SideEffectNode, Map<AbstractEndNode, List<ExpressionNode>>>
      sideEffectConditions;

  SideEffectReorderer(Graph behavior) {
    this.behavior = behavior;
    this.sideEffectTypes = new IdentityHashMap<>();
    this.sideEffectCount = new IdentityHashMap<>();
    this.sideEffectConditions = new IdentityHashMap<>();
  }

  public static void run(Graph behavior) {
    new SideEffectReorderer(behavior).reorder();
  }

  private void reorder() {
    new SideEffectInfoCollector().run();
    new Reorderer().run();
  }

  /**
   * Collects information about the CFG and the side effects in a graph, which is later
   * used by the {@link Reorderer}.
   *
   * <p>For each side effect, all instances of that side effect in the CFG are stored. The storage
   * maps the node at which the instance is scheduled to a list of conditions, which is the
   * conditions/indices of all surrounding if-/forall-blocks.
   *
   * <p>For each if- and forall-block, a set and count of the side effect types in the block
   * is stored.
   */
  class SideEffectInfoCollector {

    record Result(@Nullable MergeNode mergeNode,
                  SideEffectUtils.SideEffectTypes seTypes,
                  SideEffectCount seCount) {}

    public void run() {
      var start = getSingleNode(behavior, StartNode.class);
      resolveBranch(start, new ArrayList<>());
    }

    private Result resolveBranch(AbstractBeginNode beginNode,
                                 List<ExpressionNode> conditions) {
      ControlNode current = beginNode;
      var seTypes = SideEffectUtils.SideEffectTypes.empty();
      var seCount = new SideEffectCount();
      while (true) {
        switch (current) {
          case AbstractEndNode endNode -> {
            var sideEffects = endNode.sideEffects();
            for (var se : sideEffects) {
              sideEffectConditions.computeIfAbsent(se,
                  s -> new HashMap<>()).put(endNode, List.copyOf(conditions));
              seCount.inc(SideEffectUtils.SideEffectType.classify(se));
            }
            var mergeNode = endNode.usages()
                .filter(user -> user instanceof MergeNode)
                .map(MergeNode.class::cast)
                .findAny()
                .orElse(null);
            seTypes = seTypes.combine(new SideEffectUtils.SideEffectTypes(
                sideEffects.stream().map(SideEffectUtils.SideEffectType::classify)
                    .collect(Collectors.toSet())));
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
          new SideEffectCount().add(trueResult.seCount).add(falseResult.seCount)
      );
    }

    private Result handleForall(ForallNode forallNode, List<ExpressionNode> conditions) {
      conditions.addLast(forallNode.idx());
      var result = resolveBranch(forallNode.beginNode(), conditions);
      conditions.removeLast();
      forallNode.ensure(result.mergeNode != null, "Couldn't find merge node for forall branch");
      return result;
    }
  }

  /**
   * Uses the information collected by the {@link SideEffectInfoCollector} to reorder the
   * side effects in the graph, such that the total order imposed by their types
   * (see {@link SideEffectUtils.SideEffectType}) is upheld.
   *
   * <p>Note: modifies the information provided by {@link SideEffectInfoCollector} for its
   * own purposes and leaves it in an incomplete/inconsistent state.
   */
  class Reorderer {

    public void run() {
      var start = getSingleNode(behavior, StartNode.class);
      reorderBranch(start, 0);
    }

    /**
     * Reorders the side effects inside a branch if necessary. It does this in two steps:
     *
     * <p>First, it finds all conflicting sibling if- and forall-constructs and extracts
     * side effects out of them until no siblings are conflicting anymore.
     *
     * <p>It then reorders the sibling constructs such that all MEM side effects come before
     * all other side effects and all PC side effects come after all other side effects
     * (see {@link SideEffectUtils.SideEffectType}).
     *
     * @param beginNode The begin node of the branch.
     * @param depth The number of blocks this branch is inside of.
     */
    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    private void reorderBranch(AbstractBeginNode beginNode, int depth) {
      ControlNode current = beginNode;

      var blocks = new ArrayList<ControlSplitNode>();

      // collect all if- and forall-blocks at the current branch level
      loop: while (true) {
        switch (current) {
          case AbstractEndNode endNode -> {
            break loop;
          }
          case ControlSplitNode splitNode -> {
            blocks.add(splitNode);
            current = splitNode.mergeNode(); // TODO: this is kinda slow
          }
          case DirectionalNode directionalNode -> current = directionalNode.next();
          default -> //noinspection DataFlowIssue
              current.ensure(false,
                  "Not an expected node in the SideEffectReorderer. "
                      + "You want to implement it.");
        }
      }

      if (blocks.size() <= 1) {
        // if zero or only one inner blocks exists, then nothing on this level must be reordered
        blocks.forEach(b -> reorderBlock(b, depth));
        return;
      }

      // there can only be a few blocks with multiple side effect types:
      // - either both MEM -> SE and SE -> PC
      // - or just MEM -> PC
      // lets call these "slots"

      // we try to find blocks to fit into the slots
      // these are "frozen", meaning we will not extract side effects out of them
      var frozenNodes = new ArrayList<>();

      // sort by side effect count (largest to smallest), such that blocks with many
      // side effects are more likely to be frozen
      blocks.sort((n0, n1) -> requireNonNull(sideEffectCount.get(n1)).total()
          - requireNonNull(sideEffectCount.get(n0)).total());

      boolean memSeTransitionUsed = false;
      boolean sePcTransitionUsed = false;

      // MEM -> SE slot
      var memSeCandidate = blocks.stream()
          .filter(n -> requireNonNull(sideEffectTypes.get(n)).isMemSe()).findFirst();
      if (memSeCandidate.isPresent()) {
        frozenNodes.add(memSeCandidate.get());
        memSeTransitionUsed = true;
      }

      // SE -> PC slot
      var sePcCandidate = blocks.stream()
          .filter(n -> requireNonNull(sideEffectTypes.get(n)).isSePc()).findFirst();
      if (sePcCandidate.isPresent()) {
        frozenNodes.add(sePcCandidate.get());
        sePcTransitionUsed = true;
      }
      // MEM -> PC slot
      if (!memSeTransitionUsed && !sePcTransitionUsed) {
        // freezing a block which covers all three types should also be possible,
        // but only if nothing else CONTAINS an SE side effect
        var blocksWithSeCount = blocks.stream()
            .filter(n -> requireNonNull(sideEffectTypes.get(n))
                .contains(SideEffectUtils.SideEffectType.SE))
            .count();
        var candidate = blocks.stream()
            .filter(n -> {
              var seTypes = requireNonNull(sideEffectTypes.get(n));
              if (!seTypes.isMemPc()) {
                // only blocks covering all three types are candidates
                return false;
              }
              if (seTypes.contains(SideEffectUtils.SideEffectType.SE)) {
                // this block contains a SE, make sure no other block does
                return blocksWithSeCount <= 1;
              }
              // this block contains no SE, make sure no other block does
              return blocksWithSeCount <= 0;
            }).findFirst();
        candidate.ifPresent(frozenNodes::add);
      }

      var end = new CfgTraverser(){}.traverseBranch(beginNode);

      // extract out of remaining blocks, such that they each only contain one se type
      blocks.stream().toList().forEach(node -> {
        if (frozenNodes.contains(node)) {
          // only if we do not extract and multiple se types remain do we need to reorder
          reorderBlock(node, depth);
        } else {
          extractOutOfBlock(node, blocks, end, depth);
        }
      });

      // now it is possible to reorder everything
      // sort blocks by their side effect types
      blocks.sort((n0, n1) -> requireNonNull(sideEffectTypes.get(n0))
          .compare(requireNonNull(sideEffectTypes.get(n1))));

      // chain together the constructs in the right order
      DirectionalNode prev = beginNode;
      for (var node : blocks) {
        var next = prev.next();
        if (next != node) {
          var oldPrev = requireNonNull(node.predecessor());
          oldPrev.unlinkNext();
          prev.unlinkNext();
          oldPrev.setNext(next);
          prev.setNext(node);
        }
        prev = node.mergeNode();
      }
    }

    /**
     * Extracts side effects until only one type of side effect remains.
     * The extracted side effects are placed in new blocks, which are added to
     * the given {@code nodeList}.
     *
     * <p>{@link #sideEffectCount} is not updated and left in an inconsistent state.
     *
     * <p>{@link #sideEffectTypes} is updated for the new blocks and the given block,
     * but not for any of the inner blocks.
     *
     * @param node The start node of the block to extract out of.
     * @param nodeList The list of nodes to add the new blocks to.
     * @param endNode The end node of the branch surrounding the node, where the new
     *                blocks should be prepended.
     * @param depth The number of blocks the given block is inside of.
     */
    private void extractOutOfBlock(ControlSplitNode node, List<ControlSplitNode> nodeList,
                                   AbstractEndNode endNode, int depth) {
      var keptSeType = requireNonNull(sideEffectCount.get(node)).mostCommonType();
      sideEffectTypes.put(node, SideEffectUtils.SideEffectTypes.of(Set.of(keptSeType)));
      new CfgTraverser() {
        @Override
        public ControlNode onEnd(AbstractEndNode localEndNode) {
          localEndNode.sideEffects().stream().toList().forEach(se -> {
            var seType = SideEffectUtils.SideEffectType.classify(se);
            if (seType == keptSeType) {
              return;
            }
            localEndNode.removeSideEffect(se);
            var conditions = requireNonNull(requireNonNull(sideEffectConditions.get(se))
                .get(localEndNode));
            var relevantConditions = conditions.subList(depth, conditions.size());

            var pred = requireNonNull(endNode.predecessor());
            pred.unlinkNext();
            var newIfNode = addInNestedBlocks(relevantConditions, endNode,
                requireNonNull(endNode.graph()), se);
            pred.setNext(newIfNode);

            sideEffectTypes.put(newIfNode, SideEffectUtils.SideEffectTypes.empty());
            nodeList.add(newIfNode);
          });
          return localEndNode;
        }
      }.traverseControlSplit(node);
    }

    /**
     * Puts the given side effect into nested if- and forall-blocks. For each given condition
     * (or index) in the list, an if- or forall-block is created.
     *
     * @param conditions The conditions/indices, for each of which a nested block is created.
     * @param next The node to prepend the new blocks to.
     * @param graph The graph to add to.
     * @param se The side effect to place in nested blocks.
     * @return The outermost created node.
     */
    private static ControlSplitNode addInNestedBlocks(List<ExpressionNode> conditions,
                                                      ControlNode next, Graph graph,
                                                      SideEffectNode se) {
      if (conditions.isEmpty()) {
        throw new IllegalStateException("Expected non-empty list of conditions");
      }
      BiFunction<Graph, BranchEndNode, ControlNode> createInner = conditions.size() > 1
          ? (g, end) -> addInNestedBlocks(conditions.subList(1, conditions.size()), end, graph, se)
          : (g, end) -> {
            end.addSideEffect(se);
            return end;
          };
      var cond = conditions.getFirst();
      var pair = switch (cond) {
        case ForIdxNode idxNode -> GraphUtils.insertForall(
            graph, idxNode, createInner, se.location()
        );
        default -> GraphUtils.insertIfElse(
            graph, cond, createInner, (g, end) -> end, se.location()
        );
      };
      pair.right().setNext(next);
      return pair.left();
    }

    /**
     * Reorders the side effects inside the given block.
     *
     * @param node The start node of the block to handle.
     * @param depth The number of blocks the given block is inside of.
     */
    private void reorderBlock(ControlSplitNode node, int depth) {
      switch (node) {
        case IfNode ifNode -> {
          reorderBranch(ifNode.trueBranch(), depth + 1);
          reorderBranch(ifNode.falseBranch(), depth + 1);
        }
        case ForallNode forallNode -> reorderBranch(forallNode.beginNode(), depth + 1);
        default -> //noinspection DataFlowIssue
            node.ensure(false,
                "Not an expected node in the SideEffectReorderer. "
                    + "You want to implement it.");
      }
    }
  }

}
