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
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.pass.exception.PassError;
import vadl.rtl.map.MiaBuiltInCallMatcher;
import vadl.rtl.map.MiaMapping;
import vadl.types.BuiltInTable;
import vadl.types.MicroArchitectureType;
import vadl.viam.Specification;
import vadl.viam.Stage;
import vadl.viam.StageOutput;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.MiaBuiltInCall;
import vadl.viam.graph.dependency.ReadStageOutputNode;
import vadl.viam.graph.dependency.SideEffectNode;
import vadl.viam.graph.dependency.WriteStageOutputNode;

/**
 * Create the mapping of the instruction progress graph to the micro architecture description.
 * This adds a MiA mapping to each stage following the stage order.
 *
 * <p>Steps of the mapping in each stage:
 *
 * <li>Identify source, map and sink nodes that read, map and write an instruction type result
 * <li>Source and sink nodes are connected between stages using the stage outputs
 * <li>For each map node the previously mapped nodes are taken from its instruction type inputs
 * <li>The IPG nodes mapped by a map node are gathered using the {@link MiaBuiltInCallMatcher}
 * <li>These matched nodes' inputs are then added up, excluding the set of previously matched nodes
 * <li>This results in a set of nodes that map to the map node alone and are associated with it
 * in the MiA mapping.
 */
public class MiaMappingCreationPass extends Pass {

  public MiaMappingCreationPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("MiA Mapping Creation");
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

    // done map: mia node -> set of ipg nodes already mapped at this position
    final var done = new IdentityHashMap<Node, Set<Node>>();
    final var writeContext = new IdentityHashMap<StageOutput, MiaMapping.NodeContext>();
    final var ipgNodes = ipg.getNodes().collect(Collectors.toUnmodifiableSet());
    final var mapping = new MiaMapping(ipg);

    for (Stage stage : stages) {
      // sources, maps, sinks based on input/output types of the nodes
      var sources = stage.behavior().getNodes().filter(this::isSource)
          .collect(Collectors.toCollection(Sets::newIdentityHashSet));
      var maps = stage.behavior().getNodes().filter(this::isMap)
          .collect(Collectors.toCollection(Sets::newIdentityHashSet));
      var sinks = stage.behavior().getNodes().filter(this::isSink)
          .collect(Collectors.toCollection(Sets::newIdentityHashSet));
      if (sources.isEmpty() && maps.isEmpty() && sinks.isEmpty()) {
        continue;
      }

      // load source done sets via stage output reference
      for (Node source : sources) {
        if (source instanceof ReadStageOutputNode read) {
          var prevContext = writeContext.get(read.stageOutput());
          if (prevContext == null || !done.containsKey(prevContext.node())) {
            throw new PassError("Could not load instruction progress at %s in stage %s",
                read, stage);
          }
          var prevDone = done.get(prevContext.node());
          done.put(source, prevDone);
        } else {
          // sources must be in the done map in any case, because traversal checks
          // if inputs already done
          done.put(source, new HashSet<>());
        }
      }

      // travers map nodes depth-first. only process nodes, if all inputs were processed earlier.
      var matcher = new MiaBuiltInCallMatcher();
      var q = new ArrayDeque<Node>();
      sources.forEach(source -> {
        if (maps.contains(source)) {
          q.add(source);
        } else {
          source.usages().filter(maps::contains).forEach(q::addLast);
        }
      });
      while (!q.isEmpty()) {
        var mapNode = q.removeFirst();
        var mapNodeInputs = mapNode.inputs().filter(this::instructionNode).toList();

        // combine done nodes from inputs
        var inputDoneSets = mapNode.inputs().map(done::get).filter(Objects::nonNull).toList();
        if (inputDoneSets.size() < mapNodeInputs.size()) {
          // skip if not all inputs are processed yet,
          // only process node after the last input adds it to the queue
          continue;
        }
        var inputDone = inputDoneSets.stream().flatMap(Collection::stream)
            .collect(Collectors.toSet());

        Set<Node> matched = Collections.emptySet();
        if (mapNode instanceof MiaBuiltInCall miaCall) {
          // use matcher
          matched = matcher.match(miaCall, ipgNodes, inputDone);
        } else {
          throw new PassError("Could not map node handling instructions %s in stage %s",
              mapNode, stage);
        }

        // grow matches up until nodes already done at inputs
        var mapped = new HashSet<Node>();
        matched.forEach(match -> growInputs(match, mapped, inputDone));

        // mark nodes in ipg
        var inputContexts = mapNodeInputs.stream()
            .map(input -> {
              if (isMap(input)) {
                return mapping.contexts().get(input);
              }
              if (input instanceof ReadStageOutputNode read) {
                return writeContext.get(read.stageOutput());
              }
              return null;
            })
            .filter(Objects::nonNull).collect(Collectors.toList());
        var context = mapping.createContext(stage, mapNode, inputContexts);
        var fixed = new HashSet<>(matched);
        fixed.removeIf(inputDone::contains);
        context.fixedIpgNodes().addAll(fixed);
        context.ipgNodes().addAll(mapped);

        // save done set for this node
        inputDone.addAll(mapped);
        done.put(mapNode, inputDone);

        // add map node usages to queue
        mapNode.usages().filter(maps::contains).forEach(q::addLast);
      }

      // check if all mapped
      if (!maps.stream().allMatch(done::containsKey)) {
        throw new PassError("Could not map all nodes handling the instruction in stage %s", stage);
      }

      // save write contexts to map at sinks
      for (Node sink : sinks) {
        if (sink instanceof WriteStageOutputNode write) {
          var valueContext = mapping.contexts().get(write.value());
          if (valueContext == null) {
            throw new PassError("Could not load mapping context at %s in stage %s",
                write, stage);
          }
          setSideEffect(valueContext, write); // set side effect reference to all inputs
          var output = write.stageOutput();
          if (output != null) {
            writeContext.put(write.stageOutput(), valueContext);
          }
        }
      }
    }

