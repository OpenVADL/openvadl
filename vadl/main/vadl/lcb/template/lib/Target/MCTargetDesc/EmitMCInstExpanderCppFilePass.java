package vadl.lcb.template.lib.Target.MCTargetDesc;

import static vadl.viam.ViamError.ensureNonNull;

import java.io.IOException;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import vadl.configuration.LcbConfiguration;
import vadl.cppCodeGen.model.CppClassImplName;
import vadl.cppCodeGen.model.CppFunction;
import vadl.cppCodeGen.model.CppFunctionCode;
import vadl.cppCodeGen.model.CppFunctionName;
import vadl.cppCodeGen.model.VariantKind;
import vadl.gcb.passes.pseudo.PseudoExpansionFunctionGeneratorPass;
import vadl.gcb.passes.relocation.IdentifyFieldUsagePass;
import vadl.gcb.passes.relocation.model.ElfRelocation;
import vadl.lcb.codegen.LcbGenericCodeGenerator;
import vadl.lcb.codegen.expansion.PseudoExpansionCodeGenerator;
import vadl.lcb.passes.llvmLowering.ConstMaterialisationPseudoExpansionFunctionGeneratorPass;
import vadl.lcb.passes.llvmLowering.domain.ConstantMatPseudoInstruction;
import vadl.lcb.passes.llvmLowering.immediates.GenerateConstantMaterialisationPass;
import vadl.lcb.passes.relocation.GenerateElfRelocationPass;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.lcb.template.utils.ImmediateDecodingFunctionProvider;
import vadl.lcb.template.utils.ImmediateVariantKindProvider;
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
      Map<PseudoInstruction, CppFunction> cppFunctions,
      IdentifyFieldUsagePass.ImmediateDetectionContainer fieldUsages,
      Map<Format.Field, VariantKind> variants,
      List<ElfRelocation> relocations,
      PassResults passResults) {
    return PseudoInstructionProvider.getSupportedPseudoInstructions(specification, passResults)
        .map(pseudoInstruction -> renderPseudoInstruction(specification, cppFunctions, fieldUsages,
            variants,
            relocations,
            passResults, pseudoInstruction))
        .toList();
  }

  private @NotNull RenderedPseudoInstruction renderPseudoInstruction(
      Specification specification,
      Map<PseudoInstruction, CppFunction> cppFunctions,
      IdentifyFieldUsagePass.ImmediateDetectionContainer fieldUsages,
      Map<Format.Field, VariantKind> variants,
      List<ElfRelocation> relocations,
      PassResults passResults,
      PseudoInstruction pseudoInstruction) {
    var codeGen =
        new PseudoExpansionCodeGenerator(lcbConfiguration().processorName().value(),
            fieldUsages,
            ImmediateDecodingFunctionProvider.generateDecodeFunctions(passResults),
            variants,
            relocations,
            pseudoInstruction);
    var function = cppFunctions.get(pseudoInstruction);
    var classPrefix = new CppClassImplName(specification.simpleName() + "MCInstExpander");
    ensureNonNull(function, "a function must exist");
    return new RenderedPseudoInstruction(
        classPrefix,
        function.functionName(),
        codeGen.generateFunction(classPrefix, function,
            new LcbGenericCodeGenerator.Options(true, false)),
        pseudoInstruction);
  }

  private List<RenderedPseudoInstruction> constMatInstructions(
      Specification specification,
      Map<PseudoInstruction, CppFunction> cppFunctions,
      IdentifyFieldUsagePass.ImmediateDetectionContainer fieldUsages,
      Map<Format.Field, VariantKind> variants,
      List<ElfRelocation> relocations,
      PassResults passResults) {
    var constMats = (List<ConstantMatPseudoInstruction>) passResults.lastResultOf(
        GenerateConstantMaterialisationPass.class);

    return constMats.stream()
        .map(pseudoInstruction -> renderPseudoInstruction(specification,
            cppFunctions,
            fieldUsages,
            variants,
            relocations,
            passResults, pseudoInstruction))
        .toList();
  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    var cppFunctionsForPseudoInstructions =
        (IdentityHashMap<PseudoInstruction, CppFunction>) passResults.lastResultOf(
            PseudoExpansionFunctionGeneratorPass.class);
    var cppFunctionsForConstMatInstructions =
        (IdentityHashMap<PseudoInstruction, CppFunction>) passResults.lastResultOf(
            ConstMaterialisationPseudoExpansionFunctionGeneratorPass.class);
    var cppFunctions = Stream.concat(cppFunctionsForPseudoInstructions.entrySet().stream(),
            cppFunctionsForConstMatInstructions.entrySet().stream())
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    var fieldUsages = (IdentifyFieldUsagePass.ImmediateDetectionContainer) passResults.lastResultOf(
        IdentifyFieldUsagePass.class);
    var variants = ImmediateVariantKindProvider.variantKindsByField(passResults);
    var relocations =
        (List<ElfRelocation>) passResults.lastResultOf(GenerateElfRelocationPass.class);

    var pseudoInstructions =
        pseudoInstructions(specification, cppFunctions, fieldUsages, variants, relocations,
            passResults);
    var constMatInstructions =
        constMatInstructions(specification, cppFunctions, fieldUsages, variants, relocations,
            passResults);
    return Map.of(CommonVarNames.NAMESPACE, specification.simpleName(),
        "pseudoInstructions",
        Stream.concat(pseudoInstructions.stream(), constMatInstructions.stream()).toList()
    );
  }
}
