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
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
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
import vadl.utils.Pair;
import vadl.viam.MicroArchitecture;
import vadl.viam.Specification;
import vadl.viam.Stage;
import vadl.viam.StageOutput;
import vadl.viam.ViamError;
import vadl.viam.graph.Node;
import vadl.viam.graph.ViamGraphError;
import vadl.viam.graph.control.AbstractEndNode;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.LetNode;
import vadl.viam.graph.dependency.ReadStageOutputNode;
import vadl.viam.graph.dependency.SideEffectNode;
import vadl.viam.graph.dependency.StageEffectNode;
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
    var optIsa = viam.mia().map(MicroArchitecture::isa);
    var optMia = viam.mia();
    if (optIsa.isEmpty() || optMia.isEmpty()) {
      return null;
    }

    var ipg = optIsa.get().expectExtension(InstructionProgressGraphExtension.class).ipg();
    var stages = optMia.get().stages();

    // done map: mia node -> set of ipg nodes already mapped at this position
    final var done = new HashMap<Node, Set<Node>>();
    final var writeContext = new HashMap<StageOutput, MiaMapping.NodeContext>();
    final var ipgNodes = ipg.getNodes().collect(Collectors.toCollection(LinkedHashSet::new));
    final var mapping = new MiaMapping(optMia.get(), ipg);

    for (Stage stage : stages) {

      // integrate stage effects into the data-flow
      // the mia built-in calls attached to stage effects replace the output of their input node
      stage.behavior().getNodes(AbstractEndNode.class).forEach(endNode -> {
        endNode.sideEffects().forEach(sideEffect -> {
          if (sideEffect instanceof StageEffectNode stageEffect) {
            var call = stageEffect.miaCall();
            var inputs = call.inputs().toList();

            ViamGraphError.ensure(inputs.size() == 1, stage.behavior(), call,
                "MiA bult-in call can only have one input");

            // replace usages, keep stage effect usages
            var stageEffectUsages = inputs.getFirst().usages()
                .filter(StageEffectNode.class::isInstance).toList();
            inputs.getFirst().replaceAtAllUsages(call);
            stageEffectUsages.forEach(stageEffectUsage ->
                stageEffectUsage.replaceInput(call, inputs.getFirst()));
          }
        });
      });

      // sources, maps, sinks based on input/output types of the nodes
      var sources = stage.behavior().getNodes().filter(this::isSource)
          .collect(Collectors.toCollection(LinkedHashSet::new));
      var maps = stage.behavior().getNodes().filter(this::isMap)
          .collect(Collectors.toCollection(LinkedHashSet::new));
      var sinks = stage.behavior().getNodes().filter(this::isSink)
          .collect(Collectors.toCollection(LinkedHashSet::new));
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
          done.put(source, new LinkedHashSet<>());
        }
      }

      // traverse map nodes depth-first. only process nodes, if all inputs were processed earlier.
      var q = new ArrayDeque<Node>();
      sources.forEach(source -> {
        if (maps.contains(source)) {
          q.add(source);
        } else {
          resolveUsages(source).filter(maps::contains).forEach(q::addLast);
        }
      });
      while (!q.isEmpty()) {
        var mapNode = q.removeFirst();
        var mapNodeInputs = resolveInputs(mapNode).filter(this::instructionNode).toList();

        // combine done nodes from inputs
        var inputDoneSets = resolveInputs(mapNode).map(done::get).filter(Objects::nonNull).toList();
        if (inputDoneSets.size() < mapNodeInputs.size()) {
          // skip if not all inputs are processed yet,
          // only process node after the last input adds it to the queue
          continue;
        }
        var inputDone = inputDoneSets.stream().flatMap(Collection::stream)
            .collect(Collectors.toSet());

        // map
        var matchedMapped = mapNodes(mapNode, ipgNodes, inputDone, stage);

        // mark nodes in ipg
        var inputContexts = getInputContexts(mapNodeInputs, mapping, writeContext);
        var context = mapping.createContext(stage, mapNode, inputContexts);
        context.fixedIpgNodes().addAll(matchedMapped.left());
        context.ipgNodes().addAll(matchedMapped.right());
        for (Node usage : mapNode.usages().toList()) {
          if (usage instanceof StageEffectNode stageEffect) {
            context.sideEffects().add(stageEffect);
          }
        }

        // save done set for this node
        inputDone.addAll(matchedMapped.right());
        done.put(mapNode, inputDone);

        // add map node usages to queue
        resolveUsages(mapNode).filter(maps::contains).forEach(q::addLast);
      }

      // check if all mapped
      if (!maps.stream().allMatch(done::containsKey)) {
        throw new PassError("Could not map all nodes handling the instruction in stage %s", stage);
      }

      // save write contexts to map at sinks
      for (Node sink : sinks) {
        if (sink instanceof WriteStageOutputNode write) {
          var inputs = resolveInputs(write).filter(n -> isMap(n) || isSource(n)).toList();
          if (inputs.isEmpty()) {
            throw new PassError("Could not resolve input node at %s in stage %s",
                write, stage);
          }
          var valueContexts = getInputContexts(inputs, mapping, writeContext);
          if (valueContexts.isEmpty()) {
            throw new PassError("Could not load mapping context at %s in stage %s",
                write, stage);
          }
          setSideEffect(valueContexts.getFirst(), write); // set side effect reference to all inputs
          var output = write.stageOutput();
          if (output != null) {
            writeContext.put(write.stageOutput(), valueContexts.getFirst());
          }
        }
      }
    }

    // check mapping
    ViamError.ensure(ipg.getNodes().allMatch(n -> mapping.findContext(n).isPresent()),
        "Not all nodes mapped in MiA mapping creation");

    // attach mapping to mia
    optMia.get().attachExtension(mapping);

    return mapping;
  }

  @Nonnull
  private List<MiaMapping.NodeContext> getInputContexts(
      List<Node> mapNodeInputs,
      MiaMapping mapping,
      Map<StageOutput, MiaMapping.NodeContext> writeContext
  ) {
    return mapNodeInputs.stream()
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
  }

  private Pair<Set<Node>, Set<Node>> mapNodes(Node mapNode, Set<Node> ipgNodes, Set<Node> inputDone,
                                              Stage stage) {
    var matcher = new MiaBuiltInCallMatcher();

    Set<Node> matched;
    if (mapNode instanceof BuiltInCall call) {
      // use matcher
      matched = matcher.match(call, ipgNodes, inputDone);
      matched.removeAll(inputDone);
    } else {
      throw new PassError("Could not map node handling instructions %s in stage %s",
          mapNode, stage);
    }

    // grow matches up until nodes already done at inputs
    var mapped = new LinkedHashSet<Node>();
    matched.forEach(match -> growInputs(match, mapped, inputDone));
    return Pair.of(matched, mapped);
  }

  // expression node with instruction output
  private boolean instructionNode(Node node) {
    if (node instanceof ExpressionNode expr) {
      return expr.type().isTrivialCastTo(MicroArchitectureType.instruction())
          || expr.type().isTrivialCastTo(MicroArchitectureType.fetchResult());
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

  // resolve usages stepping over let nodes
  private Stream<Node> resolveUsages(Node node) {
    return node.usages().flatMap(u -> {
      if (u instanceof LetNode let) {
        return resolveUsages(let);
      }
      return Stream.of(u);
    });
  }

  // resolve inputs stepping over let nodes
  private Stream<Node> resolveInputs(Node node) {
    return node.inputs().flatMap(i -> {
      if (i instanceof LetNode let) {
        return resolveInputs(let);
      }
      return Stream.of(i);
    });
  }

  // expression node with instruction output _and_ instruction inputs
  private boolean isMap(Node node) {
    if (node instanceof BuiltInCall call
        && Set.of(BuiltInTable.DECODE, BuiltInTable.FETCH_NEXT).contains(call.builtIn())) {
      return true;
    }
    if (node instanceof LetNode) {
      return false;
    }
    if (node instanceof ExpressionNode expr) {
      return (expr.inputs().anyMatch(this::instructionNode) && instructionNode(expr));
    }
    return false;
  }

  // node with instruction inputs and _no_ instruction outputs
  private boolean isSink(Node node) {
    return node.inputs().anyMatch(this::instructionNode) && !instructionNode(node);
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
    if (context.sideEffects().isEmpty()) {
      context.sideEffects().add(node);
    }
    // set side effects on all predecessor contexts still in the same stage
    context.pred().forEach(pred -> {
      if (context.stage().equals(pred.stage())) {
        setSideEffect(pred, node);
      }
    });
  }
}
