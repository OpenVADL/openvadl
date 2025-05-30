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

package vadl.rtl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Assertions;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.rtl.ipg.nodes.RtlIsInstructionNode;
import vadl.rtl.ipg.nodes.RtlReadMemNode;
import vadl.rtl.ipg.nodes.RtlReadRegTensorNode;
import vadl.rtl.ipg.nodes.RtlWriteMemNode;
import vadl.rtl.ipg.nodes.RtlSelectByInstructionNode;
import vadl.rtl.passes.InstructionProgressGraphExtension;
import vadl.rtl.utils.RtlSimplificationRules;
import vadl.rtl.utils.RtlSimplifier;
import vadl.rtl.utils.SubgraphUtils;
import vadl.types.Type;
import vadl.viam.Constant;
import vadl.viam.Resource;
import vadl.viam.Specification;
import vadl.viam.graph.Graph;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ReadMemNode;
import vadl.viam.graph.dependency.ReadRegTensorNode;
import vadl.viam.graph.dependency.ReadResourceNode;
import vadl.viam.graph.dependency.WriteMemNode;
import vadl.viam.graph.dependency.WriteRegTensorNode;
import vadl.viam.graph.dependency.WriteResourceNode;

/**
 * Testing utility for checking instruction behavior against instruction progress graph behavior.
 *
 * <p>Compare specified instruction behaviors with the behavior the instruction progress graph
 * exhibits for this instruction. This includes simplifying the graph with constant values for
 * is-instruction and select-by-instruction nodes.
 */
public class InstructionBehaviorCheckPass extends Pass {

  private final boolean useInstructionContext;

  public InstructionBehaviorCheckPass(GeneralConfiguration configuration) {
    super(configuration);
    this.useInstructionContext = true;
  }

  public InstructionBehaviorCheckPass(GeneralConfiguration configuration,
                                      boolean useInstructionContext) {
    super(configuration);
    this.useInstructionContext = useInstructionContext;
  }

  @Override
  public PassName getName() {
    return PassName.of("Instruction Behavior Check");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    var isa = viam.isa().orElseThrow();
    var ipg = isa.expectExtension(InstructionProgressGraphExtension.class).ipg();

    for (var curInstr : isa.ownInstructions()) {
      Graph graph;
      if (useInstructionContext) {
        graph = new Graph("IPG graph for " + curInstr.simpleName());
        var ipgNodes = ipg.getNodes()
            .filter(n -> ipg.getContext(n).instructions().contains(curInstr))
            .collect(Collectors.toSet());
        SubgraphUtils.copy(graph, ipgNodes,
            (from, to, copyFrom) -> to.copy(),
            (from, to, copyFrom) -> null);
      } else {
        graph = ipg.copy("IPG graph for " + curInstr.simpleName());
      }

      // replace is-instruction and select-by-instruction nodes with constants/constant selection
      for (RtlIsInstructionNode isIns : graph.getNodes(RtlIsInstructionNode.class).toList()) {
        var constNode = Constant.Value.of(isIns.instructions().contains(curInstr))
            .toNode();
        isIns.replaceAndDelete(constNode);
      }
      for (RtlSelectByInstructionNode sel : graph.getNodes(RtlSelectByInstructionNode.class).toList()) {
        if (sel.selection() == null) { // selection inputs are handled during simplification
          for (int i = 0; i < sel.instructions().size(); i++) {
            if (sel.instructions().get(i).contains(curInstr)) {
              sel.replaceAndDelete(sel.values().get(i));
              break;
            }
          }
        }
      }

      // simplify using rules and remove inactive writes
      new RtlSimplifier(RtlSimplificationRules.rules).run(graph);

      for (WriteResourceNode write : graph.getNodes(WriteResourceNode.class).toList()) {
        var cond = write.nullableCondition();
        if (cond instanceof ConstantNode n && !n.constant().asVal().castTo(Type.bool()).bool()) {
          write.safeDelete();
        }
      }

      // compare number of read/write nodes per resource
      var resources = new ArrayList<Resource>();
      resources.addAll(isa.registerTensors());
      resources.addAll(isa.ownMemories());
      var pc = isa.pc();
      if (pc != null) {
        resources.remove(pc.registerTensor()); // do not check pc for now
      }

      for (Resource res : resources) {
        compare(curInstr.behavior(), graph, ReadResourceNode.class, res);
        compare(curInstr.behavior(), graph, ReadRegTensorNode.class, RtlReadRegTensorNode.class, res);
        compare(curInstr.behavior(), graph, ReadMemNode.class, RtlReadMemNode.class, res);

        compare(curInstr.behavior(), graph, WriteResourceNode.class, res);
        compare(curInstr.behavior(), graph, WriteRegTensorNode.class, res);
        compare(curInstr.behavior(), graph, WriteMemNode.class, RtlWriteMemNode.class, res);
      }
    }

    return null;
  }

  private void compare(Graph instrBeh, Graph graph, Class<? extends Node> type, Resource resource) {
    compare(instrBeh, graph, type, type, resource);
  }

  private void compare(Graph instrBeh, Graph graph,
                       Class<? extends Node> typeIns, Class<? extends Node> typeIpg,
                       Resource resource) {

    var inInstr = instrBeh.getNodes(typeIns)
        .filter(n -> filterResource(n, resource)).toList();
    var inGraph = graph.getNodes(typeIpg)
        .filter(n -> filterResource(n, resource)).toList();
    Assertions.assertEquals(inInstr.size(), inGraph.size(), "Number of "
        + typeIns.getSimpleName() + " nodes does not number of " + typeIpg.getSimpleName()
        + " nodes for resource " + resource);
  }

  private boolean filterResource(Node node, Resource resource) {
    if (resource == null) {
      return true;
    }
    if (node instanceof ReadResourceNode n) {
      return n.resourceDefinition().equals(resource);
    }
    if (node instanceof WriteResourceNode n) {
      return n.resourceDefinition().equals(resource);
    }
    return false;
  }
}
