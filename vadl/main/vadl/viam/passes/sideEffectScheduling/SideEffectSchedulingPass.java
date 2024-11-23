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

public class SideEffectSchedulingPass extends Pass {

  public SideEffectSchedulingPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("Side Effect Scheduling Pass");
  }

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


class SideEffectScheduler {

  @Nullable
  Counter.RegisterCounter pc;


  public static void run(Instruction instr, @Nullable Counter.RegisterCounter pc) {
    var startNode = getSingleNode(instr.behavior(), StartNode.class);
    var scheduler = new SideEffectScheduler();
    scheduler.pc = pc;
    scheduler.processBranch(startNode);
  }


  private AbstractEndNode processBranch(AbstractBeginNode beginNode) {
    // process until the corresponding end node of branch
    var endNode = traverseUntilMatchingBranchEnd(beginNode);

    var pcReg = pc != null ? pc.registerRef() : null;
    var partitionedEffects = endNode.sideEffects().stream()
        .collect(Collectors.partitioningBy(
            s -> s.resourceDefinition().equals(pcReg)
        ));

    var nonPcUpdateEffects = partitionedEffects.getOrDefault(false, List.of());
    var pcSideEffect = partitionedEffects.getOrDefault(true, List.of())
        .stream().findFirst();

    // all non pc updates should be inserted directly at the begin of the branch
    for (var effect : nonPcUpdateEffects) {
      beginNode.addAfter(new ScheduledNode(effect));
    }

    // add pc update directly in front of branch end
    pcSideEffect.ifPresent(pcUpdate ->
        endNode.addBefore(new InstrExitNode((WriteResourceNode) pcUpdate))
    );

    return endNode;
  }

  private AbstractEndNode traverseUntilMatchingBranchEnd(AbstractBeginNode beginNode) {

    ControlNode currNode = beginNode;

    while (true) {
      // skip all directional nodes
      currNode = skipDirectionals(currNode);

      if (currNode instanceof AbstractEndNode) {
        // when we found the end node, we return it
        return (AbstractEndNode) currNode;

      } else if (currNode instanceof ControlSplitNode splitNode) {
        // handle all branches of the nested control split node
        currNode = handleControlSplit(splitNode);

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
  private MergeNode handleControlSplit(ControlSplitNode splitNode) {
    @Nullable AbstractEndNode someEnd = null;
    for (var branch : splitNode.branches()) {
      someEnd = processBranch(branch);
    }
    splitNode.ensure(someEnd != null, "Control split has no branches.");
    splitNode.ensure(someEnd.usageCount() == 1, "End should have exactly one usage: MergeNode");
    // get the merge node from the end of the branch
    return (MergeNode) someEnd.usages().findFirst().get();
  }

  private ControlNode skipDirectionals(ControlNode node) {
    while (node instanceof DirectionalNode dirNode) {
      node = dirNode.next();
    }
    return node;
  }

}
