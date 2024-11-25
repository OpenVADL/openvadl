package vadl.iss.passes;

import static java.util.Objects.requireNonNull;
import static vadl.utils.GraphUtils.getSingleNode;

import java.io.IOException;
import java.util.Map;
import org.jetbrains.annotations.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.iss.passes.tcgLowering.TcgV;
import vadl.iss.passes.tcgLowering.Tcg_32_64;
import vadl.iss.passes.tcgLowering.nodes.TcgConstantNode;
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
import vadl.viam.graph.dependency.SideEffectNode;
import vadl.viam.graph.dependency.WriteMemNode;
import vadl.viam.graph.dependency.WriteResourceNode;
import vadl.viam.passes.CfgTraverser;

public class IssTcgConstantSchedulingPass extends Pass {

  public IssTcgConstantSchedulingPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("ISS TCG Constant Scheduling");
  }

  @Override
  public @Nullable Object execute(PassResults passResults, Specification viam)
      throws IOException {

    var assignments = passResults.lastResultOf(IssVariableAllocationPass.class,
        IssVariableAllocationPass.Result.class);

    viam.isa().get().ownInstructions().forEach(i -> {
      new IssTcgConstantScheduler(
          i.behavior(),
          requireNonNull(assignments.varAssignments().get(i))
      ).run();
    });

    return null;
  }
}


class IssTcgConstantScheduler implements CfgTraverser {

  Map<DependencyNode, TcgV> assignments;
  StartNode startNode;

  public IssTcgConstantScheduler(Graph graph, Map<DependencyNode, TcgV> assignments) {
    this.assignments = assignments;
    this.startNode = getSingleNode(graph, StartNode.class);
  }

  public void run() {
    traverseBranch(startNode);
  }

  @Override
  public void onDirectional(DirectionalNode dir) {
    if (!(dir instanceof ScheduledNode scheduledNode)) {
      return;
    }

    TcgV tcgV = assignments.get(scheduledNode.node());
    if (tcgV == null) {
      return;
    }

    if (scheduledNode.node() instanceof ExpressionNode expressionNode) {
      if (expressionNode instanceof ReadRegNode || expressionNode instanceof ReadRegFileNode) {
        // read reg are no real tcg operations and therefore constants cannot be TCGv
        return;
      }

      expressionNode.inputs()
          .forEach(i ->
              scheduleImmediateAtStart((ExpressionNode) i, tcgV.width())
          );
    } else if (scheduledNode.node() instanceof WriteResourceNode writeResourceNode) {
      // value might be a constant
      scheduleImmediateAtStart(writeResourceNode.value(), tcgV.width());

      if (writeResourceNode instanceof WriteMemNode writeMemNode) {
        // only the address of a memory write can be a tcgV value
        scheduleImmediateAtStart(writeMemNode.address(), tcgV.width());
      }
    } else {
      throw new ViamGraphError("Unexpected scheduled node at %s", dir)
          .addContext(scheduledNode.node());
    }

  }

  private void scheduleImmediateAtStart(ExpressionNode expressionNode, Tcg_32_64 width) {
    if (assignments.containsKey(expressionNode)) {
      // already scheduled
      return;
    }
    var tcgV = TcgV.constant("const_n" + expressionNode.id, width, expressionNode);
    assignments.put(expressionNode, tcgV);
    startNode.addAfter(TcgGetVar.from(tcgV));
  }


}