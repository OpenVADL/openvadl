package vadl.iss.template.target;

import static vadl.error.Diagnostic.error;
import static vadl.utils.GraphUtils.getSingleNode;

import java.util.List;
import java.util.Map;
import vadl.configuration.IssConfiguration;
import vadl.iss.codegen.IssTranslateCodeGenerator;
import vadl.iss.template.IssTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.viam.Specification;
import vadl.viam.graph.dependency.SignExtendNode;

/**
 * Emits the target/gen-arch/translate.c that contains the functions to generate
 * the TCG instructions from decoded guest instructions.
 * It also contains the {@code gen_intermediate_code} function, called by QEMU as
 * entry point to start the TCG generation.
 */
public class EmitIssTranslatePass extends IssTemplateRenderingPass {
  public EmitIssTranslatePass(IssConfiguration configuration) {
    super(configuration);
  }

  @Override
  protected String issTemplatePath() {
    return "target/gen-arch/translate.c";
  }

  @Override
  protected Map<String, Object> createVariables(PassResults passResults,
                                                Specification specification) {
    var vars = super.createVariables(passResults, specification);
    vars.put("insn_width", getInstructionWidth(specification));
    vars.put("mem_word_size", getMemoryWordSize(specification));
    vars.put("translate_functions", getTranslateFunctions(specification));
    return vars;
  }

  private static List<String> getTranslateFunctions(Specification specification) {
    var insns = specification.isa().get().ownInstructions();
    return insns.stream()
        // TODO: Remove this filter (just for testing)
        .filter(i -> i.identifier.simpleName().equalsIgnoreCase("ADD"))
        .map(IssTranslateCodeGenerator::fetch)
        .toList();
  }

  private static Map<String, Object> getMemoryWordSize(Specification specification) {
    return Map.of(
        "int", 8
    );
  }

  private static Map<String, Object> getInstructionWidth(Specification specification) {
    var refFormat = specification.isa().get().ownInstructions()
        .get(0).format();
    var width = refFormat.type().bitWidth();

    return switch (width) {
      case 8 -> Map.of(
          "short", "b",
          "int", 8
      );
      case 16 -> Map.of(
          "short", "uw",
          "int", 16
      );
      case 32 -> Map.of(
          "short", "l",
          "int", 32
      );
      case 64 -> Map.of(
          "short", "q",
          "int", 64
      );
      default -> throw error("Invalid instruction width", refFormat.identifier.sourceLocation())
          .description(
              ("The ISS generator requires that every instruction width " +
                  "is one of [8, 16, 32, 64], but found %s")
                  .formatted(width))
          .build();
    };
  }

}
