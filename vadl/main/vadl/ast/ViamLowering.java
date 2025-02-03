package vadl.ast;


import com.google.errorprone.annotations.concurrent.LazyInit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import vadl.error.Diagnostic;
import vadl.types.BitsType;
import vadl.types.DataType;
import vadl.types.Type;
import vadl.utils.SourceLocation;
import vadl.utils.WithSourceLocation;
import vadl.viam.Assembly;
import vadl.viam.AssemblyDescription;
import vadl.viam.Constant;
import vadl.viam.Counter;
import vadl.viam.Encoding;
import vadl.viam.Format;
import vadl.viam.Function;
import vadl.viam.Instruction;
import vadl.viam.Memory;
import vadl.viam.Parameter;
import vadl.viam.PseudoInstruction;
import vadl.viam.Register;
import vadl.viam.RegisterFile;
import vadl.viam.Relocation;
import vadl.viam.Specification;
import vadl.viam.asm.AsmDirectiveMapping;
import vadl.viam.asm.AsmModifier;
import vadl.viam.asm.elements.AsmAlternative;
import vadl.viam.asm.elements.AsmAlternatives;
import vadl.viam.asm.elements.AsmAssignTo;
import vadl.viam.asm.elements.AsmAssignToAttribute;
import vadl.viam.asm.elements.AsmAssignToLocalVar;
import vadl.viam.asm.elements.AsmFunctionInvocation;
import vadl.viam.asm.elements.AsmGrammarElement;
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
import vadl.viam.graph.Graph;

/**
 * The lowering that converts the AST to the VIAM.
 */
@SuppressWarnings("OverloadMethodsDeclarationOrder")
public class ViamLowering implements DefinitionVisitor<Optional<vadl.viam.Definition>> {

  @LazyInit
  private BehaviorLowering behaviorLowering;

  private final ConstantEvaluator constantEvaluator = new ConstantEvaluator();

  private final IdentityHashMap<Definition, Optional<vadl.viam.Definition>> definitionCache =
      new IdentityHashMap<>();
  private final IdentityHashMap<FormatDefinition.FormatField, Format.Field>
      formatFieldCache = new IdentityHashMap<>();

  @LazyInit
  private vadl.viam.Specification currentSpecification;

  public ViamLowering() {
    this.behaviorLowering = new BehaviorLowering(this);
  }

  /**
   * Generates a VIAM specification from an AST.
   *
   * <p>The AST must be typechecked and correct.
   *
   * @param ast to lower.
   * @return the viam specification.
   * @throws Diagnostic if something goes wrong.
   */
  public vadl.viam.Specification generate(Ast ast) {
    var spec = new Specification(
        new vadl.viam.Identifier(ParserUtils.baseName(ast.fileUri),
            SourceLocation.INVALID_SOURCE_LOCATION));
    this.currentSpecification = spec;

    spec.addAll(ast.definitions.stream()
        .map(this::fetch)
        .flatMap(Optional::stream)
        .collect(Collectors.toList()));
    return spec;
  }

  /**
   * Fetch from the cache the viam node or evaluate it.
   *
   * @param definition for which we want to find the corresponding viam node.
   * @return the viam node.
   */
  Optional<vadl.viam.Definition> fetch(Definition definition) {
    if (definitionCache.containsKey(definition)) {
      return definitionCache.get(definition);
    }

    var result = definition.accept(this);
    definitionCache.put(definition, result);
    return result;
  }

  /**
   * Fetch from the cache the format field node or evaluate it.
   *
   * @param field for which we want to find the corresponding viam node.
   * @return the viam node.
   */
  Optional<vadl.viam.Format.Field> fetch(FormatDefinition.FormatField field) {

    // FIXME: Try to evaluate the format if it hasn't been seen before.
    return Optional.ofNullable(formatFieldCache.get(field));
  }


  /**
   * Generate a new viam Identifier from an ast Identifier.
   *
   * @param viamId    often the viam identifier have a different name than the ast
   *                  (prepended by their "path")
   * @param locatable the location of the identifier in the ast.
   * @return the new identifier.
   */
  private vadl.viam.Identifier generateIdentifier(String viamId, WithSourceLocation locatable) {
    return new vadl.viam.Identifier(viamId, locatable.sourceLocation());
  }


  /**
   * A simple helper util that returns a copy of the list casted to the class provided.
   */
  private <T, U> List<T> filterAndCastToInstance(List<U> values, Class<T> type) {
    return values.stream().filter(type::isInstance).map(type::cast).toList();
  }

