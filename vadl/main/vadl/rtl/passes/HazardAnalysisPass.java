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
import java.util.ArrayList;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.rtl.analysis.HazardAnalysis;
import vadl.rtl.ipg.nodes.RtlConditionalReadNode;
import vadl.rtl.map.MiaMapping;
import vadl.viam.Resource;
import vadl.viam.Specification;
import vadl.viam.Stage;
import vadl.viam.graph.Node;
import vadl.viam.graph.ViamGraphError;
import vadl.viam.graph.dependency.ReadResourceNode;
import vadl.viam.graph.dependency.WriteResourceNode;

/**
 * Analyze data and control hazards. Attach resulting analyses to resources.
 */
public class HazardAnalysisPass extends Pass {

  public HazardAnalysisPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("Resource Hazard Analysis");
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

    var resources = new ArrayList<Resource>();
    resources.addAll(optIsa.get().ownRegisters());
    resources.addAll(optIsa.get().ownRegisterFiles());
    resources.addAll(optIsa.get().ownMemories());

    var ipg = optIsa.get().expectExtension(InstructionProgressGraphExtension.class).ipg();
    var mapping = optMia.get().expectExtension(MiaMapping.class);
    var result = new ArrayList<HazardAnalysis>();

    for (Resource resource : resources) {
      var reads = ipg.getNodes(ReadResourceNode.class)
          .filter(n -> n.resourceDefinition().equals(resource))
          .map(read -> new HazardAnalysis.ReadAnalysis(
              read, stage(mapping, read),
              condition(mapping, read),
              read.hasAddress() ? stage(mapping, read.address()) : null
          ))
          .collect(Collectors.toSet());
      var writes = ipg.getNodes(WriteResourceNode.class)
          .filter(n -> n.resourceDefinition().equals(resource))
          .map(write -> new HazardAnalysis.WriteAnalysis(
              write, stage(mapping, write),
              condition(mapping, write),
              write.hasAddress() ? stage(mapping, write.address()) : null,
              stage(mapping, write.value())
          ))
          .collect(Collectors.toSet());
      var analysis = new HazardAnalysis(resource, reads, writes);
      resource.attachExtension(analysis);
      result.add(analysis);
    }

    return result;
  }

  private Stage condition(MiaMapping mapping, Node node) {
    if (node instanceof RtlConditionalReadNode read) {
      return stage(mapping, read.condition());
    }
    if (node instanceof WriteResourceNode write) {
      return stage(mapping, write.condition());
    }
    throw new ViamGraphError("Resource read/write node without condition")
        .addContext(node);
  }

  private Stage stage(MiaMapping mapping, @Nullable Node node) {
    if (node == null) {
      throw new ViamGraphError("Unmapped node during resource hazard analysis");
    }
    return mapping.ensureContext(node).stage();
  }

}
