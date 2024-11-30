package vadl.iss.passes.safeResourceRead;

import static java.util.Objects.requireNonNull;
import static vadl.utils.GraphUtils.getSingleNode;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.jetbrains.annotations.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.iss.passes.safeResourceRead.nodes.ExprSaveNode;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Instruction;
import vadl.viam.Resource;
import vadl.viam.Specification;
import vadl.viam.graph.Graph;
import vadl.viam.graph.control.AbstractBeginNode;
import vadl.viam.graph.control.AbstractEndNode;
import vadl.viam.graph.control.BranchEndNode;
import vadl.viam.graph.control.ControlNode;
import vadl.viam.graph.control.ControlSplitNode;
import vadl.viam.graph.control.DirectionalNode;
import vadl.viam.graph.control.MergeNode;
import vadl.viam.graph.control.ScheduledNode;
import vadl.viam.graph.control.StartNode;
import vadl.viam.graph.dependency.DependencyNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.ReadResourceNode;
import vadl.viam.graph.dependency.WriteResourceNode;
import vadl.viam.passes.CfgTraverser;
import vadl.viam.passes.sideEffectScheduling.nodes.InstrExitNode;

/**
 * A pass that ensures safe resource reads in the Instruction Set Simulator (ISS).
 * This pass modifies the instruction behaviors to prevent unsafe concurrent
 * resource reads and writes.
 */
public class IssSafeResourceReadPass extends Pass {

  /**
   * Result of the IssSafeResourceReadPass execution.
   *
   * @param readTempSpillLocations A map of ReadResourceNodes to their spill
   *                               locations in the control flow.
   */
  public record Result(
      Map<ReadResourceNode, ControlNode> readTempSpillLocations
  ) {
  }

  /**
   * Constructs an IssSafeResourceReadPass with the given configuration.
   *
   * @param configuration The general configuration for the pass.
   */
  public IssSafeResourceReadPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  /**
   * Returns the name of the pass.
   *
   * @return The PassName instance representing the name of this pass.
   */
  @Override
  public PassName getName() {
    return PassName.of("Iss Secure Resource Read Pass");
  }

  /**
   * Executes the pass on the given specification.
   *
   * @param passResults The results from previous passes.
   * @param viam        The specification to execute the pass on.
   * @return The result of the pass execution.
   * @throws IOException If an I/O error occurs.
   */
  @Override
  public Result execute(PassResults passResults, Specification viam)
      throws IOException {
    var result = new Result(new HashMap<>());
    viam.isa().ifPresent(
        isa -> isa.ownInstructions().forEach(
            instruction -> new IssResourceReadSecurer(instruction, result).run()));
    return result;
  }
}

/**
 * Secures resource reads in an instruction by ensuring that
 * resource reads do not conflict with writes.
 * If necessary, it schedules a copy of the read resource to
 * a temporary location to prevent conflicts.
 */
class IssResourceReadSecurer {
  Instruction instruction;
  IssSafeResourceReadPass.Result result;

  Map<ControlNode, List<ControlNode>> dominatorSets;

  /**
   * Constructs an IssResourceReadSecurer for the given instruction and pass result.
   *
   * @param instruction The instruction to secure resource reads in.
   * @param result      The result object to store spill locations.
   */
  IssResourceReadSecurer(Instruction instruction, IssSafeResourceReadPass.Result result) {
    this.instruction = instruction;
    this.result = result;
    dominatorSets = IssDominatorAnalysis.getDominatorSets(instruction.behavior());
  }

  /**
   * Runs the resource read securer on the instruction.
   */
  void run() {
    var readResources = instruction.readResources();

    for (var resource : readResources) {
      handleReadResource(resource);
    }
  }

  /**
   * Handles securing reads for a specific resource.
   *
   * @param resource The resource to secure reads for.
   */
  private void handleReadResource(Resource resource) {
    var writeSchedules = instruction.behavior().getNodes(WriteResourceNode.class)
        .filter(wn -> wn.resourceDefinition() == resource)
        .map(wn -> wn.usages()
            // if the write is scheduled or used by an instr exit
            .filter(u -> u instanceof ScheduledNode || u instanceof InstrExitNode)
            .map(ControlNode.class::cast)
            .findAny().get())
        .toList();

    var reads = instruction.behavior().getNodes(ReadResourceNode.class)
        .filter(wn -> wn.resourceDefinition() == resource)
        .toList();

    var saveLocation = determineIfReadSaveIsRequired(reads, writeSchedules);
    if (saveLocation == null) {
      // No read save required
      return;
    }

    for (var read : reads) {
      // Set spill location for conflicting read resources
      result.readTempSpillLocations().put(read, saveLocation);
      saveResourceRead(read, saveLocation);
    }
  }

