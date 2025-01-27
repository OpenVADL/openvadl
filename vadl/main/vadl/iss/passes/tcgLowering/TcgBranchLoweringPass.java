package vadl.iss.passes.tcgLowering;

import static java.util.Objects.requireNonNull;
import static vadl.utils.GraphUtils.getSingleNode;

import com.google.errorprone.annotations.concurrent.LazyInit;
import java.io.IOException;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.iss.passes.TcgPassUtils;
import vadl.iss.passes.nodes.TcgVRefNode;
import vadl.iss.passes.tcgLowering.nodes.TcgBr;
import vadl.iss.passes.tcgLowering.nodes.TcgBrCond;
import vadl.iss.passes.tcgLowering.nodes.TcgGenLabel;
import vadl.iss.passes.tcgLowering.nodes.TcgSetLabel;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.types.Type;
import vadl.viam.Constant;
import vadl.viam.Specification;
import vadl.viam.graph.Graph;
import vadl.viam.graph.control.AbstractBeginNode;
import vadl.viam.graph.control.AbstractEndNode;
import vadl.viam.graph.control.ControlNode;
import vadl.viam.graph.control.ControlSplitNode;
import vadl.viam.graph.control.DirectionalNode;
import vadl.viam.graph.control.IfNode;
import vadl.viam.graph.control.MergeNode;
import vadl.viam.graph.control.StartNode;
import vadl.viam.graph.dependency.ConstantNode;
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

    var varAssignments = passResults.lastResultOf(IssTcgContextPass.class,
        IssTcgContextPass.Result.class);

    viam.isa().ifPresent(isa -> isa.ownInstructions()
        .forEach(instr ->
            new TcgBranchLoweringExecutor(
                instr.behavior(),
                requireNonNull(varAssignments.tcgCtxs().get(instr)).assignment()
            ).run()
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

  TcgCtx.Assignment assignments;
  @LazyInit
  StartNode startNode;

  Graph graph;

  TcgBranchLoweringExecutor(Graph graph, TcgCtx.Assignment assignments) {
    this.graph = graph;
    this.assignments = assignments;
  }

  /**
   * Initiates traversal of the CFG starting from the graph's start node.
   */
  public void run() {
    startNode = getSingleNode(graph, StartNode.class);
    traverseBranch(startNode);
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

    return buildControlSequence(ifNode);
  }


  /**
   * Converts the if-else construct into a branch/label sequence.
   * It does this by negating the condition.
   * If the original condition was true, it will jump to the else label,
   * otherwise it will keep executing until the end of the if-branch.
   */
  private ControlNode buildControlSequence(IfNode ifNode) {
    // if the else branch does not include any source code, we can skip it
    var skipElse = isEmptyBranch(ifNode.falseBranch());

    var elseLabel = genLabelObj("else");
    var endLabel = genLabelObj("end");

    // insert label generation
    ifNode.addBefore(new TcgGenLabel(elseLabel));
    if (!skipElse) {
      // we don't skip the else branch, we generate the end label
      ifNode.addBefore(new TcgGenLabel(endLabel));
    }

    // TODO: here we have potential to optimize the branch by using a branch condition
    // instead of the result of the condition.
    var condVar = varOf(ifNode.condition());

    // produce 0 value node to compare to
    var constZero = getConstantVariable(
        new ConstantNode(Constant.Value.of(
            0,
            Type.bits(condVar.width().width)
        )));

    // check if !condition by check if condition value is 0.
    // if !condition, we branch to the elseLabel.
    var condition = TcgCondition.EQ;
    var tcgBranchNode = ifNode.addBefore(
        new TcgBrCond(condVar, constZero, condition, elseLabel)
    );

    // emit the true branch
    var ifBranchEnd = traverseBranch(ifNode.trueBranch());
    // emit the else branch label
    var elseLabelPosition = ifBranchEnd.addBefore(new TcgSetLabel(elseLabel));

    if (!skipElse) {
      // right before the else branch label, we must take a jump to the end label
      elseLabelPosition.addBefore(new TcgBr(endLabel));

      // traverse and emit the false branch
      var elseBranchEnd = traverseBranch(ifNode.falseBranch());
      // emit the end label
      elseBranchEnd.addBefore(new TcgSetLabel(endLabel));
    }

    return linkBranchesAndRemoveControlSplit(
        ifNode,
        skipElse,
        tcgBranchNode,
        (MergeNode) ifBranchEnd.usages().findFirst().get()
    );
  }

  /**
   * Returns the constant variable for a constant expression.
   */
  private TcgVRefNode getConstantVariable(ExpressionNode constant) {
    constant.ensure(!TcgPassUtils.isTcg(constant), "Node is not an immediate/constant but a TCG.");
    return assignments.singleDestOf(constant);
  }

  /**
   * Unlinks the original control split and merge nodes,
   * and relinks the branches using TCG labels and jumps.
   *
   * @param ifNode        the original if-node representing the control split
   * @param tcgBranchNode the TCG branch instruction node
   * @param mergeNode     the merge node where branches join
   * @return the control node to continue traversal from after relinking
   */
  @SuppressWarnings("VariableDeclarationUsageDistance")
  private ControlNode linkBranchesAndRemoveControlSplit(
      IfNode ifNode,
      boolean skipElse,
      DirectionalNode tcgBranchNode,
      MergeNode mergeNode
  ) {

    var firstTrueBranchNode = ifNode.trueBranch().next();
    var firstFalseBranchNode = ifNode.falseBranch().next();
    var lastTrueBranchNode = mergeNode.trueBranchEnd().predecessor();
    var lastFalseBranchNode = mergeNode.falseBranchEnd().predecessor();

    // Unlink branches
    ifNode.trueBranch().setNext(null);
    ifNode.falseBranch().setNext(null);

    // node after if-else control split
    var nodeToContinue = mergeNode.next();
    // unlink merge node
    mergeNode.setNext(null);

    // Link TCG branch instruction with the first node in true branch
    tcgBranchNode.setNext(firstTrueBranchNode);

    if (skipElse) {
      // skipping else -> Link end of true branch with node to continue after merge
      lastTrueBranchNode.setNext(nodeToContinue);
    } else {
      // Link last node in true branch with first node in false branch
      lastTrueBranchNode.setNext(firstFalseBranchNode);
      // Link false branch end with node to continue after merge
      lastFalseBranchNode.setNext(nodeToContinue);
    }

    // Delete split node and branches
    ifNode.safeDelete();
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
    return TcgPassUtils.isTcg(node);
  }

  /**
   * Retrieves the TCG variable associated with the given expression node.
   *
   * @param node the expression node
   * @return the associated TCG variable
   * @throws IllegalStateException if the node is not associated with a TCG variable
   */
  private TcgVRefNode varOf(ExpressionNode node) {
    node.ensure(isTcg(node), "Expected to be a tcg node");
    return assignments.singleDestOf(node);
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

  private static boolean isEmptyBranch(AbstractBeginNode branch) {
    return branch.next() instanceof AbstractEndNode;
  }

}
