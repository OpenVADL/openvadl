package vadl.lcb.passes.llvmLowering.immediates;

import static vadl.viam.ViamError.ensure;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import org.jetbrains.annotations.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.error.Diagnostic;
import vadl.lcb.passes.isaMatching.InstructionLabel;
import vadl.lcb.passes.isaMatching.IsaMatchingPass;
import vadl.lcb.passes.llvmLowering.domain.ConstantMatPseudoInstruction;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmConstantSD;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenImmediateRecord;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.types.DataType;
import vadl.viam.Constant;
import vadl.viam.Instruction;
import vadl.viam.Parameter;
import vadl.viam.PseudoInstruction;
import vadl.viam.Specification;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.ReadResourceNode;
import vadl.viam.graph.dependency.WriteResourceNode;

/**
 * This pass generates a {@link PseudoInstruction} which consume immediates and emit a machine
 * instruction which loads the value into the register. Note that we will not be looking for
 * instruction like {@code LI} because it is hard to capture the semantics. Instead, we use
 * {@link IsaMatchingPass} and use the {@code ADDI} as move-instruction.
 */
public class GenerateConstantMaterialisationPass extends Pass {

  public GenerateConstantMaterialisationPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("GenerateConstantMaterialisationPass");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    var constantMatInstructions = new ArrayList<ConstantMatPseudoInstruction>();
    var isaMatching = ((HashMap<InstructionLabel, List<Instruction>>) passResults.lastResultOf(
        IsaMatchingPass.class));
    var immediates = ((List<TableGenImmediateRecord>) passResults.lastResultOf(
        GenerateTableGenImmediateRecordPass.class));

    var addis = isaMatching.getOrDefault(InstructionLabel.ADDI_64,
        isaMatching.getOrDefault(InstructionLabel.ADDI_32, Collections.emptyList()));
    ensure(!addis.isEmpty(), () -> Diagnostic.error(
        "Specification has no instruction for addition with immediate. Therefore, vadl "
            + "cannot generate an instruction for constant materialisation.",
        viam.sourceLocation()).build());
    var addi = addis.stream().findFirst().get();

    for (var imm : immediates) {
      var copy = addi.behavior().copy();

      // Replace field with constant 0.
      // First, get the fields which are candidates.
      // The field is a candidate if it is not used a ReadRegisterFileNode or
      // WriteRegisterFileNode.
      var fields = copy.getNodes(FieldRefNode.class)
          .filter(fieldNode -> fieldNode.usages().allMatch(
              usage -> !(usage instanceof ReadResourceNode || usage instanceof WriteResourceNode)))
          .toList();

      for (var candidate : fields) {
        candidate.replaceAndDelete(
            new LlvmConstantSD(Constant.Value.of(0, (DataType) candidate.type())));
      }

      var instruction =
          new ConstantMatPseudoInstruction(addi.identifier.append(imm.rawName() + "_const_mat"),
              new Parameter[] {}, copy, addi, imm);
      constantMatInstructions.add(instruction);
    }

    return constantMatInstructions;
  }
}
