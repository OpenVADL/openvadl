package vadl.lcb.template.lib.Target.MCTargetDesc;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import vadl.configuration.LcbConfiguration;
import vadl.gcb.passes.relocation.model.ConcreteLogicalRelocation;
import vadl.gcb.passes.relocation.model.LogicalRelocation;
import vadl.lcb.passes.relocation.GenerateLinkerComponentsPass;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.lcb.templateUtils.RegisterUtils;
import vadl.pass.PassResults;
import vadl.viam.Specification;
import vadl.viam.passes.dummyAbi.DummyAbi;

/**
 * This file includes the util functions for asm.
 */
public class EmitAsmUtilsCppFilePass extends LcbTemplateRenderingPass {

  public EmitAsmUtilsCppFilePass(LcbConfiguration lcbConfiguration)
      throws IOException {
    super(lcbConfiguration);
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/llvm/lib/Target/MCTargetDesc/AsmUtils.cpp";
  }

  @Override
  protected String getOutputPath() {
    var processorName = lcbConfiguration().processorName().value();
    return "llvm/lib/Target/" + processorName + "/MCTargetDesc/AsmUtils.cpp";
  }

  private List<ConcreteLogicalRelocation> formatModifier(PassResults passResults) {
    var container = (GenerateLinkerComponentsPass.Output) passResults.lastResultOf(
        GenerateLinkerComponentsPass.class);
    return container.elfRelocations()
        .stream()
        .filter(x -> x instanceof ConcreteLogicalRelocation)
        .map(x -> (ConcreteLogicalRelocation) x)
        .filter(distinctByKey(LogicalRelocation::variantKind))
        .toList();
  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    var abi =
        (DummyAbi) specification.definitions().filter(x -> x instanceof DummyAbi).findFirst().get();
    return Map.of(CommonVarNames.NAMESPACE, specification.simpleName(),
        "registers",
        specification.registerFiles().map(x -> RegisterUtils.getRegisterClass(x, abi.aliases()))
            .flatMap(x -> x.registers().stream()).toList(),
        "registerClasses",
        specification.registerFiles().map(x -> RegisterUtils.getRegisterClass(x, abi.aliases()))
            .toList(),
        "formatModifiers", formatModifier(passResults)
    );
  }
}
