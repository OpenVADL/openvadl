// SPDX-FileCopyrightText : © 2025-2026 TU Wien <vadl@tuwien.ac.at>
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

package vadl.iss.passes.common.safeResourceRead;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import vadl.configuration.IssConfiguration;
import vadl.iss.passes.AbstractIssPass;
import vadl.iss.passes.common.safeResourceRead.nodes.ExprSaveNode;
import vadl.iss.passes.nodes.IssReadRegNode;
import vadl.iss.passes.nodes.IssWriteRegNode;
import vadl.iss.passes.utils.IssDominatorAnalysis;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Instruction;
import vadl.viam.Resource;
import vadl.viam.Specification;
import vadl.viam.graph.Node;
import vadl.viam.graph.control.AbstractBeginNode;
import vadl.viam.graph.control.AbstractEndNode;
import vadl.viam.graph.control.ControlNode;
import vadl.viam.graph.control.DirectionalNode;
import vadl.viam.graph.control.ScheduledNode;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.DependencyNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.ReadResourceNode;
import vadl.viam.graph.dependency.WriteResourceNode;
import vadl.viam.passes.sideEffectScheduling.nodes.InstrExitNode;

/**
 * A pass that ensures safe resource reads in the Instruction Set Simulator (ISS).
 * This pass modifies the instruction behaviors to prevent unsafe concurrent
 * resource reads and writes.
 *
 * <p>From paper: To ensure that writes do not occur before reads to the same resource,
 * potentially conflicting reads must be scheduled before any writes to that resource.
 * Since register file indices and memory addresses are not statically known,
 * all reads to these resources must be conservatively treated as potential conflicts
 * with all writes to the same resource.</p>
 *
 * <p>The analysis intentionally uses post-lowering resource accesses so alias accesses are checked
 * against their effective base resources.
 */
