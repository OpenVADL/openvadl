package vadl.lcb.template.lib.Target.AsmParser;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import vadl.configuration.LcbConfiguration;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.template.Renderable;
import vadl.viam.Instruction;
import vadl.viam.InstructionSetArchitecture;
import vadl.viam.PseudoInstruction;
import vadl.viam.Specification;

/**
 * This file contains the implementation for parsing assembly files.
 */
public class EmitAsmParserCppFilePass extends LcbTemplateRenderingPass {

  public EmitAsmParserCppFilePass(LcbConfiguration lcbConfiguration) throws IOException {
    super(lcbConfiguration);
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/llvm/lib/Target/AsmParser/AsmParser.cpp";
  }

  @Override
  protected String getOutputPath() {
    var processorName = lcbConfiguration().processorName().value();
    return "llvm/lib/Target/" + processorName + "/AsmParser/" + processorName
        + "AsmParser.cpp";
  }

  record AliasDirective(String alias, String target) implements Renderable {

    @Override
    public Map<String, Object> renderObj() {
      return Map.of(
          "alias", alias,
          "target", target
      );
    }
  }

  private List<Map<String, Object>> mapInstructions(Optional<InstructionSetArchitecture> isa) {
    return isa
        .map(InstructionSetArchitecture::ownInstructions)
        .orElse(List.of())
        .stream()
        .map(i -> Map.of(
            "name", (Object) i.simpleName()
        )).toList();
  }


  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    //TODO: kper; add alias directives
    return Map.of(CommonVarNames.NAMESPACE,
        lcbConfiguration().processorName().value().toLowerCase(),
        CommonVarNames.INSTRUCTIONS, mapInstructions(specification.isa()),
        CommonVarNames.ALIASES, List.of()
    );
  }
}
