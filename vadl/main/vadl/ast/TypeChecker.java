package vadl.ast;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;
import vadl.error.Diagnostic;
import vadl.types.BitsType;
import vadl.types.BoolType;
import vadl.types.BuiltInTable;
import vadl.types.DataType;
import vadl.types.SIntType;
import vadl.types.StringType;
import vadl.types.Type;
import vadl.types.UIntType;
import vadl.types.asmTypes.AsmType;
import vadl.types.asmTypes.GroupAsmType;
import vadl.types.asmTypes.InstructionAsmType;
import vadl.types.asmTypes.StringAsmType;
import vadl.types.asmTypes.VoidAsmType;
import vadl.utils.Pair;
import vadl.utils.SourceLocation;
import vadl.utils.WithSourceLocation;

/**
 * A experimental, temporary type-checker to verify expressions and attach types to the AST.
 *
 * <p>As the typesystem can depend on constants, the typechecker needs to evaluate (at least some
 * of) them.
 */
@SuppressWarnings("checkstyle:OverloadMethodsDeclarationOrder")
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

  // FIXME: Add a cache like with fetch()

  private void throwUnimplemented(Node node) {
    throw new RuntimeException(
        "The typechecker doesn't know how to handle `%s` yet, found in %s".formatted(
            node.getClass().getSimpleName(), node.location().toIDEString()));
  }

  private IllegalStateException buildIllegalStateException(Node node, String message) {
    return new IllegalStateException(
        "The typechecker encountered an invalid state in `%s` at %s: %s".formatted(
            node.getClass().getSimpleName(), node.location().toIDEString(), message));
  }

  private void throwInvalidAsmCast(AsmType from, AsmType to, WithSourceLocation location) {
    throw Diagnostic.error("Type Mismatch", location)
        .description("Invalid cast from `%s` to `%s`.", from, to)
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

  /**
   * Wraps the expr provided with an implicit cast if it is possible, and not useless.
   *
   * @param inner expression to wrap.
   * @param to    which the expression should be casted.
   * @return the original expression, possibly wrapped.
   */
  private static Expr wrapImplicitCast(Expr inner, Type to) {
    var innerType = Objects.requireNonNull(inner.type);
    if (innerType.equals(to) || !canImplicitCast(innerType, to)) {
      return inner;
    }

    return new CastExpr(inner, to);
  }

  @Nullable
  private static Integer preferredBitWidthOf(Type type) {
    if (type instanceof BitsType bitsType) {
      return bitsType.bitWidth();
    }

    if (type instanceof BoolType) {
      return 1;
    }

    return null;
  }

  @Nullable
  private static BuiltInTable.BuiltIn getBuiltIn(String name) {
    // FIXME: Namespace should be propper resolved and not that hacky
    if (name.startsWith("VADL::")) {
      name = name.substring("VADL::".length());
    }

    // FIXME: THIS IS A TEMPORARY HACK! Propper selection should happen based on types.
    var hackyRewrites = Map.of("div", "sdiv", "mod", "smod");
    if (hackyRewrites.containsKey(name)) {
      name = hackyRewrites.get(name);
    }

    String finalBuiltinName = name;
    var matchingBuiltin = BuiltInTable.builtIns()
        .filter(b -> b.name().toLowerCase().equals(finalBuiltinName)).toList();

    if (matchingBuiltin.size() > 1) {
      throw new IllegalStateException("Multiple builtin match '$s': " + finalBuiltinName);
    }

    if (matchingBuiltin.isEmpty()) {
      return null;
    }

    return matchingBuiltin.get(0);
  }

  @Override
  public Void visit(ConstantDefinition definition) {
    definition.value.accept(this);
    Type valType = Objects.requireNonNull(definition.value.type);

    if (definition.typeLiteral == null) {
      // Do nothing on purpose
    } else {
      definition.typeLiteral.type =
          parseTypeLiteral(definition.typeLiteral, preferredBitWidthOf(valType));
      Type litType = Objects.requireNonNull(definition.typeLiteral.type);

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
    definition.type.accept(this);
    var type = Objects.requireNonNull(definition.type.type);
    if (!(type instanceof BitsType bitsType)) {
      throw Diagnostic.error("Type Mismatch", definition.type)
          .locationDescription(definition.type, "Expected bits type length but got `%s`", type)
          .build();
    }

    var bitWidth = bitsType.bitWidth();
    var bitsVerifier = new FormatBitsVerifier(bitWidth);
    var nextOccupiedBit = bitWidth - 1;

    for (var field : definition.fields) {
      if (field instanceof FormatDefinition.TypedFormatField typedField) {
        typedField.typeLiteral.accept(this);
        if (!(typedField.typeLiteral.type instanceof BitsType fieldBitsType)) {
          throw Diagnostic.error("Bits Type expected", typedField.typeLiteral)
              .build();
        }
        typedField.range = new FormatDefinition.BitRange(nextOccupiedBit,
            nextOccupiedBit - (fieldBitsType.bitWidth() - 1));
        nextOccupiedBit -= fieldBitsType.bitWidth();
        bitsVerifier.addType(fieldBitsType);

      } else if (field instanceof FormatDefinition.RangeFormatField rangeField) {
        if (rangeField.typeLiteral != null) {
          rangeField.typeLiteral.accept(this);
          rangeField.type = Objects.requireNonNull(rangeField.typeLiteral.type);
        }

        int fieldBitWidth = 0;
        rangeField.computedRanges = new ArrayList<>();
        for (var range : rangeField.ranges) {
          range.accept(this);

          // FIXME: This should be so much more elegant
          int from;
          int to;
          if (range instanceof RangeExpr rangeExpr) {
            from = constantEvaluator.eval(rangeExpr.from).value().intValueExact();
            to = constantEvaluator.eval(rangeExpr.to).value().intValueExact();
          } else {
            from = constantEvaluator.eval(range).value().intValueExact();
            to = from;
          }

          // NOTE: From is always larger than to
          var rangeSize = (from - to) + 1;
          if (rangeSize < 1) {
            throw Diagnostic.error("Invalid Range", rangeField)
                .description("Range must be >= 1 but was %s", fieldBitWidth)
                .build();
          }
          fieldBitWidth += rangeSize;
          rangeField.computedRanges.add(new FormatDefinition.BitRange(from, to));
          bitsVerifier.addRange(from, to);
        }

        if (fieldBitWidth < 1) {
          throw Diagnostic.error("Invalid Field", rangeField)
              .description("Field must be at least one bit but was %s", fieldBitWidth)
              .build();
        }

        if (rangeField.type == null) {
          // Set the type
          rangeField.type = Type.bits(fieldBitWidth);
        } else {
          // Verify the received type with the one provided in the literal.
          var rangeBitsType = Type.bits(fieldBitWidth);
          if (!canImplicitCast(rangeField.type, rangeBitsType)) {
            throw Diagnostic.error("Type Mismatch", rangeField)
                .description("Type declared as `%s`, but the range is `%s`", rangeField.type,
                    rangeBitsType)
                .build();
          }
        }

      } else if (field instanceof FormatDefinition.DerivedFormatField dfField) {
        dfField.expr.accept(this);
      } else {
        throw new RuntimeException("Unknown FormatField Class ".concat(field.getClass().getName()));
      }
    }

    if (bitsVerifier.hasViolations()) {
      throw Diagnostic.error("Invalid Format", definition)
          .description("%s", Objects.requireNonNull(bitsVerifier.getViolationsMessage()))
          .build();
    }

    return null;
  }

  @Override
  public Void visit(InstructionSetDefinition definition) {
    if (definition.extendingNode != null) {
      definition.extendingNode.accept(this);
    }

    for (var def : definition.definitions) {
      def.accept(this);
    }

    // FIXME: Verify at least one programcounter
    return null;
  }

  @Override
  public Void visit(CounterDefinition definition) {
    definition.typeLiteral.accept(this);
    return null;
  }

  @Override
  public Void visit(MemoryDefinition definition) {
    definition.addressTypeLiteral.accept(this);
    definition.dataTypeLiteral.accept(this);
    definition.type = Type.concreteRelation(
        List.of(Objects.requireNonNull(definition.addressTypeLiteral.type)),
        Objects.requireNonNull(definition.dataTypeLiteral.type));
    return null;
  }

  @Override
  public Void visit(RegisterDefinition definition) {
    definition.typeLiteral.accept(this);
    if (!(definition.typeLiteral.type instanceof DataType regType)) {
      var type = definition.typeLiteral.type;
      throw Diagnostic.error("Invalid Type", definition)
          .description("Expected register type to be one of Bits, SInt, UInt or Bool.")
          .note("Type was %s.", type == null ? "unknown" : type)
          .build();
    }
    definition.type = regType;
    return null;
  }

  @Override
  public Void visit(RegisterFileDefinition definition) {
    definition.typeLiteral.argTypes().forEach(arg -> arg.accept(this));
    definition.typeLiteral.resultType().accept(this);
    definition.type = Type.concreteRelation(
        definition.typeLiteral.argTypes().stream().map(arg -> arg.type).toList(),
        Objects.requireNonNull(definition.typeLiteral.resultType().type));

    return null;
  }

  @Override
  public Void visit(InstructionDefinition definition) {
    if (Objects.requireNonNull(definition.formatNode).type.type == null) {
      definition.formatNode.accept(this);
    }

    definition.behavior.accept(this);

    if (definition.assemblyDefinition == null) {
      throw Diagnostic.error("Missing Assembly", definition.identifier())
          .description("Every instruction needs an matching assembly definition.")
          .build();
    }
    if (definition.encodingDefinition == null) {
      throw Diagnostic.error("Missing Encoding", definition.identifier())
          .description("Every instruction needs an matching encoding definition.")
          .build();
    }

    return null;
  }

  @Override
  public Void visit(PseudoInstructionDefinition definition) {
    // Check the parameters
    definition.params.forEach(param -> param.typeLiteral.accept(this));

    // Check the statements
    definition.statements.forEach(stmt -> stmt.accept(this));

    // Verify the existenc of a matching assemblyDefinition
    if (definition.assemblyDefinition == null) {
      throw Diagnostic.error("Missing Assembly", definition.identifier())
          .description("Every pseudo instruction needs an matching assembly definition.")
          .build();
    }

    return null;
  }

  @Override
  public Void visit(RelocationDefinition definition) {
    // Check the parameter
    for (var param : definition.params) {
      param.typeLiteral.accept(this);
    }

    definition.resultTypeLiteral.accept(this);
    definition.expr.accept(this);

    // Verify the types are compatible
    var definedType = Objects.requireNonNull(definition.resultTypeLiteral.type);
    definition.expr = wrapImplicitCast(definition.expr, definedType);
    var actualType = Objects.requireNonNull(definition.expr.type);
    if (!definedType.equals(actualType)) {
      throw Diagnostic.error("Type Missmatch", definition.expr)
          .description("Expected %s but got %s", definedType, actualType)
          .build();
    }

    var argTypes = definition.params.stream().map(p -> p.typeLiteral.type).toList();
    var retType = definition.resultTypeLiteral.type;
    definition.type = Type.concreteRelation(argTypes, retType);
    return null;
  }

  @Override
  public Void visit(EncodingDefinition definition) {
    for (var item : definition.encodings.items) {
      if (!(item instanceof EncodingDefinition.EncodingField encodingField)) {
        throw new IllegalStateException("Should that be possible?");
      }

      encodingField.value.accept(this);
      var fieldType = Objects.requireNonNull(
          Objects.requireNonNull(definition.formatNode)
              .getFieldType(encodingField.field.name));

      encodingField.value = wrapImplicitCast(encodingField.value, fieldType);
      var valueType = Objects.requireNonNull(encodingField.value.type);

      if (!fieldType.equals(valueType)) {
        throw Diagnostic.error("Type Mismatch", encodingField.value)
            .description("Expected %s but got `%s`", fieldType, valueType)
            .build();
      }
    }
    return null;
  }

  @Override
  public Void visit(AssemblyDefinition definition) {
    definition.expr.accept(this);
    var exprType = Objects.requireNonNull(definition.expr.type);

    if (exprType.getClass() != StringType.class) {
      throw Diagnostic.error("Type Mismatch", definition.expr)
          .description("Expected String but got `%s`", exprType)
          .build();
    }
    return null;
  }

  @Override
  public Void visit(UsingDefinition definition) {
    definition.typeLiteral.accept(this);
    return null;
  }

  @Override
  public Void visit(FunctionDefinition definition) {
    definition.params.forEach(param -> param.typeLiteral.accept(this));
    definition.retType.accept(this);
    definition.expr.accept(this);

    var retType = Objects.requireNonNull(definition.retType.type);
    definition.expr = wrapImplicitCast(definition.expr, retType);
    var exprType = Objects.requireNonNull(definition.expr.type);

    if (!exprType.equals(retType)) {
      throw Diagnostic.error("Type Mismatch", definition.expr)
          .locationDescription(definition.retType, "Return type defined here as `%s`", retType)
          .locationDescription(definition.expr, "Expected `%s` but got `%s`", retType, exprType)
          .build();
    }

    var argTypes = definition.params.stream().map(p -> p.typeLiteral.type).toList();
    definition.type = Type.concreteRelation(argTypes, retType);
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
    throw new IllegalStateException(
        "No %s should ever reach the Typechecker".formatted(definition.getClass().getSimpleName()));
  }

  @Override
  public Void visit(MacroInstanceDefinition definition) {
    throw new IllegalStateException(
        "No %s should ever reach the Typechecker".formatted(definition.getClass().getSimpleName()));
  }

  @Override
  public Void visit(MacroMatchDefinition definition) {
    throw new IllegalStateException(
        "No %s should ever reach the Typechecker".formatted(definition.getClass().getSimpleName()));
  }

  @Override
  public Void visit(DefinitionList definition) {
    for (var item : definition.items) {
      item.accept(this);
    }
    return null;
  }

  @Override
  public Void visit(ModelDefinition definition) {
    throw new IllegalStateException(
        "No %s should ever reach the Typechecker".formatted(definition.getClass().getSimpleName()));
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
    // Do nothing on purpose.
    // The symboltable should have already resolved everything.
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
    definition.definitions.forEach(def -> def.accept(this));
    return null;
  }

  @Override
  public Void visit(AsmDescriptionDefinition definition) {
    definition.commonDefinitions.forEach(commonDef -> commonDef.accept(this));

    for (var rule : definition.rules) {
      // only visit rules that have not yet been visited,
      // as rules can be invoked by other rules and may already have an AsmType
      if (rule.asmType == null) {
        rule.accept(this);
      }
    }

    expandAsmInstructionRule(definition.rules);

    var ll1Checker = new AsmLL1Checker();
    ll1Checker.verify(definition.rules);
    return null;
  }

  /**
   * Expand the builtin rule "Instruction"
   * to be an alternative over all rules with type @instruction.
   * <p>
   * This needs to happen before the grammar is checked for LL(1) conflicts.
   * </p>
   */
  private void expandAsmInstructionRule(List<AsmGrammarRuleDefinition> rules) {
    var instructionRule = rules.stream()
        .filter(rule -> rule.isBuiltinRule && rule.identifier().name.equals("Instruction"))
        .findFirst().orElseThrow(() -> new IllegalStateException("Instruction rule not found."));

    var invalidLoc = SourceLocation.INVALID_SOURCE_LOCATION;

    var instructionRuleAlternatives = rules.stream()
        .filter(rule -> rule.asmType == InstructionAsmType.instance()
            && !List.of("Statement", "Instruction").contains(rule.identifier().name))
        .map(rule -> {

          var asmLiteral = new AsmGrammarLiteralDefinition(
              new Identifier(rule.identifier().name, invalidLoc),
              new ArrayList<>(), null, null, invalidLoc);
          asmLiteral.symbolTable = rule.symbolTable();
          asmLiteral.asmType = InstructionAsmType.instance();

          var element = new AsmGrammarElementDefinition(null, null, false, asmLiteral,
              null, null, null, null, null, invalidLoc);
          element.symbolTable = rule.symbolTable();
          element.asmType = InstructionAsmType.instance();
          return List.of(element);
        }).toList();

    instructionRule.alternatives =
        new AsmGrammarAlternativesDefinition(instructionRuleAlternatives, invalidLoc);
    instructionRule.alternatives.asmType = InstructionAsmType.instance();
    instructionRule.isBuiltinRule = false;
  }

  @Override
  public Void visit(AsmModifierDefinition definition) {
    return null;
  }

  @Override
  public Void visit(AsmDirectiveDefinition definition) {
    return null;
  }

  HashSet<String> asmRuleInvocationChain = new LinkedHashSet<>();
  HashMap<String, AsmGrammarElementDefinition> attributesAssignedInParent = new HashMap<>();

  @Override
  public Void visit(AsmGrammarRuleDefinition definition) {
    if (!asmRuleInvocationChain.add(definition.identifier().name)) {
      var cycle =
          String.join(" -> ", asmRuleInvocationChain) + " -> " + definition.identifier().name;
      throw Diagnostic.error("Found a cycle in grammar rules: %s.".formatted(cycle),
          definition.sourceLocation()).build();
    }

    definition.alternatives.accept(this);
    if (definition.asmTypeDefinition != null) {
      var castToAsmType = getAsmTypeFromAsmTypeDefinition(definition.asmTypeDefinition);
      if (definition.alternatives.asmType == null) {
        throw buildIllegalStateException(definition, "AsmType of rule body could not be resolved.");
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
    asmRuleInvocationChain.remove(definition.identifier().name);
    return null;
  }

  @Override
  public Void visit(AsmGrammarAlternativesDefinition definition) {

    preprocessAlternativesElements(definition);

    // all alternatives have to have the same type
    AsmType allAlternativeType = null;
    AsmGrammarElementDefinition allAlternativeTypeElement = null;

    for (var elements : definition.alternatives) {
      AsmType curAlternativeType = determineAlternativeType(definition, elements);

      if (allAlternativeType == null) {
        allAlternativeType = curAlternativeType;
        allAlternativeTypeElement = elements.get(0);
      } else {
        validateAsmAlternativeType(definition, elements, curAlternativeType,
            allAlternativeTypeElement, allAlternativeType);
      }
    }

    definition.asmType = allAlternativeType;
    return null;
  }

  private void preprocessAlternativesElements(AsmGrammarAlternativesDefinition definition) {
    definition.alternatives.forEach(elements -> {
      for (int i = 0; i < elements.size(); i++) {
        var element = elements.get(i);

        // mark elements that are within a repetition block
        if (element.repetitionAlternatives != null) {
          element.repetitionAlternatives.alternatives.forEach(
              es -> es.forEach(e -> e.isWithinRepetitionBlock = true));
        }

        // ensure that local variable declarations are at the beginning of a block
        if (element.localVar != null) {
          for (int j = 0; j < i; j++) {
            if (elements.get(j).localVar == null && elements.get(j).semanticPredicate == null) {
              throw Diagnostic.error(
                      "Local variable declaration is not at the beginning of a block.",
                      element.sourceLocation())
                  .locationDescription(element.localVar.sourceLocation(),
                      "Local variable declared here.")
                  .locationDescription(elements.get(0).sourceLocation(), "Block starts here.")
                  .build();
            }
          }
        }
      }
    });
  }

  @Nullable
  private AsmType determineAlternativeType(AsmGrammarAlternativesDefinition definition,
                                           List<AsmGrammarElementDefinition> elements) {
    var elementsToConsider = elements.stream().filter(
        element -> element.localVar == null && element.semanticPredicate == null
    ).toList();

    if (elementsToConsider.isEmpty()) {
      throw buildIllegalStateException(definition,
          "Typechecker found an AsmGrammarAlternative without elements.");
    }

    var groupSubtypeMap = new LinkedHashMap<String, AsmType>();
    var alreadyAssignedAttributes = new HashMap<String, AsmGrammarElementDefinition>();

    for (var element : elements) {
      if (elementsToConsider.contains(element)) {
        if (element.asmType == null) {
          if (element.repetitionAlternatives != null) {
            attributesAssignedInParent = alreadyAssignedAttributes;
          }
          element.accept(this);
        }

        appendToAsmGroupType(element, groupSubtypeMap, alreadyAssignedAttributes);
      } else {
        element.accept(this);
      }
    }

    if (elementsToConsider.size() == 1) {
      return elementsToConsider.get(0).asmType;
    }

    return new GroupAsmType(groupSubtypeMap);
  }

  private void appendToAsmGroupType(AsmGrammarElementDefinition element,
                                    Map<String, AsmType> groupSubtypeMap,
                                    Map<String, AsmGrammarElementDefinition> assignedAttributes) {
    // the following two if statements are mutually exclusive:
    // grammar syntax does not allow for an element to be assigned to an attribute
    // and be of GroupAsmType at the same time

    // consider elements which are assigned to an attribute
    if (element.attribute != null && !element.isAttributeLocalVar
        && !element.isWithinRepetitionBlock) {
      if (groupSubtypeMap.containsKey(element.attribute.name)) {
        throw Diagnostic.error("Found multiple assignments to attribute.", element)
            .description("Attribute %s has already been assigned to.",
                element.attribute.name).build();
      }
      groupSubtypeMap.put(element.attribute.name, element.asmType);
      assignedAttributes.put(element.attribute.name, element);
    }

    // flatten nested GroupAsmTypes from group and option blocks
    // ignore the type of repetition blocks
    if (element.optionAlternatives != null || element.groupAlternatives != null) {
      if (element.asmType instanceof GroupAsmType elementAsmType) {
        elementAsmType.getSubtypeMap().keySet().forEach(
            key -> throwErrorOnNestedMultipleAttributeAssignment(element, groupSubtypeMap, key)
        );
        groupSubtypeMap.putAll(elementAsmType.getSubtypeMap());
      } else {

        var alternatives = element.optionAlternatives != null ? element.optionAlternatives :
            element.groupAlternatives;
        Objects.requireNonNull(alternatives);

        var attribute = alternatives.alternatives.get(0).stream().filter(
            e -> e.localVar == null && e.semanticPredicate == null
        ).findFirst().map(e -> e.attribute);

        if (attribute.isPresent()) {
          throwErrorOnNestedMultipleAttributeAssignment(element, groupSubtypeMap,
              attribute.get().name);
          groupSubtypeMap.put(attribute.get().name, element.asmType);
        }
      }
    }
  }

  private void throwErrorOnNestedMultipleAttributeAssignment(AsmGrammarElementDefinition element,
                                                             Map<String, AsmType> groupSubtypeMap,
                                                             String attributeToBeAdded) {
    if (groupSubtypeMap.containsKey(attributeToBeAdded)) {
      throw Diagnostic.error("Found invalid attribute assignment.", element)
          .description(
              "Attribute %s assigned in this block is already assigned in enclosing block.",
              attributeToBeAdded)
          .build();
    }
  }

  private void validateAsmAlternativeType(AsmGrammarAlternativesDefinition definition,
                                          List<AsmGrammarElementDefinition> elements,
                                          @Nullable
                                          AsmType curAlternativeType,
                                          @Nullable
                                          AsmGrammarElementDefinition allAlternativeTypeElement,
                                          AsmType allAlternativeType) {
    if (curAlternativeType == null || allAlternativeTypeElement == null) {
      throw buildIllegalStateException(definition,
          "AsmType of an asm alternative could not be resolved.");
    }

    if (!allAlternativeType.equals(curAlternativeType)) {
      throw Diagnostic.error(
              "Found asm alternatives with differing AsmTypes.", definition.sourceLocation())
          .note("All alternatives must resolve to the same AsmType.")
          .locationDescription(allAlternativeTypeElement,
              "Found alternative with type %s,", allAlternativeType)
          .locationDescription(elements.get(0),
              "Found other alternative with type %s,", curAlternativeType)
          .build();
    }
  }

  @Override
  public Void visit(AsmGrammarElementDefinition definition) {

    if (definition.localVar != null) {
      definition.localVar.accept(this);
      definition.asmType = definition.localVar.asmType;
    }

    visitAsmLiteral(definition);

    visitGroupAlternatives(definition);

    if (definition.optionAlternatives != null) {
      definition.optionAlternatives.accept(this);
      definition.asmType = definition.optionAlternatives.asmType;
    }
    if (definition.repetitionAlternatives != null) {
      definition.repetitionAlternatives.accept(this);
      definition.asmType = definition.repetitionAlternatives.asmType;
    }

    if (definition.semanticPredicate != null) {
      definition.semanticPredicate.accept(this);
      if (definition.semanticPredicate.type != Type.bool()) {
        throw Diagnostic.error("Semantic predicate expression does not evaluate to Boolean.",
            definition.semanticPredicate).build();
      }
    }

    // actions that depend on the resolved asm type of this element
    updateLocalVarIfNecessary(definition);
    validateAttributeAsmType(definition);

    return null;
  }

  private void visitAsmLiteral(AsmGrammarElementDefinition definition) {
    if (definition.asmLiteral == null) {
      return;
    }

    definition.asmLiteral.accept(this);
    if (definition.asmLiteral.asmType == null) {
      throw buildIllegalStateException(definition, "AsmType of asm literal could not be resolved.");
    }
    definition.asmType = definition.asmLiteral.asmType;
  }

  private void visitGroupAlternatives(AsmGrammarElementDefinition definition) {
    if (definition.groupAlternatives == null) {
      return;
    }

    definition.groupAlternatives.accept(this);
    if (definition.groupAsmTypeDefinition == null) {
      definition.asmType = definition.groupAlternatives.asmType;
      return;
    }

    var castToAsmType = getAsmTypeFromAsmTypeDefinition(definition.groupAsmTypeDefinition);
    if (definition.groupAlternatives.asmType == null) {
      throw buildIllegalStateException(definition,
          "AsmType of group element could not be resolved.");
    }

    if (definition.groupAlternatives.asmType.canBeCastTo(castToAsmType)) {
      definition.asmType = castToAsmType;
    } else {
      throwInvalidAsmCast(definition.groupAlternatives.asmType, castToAsmType,
          definition.groupAsmTypeDefinition);
    }
  }

  private void updateLocalVarIfNecessary(AsmGrammarElementDefinition definition) {
    if (definition.attribute != null && definition.isAttributeLocalVar) {
      var localVarDefinition = (AsmGrammarLocalVarDefinition) definition.symbolTable()
          .resolveNode(definition.attribute.name);
      if (localVarDefinition == null) {
        throw buildIllegalStateException(definition,
            "Assigning to unknown local variable %s.".formatted(definition.attribute.name));
      }
      localVarDefinition.asmType = definition.asmType;
    }
  }

  private void validateAttributeAsmType(AsmGrammarElementDefinition definition) {
    if (definition.attribute != null && !definition.isAttributeLocalVar) {
      if (!definition.isWithinRepetitionBlock && definition.isPlusEqualsAttributeAssign) {
        throw Diagnostic.error("'+=' assignments are only allowed inside of repetition blocks.",
            definition.sourceLocation()).build();
      }

      if (definition.isWithinRepetitionBlock) {
        if (!definition.isPlusEqualsAttributeAssign) {
          throw Diagnostic.error("Only '+=' assignments are allowed in repetition blocks.",
              definition.sourceLocation()).build();
        }

        var parentAttributeElement = attributesAssignedInParent.get(definition.attribute.name);
        if (parentAttributeElement == null || parentAttributeElement.asmType == null) {
          throw Diagnostic.error(
                  "'%s' does not exist in the surrounding block."
                      .formatted(definition.attribute.name), definition.sourceLocation())
              .note("'+=' assignments have to reference an attribute in the surrounding block.")
              .build();
        }

        if (definition.asmType == null) {
          throw buildIllegalStateException(definition,
              "AsmType of asm element could not be resolved.");
        }

        if (!definition.asmType.canBeCastTo(parentAttributeElement.asmType)) {
          throw Diagnostic.error(
                  "Element of AsmType %s cannot be '+=' assigned to attribute %s of AsmType %s."
                      .formatted(definition.asmType, definition.attribute.name,
                          parentAttributeElement.asmType), definition.sourceLocation())
              .locationDescription(parentAttributeElement,
                  "Attribute %s is assigned to AsmType %s here.", definition.attribute.name,
                  parentAttributeElement.asmType)
              .build();
        }
      }
    }
  }

  @Override
  public Void visit(AsmGrammarLiteralDefinition definition) {

    if (definition.stringLiteral != null) {
      visitAsmStringLiteralUsage(definition);
      return null;
    }

    if (definition.id == null) {
      throw buildIllegalStateException(definition,
          "AsmGrammarLiteral is not a StringLiteral "
              + "and does not reference a grammar rule / function / local variable.");
    }

    var invocationSymbolOrigin = definition.symbolTable().resolveNode(definition.id.name);
    if (invocationSymbolOrigin == null) {
      throw buildIllegalStateException(definition, "Symbol %s used in grammar rule does not exist."
          .formatted(definition.id.name));
    } else if (invocationSymbolOrigin instanceof AsmGrammarRuleDefinition rule) {
      visitAsmRuleInvocation(definition, rule);
    } else if (invocationSymbolOrigin instanceof AsmGrammarLocalVarDefinition localVar) {
      visitAsmLocalVarUsage(definition, localVar);
    } else if (invocationSymbolOrigin instanceof FunctionDefinition function) {
      visitAsmFunctionInvocation(definition, function);
    } else {
      throw Diagnostic.error(("Symbol %s used in grammar rule does not reference a grammar rule "
              + "/ function / local variable.").formatted(definition.id.name), definition)
          .locationDescription(invocationSymbolOrigin, "Symbol %s is defined here.",
              definition.id.name)
          .build();
    }

    return null;
  }

  private void visitAsmStringLiteralUsage(AsmGrammarLiteralDefinition definition) {
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

  private void visitAsmRuleInvocation(AsmGrammarLiteralDefinition enclosingAsmLiteral,
                                      AsmGrammarRuleDefinition invokedRule) {
    if (invokedRule.asmType == null) {
      invokedRule.accept(this);
    }

    if (invokedRule.asmType == null) {
      throw buildIllegalStateException(enclosingAsmLiteral,
          "Could not resolve AsmType of grammar rule %s.".formatted(invokedRule.identifier().name));
    }

    determineAsmTypeForEnclosingLiteral(enclosingAsmLiteral, invokedRule.asmType);
  }

  private void visitAsmLocalVarUsage(AsmGrammarLiteralDefinition enclosingAsmLiteral,
                                     AsmGrammarLocalVarDefinition localVar) {
    if (!enclosingAsmLiteral.parameters.isEmpty()) {
      throw Diagnostic.error("Local variable with parameters.", enclosingAsmLiteral)
          .note("Usage of a local variable cannot have parameters.").build();
    }

    if (localVar.asmType == null) {
      localVar.accept(this);
    }

    if (localVar.asmType == null) {
      throw buildIllegalStateException(enclosingAsmLiteral,
          "Could not resolve AsmType of local variable %s.".formatted(localVar.identifier().name));
    }

    determineAsmTypeForEnclosingLiteral(enclosingAsmLiteral, localVar.asmType);
  }

  private void visitAsmFunctionInvocation(AsmGrammarLiteralDefinition enclosingAsmLiteral,
                                          FunctionDefinition function) {
    if (function.type == null) {
      function.accept(this);
    }

    if (enclosingAsmLiteral.parameters.size() != function.params.size()) {
      throw Diagnostic.error("Arguments Mismatch", enclosingAsmLiteral)
          .locationDescription(function, "Expected %d arguments.", function.params.size())
          .locationDescription(enclosingAsmLiteral, "But got %d arguments.",
              enclosingAsmLiteral.parameters.size())
          .build();
    }

    for (int i = 0; i < enclosingAsmLiteral.parameters.size(); i++) {
      var asmParam = enclosingAsmLiteral.parameters.get(i);
      asmParam.accept(this);
      Objects.requireNonNull(asmParam.asmType);

      var argumentType = function.params.get(i).typeLiteral.type;
      Objects.requireNonNull(argumentType);

      if (!canImplicitCast(asmParam.asmType.toOperationalType(), argumentType)) {
        throw Diagnostic.error("Type Mismatch in function argument", enclosingAsmLiteral)
            .locationDescription(function.params.get(i), "Expected %s.", argumentType)
            .locationDescription(asmParam, "Got %s (from %s).",
                asmParam.asmType.toOperationalType(), asmParam.asmType)
            .build();
      }
    }

    Objects.requireNonNull(function.retType.type);
    var returnType = AsmType.getAsmTypeFromOperationalType(function.retType.type);
    determineAsmTypeForEnclosingLiteral(enclosingAsmLiteral, returnType);
  }

  private void determineAsmTypeForEnclosingLiteral(AsmGrammarLiteralDefinition enclosingAsmLiteral,
                                                   AsmType beforeCastType) {
    if (enclosingAsmLiteral.asmTypeDefinition != null) {
      var castToAsmType = getAsmTypeFromAsmTypeDefinition(enclosingAsmLiteral.asmTypeDefinition);
      if (beforeCastType.canBeCastTo(castToAsmType)) {
        enclosingAsmLiteral.asmType = castToAsmType;
      } else {
        throwInvalidAsmCast(beforeCastType, castToAsmType, enclosingAsmLiteral.asmTypeDefinition);
      }
    } else {
      enclosingAsmLiteral.asmType = beforeCastType;
    }
  }

  private AsmType getAsmTypeFromAsmTypeDefinition(AsmGrammarTypeDefinition definition) {
    var correspondingAsmType = AsmType.ASM_TYPES.get(definition.id.name);
    if (correspondingAsmType == null) {
      throw buildIllegalStateException(definition,
          "Symbol resolution found asm type %s but the typechecker could not find it.".formatted(
              definition.id.name));
    }
    return correspondingAsmType;
  }

  @Override
  public Void visit(AsmGrammarLocalVarDefinition definition) {
    if (definition.asmLiteral.id != null && definition.asmLiteral.id.name.equals("null")) {
      definition.asmType = VoidAsmType.instance();
      return null;
    }

    definition.asmLiteral.accept(this);
    definition.asmType = definition.asmLiteral.asmType;
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
    definition.definitions.forEach(def -> def.accept(this));
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
    if (definition.kind == CpuFunctionDefinition.BehaviorKind.START) {
      definition.expr.accept(this);
      var exprType = Objects.requireNonNull(definition.expr.type);
      // FIXME: the type must fit into the memory index (address).
      if (!(exprType instanceof DataType)) {
        throw Diagnostic.error("Type mismatch", definition.expr)
            .description("Expected typ of DataType but got %s", exprType)
            .build();
      }
    } else {
      throw new IllegalStateException("Not implemented behavior kind: " + definition.kind);
    }

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

    var origin = Objects.requireNonNull(expr.symbolTable).requireAs(expr, Node.class);

    if (origin instanceof ConstantDefinition constDef) {
      if (constDef.value.type == null) {
        constDef.accept(this);
      }
      expr.type = constDef.value.type;
      return null;
    }

    if (origin instanceof FormatDefinition.RangeFormatField field) {
      // FIXME: Unfortonatley the format fields need to be specified in declare-after-use for now
      expr.type = field.type;
      return null;
    }

    if (origin instanceof FormatDefinition.TypedFormatField field) {
      // FIXME: Unfortonatley the format fields need to be specified in declare-after-use for now
      expr.type = field.typeLiteral.type;
      return null;
    }

    if (origin instanceof FormatDefinition.DerivedFormatField field) {
      if (field.expr.type == null) {
        field.expr.accept(this);
      }
      expr.type = field.expr.type;
      return null;
    }

    if (origin instanceof Parameter parameter) {
      expr.type = parameter.typeLiteral.type;
      return null;
    }

    if (origin instanceof CounterDefinition counter) {
      if (counter.typeLiteral.type == null) {
        counter.accept(this);
      }
      expr.type = Objects.requireNonNull(counter.typeLiteral.type);
      return null;
    }

    if (origin instanceof LetExpr letExpr) {
      // No need to check because this can only be the case if we are inside the let statement.
      expr.type = Objects.requireNonNull(letExpr.valueExpr.type);
      return null;
    }

    if (origin instanceof LetStatement letStatement) {
      // No need to check because this can only be the case if we are inside the let statement.
      expr.type = Objects.requireNonNull(letStatement.getTypeOf(expr.name));
      return null;
    }

    if (origin instanceof FunctionDefinition functionDefinition) {
      // It's a call without arguments
      if (functionDefinition.type == null) {
        functionDefinition.accept(this);
      }

      if (!functionDefinition.params.isEmpty()) {
        throw Diagnostic.error("Invalid Function Call", expr)
            .description("Expected `%s` arguments but got `%s`", functionDefinition.params.size(),
                0)
            .build();
      }
      expr.type = functionDefinition.retType.type;
      return null;
    }

    if (origin instanceof RegisterDefinition registerDefinition) {
      if (registerDefinition.type == null) {
        registerDefinition.accept(this);
      }
      expr.type = Objects.requireNonNull(registerDefinition.type);
      return null;
    }

    if (origin != null) {
      // It's not a builtin but we don't handle it yet.
      throw new RuntimeException("Don't handle class " + origin.getClass().getName());
    }

    // It's also possible to call functions without parenthesis if the function doesn't take any
    // arguments.
    var matchingBuiltins = BuiltInTable.builtIns()
        .filter(b -> b.signature().argTypeClasses().isEmpty())
        .filter(b -> b.name().toLowerCase().equals(expr.name))
        .toList();

    if (matchingBuiltins.size() == 1) {
      expr.type = matchingBuiltins.get(0).returns(List.of());
      return null;
    }

    // The symbol resolver should have caught that
    throw new IllegalStateException("Cannot find symbol %s".formatted(expr.name));

  }

  private void visitLogicalBinaryExpression(BinaryExpr expr) {
    var leftTyp = Objects.requireNonNull(expr.left.type);

    // Both sides must be boolean
    if (!(leftTyp instanceof BoolType) && !canImplicitCast(leftTyp, Type.bool())) {
      throw Diagnostic.error("Type Mismatch", expr)
          .locationDescription(expr, "Expected a Boolean here but the left side was an `%s`",
              leftTyp)
          .description("The `%s` operator only works on booleans.", expr.operator())
          .build();
    }
    expr.left = wrapImplicitCast(expr.left, Type.bool());

    var rightTyp = Objects.requireNonNull(expr.right.type);
    if (!(rightTyp instanceof BoolType) && !canImplicitCast(rightTyp, Type.bool())) {
      throw Diagnostic.error("Type Mismatch", expr)
          .locationDescription(expr, "Expected a Boolean here but the right side was an `%s`",
              rightTyp)
          .description("The `%s` operator only works on booleans.", expr.operator())
          .build();
    }
    expr.right = wrapImplicitCast(expr.right, Type.bool());

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
    if (Operator.arithmeticOperators.contains(expr.operator())
        || Operator.artihmeticComparisons.contains(expr.operator())) {

      if (leftTyp.equals(Type.bool())) {
        expr.left = wrapImplicitCast(expr.left, Type.bits(1));
        leftTyp = Objects.requireNonNull(expr.left.type);
      }
      if (rightTyp.equals(Type.bool())) {
        expr.right = wrapImplicitCast(expr.right, Type.bits(1));
        rightTyp = Objects.requireNonNull(expr.right.type);
      }

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
        expr.right = new CastExpr(expr.right, closestUIntType);
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
      return null;
    }

    // If only one type is const, cast it to it's partner (or as close as possible)
    if (leftTyp instanceof ConstantType leftConstType) {
      expr.left = new CastExpr(expr.left, leftConstType.closestTo(rightTyp));
      leftTyp = Objects.requireNonNull(expr.left.type);
    } else if (rightTyp instanceof ConstantType rightConstType) {
      expr.right =
          new CastExpr(expr.right, rightConstType.closestTo(leftTyp));
      rightTyp = Objects.requireNonNull(expr.right.type);
    }

    // Long Multiply has different rules than all other arithmetic operations
    if (expr.operator() == Operator.LongMultiply) {
      // At this point both must be Bits or a subtype
      var leftBitWidth = ((BitsType) leftTyp).bitWidth();
      var rightBitWidth = ((BitsType) rightTyp).bitWidth();
      if (leftBitWidth != rightBitWidth) {
        throw Diagnostic.error("Type Mismatch", expr)
            .description("Both sides must have the same width but left is `%s` while right is `%s`",
                leftTyp, rightTyp)
            .build();
      }

      // Rules determining the return type (switched input operators omitted because of
      // commutative property)
      // SInt<N> +# SInt<N> -> SInt<2*N>
      // SInt<N> +# UInt<N> -> SInt<2*N>
      // SInt<N> +# Bits<N> -> SInt<2*N>
      // UInt<N> +# UInt<N> -> UInt<2*N>
      // UInt<N> +# Bits<N> -> UInt<2*N>
      // Bits<N> +# Bits<N> -> Bits<2*N>
      if (leftTyp instanceof SIntType || rightTyp instanceof SIntType) {
        expr.type = Type.signedInt(leftBitWidth * 2);
        return null;
      } else if (leftTyp instanceof UIntType || rightTyp instanceof UIntType) {
        expr.type = Type.unsignedInt(leftBitWidth * 2);
        return null;
      } else {
        expr.type = Type.bits(leftBitWidth * 2);
      }
    }

    var bitWidth = ((BitsType) leftTyp).bitWidth();
    var sizedUInt = Type.unsignedInt(bitWidth);
    var sizedSInt = Type.signedInt(bitWidth);
    var sizedBits = Type.bits(bitWidth);
    var specialBinaryPattern = Map.of(
        Pair.of(sizedUInt, sizedBits), Pair.of(sizedUInt, sizedUInt),
        Pair.of(sizedBits, sizedUInt), Pair.of(sizedUInt, sizedUInt),
        Pair.of(sizedSInt, sizedBits), Pair.of(sizedSInt, sizedSInt),
        Pair.of(sizedBits, sizedSInt), Pair.of(sizedSInt, sizedSInt)
    );

    if (((BitsType) leftTyp).bitWidth() == ((BitsType) rightTyp).bitWidth()
        && specialBinaryPattern.containsKey(Pair.of(leftTyp, rightTyp))) {
      var target = Objects.requireNonNull(specialBinaryPattern.get(Pair.of(leftTyp, rightTyp)));
      if (!leftTyp.equals(target.left())) {
        expr.left = new CastExpr(expr.left, target.left());
        leftTyp = Objects.requireNonNull(expr.left.type);
      } else {
        expr.right = new CastExpr(expr.right, target.right());
        rightTyp = Objects.requireNonNull(expr.right.type);
      }
    }

    // Apply general implicit casting rules after specialised once.
    expr.left = wrapImplicitCast(expr.left, rightTyp);
    leftTyp = Objects.requireNonNull(expr.left.type);

    expr.right = wrapImplicitCast(expr.right, leftTyp);
    rightTyp = Objects.requireNonNull(expr.right.type);

    if (!leftTyp.equals(rightTyp)) {
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
    // Arithmetic grouping
    if (expr.expressions.size() == 1) {
      expr.expressions.get(0).accept(this);
      expr.type = expr.expressions.get(0).type;
      return null;
    }

    // String concat
    for (var argument : expr.expressions) {
      argument.accept(this);
      var argType = Objects.requireNonNull(argument.type);
      if (!argType.equals(Type.string())) {
        throw Diagnostic.error("Type Mismatch", argument.location())
            .locationNote(argument, "Expected string but got `%s`", argType)
            .description("The string concat operator can only concat strings.")
            .build();
      }
    }
    expr.type = Type.string();
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
    expr.from.accept(this);
    expr.to.accept(this);

    var fromType = Objects.requireNonNull(expr.from.type);
    var toType = Objects.requireNonNull(expr.to.type);

    if (!(fromType instanceof BitsType) && !(fromType instanceof ConstantType)) {
      throw Diagnostic.error("Type Mismatch", expr.from)
          .description("The from part of a range must be a number but was %s", fromType)
          .build();
    }

    if (!(toType instanceof BitsType) && !(toType instanceof ConstantType)) {
      throw Diagnostic.error("Type Mismatch", expr.to)
          .description("The to part of a range must be a number but was %s", fromType)
          .build();
    }

    var fromVal = constantEvaluator.eval(expr.from).value();
    var toVal = constantEvaluator.eval(expr.to).value();

    if (toVal.compareTo(fromVal) > 0) {
      throw Diagnostic.error("Invalid range", expr)
          .description("From is %s but to is %s, but ranges must be decreasing", fromVal, toVal)
          .build();
    }

    // FIXME: There is no range type so I am not sure what to assign here
    expr.type = null;
    return null;
  }

  /**
   * Parses the type literal to an actual type.
   *
   * <p>The parsing can be dependent on the context the literal is placed.
   * Sometimes there can be a preferred bit size, like in castings when the source already has a bit
   * size.
   *
   * <p>This function doesn't modify any node, and the caller needs to do that.
   *
   * @param expr              of the literal.
   * @param preferredBitWidth of the target type, will only apply if nothing else is found.
   * @return the parsed type.
   */
  private Type parseTypeLiteral(TypeLiteral expr, @Nullable Integer preferredBitWidth) {
    var base = expr.baseType.pathToString();

    if (base.equals("Bool")) {
      if (!expr.sizeIndices.isEmpty()) {
        throw Diagnostic.error("Invalid Type Notation", expr.location())
            .description("The Bool type doesn't use the size notation as it is always one bit.")
            .build();
      }
      return Type.bool();
    }

    // The basic types SINT<n>, UINT<n> and BITS<n>
    if (Arrays.asList("SInt", "UInt", "Bits").contains(base)) {

      if (expr.sizeIndices.isEmpty() && preferredBitWidth == null) {
        throw Diagnostic.error("Invalid Type Notation", expr.location())
            .description(
                "Unsized `%s` can only be used in special places when it's obvious what the bit"
                    + " width should be.",
                base)
            .help("Try adding a size parameter here.")
            .build();
      }

      if (!expr.sizeIndices.isEmpty()
          && (expr.sizeIndices.size() != 1 || expr.sizeIndices.get(0).size() != 1)) {
        throw Diagnostic.error("Invalid Type Notation", expr.location())
            .description("The %s type requires exactly one size parameter.", base)
            .build();
      }

      int bitWidth;
      if (!expr.sizeIndices.isEmpty()) {
        var widthExpr = expr.sizeIndices.get(0).get(0);
        widthExpr.accept(this);
        bitWidth = constantEvaluator.eval(widthExpr).value().intValueExact();

        if (bitWidth < 1) {
          throw Diagnostic.error("Invalid Type Notation", widthExpr.location())
              .locationDescription(widthExpr.location(),
                  "Width must of a %s must be greater than 1 but was %s", base, bitWidth)
              .build();
        }
      } else {
        bitWidth = Objects.requireNonNull(preferredBitWidth);
      }

      return switch (base) {
        case "SInt" -> Type.signedInt(bitWidth);
        case "UInt" -> Type.unsignedInt(bitWidth);
        case "Bits" -> Type.bits(bitWidth);
        default -> throw new IllegalStateException("Unexpected value: " + base);
      };
    }

    // Find the type from the symbol table
    var usingDef = expr.symbolTable().findAs(((Identifier) expr.baseType), UsingDefinition.class);
    if (usingDef != null) {
      if (usingDef.typeLiteral.type == null) {
        usingDef.typeLiteral.accept(this);
      }

      return Objects.requireNonNull(usingDef.typeLiteral.type);
    }

    throw new RuntimeException(
        "No type with the name `%s` exists.".formatted(expr.baseType.pathToString()));
  }

  @Override
  public Void visit(TypeLiteral expr) {
    expr.type = parseTypeLiteral(expr, null);
    return null;
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
        expr.computedTarget = BuiltInTable.NEG;
        if (!(innerType instanceof BitsType) && !(innerType instanceof ConstantType)) {
          throw Diagnostic
              .error("Type Mismatch", expr)
              .description("Expected a numerical type but got `%s`", innerType)
              .build();
        }
      }
      case COMPLEMENT -> {
        expr.computedTarget = BuiltInTable.NOT;
        if (!(innerType instanceof BitsType)) {
          throw Diagnostic
              .error("Type Mismatch", expr)
              .description("Expected a numerical type with fixed bit-width but got `%s`", innerType)
              .build();
        }
      }
      case LOG_NOT -> {
        expr.computedTarget = BuiltInTable.NOT;
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

  private void visitSliceCall(CallExpr expr) {
    var identifierTarget = ((Identifier) expr.target.path());
    if (identifierTarget.type == null) {
      identifierTarget.accept(this);
    }

    var targetType = Objects.requireNonNull(identifierTarget.type);
    if (!(targetType.getClass() != BitsType.class)) {
      throw Diagnostic.error("Type Mismatch", identifierTarget)
          .description("Only bit types can be sliced but the target was a `%s`", targetType)
          .build();
    }

    var rangeExpr = (RangeExpr) expr.flatArgs().get(0);
    int from = constantEvaluator.eval(rangeExpr.from).value().intValueExact();
    int to = constantEvaluator.eval(rangeExpr.to).value().intValueExact();

    // NOTE: From is always larger than to
    var rangeSize = (from - to) + 1;
    if (rangeSize < 1) {
      throw Diagnostic.error("Invalid Range", rangeExpr)
          .description("Range must be >= 1 but was %s", rangeSize)
          .build();
    }

    // FIXME: Verify that the range is inside the target.

    // That isn't quite correct, shouldn't SInt also work?
    expr.type = Type.bits(rangeSize);
  }

  @Override
  public Void visit(CallExpr expr) {

    // Might be a slice
    if (expr.flatArgs().size() == 1 && expr.flatArgs().get(0) instanceof RangeExpr) {
      visitSliceCall(expr);
      return null;
    }

    var callTarget = Objects.requireNonNull(expr.symbolTable)
        .findAs(expr.target.path().pathToString(), Definition.class);

    // Handle register
    if (callTarget instanceof RegisterFileDefinition registerFile) {
      if (expr.argsIndices.size() != 1 || expr.argsIndices.get(0).size() != 1) {
        throw Diagnostic.error("Invalid Register Usage", expr)
            .description("A register call must have exactly one argument.")
            .build();
      }

      var argList = expr.argsIndices.get(0);
      var arg = argList.get(0);
      arg.accept(this);

      if (registerFile.type == null) {
        registerFile.accept(this);
      }
      var requiredArgType =
          Objects.requireNonNull(Objects.requireNonNull(registerFile.type).argTypes().get(0));
      argList.set(0, wrapImplicitCast(arg, requiredArgType));
      arg = argList.get(0);
      var actualArgType = Objects.requireNonNull(arg.type);

      if (!actualArgType.equals(requiredArgType)) {
        throw Diagnostic.error("Type Mismatch", arg)
            .description("Expected %s but got `%s`", requiredArgType, actualArgType)
            .build();
      }

      expr.type = Objects.requireNonNull(registerFile.type).resultType();
      expr.computedTarget = registerFile;
      return null;
    }

    // Handle memory
    if (callTarget instanceof MemoryDefinition memDef) {
      if (expr.argsIndices.size() != 1 || expr.argsIndices.get(0).size() != 1) {
        throw Diagnostic.error("Invalid Memory Usage", expr)
            .description("Memory access must have exactly one argument.")
            .build();
      }

      var argList = expr.argsIndices.get(0);
      var arg = argList.get(0);
      if (arg.type == null) {
        arg.accept(this);
      }

      if (memDef.type == null) {
        memDef.accept(this);
      }
      var requiredArgType =
          Objects.requireNonNull(Objects.requireNonNull(memDef.type).argTypes().get(0));

      argList.set(0, wrapImplicitCast(arg, requiredArgType));
      arg = argList.get(0);
      var actualArgType = Objects.requireNonNull(arg.type);

      if (!actualArgType.equals(requiredArgType)) {
        throw Diagnostic.error("Type Mismatch", arg)
            .description("Expected %s but got `%s`", requiredArgType, actualArgType)
            .build();
      }

      var callType = Objects.requireNonNull(memDef.type).resultType();
      if (expr.target instanceof SymbolExpr targetSymbol) {
        int multiplier = constantEvaluator.eval(targetSymbol.size).value().intValueExact();
        if (!(callType instanceof BitsType callBitsType)) {
          throw new IllegalStateException();
        }

        callType = callBitsType.scaleBy(multiplier);
      }

      expr.computedTarget = memDef;
      expr.type = callType;
      return null;
    }

    // Handle Counter
    if (callTarget instanceof CounterDefinition counterDef) {
      if (counterDef.typeLiteral.type == null) {
        counterDef.accept(this);
      }
      var counterType = counterDef.typeLiteral.type;
      expr.computedTarget = counterDef;
      expr.type = counterType;

      if (!expr.argsIndices.isEmpty()) {
        throw Diagnostic.error("Invalid Counter Usage", expr)
            .description("A counter isn't a callable thing.")
            .build();
      }

      if (counterDef.kind == CounterDefinition.CounterKind.PROGRAM) {
        var allowedSubcalls = List.of("next");
        // FIXME: better error message
        if (expr.subCalls.stream().anyMatch(s -> !allowedSubcalls.contains(s.id().name))) {
          throw Diagnostic.error("Unknown counter access", expr)
              .description("Unknown counter access, only the following are allowed %s",
                  allowedSubcalls)
              .build();
        }
        if (expr.subCalls.stream().anyMatch(s -> !s.argsIndices().isEmpty())) {
          throw Diagnostic.error("Invalid next of counter", expr)
              .description("`.next` doesn't take any arguments")
              .build();
        }
      } else {
        throw new RuntimeException("Don't know how to handle group counters yet");
      }

      return null;
    }

    // User defined functions
    if (callTarget instanceof FunctionDefinition functionDef) {
      if (functionDef.type == null) {
        functionDef.accept(this);
      }

      var funcType = Objects.requireNonNull(functionDef.type);
      var expectedArgCount = funcType.argTypes().size();
      var actualArgCount = expr.flatArgs().size();
      if (expectedArgCount != actualArgCount) {
        throw Diagnostic.error("Invalid Function Call", expr)
            .description("Expected `%s` arguments but got `%s`", expectedArgCount, actualArgCount)
            .build();
      }

      // NOTE: This code is so cursed because we are retaining the structure of the code rather than
      // the semantic in the AST.
      int argCount = 0;
      for (var i = 0; i < expr.argsIndices.size(); i++) {
        for (var j = 0; j < expr.argsIndices.get(i).size(); j++) {
          var argNode = expr.argsIndices.get(i).get(j);
          argNode.accept(this);
          expr.argsIndices.get(i)
              .set(j, wrapImplicitCast(argNode, funcType.argTypes().get(argCount)));
          argNode = expr.argsIndices.get(i).get(j);
          if (!Objects.requireNonNull(argNode.type).equals(funcType.argTypes().get(argCount))) {
            throw Diagnostic.error("Type Mismatch", argNode)
                .description("Expected %s but got `%s`", funcType.argTypes(), argNode)
                .build();
          }
          argCount++;
        }
      }

      expr.computedTarget = functionDef;
      expr.type = funcType.resultType();
      return null;
    }

    // Relocation call (similar to function)
    if (callTarget instanceof RelocationDefinition relocationDef) {
      if (relocationDef.type == null) {
        relocationDef.accept(this);
      }

      var relocationType = Objects.requireNonNull(relocationDef.type);
      var expectedArgCount = relocationType.argTypes().size();
      var actualArgCount = expr.flatArgs().size();
      if (expectedArgCount != actualArgCount) {
        throw Diagnostic.error("Invalid Function Call", expr)
            .description("Expected %s arguments but got `%s`", expectedArgCount, actualArgCount)
            .build();
      }

      // NOTE: This code is so cursed because we are retaining the structure of the code rather than
      // the semantic in the AST.
      int argCount = 0;
      for (var i = 0; i < expr.argsIndices.size(); i++) {
        for (var j = 0; j < expr.argsIndices.get(i).size(); j++) {
          var argNode = expr.argsIndices.get(i).get(j);
          argNode.accept(this);
          expr.argsIndices.get(i)
              .set(j, wrapImplicitCast(argNode, relocationType.argTypes().get(argCount)));
          argNode = expr.argsIndices.get(i).get(j);
          if (!Objects.requireNonNull(argNode.type)
              .equals(relocationType.argTypes().get(argCount))) {
            throw Diagnostic.error("Type Mismatch", argNode)
                .description("Expected %s but got `%s`", relocationType.argTypes(), argNode)
                .build();
          }
          argCount++;
        }
      }

      expr.computedTarget = relocationDef;
      expr.type = relocationType.resultType();
      return null;
    }


    // Builtin function
    var builtin = getBuiltIn(expr.target.path().pathToString());
    if (builtin != null) {
      expr.flatArgs().forEach(a -> a.accept(this));
      var argTypes = Objects.requireNonNull(expr.flatArgs().stream().map(v -> v.type)).toList();

      if (!builtin.takes(argTypes)) {
        throw Diagnostic.error("Type Mismatch", expr)
            .description("Expected %s but got `%s`", builtin.signature().argTypeClasses(), argTypes)
            .build();
      }

      // Note: cannot set the computed type because builtins aren't a definition.
      expr.computedBuiltIn = builtin;
      expr.type = builtin.returns(argTypes);
      return null;
    }

    var description = callTarget == null
        ? "No callable object with that name exists"
        : "Cannot call a `%s`".formatted(callTarget);
    throw Diagnostic.error("Unknown callable `%s`".formatted(expr.target.path().pathToString()),
            expr.location)
        .description("%s", description)
        .build();
  }

  @SuppressWarnings("UnusedVariable")
  @Override
  public Void visit(IfExpr expr) {
    expr.condition.accept(this);
    expr.condition = wrapImplicitCast(expr.condition, Type.bool());
    var condType = Objects.requireNonNull(expr.condition.type);
    if (condType != Type.bool()) {
      throw Diagnostic.error("Type Mismatch", expr.condition)
          .description("Expected %s but got `%s`", Type.bool(), condType)
          .build();
    }

    expr.thenExpr.accept(this);
    expr.elseExpr.accept(this);
    var thenType = Objects.requireNonNull(expr.thenExpr.type);
    var elseType = Objects.requireNonNull(expr.elseExpr.type);

    // Apply general implicit casting rules after specialised once.
    expr.thenExpr = wrapImplicitCast(expr.thenExpr, elseType);
    thenType = Objects.requireNonNull(expr.thenExpr.type);
    expr.elseExpr = wrapImplicitCast(expr.elseExpr, thenType);
    elseType = Objects.requireNonNull(expr.elseExpr.type);

    if (!thenType.equals(elseType)) {
      throw Diagnostic.error("Type Mismatch", expr)
          .description(
              "Both the than and else branch should have the same type "
                  + "but than is `%s` and else is `%s`.",
              thenType, elseType)
          .build();
    }

    expr.type = thenType;
    return null;
  }

  @Override
  public Void visit(LetExpr expr) {
    expr.valueExpr.accept(this);
    expr.body.accept(this);
    expr.type = expr.body.type;
    return null;
  }

  @Override
  public Void visit(CastExpr expr) {
    expr.value.accept(this);
    var valType = Objects.requireNonNull(expr.value.type);


    expr.typeLiteral.type = parseTypeLiteral(expr.typeLiteral, preferredBitWidthOf(valType));
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
    throw new IllegalStateException(
        "No %s should ever reach the Typechecker".formatted(expr.getClass().getSimpleName()));
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
    statement.statements.forEach(s -> s.accept(this));
    return null;
  }

  @Override
  public Void visit(LetStatement statement) {
    if (statement.identifiers.size() == 1) {
      statement.valueExpr.accept(this);
      statement.body.accept(this);
      return null;
    }

    throw new RuntimeException("Cannot handle tuple unpacking yet");
  }

  @Override
  public Void visit(IfStatement statement) {
    statement.condition.accept(this);
    statement.condition = wrapImplicitCast(statement.condition, Type.bool());
    var condType = Objects.requireNonNull(statement.condition.type);
    if (condType != Type.bool()) {
      throw Diagnostic.error("Type Mismatch", statement.condition)
          .description("Expected `%s` but got `%s`", Type.bool(), condType)
          .build();
    }

    statement.thenStmt.accept(this);
    if (statement.elseStmt != null) {
      statement.elseStmt.accept(this);
    }
    return null;
  }

  @Override
  public Void visit(AssignmentStatement statement) {
    statement.target.accept(this);
    statement.valueExpression.accept(this);

    var targetType = Objects.requireNonNull(statement.target.type);
    var valueType = Objects.requireNonNull(statement.valueExpression.type);

    if (!targetType.equals(valueType) && canImplicitCast(valueType, targetType)) {
      statement.valueExpression =
          new CastExpr(statement.valueExpression, targetType);
      valueType = targetType;
    }

    if (!targetType.equals(valueType)) {
      throw Diagnostic.error("Type mismatch", statement.valueExpression)
          .description("Expected %s but got `%s`", targetType, valueType)
          .build();
    }

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
    statement.items.forEach(s -> s.accept(this));
    return null;
  }

  @Override
  public Void visit(InstructionCallStatement statement) {
    // FIXME: Is that true?
    // Ok my assumption is that when we point to an instruction we are gonna need named arguments
    // and if we call an pseudo instruction we take unnamed (positional) arguments

    if (statement.instrDef instanceof InstructionDefinition instrDef) {
      // FIXME fetch it
      //instrDef.accept(this);

      if (!statement.unnamedArguments.isEmpty()) {
        var loc = statement.unnamedArguments.get(0).location()
            .join(statement.unnamedArguments.get(statement.unnamedArguments.size() - 1).location());
        throw Diagnostic.error("Invalid Arguments", loc)
            .description("Calls to instructions only accept named arguments")
            .build();
      }

      // Implicit cast and check the arguments
      for (var i = 0; i < statement.namedArguments.size(); i++) {
        // TODO: Check all format fields that arne't part of the ecoding
        var format = Objects.requireNonNull(instrDef.formatNode);

        var arg = statement.namedArguments.get(i);
        // FIXME: better error
        var targetType = Objects.requireNonNull(format.getFieldType(arg.name().name));

        arg.value().accept(this);

        statement.namedArguments.set(i,
            new InstructionCallStatement.NamedArgument(arg.name(),
                wrapImplicitCast(arg.value(), targetType)));
        arg = statement.namedArguments.get(i);
        var actualType = Objects.requireNonNull(arg.value().type);

        if (!targetType.equals(actualType)) {
          throw Diagnostic.error("Type Mismatch", arg.location())
              .description("Expected `%s` but got `%s`", targetType, actualType)
              .build();
        }
      }


    } else if (statement.instrDef instanceof PseudoInstructionDefinition pseudoDef) {
      // FIXME fetch it
      //pseudoDef.accept(this);

      if (!statement.namedArguments.isEmpty()) {
        var loc = statement.namedArguments.get(0).location()
            .join(statement.namedArguments.get(statement.namedArguments.size() - 1).location());
        throw Diagnostic.error("Invalid Arguments", loc)
            .description("Calls to pseudo instructions only accept unnamed (positional) arguments")
            .build();
      }

      // Check the argument and parameter count
      var paramCount = pseudoDef.params.size();
      var argCount = statement.unnamedArguments.size();
      if (paramCount != argCount) {
        throw Diagnostic.error("Arguments Mismatch", statement.location())
            .description("Expected %s arguments but got %s", paramCount, argCount)
            .build();
      }

      // Implicit cast and check the arguments
      for (var i = 0; i < statement.unnamedArguments.size(); i++) {
        var targetType = Objects.requireNonNull(pseudoDef.params.get(i).typeLiteral.type);
        var arg = statement.unnamedArguments.get(i);
        arg.accept(this);
        statement.unnamedArguments.set(i,
            wrapImplicitCast(arg, targetType));
        arg = statement.unnamedArguments.get(i);
        var actualType = Objects.requireNonNull(statement.unnamedArguments.get(i).type);

        if (!targetType.equals(actualType)) {
          throw Diagnostic.error("Type Mismatch", arg.location())
              .description("Expected `%s` but got `%s`", targetType, actualType)
              .build();
        }
      }

    } else {
      throw new IllegalStateException("Unknown instruction definition");
    }


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
