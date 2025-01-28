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
import vadl.types.BuiltInTable;
import vadl.types.Type;
import vadl.viam.Instruction;
import vadl.viam.Specification;
import vadl.viam.graph.control.InstrEndNode;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.SignExtendNode;

/**
 * This pass manipulates the VIAM with hardcoded elements in an early stage.
 */
// TODO: Remove this once frontend is working
public class IssHardcodedBehaviorAddOnPass extends Pass {

  public IssHardcodedBehaviorAddOnPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("ISS Hardcoded Behavior Add-Ons");
  }

  List<Consumer<Instruction>> instrAddOns = List.of(
      this::mulwChangeSmullToMulBuiltin
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

  /* Changes the smull call in MULW to MUL. */
  private void mulwChangeSmullToMulBuiltin(Instruction instr) {
    if (!instr.simpleName().equals("MULW")) {
      return;
    }

    var smull = instr.behavior().getNodes(BuiltInCall.class)
        .filter(b -> b.builtIn() == BuiltInTable.SMULL)
        .findFirst().get();

    var mulNew = new BuiltInCall(
        BuiltInTable.MUL,
        smull.arguments(),
        Type.bits(32)
    );
    var signExtend = instr.behavior().addWithInputs(
        new SignExtendNode(mulNew, Type.bits(64))
    );
    smull.replaceAndDelete(signExtend);
  }


}
