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

package vadl.viam.passes.sideEffectScheduling;

import static vadl.utils.GraphUtils.getSingleNode;

import com.google.common.collect.Lists;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.utils.ViamUtils;
import vadl.viam.Counter;
import vadl.viam.DefProp;
import vadl.viam.Instruction;
import vadl.viam.Procedure;
import vadl.viam.Specification;
import vadl.viam.graph.Graph;
import vadl.viam.graph.control.AbstractBeginNode;
import vadl.viam.graph.control.AbstractEndNode;
import vadl.viam.graph.control.ControlNode;
import vadl.viam.graph.control.ControlSplitNode;
import vadl.viam.graph.control.DirectionalNode;
import vadl.viam.graph.control.MergeNode;
import vadl.viam.graph.control.ScheduledNode;
import vadl.viam.graph.control.StartNode;
import vadl.viam.graph.dependency.ProcCallNode;
import vadl.viam.graph.dependency.WriteResourceNode;
import vadl.viam.passes.sideEffectScheduling.nodes.InstrExitNode;

/**
 * A pass that schedules side effects within the control flow graph (CFG) of instructions.
 * It separates side effects that modify the program counter (PC) from other side effects.
 * Non-PC side effects are scheduled at the beginning of branches, while PC updates are scheduled
 * immediately before the branch ends.
 *
 * <p>This pass also handles scheduling of {@link Procedure}s.</p>
 *
 * <p>From paper: The VIAM behavior graph represents expressions and side effects using a
 * dependency graph.
 * However, since TCG ops execute sequentially, this dependency graph must be scheduled.
 * The first step in this process is scheduling side effects, such as register writes.
 * Additionally, the pass analyzes whether a side effect causes an instruction exit by modifying
 * the program counter.
 * Non-exit side effects are scheduled at the start of the control flow branch,
 * while program counter manipulations are placed immediately before the branch end.
 * This ensures that a jump out of the instruction does not occur
 * before all other side effects have been applied.</p>
 */
public class SideEffectSchedulingPass extends Pass {

  /**
   * Constructs a new {@code SideEffectSchedulingPass} with the given configuration.
   *
   * @param configuration The general configuration for this pass.
   */
  public SideEffectSchedulingPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  /**
   * Returns the name of the pass.
   *
   * @return The pass name "Side Effect Scheduling Pass".
   */
  @Override
  public PassName getName() {
    return PassName.of("Side Effect Scheduling Pass");
  }

  /**
   * Executes the side effect scheduling pass on the given specification.
   * It processes each instruction in the ISA and schedules side effects appropriately.
   *
   * @param passResults The results from previous passes.
   * @param viam        The VIAM specification.
   * @return {@code null}
   * @throws IOException If an I/O error occurs.
   */
  @Override
  public @Nullable Object execute(PassResults passResults, Specification viam)
      throws IOException {

    var defs = ViamUtils.findDefinitionsByFilter(viam, def ->
        def instanceof Instruction || def instanceof Procedure);

    var isa = viam.isa().get();
    var pc = isa.pc();
    isa.ensure(pc == null || pc.registerTensor().isSingleRegister(),
        "Only RegisterCounters are currently supported for this pass. Got: %s", pc);

    for (var def : defs) {
      ((DefProp.WithBehavior) def).behaviors().forEach(behavior -> {
        SideEffectScheduler.run(behavior, pc);
      });
    }
    return null;
  }
}

/**
 * Schedules side effects in an instruction's control flow graph.
 * It separates PC updates from other side effects and schedules them at appropriate locations.
 */
class SideEffectScheduler {

  /**
   * The program counter register counter, if available.
   */
  @Nullable
  Counter pc;

  /**
   * Runs the side effect scheduling on the given instruction.
   *
   * @param behavior The behavior to process.
   * @param pc       The program counter register counter, or {@code null} if not available.
   */
  public static void run(Graph behavior, @Nullable Counter pc) {
    var startNode = getSingleNode(behavior, StartNode.class);
    var scheduler = new SideEffectScheduler();
    scheduler.pc = pc;
    scheduler.processBranch(startNode);
  }

  /**
   * Processes a branch starting from the given begin node.
   * It schedules non-PC side effects at the beginning
   * of the branch and PC updates before the branch end.
   *
   * @param beginNode The starting node of the branch.
   * @return The corresponding end node of the branch.
   */
  private AbstractEndNode processBranch(AbstractBeginNode beginNode) {
    // Process until the corresponding end node of the branch
    var endNode = traverseUntilMatchingBranchEnd(beginNode);

    var pcReg = pc != null ? pc.registerTensor() : null;
    var partitionedEffects = endNode.sideEffects().stream()
        // find side effects that cause instruction exits
        .collect(Collectors.partitioningBy(
            s ->
                (s instanceof WriteResourceNode write && write.resourceDefinition().equals(pcReg))
                    || (s instanceof ProcCallNode procCall && procCall.exceptionRaise())
        ));

    var nonPcUpdateEffects = partitionedEffects.getOrDefault(false, List.of());
    var instrExitSideEffects = partitionedEffects.getOrDefault(true, List.of())
        .stream().findFirst();

    // All non-PC updates should be inserted directly at the beginning of the branch
    for (var effect : Lists.reverse(nonPcUpdateEffects)) {
      beginNode.addAfter(new ScheduledNode(effect));
    }

    // Add PC update directly in front of branch end
    instrExitSideEffects.ifPresent(exitCause -> {
          if (exitCause instanceof ProcCallNode procCall) {
            endNode.addBefore(new InstrExitNode.Raise(procCall));
          } else if (exitCause instanceof WriteResourceNode write) {
            endNode.addBefore(new InstrExitNode.PcChange(write));
          } else {
            throw new IllegalStateException("Unexpected exit cause: " + exitCause);
          }
        }

    );

    return endNode;
  }

  /**
   * Traverses the control flow graph until it finds the matching end node for the given begin node.
   *
   * @param beginNode The begin node to start traversal from.
   * @return The matching end node.
   */
  private AbstractEndNode traverseUntilMatchingBranchEnd(AbstractBeginNode beginNode) {

    ControlNode currNode = beginNode;

    while (true) {
      // Skip all directional nodes
      currNode = skipDirectionals(currNode);

      if (currNode instanceof AbstractEndNode) {
        // When we find the end node, we return it
        return (AbstractEndNode) currNode;

      } else if (currNode instanceof ControlSplitNode splitNode) {
        // Handle all branches of the nested control split node
        currNode = handleControlSplit(splitNode);

      } else {
        currNode.ensure(false,
            "Expected directional or control split node, but got this node in CFG."
        );
      }
    }
  }

  /**
   * Processes all branches of the control split node and returns the merge node.
   *
   * @param splitNode The control split node to process.
   * @return The merge node corresponding to the control split.
   */
  private MergeNode handleControlSplit(ControlSplitNode splitNode) {
    @Nullable AbstractEndNode someEnd = null;
    for (var branch : splitNode.branches()) {
      someEnd = processBranch(branch);
    }
    splitNode.ensure(someEnd != null, "Control split has no branches.");
    splitNode.ensure(someEnd.usageCount() == 1, "End should have exactly one usage: MergeNode");
    // Get the merge node from the end of the branch
    return (MergeNode) someEnd.usages().findFirst().get();
  }

  /**
   * Skips all directional nodes starting from the
   * given node and returns the next non-directional node.
   *
   * @param node The starting control node.
   * @return The next non-directional control node.
   */
  private ControlNode skipDirectionals(ControlNode node) {
    while (node instanceof DirectionalNode dirNode) {
      node = dirNode.next();
    }
    return node;
  }
}