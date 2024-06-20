package vadl.lcb.lib.Target;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;
import vadl.gcb.valuetypes.ProcessorName;
import vadl.lcb.config.LcbConfiguration;
import vadl.lcb.template.CommonVarNames;
import vadl.template.AbstractTemplateRenderingPass;
import vadl.viam.Specification;

/**
 * This file contains the transformation from DAG to InstructionSelectionDag.
 */
public class EmitDAGToDAGISelCppFilePass extends AbstractTemplateRenderingPass {

  private final ProcessorName processorName;

  public EmitDAGToDAGISelCppFilePass(LcbConfiguration lcbConfiguration, ProcessorName processorName)
      throws IOException {
    super(lcbConfiguration.outputPath());
    this.processorName = processorName;
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/llvm/Target/DAGToDAGISel.cpp";
  }

  @Override
  protected String getOutputPath() {
    return "llvm/Target/" + processorName.value() + "/" + processorName.value()
        + "DAGToDAGISel.cpp";
  }

  @Override
  protected Map<String, Object> createVariables(Specification specification) {
    return Map.of(CommonVarNames.NAMESPACE, specification.name());
  }
}
