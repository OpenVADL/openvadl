package vadl.lcb.template.lib.Target.AsmParser;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import vadl.configuration.LcbConfiguration;
import vadl.lcb.codegen.assembly.ParserGenerator;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.template.Renderable;
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

  record ParsingResultRecord(String type, String functionName, String comment) implements
      Renderable {

    @Override
    public Map<String, Object> renderObj() {
      return Map.of(
          "type", type,
          "functionName", functionName,
          "comment", comment
      );
    }
  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    var composedStructs = composedStructs(specification);
    var singleFieldStructs = singleFieldStructs(specification);
    //var instructions = instructions(specification);
    //var constants = constants(specification);
    //var parsingResults = Stream.concat(constants, instructions).toList();
    var grammarRules = grammarRules(specification);
    return Map.of(CommonVarNames.NAMESPACE,
        lcbConfiguration().processorName().value().toLowerCase(),
        "formats", Stream.concat(composedStructs, singleFieldStructs).toList(),
        "parsingResults", grammarRules);
  }

  @Nonnull
  private static Stream<ParserGenerator.FieldStructEnumeration> composedStructs(
      Specification specification) {
    return specification.isa()
        .map(isa -> isa.ownInstructions().stream())
        .orElse(Stream.empty())
        .flatMap(
            instruction -> instruction.assembly().function().behavior().getNodes(BuiltInCall.class))
        .filter(node -> node.builtIn() == BuiltInTable.CONCATENATE_STRINGS)
        .map(ParserGenerator::mapParserRecord)
        .distinct()
        .sorted(Comparator.comparing(ParserGenerator.FieldStructEnumeration::structName));
  }

  @Nonnull
  private static Stream<ParserGenerator.FieldStructEnumeration> singleFieldStructs(
      Specification specification) {
    return specification.isa()
        .map(isa -> isa.ownFormats().stream())
        .orElse(Stream.empty())
        .flatMap(format -> Arrays.stream(format.fields()))
        .map(field -> new ParserGenerator.FieldStructEnumeration(
            ParserGenerator.generateStructName(List.of(field)),
            Stream.of(field).map(ParserGenerator::generateFieldName).toList()))
        .distinct()
        .sorted(Comparator.comparing(ParserGenerator.FieldStructEnumeration::structName));
  }

  /*
  @Nonnull
  private static Stream<ParsingResultRecord> instructions(
      Specification specification) {
    return specification.isa()
        .map(isa -> isa.ownInstructions().stream())
        .orElse(Stream.empty())
        .map(instruction -> new ParsingResultRecord("NoData",
            ParserGenerator.generateInstructionName(instruction), instruction.simpleName()));
  }

  @Nonnull
  private static Stream<ParsingResultRecord> constants(
      Specification specification) {
    return
        specification.isa()
            .map(isa -> isa.ownInstructions().stream())
            .orElse(Stream.empty())
            .flatMap(instruction -> instruction.assembly().function().behavior().getNodes(
                AssemblyConstant.class))
            .sorted(Comparator.comparing(AssemblyConstant::kind)) // Sort by something
            .map(assemblyConstant -> new ParsingResultRecord("StringRef",
                ParserGenerator.generateConstantName(assemblyConstant),
                assemblyConstant.constant().toString()))
            .filter(distinctByKeyClass(x -> x.functionName));
  }

  public static <T> Predicate<T> distinctByKeyClass(
      final Function<? super T, Object> keyExtractor) {
    Map<Object, Boolean> seen = new ConcurrentHashMap<>();
    return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
  }
  */

  private Stream<ParsingResultRecord> grammarRules(Specification specification) {
    return specification.assemblyDescription().map(
        asmDesc -> asmDesc.rules().stream().map(
            rule -> new ParsingResultRecord(
                rule.getAsmType().toCppTypeString(specification.simpleName()),
                rule.simpleName().trim(), rule.simpleName()
            )
        )
    ).orElse(Stream.empty());
  }
}
