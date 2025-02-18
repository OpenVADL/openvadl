package vadl.lcb.template.lib.Target.MCTargetDesc;

import static vadl.viam.ViamError.ensureNonNull;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import vadl.configuration.LcbConfiguration;
import vadl.lcb.passes.llvmLowering.immediates.GenerateTableGenImmediateRecordPass;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenImmediateRecord;
import vadl.lcb.passes.relocation.GenerateLinkerComponentsPass;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.lcb.template.utils.BaseInfoFunctionProvider;
import vadl.lcb.template.utils.ImmediateDecodingFunctionProvider;
import vadl.pass.PassResults;
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

  record Wrapper(TableGenImmediateRecord record, String baseInfoName) {

  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    var immediateRecords = ((List<TableGenImmediateRecord>) passResults.lastResultOf(
        GenerateTableGenImmediateRecordPass.class));
    var output = (GenerateLinkerComponentsPass.Output) passResults.lastResultOf(
        GenerateLinkerComponentsPass.class);
    var decodingFunctions = ImmediateDecodingFunctionProvider.generateDecodeFunctions(passResults);

    var wrapped = immediateRecords.stream().map(x -> {
      var function = decodingFunctions.get(x.fieldAccessRef().fieldRef());
      ensureNonNull(function, "function must not be null");
      return new Wrapper(x, x.rawName());
    }).toList();

    var baseInfos = BaseInfoFunctionProvider.getBaseInfoRecords(passResults);

    return Map.of(CommonVarNames.NAMESPACE,
        lcbConfiguration().processorName().value().toLowerCase(),
        "immediates", wrapped,
        "variantKinds", output.variantKinds(),
        "baseInfos", baseInfos
    );
  }
}
