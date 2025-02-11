package vadl.lcb.template.lib.Target.AsmParser;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
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
import vadl.types.asmTypes.AsmType;
import vadl.types.asmTypes.GroupAsmType;
import vadl.viam.Specification;
import vadl.viam.ViamError;
import vadl.viam.asm.AsmGrammarVisitor;
import vadl.viam.asm.elements.AsmAlternative;
import vadl.viam.asm.elements.AsmAlternatives;
import vadl.viam.asm.elements.AsmAssignToAttribute;
import vadl.viam.asm.elements.AsmAssignToLocalVar;
import vadl.viam.asm.elements.AsmFunctionInvocation;
import vadl.viam.asm.elements.AsmGroup;
import vadl.viam.asm.elements.AsmLocalVarDefinition;
import vadl.viam.asm.elements.AsmLocalVarUse;
import vadl.viam.asm.elements.AsmOption;
import vadl.viam.asm.elements.AsmRepetition;
import vadl.viam.asm.elements.AsmRuleInvocation;
import vadl.viam.asm.elements.AsmStringLiteralUse;
import vadl.viam.asm.rules.AsmBuiltinRule;
import vadl.viam.asm.rules.AsmGrammarRule;
import vadl.viam.asm.rules.AsmNonTerminalRule;
import vadl.viam.asm.rules.AsmTerminalRule;
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
    var parsedValueStructs = parsedValueStructs(specification);
    return Map.of(CommonVarNames.NAMESPACE,
        lcbConfiguration().processorName().value().toLowerCase(),
        "formats", Stream.concat(composedStructs, singleFieldStructs).toList(),
        "parsingResults", grammarRules,
        "parsedValueStructs", parsedValueStructs);
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

  private Stream<ParsedValueStruct> parsedValueStructs(
      Specification specification) {
    return specification.assemblyDescription().map(
        asmDesc -> new StructGenerator().getAllStructs(specification.simpleName(), asmDesc.rules())
    ).orElse(Stream.empty());
  }
}


class ParsedValueStruct {
  private final String name;
  private final Stream<StructField> fields;

  record StructField(String name, String cppTypeString) {

  }

  public String getName() {
    return name;
  }

  public Stream<StructField> getFields() {
    return fields;
  }

  public ParsedValueStruct(String namespace, String name, Map<String, AsmType> fieldWithAsmType) {
    this.name = name;
    fields = fieldWithAsmType.entrySet().stream().map(
        entry -> new StructField(entry.getKey(), entry.getValue().toCppTypeString(namespace))
    );
  }
}

class StructGenerator implements AsmGrammarVisitor {

  HashSet<ParsedValueStruct> generatedStructs = new HashSet<>();
  HashSet<AsmType> alreadyGeneratedTypes = new HashSet<>();
  private String namespace = "";

  Stream<ParsedValueStruct> getAllStructs(String namespace, List<AsmGrammarRule> rules) {
    this.namespace = namespace;
    rules.forEach(rule -> rule.accept(this));
    return generatedStructs.stream();
  }

  private void generateStructIfNecessary(AsmType type) {
    if (alreadyGeneratedTypes.contains(type)) {
      return;
    }

    if (!(type instanceof GroupAsmType groupType)) {
      return;
    }

    var structName = groupType.toCppTypeString(namespace);
    if (generatedStructs.stream().anyMatch(s -> s.getName().equals(structName))) {
      throw new ViamError("Struct with name " + structName + " already exists");
    }

    var struct =
        new ParsedValueStruct(namespace, groupType.toCppTypeString(namespace),
            groupType.getSubtypeMap());
    generatedStructs.add(struct);
    alreadyGeneratedTypes.add(type);
  }

  @Override
  public void visit(AsmGrammarRule rule) {
    rule.accept(this);
  }

  @Override
  public void visit(AsmBuiltinRule rule) {
    // Do nothing because AsmType of builtin rules is never GroupAsmType
  }

  @Override
  public void visit(AsmNonTerminalRule rule) {
    generateStructIfNecessary(rule.getAsmType());
    rule.getAlternatives().accept(this);
  }

  @Override
  public void visit(AsmTerminalRule rule) {
    // Do nothing because AsmType of terminal rules is never GroupAsmType
  }

  @Override
  public void visit(AsmAlternative element) {
    element.elements().forEach(e -> e.accept(this));
  }

  @Override
  public void visit(AsmAlternatives element) {
    generateStructIfNecessary(element.asmType());
    element.alternatives().forEach(alt -> alt.accept(this));
  }

  @Override
  public void visit(AsmAssignToAttribute element) {
    // Do nothing because LHS of assignment does not have an AsmType
  }

  @Override
  public void visit(AsmAssignToLocalVar element) {
    // Do nothing because LHS of assignment does not have an AsmType
  }

  @Override
  public void visit(AsmFunctionInvocation element) {
    // Do nothing because AsmType of function invocation is never GroupAsmType
  }

  @Override
  public void visit(AsmGroup element) {
    generateStructIfNecessary(element.asmType());
    element.alternatives().accept(this);
  }

  @Override
  public void visit(AsmLocalVarDefinition element) {
    generateStructIfNecessary(element.asmType());
    if (element.asmLiteral() != null) {
      element.asmLiteral().accept(this);
    }
  }

  @Override
  public void visit(AsmLocalVarUse element) {
    generateStructIfNecessary(element.asmType());
  }

  @Override
  public void visit(AsmOption element) {
    element.alternatives().accept(this);
  }

  @Override
  public void visit(AsmRepetition element) {
    element.alternatives().accept(this);
  }

  @Override
  public void visit(AsmRuleInvocation element) {
    generateStructIfNecessary(element.asmType());
  }

  @Override
  public void visit(AsmStringLiteralUse element) {
    // Do nothing because AsmType of string literals is never GroupAsmType
  }
}
