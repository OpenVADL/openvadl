package vadl.lcb.template.lib.Target.AsmParser;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import vadl.configuration.LcbConfiguration;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.pass.PassResults;
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

  record AliasDirective(String alias, String target) {

  }

  private List<Instruction> mapInstructions(Optional<InstructionSetArchitecture> isa) {
    return isa
        .map(InstructionSetArchitecture::ownInstructions)
        .orElse(List.of());
  }

  private List<PseudoInstruction> mapPseudoInstructions(Optional<InstructionSetArchitecture> isa) {
    return isa
        .map(InstructionSetArchitecture::ownPseudoInstructions)
        .orElse(List.of());
  }


  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    //TODO: kper; add alias directives
    return Map.of(CommonVarNames.NAMESPACE, specification.name(),
        CommonVarNames.INSTRUCTIONS, mapInstructions(specification.isa()),
        CommonVarNames.PSEUDO_INSTRUCTIONS, mapPseudoInstructions(specification.isa()),
        CommonVarNames.ALIASES, List.of()
    );
  }
}
