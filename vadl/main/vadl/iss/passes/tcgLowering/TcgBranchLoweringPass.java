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

package vadl.iss.passes.tcgLowering;

import static java.util.Objects.requireNonNull;
import static vadl.utils.GraphUtils.getSingleNode;

import com.google.errorprone.annotations.concurrent.LazyInit;
import java.io.IOException;
import javax.annotation.Nullable;
import vadl.configuration.IssConfiguration;
import vadl.iss.passes.AbstractIssPass;
import vadl.iss.passes.TcgPassUtils;
import vadl.iss.passes.nodes.TcgVRefNode;
import vadl.iss.passes.tcgLowering.nodes.TcgBr;
import vadl.iss.passes.tcgLowering.nodes.TcgBrCond;
import vadl.iss.passes.tcgLowering.nodes.TcgGenLabel;
import vadl.iss.passes.tcgLowering.nodes.TcgSetLabel;
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
import vadl.viam.graph.control.ScheduledNode;
import vadl.viam.graph.control.StartNode;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.passes.CfgTraverser;

/**
 * The {@code TcgBranchLoweringPass} class implements a compiler pass that lowers high-level
 * branch constructs into low-level TCG (Tiny Code Generator) instructions
 * suitable for code generation.
 *
 * <p>From paper: At this stage, branches within the instruction are represented in the control
 * flow using if-else nodes.
 * However, TCG implements jumps within a TB using goto-like operations, such as
 * {@code set_label, br} and {@code brcond}.
 * This pass analyzes which if-else control flow must be converted into TCG operations—specifically,
 * those where the condition expression was previously scheduled as a TCG operation.
 * These control flow structures are then transformed into a linear sequence of TCG operations
 * using labels and conditional branching.</p>
 */
public class TcgBranchLoweringPass extends AbstractIssPass {

  /**
   * Constructs a new {@code TcgBranchLoweringPass} with the specified configuration.
   *
   * @param configuration the general configuration settings for the pass
   */
  public TcgBranchLoweringPass(IssConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("TCG Branch Lowering");
  }

