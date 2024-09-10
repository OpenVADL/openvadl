package vadl.lcb.template.lib.Target.MCTargetDesc;

import java.io.IOException;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import vadl.configuration.LcbConfiguration;
import vadl.gcb.passes.pseudo.PseudoExpansionFunctionGeneratorPass;
import vadl.lcb.codegen.expansion.PseudoExpansionCodeGenerator;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.viam.Function;
import vadl.viam.PseudoInstruction;
import vadl.viam.Specification;

/**
 * This file includes the implementations for expanding instructions in the MC layer.
 */
public class EmitMCInstExpanderCppFilePass extends LcbTemplateRenderingPass {

  public EmitMCInstExpanderCppFilePass(LcbConfiguration lcbConfiguration) throws IOException {
    super(lcbConfiguration);
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/llvm/lib/Target/MCTargetDesc/TargetMCInstExpander.cpp";
  }

  @Override
  protected String getOutputPath() {
    var processorName = lcbConfiguration().processorName().value();
    return "lcb/llvm/lib/Target/" + processorName + "/MCTargetDesc/"
        + processorName + "MCInstExpander.cpp";
  }

  record RenderedPseudoInstruction(String header, String code,
                                   PseudoInstruction pseudoInstruction) {

  }

  /**
   * Get the simple names of the pseudo instructions
   */
  private List<RenderedPseudoInstruction> pseudoInstructions(
      Specification specification, IdentityHashMap<PseudoInstruction, Function> wrapped) {
    return specification.isas()
        .flatMap(isa -> isa.ownPseudoInstructions().stream())
        .map(x -> {
          var codeGen = new PseudoExpansionCodeGenerator();
          var function = wrapped.get(x);
          return new RenderedPseudoInstruction(
              codeGen.getFunctionName(x.identifier.simpleName()),
              codeGen.generateFunction(function), x);
        })
        .toList();
  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    var wrapped =
        (IdentityHashMap<PseudoInstruction, Function>) passResults.lastResultOf(
            PseudoExpansionFunctionGeneratorPass.class);
    return Map.of(CommonVarNames.NAMESPACE, specification.name(),
        "pseudoInstructions", pseudoInstructions(specification, wrapped));
  }
}
