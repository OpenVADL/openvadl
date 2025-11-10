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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.rtl.analysis.HazardAnalysis;
import vadl.rtl.ipg.nodes.RtlConditionalReadNode;
import vadl.rtl.ipg.nodes.RtlSelectByInstructionNode;
import vadl.rtl.map.MiaMapping;
import vadl.utils.Pair;
import vadl.viam.Constant;
import vadl.viam.MicroArchitecture;
import vadl.viam.Resource;
import vadl.viam.Specification;
import vadl.viam.Stage;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.ViamGraphError;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.LetNode;
import vadl.viam.graph.dependency.ReadResourceNode;
import vadl.viam.graph.dependency.SelectNode;
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
    var optIsa = viam.mia().map(MicroArchitecture::isa);
    if (optIsa.isEmpty()) {
      return null;
    }
    var optMia = viam.mia();
    if (optMia.isEmpty()) {
      return null;
    }

    var resources = new ArrayList<Resource>();
    resources.addAll(optIsa.get().registerTensors());
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
              stage(mapping, read.indices())
          ))
          .collect(Collectors.toCollection(LinkedHashSet::new));
      var writes = ipg.getNodes(WriteResourceNode.class)
          .filter(n -> n.resourceDefinition().equals(resource))
          .map(write -> new HazardAnalysis.WriteAnalysis(
              write, stage(mapping, write),
              condition(mapping, write),
              stage(mapping, write.indices()),
              stage(mapping, write.value())
          ))
          .collect(Collectors.toCollection(LinkedHashSet::new));
      var forwards = writes.stream()
          .flatMap(writeAnalysis -> forward(mapping, writeAnalysis))
          .collect(Collectors.toCollection(LinkedHashSet::new));
      var analysis = new HazardAnalysis(resource, reads, writes, forwards);
      resource.attachExtension(analysis);
      result.add(analysis);
    }

    return result;
  }

  private Stage condition(MiaMapping mapping, Node node) {
    if (node instanceof RtlConditionalReadNode read) {
      return stage(mapping, read.nullableCondition());
    }
    if (node instanceof WriteResourceNode write) {
      return stage(mapping, write.nullableCondition());
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

  @Nullable
  private Stage stage(MiaMapping mapping, NodeList<ExpressionNode> list) {
    if (list.isEmpty()) {
      return null;
    }
    var stages = list.stream().map(node -> mapping.ensureContext(node).stage())
        .collect(Collectors.toSet());
    return mapping.mia().stages().reversed().stream().filter(stages::contains).findFirst()
        .orElse(null); // get last stage for nodes in list
  }

  private Stream<HazardAnalysis.ForwardAnalysis> forward(MiaMapping mapping,
                                                         HazardAnalysis.WriteAnalysis analysis) {
    var result = new ArrayList<HazardAnalysis.ForwardAnalysis>();

    // start with forwarding in write stage
    var write = analysis.node();
    var stage = analysis.effect();
    var conditions = new ArrayList<Pair<ExpressionNode, ExpressionNode>>();
    conditions.add(Pair.of(write.condition(), Constant.Value.of(true).toNode()));
    ExpressionNode address = null;
    if (write.hasAddress()) {
      address = write.address();
    }
    ExpressionNode value = write.value();

    // add forward from write stage (always possible)
    result.add(new HazardAnalysis.ForwardAnalysis(write, analysis.effect(), stage,
        new ArrayList<>(conditions), address, value));

    // stop if we passed the stage where address or condition are available
    var stop = Stream.of(analysis.condition(), analysis.address()).filter(Objects::nonNull)
        .map(Stage::prev).filter(Objects::nonNull).toList();

    // try to resolve value in previous stages
    stage = stage.prev();
    while (stage != null && !stop.contains(stage)) {
      value = resolveForward(mapping, stage, value, conditions);
      if (value == null) {
        break; // can not resolve anymore (provide value and condition for forwarding)
      }
      result.add(new HazardAnalysis.ForwardAnalysis(write, analysis.effect(), stage,
          new ArrayList<>(conditions), address, value));
      stage = stage.prev();
    }

    return result.stream();
  }

  // resolve select-by-instruction and select nodes and collect condition
  @Nullable
  private ExpressionNode resolveForward(
      MiaMapping mapping, Stage stageFrom, ExpressionNode value,
      List<Pair<ExpressionNode, ExpressionNode>> conditions
  ) {
    if (mapping.containsInStage(stageFrom, value)) {
      return value;
    }
    if (value instanceof RtlSelectByInstructionNode select) {
      for (int i = 0; i < select.instructions().size(); i++) {
        var val = select.values().get(i);
        var res = resolveForward(mapping, stageFrom, val, conditions);
        if (res != null) {
          var sel = Objects.requireNonNull(select.selection());
          var index = Constant.Value.of(i, sel.type().asDataType()).toNode();
          conditions.add(Pair.of(sel, index));
          return res;
        }
      }
    }
    if (value instanceof SelectNode select) {
      var res = resolveForward(mapping, stageFrom, select.trueCase(), conditions);
      if (res != null) {
        conditions.add(Pair.of(select.condition(), Constant.Value.of(true).toNode()));
        return res;
      }
      res = resolveForward(mapping, stageFrom, select.falseCase(), conditions);
      if (res != null) {
        conditions.add(Pair.of(select.condition(), Constant.Value.of(true).toNode()));
        return res;
      }
    }
    if (value instanceof LetNode let) {
      return resolveForward(mapping, stageFrom, let.expression(), conditions);
    }
    return null;
  }

}
