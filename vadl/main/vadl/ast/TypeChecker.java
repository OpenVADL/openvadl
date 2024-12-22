package vadl.ast;

import java.math.BigInteger;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vadl.error.Diagnostic;
import vadl.types.BitsType;
import vadl.types.SIntType;
import vadl.types.Type;
import vadl.types.asmTypes.AsmType;
import vadl.types.asmTypes.GroupAsmType;
import vadl.types.asmTypes.StringAsmType;
import vadl.utils.WithSourceLocation;

/**
 * A experimental, temporary type-checker to verify expressions and attach types to the AST.
 *
 * <p>As the typesystem can depend on constants, the typechecker needs to evaluate (at least some
 * of) them.
 */
public class TypeChecker
    implements DefinitionVisitor<Void>, StatementVisitor<Void>, ExprVisitor<Void> {

  private static final Logger log = LoggerFactory.getLogger(TypeChecker.class);
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
    //    throw new RuntimeException(
    //        "The typechecker doesn't know how to handle `%s` yet, found in %s".formatted(
    //            node.getClass().getSimpleName(), node.location().toIDEString()));
    log.error("The typechecker doesn't know how to handle `%s` yet, found in %s".formatted(
        node.getClass().getSimpleName(), node.location().toIDEString()));
  }

  private void throwInvalidState(Node node, String message) {
    throw new RuntimeException(
        "The typechecker encountered an invalid state in `%s` at %s: %s".formatted(
            node.getClass().getSimpleName(), node.location().toIDEString(), message));
  }

  private void throwInvalidAsmCast(AsmType from, AsmType to, WithSourceLocation location) {
    throw Diagnostic.error(
            "From %s to %s is an invalid cast of assembly types.".formatted(from, to), location)
        .build();
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
    throwUnimplemented(definition);
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
    for (var rule : definition.rules) {
      // Only visit rules that have not yet been checked,
      // as rules can be invoked by other rules and may have already been checked
      if (rule.asmType == null) {
        rule.accept(this);
      }
    }
    definition.commonDefinitions.forEach(commonDef -> commonDef.accept(this));
    return null;
  }

  @Override
  public Void visit(AsmModifierDefinition definition) {
    return null;
  }

  @Override
  public Void visit(AsmDirectiveDefinition definition) {
    return null;
  }

  @Override
  public Void visit(AsmGrammarRuleDefinition definition) {
    definition.alternatives.accept(this);
    if (definition.asmTypeDefinition != null) {
      var castToAsmType = getAsmTypeFromAsmTypeDefinition(definition.asmTypeDefinition);
      if (definition.alternatives.asmType == null) {
        throwInvalidState(definition, "AsmType of rule body could not be resolved.");
        return null;
      }
      if (definition.alternatives.asmType.canBeCastTo(castToAsmType)) {
        definition.asmType = castToAsmType;
      } else {
        throwInvalidAsmCast(definition.alternatives.asmType, castToAsmType,
            definition.asmTypeDefinition);
      }
    } else {
      definition.asmType = definition.alternatives.asmType;
    }
    return null;
  }

  @Override
  public Void visit(AsmGrammarAlternativesDefinition definition) {

    // resolve types of all elements
    definition.alternatives.forEach(elements -> {
      for (var element : elements) {
        if (element.asmType == null) {
          element.accept(this);
        }
      }
    });

    // all alternatives have to have the same type
    AsmType allAlternativeType = null;

    for (var elements : definition.alternatives) {
      AsmType curAlternativeType;
      if (elements.isEmpty()) {
        throwInvalidState(definition,
            "Typechecker found an AsmGrammarAlternative without elements.");
      }
      if (elements.size() == 1) {
        curAlternativeType = elements.get(0).asmType;
      } else {
        // create GroupAsmType, but only consider elements which are assigned to an attribute
        var groupSubtypes =
            elements.stream().filter(e -> e.attribute != null && !e.isAttributeLocalVar)
                .map(e -> e.asmType).toList();
        curAlternativeType = new GroupAsmType(groupSubtypes);
      }

      if (allAlternativeType == null) {
        allAlternativeType = curAlternativeType;
        continue;
      }

      if (!allAlternativeType.equals(curAlternativeType)) {
        // TODO error: alternatives with different types
      }
    }

    definition.asmType = allAlternativeType;
    return null;
  }

  @Override
  public Void visit(AsmGrammarElementDefinition definition) {

    if (definition.localVar != null) {
      definition.localVar.accept(this);
    }

    if (definition.asmLiteral != null) {
      definition.asmLiteral.accept(this);
      if (definition.asmLiteral.asmType == null) {
        throwInvalidState(definition, "AsmType of asm literal could not be resolved.");
        return null;
      }
      definition.asmType = definition.asmLiteral.asmType;
    }

    if (definition.groupAlternatives != null) {
      definition.groupAlternatives.accept(this);

      if (definition.groupAsmTypeDefinition != null) {
        var castToAsmType = getAsmTypeFromAsmTypeDefinition(definition.groupAsmTypeDefinition);
        if (definition.groupAlternatives.asmType == null) {
          throwInvalidState(definition, "AsmType of group element could not be resolved.");
          return null;
        }
        if (definition.groupAlternatives.asmType.canBeCastTo(castToAsmType)) {
          definition.asmType = castToAsmType;
        } else {
          throwInvalidAsmCast(definition.groupAlternatives.asmType, castToAsmType,
              definition.groupAsmTypeDefinition);
        }
      } else {
        definition.asmType = definition.groupAlternatives.asmType;
      }
    }
    if (definition.optionAlternatives != null) {
      definition.optionAlternatives.accept(this);
      definition.asmType = definition.optionAlternatives.asmType;
    }
    if (definition.repetitionAlternatives != null) {
      definition.repetitionAlternatives.accept(this);
      definition.asmType = definition.repetitionAlternatives.asmType;
    }

    if (definition.semanticPredicate != null) {
      // TODO: expression type checking
      // definition.semanticPredicate.accept(this);
    }

    return null;
  }

  @Override
  public Void visit(AsmGrammarLiteralDefinition definition) {

    if (definition.stringLiteral != null) {
      asmStringLiteralUsage(definition);
      return null;
    }

    if (definition.id == null) {
      throwInvalidState(definition,
          "AsmGrammarLiteral is not a StringLiteral "
              + "and does not reference a grammar rule / function / local variable.");
      return null;
    }

    var invocationSymbolOrigin = definition.symbolTable().resolveNode(definition.id.name);
    if (invocationSymbolOrigin instanceof AsmGrammarRuleDefinition rule) {
      visitAsmRuleInvocation(definition, rule);
    } else if (invocationSymbolOrigin instanceof AsmGrammarLocalVarDefinition localVar) {
      visitAsmLocalVarUsage(definition, localVar);
    } else if (invocationSymbolOrigin instanceof FunctionDefinition function) {
      // TODO check input fits to types of arguments
      // TODO check return type and infer asm type

    } else {
      // TODO error: symbol in grammar rule has to be rule / localVar / function
    }

    return null;
  }

  private void asmStringLiteralUsage(AsmGrammarLiteralDefinition definition) {
    if (definition.asmTypeDefinition != null) {
      var castToAsmType = getAsmTypeFromAsmTypeDefinition(definition.asmTypeDefinition);
      if (StringAsmType.instance().canBeCastTo(castToAsmType)) {
        definition.asmType = castToAsmType;
      } else {
        throwInvalidAsmCast(StringAsmType.instance(), castToAsmType, definition.asmTypeDefinition);
      }
    } else {
      definition.asmType = StringAsmType.instance();
    }
  }

  private void visitAsmRuleInvocation(AsmGrammarLiteralDefinition definition,
                                      AsmGrammarRuleDefinition invokedRule) {
    if (invokedRule.asmType == null) {
      invokedRule.accept(this);
    }

    if (invokedRule.asmType == null) {
      throwInvalidState(definition,
          "Could not resolve AsmType of grammar rule %s.".formatted(invokedRule.identifier().name));
      return;
    }

    if (definition.asmTypeDefinition != null) {
      var castToAsmType = getAsmTypeFromAsmTypeDefinition(definition.asmTypeDefinition);
      if (invokedRule.asmType.canBeCastTo(castToAsmType)) {
        definition.asmType = castToAsmType;
      } else {
        throwInvalidAsmCast(invokedRule.asmType, castToAsmType, definition.asmTypeDefinition);
      }
    } else {
      definition.asmType = invokedRule.asmType;
    }
  }

  private void visitAsmLocalVarUsage(AsmGrammarLiteralDefinition definition,
                                     AsmGrammarLocalVarDefinition localVar) {
    if (localVar.asmLiteral.asmType == null) {
      localVar.accept(this);
    }

    if (definition.asmTypeDefinition != null) {
      // TODO is it allowed to cast a local variable usage?
    } else {
      definition.asmType = localVar.asmLiteral.asmType;
    }
  }

  // NullAway says that a nullable value is returned, but if asmType is null an exception is thrown
  @SuppressWarnings("NullAway")
  private AsmType getAsmTypeFromAsmTypeDefinition(AsmGrammarTypeDefinition definition) {
    var asmType = AsmType.ASM_TYPES.get(definition.id.name);
    if (asmType == null) {
      throwInvalidState(definition,
          "Symbol resolution found asm type %s but the typechecker could not find it.".formatted(
              definition.id.name));
    }
    return asmType;
  }

  @SuppressWarnings("checkstyle:OverloadMethodsDeclarationOrder")
  @Override
  public Void visit(AsmGrammarLocalVarDefinition definition) {
    definition.asmLiteral.accept(this);
    return null;
  }

  @Override
  public Void visit(AsmGrammarTypeDefinition definition) {
    // symbol checking ensures that Identifier of AsmGrammarTypeDefinition is a valid AsmType
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
    throwUnimplemented(expr);
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
    throwUnimplemented(expr);
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
