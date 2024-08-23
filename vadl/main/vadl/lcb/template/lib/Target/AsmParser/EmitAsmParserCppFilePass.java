package vadl.lcb.template.lib.Target.AsmParser;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import vadl.gcb.valuetypes.ProcessorName;
import vadl.configuration.LcbConfiguration;
import vadl.lcb.template.CommonVarNames;
import vadl.pass.PassResults;
import vadl.template.AbstractTemplateRenderingPass;
import vadl.viam.Instruction;
import vadl.viam.InstructionSetArchitecture;
import vadl.viam.PseudoInstruction;
import vadl.viam.Specification;

/**
 * This file contains the implementation for parsing assembly files.
 */
public class EmitAsmParserCppFilePass extends AbstractTemplateRenderingPass {

  private final ProcessorName processorName;

  public EmitAsmParserCppFilePass(LcbConfiguration lcbConfiguration,
                                  ProcessorName processorName) throws IOException {
    super(lcbConfiguration.outputPath());
    this.processorName = processorName;
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/llvm/lib/Target/AsmParser/AsmParser.cpp";
  }

  @Override
  protected String getOutputPath() {
    return "llvm/lib/Target/" + processorName.value() + "/AsmParser/" + processorName.value()
        + "AsmParser.cpp";
  }

  record AliasDirective(String alias, String target) {

  }

  private List<Instruction> mapInstructions(Stream<InstructionSetArchitecture> isas) {
    return isas
        .flatMap(x -> x.instructions().stream())
        .toList();
  }

  private List<PseudoInstruction> mapPseudoInstructions(Stream<InstructionSetArchitecture> isas) {
    return isas
        .flatMap(x -> x.pseudoInstructions().stream())
        .toList();
  }


  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    //TODO: kper; add alias directives
    return Map.of(CommonVarNames.NAMESPACE, specification.name(),
        CommonVarNames.INSTRUCTIONS, mapInstructions(specification.isas()),
        CommonVarNames.PSEUDO_INSTRUCTIONS, mapPseudoInstructions(specification.isas()),
        CommonVarNames.ALIASES, List.of()
    );
  }
}
