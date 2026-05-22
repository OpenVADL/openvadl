// SPDX-FileCopyrightText : © 2025-2026 TU Wien <vadl@tuwien.ac.at>
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

package vadl.iss.passes.scalar;

import java.io.IOException;
import javax.annotation.Nullable;
import vadl.configuration.IssConfiguration;
import vadl.iss.passes.AbstractIssPass;
import vadl.iss.passes.TcgPassUtils;
import vadl.iss.passes.nodes.IssMoveNode;
import vadl.iss.passes.nodes.IssTempExprNode;
import vadl.iss.passes.tcgLowering.TcgCtx;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.utils.GraphUtils;
import vadl.viam.Specification;
import vadl.viam.graph.Graph;
import vadl.viam.graph.Node;
import vadl.viam.graph.control.ScheduledNode;
import vadl.viam.graph.dependency.SelectNode;
import vadl.viam.passes.GraphProcessor;

/**
 * Represents a pass for lowering {@link SelectNode} expressions
 * in the ISS (Instruction Set Simulator) into a control flow graph.
 *
 * <p>This is necessary for selects where a branch contains an operation that must not be
 * executed if the select condition is false.
 * E.g. the division behavior in VADL is undefined for division by zero.
 * If the user does not check if the divisor is zero, the corresponding div operation
 * in QEMU will cause a crash.
 * If the user, however, checks for zero using a select expression, it must not
 * evaluate the division operation, as it would cause a crash in the QEMU (on x86).
 * Instead, we transform the select expression into a control flow, which ensures that
 * only the taken branch is evaluated.
 */
public class IssSelectLoweringPass extends AbstractIssPass {
  public IssSelectLoweringPass(IssConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("ISS Select Lowering Pass");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    normalTcgInstrs(viam).forEach(instr -> new IssSelectLowerer(
        instr.behavior(),
        instr.expectExtension(TcgCtx.class).assignment()
    ).run());
    return null;
  }
}

class IssSelectLowerer extends GraphProcessor<Void> {

  private Graph graph;
  private TcgCtx.Assignment assignments;

  public IssSelectLowerer(Graph graph, TcgCtx.Assignment assignments) {
    this.graph = graph;
    this.assignments = assignments;
  }

  void run() {
    var selectOrder = GraphUtils.topologyOrderOfDependencyNodes(graph)
        // we must reverse the order as we want to process user select nodes first
        .reversed().stream()
        .filter(n -> n instanceof SelectNode);


    selectOrder.forEach(this::processNode);
  }


  @Override
  protected Void processUnprocessedNode(Node toProcess) {
    if (!(toProcess instanceof SelectNode select)) {
      toProcess.visitInputs(this);
      return null;
    }

    if (!TcgPassUtils.mustBeScheduled(select)) {
      // a select node not turned into a TCG node,
      // can stay a select node as the corresponding C code is a ternary expression.
      return null;
    }

    // find a the safe insertion point for the expression to be scheduled
    var insertionPoint = TcgPassUtils.findLatestSafeInsertionPoint(select);

    var resultExprNode =
        graph.addWithInputs(new IssTempExprNode(select.id.numericId(), select.type()));


    // we must schedule the result directly AFTER the if/else, so that users
    // cannot be scheduled before the if/else which would cause a read before write.
    var scheduledResultNode = graph.addWithInputs(
        new ScheduledNode(resultExprNode)
    );

    // we unlink the insertionPoint from its next node (as we insert the if/else cfg at this point)
    var originalNext = insertionPoint.unlinkNext();
    // set the scheduled node's next node to the original next node of the insertionPoint.
    // this must be done before getting the singleDestOf, so it is recognized as scheduled node.
    scheduledResultNode.setNext(originalNext);

    // determine the result TCG variable in the assignments.
    var selectResultTcgVar = assignments.singleDestOf(resultExprNode);

    var ifElse = GraphUtils.insertIfElse(insertionPoint, select.condition(),
        (graph, end) -> graph.addWithInputs(
            new ScheduledNode(
                // emit IssMoveNode that moves the true expression into the result variable
                new IssMoveNode(selectResultTcgVar, select.trueCase()),
                end
            )
        ),
        (graph, end) -> graph.addWithInputs(
            new ScheduledNode(
                // emit IssMoveNode that moves the false expression into the result variable
                new IssMoveNode(selectResultTcgVar, select.falseCase()),
                end
            )
        ));


    // set the insertion node's next to the ifNode
    insertionPoint.setNext(ifElse.left());
    // set the merge's next node to the scheduled result
    ifElse.right().setNext(scheduledResultNode);

    // replace the select expression by the new result node expression and remove it
    select.replaceAndDelete(resultExprNode);

    return null;
  }

}
