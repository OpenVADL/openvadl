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

import com.google.common.collect.Sets;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.rtl.ipg.InstructionProgressGraph;
import vadl.rtl.ipg.nodes.RtlSelectByInstructionNode;
import vadl.rtl.map.MiaMapping;
import vadl.rtl.utils.GraphMergeUtils;
import vadl.rtl.utils.RtlSimplificationRules;
import vadl.rtl.utils.RtlSimplifier;
import vadl.viam.Specification;
import vadl.viam.Stage;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.passes.canonicalization.Canonicalizer;

/**
 * Improve the MiA mapping by moving nodes on the fringe of two map nodes if they reduce the size of
 * registers needed between stages. If we encounter a select-by-instruction node that has inputs
 * from multiple stages, it is split up to reduce the number of results to pass between stages.
 * We also introduce select-by-instruction nodes to reduce stage outputs.
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

    var ipg = optIsa.get().expectExtension(InstructionProgressGraphExtension.class).ipg();

    var stages = optMia.get().stages();
    final var mapping = optMia.get().extension(MiaMapping.class);
    if (mapping == null) {
      return null;
    }

    var added = new HashSet<Node>();
    var removed = new HashSet<Node>();
    int changes;
    do {
      added.clear();
      removed.clear();
      changes = 0;

      // move nodes up where necessary
      changes += moveNodesUp(stages, mapping);

      // split select-by-instruction nodes by stages
      splitSelectNodes(stages, mapping, ipg, added);
      changes += moveNodesUp(stages, mapping);

      // share stage outputs by introducing select-by-instruction nodes
      combineOutputs(stages, mapping, ipg, added);
      dedupNodes(stages, mapping, ipg, removed);

    } while (changes > 0 || !Sets.symmetricDifference(added, removed).isEmpty());

    // optimize
    Canonicalizer.canonicalize(ipg);
    new RtlSimplifier(RtlSimplificationRules.rules).run(ipg, mapping);

    return null;
  }

  private void splitSelectNodes(List<Stage> stages, MiaMapping mapping,
                                InstructionProgressGraph ipg, Set<Node> added) {
    var stagesRev = new ArrayList<>(stages);
    Collections.reverse(stagesRev);
    for (Stage stage : stagesRev) {
      // split up select-by-instruction nodes
      var selects = mapping.stageContexts(stage)
          .flatMap(MiaMapping.NodeContext::movableIpgNodes)
          .filter(RtlSelectByInstructionNode.class::isInstance)
          .map(RtlSelectByInstructionNode.class::cast)
          .collect(Collectors.toSet());
      for (RtlSelectByInstructionNode select : selects) {
        var values = new HashSet<>(select.values());
        var valOutStage = new HashSet<ExpressionNode>();
        // partition value inputs by in/out stage
        for (ExpressionNode val : values) {
          var valStage = mapping.ensureContext(val).stage();
          if (!valStage.equals(stage)) {
            valOutStage.add(val);
          }
        }
        // split for more than one value from outside stage, but not for all
        if (valOutStage.size() > 1 && valOutStage.size() < values.size()) {
          var newSelect = select.split(valOutStage);
          mapping.ensureContext(select).ipgNodes().add(newSelect);
          ipg.getContext(newSelect).instructions()
              .addAll(newSelect.instructions().stream().flatMap(Collection::stream).toList());
          added.add(newSelect);
        }
      }
    }
  }

  private int moveNodesUp(List<Stage> stages, MiaMapping mapping) {
    int count = 0;
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
          count++;
        }
      }
    } while (change);
    return count;
  }

  private void combineOutputs(List<Stage> stages, MiaMapping mapping,
                              InstructionProgressGraph ipg, Set<Node> added) {
    for (Stage stage : stages) {
      var outMap = new HashMap<Integer, Set<ExpressionNode>>();
      mapping.stageOutputs(stage)
          .filter(n -> bitWidth(n) > 1)
          .forEach(node -> {
            outMap.computeIfAbsent(bitWidth(node), k -> new HashSet<>()).add(node);
          });
      for (Set<ExpressionNode> set : outMap.values()) {
        if (set.size() <= 1) {
          continue;
        }
        // add select-by-instruction nodes for every usage as an output before we try to merge them
        var selects = set.stream().flatMap(expr -> {
          if (expr instanceof RtlSelectByInstructionNode select) {
            return Stream.of(select);
          }
          var outputUsages = expr.usages()
              .filter(u -> !mapping.containsInStage(stage, u)).toList();
          return outputUsages.stream().map(usage -> {
            var ins = ipg.getContext(usage).instructions();
            var sel = ipg.add(new RtlSelectByInstructionNode(expr.type()), ins);
            ins.forEach(instr -> sel.add(instr, expr));
            usage.replaceInput(expr, sel);
            mapping.ensureContext(expr).ipgNodes().add(sel);
            added.add(sel);
            return sel;
          });
        }).toList();

        // merge
        GraphMergeUtils.merge(selects,
            new GraphMergeUtils.SelectByInstructionMergeStrategy(
                node -> ipg.getContext(node).instructions(),
                (n1, n2) ->
                    ipg.getContext(n1).instructions().addAll(ipg.getContext(n2).instructions())
            ));

        // remove select-by-instruction node again, if not merged or deleted
        for (RtlSelectByInstructionNode select : selects) {
          if (select.isActive() && select.values().size() == 1) {
            var expr = select.values().getFirst();
            select.replaceAndDelete(expr);
          }
          if (select.isDeleted()) {
            mapping.removeNode(select);
            added.remove(select);
          }
        }
      }
    }
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

  private void dedupNodes(List<Stage> stages, MiaMapping mapping,
                          InstructionProgressGraph ipg, Set<Node> removed) {
    boolean change = true;
    while (change) {
      change = false;
      for (Stage stage : stages) {
        for (Node node : mapping.stageIpgNodes(stage).toList()) {
          if (node.isDeleted() || node.inputs().findAny().isEmpty()) {
            continue;
          }
          var dup = ipg.findDuplicate(node);
          if (dup != null) {
            var ins = ipg.getContext(dup).instructions();
            ipg.getContext(node).instructions().addAll(ins);
            dup.replaceAndDelete(node);
            change = true;
            removed.add(dup);
          }
        }
      }
    }
  }

}
