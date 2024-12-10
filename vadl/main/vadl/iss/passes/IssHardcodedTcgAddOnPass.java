package vadl.iss.passes;

import static vadl.utils.GraphUtils.getSingleNode;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import org.jetbrains.annotations.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.iss.passes.tcgLowering.nodes.TcgHelperCall;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.types.Type;
import vadl.utils.GraphUtils;
import vadl.viam.Instruction;
import vadl.viam.Specification;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.control.InstrEndNode;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.DependencyNode;

public class IssHardcodedTcgAddOnPass extends Pass {

  public IssHardcodedTcgAddOnPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("ISS Hardcoded TCG Add-Ons");
  }

  List<Consumer<Instruction>> instrAddOns = List.of(
      this::eCallAddExceptionRaise
  );

  @Override
  public @Nullable Object execute(PassResults passResults, Specification viam)
      throws IOException {

    viam.isa().ifPresent(isa ->
        isa.ownInstructions()
            .forEach(i ->
                instrAddOns.forEach(f -> f.accept(i))));
    
    return null;
  }


  /*
   * Add a tcg helper call to raise_exception in ecall. We do this as the current
   * specification does not yet implement it, however, it is necessary for tests.
   */
  private void eCallAddExceptionRaise(Instruction instr) {
    if (!instr.simpleName().equals("ECALL")) {
      return;
    }

    var instrEnd = getSingleNode(instr.behavior(), InstrEndNode.class);
    var M_CALL_EXP = GraphUtils.intSNode(0xb, 32);
    var args = new NodeList<DependencyNode>(M_CALL_EXP);
    instrEnd.addBefore(new TcgHelperCall(null, args, true, "raise_exception"));

  }


}
