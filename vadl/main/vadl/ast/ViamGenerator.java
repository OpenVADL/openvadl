package vadl.ast;


import com.google.errorprone.annotations.concurrent.LazyInit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import vadl.error.Diagnostic;
import vadl.types.BitsType;
import vadl.types.DataType;
import vadl.types.Type;
import vadl.utils.SourceLocation;
import vadl.viam.Assembly;
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
import vadl.viam.graph.Graph;

/**
 * The lowering that converts the AST to the VIAM.
 */
public class ViamGenerator
    implements DefinitionVisitor<Optional<vadl.viam.Definition>>, StatementVisitor<Graph>,
    ExprVisitor<Graph> {

  private final ConstantEvaluator constantEvaluator = new ConstantEvaluator();

  final private IdentityHashMap<Definition, Optional<vadl.viam.Definition>> definitionCache =
      new IdentityHashMap<>();

  @LazyInit
  private vadl.viam.Specification currentSpecification;

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
    // FIXME: Add name to specification.
    var spec = new Specification(
        new vadl.viam.Identifier("Name not yet in AST", SourceLocation.INVALID_SOURCE_LOCATION));
    this.currentSpecification = spec;

    spec.addAll(ast.definitions.stream()
        .map(this::fetch)
        .flatMap(Optional::stream)
        .collect(Collectors.toList()));
    return spec;
  }

  private vadl.viam.Identifier generateIdentifier(Identifier identifier) {
    return new vadl.viam.Identifier(identifier.toString(), SourceLocation.INVALID_SOURCE_LOCATION);
  }

  /**
   * Fetch from the cache the viam node or evaluate it.
   *
   * @param definition for which we want to find the corresponding viam node.
   * @return the viam node.
   */
  private Optional<vadl.viam.Definition> fetch(Definition definition) {
    if (definitionCache.containsKey(definition)) {
      return definitionCache.get(definition);
    }

    var result = definition.accept(this);
    definitionCache.put(definition, result);
    return result;
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
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        definition.getClass().getSimpleName()));
  }

  @Override
  public Optional<vadl.viam.Definition> visit(AsmDirectiveDefinition definition) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        definition.getClass().getSimpleName()));
  }

  @Override
  public Optional<vadl.viam.Definition> visit(AsmGrammarAlternativesDefinition definition) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        definition.getClass().getSimpleName()));
  }

  @Override
  public Optional<vadl.viam.Definition> visit(AsmGrammarElementDefinition definition) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        definition.getClass().getSimpleName()));
  }

  @Override
  public Optional<vadl.viam.Definition> visit(AsmGrammarLiteralDefinition definition) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        definition.getClass().getSimpleName()));
  }

  @Override
  public Optional<vadl.viam.Definition> visit(AsmGrammarLocalVarDefinition definition) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        definition.getClass().getSimpleName()));
  }

  @Override
  public Optional<vadl.viam.Definition> visit(AsmGrammarRuleDefinition definition) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        definition.getClass().getSimpleName()));
  }

  @Override
  public Optional<vadl.viam.Definition> visit(AsmGrammarTypeDefinition definition) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        definition.getClass().getSimpleName()));
  }

  @Override
  public Optional<vadl.viam.Definition> visit(AsmModifierDefinition definition) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        definition.getClass().getSimpleName()));
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
        new Format(generateIdentifier(definition.identifier()),
            (BitsType) Objects.requireNonNull(definition.type.type));

    var fields = new ArrayList<Format.Field>();
    var fieldAccesses = new ArrayList<Format.FieldAccess>();
    for (var fieldDefinition : definition.fields) {

      if (fieldDefinition instanceof FormatDefinition.TypedFormatField typedField) {
        fields.add(new Format.Field(
            generateIdentifier(fieldDefinition.identifier()),
            (BitsType) Objects.requireNonNull(typedField.typeLiteral.type),
            new Constant.BitSlice(new Constant.BitSlice.Part[] {
                new Constant.BitSlice.Part(
                    Objects.requireNonNull(typedField.range).from(),
                    Objects.requireNonNull(typedField.range).to())}),
            format
        ));
        continue;
      }

      if (fieldDefinition instanceof FormatDefinition.RangeFormatField rangeField) {
        fields.add(new Format.Field(
            generateIdentifier(fieldDefinition.identifier()),
            (BitsType) Objects.requireNonNull(rangeField.type),
            new Constant.BitSlice(Objects.requireNonNull(rangeField.computedRanges).stream()
                .map(r -> new Constant.BitSlice.Part(r.from(), r.to()))
                .toArray(Constant.BitSlice.Part[]::new)),
            format
        ));
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

  private Assembly visitAssembly(AssemblyDefinition definition, String instructionName) {
    var identifier = generateIdentifier(definition.identifiers.stream()
        .map(i -> (Identifier) i)
        .filter(i -> i.name.equals(instructionName))
        .findFirst().orElseThrow());

    var behaivor = definition.expr.accept(this);

    return new Assembly(
        identifier,
        new Function(identifier, new Parameter[0], Type.string(), behaivor)
    );
  }

  private Encoding visitEncoding(EncodingDefinition definition) {
    var fields = new ArrayList<Encoding.Field>();
    for (var item : definition.encodings.items) {
      var encodingDef = (EncodingDefinition.EncodingField) item;

      var identifier = generateIdentifier(encodingDef.field);
      var formatField =
          Arrays.stream(((Format) fetch(
                  Objects.requireNonNull(definition.formatNode)).orElseThrow()).fields())
              .filter(f -> f.identifier.name().equals(identifier.name())).findFirst().orElseThrow();

      // FIXME: Maybe cache it in the AST after typechecking?
      var evaluated = constantEvaluator.eval(encodingDef.value);
      var field = new Encoding.Field(identifier, formatField, evaluated.toViamConstant());
      fields.add(field);
    }


    return new Encoding(
        generateIdentifier(definition.identifier()),
        (Format) fetch(Objects.requireNonNull(definition.formatNode)).orElseThrow(),
        fields.toArray(new Encoding.Field[0])
    );
  }

  @Override
  public Optional<vadl.viam.Definition> visit(InstructionDefinition definition) {
    var behaivor = definition.behavior.accept(this);
    var assembly = visitAssembly(Objects.requireNonNull(definition.assemblyDefinition),
        definition.identifier().name);
    var encoding = visitEncoding(Objects.requireNonNull(definition.encodingDefinition));

    var instruction = new Instruction(
        generateIdentifier(definition.identifier()),
        behaivor,
        assembly,
        encoding
    );
    return Optional.of(instruction);
  }

  @Override
  public Optional<vadl.viam.Definition> visit(InstructionSetDefinition definition) {
    var identifier = generateIdentifier(definition.identifier());

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
        generateIdentifier(definition.identifier()),
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

  @Override
  public Graph visit(Identifier expr) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        expr.getClass().getSimpleName()));
  }

  @Override
  public Graph visit(BinaryExpr expr) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        expr.getClass().getSimpleName()));
  }

  @Override
  public Graph visit(GroupedExpr expr) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        expr.getClass().getSimpleName()));
  }

  @Override
  public Graph visit(IntegerLiteral expr) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        expr.getClass().getSimpleName()));
  }

  @Override
  public Graph visit(BinaryLiteral expr) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        expr.getClass().getSimpleName()));
  }

  @Override
  public Graph visit(BoolLiteral expr) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        expr.getClass().getSimpleName()));
  }

  @Override
  public Graph visit(StringLiteral expr) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        expr.getClass().getSimpleName()));
  }

  @Override
  public Graph visit(PlaceholderExpr expr) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        expr.getClass().getSimpleName()));
  }

  @Override
  public Graph visit(MacroInstanceExpr expr) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        expr.getClass().getSimpleName()));
  }

  @Override
  public Graph visit(RangeExpr expr) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        expr.getClass().getSimpleName()));
  }

  @Override
  public Graph visit(TypeLiteral expr) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        expr.getClass().getSimpleName()));
  }

  @Override
  public Graph visit(IdentifierPath expr) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        expr.getClass().getSimpleName()));
  }

  @Override
  public Graph visit(UnaryExpr expr) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        expr.getClass().getSimpleName()));
  }

  @Override
  public Graph visit(CallExpr expr) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        expr.getClass().getSimpleName()));
  }

  @Override
  public Graph visit(IfExpr expr) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        expr.getClass().getSimpleName()));
  }

  @Override
  public Graph visit(LetExpr expr) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        expr.getClass().getSimpleName()));
  }

  @Override
  public Graph visit(CastExpr expr) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        expr.getClass().getSimpleName()));
  }

  @Override
  public Graph visit(SymbolExpr expr) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        expr.getClass().getSimpleName()));
  }

  @Override
  public Graph visit(MacroMatchExpr expr) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        expr.getClass().getSimpleName()));
  }

  @Override
  public Graph visit(MatchExpr expr) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        expr.getClass().getSimpleName()));
  }

  @Override
  public Graph visit(ExtendIdExpr expr) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        expr.getClass().getSimpleName()));
  }

  @Override
  public Graph visit(IdToStrExpr expr) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        expr.getClass().getSimpleName()));
  }

  @Override
  public Graph visit(ExistsInExpr expr) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        expr.getClass().getSimpleName()));
  }

  @Override
  public Graph visit(ExistsInThenExpr expr) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        expr.getClass().getSimpleName()));
  }

  @Override
  public Graph visit(ForallThenExpr expr) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        expr.getClass().getSimpleName()));
  }

  @Override
  public Graph visit(ForallExpr expr) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        expr.getClass().getSimpleName()));
  }

  @Override
  public Graph visit(SequenceCallExpr expr) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        expr.getClass().getSimpleName()));
  }

  @Override
  public Graph visit(AssignmentStatement statement) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        statement.getClass().getSimpleName()));
  }

  @Override
  public Graph visit(BlockStatement statement) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        statement.getClass().getSimpleName()));
  }

  @Override
  public Graph visit(CallStatement statement) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        statement.getClass().getSimpleName()));
  }

  @Override
  public Graph visit(ForallStatement statement) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        statement.getClass().getSimpleName()));
  }

  @Override
  public Graph visit(IfStatement statement) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        statement.getClass().getSimpleName()));
  }

  @Override
  public Graph visit(InstructionCallStatement statement) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        statement.getClass().getSimpleName()));
  }

  @Override
  public Graph visit(LetStatement statement) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        statement.getClass().getSimpleName()));
  }

  @Override
  public Graph visit(LockStatement statement) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        statement.getClass().getSimpleName()));
  }

  @Override
  public Graph visit(MacroInstanceStatement statement) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        statement.getClass().getSimpleName()));
  }

  @Override
  public Graph visit(MacroMatchStatement statement) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        statement.getClass().getSimpleName()));
  }

  @Override
  public Graph visit(MatchStatement statement) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        statement.getClass().getSimpleName()));
  }

  @Override
  public Graph visit(PlaceholderStatement statement) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        statement.getClass().getSimpleName()));
  }

  @Override
  public Graph visit(RaiseStatement statement) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        statement.getClass().getSimpleName()));
  }

  @Override
  public Graph visit(StatementList statement) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        statement.getClass().getSimpleName()));
  }
}
