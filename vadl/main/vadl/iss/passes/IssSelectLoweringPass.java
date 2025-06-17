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

import static vadl.iss.passes.TcgPassUtils.isTcg;

import java.io.IOException;
import javax.annotation.Nullable;
import vadl.configuration.IssConfiguration;
import vadl.iss.passes.nodes.IssMoveExprNode;
import vadl.iss.passes.nodes.IssSelectNode;
import vadl.iss.passes.nodes.IssTempExprNode;
import vadl.iss.passes.nodes.TcgVRefNode;
import vadl.iss.passes.tcgLowering.TcgCondition;
import vadl.iss.passes.tcgLowering.TcgCtx;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Constant;
import vadl.viam.Specification;
import vadl.viam.graph.Graph;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.control.BeginNode;
import vadl.viam.graph.control.BranchEndNode;
import vadl.viam.graph.control.IfNode;
import vadl.viam.graph.control.MergeNode;
import vadl.viam.graph.control.ScheduledNode;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.DependencyNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.SelectNode;
import vadl.viam.passes.GraphProcessor;

public class IssSelectLoweringPass extends AbstractIssPass {

  public IssSelectLoweringPass(IssConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("TCG Select Lowering");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {

    viam.isa().ifPresent(isa ->
        isa.ownInstructions().forEach(instr -> new IssSelectLowerer(
            instr.behavior(),
            instr.expectExtension(TcgCtx.class).assignment()
        ).run()));
    return null;
  }
}

// TODO:  REVERT THE REVERT IN THE AARCH64.vadl SPEC !!!!!!!!!!!!!!!!!!
//   !!!!!!!!!!!!!!!
class IssSelectLowerer extends GraphProcessor<Void> {
  private Graph graph;
  private TcgCtx.Assignment assignments;

  public IssSelectLowerer(Graph graph, TcgCtx.Assignment assignments) {
    this.graph = graph;
    this.assignments = assignments;
  }

  void run() {
    // find all expression nodes that are a root expression (no other expression depends on it)
    graph.getNodes(ExpressionNode.class)
        .filter(f -> f.usages()
            .noneMatch(u -> u instanceof ExpressionNode)
        ).forEach(this::processNode);

    graph.getNodes(SelectNode.class)
        .filter(TcgPassUtils::isTcg)
        .forEach(this::turnIntoIssSelect);

    // TODO: can be probably integrated into turnIntoIssSelect
    cleanUpUnusedScheduledNodes(graph);
  }

  @Override
  protected Void processUnprocessedNode(Node toProcess) {
    if (!isTcg((DependencyNode) toProcess)) {
      return null;
    }

    if (!(toProcess instanceof SelectNode selectNode)) {
      // process inputs
      toProcess.inputs().forEach(this::processNode);
      return null;
    }

    var trueEnd = graph.add(new BranchEndNode(new NodeList<>()));
    var trueStart = graph.add(new BeginNode(trueEnd));

    var falseEnd = graph.add(new BranchEndNode(new NodeList<>()));
    var falseStart = graph.add(new BeginNode(falseEnd));

    var ifNode = graph.add(new IfNode(selectNode.condition(), trueStart, falseStart));
    var mergeNode = graph.add(new MergeNode(new NodeList<>(trueEnd, falseEnd)));

    var trueValue = selectNode.trueCase();
    var falseValue = selectNode.falseCase();

    var selectTempExpr =
        selectNode.replace(new IssTempExprNode(selectNode.id.numericId(), selectNode.type()));

    trueEnd.addBefore(graph.addWithInputs(
        new IssMoveExprNode(varOf(selectTempExpr), trueValue)
    ));

    falseEnd.addBefore(graph.addWithInputs(
        new IssMoveExprNode(varOf(selectTempExpr), falseValue)
    ));

    var schedule = scheduledNode(selectTempExpr);
    var succ = schedule.unlinkNext();
    // replace schedule node of select by if-else control flow
    schedule.setNext(ifNode);
    mergeNode.setNext(succ);

    // delete original select node
    selectNode.safeDelete();

    processNode(trueValue);
    processNode(falseValue);

    return null;
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

  private IssSelectNode turnIntoIssSelect(SelectNode selectNode) {
    if (selectNode.condition() instanceof BuiltInCall call) {
      var cond = TcgPassUtils.conditionOf(call.builtIn());
      if (cond != null) {
        // we can inline the condition into the IssSelectNode
        var node = new IssSelectNode(cond, call.arg(0), call.arg(1), selectNode.trueCase(),
            selectNode.falseCase());
        return selectNode.replaceAndDelete(node);
      }
    }

    // We couldn't inline the select node, so we must produce a comparison to true
    return selectNode.replaceAndDelete(
        new IssSelectNode(
            TcgCondition.EQ,
            selectNode.condition(),
            Constant.Value.of(true).toNode(),
            selectNode.trueCase(),
            selectNode.falseCase()
        )
    );
  }

  private boolean shouldBeLowered(IssSelectNode selectNode) {
    // TODO: find condition when to convert
    return true;
  }

  private ScheduledNode scheduledNode(ExpressionNode expr) {
    return expr.usages().filter(n -> n instanceof ScheduledNode)
        .map(ScheduledNode.class::cast).findFirst().get();
  }

  /**
   * Remove all nodes that might where scheduled but than not used.
   * This might happen if a {@link SelectNode} is turned into a {@link IssSelectNode}.
   */
  // TODO: Remove
  private static void cleanUpUnusedScheduledNodes(Graph graph) {
    graph.getNodes(ExpressionNode.class)
        .filter(e -> {
          var usages = e.usages().toList();
          return usages.size() == 1 && usages.getFirst() instanceof ScheduledNode;
        })
        .forEach(n -> {
          ((ScheduledNode) n.usages().findFirst().get()).replaceByNothingAndDelete();
          if (n.isActive()) {
            n.safeDelete();
          }
        });
  }
}
