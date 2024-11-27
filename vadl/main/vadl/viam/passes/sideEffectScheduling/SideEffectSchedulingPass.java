package vadl.viam.passes.sideEffectScheduling;

import static vadl.utils.GraphUtils.getSingleNode;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import org.jetbrains.annotations.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Counter;
import vadl.viam.Instruction;
import vadl.viam.Specification;
import vadl.viam.graph.control.AbstractBeginNode;
import vadl.viam.graph.control.AbstractEndNode;
import vadl.viam.graph.control.ControlNode;
import vadl.viam.graph.control.ControlSplitNode;
import vadl.viam.graph.control.DirectionalNode;
import vadl.viam.graph.control.MergeNode;
import vadl.viam.graph.control.ScheduledNode;
import vadl.viam.graph.control.StartNode;
import vadl.viam.graph.dependency.WriteResourceNode;
import vadl.viam.passes.sideEffectScheduling.nodes.InstrExitNode;

/**
 * A pass that schedules side effects within the control flow graph (CFG) of instructions.
 * It separates side effects that modify the program counter (PC) from other side effects.
 * Non-PC side effects are scheduled at the beginning of branches, while PC updates are scheduled
 * immediately before the branch ends.
 */
public class SideEffectSchedulingPass extends Pass {

  /**
   * Constructs a new {@code SideEffectSchedulingPass} with the given configuration.
   *
   * @param configuration The general configuration for this pass.
   */
  public SideEffectSchedulingPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  /**
   * Returns the name of the pass.
   *
   * @return The pass name "Side Effect Scheduling Pass".
   */
  @Override
  public PassName getName() {
    return PassName.of("Side Effect Scheduling Pass");
  }

  /**
   * Executes the side effect scheduling pass on the given specification.
   * It processes each instruction in the ISA and schedules side effects appropriately.
   *
   * @param passResults The results from previous passes.
   * @param viam        The VIAM specification.
   * @return {@code null}
   * @throws IOException If an I/O error occurs.
   */
  @Override
  public @Nullable Object execute(PassResults passResults, Specification viam)
      throws IOException {

    viam.isa().ifPresent(isa -> {
      var pc = isa.pc();
      isa.ensure(pc == null || pc instanceof Counter.RegisterCounter,
          "Only RegisterCounters are currently supported for this pass. Got: %s", pc);

      isa.ownInstructions().forEach(
          instruction -> SideEffectScheduler.run(instruction, (Counter.RegisterCounter) pc));
    });
    return null;
  }
}

/**
 * Schedules side effects in an instruction's control flow graph.
 * It separates PC updates from other side effects and schedules them at appropriate locations.
 */
class SideEffectScheduler {

  /**
   * The program counter register counter, if available.
   */
  @Nullable
  Counter.RegisterCounter pc;

  /**
   * Runs the side effect scheduling on the given instruction.
   *
   * @param instr The instruction to process.
   * @param pc    The program counter register counter, or {@code null} if not available.
   */
  public static void run(Instruction instr, @Nullable Counter.RegisterCounter pc) {
    var startNode = getSingleNode(instr.behavior(), StartNode.class);
    var scheduler = new SideEffectScheduler();
    scheduler.pc = pc;
    scheduler.processBranch(startNode);
  }

  /**
   * Processes a branch starting from the given begin node.
   * It schedules non-PC side effects at the beginning
   * of the branch and PC updates before the branch end.
   *
   * @param beginNode The starting node of the branch.
   * @return The corresponding end node of the branch.
   */
  private AbstractEndNode processBranch(AbstractBeginNode beginNode) {
    // Process until the corresponding end node of the branch
    var endNode = traverseUntilMatchingBranchEnd(beginNode);

    var pcReg = pc != null ? pc.registerRef() : null;
    var partitionedEffects = endNode.sideEffects().stream()
        .collect(Collectors.partitioningBy(
            s -> s.resourceDefinition().equals(pcReg)
        ));

    var nonPcUpdateEffects = partitionedEffects.getOrDefault(false, List.of());
    var pcSideEffect = partitionedEffects.getOrDefault(true, List.of())
        .stream().findFirst();

    // All non-PC updates should be inserted directly at the beginning of the branch
    for (var effect : nonPcUpdateEffects) {
      beginNode.addAfter(new ScheduledNode(effect));
    }

    // Add PC update directly in front of branch end
    pcSideEffect.ifPresent(pcUpdate ->
        endNode.addBefore(new InstrExitNode((WriteResourceNode) pcUpdate))
    );

    return endNode;
  }

  /**
   * Traverses the control flow graph until it finds the matching end node for the given begin node.
   *
   * @param beginNode The begin node to start traversal from.
   * @return The matching end node.
   */
  private AbstractEndNode traverseUntilMatchingBranchEnd(AbstractBeginNode beginNode) {

    ControlNode currNode = beginNode;

    while (true) {
      // Skip all directional nodes
      currNode = skipDirectionals(currNode);

      if (currNode instanceof AbstractEndNode) {
        // When we find the end node, we return it
        return (AbstractEndNode) currNode;

      } else if (currNode instanceof ControlSplitNode splitNode) {
        // Handle all branches of the nested control split node
        currNode = handleControlSplit(splitNode);

      } else {
        currNode.ensure(false,
            "Expected directional or control split node, but got this node in CFG."
        );
      }
    }
  }

  /**
   * Processes all branches of the control split node and returns the merge node.
   *
   * @param splitNode The control split node to process.
   * @return The merge node corresponding to the control split.
   */
  private MergeNode handleControlSplit(ControlSplitNode splitNode) {
    @Nullable AbstractEndNode someEnd = null;
    for (var branch : splitNode.branches()) {
      someEnd = processBranch(branch);
    }
    splitNode.ensure(someEnd != null, "Control split has no branches.");
    splitNode.ensure(someEnd.usageCount() == 1, "End should have exactly one usage: MergeNode");
    // Get the merge node from the end of the branch
    return (MergeNode) someEnd.usages().findFirst().get();
  }

  /**
   * Skips all directional nodes starting from the
   * given node and returns the next non-directional node.
   *
   * @param node The starting control node.
   * @return The next non-directional control node.
   */
  private ControlNode skipDirectionals(ControlNode node) {
    while (node instanceof DirectionalNode dirNode) {
      node = dirNode.next();
    }
    return node;
  }
}