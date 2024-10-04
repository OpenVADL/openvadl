package vadl.lcb.template.lib.Target.MCTargetDesc;

import static vadl.viam.ViamError.ensureNonNull;

import java.io.IOException;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import vadl.configuration.LcbConfiguration;
import vadl.cppCodeGen.model.CppFunction;
<<<<<<<HEAD
import vadl.gcb.passes.pseudo.PseudoExpansionFunctionGeneratorPass;
=======
import vadl.gcb.passes.pseudo.AbstractPseudoExpansionFunctionGeneratorPass;
>>>>>>>ab3e21a5(lcb:Refactored pseudo expansion)
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.lcb.template.utils.PseudoInstructionProvider;
import vadl.pass.PassResults;
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
    return "llvm/lib/Target/" + processorName + "/MCTargetDesc/" + processorName
        + "MCInstExpander.h";
  }

  record RenderedPseudoInstruction(String header, PseudoInstruction pseudoInstruction) {

  }

  /**
   * Get the simple names of the pseudo instructions.
   */
  private List<RenderedPseudoInstruction> pseudoInstructions(
      Specification specification,
      PassResults passResults,
      IdentityHashMap<PseudoInstruction, CppFunction> cppFunctions
  ) {
    return PseudoInstructionProvider.getSupportedPseudoInstructions(specification, passResults)
        .map(x -> new RenderedPseudoInstruction(
            ensureNonNull(cppFunctions.get(x), "cppFunction must exist")
                .functionName().lower(),
            x
        )).toList();
  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    var cppFunctions = (IdentityHashMap<PseudoInstruction, CppFunction>) passResults.lastResultOf(
        AbstractPseudoExpansionFunctionGeneratorPass.class);
    return Map.of(CommonVarNames.NAMESPACE, specification.simpleName(), "pseudoInstructions",
        pseudoInstructions(specification, passResults, cppFunctions));
  }
}
