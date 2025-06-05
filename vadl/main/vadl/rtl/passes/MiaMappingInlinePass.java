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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.rtl.ipg.InstructionProgressGraph;
import vadl.rtl.ipg.nodes.RtlConditionalReadNode;
import vadl.rtl.map.MiaMapping;
import vadl.rtl.utils.SubgraphUtils;
import vadl.utils.GraphUtils;
import vadl.utils.Pair;
import vadl.viam.Specification;
import vadl.viam.Stage;
import vadl.viam.StageOutput;
import vadl.viam.graph.Node;
import vadl.viam.graph.ViamGraphError;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.ReadStageOutputNode;
import vadl.viam.graph.dependency.SideEffectNode;
import vadl.viam.graph.dependency.WriteStageOutputNode;

/**
 * Inline nodes from the instruction progress graph into the MiA description based on the
 * MiA mapping. This adds the instruction progress graph nodes in the stage behaviors.
 */
public class MiaMappingInlinePass extends Pass {

  public MiaMappingInlinePass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("MiA Mapping Inline");
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

    var map = new HashMap<Pair<Node, Stage>, StageOutput>();

    for (Stage stage : stages) {
      var stageContexts = mapping.stageContexts(stage).toList();
      var stageNodes = stageContexts.stream()
          .map(MiaMapping.NodeContext::ipgNodes).flatMap(Collection::stream)
          .collect(Collectors.toSet());

      // copy subgraph to stage behavior
      // add stage outputs to pass data between stages
      var copyMap = SubgraphUtils.copy(stage.behavior(), stageNodes,
          (originalFrom, originalTo, copyFrom) -> {
            if (originalTo instanceof ExpressionNode originalExpr) {
              var output = resolveStageOutput(originalExpr, stage.prev(), map);
              return new ReadStageOutputNode(output);
            }
            return null;
          },
          (originalFrom, originalTo, copyFrom) -> {
            if (originalFrom instanceof ConstantNode) {
              return null;
            }
            if (originalFrom instanceof ExpressionNode originalExpr
                && copyFrom instanceof ExpressionNode copyExpr) {
              var output = outputFor(originalExpr, stage, map);
              return new WriteStageOutputNode(output, copyExpr);
            }
            return null;
          });

      // add conditions to read and write nodes
      for (MiaMapping.NodeContext context : stageContexts) {
        for (Node src : context.ipgNodes()) {
          var dest = copyMap.get(src);
          if (dest instanceof SideEffectNode node) {
            var cond = node.condition();
            node.ensure(cond != null, "Condition input must be set before we extend it");
            node.setCondition(patchCondition(cond, context));
          }
          if (dest instanceof RtlConditionalReadNode read) {
            var cond = read.condition();
            read.asReadNode().ensure(cond != null,
                "Condition input must be set before we extend it");
            read.setCondition(patchCondition(cond, context));
          }
        }
      }
    }

    return null;
  }

  private StageOutput resolveStageOutput(ExpressionNode node, @Nullable Stage stage,
                                         Map<Pair<Node, Stage>, StageOutput> map) {
    if (stage == null) {
      throw new ViamGraphError("Can not find output of previous stage for node")
          .addContext(node);
    }

    // try to get output node from this stage
    var inStage = map.get(Pair.of(node, stage));
    if (inStage != null) {
      return inStage;
    }

    // if not, recurse through previous stages
    var inPrev = resolveStageOutput(node, stage.prev(), map);

    // create new output and pass it through from previous stage
    // by introducing a read and write node
    var output = outputFor(node, stage, map);
    var read = new ReadStageOutputNode(inPrev);
    var write = new WriteStageOutputNode(output, read);
    stage.behavior().addWithInputs(write);

    return output;
  }

  private StageOutput outputFor(ExpressionNode node, Stage stage,
                                Map<Pair<Node, Stage>, StageOutput> map) {
    // get existing output
    var existing = map.get(Pair.of(node, stage));
    if (existing != null) {
      return existing;
    }

    // else create new output
    var output = new StageOutput(
        stage.identifier.append(nameFor(node)),
        node.type()
    );
    stage.addOutput(output);
    map.put(Pair.of(node, stage), output);

    return output;
  }

  private String nameFor(Node node) {
    var fallback = "n" + node.id.numericId();
    if (node.ensureGraph() instanceof InstructionProgressGraph ipg) {
      return ipg.getContext(node).shortestNameHint().orElse(fallback);
    }
    return fallback;
  }

  private ExpressionNode patchCondition(ExpressionNode cond, MiaMapping.NodeContext context) {
    var miaCond = context.sideEffects().stream()
        .map(SideEffectNode::nullableCondition)
        .map(Optional::ofNullable)
        .reduce(this::orReduce); // returns empty if a null or true condition is encountered
    if (miaCond.isPresent() && miaCond.get().isPresent()) {
      cond = cond.ensureGraph().addWithInputs(GraphUtils.and(miaCond.get().get(), cond));
    }
    return cond;
  }

  private Optional<ExpressionNode> orReduce(Optional<ExpressionNode> c1,
                                            Optional<ExpressionNode> c2) {
    if (c1.isEmpty() || c2.isEmpty()) {
      return Optional.empty();
    }
    if (c1.get() instanceof ConstantNode c && c.constant().asVal().bool()) {
      return Optional.empty();
    }
    if (c2.get() instanceof ConstantNode c && c.constant().asVal().bool()) {
      return Optional.empty();
    }
    return Optional.of(GraphUtils.or(c1.get().copy(), c2.get().copy()));
  }

}
