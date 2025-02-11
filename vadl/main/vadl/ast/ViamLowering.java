package vadl.ast;


import com.google.errorprone.annotations.concurrent.LazyInit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
import vadl.viam.InstructionSetArchitecture;
import vadl.viam.Memory;
import vadl.viam.MicroProcessor;
import vadl.viam.PseudoInstruction;
import vadl.viam.Register;
import vadl.viam.RegisterFile;
import vadl.viam.Relocation;
import vadl.viam.Specification;
import vadl.viam.annotations.AsmParserCaseSensitive;
import vadl.viam.annotations.AsmParserCommentString;
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
  private final IdentityHashMap<FormatDefinition.FormatField, vadl.viam.Definition>
      formatFieldCache = new IdentityHashMap<>();
  private final IdentityHashMap<Parameter, vadl.viam.Parameter>
      parameterCache = new IdentityHashMap<>();


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
    result.ifPresent(value -> value.setSourceLocationIfNotSet(definition.sourceLocation()));
    definitionCache.put(definition, result);
    return result;
  }

  /**
   * Fetch from the cache the format field node or evaluate it.
   *
   * @param field for which we want to find the corresponding viam node.
   * @return the viam node.
   */
  Optional<vadl.viam.Definition> fetch(FormatDefinition.FormatField field) {
    // FIXME: Try to evaluate the format if it hasn't been seen before.
    var result = Optional.ofNullable(formatFieldCache.get(field));
    result.ifPresent(f -> f.setSourceLocationIfNotSet(field.sourceLocation()));
    return result;
  }

  /**
   * Fetch from the cache the format field node or evaluate it.
   *
   * @param parameter for which we want to find the corresponding viam node.
   * @return the viam node.
   */
  Optional<vadl.viam.Parameter> fetch(Parameter parameter) {
    // FIXME: Try to evaluate the format if it hasn't been seen before.
    var result = Optional.ofNullable(parameterCache.get(parameter));
    result.ifPresent(f -> f.setSourceLocationIfNotSet(parameter.sourceLocation()));
    return result;
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
    var parts = viamId.split("::");
    return new vadl.viam.Identifier(parts, locatable.sourceLocation());
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
    // FIXME: Generate ABI
    return Optional.empty();
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

    var asmDescription =
        new AssemblyDescription(id, modifiers, directives, rules, commonDefinitions);

    lowerAsmDescriptionAnnotations(definition, asmDescription);

    return Optional.of(asmDescription);
  }

  private static void lowerAsmDescriptionAnnotations(AsmDescriptionDefinition definition,
                                                     AssemblyDescription asmDescription) {
    // FIXME: integrate with general annotation lowering once it is implemented
    definition.annotations.annotations().forEach(
        annotation -> {
          // annotations of the form [ A = B ]
          if (annotation.expr() instanceof BinaryExpr binaryExpr
              && binaryExpr.operator() == Operator.Equal) {

            if (binaryExpr.left instanceof Identifier annoId) {
              var annoName = annoId.name;

              // [ commentString = ";" ]
              if (binaryExpr.right instanceof StringLiteral string
                  && annoName.equals("commentString") && !string.value.isEmpty()) {
                asmDescription.addAnnotation(new AsmParserCommentString(string.value));
                return;
              }

              // [ caseSensitive = true ]
              if (binaryExpr.right instanceof BoolLiteral bool
                  && annoName.equals("caseSensitive")) {
                asmDescription.addAnnotation(new AsmParserCaseSensitive(bool.value));
                return;
              }
            }
          }
          throw Diagnostic.error("Unknown annotation on assembly description", definition).build();
        }
    );
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
    // Do nothing on purpose.
    // Constants are folded in the lowering and are not translated to VIAM.
    return Optional.empty();
  }

  @Override
  public Optional<vadl.viam.Definition> visit(CounterDefinition definition) {
    var identifier = generateIdentifier(definition.viamId, definition.identifier().location());

    // FIXME: Further research for the parameters (probably don't apply to counter)
    var reg = new Register(identifier,
        (DataType) Objects.requireNonNull(definition.typeLiteral.type),
        Register.AccessKind.FULL,
        Register.AccessKind.FULL,
        null,
        new Register[] {});

    Map<CounterDefinition.CounterKind, Counter.Kind> kinds =
        Map.of(CounterDefinition.CounterKind.PROGRAM, Counter.Kind.PROGRAM_COUNTER,
            CounterDefinition.CounterKind.GROUP, Counter.Kind.GROUP_COUNTER);
    var kind = Objects.requireNonNull(kinds.get(definition.kind));
    var counter = new Counter.RegisterCounter(identifier,
        reg,
        Counter.Position.CURRENT, //FIXME: read this from, annotation or somewhere?
        kind);
    return Optional.of(counter);
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
            new Constant.BitSlice(new Constant.BitSlice.Part(
                Objects.requireNonNull(typedField.range).from(),
                Objects.requireNonNull(typedField.range).to())),
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
        var identifier =
            generateIdentifier(definition.viamId + "::" + fieldDefinition.identifier().name,
                fieldDefinition.identifier());

        var accessName = identifier.name() + "::decode";
        var accessGraph =
            behaviorLowering.getGraph(derivedField.expr, accessName);
        var access =
            new Function(generateIdentifier(accessName, derivedField.identifier),
                new vadl.viam.Parameter[0],
                Objects.requireNonNull(derivedField.expr.type), accessGraph);

        // FIXME: Add encoding from language
        @Nullable Function encoding = null;


        // FIXME: Add real predicates
        var predicateName = identifier.name() + "::predicate";
        var predicateGraph =
            behaviorLowering.getGraph(new BoolLiteral(true, SourceLocation.INVALID_SOURCE_LOCATION),
                predicateName);

        var parameter = new vadl.viam.Parameter(
            new vadl.viam.Identifier(fieldDefinition.identifier().name,
                SourceLocation.INVALID_SOURCE_LOCATION),
            Objects.requireNonNull(derivedField.expr.type));
        var predicate = new Function(
            generateIdentifier(predicateName, derivedField.identifier),
            new vadl.viam.Parameter[] {parameter}, Type.bool(), predicateGraph
        );


        var field = new Format.FieldAccess(identifier, access, encoding, predicate);
        fieldAccesses.add(field);
        formatFieldCache.put(derivedField, field);
        continue;
      }

      throw new IllegalStateException(
          "Don't know how to generate fields for " + fieldDefinition.getClass());
    }

    format.setFields(fields.toArray(new Format.Field[0]));
    format.setFieldAccesses(
        fieldAccesses.toArray(fieldAccesses.toArray(new Format.FieldAccess[0])));
    return Optional.of(format);
  }

  @Override
  public Optional<vadl.viam.Definition> visit(FunctionDefinition definition) {
    var identifier = generateIdentifier(definition.viamId, definition.identifier());
    var parameters = new ArrayList<vadl.viam.Parameter>();
    for (var parameter : definition.params) {
      var viamParameter = new vadl.viam.Parameter(
          generateIdentifier(parameter.name.name, parameter.name.location()),
          Objects.requireNonNull(parameter.typeLiteral.type));
      parameterCache.put(parameter, viamParameter);
      parameters.add(viamParameter);
    }
    var behaivor = behaviorLowering.getGraph(definition.expr, "behaviour");

    return Optional.of(new Function(identifier,
        parameters.toArray(new vadl.viam.Parameter[0]),
        Objects.requireNonNull(definition.retType.type),
        behaivor));
  }

  @Override
  public Optional<vadl.viam.Definition> visit(GroupDefinition definition) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        definition.getClass().getSimpleName()));
  }

  @Override
  public Optional<vadl.viam.Definition> visit(ImportDefinition definition) {
    // Do nothing on purpose.
    // The symboltable should have already resolved everything.
    return Optional.empty();
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
        new Function(funcIdentifier, new vadl.viam.Parameter[0], Type.string(), behavior)
    );
  }

  private Encoding visitEncoding(EncodingDefinition definition,
                                 InstructionDefinition instructionDefinition) {
    var fields = new ArrayList<Encoding.Field>();
    for (var item : definition.encodings.items) {
      var encodingDef = (EncodingDefinition.EncodingField) item;
      var formatField = (Format.Field) fetch(Objects.requireNonNull(definition.formatNode)
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

  private InstructionSetArchitecture visitIsa(InstructionSetDefinition definition) {
    var identifier = generateIdentifier(definition.identifier().name, definition.identifier());

    // FIXME: make this togroup instead of toList
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

    return new vadl.viam.InstructionSetArchitecture(
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
    );
  }

  @Override
  public Optional<vadl.viam.Definition> visit(InstructionSetDefinition definition) {
    // The ISA isn't directly lowered when we visit it.
    // This is because there can be multiple ISAs in the AST but only one in the VIAM and the
    // selection and lowering is driven by the MicroprocessorDefinition.
    return Optional.empty();
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
    var identifier = generateIdentifier(definition.viamId, definition.identifier());
    return Optional.of(new Memory(identifier,
        (DataType) Objects.requireNonNull(definition.addressTypeLiteral.type),
        (DataType) Objects.requireNonNull(definition.dataTypeLiteral.type)));
  }

  @Override
  public Optional<vadl.viam.Definition> visit(MicroArchitectureDefinition definition) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        definition.getClass().getSimpleName()));
  }

  @Override
  public Optional<vadl.viam.Definition> visit(MicroProcessorDefinition definition) {
    var identifier = generateIdentifier(definition.viamId, definition.identifier());
    // create emtpy list of ast definitions
    // for each isa in mip add definitions to definition list
    // create new isa ast node with list of definitions
    // visitIsa on created isa ast node
    var isa = visitIsa(mergeIsa(definition.implementedIsaNodes));
    return Optional.of(new MicroProcessor(identifier, isa, null, null, null));
  }

  private InstructionSetDefinition mergeIsa(List<InstructionSetDefinition> definitions) {

    Set<InstructionSetDefinition> processedIsas =
        Collections.newSetFromMap(new IdentityHashMap<>());
    var nodeList = new ArrayList<Definition>();

    for (var definition : definitions) {
      mergeInto(definition, nodeList, processedIsas);
    }

    var identifier = findIsaIdentifier(definitions);
    var location = findIsaLocation(definitions);

    // create new isa ast node
    return new InstructionSetDefinition(identifier, null, nodeList, location);
  }

  private void mergeInto(InstructionSetDefinition definition, List<Definition> nodeCollection,
                         Set<InstructionSetDefinition> processedIsas) {
    // check if ISA was already added
    if (processedIsas.contains(definition)) {
      return;
    }

    var extending = definition.extendingNode;
    if (extending != null) {
      mergeInto(extending, nodeCollection, processedIsas);
    }

    // add all definition nodes to node collection
    nodeCollection.addAll(definition.definitions);
    processedIsas.add(definition);
  }

  private Identifier findIsaIdentifier(List<InstructionSetDefinition> definitions) {
    // FIXME: If more than 1 isas, use some other identifier (from target annotation)
    return definitions.get(0).identifier();
  }

  private SourceLocation findIsaLocation(List<InstructionSetDefinition> definitions) {
    // FIXME: If more than 1 isas, use some other location (or invalid location)
    return definitions.get(0).sourceLocation();
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
    var identifier = generateIdentifier(definition.viamId, definition.identifier());
    var parameters = definition.params.stream()
        .map(p -> new vadl.viam.Parameter(
            generateIdentifier(p.name.name, p.name.location()),
            Objects.requireNonNull(p.typeLiteral.type)))
        .toArray(vadl.viam.Parameter[]::new);

    //var graph = behaviorLowering.getPseudoInstructionGraph(definition);
    //var assembly = fetch(definition.assemblyDefinition).orElseThrow();

    throw new RuntimeException();
//    return Optional.of(new PseudoInstruction(
//        identifier,
//        parameters,
//        //graph,
//        assembly
//    ));
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
    var identifier = generateIdentifier(definition.viamId, definition.identifier());
    var parameters = definition.params.stream()
        .map(p -> new vadl.viam.Parameter(generateIdentifier(p.name.name, p.name.location()),
            Objects.requireNonNull(p.typeLiteral.type)))
        .toArray(vadl.viam.Parameter[]::new);
    var graph = behaviorLowering.getGraph(definition.expr, identifier.name() + "::behavior");

    return Optional.of(
        new Relocation(
            identifier,
            parameters,
            Objects.requireNonNull(definition.resultTypeLiteral.type),
            graph));
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
