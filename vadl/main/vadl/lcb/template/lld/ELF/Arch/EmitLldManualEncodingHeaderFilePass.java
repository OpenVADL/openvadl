package vadl.lcb.template.lld.ELF.Arch;

import java.io.IOException;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;
import vadl.configuration.LcbConfiguration;
import vadl.gcb.passes.relocation.model.RelocationLowerable;
import vadl.lcb.codegen.LcbGenericCodeGenerator;
import vadl.lcb.passes.relocation.GenerateLinkerComponentsPass;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.viam.Specification;

/**
 * This file contains the function to update an immediate when a relocation has to be applied.
 */
public class EmitLldManualEncodingHeaderFilePass extends LcbTemplateRenderingPass {

  public EmitLldManualEncodingHeaderFilePass(LcbConfiguration lcbConfiguration) throws IOException {
    super(lcbConfiguration);
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/lld/ELF/Arch/TargetManualEncoding.hpp";
  }

  @Override
  protected String getOutputPath() {
    return "lld/ELF/Arch/" + lcbConfiguration().processorName().value() + "ManualEncoding.cpp";
  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    var output =
        (GenerateLinkerComponentsPass.Output) passResults.lastResultOf(
            GenerateLinkerComponentsPass.class);
    var elfRelocations = output.elfRelocations();
    return Map.of(CommonVarNames.NAMESPACE, specification.simpleName(),
        "functions", elfRelocations.stream()
            .filter(x -> x instanceof RelocationLowerable)
            .map(x -> (RelocationLowerable) x)
            .collect(Collectors.groupingBy(x -> x.fieldUpdateFunction().functionName().lower()))
            .values()
            .stream()
            .map(x -> x.get(0)) // only consider one relocation because we do not need duplication
            .sorted(Comparator.comparing(o -> o.elfRelocationName().value()))
            .map(elfRelocation -> new LcbGenericCodeGenerator().generateFunction(
                elfRelocation.fieldUpdateFunction()))
            .toList());
  }
}
