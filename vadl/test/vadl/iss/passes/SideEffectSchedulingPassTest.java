// SPDX-FileCopyrightText : © 2026 TU Wien <vadl@tuwien.ac.at>
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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static vadl.utils.GraphUtils.getSingleNode;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import vadl.AbstractTest;
import vadl.configuration.IssConfiguration;
import vadl.iss.SideEffectUtils;
import vadl.iss.passes.common.IssSideEffectReorderingPass;
import vadl.pass.PassOrders;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.utils.ViamUtils;
import vadl.viam.DefProp;
import vadl.viam.Instruction;
import vadl.viam.Procedure;
import vadl.viam.Specification;
import vadl.viam.graph.Graph;
import vadl.viam.graph.control.AbstractBeginNode;
import vadl.viam.graph.control.AbstractEndNode;
import vadl.viam.graph.control.ControlNode;
import vadl.viam.graph.control.DirectionalNode;
import vadl.viam.graph.control.ForallNode;
import vadl.viam.graph.control.IfNode;
import vadl.viam.graph.control.ScheduledNode;
import vadl.viam.graph.control.StartNode;
import vadl.viam.graph.dependency.SideEffectNode;
import vadl.viam.passes.sideEffectScheduling.SideEffectSchedulingPass;
import vadl.viam.passes.sideEffectScheduling.nodes.InstrExitNode;

public class SideEffectSchedulingPassTest extends AbstractTest {

  /**
   * Tests that after running the {@link IssSideEffectReorderingPass} and
   * {@link SideEffectSchedulingPass}, all side effects are in the correct order.
   */
  @Test
  public void testSideEffectsOrder() throws DuplicatedPassKeyException, IOException {
    var spec = spec();
    behaviors(spec).forEach(this::checkSideEffectOrder);
  }

  private Set<Graph> behaviors(Specification spec) {
    return ViamUtils.findDefinitionsByFilter(spec, def ->
            def instanceof Instruction || def instanceof Procedure)
        .stream().flatMap(def -> ((DefProp.WithBehavior) def).behaviors().stream())
        .collect(Collectors.toSet());
  }

  private void checkSideEffectOrder(Graph behavior) {
    var start = getSingleNode(behavior, StartNode.class);
    checkBranch(start, new HashSet<>());
  }

  private Set<SideEffectUtils.SideEffectType> checkBranch(
      AbstractBeginNode beginNode, Set<SideEffectUtils.SideEffectType> prev) {
    ControlNode current = beginNode;

    while (true) {
      switch (current) {
        case AbstractEndNode endNode -> {
          return prev;
        }
        case ScheduledNode scheduledNode -> {
          var node = scheduledNode.node();
          if (node instanceof SideEffectNode sideEffect) {
            var type = SideEffectUtils.SideEffectType.classify(sideEffect);
            // check that all prev side effects may come before this side effect
            checkOrder(prev, type, current.graph());
            prev.add(type);
          }
          current = scheduledNode.next();
        }
        case InstrExitNode instrExit -> {
          checkOrder(prev, SideEffectUtils.SideEffectType.PC, current.graph());
          current = instrExit.next();
        }
        case IfNode splitNode -> {
          var seTrue = checkBranch(splitNode.trueBranch(), new HashSet<>(prev));
          checkBranch(splitNode.falseBranch(), prev);
          prev.addAll(seTrue);
          current = splitNode.mergeNode();
        }
        case ForallNode splitNode -> {
          // check twice:
          // - once with prev
          // - once with prev + side effects inside forall
          checkBranch(splitNode.beginNode(), prev);
          checkBranch(splitNode.beginNode(), prev);
          current = splitNode.mergeNode();
        }
        case DirectionalNode directionalNode -> current = directionalNode.next();
        default -> //noinspection DataFlowIssue
            current.ensure(false,
                "Not an expected node in the IssSideEffectReorderingPassTest. "
                    + "You want to implement it.");
      }
    }
  }

  private void checkOrder(Set<SideEffectUtils.SideEffectType> prev,
                          SideEffectUtils.SideEffectType c, Graph graph) {
    for (var p : prev) {
      assertTrue(p.ord() <= c.ord(), "Side effect order is not correct: "
          + "found %s before %s in %s".formatted(p, c, graph.name));
    }
  }

  private Specification spec()
      throws IOException, DuplicatedPassKeyException {
    var config = new IssConfiguration(getConfiguration(false));
    var specFile = "passes/sideEffectReordering/reordering-spec.vadl";
    return setupPassManagerAndRunSpec(specFile,
        PassOrders.iss(config).untilFirst(SideEffectSchedulingPass.class)
    ).specification();
  }
}
