package vadl.lcb.template.lib.Target;

import java.io.IOException;
import java.util.Map;
import vadl.gcb.valuetypes.ProcessorName;
import vadl.configuration.LcbConfiguration;
import vadl.lcb.template.CommonVarNames;
import vadl.pass.PassResults;
import vadl.template.AbstractTemplateRenderingPass;
import vadl.viam.Specification;

/**
 * This file contains the definitions for the transformation from DAG to InstructionSelectionDag.
 */
public class EmitDAGToDAGIselHeaderFilePass extends AbstractTemplateRenderingPass {

  private final ProcessorName processorName;

  public EmitDAGToDAGIselHeaderFilePass(LcbConfiguration lcbConfiguration,
                                        ProcessorName processorName) throws IOException {
    super(lcbConfiguration.outputPath());
    this.processorName = processorName;
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/llvm/lib/Target/DAGToDAGISel.h";
  }

  @Override
  protected String getOutputPath() {
    return "llvm/lib/Target/" + processorName.value() + "/" + processorName.value()
        + "DAGToDAGISel.h";
  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    return Map.of(CommonVarNames.NAMESPACE, specification.name());
  }
}
