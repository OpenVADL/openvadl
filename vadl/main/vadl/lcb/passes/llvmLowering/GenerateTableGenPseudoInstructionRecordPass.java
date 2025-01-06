package vadl.lcb.passes.llvmLowering;

import static vadl.viam.ViamError.ensureNonNull;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.configuration.LcbConfiguration;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenPseudoInstruction;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Specification;

/**
 * This pass generates {@link TableGenPseudoInstruction} from the {@link LlvmLoweringPass}.
 */
public class GenerateTableGenPseudoInstructionRecordPass extends Pass {

  public GenerateTableGenPseudoInstructionRecordPass(
      GeneralConfiguration configuration) {
    super(configuration);
  }

  public LcbConfiguration lcbConfiguration() {
    return (LcbConfiguration) configuration();
  }

  @Override
  public PassName getName() {
    return new PassName("GenerateTableGenPseudoInstructionRecordsPass");
  }

  @Nullable
  @Override
  public List<TableGenPseudoInstruction> execute(PassResults passResults, Specification viam)
      throws IOException {
    var llvmLoweringPassResult =
        (LlvmLoweringPass.LlvmLoweringPassResult) ensureNonNull(
            passResults.lastResultOf(LlvmLoweringPass.class),
            "llvmLowering must exist");

    return
        llvmLoweringPassResult.pseudoInstructionRecords().entrySet().stream()
            .sorted(Comparator.comparing(o -> o.getKey().identifier.simpleName()))
            .map(entry -> {
              var instruction = entry.getKey();
              var result = entry.getValue();
              return new TableGenPseudoInstruction(
                  instruction.identifier.simpleName(),
                  lcbConfiguration().processorName().value(),
                  result.flags(),
                  result.inputs(),
                  result.outputs(),
                  result.uses(),
                  result.defs(),
                  result.patterns()
              );
            })
            .toList();
  }
}