package vadl.lcb.template.lib.Target.MCTargetDesc;

import static vadl.lcb.template.lib.Target.MCTargetDesc.EmitMCCodeEmitterCppFilePass.WRAPPER;
import static vadl.lcb.template.utils.ImmediateEncodingFunctionProvider.generateEncodeFunctions;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import vadl.configuration.LcbConfiguration;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.viam.Specification;

/**
 * This file contains the definitions for emitting MC instructions.
 */
public class EmitMCCodeEmitterHeaderFilePass extends LcbTemplateRenderingPass {

  public EmitMCCodeEmitterHeaderFilePass(LcbConfiguration lcbConfiguration) throws IOException {
    super(lcbConfiguration);
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/llvm/lib/Target/MCTargetDesc/TargetMCCodeEmitter.h";
  }

  @Override
  protected String getOutputPath() {
    var processorName = lcbConfiguration().processorName().value();
    return "llvm/lib/Target/" + processorName + "/MCTargetDesc/"
        + processorName + "MCCodeEmitter.h";
  }

  record Aggregate(String encodeWrapper, String encode) {

  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    return Map.of(CommonVarNames.NAMESPACE,
        lcbConfiguration().processorName().value().toLowerCase(),
        "immediates", generateImmediates(passResults));
  }

  private List<Aggregate> generateImmediates(PassResults passResults) {
    return generateEncodeFunctions(passResults)
        .values()
        .stream()
        .map(f -> new Aggregate(f.identifier.append(WRAPPER).lower(),
            f.identifier.lower()))
        .toList();
  }
}
