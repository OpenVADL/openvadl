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
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.rtl.map.MiaMapping;
import vadl.viam.Specification;
import vadl.viam.Stage;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * Improve the MiA mapping by moving nodes on the fringe of two map nodes if they reduce the size of
 * registers needed between stages.
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

    boolean change;
    do {
      change = false;
      for (Stage stage : stages) {
        var candidates = mapping.stageContexts(stage)
            .flatMap(MiaMapping.NodeContext::movableIpgNodes)
            .filter(n -> isCandidate(stage, mapping, n)).toList();
        for (Node candidate : candidates) {
          var context = mapping.findContext(candidate).orElseThrow();
          context.ipgNodes().remove(candidate);
          context.pred().forEach(pred -> pred.ipgNodes().add(candidate));
          change = true;
        }
      }
    } while (change);

    return null;
  }

  private boolean isCandidate(Stage stage, MiaMapping mapping, Node ipgNode) {
    return (ipgNode.inputs().noneMatch(node -> mapping.containsInStage(stage, node))
        && ipgNode.inputs().mapToInt(this::bitWidth).sum() > bitWidth(ipgNode)
        && ipgNode.inputs().noneMatch(
            input -> hasMoreUsages(stage, mapping, ipgNode, input)));
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
