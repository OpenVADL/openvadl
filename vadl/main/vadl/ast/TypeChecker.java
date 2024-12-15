package vadl.ast;

import java.math.BigInteger;
import java.util.Objects;
import vadl.error.Diagnostic;
import vadl.types.BitsType;
import vadl.types.SIntType;
import vadl.types.Type;

/**
 * A experimental, temporary type-checker to verify expressions and attach types to the AST.
 *
 * <p>As the typesystem can depend on constants, the typechecker needs to evaluate (at least some
 * of) them.
 */
public class TypeChecker
    implements DefinitionVisitor<Void>, StatementVisitor<Void>, ExprVisitor<Void> {

  //private final List<Diagnostic> errors = new ArrayList<>();
  private final ConstantEvaluator constantEvaluator;

  public TypeChecker() {
    constantEvaluator = new ConstantEvaluator();
  }

  /**
   * Verify that the program is well-typed.
   *
   * @param ast to verify
   * @throws Diagnostic if the program isn't well typed
   */
  public void verify(Ast ast) {
    for (var definition : ast.definitions) {
      definition.accept(this);
    }
  }

  private void throwUnimplemented(Node node) {
    throw new RuntimeException(
        "The typechecker doesn't know how to handle `%s` yet, found in %s".formatted(
            node.getClass().getSimpleName(), node.location().toIDEString()));
  }

  /**
   * Tests whether a type can implicitly be cast to another.
   *
   * @param from is the source type.
   * @param to   is the target type.
   * @return true if the cast can happen implicitly, false otherwise.
   */
  private static boolean canImplicitCast(Type from, Type to) {
    if (from.equals(to)) {
      return true;
    }

    if (from instanceof ConstantType fromConstant) {
      if (to == Type.bool()) {
        return false;
      }

      if (to instanceof SIntType toSint) {
        var availableWidth = toSint.bitWidth();
        var value = fromConstant.getValue();
        var isNegative = value.compareTo(BigInteger.ZERO) < 0;
        var requiredWidth = value.bitLength() + (isNegative ? 1 : 0);

        return availableWidth >= requiredWidth;
      }
    }

    return false;
  }

  @Override
  public Void visit(ConstantDefinition definition) {
    definition.value.accept(this);

    if (definition.typeLiteral == null) {
      definition.type = definition.value.type;
    } else {
      definition.typeLiteral.accept(this);
      Type litType = Objects.requireNonNull(definition.typeLiteral.type);
      definition.type = litType;

      Type valType = Objects.requireNonNull(definition.value.type);
      if (!canImplicitCast(valType, litType)) {
        throw Diagnostic.error("Type missmatch: expected %s, got %s".formatted(litType, valType),
            definition.value.location()
        ).build();
      }

      // FIXME: Should I now insert a cast here?
    }

    return null;
  }

  @Override
  public Void visit(FormatDefinition definition) {
    throwUnimplemented(definition);
    return null;
  }

  @Override
  public Void visit(InstructionSetDefinition definition) {
    for (var def : definition.definitions) {
      def.accept(this);
    }
    return null;
  }

  @Override
  public Void visit(CounterDefinition definition) {
    throwUnimplemented(definition);
    return null;
  }

  @Override
  public Void visit(MemoryDefinition definition) {
    throwUnimplemented(definition);
    return null;
  }

  @Override
  public Void visit(RegisterDefinition definition) {
    throwUnimplemented(definition);
    return null;
  }

  @Override
  public Void visit(RegisterFileDefinition definition) {
    throwUnimplemented(definition);
    return null;
  }

  @Override
  public Void visit(InstructionDefinition definition) {
    throwUnimplemented(definition);
    return null;
  }

  @Override
  public Void visit(PseudoInstructionDefinition definition) {
    throwUnimplemented(definition);
    return null;
  }

  @Override
  public Void visit(RelocationDefinition definition) {
    throwUnimplemented(definition);
    return null;
  }

  @Override
  public Void visit(EncodingDefinition definition) {
    throwUnimplemented(definition);
    return null;
  }

  @Override
  public Void visit(AssemblyDefinition definition) {
    throwUnimplemented(definition);
    return null;
  }

  @Override
  public Void visit(UsingDefinition definition) {
    throwUnimplemented(definition);
    return null;
  }

  @Override
  public Void visit(FunctionDefinition definition) {
    throwUnimplemented(definition);
    return null;
  }

  @Override
  public Void visit(AliasDefinition definition) {
    throwUnimplemented(definition);
    return null;
  }

  @Override
  public Void visit(EnumerationDefinition definition) {
    throwUnimplemented(definition);
    return null;
  }

  @Override
  public Void visit(ExceptionDefinition definition) {
    throwUnimplemented(definition);
    return null;
  }

  @Override
  public Void visit(PlaceholderDefinition definition) {
    throwUnimplemented(definition);
    return null;
  }

  @Override
  public Void visit(MacroInstanceDefinition definition) {
    throwUnimplemented(definition);
    return null;
  }

  @Override
  public Void visit(MacroMatchDefinition definition) {
    throwUnimplemented(definition);
    return null;
  }

  @Override
  public Void visit(DefinitionList definition) {
    throwUnimplemented(definition);
    return null;
  }

  @Override
  public Void visit(ModelDefinition definition) {
    throwUnimplemented(definition);
    return null;
  }

  @Override
  public Void visit(RecordTypeDefinition definition) {
    throwUnimplemented(definition);
    return null;
  }

  @Override
  public Void visit(ModelTypeDefinition definition) {
    throwUnimplemented(definition);
    return null;
  }

  @Override
  public Void visit(ImportDefinition importDefinition) {
    throwUnimplemented(importDefinition);
    return null;
  }

  @Override
  public Void visit(ProcessDefinition processDefinition) {
    throwUnimplemented(processDefinition);
    return null;
  }

  @Override
  public Void visit(OperationDefinition operationDefinition) {
    throwUnimplemented(operationDefinition);
    return null;
  }

  @Override
  public Void visit(GroupDefinition groupDefinition) {
    throwUnimplemented(groupDefinition);
    return null;
  }

  @Override
  public Void visit(ApplicationBinaryInterfaceDefinition definition) {
    throwUnimplemented(definition);
    return null;
  }

  @Override
  public Void visit(AsmDescriptionDefinition definition) {
    throwUnimplemented(definition);
    return null;
  }

  @Override
  public Void visit(AsmDirectiveDefinition definition) {
    throwUnimplemented(definition);
    return null;
  }

  @Override
  public Void visit(AsmGrammarAlternativesDefinition definition) {
    throwUnimplemented(definition);
    return null;
  }

  @Override
  public Void visit(AsmGrammarElementDefinition definition) {
    throwUnimplemented(definition);
    return null;
  }

  @Override
  public Void visit(AsmGrammarLiteralDefinition definition) {
    throwUnimplemented(definition);
    return null;
  }

  @Override
  public Void visit(AsmGrammarLocalVarDefinition definition) {
    throwUnimplemented(definition);
    return null;
  }

  @Override
  public Void visit(AsmGrammarRuleDefinition definition) {
    throwUnimplemented(definition);
    return null;
  }

  @Override
  public Void visit(AsmGrammarTypeDefinition definition) {
    throwUnimplemented(definition);
    return null;
  }

  @Override
  public Void visit(AsmModifierDefinition definition) {
    throwUnimplemented(definition);
    return null;
  }

  @Override
  public Void visit(AbiSequenceDefinition definition) {
    throwUnimplemented(definition);
    return null;
  }

  @Override
  public Void visit(SpecialPurposeRegisterDefinition definition) {
    throwUnimplemented(definition);
    return null;
  }

  @Override
  public Void visit(MicroProcessorDefinition definition) {
    throwUnimplemented(definition);
    return null;
  }

  @Override
  public Void visit(PatchDefinition definition) {
    throwUnimplemented(definition);
    return null;
  }

  @Override
  public Void visit(SourceDefinition definition) {
    throwUnimplemented(definition);
    return null;
  }

  @Override
  public Void visit(CpuFunctionDefinition definition) {
    throwUnimplemented(definition);
    return null;
  }

  @Override
  public Void visit(CpuProcessDefinition definition) {
    throwUnimplemented(definition);
    return null;
  }

  @Override
  public Void visit(MicroArchitectureDefinition definition) {
    throwUnimplemented(definition);
    return null;
  }

  @Override
  public Void visit(MacroInstructionDefinition definition) {
    throwUnimplemented(definition);
    return null;
  }

  @Override
  public Void visit(PortBehaviorDefinition definition) {
    throwUnimplemented(definition);
    return null;
  }

  @Override
  public Void visit(PipelineDefinition definition) {
    throwUnimplemented(definition);
    return null;
  }

  @Override
  public Void visit(StageDefinition definition) {
    throwUnimplemented(definition);
    return null;
  }

  @Override
  public Void visit(CacheDefinition definition) {
    throwUnimplemented(definition);
    return null;
  }

  @Override
  public Void visit(LogicDefinition definition) {
    throwUnimplemented(definition);
    return null;
  }

  @Override
  public Void visit(SignalDefinition definition) {
    throwUnimplemented(definition);
    return null;
  }

  @Override
  public Void visit(Identifier expr) {
    // The typechecker should have already caught that.

    // FIXME: Handle builtin values
    var origin = Objects.requireNonNull(
        Objects.requireNonNull(expr.symbolTable).requireAs(expr, Node.class)
    );

    if (origin instanceof ConstantDefinition constDef) {
      if (constDef.type == null) {
        constDef.accept(this);
      }
      expr.type = constDef.type;
    } else {
      throw new RuntimeException("Don't handle class " + origin.getClass().getName());
    }

    return null;
  }

  @Override
  public Void visit(BinaryExpr expr) {
    throwUnimplemented(expr);
    return null;
  }

  @Override
  public Void visit(GroupedExpr expr) {
    throwUnimplemented(expr);
    return null;
  }

  @Override
  public Void visit(IntegerLiteral expr) {
    expr.type = new ConstantType(expr.number);
    return null;
  }

  @Override
  public Void visit(BinaryLiteral expr) {
    throwUnimplemented(expr);
    return null;
  }

  @Override
  public Void visit(BoolLiteral expr) {
    expr.type = Type.bool();
    return null;
  }

  @Override
  public Void visit(StringLiteral expr) {
    throwUnimplemented(expr);
    return null;
  }

  @Override
  public Void visit(PlaceholderExpr expr) {
    throwUnimplemented(expr);
    return null;
  }

  @Override
  public Void visit(MacroInstanceExpr expr) {
    throwUnimplemented(expr);
    return null;
  }

  @Override
  public Void visit(RangeExpr expr) {
    throwUnimplemented(expr);
    return null;
  }

  @Override
  public Void visit(TypeLiteral expr) {
    var base = expr.baseType.pathToString();

    if (base.equals("Bool")) {
      if (!expr.sizeIndices.isEmpty()) {
        throw Diagnostic.error("Invalid Type Notation", expr.location())
            .description("The Bool type doesn't use the size notation as it is always one bit.")
            .build();
      }
      expr.type = Type.bool();
      return null;
    }

    if (base.equals("SInt")) {
      if (expr.sizeIndices.size() != 1 || expr.sizeIndices.get(0).size() != 1) {
        throw Diagnostic.error("Invalid Type Notation", expr.location())
            .description("The SInt type requires exactly one size parameter.")
            .build();
      }

      // FIXME: Should we verify the type here at all?
      var widthExpr = expr.sizeIndices.get(0).get(0);
      widthExpr.accept(this);
      var bitWidth = constantEvaluator.eval(widthExpr).value();
      if (bitWidth.compareTo(BigInteger.valueOf(1)) < 0) {
        throw Diagnostic.error("Invalid Type Notation", widthExpr.location())
            .locationDescription(widthExpr.location(),
                "Width must be a positive integer greater 1 but was %s", bitWidth)
            .build();
      }

      // FIXME ensure that bitWidth isn't > int32.MAX

      expr.type = Type.signedInt(bitWidth.intValueExact());
      return null;
    }


    throw new RuntimeException(
        "Don't know how to parse the typeliteral %s yet.".formatted(expr.baseType.pathToString()));
  }

  @Override
  public Void visit(IdentifierPath expr) {
    throwUnimplemented(expr);
    return null;
  }

  @Override
  public Void visit(UnaryExpr expr) {
    expr.operand.accept(this);
    var innerType = Objects.requireNonNull(expr.operand.type);

    switch (expr.unOp().operator) {
      // FIXME: doesn't work for sint and const as expected.
      case NEGATIVE, COMPLEMENT -> {
        if (!(innerType instanceof BitsType) && !(innerType instanceof ConstantType)) {
          throw Diagnostic
              .error("Type Mismatch", expr)
              .description("Expected a numerical type but got `%s`", innerType)
              .build();
        }
      }
      case LOG_NOT -> {
        if (!innerType.equals(Type.bool())) {
          throw Diagnostic
              .error("Type Mismatch: expected `Bool`, got `%s`".formatted(innerType), expr)
              .help("For numerical types you can negate them with a minus `-`")
              .build();
        }
      }
      default -> throwUnimplemented(expr);
    }

    if (innerType instanceof ConstantType) {
      // Evaluate the expression for constant types
      expr.type = constantEvaluator.eval(expr).type();
    } else {
      expr.type = innerType;
    }

    return null;
  }

  @Override
  public Void visit(CallExpr expr) {
    throwUnimplemented(expr);
    return null;
  }

  @Override
  public Void visit(IfExpr expr) {
    throwUnimplemented(expr);
    return null;
  }

  @Override
  public Void visit(LetExpr expr) {
    throwUnimplemented(expr);
    return null;
  }

  @Override
  public Void visit(CastExpr expr) {
    expr.value.accept(this);
    expr.typeLiteral.accept(this);

    //var valueType = Objects.requireNonNull(expr.value.type);
    var litType = Objects.requireNonNull(expr.typeLiteral.type);

    // FIXME: For complex types add restrictions here

    expr.type = litType;
    return null;
  }

  @Override
  public Void visit(SymbolExpr expr) {
    throwUnimplemented(expr);
    return null;
  }

  @Override
  public Void visit(MacroMatchExpr expr) {
    throwUnimplemented(expr);
    return null;
  }

  @Override
  public Void visit(MatchExpr expr) {
    throwUnimplemented(expr);
    return null;
  }

  @Override
  public Void visit(ExtendIdExpr expr) {
    throwUnimplemented(expr);
    return null;
  }

  @Override
  public Void visit(IdToStrExpr expr) {
    throwUnimplemented(expr);
    return null;
  }

  @Override
  public Void visit(ExistsInExpr expr) {
    throwUnimplemented(expr);
    return null;
  }

  @Override
  public Void visit(ExistsInThenExpr expr) {
    throwUnimplemented(expr);
    return null;
  }

  @Override
  public Void visit(ForallThenExpr expr) {
    throwUnimplemented(expr);
    return null;
  }

  @Override
  public Void visit(ForallExpr expr) {
    throwUnimplemented(expr);
    return null;
  }

  @Override
  public Void visit(SequenceCallExpr expr) {
    throwUnimplemented(expr);
    return null;
  }

  @Override
  public Void visit(BlockStatement statement) {
    throwUnimplemented(statement);
    return null;
  }

  @Override
  public Void visit(LetStatement statement) {
    throwUnimplemented(statement);
    return null;
  }

  @Override
  public Void visit(IfStatement statement) {
    throwUnimplemented(statement);
    return null;
  }

  @Override
  public Void visit(AssignmentStatement statement) {
    throwUnimplemented(statement);
    return null;
  }

  @Override
  public Void visit(RaiseStatement statement) {
    throwUnimplemented(statement);
    return null;
  }

  @Override
  public Void visit(CallStatement statement) {
    throwUnimplemented(statement);
    return null;
  }

  @Override
  public Void visit(PlaceholderStatement statement) {
    throwUnimplemented(statement);
    return null;
  }

  @Override
  public Void visit(MacroInstanceStatement statement) {
    throwUnimplemented(statement);
    return null;
  }

  @Override
  public Void visit(MacroMatchStatement statement) {
    throwUnimplemented(statement);
    return null;
  }

  @Override
  public Void visit(MatchStatement statement) {
    throwUnimplemented(statement);
    return null;
  }

  @Override
  public Void visit(StatementList statement) {
    throwUnimplemented(statement);
    return null;
  }

  @Override
  public Void visit(InstructionCallStatement statement) {
    throwUnimplemented(statement);
    return null;
  }

  @Override
  public Void visit(LockStatement statement) {
    throwUnimplemented(statement);
    return null;
  }

  @Override
  public Void visit(ForallStatement statement) {
    throwUnimplemented(statement);
    return null;
  }
}