  /**
   * Determines whether a read save is required by checking for conflicts between reads and writes.
   *
   * @param reads           The list of ReadResourceNodes for the resource.
   * @param scheduledWrites The list of ScheduledNodes for writes to the resource.
   * @return The ControlNode where the read should be saved, or null if no save is required.
   */
  private @Nullable ControlNode determineIfReadSaveIsRequired(List<ReadResourceNode> reads,
                                                              List<ControlNode> scheduledWrites) {
    // TODO: We can skip the copy read temp scheduling if:
    //  - The write schedules are in non-overlapping branches

    // Create a set of conflicting nodes.
    var conflictNodes = new HashSet<ControlNode>();
    for (var read : reads) {
      // For all reads to the resource, consider all control node usages of this to be conflicting
      findAllControlUsagesOf(read)
          // Ignore AbstractEndNode as we already scheduled side effects
          .filter(n -> !(n instanceof AbstractEndNode))
          .forEach(conflictNodes::add);
    }

    // Until now, only nodes in the conflict sets are control nodes that use
    // the resource by reading from it.
    // If all of those control nodes are dominators of all write schedules,
    // they are not conflicting sources.
    // Note: This method will not work in every case but in most cases,
    // because the dominator set only defines definite dominators, not potential ones.
    var allDominators = true;
    for (var conflictNode : conflictNodes) {
      if (scheduledWrites.contains(conflictNode)) {
        // Conflicting write schedules are determined later
        continue;
      }
      for (var writeSchedule : scheduledWrites) {
        var writeDominators = requireNonNull(dominatorSets.get(writeSchedule));
        if (!writeDominators.contains(conflictNode)) {
          allDominators = false;
          break;
        }
      }
    }

    if (allDominators) {
      // All conflicting nodes until now are not really in conflict with the writes.
      conflictNodes.clear();
    }

    // Add scheduled writes as those are definitely potential conflicts
    conflictNodes.addAll(scheduledWrites);

    if (conflictNodes.size() <= 1) {
      // There is no conflict; we don't have to schedule a copy
      return null;
    }

    return findLatestCommonNode(conflictNodes, dominatorSets);

  }

  /**
   * Schedules a save of the read resource at the specified control node location.
   *
   * @param resource The ReadResourceNode to save.
   * @param location The ControlNode where the save should be scheduled.
   */
  private void saveResourceRead(ReadResourceNode resource, ControlNode location) {
    // If location is a start node, we must spill after the node; otherwise, we spill before
    var spillAfter = location instanceof AbstractBeginNode;

    ExpressionNode saveNode = new ExprSaveNode(resource);
    saveNode = resource.replace(saveNode);
    var scheduledSaveNode = new ScheduledNode(saveNode);

    if (spillAfter) {
      ((AbstractBeginNode) location).addAfter(scheduledSaveNode);
    } else {
      // We assume that there are only directional nodes before the location's control node.
      // If this assumption is not correct, we have to fix this implementation
      location.ensure(location.predecessor() instanceof DirectionalNode,
          "Expected that location's predecessor is a directional node. But was %s",
          location.predecessor());

      var pred = (DirectionalNode) location.predecessor();
      pred.addAfter(scheduledSaveNode);
    }
  }

  /**
   * Finds all control node usages of a dependency node recursively.
   *
   * @param dependencyNode The dependency node to find usages of.
   * @return A stream of control nodes that use the dependency node.
   */
  private static Stream<ControlNode> findAllControlUsagesOf(DependencyNode dependencyNode) {
    var s1 = dependencyNode.usages()
        .filter(DependencyNode.class::isInstance)
        .flatMap(u -> findAllControlUsagesOf((DependencyNode) u));
    var s2 = dependencyNode.usages()
        .filter(ControlNode.class::isInstance)
        .map(ControlNode.class::cast);
    return Stream.concat(s1, s2);
  }

  /**
   * Finds the control node that is the latest common dominator of all nodes in the given set.
   *
   * @param set           The set of control nodes.
   * @param dominatorSets The map of dominator sets for each control node.
   * @return The control node that is the latest common dominator.
   */
  @SuppressWarnings("LineLength")
  private static ControlNode findLatestCommonNode(Set<ControlNode> set,
                                                  Map<ControlNode, List<ControlNode>> dominatorSets) {

    var domSets = new ArrayList<List<ControlNode>>();
    for (ControlNode node : set) {
      domSets.add(new ArrayList<>(dominatorSets.get(node)));
    }

    // Start node is always common
    var lastCommon = domSets.get(0).get(0);

    // Remove first from all (as first is always the start node)
    for (var s : domSets) {
      s.remove(0);
    }

    while (true) {
      @Nullable ControlNode nextCommon = null;

      for (var dominators : domSets) {
        if (dominators.isEmpty()) {
          return lastCommon;
        }

        var thisNext = dominators.remove(0);
        if (lastCommon != null && thisNext != lastCommon) {
          return lastCommon;
        }
        nextCommon = thisNext;
      }

      lastCommon = requireNonNull(nextCommon);
    }

  }
}

/**
 * Performs dominator analysis on a control flow graph (CFG)
 * to compute dominator sets for control nodes.
 */
class IssDominatorAnalysis implements CfgTraverser {

  Map<ControlNode, List<ControlNode>> dominatorSets = new HashMap<>();
  ArrayDeque<Integer> splitDominatorIndexStack = new ArrayDeque<>();

  // The current dominators during traversal
  List<ControlNode> dominators = new ArrayList<>();

  /**
   * Computes the dominator sets for the given control flow graph.
   *
   * @param cfg The control flow graph to compute dominator sets for.
   * @return A map of control nodes to their dominator sets.
   */
  static Map<ControlNode, List<ControlNode>> getDominatorSets(Graph cfg) {
    var analysis = new IssDominatorAnalysis();
    var start = getSingleNode(cfg, StartNode.class);
    analysis.traverseBranch(start);
    return analysis.dominatorSets;
  }

  @Override
  public void onControlNode(ControlNode n) {
    if (n instanceof ControlSplitNode) {
      // Push index of splitNode
      splitDominatorIndexStack.push(dominators.size() - 1);
    } else if (n instanceof BranchEndNode || n instanceof MergeNode) {
      // Reset sub-branch dominators
      dominators = dominators.subList(0, requireNonNull(splitDominatorIndexStack.peek()) + 1);
    }

    // Add itself to dominator list
    dominators.add(n);

    // Copy to dominator sets
    dominatorSets.put(n, new ArrayList<>(dominators));
  }
}