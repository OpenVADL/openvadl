package vadl.lcb.template.lib.Target;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Stream;
import vadl.configuration.LcbConfiguration;
import vadl.lcb.passes.llvmLowering.GenerateRegisterClassesPass;
import vadl.lcb.passes.llvmLowering.tablegen.model.register.TableGenRegisterClass;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.viam.Specification;

/**
 * This file contains the register definitions for compiler backend.
 */
public class EmitRegisterInfoTableGenFilePass extends LcbTemplateRenderingPass {

  public EmitRegisterInfoTableGenFilePass(LcbConfiguration lcbConfiguration) throws IOException {
    super(lcbConfiguration);
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/llvm/lib/Target/RegisterInfo.td";
  }

  @Override
  protected String getOutputPath() {
    var processorName = lcbConfiguration().processorName().value();
    return "llvm/lib/Target/" + processorName + "/" + processorName
        + "RegisterInfo.td";
  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    var output = ((GenerateRegisterClassesPass.Output) passResults.lastResultOf(
        GenerateRegisterClassesPass.class));
    var registerClasses = output.registerClasses();
    var registersFromClasses = registerClasses
        .stream()
        .flatMap(rc -> rc.registers().stream())
        .distinct()
        .toList();

    return Map.of(CommonVarNames.NAMESPACE,
        lcbConfiguration().processorName().value().toLowerCase(),
        "registerFiles", registerClasses.stream().map(this::map).toList(),
        "registers",
        Stream.concat(output.registers().stream(), registersFromClasses.stream())
            .toList());
  }

  private Map<String, Object> map(TableGenRegisterClass obj) {
    return Map.of(
        "name", obj.name(),
        "namespace", obj.namespace().value(),
        "regTypesString", obj.regTypesString(),
        "registerString", obj.registerString(),
        "alignment", obj.alignment()
    );
  }
}
