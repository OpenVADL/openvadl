package vadl.lcb.template.lib.Target.AsmParser;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import vadl.gcb.valuetypes.ProcessorName;
import vadl.lcb.config.LcbConfiguration;
import vadl.lcb.template.CommonVarNames;
import vadl.template.AbstractTemplateRenderingPass;
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

  record Operand(String identifier) {

  }

  record Instruction(String simpleName, List<Operand> llvmOperands) {

  }

  record AliasDirective(String alias, String target) {

  }

  @Override
  protected Map<String, Object> createVariables(Specification specification) {
    return Map.of(CommonVarNames.NAMESPACE, specification.name(),
        "instructions", List.of(new Instruction("simpleNameValue", List
            .of(new Operand("operandValue1"), new Operand("operandValue2")))),
        "aliases", List.of(new AliasDirective(".dword", ".8byte"),
            new AliasDirective(".word", ".4byte"),
            new AliasDirective(".hword", ".2byte"),
            new AliasDirective(".half", ".2byte")));
  }
}
