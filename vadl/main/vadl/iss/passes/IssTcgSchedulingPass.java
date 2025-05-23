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

package vadl.iss.passes;

import static java.util.Objects.requireNonNull;
import static vadl.utils.GraphUtils.getSingleNode;

import com.google.errorprone.annotations.concurrent.LazyInit;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import vadl.configuration.IssConfiguration;
import vadl.iss.passes.nodes.IssGhostCastNode;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Counter;
import vadl.viam.Specification;
import vadl.viam.graph.Graph;
import vadl.viam.graph.Node;
import vadl.viam.graph.ViamGraphError;
import vadl.viam.graph.control.AbstractBeginNode;
import vadl.viam.graph.control.AbstractEndNode;
import vadl.viam.graph.control.ControlNode;
import vadl.viam.graph.control.ControlSplitNode;
import vadl.viam.graph.control.DirectionalNode;
import vadl.viam.graph.control.IfNode;
import vadl.viam.graph.control.ScheduledNode;
import vadl.viam.graph.control.StartNode;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.DependencyNode;
import vadl.viam.graph.dependency.ReadRegTensorNode;
import vadl.viam.graph.dependency.ReadResourceNode;
import vadl.viam.graph.dependency.SelectNode;
import vadl.viam.graph.dependency.WriteRegTensorNode;
import vadl.viam.graph.dependency.WriteResourceNode;
import vadl.viam.passes.CfgTraverser;
import vadl.viam.passes.GraphProcessor;

/**
 * A pass that schedules the translation of instruction behaviors to TCG (Tiny Code Generator)
 * operations for an Instruction Set Simulator (ISS).
 * It ensures that instructions are correctly scheduled
 * for translation, considering control flow and dependencies.
 *
 * <p>From paper: Before lowering to TCG operations,
 * it must be determined which expressions are evaluated during TCG translation and which
 * are executed at runtime when the translated TCG code is executed.
 * Expressions evaluated at runtime must be converted into TCG operations and scheduled accordingly.
 * This includes all expressions that depend on the CPU state or memory, such as register reads.
 * Conversely, expressions that depend only on immediate values, such as format field values,
 * can be computed at translation time and represented as constant TCG variables.
 * These expressions do not require scheduling, as their dependencies can be directly translated
 * into C expressions.
 * After this pass, all dependency nodes corresponding to TCG operations are correctly scheduled.
 * </p>
 */
public class IssTcgSchedulingPass extends AbstractIssPass {

  /**
   * Constructs a new ISS TCG Scheduling Pass with the given configuration.
   *
   * @param configuration The ISS configuration.
   */
  public IssTcgSchedulingPass(IssConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("ISS TCG Scheduling Pass");
  }

  @Override
  public @Nullable Object execute(PassResults passResults, Specification viam)
      throws IOException {

    viam.isa().ifPresent(isa -> {
      var pc = requireNonNull(isa.pc());
      pc.ensure(pc.registerTensor().isSingleRegister(), "Only one-dimensional PC supported yet");

      isa.ownInstructions()
          .forEach(instr ->
              IssTcgScheduler.runOn(instr.behavior(), pc));
    });

    return null;
  }
}

/**
 * Schedules nodes in the control flow graph (CFG) for TCG translation.
 * <p>
 * This scheduler traverses the CFG and schedules nodes by inserting {@link ScheduledNode}s
 * where necessary, ensuring that all dependencies are correctly
 * handled and that the generated TCG code will execute in the correct order.
 * </p>
 */
class IssTcgScheduler extends GraphProcessor<Optional<ScheduledNode>> implements CfgTraverser {

  private Counter pc;

  @LazyInit
  private ControlNode currentRootUser;

  /**
   * A stack of nested sub-branches that are pushed when opening a new branch
   * and popped when closing it (reaching the end of the branch).
   * <p>
   * The top of the stack contains all nodes that are processed (and available)
   * in the current sub-branch. If we open another sub-branch, a new empty set is
   * pushed to the stack.
   * Later, when this sub-branch ends, we have to remove all the nodes from the set at
   * the top of the graph processor, as the results are no longer valid.
   * </p>
   * <p>
   * See the test case <code>valid_branch_1.vadl</code> for more details.
   * </p>
   */
  private final ArrayDeque<Set<Node>> stackOfNestedBranches = new ArrayDeque<>();

