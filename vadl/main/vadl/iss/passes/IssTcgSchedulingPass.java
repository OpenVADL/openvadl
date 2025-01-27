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
import vadl.iss.passes.safeResourceRead.nodes.ExprSaveNode;
import vadl.pass.Pass;
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
import vadl.viam.graph.dependency.DependencyNode;
import vadl.viam.graph.dependency.ReadRegFileNode;
import vadl.viam.graph.dependency.ReadResourceNode;
import vadl.viam.graph.dependency.WriteRegFileNode;
import vadl.viam.passes.CfgTraverser;
import vadl.viam.passes.GraphProcessor;

/**
 * A pass that schedules the translation of instruction behaviors to TCG (Tiny Code Generator)
 * operations for an Instruction Set Simulator (ISS).
 * It ensures that instructions are correctly scheduled
 * for translation, considering control flow and dependencies.
 */
public class IssTcgSchedulingPass extends Pass {

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
      pc.ensure(pc instanceof Counter.RegisterCounter, "Expected RegisterCounter");

      isa.ownInstructions()
          .forEach(instr ->
              IssTcgScheduler.runOn(instr.behavior(), (Counter.RegisterCounter) pc));
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

  private Counter.RegisterCounter pc;

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
  public IssTcgScheduler(Counter.RegisterCounter pc) {
    this.pc = pc;
  }

  /**
   * Runs the scheduler on the given graph with the specified program counter.
   *
   * @param graph The control flow graph to process.
   * @param pc    The program counter register counter.
   */
  static void runOn(Graph graph, Counter.RegisterCounter pc) {
    var start = getSingleNode(graph, StartNode.class);
    new IssTcgScheduler(pc).traverseBranch(start);
  }

  /**
   * Processes a directional node in the CFG.
   *
   * @param dir The directional node to process.
   */
  @Override
  public void onDirectional(DirectionalNode dir) {
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
  }

  /**
   * Processes a control split node in the CFG.
   *
   * @param controlSplit The control split node to process.
   * @throws ViamGraphError If the control split type is not supported.
   */
  @Override
  public void onControlSplit(ControlSplitNode controlSplit) {
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
  }

  /**
   * Processes the end of a branch in the CFG.
   *
   * @param endNode The end node of the branch.
   */
  @Override
  public void onEnd(AbstractEndNode endNode) {
    // We reached the end of the current branch.
    // We delete all results of this branch as they are no longer valid
    // and pop it, so the current branch is now the previous parent branch
    for (var node : stackOfNestedBranches.pop()) {
      super.processedNodes.remove(node);
    }
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
    validateRegFileAccessAddress(toProcess);

    if (toProcess instanceof ReadResourceNode readResourceNode) {
      if (readResourceNode.resourceDefinition() == pc.registerRef()) {
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
   * Validates that the address of a register file access is not scheduled.
   * <p>
   * The address must be determined at compile time, not at TCG code generation time.
   * </p>
   *
   * @param toProcess The node representing the register file access.
   */
  private void validateRegFileAccessAddress(Node toProcess) {
    if (toProcess instanceof ReadRegFileNode readRegFileNode) {
      // Check that the address is not a TCG resource.
      // The address must be determined at "compile" time
      var addressRes = getResultOf(readRegFileNode.address(), Optional.class);
      toProcess.ensure(addressRes.isEmpty(),
          "Node's address is not allowed to be TCG time but must "
              + "be compile-time annotated (immediates): %s",
          readRegFileNode.address());
    }

    if (toProcess instanceof WriteRegFileNode writeResourceNode) {
      var addressRes = getResultOf(writeResourceNode.address(), Optional.class);
      writeResourceNode.ensure(addressRes.isEmpty(),
          "Node's address is not allowed to be TCG time but must "
              + "be compile-time annotated (immediates): %s",
          writeResourceNode.address()
      );
    }
  }
}
