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

import com.google.common.collect.Multimaps;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.rtl.ipg.InstructionProgressGraph;
import vadl.rtl.ipg.nodes.RtlReadMemNode;
import vadl.rtl.ipg.nodes.RtlWriteMemNode;
import vadl.rtl.map.MiaMapping;
import vadl.viam.Specification;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ReadMemNode;
import vadl.viam.graph.dependency.ReadRegFileNode;
import vadl.viam.graph.dependency.ReadRegNode;
import vadl.viam.graph.dependency.WriteMemNode;
import vadl.viam.graph.dependency.WriteRegFileNode;
import vadl.viam.graph.dependency.WriteRegNode;

/**
 * Try to merge nodes in the instruction progress graph in order to simplify it.
 * Respects the MiA mapping by only merging nodes mapped to the same stage.
 *
 * <li>Merge non-concurrent reads and writes and add select nodes for inputs.
 */
public class InstructionProgressGraphMergePass extends Pass {

  public InstructionProgressGraphMergePass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("Instruction Progress Graph Merge");
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

    var ipg = optIsa.get().expectExtension(InstructionProgressGraphExtension.class).ipg();
    var stages = optMia.get().stages();
    var mapping = optMia.get().extension(MiaMapping.class);
    if (mapping == null) {
      return null;
    }

    var deleted = new ArrayList<Node>();

    for (var stage : stages) {
      var contexts = mapping.stageContexts(stage);
      Multimaps.index(contexts.iterator(), MiaMapping.NodeContext::sideEffects)
          .asMap().forEach((sideEffects, contextList) -> {
            // only merge nodes with same set of side effects associated
            var ipgNodes = contextList.stream()
                .map(MiaMapping.NodeContext::ipgNodes).flatMap(Collection::stream)
                .collect(Collectors.toSet());
            merge(ipg, mapping, ReadRegNode.class, ipgNodes, deleted);
            merge(ipg, mapping, ReadRegFileNode.class, ipgNodes, deleted);
            merge(ipg, mapping, ReadMemNode.class, ipgNodes, deleted);
            merge(ipg, mapping, RtlReadMemNode.class, ipgNodes, deleted);
            merge(ipg, mapping, WriteRegNode.class, ipgNodes, deleted);
            merge(ipg, mapping, WriteRegFileNode.class, ipgNodes, deleted);
            merge(ipg, mapping, WriteMemNode.class, ipgNodes, deleted);
            merge(ipg, mapping, RtlWriteMemNode.class, ipgNodes, deleted);
          });
    }

    return deleted;
  }

  private <T extends Node> void merge(InstructionProgressGraph ipg, MiaMapping mapping,
                                      Class<T> nodeClass, Set<Node> ipgNodes, List<Node> deleted) {
    ipg.merge(ipgNodes.stream().filter(nodeClass::isInstance)
        .map(nodeClass::cast).collect(Collectors.toSet()),
        removed -> {
          mapping.removeNode(removed);
          ipgNodes.remove(removed);
          deleted.add(removed);
        },
        added -> {
          var usage = mapping.ensureContext(added.usages().findFirst().orElseThrow());
          usage.ipgNodes().add(added);
        });
  }

}
