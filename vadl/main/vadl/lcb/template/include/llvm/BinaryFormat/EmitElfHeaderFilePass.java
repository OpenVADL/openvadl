package vadl.lcb.include.llvm.BinaryFormat;

import java.io.IOException;
import java.util.Map;
import vadl.configuration.LcbConfiguration;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.viam.Specification;

/**
 * This file contains common, non-processor-specific data structures and
 * constants for the ELF file format.
 */
public class EmitElfHeaderFilePass extends LcbTemplateRenderingPass {

  public EmitElfHeaderFilePass(LcbConfiguration lcbConfiguration)
      throws IOException {
    super(lcbConfiguration);
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/llvm/include/llvm/BinaryFormat/ELF.h";
  }

  @Override
  protected String getOutputPath() {
    return "llvm/include/llvm/BinaryFormat/ELF.h";
  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    return Map.of(CommonVarNames.NAMESPACE, specification.name());
  }
}
