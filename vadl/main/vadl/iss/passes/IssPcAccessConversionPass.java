package vadl.iss.passes;

import java.io.IOException;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.iss.passes.nodes.IssStaticPcRegNode;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.utils.GraphUtils;
import vadl.viam.Specification;
import vadl.viam.graph.Graph;
import vadl.viam.graph.control.ScheduledNode;
import vadl.viam.graph.dependency.ReadRegNode;
import vadl.viam.graph.dependency.ReadResourceNode;
import vadl.viam.passes.sideEffectScheduling.nodes.InstrExitNode;

/**
 * Determines if a {@link InstrExitNode instr exit} can be done with a statically known
 * PC (e.g. because of a jump to current PC + 4 which is known from the {@code DisasContext})
 * or if it is dependent of a statically unknown register (e.g. in {@code JALR} where the
 * new PC is read from a register in X).
 * If the latter is the case, the PC write must be scheduled, otherwise not.
 *
 * <p>Additionally, the pass converts all PC reads into {@link IssStaticPcRegNode}s, so
 * they are not scheduled in the succeeding {@link IssTcgSchedulingPass}.</p>
 */
public class IssPcAccessConversionPass extends Pass {

  public IssPcAccessConversionPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("ISS PC Access Conversion");
  }

  @Override
  public @Nullable Object execute(PassResults passResults, Specification viam)
      throws IOException {

    viam.isa().ifPresent(isa -> {
      isa.ownInstructions().forEach(i -> new IssPcAccessConverter(i.behavior()).run());
    });
    return null;
  }
}


class IssPcAccessConverter {

  Graph graph;

  public IssPcAccessConverter(Graph graph) {
    this.graph = graph;
  }

  void run() {

    // replace read reg nodes of pcs to be just a CpuReg access of the ISS (No tcg op required)
    graph.getNodes(ReadRegNode.class)
        .filter(ReadRegNode::isPcAccess)
        .forEach(n -> n.replaceAndDelete(new IssStaticPcRegNode(n.register())));

    // handle the instr exits
    graph.getNodes(InstrExitNode.class)
        .forEach(this::handleInstrExit);
  }

  private void handleInstrExit(InstrExitNode node) {
    // if a pc write only uses nodes that are known at compile time (of TCG)
    // we potentially don't need a TCG operation
    // (we would produce gen_goto_tb())
    var targetPcStaticallyKnown =
        !GraphUtils.hasDependencies(node.pcWrite().value(), n -> n instanceof ReadResourceNode);

    // determine if the write is already scheduled
    var alreadyScheduled = GraphUtils.hasUser(node.pcWrite(), n -> n instanceof ScheduledNode);

    if (!alreadyScheduled && !targetPcStaticallyKnown) {
      // if the target is not statically known, we must schedule the PC write
      // before the instr exit
      node.addBefore(new ScheduledNode(node.pcWrite()));
    }
  }


}