    // attach mapping to mia
    optMia.get().attachExtension(mapping);

    return mapping;
  }

  // expression node with instruction output
  private boolean instructionNode(Node node) {
    if (node instanceof ExpressionNode expr) {
      return expr.type().isTrivialCastTo(MicroArchitectureType.instruction());
    }
    return false;
  }

  // expression node with instruction output and _no_ instruction input
  private boolean isSource(Node node) {
    if (node instanceof ExpressionNode expr) {
      return (expr.inputs().noneMatch(this::instructionNode) && instructionNode(expr));
    }
    return false;
  }

  // expression node with instruction output _and_ instruction inputs
  private boolean isMap(Node node) {
    if (node instanceof MiaBuiltInCall miaCall && miaCall.builtIn() == BuiltInTable.DECODE) {
      return true;
    }
    if (node instanceof ExpressionNode expr) {
      return (expr.inputs().anyMatch(this::instructionNode) && instructionNode(expr));
    }
    return false;
  }

  // node with instruction inputs and _no_ instruction outputs
  private boolean isSink(Node node) {
    return node.inputs().anyMatch(this::instructionNode)
        && node.usages().noneMatch(this::instructionNode);
  }

  // recursively grow result nodes set up until (and excluding) the limit set of nodes
  private void growInputs(Node node, Set<Node> result, Set<Node> limit) {
    if (limit.contains(node)) {
      return;
    }
    result.add(node);
    node.inputs().forEach(n -> growInputs(n, result, limit));
  }

  private void setSideEffect(MiaMapping.NodeContext context, SideEffectNode node) {
    context.sideEffects().add(node);
    // set side effects on all predecessor contexts still in the same stage
    context.pred().forEach(pred -> {
      if (context.stage().equals(pred.stage())) {
        setSideEffect(pred, node);
      }
    });
  }
}
