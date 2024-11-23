package vadl.iss.passes.tcgLowering;

import static java.util.Objects.requireNonNull;
import static vadl.utils.GraphUtils.getSingleNode;

import java.io.IOException;
import java.util.Map;
import org.jetbrains.annotations.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.iss.passes.IssVariableAllocationPass;
import vadl.iss.passes.tcgLowering.nodes.TcgBr;
import vadl.iss.passes.tcgLowering.nodes.TcgBrCondImm;
import vadl.iss.passes.tcgLowering.nodes.TcgGenLabel;
import vadl.iss.passes.tcgLowering.nodes.TcgSetLabel;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.types.Type;
import vadl.viam.Constant;
import vadl.viam.Specification;
import vadl.viam.graph.Graph;
import vadl.viam.graph.control.ControlNode;
import vadl.viam.graph.control.ControlSplitNode;
import vadl.viam.graph.control.DirectionalNode;
import vadl.viam.graph.control.IfNode;
import vadl.viam.graph.control.MergeNode;
import vadl.viam.graph.control.StartNode;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.DependencyNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.passes.CfgTraverser;

/**
 * The {@code TcgBranchLoweringPass} class implements a compiler pass that lowers high-level
 * branch constructs into low-level TCG (Tiny Code Generator) instructions
 * suitable for code generation.
 */
public class TcgBranchLoweringPass extends Pass {

  /**
   * Constructs a new {@code TcgBranchLoweringPass} with the specified configuration.
   *
   * @param configuration the general configuration settings for the pass
   */
  public TcgBranchLoweringPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("TCG Branch Lowering");
  }

  @Override
  public @Nullable Object execute(PassResults passResults, Specification viam)
      throws IOException {

    var varAssignments = passResults.lastResultOf(IssVariableAllocationPass.class,
        IssVariableAllocationPass.Result.class);

    viam.isa().ifPresent(isa -> isa.ownInstructions()
        .forEach(instr ->
            new TcgBranchLoweringExecutor(
                requireNonNull(varAssignments.varAssignments().get(instr))
            )
                .runOn(instr.behavior())
        ));

    return null;
  }
}

/**
 * The {@code TcgBranchLoweringExecutor} class traverses the control flow graph (CFG)
 * of an instruction's behavior and lowers high-level control constructs (like if-statements)
 * into low-level TCG instructions.
 */
class TcgBranchLoweringExecutor implements CfgTraverser {

  Map<DependencyNode, TcgV> assignments;

  TcgBranchLoweringExecutor(Map<DependencyNode, TcgV> assignments) {
    this.assignments = assignments;
  }

  /**
   * Initiates traversal of the CFG starting from the graph's start node.
   *
   * @param graph the control flow graph to traverse
   */
  public void runOn(Graph graph) {
    var start = getSingleNode(graph, StartNode.class);
    traverseBranch(start);
  }

  @Override
  public ControlNode traverseControlSplit(ControlSplitNode splitNode) {
    splitNode.ensure(splitNode instanceof IfNode, "Unsupported control split node");

    var ifNode = (IfNode) splitNode;

    if (!isTcg(ifNode.condition())) {
      // if the condition is immediate, we emit C-If construct
      // and no TCG operations
      return CfgTraverser.super.traverseControlSplit(ifNode);
    }

    var takenLabel = genLabelObj("taken");
    var endLabel = genLabelObj("end");

    // insert label generation
    splitNode.addBefore(new TcgGenLabel(takenLabel));
    splitNode.addBefore(new TcgGenLabel(endLabel));

    // TODO: here we have potential to optimize the branch by using a branch condition
    // instead of the result of the condition.
    var condVar = varOf(ifNode.condition());

    // produce 0 value node to compare to
    var graph = requireNonNull(ifNode.graph());
    var zeroValue = graph.add(new ConstantNode(Constant.Value.of(
        0,
        Type.bits(condVar.width.width)
    )));

    // check if true by check if value is not 0.
    // if true, we branch to the ifLabel.
    var tcgBranchNode = splitNode.addBefore(
        new TcgBrCondImm(condVar, zeroValue, TcgCondition.NE, takenLabel)
    );

    // emit the false branch
    var falseBranchEnd = traverseBranch(ifNode.falseBranch());
    // at end of false branch we must take a jump
    falseBranchEnd.addBefore(new TcgBr(endLabel));

    // emit the taken branch label
    var takenLabelPosition = falseBranchEnd.addBefore(new TcgSetLabel(takenLabel));

    // traverse true branch
    var trueBranchEnd = traverseBranch(ifNode.trueBranch());
    // emit the end label
    var endLabelPosition = trueBranchEnd.addBefore(new TcgSetLabel(endLabel));

    return linkBranchesAndRemoveControlSplit(
        ifNode,
        tcgBranchNode,
        ifNode.trueBranch().next(),
        ifNode.falseBranch().next(),
        endLabelPosition,
        takenLabelPosition,
        (MergeNode) falseBranchEnd.usages().findFirst().get()
    );
  }

  /**
   * Unlinks the original control split and merge nodes,
   * and relinks the branches using TCG labels and jumps.
   *
   * @param ifNode               the original if-node representing the control split
   * @param tcgBranchNode        the TCG branch instruction node
   * @param firstTrueBranchNode  the first node in the true branch
   * @param firstFalseBranchNode the first node in the false branch
   * @param lastTrueBranchNode   the last node in the true branch
   * @param lastFalseBranchNode  the last node in the false branch
   * @param mergeNode            the merge node where branches join
   * @return the control node to continue traversal from after relinking
   */
  private ControlNode linkBranchesAndRemoveControlSplit(
      IfNode ifNode,
      DirectionalNode tcgBranchNode,
      ControlNode firstTrueBranchNode,
      ControlNode firstFalseBranchNode,
      DirectionalNode lastTrueBranchNode,
      DirectionalNode lastFalseBranchNode,
      MergeNode mergeNode
  ) {

    // Unlink branches
    ifNode.trueBranch().setNext(null);
    ifNode.falseBranch().setNext(null);

    // Link TCG branch instruction with first node in false branch
    tcgBranchNode.setNext(firstFalseBranchNode);
    // Link last node in false branch with first node in true branch
    lastFalseBranchNode.setNext(firstTrueBranchNode);

    // Delete split node and branches
    ifNode.safeDelete();

    // Link true branch end with node to continue after merge
    var nodeToContinue = mergeNode.next();
    mergeNode.setNext(null);
    lastTrueBranchNode.setNext(nodeToContinue);

    // Delete merge node and all branch ends
    mergeNode.safeDelete();

    return nodeToContinue;
  }

  /**
   * Checks if the given expression node corresponds to a TCG variable.
   *
   * @param node the expression node to check
   * @return {@code true} if the node is associated with a TCG variable; {@code false} otherwise
   */
  private boolean isTcg(ExpressionNode node) {
    return assignments.containsKey(node);
  }

  /**
   * Retrieves the TCG variable associated with the given expression node.
   *
   * @param node the expression node
   * @return the associated TCG variable
   * @throws IllegalStateException if the node is not associated with a TCG variable
   */
  private TcgV varOf(ExpressionNode node) {
    node.ensure(isTcg(node), "Expected to be a tcg node");
    return requireNonNull(assignments.get(node));
  }

  private int labelCnt = 0;

  /**
   * Generates a new unique TCG label with the given name prefix.
   *
   * @param namePrefix the prefix for the label name
   * @return a new {@code TcgLabel} object
   */
  private TcgLabel genLabelObj(String namePrefix) {
    var prefix = "l_" + namePrefix + "_";
    return new TcgLabel(prefix + labelCnt++);

  }

}
