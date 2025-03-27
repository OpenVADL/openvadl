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

package vadl.rtl.passes;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.rtl.ipg.nodes.SelectByInstructionNode;
import vadl.rtl.map.MiaMapping;
import vadl.viam.Specification;
import vadl.viam.Stage;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * Improve the MiA mapping by moving nodes on the fringe of two map nodes if they reduce the size of
 * registers needed between stages. If we encounter a select-by-instruction node that has inputs
 * from multiple stages, it is split up to reduce the number of results to pass between stages.
 */
public class MiaMappingOptimizePass extends Pass {

  public MiaMappingOptimizePass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("MiA Mapping Optimize");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    var optIsa = viam.isa();
    if (optIsa.isEmpty()) {
      return null;
    }
    var optMia = viam.mia();
    if (optMia.isEmpty()) {
      return null;
    }

    var stages = optMia.get().stages();
    final var mapping = optMia.get().extension(MiaMapping.class);
    if (mapping == null) {
      return null;
    }

    // move nodes up where necessary
    moveNodesUp(stages, mapping);

    // split select-by-instruction nodes by stages
    splitSelectNodes(stages, mapping);
    moveNodesUp(stages, mapping);

    return null;
  }

  private void splitSelectNodes(List<Stage> stages, MiaMapping mapping) {
    for (Stage stage : stages) {
      // split up select-by-instruction nodes
      var selects = mapping.stageContexts(stage)
          .flatMap(MiaMapping.NodeContext::movableIpgNodes)
          .filter(SelectByInstructionNode.class::isInstance)
          .map(SelectByInstructionNode.class::cast)
          .collect(Collectors.toSet());
      for (SelectByInstructionNode select : selects) {
        var stageToVals = new HashMap<Stage, Set<ExpressionNode>>();

        // partition value inputs by stage
        for (ExpressionNode val : select.values()) {
          var valStage = mapping.ensureContext(val).stage();
          stageToVals.computeIfAbsent(valStage, k -> new HashSet<>())
              .add(val);
        }
        if (stageToVals.size() > 1) {
          for (var entry : stageToVals.entrySet()) {
            var valStage = entry.getKey();
            var vals = entry.getValue();
            // split only for stage different from the current and more than one value input
            if (!valStage.equals(stage) && vals.size() > 1) {
              var newSelect = select.split(vals);
              mapping.ensureContext(select).ipgNodes().add(newSelect);
            }
          }
        }
      }
    }
  }

  private void moveNodesUp(List<Stage> stages, MiaMapping mapping) {
    boolean change;
    do {
      change = false;
      for (Stage stage : stages) {
        // move nodes that are not fixed if we reduce bits passed between stages
        var candidates = mapping.stageContexts(stage)
            .flatMap(MiaMapping.NodeContext::movableIpgNodes)
            .filter(n -> isCandidate(stage, mapping, n)).toList();
        for (Node candidate : candidates) {
          var context = mapping.ensureContext(candidate);
          context.ipgNodes().remove(candidate);
          context.pred().forEach(pred -> pred.ipgNodes().add(candidate));
          change = true;
        }
      }
    } while (change);
  }

  // node can be moved if it has no inputs from the current stage and
  // the bits we save passing between the stages outweigh the bits the node outputs
  private boolean isCandidate(Stage stage, MiaMapping mapping, Node ipgNode) {
    return (ipgNode.inputs().noneMatch(node -> mapping.containsInStage(stage, node))
        && sumInputsWithoutMoreUsages(stage, mapping, ipgNode) > bitWidth(ipgNode));
  }

  // sum bit widths of inputs that have no more usages in the current stage
  // i.e., are saved when moving the node up
  private int sumInputsWithoutMoreUsages(Stage stage, MiaMapping mapping, Node ipgNode) {
    return ipgNode.inputs()
        .filter(input -> !hasMoreUsages(stage, mapping, ipgNode, input))
        .mapToInt(this::bitWidth).sum();
  }

  private int bitWidth(Node input) {
    if (input instanceof ExpressionNode expr) {
      return expr.type().asDataType().bitWidth();
    }
    return 0;
  }

  // node has more usage inside the given mapping than at the node self
  private boolean hasMoreUsages(Stage stage, MiaMapping mapping, Node self, Node node) {
    return node.usages().filter(n -> !self.equals(n))
        .anyMatch(other -> mapping.containsInStage(stage, other));
  }
}
