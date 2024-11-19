package vadl.iss.passes;

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
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vadl.configuration.GeneralConfiguration;
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
import vadl.viam.graph.dependency.ReadResourceNode;
import vadl.viam.graph.dependency.WriteResourceNode;

public class IssSecureResourceReadPass extends Pass {

  public record Result(
      Map<ReadResourceNode, ControlNode> readTempSpillLocations
  ) {
  }

  public IssSecureResourceReadPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("Iss Secure Resource Read Pass");
  }

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

class IssResourceReadSecurer {
  private static final Logger log = LoggerFactory.getLogger(IssResourceReadSecurer.class);
  Instruction instruction;
  IssSecureResourceReadPass.Result result;

  Map<ControlNode, List<ControlNode>> dominatorSets;

  IssResourceReadSecurer(Instruction instruction, IssSecureResourceReadPass.Result result) {
    this.instruction = instruction;
    this.result = result;
    dominatorSets = IssDominatorAnalysis.getDominatorSets(instruction.behavior());
  }

  void run() {
    var readResources = instruction.readResources();

    for (var resource : readResources) {
      handleReadResource(resource);
    }
  }

  private void handleReadResource(Resource resource) {
    // TODO: We can skip the copy read temp scheduling if:
    //  - The write schedules are in non-overlapping branches

    var writeSchedules = instruction.behavior().getNodes(WriteResourceNode.class)
        .filter(wn -> wn.resourceDefinition() == resource)
        .map(wn -> wn.usages().filter(ScheduledNode.class::isInstance)
            .map(ScheduledNode.class::cast)
            .findAny().get())
        .toList();

    var reads = instruction.behavior().getNodes(ReadResourceNode.class)
        .filter(wn -> wn.resourceDefinition() == resource)
        .toList();

    // create set of conflicting nodes.
    var conflictNodes = new HashSet<ControlNode>();
    for (var read : reads) {
      // for all reads to the resource, consider all control node usages of this to be conflicting
      findAllControlUsagesOf(read)
          // ignore abstract end-node as we already scheduled side effects
          .filter(n -> !(n instanceof AbstractEndNode))
          .forEach(conflictNodes::add);
    }

    // until now the, only nodes in the conflict sets are control nodes that use
    // the resource by reading from it.
    // if all of those control nodes are dominators of all write schedules,
    // they are no conflicting sources.
    // Note: This method will not work in every case but in most cases.
    //  this is because the dominator set does only define definite dominators, not potential ones.
    var allDominators = true;
    for (var conflictNode : conflictNodes) {
      if (writeSchedules.contains(conflictNode)) {
        // conflicting writeSchedules are determined later
        continue;
      }
      for (var writeSchedule : writeSchedules) {
        var writeDominators = requireNonNull(dominatorSets.get(writeSchedule));
        if (!writeDominators.contains(conflictNode)) {
          allDominators = false;
          break;
        }
      }
    }

    if (allDominators) {
      // all conflicting nodes until now are not really in conflict with the writes.
      conflictNodes.clear();
    }

    // add scheduled writes as those are definitely potential conflicts
    conflictNodes.addAll(writeSchedules);

    if (conflictNodes.size() <= 1) {
      // there is no conflict, we don't have to schedule a copy
      return;
    }

    var latestCommon = findLatestCommonNode(conflictNodes, dominatorSets);

    for (var read : reads) {
      // set spill location for conflicting read resources
      result.readTempSpillLocations().put(read, latestCommon);
    }

    // TODO: Actual spilling
  }

  private static Stream<ControlNode> findAllControlUsagesOf(DependencyNode dependencyNode) {
    var s1 = dependencyNode.usages()
        .filter(DependencyNode.class::isInstance)
        .flatMap(u -> findAllControlUsagesOf((DependencyNode) u));
    var s2 = dependencyNode.usages()
        .filter(ControlNode.class::isInstance)
        .map(ControlNode.class::cast);
    return Stream.concat(s1, s2);
  }


  /*
   * Finds the control node that is the latest common dominator of all nodes in the given
   * set.
   */
  private static ControlNode findLatestCommonNode(Set<ControlNode> set,
                                                  Map<ControlNode, List<ControlNode>> dominatorSets) {

    var domSets = new ArrayList<List<ControlNode>>();
    for (ControlNode node : set) {
      domSets.add(dominatorSets.get(node));
    }

    // start is always common
    var lastCommon = domSets.get(0).get(0);

    // remove first from all (as first is always the start node)
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

class IssDominatorAnalysis {

  Graph cfg;
  Map<ControlNode, List<ControlNode>> dominatorSets = new HashMap<>();

  // the current dominators during traversal
  List<ControlNode> dominators = new ArrayList<>();

  public IssDominatorAnalysis(Graph cfg) {
    this.cfg = cfg;
  }

  static Map<ControlNode, List<ControlNode>> getDominatorSets(Graph cfg) {
    var analysis = new IssDominatorAnalysis(cfg);
    analysis.computeDominatorSets();
    return analysis.dominatorSets;
  }

  private void computeDominatorSets() {

    var start = getSingleNode(cfg, StartNode.class);
    var splitDominatorIndexStack = new ArrayDeque<Integer>();

    traverse(start, n -> {
      if (n instanceof ControlSplitNode splitNode) {
        // push index of splitNode
        splitDominatorIndexStack.push(dominators.size() - 1);
      } else if (n instanceof BranchEndNode || n instanceof MergeNode) {
        // reset sub branch dominators
        dominators = dominators.subList(0, requireNonNull(splitDominatorIndexStack.peek()) + 1);
      }

      // at itself to dominator list
      dominators.add(n);

      // copy to dominator sets
      dominatorSets.put(n, new ArrayList<>(dominators));

    });
  }


  AbstractEndNode traverse(AbstractBeginNode branchBegin, Consumer<ControlNode> nodeHandler) {
    ControlNode currNode = branchBegin;

    while (true) {
      nodeHandler.accept(currNode);

      if (currNode instanceof AbstractEndNode) {
        // when we found the end node, we return it
        return (AbstractEndNode) currNode;
      } else if (currNode instanceof DirectionalNode direNode) {
        currNode = direNode.next();
      } else if (currNode instanceof ControlSplitNode splitNode) {
        // handle all branches of the nested control split node
        currNode = handleControlSplit(splitNode, nodeHandler);
      } else {
        currNode.ensure(false,
            "Expected directional or control split node, but got this node in CFG."
        );
      }
    }
  }

  /*
   * Process all branches of the control split. It will return the control split's MergeNode.
   */
  private MergeNode handleControlSplit(ControlSplitNode splitNode,
                                       Consumer<ControlNode> nodeHandler) {
    @Nullable AbstractEndNode someEnd = null;
    for (var branch : splitNode.branches()) {
      someEnd = traverse(branch, nodeHandler);
    }
    splitNode.ensure(someEnd != null, "Control split has no branches.");
    splitNode.ensure(someEnd.usageCount() == 1, "End should have exactly one usage: MergeNode");
    // get the merge node from the end of the branch
    return (MergeNode) someEnd.usages().findFirst().get();
  }

}

