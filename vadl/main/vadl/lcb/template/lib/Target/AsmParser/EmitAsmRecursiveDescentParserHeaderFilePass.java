package vadl.lcb.template.lib.Target.AsmParser;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import vadl.configuration.LcbConfiguration;
import vadl.gcb.passes.assemblyConstantIntern.AssemblyConstant;
import vadl.gcb.passes.assemblyConstantIntern.AssemblyReplacementConstantPass;
import vadl.lcb.codegen.assembly.ParserGenerator;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.viam.Constant;
import vadl.viam.Specification;

/**
 * This file includes the definitions for the asm parser.
 */
public class EmitAsmRecursiveDescentParserHeaderFilePass extends LcbTemplateRenderingPass {

  public EmitAsmRecursiveDescentParserHeaderFilePass(LcbConfiguration lcbConfiguration)
      throws IOException {
    super(lcbConfiguration);
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/llvm/lib/Target/AsmParser/AsmRecursiveDescentParser.h";
  }

  @Override
  protected String getOutputPath() {
    return "llvm/lib/Target/" + lcbConfiguration().processorName().value()
        + "/AsmParser/AsmRecursiveDescentParser.h";
  }

  record ParserRecord(String structName, List<String> fieldNames) {
  }

  record ParsingResultRecord(String type, String functionName, String comment) {

  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    var structs = specification.isas()
        .flatMap(isa -> isa.ownFormats().stream())
        .map(format -> {
          var fields = Arrays.stream(format.fields()).toList();
          var fieldNames = fields.stream()
              .map(ParserGenerator::generateFieldName)
              .toList();
          return new ParserRecord(ParserGenerator.generateStructName(fields), fieldNames);
        })
        .distinct()
        .toList();

    var parsingResultsInstructions =
        specification.isas().flatMap(isa -> isa.ownInstructions().stream())
            .map(instruction -> new ParsingResultRecord("NoData",
                ParserGenerator.generateInstructionName(instruction), instruction.name()));
    var parsingResultsConstants =
        specification.isas().flatMap(isa -> isa.ownInstructions().stream())
            .flatMap(instruction -> instruction.assembly().function().behavior().getNodes(
                AssemblyConstant.class))
            .sorted(Comparator.comparing(AssemblyConstant::kind)) // Sort by something
            .map(assemblyConstant -> new ParsingResultRecord("StringRef",
                ParserGenerator.generateConstantName(assemblyConstant),
                assemblyConstant.constant().toString()));

    return Map.of(CommonVarNames.NAMESPACE, specification.name(),
        "formats", structs,
        "parsingResults", Stream.concat(parsingResultsConstants, parsingResultsInstructions));
  }
}
