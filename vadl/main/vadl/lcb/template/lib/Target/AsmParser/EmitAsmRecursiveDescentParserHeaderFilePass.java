package vadl.lcb.template.lib.Target.AsmParser;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import vadl.configuration.LcbConfiguration;
import vadl.gcb.passes.assembly.AssemblyConstant;
import vadl.lcb.codegen.assembly.ParserGenerator;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.types.BuiltInTable;
import vadl.viam.Specification;
import vadl.viam.graph.dependency.BuiltInCall;

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

  record ParsingResultRecord(String type, String functionName, String comment) {

  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    var composedStructs = composedStructs(specification);
    var singleFieldStructs = singleFieldStructs(specification);
    var instructions = instructions(specification);
    var constants = constants(specification);
    return Map.of(CommonVarNames.NAMESPACE, specification.name(),
        "formats", Stream.concat(composedStructs, singleFieldStructs),
        "parsingResults", Stream.concat(constants, instructions));
  }

  @NotNull
  private static Stream<ParserGenerator.FieldStructEnumeration> composedStructs(
      Specification specification) {
    return specification.isas()
        .flatMap(isa -> isa.ownInstructions().stream())
        .flatMap(
            instruction -> instruction.assembly().function().behavior().getNodes(BuiltInCall.class))
        .filter(node -> node.builtIn() == BuiltInTable.CONCATENATE_STRINGS)
        .map(ParserGenerator::mapParserRecord)
        .distinct();
  }

  @NotNull
  private static Stream<ParserGenerator.FieldStructEnumeration> singleFieldStructs(
      Specification specification) {
    return specification.isas()
        .flatMap(isa -> isa.ownFormats().stream())
        .flatMap(format -> Arrays.stream(format.fields()))
        .map(field -> new ParserGenerator.FieldStructEnumeration(
            ParserGenerator.generateStructName(List.of(field)),
            Stream.of(field).map(ParserGenerator::generateFieldName).toList()))
        .distinct();
  }

  @NotNull
  private static Stream<ParsingResultRecord> instructions(
      Specification specification) {
    return specification.isas().flatMap(isa -> isa.ownInstructions().stream())
        .map(instruction -> new ParsingResultRecord("NoData",
            ParserGenerator.generateInstructionName(instruction), instruction.name()));
  }

  @NotNull
  private static Stream<ParsingResultRecord> constants(
      Specification specification) {
    return
        specification.isas().flatMap(isa -> isa.ownInstructions().stream())
            .flatMap(instruction -> instruction.assembly().function().behavior().getNodes(
                AssemblyConstant.class))
            .sorted(Comparator.comparing(AssemblyConstant::kind)) // Sort by something
            .map(assemblyConstant -> new ParsingResultRecord("StringRef",
                ParserGenerator.generateConstantName(assemblyConstant),
                assemblyConstant.constant().toString()));
  }
}
