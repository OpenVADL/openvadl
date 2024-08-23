package vadl.lcb.clang.lib.Basic.Targets;

import java.io.IOException;
import java.util.Map;
import vadl.gcb.valuetypes.ProcessorName;
import vadl.configuration.LcbConfiguration;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.template.AbstractTemplateRenderingPass;
import vadl.viam.Specification;

/**
 * This file contains the datatype configuration for the llvm's types.
 */
public class EmitClangTargetHeaderFilePass extends LcbTemplateRenderingPass {

  private final ProcessorName processorName;

  record DataLayout(boolean isBigEndian, int pointerSize, int pointerAlignment) {
  }

  public EmitClangTargetHeaderFilePass(LcbConfiguration lcbConfiguration,
                                       ProcessorName processorName) throws IOException {
    super(lcbConfiguration);
    this.processorName = processorName;
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/clang/lib/Basic/Targets/ClangTarget.h";
  }

  @Override
  protected String getOutputPath() {
    return "clang/lib/Basic/Targets/" + processorName.value() + ".h";
  }

  private String createDataLayout(DataLayout dataLayout) {
    String loweredEndian = dataLayout.isBigEndian ? "E-" : "e-";
    String loweredPointer =
        String.format("p:%d:%d-", dataLayout.pointerSize, dataLayout.pointerSize);
    return String.format("%sm:e-%sS0-a:0:64-i1:8:8-i8:8:8-i16:16:16-i32:32:32-i64:32:64",
        loweredEndian, loweredPointer);
  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    return Map.of(CommonVarNames.NAMESPACE, specification.name(),
        CommonVarNames.DATALAYOUT, createDataLayout(new DataLayout(false, 32, 32)));
  }
}
