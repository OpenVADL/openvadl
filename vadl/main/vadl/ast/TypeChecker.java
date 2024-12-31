package vadl.ast;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import vadl.error.Diagnostic;
import vadl.types.BitsType;
import vadl.types.BoolType;
import vadl.types.SIntType;
import vadl.types.Type;
import vadl.types.UIntType;

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

      if (to.getClass() == SIntType.class) {
        var availableWidth = ((SIntType) to).bitWidth();
        return availableWidth >= fromConstant.requiredBitWidth();
      }

      if (to.getClass() == UIntType.class) {
        var availableWidth = ((UIntType) to).bitWidth();
        var value = fromConstant.getValue();
        var isNegative = value.compareTo(BigInteger.ZERO) < 0;
        if (isNegative) {
          return false;
        }

        return availableWidth >= fromConstant.requiredBitWidth();
      }

      if (to.getClass() == BitsType.class) {
        var availableWidth = ((BitsType) to).bitWidth();
        var value = fromConstant.getValue();
        var isNegative = value.compareTo(BigInteger.ZERO) < 0;
        if (isNegative) {
          return false;
        }

        return availableWidth >= fromConstant.requiredBitWidth();
      }

    }

    // Bool => Bits<1>
    if (from.getClass() == BoolType.class) {
      return (to.getClass() == BitsType.class) && (((BitsType) to).bitWidth() == 1);
    }

    // SInt<n> => Bits<n>
    if (from.getClass() == SIntType.class) {
      if (to.getClass() == BitsType.class) {
        return ((SIntType) from).bitWidth() == ((BitsType) to).bitWidth();
      }
    }

    // UInt<n> => Bits<n>
    if (from.getClass() == UIntType.class) {
      if (to.getClass() == BitsType.class) {
        var fromUInt = (UIntType) from;
        var toBitsType = (BitsType) from;
        return fromUInt.bitWidth() == toBitsType.bitWidth();
      }
    }

    // Bits<1> => Bool
    if (from.getClass() == BitsType.class) {
      if (to.getClass() == BoolType.class) {
        var fromBits = (BitsType) from;
        return (fromBits.bitWidth() == 1);
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

      // Insert a cast if needed
      if (!litType.equals(valType)) {
        definition.value = new CastExpr(definition.value, definition.typeLiteral);
        definition.value.accept(this);
      }
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
    throw new IllegalStateException(
        "The type-checker should never see a %s".formatted(definition.getClass().getSimpleName()));
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

  private void visitLogicalBinaryExpression(BinaryExpr expr) {
    var leftTyp = Objects.requireNonNull(expr.left.type);
    var rightTyp = Objects.requireNonNull(expr.right.type);

    // Both sides must be boolean
    if (!(leftTyp instanceof BoolType) && !canImplicitCast(leftTyp, Type.bool())) {
      throw Diagnostic.error("Type Mismatch", expr)
          .locationDescription(expr, "Expected a Boolean here but the left side was an `%s`",
              leftTyp)
          .description("The `%s` operator only works on booleans.", expr.operator())
          .build();
    }

    if (!(leftTyp instanceof BoolType)) {
      expr.left = new CastExpr(expr.left, Type.bool(), expr.left.location());
      leftTyp = Objects.requireNonNull(expr.left.type);
    }

    if (!(rightTyp instanceof BoolType) && !canImplicitCast(rightTyp, Type.bool())) {
      throw Diagnostic.error("Type Mismatch", expr)
          .locationDescription(expr, "Expected a Boolean here but the right side was an `%s`",
              rightTyp)
          .description("The `%s` operator only works on booleans.", expr.operator())
          .build();
    }

    if (!(rightTyp instanceof BoolType)) {
      expr.right = new CastExpr(expr.right, Type.bool(), expr.right.location());
      rightTyp = Objects.requireNonNull(expr.right.type);
    }

    // Return is always boolean
    expr.type = Type.bool();
  }

  @Override
  public Void visit(BinaryExpr expr) {
    expr.left.accept(this);
    expr.right.accept(this);
    var leftTyp = Objects.requireNonNull(expr.left.type);
    var rightTyp = Objects.requireNonNull(expr.right.type);

    // Logical operations are easy, let's get them out of the way.
    if (Operator.logicalComparisions.contains(expr.operator())) {
      visitLogicalBinaryExpression(expr);
      return null;
    }

    // Verify the rough shapes of the input parameters
    // This however doesn't check if the types relate to each other.
    if (Operator.arithmeticOperators.contains(expr.operator()) ||
        Operator.artihmeticComparisons.contains(expr.operator())) {

      if (!(leftTyp instanceof BitsType) && !(leftTyp instanceof ConstantType)) {
        throw Diagnostic.error("Type Missmatch", expr)
            .locationDescription(expr, "Expected a number here but the left side was an `%s`",
                leftTyp)
            .description("The `%s` operator only works on numbers.", expr.operator())
            .build();
      }
      if (!(rightTyp instanceof BitsType) && !(rightTyp instanceof ConstantType)) {
        throw Diagnostic.error("Type Missmatch", expr)
            .locationDescription(expr, "Expected a number here but the right side was an `%s`",
                rightTyp)
            .description("The `%s` operator only works on numbers.", expr.operator())
            .build();
      }
    } else {
      throw new RuntimeException("Don't handle operator " + expr.operator());
    }

    // FIXME: Incorporate special casting rules for binary operators.
    // Shifts and rotates require that the right type is uint and the left can be anything.
    var requireRightUInt =
        List.of(Operator.ShiftLeft, Operator.ShiftRight, Operator.RotateLeft, Operator.RotateRight);
    if (requireRightUInt.contains(expr.operator())) {

      Type closestUIntType;
      if (rightTyp instanceof BitsType bitsRightType) {
        closestUIntType = Type.unsignedInt(bitsRightType.bitWidth());
      } else if (rightTyp instanceof ConstantType constantRightType) {
        closestUIntType = constantRightType.closestUInt();
      } else {
        throw new IllegalStateException("Don't handle operator " + expr.operator());
      }

      if (!(rightTyp instanceof UIntType) && !canImplicitCast(rightTyp, closestUIntType)) {
        throw Diagnostic.error("Type Missmatch", expr)
            .locationNote(expr, "The right type must be unsigned but is %s", rightTyp)
            .build();
      }

      if (!(rightTyp instanceof UIntType)) {
        expr.right = new CastExpr(expr.right, closestUIntType, expr.right.location());
        rightTyp = Objects.requireNonNull(expr.right.type);
      }

      // Only the left side decides the output type
      if (leftTyp instanceof ConstantType) {

        if (List.of(Operator.RotateLeft, Operator.RotateRight).contains(expr.operator())) {
          throw Diagnostic.error("Type Missmatch", expr)
              .locationNote(expr, "The left side must be a concrete type but was %s", rightTyp)
              .description("Rotate operations require a type with a fixed bit width.")
              .build();
        }

        var result = constantEvaluator.eval(expr);
        expr.type = result.type();
        return null;
      }

      expr.type = leftTyp;
      return null;
    }

    // Const types are a special case
    if (leftTyp instanceof ConstantType && rightTyp instanceof ConstantType) {
      var result = constantEvaluator.eval(expr);
      expr.type = result.type();
    }

    // Insert implicit cast if needed
    if (leftTyp.equals(rightTyp)) {
      // Do nothing on purpose
    } else if (canImplicitCast(leftTyp, rightTyp)) {
      // FIXME: Somehow we need to convert a type into a typeliteral.
      //expr.left = new CastExpr(expr.left, expr.right.type);
      throw new RuntimeException("Casting not yet implemented");

    } else if (canImplicitCast(rightTyp, leftTyp)) {
      // FIXME: Somehow we need to convert a type into a typeliteral.
      //expr.left = new CastExpr(expr.left, expr.right.type);
      throw new RuntimeException("Casting not yet implemented");

    } else {
      throw Diagnostic.error("Type Missmatch", expr)
          .locationNote(expr, "The left type is %s while right is %s", leftTyp, rightTyp)
          .description(
              "Both types on the left and right side of an binary operation should be equal.")
          .build();
    }

    if (Operator.artihmeticComparisons.contains(expr.operator())) {
      // Output type depends on type of operation
      expr.type = Type.bool();
    } else if (Operator.arithmeticOperators.contains(expr.operator())) {
      // Note: No that isn't the same as leftTyp
      expr.type = expr.left.type;
    } else {
      throw new RuntimeException("Don't yet know how to handle " + expr.operator);
    }

    return null;
  }

  @Override
  public Void visit(GroupedExpr expr) {
    if (expr.expressions.size() != 1) {
      throw new RuntimeException("Research what to do in that case");
    }
    expr.expressions.get(0).accept(this);
    expr.type = expr.expressions.get(0).type;
    return null;
  }

  @Override
  public Void visit(IntegerLiteral expr) {
    expr.type = new ConstantType(expr.number);
    return null;
  }

  @Override
  public Void visit(BinaryLiteral expr) {
    expr.type = Type.bits(expr.bitWidth);
    return null;
  }

  @Override
  public Void visit(BoolLiteral expr) {
    expr.type = Type.bool();
    return null;
  }

  @Override
  public Void visit(StringLiteral expr) {
    expr.type = Type.string();
    return null;
  }

  @Override
  public Void visit(PlaceholderExpr expr) {
    throw new IllegalStateException(
        "The typechecker should never see a %s".formatted(expr.getClass().getSimpleName()));
  }

  @Override
  public Void visit(MacroInstanceExpr expr) {
    throw new IllegalStateException(
        "The typechecker should never see a %s".formatted(expr.getClass().getSimpleName()));
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

    // The basic types SINT<n>, UINT<n> and BITS<n>
    if (Arrays.asList("SInt", "UInt", "Bits").contains(base)) {
      if (expr.sizeIndices.size() != 1 || expr.sizeIndices.get(0).size() != 1) {
        throw Diagnostic.error("Invalid Type Notation", expr.location())
            .description("The %s type requires exactly one size parameter.", base)
            .build();
      }

      var widthExpr = expr.sizeIndices.get(0).get(0);
      widthExpr.accept(this);
      var bitWidth = constantEvaluator.eval(widthExpr).value();

      var minWidth = BigInteger.ONE;
      if (bitWidth.compareTo(minWidth) < 0) {
        throw Diagnostic.error("Invalid Type Notation", widthExpr.location())
            .locationDescription(widthExpr.location(),
                "Width must of a %s must be greater than %s but was %s", base, minWidth, bitWidth)
            .build();
      }


      // FIXME: ensure that bitWidth isn't > int32.MAX

      expr.type = switch (base) {
        case "SInt" -> Type.signedInt(bitWidth.intValueExact());
        case "UInt" -> Type.unsignedInt(bitWidth.intValueExact());
        case "Bits" -> Type.bits(bitWidth.intValueExact());
        default -> throw new IllegalStateException("Unexpected value: " + base);
      };
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
      case NEGATIVE -> {
        if (!(innerType instanceof BitsType) && !(innerType instanceof ConstantType)) {
          throw Diagnostic
              .error("Type Mismatch", expr)
              .description("Expected a numerical type but got `%s`", innerType)
              .build();
        }
      }
      case COMPLEMENT -> {
        if (!(innerType instanceof BitsType)) {
          throw Diagnostic
              .error("Type Mismatch", expr)
              .description("Expected a numerical type with fixed bit-width but got `%s`", innerType)
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
    Objects.requireNonNull(expr.typeLiteral).accept(this);

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
