package vadl.lcb.template.lib.Target.MCTargetDesc;

import java.io.IOException;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import vadl.configuration.LcbConfiguration;
import vadl.cppCodeGen.model.VariantKind;
import vadl.lcb.codegen.GenerateImmediateKindPass;
import vadl.lcb.passes.llvmLowering.immediates.GenerateTableGenImmediateRecordPass;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenImmediateRecord;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.viam.Format;
import vadl.viam.Specification;

/**
 * This file contains the logic for emitting MC operands.
 */
public class EmitMCExprCppFilePass extends LcbTemplateRenderingPass {
  public EmitMCExprCppFilePass(LcbConfiguration lcbConfiguration)
      throws IOException {
    super(lcbConfiguration);
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/llvm/lib/Target/MCTargetDesc/TargetMCExpr.cpp";
  }

  @Override
  protected String getOutputPath() {
    var processorName = lcbConfiguration().processorName().value();
    return "llvm/lib/Target/" + processorName + "/MCTargetDesc/"
        + processorName + "MCExpr.cpp";
  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    var immediateRecords = ((List<TableGenImmediateRecord>) passResults.lastResultOf(
        GenerateTableGenImmediateRecordPass.class));

    return Map.of(CommonVarNames.NAMESPACE, specification.simpleName(),
        "immediates", immediateRecords);
  }
}
