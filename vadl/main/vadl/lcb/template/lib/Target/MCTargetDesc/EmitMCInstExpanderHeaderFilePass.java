package vadl.lcb.template.lib.Target.MCTargetDesc;

import java.io.IOException;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import vadl.configuration.LcbConfiguration;
import vadl.cppCodeGen.model.CppFunction;
import vadl.gcb.passes.pseudo.PseudoExpansionFunctionGeneratorPass;
import vadl.gcb.passes.relocation.DetectImmediatePass;
import vadl.lcb.codegen.expansion.PseudoExpansionCodeGenerator;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.lcb.template.utils.ImmediateDecodingFunctionProvider;
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
    return "lcb/llvm/lib/Target/" + processorName + "/MCTargetDesc/" + processorName
        + "MCInstExpander.h";
  }

  record RenderedPseudoInstruction(String header, PseudoInstruction pseudoInstruction) {

  }

  /**
   * Get the simple names of the pseudo instructions.
   */
  private List<RenderedPseudoInstruction> pseudoInstructions(
      Specification specification,
      IdentityHashMap<PseudoInstruction, CppFunction> cppFunctions
  ) {
    return specification.isa()
        .map(x -> x.ownPseudoInstructions().stream()).orElseGet(Stream::empty)
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
        PseudoExpansionFunctionGeneratorPass.class);
    return Map.of(CommonVarNames.NAMESPACE, specification.name(), "pseudoInstructions",
        pseudoInstructions(specification, cppFunctions));
  }
}
