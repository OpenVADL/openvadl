package vadl.lcb.template.lib.Target.MCTargetDesc;

import static vadl.viam.ViamError.ensureNonNull;

import java.io.IOException;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import vadl.configuration.LcbConfiguration;
import vadl.cppCodeGen.model.CppClassImplName;
import vadl.cppCodeGen.model.CppFunction;
import vadl.cppCodeGen.model.CppFunctionCode;
import vadl.cppCodeGen.model.CppFunctionName;
import vadl.cppCodeGen.model.VariantKind;
import vadl.gcb.passes.pseudo.PseudoExpansionFunctionGeneratorPass;
import vadl.gcb.passes.relocation.DetectImmediatePass;
import vadl.gcb.passes.relocation.model.ElfRelocation;
import vadl.lcb.codegen.GenerateImmediateKindPass;
import vadl.lcb.codegen.expansion.PseudoExpansionCodeGenerator;
import vadl.lcb.passes.relocation.GenerateElfRelocationPass;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.lcb.template.utils.ImmediateDecodingFunctionProvider;
import vadl.lcb.template.utils.PseudoInstructionProvider;
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
    return "llvm/lib/Target/" + processorName + "/MCTargetDesc/"
        + processorName + "MCInstExpander.cpp";
  }

  record RenderedPseudoInstruction(CppClassImplName classImpl,
                                   CppFunctionName header,
                                   CppFunctionCode code,
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
    return PseudoInstructionProvider.getSupportedPseudoInstructions(specification, passResults)
        .map(pseudoInstruction -> {
          var codeGen =
              new PseudoExpansionCodeGenerator(lcbConfiguration().processorName().value(),
                  fieldUsages,
                  ImmediateDecodingFunctionProvider.generateDecodeFunctions(passResults),
                  variants,
                  relocations);
          var function = wrapped.get(pseudoInstruction);
          var classPrefix = new CppClassImplName(specification.name() + "MCInstExpander");
          ensureNonNull(function, "a function must exist");
          return new RenderedPseudoInstruction(
              classPrefix,
              function.functionName(),
              codeGen.generateFunction(classPrefix, function, true),
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
    return Map.of(CommonVarNames.NAMESPACE, specification.simpleName(),
        "pseudoInstructions",
        pseudoInstructions(specification, wrapped, fieldUsages, variants, relocations,
            passResults));
  }
}