  /**
   * Constructs an {@code IssTcgScheduler} with the given program counter (PC) register counter.
   *
   * @param pc The program counter register counter.
   */
  public IssTcgScheduler(Counter pc) {
    this.pc = pc;
  }

  /**
   * Runs the scheduler on the given graph with the specified program counter.
   *
   * @param graph The control flow graph to process.
   * @param pc    The program counter register counter.
   */
  static void runOn(Graph graph, Counter pc) {
    var start = getSingleNode(graph, StartNode.class);
    new IssTcgScheduler(pc).traverseBranch(start);

    // unschedule unnecessary conditions again
    unscheduleConditions(graph);
  }

  /**
   * Processes a directional node in the CFG.
   *
   * @param dir The directional node to process.
   */
  @Override
  public ControlNode onDirectional(DirectionalNode dir) {
    if (dir instanceof ScheduledNode schNode) {
      // The node is scheduled, so we must also schedule its inputs
      // before the scheduling of itself.
      currentRootUser = schNode;
      // Process all inputs of the already scheduled node
      schNode.node().visitInputs(this);
    } else if (dir instanceof AbstractBeginNode) {
      // We start a new branch, so we have to push a new empty set on our stack
      stackOfNestedBranches.push(new HashSet<>());
    }

    return dir;
  }

  /**
   * Processes a control split node in the CFG.
   *
   * @param controlSplit The control split node to process.
   * @throws ViamGraphError If the control split type is not supported.
   */
  @Override
  public ControlNode onControlSplit(ControlSplitNode controlSplit) {
    if (controlSplit instanceof IfNode ifNode) {
      // If there is a control split, we have to schedule its condition
      currentRootUser = controlSplit;
      // Schedule the ifNode's condition
      processNode(ifNode.condition());
    } else {
      throw new ViamGraphError("Currently the control split type %s is not supported",
          controlSplit.getClass().getSimpleName())
          .addContext(controlSplit);
    }
    return controlSplit;
  }

  /**
   * Processes the end of a branch in the CFG.
   *
   * @param endNode The end node of the branch.
   */
  @Override
  public ControlNode onEnd(AbstractEndNode endNode) {
    // We reached the end of the current branch.
    // We delete all results of this branch as they are no longer valid
    // and pop it, so the current branch is now the previous parent branch
    for (var node : stackOfNestedBranches.pop()) {
      super.processedNodes.remove(node);
    }
    return endNode;
  }

  /**
   * Processes a node that has not been processed yet.
   *
   * @param toProcess The node to process.
   * @return An {@link Optional} containing a {@link ScheduledNode} if the node was scheduled,
   *     or empty if not.
   * @throws ViamGraphError If the node is not a {@link DependencyNode}.
   */
  @Override
  protected Optional<ScheduledNode> processUnprocessedNode(Node toProcess) {
    // Look at inputs first
    toProcess.visitInputs(this);

    // Mark as processed in current branch.
    // Later, if we have to pop the branch, this must be removed from the
    // process results, as the result is invalid now.
    // Look at the valid_branch_1.vadl test
    requireNonNull(stackOfNestedBranches.peek()).add(toProcess);

    // Validate that register file accesses are not scheduled.
    // This is because those addresses are not TCG opcodes later on (but C++ evaluated immediates)
    validateRegTensorAccessIndices(toProcess);

    if (toProcess instanceof ReadResourceNode readResourceNode) {
      if (readResourceNode.resourceDefinition() == pc.registerTensor()) {
        // PC registers are not lowered to TCG as they can be accessed directly using
        // ctx->base.pc_next
        return Optional.empty();
      }
      return Optional.of(scheduleNode(readResourceNode));
    } else if (toProcess instanceof DependencyNode node) {
      if (TcgPassUtils.isTcg(node)) {
        // the node was already scheduled
        return node.usages().filter(u -> u instanceof ScheduledNode)
            .map(ScheduledNode.class::cast)
            .findFirst();
      }

      // In general, a node is scheduled if one of its inputs is scheduled
      var mustBeScheduled = toProcess.inputs()
          .anyMatch(n -> getResultOf(n).isPresent());

      if (mustBeScheduled && node instanceof IssGhostCastNode ghostCast) {
        // ghost cast nodes are removed if they are not used as C expressions
        var inputScheduleNode = getResultOf(ghostCast.value()).orElseThrow();
        ghostCast.usages().toList().forEach(u -> u.replaceInput(ghostCast, ghostCast.value()));
        // remove the schedule node of the ghostCast's value node.
        return Optional.of(inputScheduleNode);
      }

      return mustBeScheduled
          ? Optional.of(scheduleNode(node))
          : Optional.empty();
    } else {
      throw new ViamGraphError("Expected Dependency nodes only, but got this node")
          .addContext(toProcess);
    }
  }