  @Override
  public Optional<vadl.viam.Definition> visit(AbiSequenceDefinition definition) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        definition.getClass().getSimpleName()));
  }

  @Override
  public Optional<vadl.viam.Definition> visit(AliasDefinition definition) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        definition.getClass().getSimpleName()));
  }

  @Override
  public Optional<vadl.viam.Definition> visit(ApplicationBinaryInterfaceDefinition definition) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        definition.getClass().getSimpleName()));
  }

  @Override
  public Optional<vadl.viam.Definition> visit(AsmDescriptionDefinition definition) {

    var id = generateIdentifier(definition.viamId, definition.identifier());

    var modifiers = definition.modifiers.stream()
        .map(m -> (AsmModifier) fetch(m).orElseThrow()).toList();

    var directives = definition.directives.stream()
        .map(d -> (AsmDirectiveMapping) fetch(d).orElseThrow()).toList();

    var rules = definition.rules.stream().map(this::fetch).flatMap(Optional::stream)
        .map(rule -> (AsmGrammarRule) rule).toList();

    var commonDefinitions =
        definition.commonDefinitions.stream().map(this::fetch).flatMap(Optional::stream).toList();

    return Optional.of(
        new AssemblyDescription(id, modifiers, directives, rules, commonDefinitions));
  }

  @Override
  public Optional<vadl.viam.Definition> visit(AsmDirectiveDefinition definition) {
    var directive = Arrays.stream(AsmDirective.values())
        .filter(x -> x.toString().equals(definition.builtinDirective.name)).findFirst()
        .orElseThrow();
    var id = ((StringLiteral) definition.stringLiteral).value;
    return Optional.of(new AsmDirectiveMapping(generateIdentifier(id, definition), directive,
        definition.sourceLocation()));
  }

  @Override
  public Optional<vadl.viam.Definition> visit(AsmGrammarAlternativesDefinition definition) {
    // Do nothing on purpose.
    // AsmGrammarElementDefinition definitions are visited as part of the AsmGrammarRuleDefinition
    // as this also does reflect better the structure in the viam.
    // You can look at visitAsmAlternatives where it is implemented.
    return Optional.empty();
  }

  @Override
  public Optional<vadl.viam.Definition> visit(AsmGrammarElementDefinition definition) {
    // Do nothing on purpose.
    // AsmGrammarElementDefinition definitions are visited as part of the AsmGrammarRuleDefinition
    // as this also does reflect better the structure in the viam.
    // You can look at visitAsmElement where it is implemented.
    return Optional.empty();
  }

  @Override
  public Optional<vadl.viam.Definition> visit(AsmGrammarLiteralDefinition definition) {
    // Do nothing on purpose.
    // AsmGrammarLiteralDefinition definitions are visited as part of the AsmGrammarRuleDefinition
    // as this also does reflect better the structure in the viam.
    // You can look at visitAsmLiteral where it is implemented.
    return Optional.empty();
  }

  @Override
  public Optional<vadl.viam.Definition> visit(AsmGrammarLocalVarDefinition definition) {
    // Do nothing on purpose.
    // AsmGrammarLocalVarDefinition definitions are visited as part of the AsmGrammarRuleDefinition
    // as this also does reflect better the structure in the viam.
    // You can look at visitAsmElement where it is implemented.
    return Optional.empty();
  }

  @Override
  public Optional<vadl.viam.Definition> visit(AsmGrammarRuleDefinition definition) {
    var id = generateIdentifier(definition.viamId, definition.identifier());
    if (definition.isTerminalRule) {
      var literal =
          Objects.requireNonNull(definition.alternatives.alternatives.get(0).get(0).asmLiteral);
      var stringValue = Objects.requireNonNull((StringLiteral) literal.stringLiteral).value;
      return Optional.of(new AsmTerminalRule(id, stringValue));
    }

    if (definition.isBuiltinRule) {
      return Optional.of(new AsmBuiltinRule(id));
    }

    return Optional.of(
        new AsmNonTerminalRule(id, visitAsmAlternatives(definition.alternatives, false),
            definition.sourceLocation())
    );
  }

  private AsmAlternatives visitAsmAlternatives(AsmGrammarAlternativesDefinition definition,
                                               boolean isWithinOptionOrRepetition) {
    var alternatives = definition.alternatives;
    var semanticPredicateApplies = !isWithinOptionOrRepetition || alternatives.size() != 1;
    return new AsmAlternatives(alternatives.stream()
        .map(alternative -> visitAsmAlternative(alternative, semanticPredicateApplies)).toList());
  }

  private AsmAlternative visitAsmAlternative(List<AsmGrammarElementDefinition> elements,
                                             boolean semanticPredicateAppliesToAlternatives) {
    Graph semanticPredicate = null;
    var semPredExpr = elements.get(0).semanticPredicate;
    if (semanticPredicateAppliesToAlternatives && semPredExpr != null) {
      semanticPredicate = behaviorLowering.getGraph(semPredExpr, "semanticPredicate");
    }
    var grammarElements =
        elements.stream().map(this::visitAsmElement).filter(Objects::nonNull).toList();
    return new AsmAlternative(semanticPredicate, grammarElements);
  }

  @Nullable
  private AsmGrammarElement visitAsmElement(AsmGrammarElementDefinition definition) {
    if (definition.optionAlternatives != null) {
      var semanticPredicate = potentialSemanticPredicate(definition.optionAlternatives);
      var alternatives = visitAsmAlternatives(definition.optionAlternatives, true);
      return new AsmOption(semanticPredicate, alternatives);
    }

    if (definition.repetitionAlternatives != null) {
      var semanticPredicate = potentialSemanticPredicate(definition.repetitionAlternatives);
      var alternatives = visitAsmAlternatives(definition.repetitionAlternatives, true);
      return new AsmRepetition(semanticPredicate, alternatives);
    }

    if (definition.groupAlternatives != null) {
      var alternatives = visitAsmAlternatives(definition.groupAlternatives, false);
      return new AsmGroup(alternatives);
    }

    AsmAssignTo assignTo = null;
    if (definition.attribute != null) {
      assignTo = definition.isAttributeLocalVar
          ? new AsmAssignToLocalVar(definition.attribute.name)
          : new AsmAssignToAttribute(definition.attribute.name);
    }

    if (definition.localVar != null) {
      AsmGrammarElement literal = null;
      if (definition.localVar.asmLiteral.id == null
          || !definition.localVar.asmLiteral.id.name.equals("null")) {
        literal = visitAsmLiteral(assignTo, definition.localVar.asmLiteral);
      }
      return new AsmLocalVarDefinition(definition.localVar.id.name, literal);
    }

    if (definition.asmLiteral != null) {
      return visitAsmLiteral(assignTo, definition.asmLiteral);
    }

    return null;
  }

  @Nullable
  private Graph potentialSemanticPredicate(AsmGrammarAlternativesDefinition definition) {
    Graph semanticPredicate = null;
    var semPredExpr = definition.alternatives.get(0).get(0).semanticPredicate;

    if (definition.alternatives.size() == 1 && semPredExpr != null) {
      semanticPredicate = behaviorLowering.getGraph(semPredExpr, "semanticPredicate");
    }
    return semanticPredicate;
  }

  @Nullable
  private AsmGrammarElement visitAsmLiteral(@Nullable AsmAssignTo assignToElement,
                                            AsmGrammarLiteralDefinition definition) {
    if (definition.stringLiteral != null) {
      var stringValue = ((StringLiteral) definition.stringLiteral).value;
      return new AsmStringLiteralUse(assignToElement, stringValue);
    }

    Objects.requireNonNull(definition.id);
    var invocationSymbolOrigin = definition.symbolTable().resolveNode(definition.id.name);

    if (invocationSymbolOrigin instanceof AsmGrammarLocalVarDefinition) {
      return new AsmLocalVarUse(assignToElement, definition.id.name);
    }

    if (invocationSymbolOrigin instanceof FunctionDefinition functionDefinition) {
      // TODO: store reference to function instead of function identifier
      //       once function lowering is supported
      // var function = fetch(functionDefinition).orElseThrow();
      var parameters = definition.parameters.stream()
          .map(param -> visitAsmLiteral(null, param)).toList();
      return new AsmFunctionInvocation(assignToElement, definition.id.name, parameters);
    }

    if (invocationSymbolOrigin instanceof AsmGrammarRuleDefinition ruleDefinition) {
      var rule = (AsmGrammarRule) fetch(ruleDefinition).orElseThrow();
      var parameters = definition.parameters.stream()
          .map(param -> visitAsmLiteral(null, param)).toList();
      return new AsmRuleInvocation(assignToElement, rule, parameters);
    }

    return null;
  }

  @Override
  public Optional<vadl.viam.Definition> visit(AsmGrammarTypeDefinition definition) {
    // Do nothing on purpose.
    // The typechecker already resolved all types they are no longer needed.
    return Optional.empty();
  }

  @Override
  public Optional<vadl.viam.Definition> visit(AsmModifierDefinition definition) {
    var relocationDefinition =
        definition.symbolTable().findAs(definition.relocation, RelocationDefinition.class);

    Objects.requireNonNull(relocationDefinition);
    var relocation = (Relocation) fetch(relocationDefinition).orElseThrow();
    var id = ((StringLiteral) definition.stringLiteral).value;

    return Optional.of(
        new AsmModifier(generateIdentifier(id, definition), relocation,
            definition.sourceLocation()));
  }

  @Override
  public Optional<vadl.viam.Definition> visit(AssemblyDefinition definition) {
    // Do nothing on purpose.
    // Assembly definitions are visited as part of the instruction as this also does reflect
    // better the structure in the viam.
    // You can look at visitAssembly where it is implemented.
    return Optional.empty();
  }

  @Override
  public Optional<vadl.viam.Definition> visit(CacheDefinition definition) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        definition.getClass().getSimpleName()));
  }

  @Override
  public Optional<vadl.viam.Definition> visit(ConstantDefinition definition) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        definition.getClass().getSimpleName()));
  }

  @Override
  public Optional<vadl.viam.Definition> visit(CounterDefinition definition) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        definition.getClass().getSimpleName()));
  }

  @Override
  public Optional<vadl.viam.Definition> visit(CpuFunctionDefinition definition) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        definition.getClass().getSimpleName()));
  }

  @Override
  public Optional<vadl.viam.Definition> visit(CpuProcessDefinition definition) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        definition.getClass().getSimpleName()));
  }

  @Override
  public Optional<vadl.viam.Definition> visit(DefinitionList definition) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        definition.getClass().getSimpleName()));
  }

  @Override
  public Optional<vadl.viam.Definition> visit(EncodingDefinition definition) {
    // Do nothing on purpose.
    // Encoding definitions are visited as part of the instruction as this also does reflect
    // better the structure in the viam.
    // You can look at visitEncoding where it is implemented.
    return Optional.empty();
  }

  @Override
  public Optional<vadl.viam.Definition> visit(EnumerationDefinition definition) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        definition.getClass().getSimpleName()));
  }

  @Override
  public Optional<vadl.viam.Definition> visit(ExceptionDefinition definition) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        definition.getClass().getSimpleName()));
  }

  @Override
  public Optional<vadl.viam.Definition> visit(FormatDefinition definition) {
    var format =
        new Format(generateIdentifier(definition.viamId, definition.identifier()),
            (BitsType) Objects.requireNonNull(definition.type.type));

    var fields = new ArrayList<Format.Field>();
    var fieldAccesses = new ArrayList<Format.FieldAccess>();
    for (var fieldDefinition : definition.fields) {

      if (fieldDefinition instanceof FormatDefinition.TypedFormatField typedField) {
        var field = new Format.Field(
            generateIdentifier(definition.viamId + "::" + fieldDefinition.identifier().name,
                fieldDefinition.identifier()),
            (BitsType) Objects.requireNonNull(typedField.typeLiteral.type),
            new Constant.BitSlice(new Constant.BitSlice.Part[] {
                new Constant.BitSlice.Part(
                    Objects.requireNonNull(typedField.range).from(),
                    Objects.requireNonNull(typedField.range).to())}),
            format
        );
        formatFieldCache.put(typedField, field);
        fields.add(field);
        continue;
      }

      if (fieldDefinition instanceof FormatDefinition.RangeFormatField rangeField) {
        var field = new Format.Field(
            generateIdentifier(definition.viamId + "::" + fieldDefinition.identifier().name,
                fieldDefinition.identifier()),
            (BitsType) Objects.requireNonNull(rangeField.type),
            new Constant.BitSlice(Objects.requireNonNull(rangeField.computedRanges).stream()
                .map(r -> new Constant.BitSlice.Part(r.from(), r.to()))
                .toArray(Constant.BitSlice.Part[]::new)),
            format
        );
        fields.add(field);
        formatFieldCache.put(rangeField, field);
        continue;
      }

      if (fieldDefinition instanceof FormatDefinition.DerivedFormatField derivedField) {
        // FIXME: Implement this and figure out what the predicate does here?
        continue;
      }

      throw new IllegalStateException(
          "Don't know how to generate fields for " + fieldDefinition.getClass());
    }

    format.setFields(fields.toArray(new Format.Field[0]));
    format.setFieldAccesses(fieldAccesses.toArray(new Format.FieldAccess[0]));
    return Optional.of(format);
  }

  @Override
  public Optional<vadl.viam.Definition> visit(FunctionDefinition definition) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        definition.getClass().getSimpleName()));
  }

  @Override
  public Optional<vadl.viam.Definition> visit(GroupDefinition definition) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        definition.getClass().getSimpleName()));
  }

  @Override
  public Optional<vadl.viam.Definition> visit(ImportDefinition definition) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        definition.getClass().getSimpleName()));
  }

  private Assembly visitAssembly(AssemblyDefinition definition,
                                 InstructionDefinition instructionDefinition) {

    var identifierLoc = definition.identifiers.stream()
        .map(i -> (Identifier) i)
        .filter(i -> i.name.equals(instructionDefinition.identifier().name))
        .findFirst().orElseThrow().location();
    var identifierName = instructionDefinition.viamId + "::assembly";
    var funcIdentifier =
        new vadl.viam.Identifier(identifierName + "::func", identifierLoc);

    var behavior = behaviorLowering.getGraph(definition.expr, funcIdentifier.name());

    // FIXME: Add to cache? But how, because one assemby ast node might be used for multiple
    // assembly in the VIAM.

    return new Assembly(
        new vadl.viam.Identifier(identifierName, identifierLoc),
        new Function(funcIdentifier, new Parameter[0], Type.string(), behavior)
    );
  }

  private Encoding visitEncoding(EncodingDefinition definition,
                                 InstructionDefinition instructionDefinition) {
    var fields = new ArrayList<Encoding.Field>();
    for (var item : definition.encodings.items) {
      var encodingDef = (EncodingDefinition.EncodingField) item;
      var formatField = fetch(Objects.requireNonNull(definition.formatNode)
          .getField(encodingDef.field.name)).orElseThrow();
      var identifier =
          generateIdentifier(definition.viamId + "::encoding::" + encodingDef.field.name,
              encodingDef.field);

      // FIXME: Maybe cache it in the AST after typechecking?
      var evaluated = constantEvaluator.eval(encodingDef.value);
      var field = new Encoding.Field(identifier, formatField, evaluated.toViamConstant());
      fields.add(field);
    }

    // FIXME: Add to cache?

    return new Encoding(
        generateIdentifier(instructionDefinition.viamId + "::encoding", definition.identifier()),
        (Format) fetch(Objects.requireNonNull(definition.formatNode)).orElseThrow(),
        fields.toArray(new Encoding.Field[0])
    );
  }

  @Override
  public Optional<vadl.viam.Definition> visit(InstructionDefinition definition) {
    var behavior = behaviorLowering.getInstructionGraph(definition);

    var assembly = visitAssembly(Objects.requireNonNull(definition.assemblyDefinition),
        definition);
    var encoding =
        visitEncoding(Objects.requireNonNull(definition.encodingDefinition), definition);

    var instruction = new Instruction(
        generateIdentifier(definition.viamId, definition.identifier()),
        behavior,
        assembly,
        encoding
    );
    return Optional.of(instruction);
  }

  @Override
  public Optional<vadl.viam.Definition> visit(InstructionSetDefinition definition) {
    var identifier = generateIdentifier(definition.viamId, definition.identifier());

    var allDefinitions =
        definition.definitions.stream().map(this::fetch).flatMap(Optional::stream).toList();
    var formats = filterAndCastToInstance(allDefinitions, Format.class);
    var functions = filterAndCastToInstance(allDefinitions, Function.class);
    var relocations = filterAndCastToInstance(allDefinitions, Relocation.class);
    var instructions = filterAndCastToInstance(allDefinitions, Instruction.class);
    var pseudoInstructions = filterAndCastToInstance(allDefinitions, PseudoInstruction.class);
    var registers = filterAndCastToInstance(allDefinitions, Register.class);
    var registerFiles = filterAndCastToInstance(allDefinitions, RegisterFile.class);
    var programCounter = allDefinitions.stream()
        .filter(d -> d instanceof Counter && ((Counter) d).kind() == Counter.Kind.PROGRAM_COUNTER)
        .map(v -> (Counter) v)
        .findFirst().orElse(null);
    var memories = filterAndCastToInstance(allDefinitions, Memory.class);

    return Optional.of(new vadl.viam.InstructionSetArchitecture(
        identifier,
        currentSpecification,
        formats,
        functions,
        relocations,
        instructions,
        pseudoInstructions,
        registers,
        registerFiles,
        programCounter,
        memories
    ));
  }

  @Override
  public Optional<vadl.viam.Definition> visit(LogicDefinition definition) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        definition.getClass().getSimpleName()));
  }

  @Override
  public Optional<vadl.viam.Definition> visit(MacroInstanceDefinition definition) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        definition.getClass().getSimpleName()));
  }

  @Override
  public Optional<vadl.viam.Definition> visit(MacroInstructionDefinition definition) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        definition.getClass().getSimpleName()));
  }

  @Override
  public Optional<vadl.viam.Definition> visit(MacroMatchDefinition definition) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        definition.getClass().getSimpleName()));
  }

  @Override
  public Optional<vadl.viam.Definition> visit(MemoryDefinition definition) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        definition.getClass().getSimpleName()));
  }

  @Override
  public Optional<vadl.viam.Definition> visit(MicroArchitectureDefinition definition) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        definition.getClass().getSimpleName()));
  }

  @Override
  public Optional<vadl.viam.Definition> visit(MicroProcessorDefinition definition) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        definition.getClass().getSimpleName()));
  }

  @Override
  public Optional<vadl.viam.Definition> visit(ModelDefinition definition) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        definition.getClass().getSimpleName()));
  }

  @Override
  public Optional<vadl.viam.Definition> visit(ModelTypeDefinition definition) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        definition.getClass().getSimpleName()));
  }

  @Override
  public Optional<vadl.viam.Definition> visit(OperationDefinition definition) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        definition.getClass().getSimpleName()));
  }

  @Override
  public Optional<vadl.viam.Definition> visit(PatchDefinition definition) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        definition.getClass().getSimpleName()));
  }

  @Override
  public Optional<vadl.viam.Definition> visit(PipelineDefinition definition) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        definition.getClass().getSimpleName()));
  }

  @Override
  public Optional<vadl.viam.Definition> visit(PlaceholderDefinition definition) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        definition.getClass().getSimpleName()));
  }

  @Override
  public Optional<vadl.viam.Definition> visit(PortBehaviorDefinition definition) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        definition.getClass().getSimpleName()));
  }

  @Override
  public Optional<vadl.viam.Definition> visit(ProcessDefinition definition) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        definition.getClass().getSimpleName()));
  }

  @Override
  public Optional<vadl.viam.Definition> visit(PseudoInstructionDefinition definition) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        definition.getClass().getSimpleName()));
  }

  @Override
  public Optional<vadl.viam.Definition> visit(RecordTypeDefinition definition) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        definition.getClass().getSimpleName()));
  }

  @Override
  public Optional<vadl.viam.Definition> visit(RegisterDefinition definition) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        definition.getClass().getSimpleName()));
  }

  @Override
  public Optional<vadl.viam.Definition> visit(RegisterFileDefinition definition) {
    // FIXME: Add proper constraints
    var regFile = new RegisterFile(
        generateIdentifier(definition.viamId, definition.identifier()),
        (DataType) Objects.requireNonNull(definition.type).argTypes().get(0),
        (DataType) Objects.requireNonNull(definition.type).resultType(),
        new RegisterFile.Constraint[0]
    );
    return Optional.of(regFile);
  }

  @Override
  public Optional<vadl.viam.Definition> visit(RelocationDefinition definition) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        definition.getClass().getSimpleName()));
  }

  @Override
  public Optional<vadl.viam.Definition> visit(SignalDefinition definition) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        definition.getClass().getSimpleName()));
  }

  @Override
  public Optional<vadl.viam.Definition> visit(SourceDefinition definition) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        definition.getClass().getSimpleName()));
  }

  @Override
  public Optional<vadl.viam.Definition> visit(SpecialPurposeRegisterDefinition definition) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        definition.getClass().getSimpleName()));
  }

  @Override
  public Optional<vadl.viam.Definition> visit(StageDefinition definition) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        definition.getClass().getSimpleName()));
  }

  @Override
  public Optional<vadl.viam.Definition> visit(UsingDefinition definition) {
    // Do nothing on purpose.
    // The typechecker already resolved all types they are no longer needed.
    return Optional.empty();
  }
}