  @Override
  public @Nullable Object execute(PassResults passResults, Specification viam)
      throws IOException {

    var optimizeCtrlFlow = !configuration().isSkip(IssConfiguration.IssOptsToSkip.OPT_CTRL_FLOW);

    viam.isa().ifPresent(isa -> isa.ownInstructions()
        .forEach(instr ->
            new TcgBranchLoweringExecutor(
                instr.behavior(),
                instr.expectExtension(TcgCtx.class).assignment()
            ).run(optimizeCtrlFlow)
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
  // only false if `--skip opt-ctrl-flow` set via cli.
  // if true, empty else branches are optimized
  boolean optimizeCtrlFlow = true;

  TcgBranchLoweringExecutor(Graph graph, TcgCtx.Assignment assignments) {
    this.graph = graph;
    this.assignments = assignments;
  }

  /**
   * Initiates traversal of the CFG starting from the graph's start node.
   */
  public void run(boolean optimizeCtrlFlow) {
    this.optimizeCtrlFlow = optimizeCtrlFlow;
    startNode = getSingleNode(graph, StartNode.class);
    traverseBranch(startNode);
    // clean up all conditions that were inlined into the TCG branch operation itself
    cleanUpUnusedConditions();
  }

  @Override
  public ControlNode traverseControlSplit(ControlSplitNode splitNode) {
    splitNode.ensure(splitNode instanceof IfNode, "Unsupported control split node");

    var ifNode = (IfNode) splitNode;

    if (!isCondTcg(ifNode.condition())) {
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
    var skipElse = optimizeCtrlFlow && isEmptyBranch(ifNode.falseBranch());

    var elseLabel = genLabelObj("else");
    var endLabel = genLabelObj("end");

    // insert label generation
    ifNode.addBefore(new TcgGenLabel(elseLabel));
    if (!skipElse) {
      // we don't skip the else branch, we generate the end label
      ifNode.addBefore(new TcgGenLabel(endLabel));
    }

    var tcgBranchNode = ifNode.addBefore(
        buildBrCondToElse(ifNode, elseLabel)
    );

    // emit the true branch
    var ifBranchEnd = traverseBranch(ifNode.trueBranch());
    // emit the else branch label
    var elseLabelPosition = ifBranchEnd.addBefore(new TcgSetLabel(elseLabel));

    var mergeNode = (MergeNode) ifBranchEnd.usages().findFirst().get();
    if (!skipElse) {
      // right before the else branch label, we must take a jump to the end label
      elseLabelPosition.addBefore(new TcgBr(endLabel));

      // traverse and emit the false branch
      traverseBranch(ifNode.falseBranch());
      // emit the end label after merge node
      mergeNode.addAfter(new TcgSetLabel(endLabel));
    }

    return linkBranchesAndRemoveControlSplit(
        ifNode,
        skipElse,
        tcgBranchNode,
        mergeNode
    );
  }

  /**
   * Builds the brcond op for if node to jump to else label.
   * If the condition can directly be computed in the brcond (in most cases true),
   * it will not use the TCGv of the condition expression.
   * Otherwise, it will compare the condition expression variable to constant 0.
   */
  private TcgBrCond buildBrCondToElse(IfNode ifNode, TcgLabel elseLabel) {
    var condFromBuiltIn = ifNode.condition() instanceof BuiltInCall builtInCall
        ? TcgPassUtils.conditionOf(builtInCall.builtIn())
        : null;

    if (condFromBuiltIn != null) {
      // we directly compute the branch condition in brcond, instead of
      // creating a variable for it.

      // first we negate the condition as we jump to the else branch
      var condNegated = condFromBuiltIn.not();

      // get variables of lhs and rhs and return brcond
      var condCall = (BuiltInCall) ifNode.condition();
      var lhsVar = varOf(condCall.arguments().get(0));
      var rhsVar = varOf(condCall.arguments().get(1));
      return new TcgBrCond(lhsVar, rhsVar, condNegated, elseLabel);
    } else {
      // we cannot make the condition directly in the brcond op.
      // so we compare the result with eq 0 -> branch to else if cond false.
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
      return new TcgBrCond(condVar, constZero, condition, elseLabel);
    }
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
    var lastTrueBranchNode = requireNonNull(mergeNode.trueBranchEnd().predecessor());
    var lastFalseBranchNode = requireNonNull(mergeNode.falseBranchEnd().predecessor());

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
   * Check if the condition is a TCG.
   * If might not be scheduled due to optimization, eventhough it depends
   * on TCG arguments.
   */
  public boolean isCondTcg(ExpressionNode node) {
    if (node instanceof BuiltInCall builtInCall) {
      // it is TCG if an argument is TCG.
      return builtInCall.arguments().stream().anyMatch(this::isTcg);
    }
    return isTcg(node);
  }

  /**
   * Retrieves the TCG variable associated with the given expression node.
   *
   * @param node the expression node
   * @return the associated TCG variable
   */
  private TcgVRefNode varOf(ExpressionNode node) {
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

  /**
   * Conditions that could directly be embedded in the branch instructions
   * might not be used anymore.
   * E.g. a EQU condition can be unscheduled and removed if it is only used by
   * TCG branches.
   */
  private void cleanUpUnusedConditions() {
    // search for all scheduled nodes that have a built-in not used by any other node
    // except for the scheduled node and the built-in can be represented as TCG condition.
    var schedulesToRemove = graph.getNodes(ScheduledNode.class)
        .filter(n -> n.node() instanceof BuiltInCall call
            && call.usageCount() == 1
            && TcgPassUtils.conditionOf(call.builtIn()) != null
        )
        .toList();

    for (var schedule : schedulesToRemove) {
      schedule.replaceByNothingAndDelete();
    }
  }

}