  /**
   * Schedules a dependency node by inserting a {@link ScheduledNode} before the current root user.
   *
   * @param node The dependency node to schedule.
   * @return The {@link ScheduledNode} that was inserted.
   */
  private ScheduledNode scheduleNode(DependencyNode node) {
    var scheduledNode = new ScheduledNode(node);
    return currentRootUser.addBefore(scheduledNode);
  }

  /**
   * Validates that the indices of a register tensor accesses are not scheduled.
   * <p>
   * The indices must be determined at translation time, not at TCG code run time.
   * </p>
   *
   * @param toProcess The node representing the register tensor access.
   */
  private void validateRegTensorAccessIndices(Node toProcess) {
    if (toProcess instanceof ReadRegTensorNode regRead) {
      // Check that the indices are not a TCG resource.
      // The indices must be determined at "translation" time
      for (var i : regRead.indices()) {
        var addressRes = getResultOf(i, Optional.class);
        toProcess.ensure(addressRes.isEmpty(),
            "Node's index is not allowed to be TCG time but must "
                + "be compile-time annotated (immediates): %s",
            regRead.indices());
      }
    }

    if (toProcess instanceof WriteRegTensorNode regWrite) {
      for (var i : regWrite.indices()) {
        var addressRes = getResultOf(i, Optional.class);
        regWrite.ensure(addressRes.isEmpty(),
            "Node's address is not allowed to be TCG time but must "
                + "be compile-time annotated (immediates): %s",
            regWrite.indices()
        );
      }
    }
  }

  /**
   * This will check for all conditions (of if/select nodes),
   * if it can be omitted to place them as TCG instruction.
   */
  private static void unscheduleConditions(Graph graph) {
    graph.getNodes(Set.of(IfNode.class, SelectNode.class))
        .map(n -> n instanceof IfNode ifNode
            ? ifNode.condition()
            : ((SelectNode) n).condition()
        )
        .filter(BuiltInCall.class::isInstance)
        .map(BuiltInCall.class::cast)
        .forEach(IssTcgScheduler::unscheduleCondition);
  }

  /**
   * This will unschedule the condition of the if/select node, if it can be
   * directly checked in the TCG brcond/movcond op.
   * This optimizes unnecessary setconds and moves in the resulting TCG ops.
   *
   * <p>it is possible if the condition is not used as input by others than the scheduled node
   * and write nodes that use it as condition only.</p>
   */
  private static void unscheduleCondition(BuiltInCall cond) {
    if (TcgPassUtils.conditionOf(cond.builtIn()) == null) {
      // can not be expressed by tcg condition
      return;
    }
    var condScheduleNode = cond.usages()
        .filter(t -> t instanceof ScheduledNode)
        .map(ScheduledNode.class::cast)
        .findAny().orElse(null);

    if (condScheduleNode == null) {
      // it isn't even scheduled, so we can't unscheduled it anyway
      return;
    }

    // we can handle the condition directly in the brcond tcg instruction
    // so we want to unschedule the condition if possible.
    // it is possible if the condition is not used as input by others than the scheduled node
    // and write nodes that use it as condition only.
    var mustBeTcg = cond.usages()
        // ignore scheduled node
        .filter(t -> !(t instanceof ScheduledNode))
        // ignore if nodes and select node usages
        .filter(t -> !(t instanceof IfNode || t instanceof SelectNode))
        // ignore writes where it is not used as address or value (e.i. as condition only)
        .anyMatch(t -> {
          if (!(t instanceof WriteResourceNode write)) {
            // it has some other user -> required to be TCGv
            return true;
          }
          if (write.value() == cond) {
            // it is used as value -> required to be TCGv
            return true;
          }

          return write.indices().stream()
              .anyMatch(ind -> ind == cond);
        });

    if (!mustBeTcg) {
      // we can unschedule the node
      condScheduleNode.replaceByNothingAndDelete();
    }
  }
}
