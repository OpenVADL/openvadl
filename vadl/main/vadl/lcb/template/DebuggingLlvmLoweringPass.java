package vadl.lcb.template;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.Nullable;
import vadl.gcb.valuetypes.ProcessorName;
import vadl.lcb.config.LcbConfiguration;
import vadl.lcb.passes.isa_matching.InstructionLabel;
import vadl.pass.PassKey;
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

  @Override
  protected Map<String, Object> createVariables(final Map<PassKey, Object> passResults,
                                                Specification specification) {
    var variables = new HashMap<String, Object>();
    variables.put(CommonVarNames.NAMESPACE, specification.name());
    variables.put("processorName", processorName);

    var isaMatching =
        (HashMap<InstructionLabel, Instruction>) passResults.get(new PassKey("IsaMatchingPass"));

    if (isaMatching != null) {
      isaMatching.forEach((key, value) -> variables.put("isa." + key.name(), value.name()));
    }

    return variables;
  }

  @Nullable
  @Override
  public Object execute(Map<PassKey, Object> passResults, Specification viam)
      throws IOException {
    return null;
  }
}
