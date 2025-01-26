package vadl.iss.passes;

import static java.util.Objects.requireNonNull;
import static vadl.utils.GraphUtils.getSingleNode;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.iss.passes.nodes.TcgVRefNode;
import vadl.iss.passes.tcgLowering.TcgV;
import vadl.iss.passes.tcgLowering.Tcg_32_64;
import vadl.iss.passes.tcgLowering.nodes.TcgGetVar;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Specification;
import vadl.viam.graph.Graph;
import vadl.viam.graph.ViamGraphError;
import vadl.viam.graph.control.DirectionalNode;
import vadl.viam.graph.control.ScheduledNode;
import vadl.viam.graph.control.StartNode;
import vadl.viam.graph.dependency.DependencyNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.ReadRegFileNode;
import vadl.viam.graph.dependency.ReadRegNode;
import vadl.viam.graph.dependency.WriteMemNode;
import vadl.viam.graph.dependency.WriteResourceNode;
import vadl.viam.passes.CfgTraverser;

/**
 * A pass that schedules constants for the Tiny Code Generator (TCG)
 * in the Instruction Set Simulator (ISS).
 * This pass traverses each instruction's behavior graph
 * and schedules constants to be loaded at the start.
 * This makes it easier to handle TCG lowering, as each TCG op is required to be only
 * based on TCGv arguments and does not need an immediate equivalent (e.g. add and addi).
 * This does harm performance, as under the hood, a call to {@code tcg_gen_addi} will
 * wrap the immediate argument in a TCGv constant anyways.
 */
public class IssTcgConstantSchedulingPass extends Pass {

  /**
   * Constructs a new {@code IssTcgConstantSchedulingPass} with the specified configuration.
   *
   * @param configuration The general configuration for this pass.
   */
  public IssTcgConstantSchedulingPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  /**
   * Returns the name of the pass.
   *
   * @return The pass name "ISS TCG Constant Scheduling".
   */
  @Override
  public PassName getName() {
    return PassName.of("ISS TCG Constant Scheduling");
  }

  /**
   * Executes the pass on the given specification and pass results.
   * This pass schedules constants for TCG in the ISS
   * by traversing each instruction's behavior graph.
   *
   * @param passResults The results from previous passes.
   * @param viam        The VIAM specification.
   * @return {@code null}
   * @throws IOException If an I/O error occurs during execution.
   */
  @Override
  public @Nullable Object execute(PassResults passResults, Specification viam)
      throws IOException {

    var assignments = passResults.lastResultOf(IssVarSsaAssignment.class,
        IssVarSsaAssignment.Result.class);

    viam.isa().get().ownInstructions().forEach(i -> {
      new IssTcgConstantScheduler(
          i.behavior(),
          requireNonNull(assignments.varAssignments().get(i))
      ).run();
    });

    return null;
  }
}

/**
 * Schedules immediate constants at the start of the instruction behavior graph.
 * Implements the {@link CfgTraverser} interface to traverse the control flow graph.
 */
class IssTcgConstantScheduler implements CfgTraverser {

  /**
   * Map of dependency nodes to their assigned TCG variables.
   */
  Map<DependencyNode, List<TcgVRefNode>> assignments;
  /**
   * The start node of the instruction behavior graph.
   */
  StartNode startNode;

  /**
   * Constructs an {@code IssTcgConstantScheduler} with the given behavior graph and assignments.
   *
   * @param graph       The behavior graph of the instruction.
   * @param assignments The map of dependency nodes to TCG variable assignments.
   */
  public IssTcgConstantScheduler(Graph graph, Map<DependencyNode, List<TcgVRefNode>> assignments) {
    this.assignments = assignments;
    this.startNode = getSingleNode(graph, StartNode.class);
  }

  /**
   * Starts the traversal of the behavior graph to schedule constants.
   */
  public void run() {
    traverseBranch(startNode);
  }

  /**
   * Processes directional nodes during CFG traversal.
   * If the node is a {@link ScheduledNode}, it checks if it has a TCG variable assignment.
   * If so, it schedules immediate constants for its input expressions at the start node.
   *
   * @param dir The directional node being processed.
   */
  @Override
  public void onDirectional(DirectionalNode dir) {
    if (!(dir instanceof ScheduledNode scheduledNode)) {
      return;
    }

    var tcgVs = assignments.get(scheduledNode.node());
    if (tcgVs == null || tcgVs.isEmpty()) {
      return;
    }

    for (var tcgV : tcgVs) {
      if (scheduledNode.node() instanceof ExpressionNode expressionNode) {
        if (expressionNode instanceof ReadRegNode || expressionNode instanceof ReadRegFileNode) {
          // Read register nodes are not real TCG operations; constants cannot be TCG variables.
          return;
        }

        expressionNode.inputs()
            .forEach(i ->
                scheduleImmediateAtStart((ExpressionNode) i, tcgV.width())
            );
      } else if (scheduledNode.node() instanceof WriteResourceNode writeResourceNode) {
        // The value might be a constant.
        scheduleImmediateAtStart(writeResourceNode.value(), tcgV.width());

        if (writeResourceNode instanceof WriteMemNode writeMemNode) {
          // Only the address of a memory write can be a TCG variable.
          scheduleImmediateAtStart(writeMemNode.address(), tcgV.width());
        }
      } else {
        throw new ViamGraphError("Unexpected scheduled node at %s", dir)
            .addContext(scheduledNode.node());
      }
    }
  }

  /**
   * Schedules an immediate constant expression at the start node if not already scheduled.
   * Creates a constant TCG variable and adds a {@link TcgGetVar}
   * node after the start node.
   *
   * @param expressionNode The expression node representing the constant.
   * @param width          The width of the TCG variable.
   */
  private void scheduleImmediateAtStart(ExpressionNode expressionNode, Tcg_32_64 width) {
    if (assignments.containsKey(expressionNode)) {
      // Already scheduled.
      return;
    }
    var constName = TcgPassUtils.exprVarName(expressionNode);
    var tcgV = TcgV.constant("const_" + constName, width, expressionNode);
    var refNode = requireNonNull(expressionNode.graph()).addWithInputs(new TcgVRefNode(tcgV));
    assignments.put(expressionNode, List.of(refNode));
    startNode.addAfter(TcgGetVar.from(refNode));
  }

}