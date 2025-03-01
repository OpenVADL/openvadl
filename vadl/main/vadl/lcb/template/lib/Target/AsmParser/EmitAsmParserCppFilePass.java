package vadl.lcb.template.lib.Target.AsmParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import vadl.configuration.LcbConfiguration;
import vadl.lcb.passes.llvmLowering.LlvmLoweringPass;
import vadl.lcb.passes.llvmLowering.domain.LlvmLoweringRecord;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.tableGenParameter.TableGenParameterTypeAndName;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.template.Renderable;
import vadl.viam.AssemblyDescription;
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

  private List<Map<String, String>> instructionsWithOperands(PassResults results) {
    var output =
        (LlvmLoweringPass.LlvmLoweringPassResult) results.lastResultOf(LlvmLoweringPass.class);
    var result = new ArrayList<Map<String, String>>();

    output.machineInstructionRecords().forEach(
        (insn, llvmRecord) -> buildInstructionOperandMap(insn.simpleName(), llvmRecord, result)
    );

    output.pseudoInstructionRecords().forEach(
        (pseudo, llvmRecord) -> buildInstructionOperandMap(pseudo.simpleName(), llvmRecord, result)
    );

    return result;
  }

  private void buildInstructionOperandMap(String insnName, LlvmLoweringRecord llvmRecord,
                                          List<Map<String, String>> results) {
    var inputs = llvmRecord.inputs().stream()
        .map(i -> ((TableGenParameterTypeAndName) i.parameter()).name());
    var outputs = llvmRecord.outputs().stream()
        .map(p -> ((TableGenParameterTypeAndName) p.parameter()).name());

    var operands = Stream.concat(outputs, inputs).map(op -> '"' + op + '"').toList();
    results.add(Map.of(
        "name", insnName,
        "operands", String.join(", ", operands)
    ));
  }


  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    return Map.of(CommonVarNames.NAMESPACE,
        lcbConfiguration().processorName().value().toLowerCase(),
        CommonVarNames.ALIASES, directiveMappings(specification.assemblyDescription()),
        CommonVarNames.INSTRUCTIONS, instructionsWithOperands(passResults)
    );
  }

  private List<AliasDirective> directiveMappings(Optional<AssemblyDescription> asmDescription) {
    return asmDescription.map(
        asmDesc -> asmDesc.directives().stream().map(
            d -> new AliasDirective(d.getAlias(), d.getTarget())).toList()
    ).orElse(List.of());
  }
}
