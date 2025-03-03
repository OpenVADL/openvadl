package vadl.rtl.passes;

import java.io.IOException;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.rtl.ipg.InstructionProgressGraph;
import vadl.rtl.ipg.InstructionWordSlice;
import vadl.viam.Instruction;
import vadl.viam.Specification;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.SideEffectNode;

/**
 * Create the instruction progress graph from instruction behaviors. Adds the result to the
 * instruction set architecture definition as an extension.
 */
public class InstructionProgressGraphCreationPass extends Pass {

  public InstructionProgressGraphCreationPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("InstructionProgressGraphCreation");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    var optIsa = viam.isa();
    if (optIsa.isEmpty()) {
      return null;
    }

    var ipg = new InstructionProgressGraph("Instruction Progress Graph", optIsa.get());
    optIsa.get().ownInstructions().forEach(instr -> processInstruction(ipg, instr));

    // Attach IPG to ISA
    var ipgExt = new InstructionProgressGraphExtension(ipg);
    optIsa.get().attachExtension(ipgExt);

    return ipg;
  }

  private void processInstruction(InstructionProgressGraph ipg, Instruction instruction) {

    // replace FieldRefNodes with InstructionWordSliceNodes
    // instruction word slice nodes are unique across formats (are added only once to the IPG) and
    // collect the fields referencing the same slice in a set
    var behavior = instruction.behavior().copy();
    behavior.getNodes(FieldRefNode.class).toList().forEach(fieldRefNode -> {
      var field = fieldRefNode.formatField();
      var slice = behavior.addWithInputs(
          new InstructionWordSlice(field.format().type(), field.bitSlice(), field.type()));
      slice.addField(field);
      fieldRefNode.replaceAndDelete(slice);
    });

    // copy side effect nodes to IPG
    behavior.getNodes(SideEffectNode.class).forEach(sideEffect -> {
      var copy = sideEffect.copy();
      var ipgNode = ipg.addWithInputs(copy);
      ipg.markNodeWithInputs(ipgNode, instruction);
    });
  }
}
