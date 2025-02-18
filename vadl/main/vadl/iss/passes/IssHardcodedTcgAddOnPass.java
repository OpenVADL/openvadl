package vadl.iss.passes;

import static vadl.utils.GraphUtils.getSingleNode;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.iss.passes.tcgLowering.nodes.TcgGenException;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Instruction;
import vadl.viam.Specification;
import vadl.viam.graph.control.InstrEndNode;

/**
 * This pass manipulates the VIAM with hardcoded elements.
 * E.g. it adds an exception generation to {@code ECALL} instruction because
 * this is not yet supported in the VADL specification.
 */
public class IssHardcodedTcgAddOnPass extends Pass {

  public IssHardcodedTcgAddOnPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("ISS Hardcoded TCG Add-Ons");
  }

  List<Consumer<Instruction>> instrAddOns = List.of(
      this::ecallAddExceptionRaise
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
  private void ecallAddExceptionRaise(Instruction instr) {
    if (!instr.simpleName().equals("ECALL")) {
      return;
    }

    var instrEnd = getSingleNode(instr.behavior(), InstrEndNode.class);
    instrEnd.addBefore(new TcgGenException(0xb));
  }
  
}
