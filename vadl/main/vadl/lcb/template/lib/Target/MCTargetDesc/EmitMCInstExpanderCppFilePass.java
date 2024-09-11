package vadl.lcb.template.lib.Target.MCTargetDesc;

import java.io.IOException;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import vadl.configuration.LcbConfiguration;
import vadl.cppCodeGen.model.CppFunction;
import vadl.cppCodeGen.model.CppFunctionCode;
import vadl.cppCodeGen.model.CppFunctionName;
import vadl.cppCodeGen.model.VariantKind;
import vadl.gcb.passes.pseudo.PseudoExpansionFunctionGeneratorPass;
import vadl.gcb.passes.relocation.DetectImmediatePass;
import vadl.lcb.codegen.GenerateImmediateKindPass;
import vadl.lcb.codegen.expansion.PseudoExpansionCodeGenerator;
import vadl.lcb.passes.relocation.GenerateElfRelocationPass;
import vadl.gcb.passes.relocation.model.ElfRelocation;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.lcb.template.utils.ImmediateDecodingFunctionProvider;
import vadl.pass.PassResults;
import vadl.viam.Format;
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

  record RenderedPseudoInstruction(CppFunctionName header, CppFunctionCode code,
                                   PseudoInstruction pseudoInstruction) {

  }

  /**
   * Get the simple names of the pseudo instructions.
   */
  private List<RenderedPseudoInstruction> pseudoInstructions(
      Specification specification,
      IdentityHashMap<PseudoInstruction, CppFunction> wrapped,
      DetectImmediatePass.ImmediateDetectionContainer fieldUsages,
      IdentityHashMap<Format.Field, VariantKind> variants,
      List<ElfRelocation> relocations,
      PassResults passResults) {
    return specification
        .isa()
        .map(isa -> isa.ownPseudoInstructions().stream()).orElseGet(Stream::empty)
        .map(pseudoInstruction -> {
          var codeGen =
              new PseudoExpansionCodeGenerator(lcbConfiguration().processorName().value(),
                  fieldUsages,
                  ImmediateDecodingFunctionProvider.generateDecodeFunctions(passResults),
                  variants,
                  relocations);
          var function = wrapped.get(pseudoInstruction);
          ensureNonNull(function, "a function must exist");
          return new RenderedPseudoInstruction(
              function.functionName(),
              codeGen.generateFunction(function),
              pseudoInstruction);
        })
        .toList();
  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    var wrapped =
        (IdentityHashMap<PseudoInstruction, CppFunction>) passResults.lastResultOf(
            PseudoExpansionFunctionGeneratorPass.class);
    var fieldUsages = (DetectImmediatePass.ImmediateDetectionContainer) passResults.lastResultOf(
        DetectImmediatePass.class);
    var variants = (IdentityHashMap<Format.Field, VariantKind>) passResults.lastResultOf(
        GenerateImmediateKindPass.class);
    var relocations =
        (List<ElfRelocation>) passResults.lastResultOf(GenerateElfRelocationPass.class);
    return Map.of(CommonVarNames.NAMESPACE, specification.name(),
        "pseudoInstructions",
        pseudoInstructions(specification, wrapped, fieldUsages, variants, relocations,
            passResults));
  }
}
