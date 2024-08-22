package vadl.lcb.template;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import vadl.gcb.valuetypes.ProcessorName;
import vadl.lcb.config.LcbConfiguration;
import vadl.lcb.passes.isaMatching.InstructionLabel;
import vadl.pass.PassKey;
import vadl.pass.PassResults;
import vadl.pass.PassResults;
import vadl.template.AbstractTemplateRenderingPass;
import vadl.viam.Instruction;
import vadl.viam.Specification;

/**
 * File for debugging the instruction lowering.
 */
public class DebuggingLlvmLoweringPass extends AbstractTemplateRenderingPass {

  private final ProcessorName processorName;

  public DebuggingLlvmLoweringPass(LcbConfiguration lcbConfiguration, ProcessorName processorName)
      throws IOException {
    super(lcbConfiguration.outputPath());
    this.processorName = processorName;
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/DebuggingLlvmLowering.txt";
  }

  @Override
  protected String getOutputPath() {
    return "DebuggingLlvmLowering.txt";
  }

  record IsaMatchingPair(InstructionLabel label, String matchedInstruction) {

  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    var variables = new HashMap<String, Object>();
    variables.put(CommonVarNames.NAMESPACE, specification.name());
    variables.put("processorName", processorName);

    var isaMatching =
        (HashMap<InstructionLabel, Instruction>) passResults.get(new PassKey("IsaMatchingPass"));

    if (isaMatching != null) {
      var matched = Arrays.stream(InstructionLabel.values()).map(l -> new IsaMatchingPair(l,
              Optional.ofNullable(isaMatching.get(l)).map(x -> x.identifier.name()).orElse("---")))
          .toList();
      variables.put("isa", matched);
    }

    return variables;
  }
}
