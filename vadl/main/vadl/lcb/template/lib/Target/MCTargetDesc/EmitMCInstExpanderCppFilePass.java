package vadl.lcb.template.lib.Target.MCTargetDesc;

import static vadl.viam.ViamError.ensureNonNull;

import java.io.IOException;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import vadl.configuration.LcbConfiguration;
import vadl.cppCodeGen.model.CppClassImplName;
import vadl.cppCodeGen.model.GcbExpandPseudoInstructionCppFunction;
import vadl.cppCodeGen.model.VariantKind;
import vadl.gcb.passes.IdentifyFieldUsagePass;
import vadl.gcb.passes.relocation.model.CompilerRelocation;
import vadl.lcb.codegen.expansion.PseudoExpansionCodeGenerator;
import vadl.lcb.passes.pseudo.PseudoExpansionFunctionGeneratorPass;
import vadl.lcb.passes.relocation.GenerateLinkerComponentsPass;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.lcb.template.utils.ImmediateDecodingFunctionProvider;
import vadl.lcb.template.utils.PseudoInstructionProvider;
import vadl.pass.PassResults;
import vadl.template.Renderable;
import vadl.viam.Abi;
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
                                   String header,
                                   String code,
                                   PseudoInstruction pseudoInstruction) implements Renderable {

    @Override
    public Map<String, Object> renderObj() {
      return Map.of(
          "header", header,
          "code", code,
          "classImpl", classImpl,
          "pseudoInstruction", Map.of(
              "name", pseudoInstruction.simpleName()
          )
      );
    }
  }

  /**
   * Get the simple names of the pseudo instructions.
   */
  private List<RenderedPseudoInstruction> pseudoInstructions(
      Specification specification,
      Map<PseudoInstruction, GcbExpandPseudoInstructionCppFunction> cppFunctions,
      IdentifyFieldUsagePass.ImmediateDetectionContainer fieldUsages,
      Map<Format.Field, List<VariantKind>> variants,
      List<CompilerRelocation> relocations,
      PassResults passResults) {
    return PseudoInstructionProvider.getSupportedPseudoInstructions(specification, passResults)
        .map(pseudoInstruction -> renderPseudoInstruction(cppFunctions, fieldUsages,
            variants,
            relocations,
            passResults, pseudoInstruction))
        .toList();
  }

  private @Nonnull RenderedPseudoInstruction renderPseudoInstruction(
      Map<PseudoInstruction, GcbExpandPseudoInstructionCppFunction> cppFunctions,
      IdentifyFieldUsagePass.ImmediateDetectionContainer fieldUsages,
      Map<Format.Field, List<VariantKind>> variants,
      List<CompilerRelocation> relocations,
      PassResults passResults,
      PseudoInstruction pseudoInstruction) {
    var function = ensureNonNull(cppFunctions.get(pseudoInstruction),
        "cpp function must exist)");

    var base = lcbConfiguration().processorName();
    var codeGen =
        new PseudoExpansionCodeGenerator(base,
            fieldUsages,
            ImmediateDecodingFunctionProvider.generateDecodeFunctions(passResults),
            variants,
            relocations,
            pseudoInstruction,
            function);

    var renderedFunction = codeGen.genFunctionDefinition();
    var classPrefix = new CppClassImplName(
        lcbConfiguration().processorName().value().toLowerCase() + "MCInstExpander");
    ensureNonNull(function, "a function must exist");
    return new RenderedPseudoInstruction(
        classPrefix,
        pseudoInstruction.identifier.lower() + "_" + function.identifier.simpleName(),
        renderedFunction,
        pseudoInstruction);
  }

  private List<RenderedPseudoInstruction> compilerInstructions(
      Abi abi,
      Map<PseudoInstruction, GcbExpandPseudoInstructionCppFunction> cppFunctions,
      IdentifyFieldUsagePass.ImmediateDetectionContainer fieldUsages,
      Map<Format.Field, List<VariantKind>> variants,
      List<CompilerRelocation> relocations,
      PassResults passResults) {
    return Stream.of(abi.returnSequence(), abi.callSequence())
        .map(pseudoInstruction -> renderPseudoInstruction(
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
    var abi =
        (Abi) specification.definitions().filter(x -> x instanceof Abi).findFirst().get();
    var cppFunctionsForPseudoInstructions =
        (IdentityHashMap<PseudoInstruction, GcbExpandPseudoInstructionCppFunction>)
            passResults.lastResultOf(
                PseudoExpansionFunctionGeneratorPass.class);
    var cppFunctions = cppFunctionsForPseudoInstructions.entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    var fieldUsages = (IdentifyFieldUsagePass.ImmediateDetectionContainer) passResults.lastResultOf(
        IdentifyFieldUsagePass.class);
    var output = (GenerateLinkerComponentsPass.Output) passResults.lastResultOf(
        GenerateLinkerComponentsPass.class);
    var variants = output.variantKindMap();
    var relocations = output.elfRelocations();

    var pseudoInstructions =
        pseudoInstructions(specification, cppFunctions, fieldUsages, variants, relocations,
            passResults);
    var compilerInstructions =
        compilerInstructions(abi, cppFunctions, fieldUsages, variants, relocations,
            passResults);


    return Map.of(CommonVarNames.NAMESPACE,
        lcbConfiguration().processorName().value().toLowerCase(),
        "pseudoInstructions",
        Stream.concat(pseudoInstructions.stream(),
            compilerInstructions.stream()).toList()
    );
  }
}
