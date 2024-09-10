package vadl.lcb.template.lib.Target.MCTargetDesc;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import vadl.configuration.LcbConfiguration;
import vadl.lcb.codegen.expansion.PseudoExpansionCodeGenerator;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.viam.Instruction;
import vadl.viam.PseudoInstruction;
import vadl.viam.Specification;

/**
 * This file includes the definitions for expanding instructions in the MC layer.
 */
public class EmitMCInstExpanderHeaderFilePass extends LcbTemplateRenderingPass {

  public EmitMCInstExpanderHeaderFilePass(LcbConfiguration lcbConfiguration) throws IOException {
    super(lcbConfiguration);
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/llvm/lib/Target/MCTargetDesc/TargetMCInstExpander.h";
  }

  @Override
  protected String getOutputPath() {
    var processorName = lcbConfiguration().processorName().value();
    return "lcb/llvm/lib/Target/" + processorName + "/MCTargetDesc/"
        + processorName + "MCInstExpander.h";
  }

  record RenderedPseudoInstruction(String header, PseudoInstruction pseudoInstruction) {

  }

  /**
   * Get the simple names of the pseudo instructions
   */
  private List<RenderedPseudoInstruction> pseudoInstructions(Specification specification) {
    return specification.isas()
        .flatMap(isa -> isa.ownPseudoInstructions().stream())
        .map(x -> new RenderedPseudoInstruction(
            new PseudoExpansionCodeGenerator().getFunctionName(x.identifier.simpleName()), x))
        .toList();
  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    return Map.of(CommonVarNames.NAMESPACE, specification.name(),
        "pseudoInstructions", pseudoInstructions(specification));
  }
}