public class IssSafeResourceReadPass extends AbstractIssPass {

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
  public IssSafeResourceReadPass(IssConfiguration configuration) {
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
    tcgInstrs(viam).forEach(
        instruction -> new IssResourceReadSecurer(instruction, result).run());
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

  IssDominatorAnalysis dominatorAnalysis;

  /**
   * Constructs an IssResourceReadSecurer for the given instruction and pass result.
   *
   * @param instruction The instruction to secure resource reads in.
   * @param result      The result object to store spill locations.
   */
  IssResourceReadSecurer(Instruction instruction, IssSafeResourceReadPass.Result result) {
    this.instruction = instruction;
    this.result = result;
    dominatorAnalysis = IssDominatorAnalysis.analyze(instruction.behavior());
  }

  /**
   * Runs the resource read securer on the instruction.
   */
  void run() {
    readResources().forEach(resource -> {
      for (var group : saveGroupsFor(resource)) {
        for (var read : group.reads()) {
          result.readTempSpillLocations().put(read, group.location());
          saveResourceRead(read, group.location());
        }
      }
    });
  }

  /**
   * Determines whether securing this instruction would require at least one saved resource read.
   *
   * <p>This is intentionally a dry-run variant of {@link #run()} so analyses can ask the same
   * question without mutating the graph.</p>
   */
  boolean requiresReadSave() {
    return readResources().anyMatch(resource -> !saveGroupsFor(resource).isEmpty());
  }

  private Stream<Resource> readResources() {
    // Use post-lowering graph resources instead of Instruction.readResources() cache,
    // because alias lowering rewrites accesses to their effective base resources.
    return instruction.behavior().getNodes(ReadResourceNode.class)
        .map(ReadResourceNode::resourceDefinition)
        .collect(Collectors.toSet())
        .stream();
  }

  /**
   * Returns the save groups needed to secure reads of {@code resource}. Each group binds a
   * set of reads to a single control node at which they must be spilled. An empty list means
   * no save is needed for this resource.
   *
   * <p>When every access on {@code resource} is an {@link IssReadRegNode} /
   * {@link IssWriteRegNode} with a constant CHUNK window and all windows are pairwise
   * non-overlapping, accesses are partitioned per window and the conflict analysis is run
   * independently per partition. This is precise enough to recognize unrolled per-element
   * tensor writes where iteration {@code i} only reads and writes its own chunk offset —
   * a pattern the conservative whole-resource analysis would falsely flag as requiring saves.
   */
  private List<SaveGroup> saveGroupsFor(Resource resource) {
    var allReads = instruction.behavior().getNodes(ReadResourceNode.class)
        .filter(wn -> wn.resourceDefinition() == resource)
        .toList();
    var allWrites = instruction.behavior().getNodes(WriteResourceNode.class)
        .filter(wn -> wn.resourceDefinition() == resource)
        .toList();

    var partition = tryPartitionByChunkWindow(allReads, allWrites);
    if (partition != null) {
      var groups = new ArrayList<SaveGroup>();
      for (var bucket : partition.values()) {
        var loc = determineIfReadSaveIsRequired(bucket.reads, writeSchedulesOf(bucket.writes));
        if (loc != null) {
          groups.add(new SaveGroup(bucket.reads, loc));
        }
      }
      return groups;
    }

    var loc = determineIfReadSaveIsRequired(allReads, writeSchedulesOf(allWrites));
    if (loc == null) {
      return List.of();
    }
    return List.of(new SaveGroup(allReads, loc));
  }

  private static List<ControlNode> writeSchedulesOf(List<? extends WriteResourceNode> writes) {
    return writes.stream()
        .map(wn -> wn.usages()
            // if the write is scheduled or used by an instr exit
            .filter(u -> u instanceof ScheduledNode || u instanceof InstrExitNode)
            .map(ControlNode.class::cast)
            .findAny().get())
        .toList();
  }

  /**
   * Partitions reads and writes by their constant CHUNK window. Returns {@code null} if any
   * access is not a constant CHUNK, or if any two distinct windows overlap partially. When
   * non-null, every returned bucket describes a bit range that is disjoint from every other
   * bucket's range, so the conflict analysis can treat them as independent sub-resources.
   */
  private static @Nullable Map<ChunkWindow, ChunkBucket> tryPartitionByChunkWindow(
      List<ReadResourceNode> reads, List<WriteResourceNode> writes) {
    var buckets = new HashMap<ChunkWindow, ChunkBucket>();
    for (var r : reads) {
      var w = chunkWindowOf(r);
      if (w == null) {
        return null;
      }
      buckets.computeIfAbsent(w, k -> new ChunkBucket()).reads.add(r);
    }
    for (var wr : writes) {
      var w = chunkWindowOf(wr);
      if (w == null) {
        return null;
      }
      buckets.computeIfAbsent(w, k -> new ChunkBucket()).writes.add(wr);
    }
    // Require pairwise non-overlap (equal windows share a bucket and are fine).
    var keys = new ArrayList<>(buckets.keySet());
    for (int i = 0; i < keys.size(); i++) {
      for (int j = i + 1; j < keys.size(); j++) {
        if (partiallyOverlaps(keys.get(i), keys.get(j))) {
          return null;
        }
      }
    }
    return buckets;
  }

  @SuppressWarnings("LocalVariableName")
  private static boolean partiallyOverlaps(ChunkWindow a, ChunkWindow b) {
    long aLo = a.offset();
    long aHi = aLo + a.width();
    long bLo = b.offset();
    long bHi = bLo + b.width();
    return aHi > bLo && bHi > aLo;
  }

  private static @Nullable ChunkWindow chunkWindowOf(Node n) {
    if (n instanceof IssReadRegNode r && r.windowKind() == IssReadRegNode.WindowKind.CHUNK) {
      return windowFromNodes(r.bitOffset(), r.bitWidth());
    }
    if (n instanceof IssWriteRegNode w && w.windowKind() == IssWriteRegNode.WindowKind.CHUNK) {
      return windowFromNodes(w.bitOffset(), w.bitWidth());
    }
    return null;
  }

  private static @Nullable ChunkWindow windowFromNodes(ExpressionNode offset,
                                                       ExpressionNode width) {
    if (offset instanceof ConstantNode co && width instanceof ConstantNode cw) {
      return new ChunkWindow(
          co.constant().asVal().intValue(),
          cw.constant().asVal().intValue());
    }
    return null;
  }

  private record ChunkWindow(int offset, int width) {
  }

  private static final class ChunkBucket {
    final List<ReadResourceNode> reads = new ArrayList<>();
    final List<WriteResourceNode> writes = new ArrayList<>();
  }

  private record SaveGroup(List<ReadResourceNode> reads, ControlNode location) {
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
        var writeDominators = dominatorAnalysis.dominatorsOf(writeSchedule);
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

    return dominatorAnalysis.latestCommonDominator(conflictNodes);

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

}
