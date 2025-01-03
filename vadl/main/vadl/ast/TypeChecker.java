package vadl.ast;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vadl.error.Diagnostic;
import vadl.types.BitsType;
import vadl.types.SIntType;
import vadl.types.Type;
import vadl.types.asmTypes.AsmType;
import vadl.types.asmTypes.GroupAsmType;
import vadl.types.asmTypes.StringAsmType;
import vadl.types.asmTypes.VoidAsmType;
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

  private void throwIllegalState(Node node, String message) {
    throw new IllegalStateException(
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
      // only visit rules that have not yet been visited,
      // as rules can be invoked by other rules and may already have an AsmType
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
        throwIllegalState(definition, "AsmType of rule body could not be resolved.");
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

  private static void preprocessAlternativesElements(AsmGrammarAlternativesDefinition definition) {
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
      throwIllegalState(definition,
          "Typechecker found an AsmGrammarAlternative without elements.");
    }

    if (elementsToConsider.size() == 1) {
      elementsToConsider.get(0).accept(this);
      return elementsToConsider.get(0).asmType;
    }

    var groupSubtypes = new ArrayList<AsmType>();
    var alreadyAssignedAttributes = new HashMap<String, AsmGrammarElementDefinition>();

    for (var element : elementsToConsider) {

      if (element.asmType == null) {
        if (element.repetitionAlternatives != null) {
          attributesAssignedInParent = alreadyAssignedAttributes;
        }
        element.accept(this);
      }

      appendToAsmGroupType(element, groupSubtypes, alreadyAssignedAttributes);
    }

    return new GroupAsmType(groupSubtypes);
  }

  private void appendToAsmGroupType(AsmGrammarElementDefinition element,
                                    List<AsmType> groupSubtypes,
                                    Map<String, AsmGrammarElementDefinition> assignedAttributes) {
    // these two if statements are mutually exclusive:
    // grammar syntax does not allow for an element to be assigned to an attribute
    // and be of GroupAsmType at the same time

    // consider elements which are assigned to an attribute
    if (element.attribute != null && !element.isAttributeLocalVar
        && !element.isWithinRepetitionBlock) {
      groupSubtypes.add(element.asmType);

      var otherElement = assignedAttributes.put(element.attribute.name, element);
      if (otherElement != null) {
        throw Diagnostic.error(
                "Found multiple assignments to attribute %s in a grammar rule."
                    .formatted(element.attribute.name), element.sourceLocation())
            .locationDescription(otherElement,
                "Attribute %s has already been assigned to here.", element.attribute.name)
            .build();
      }
    }

    // flatten nested GroupAsmTypes from group and option blocks
    // ignore the type of repetition blocks
    if (element.repetitionAlternatives == null
        && element.asmType instanceof GroupAsmType elementAsmType) {
      groupSubtypes.addAll(elementAsmType.getSubtypes());
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
      throwIllegalState(definition, "AsmType of an asm alternative could not be resolved.");
      return;
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

    // TODO: expression type checking
    //if (definition.semanticPredicate != null) {
    // definition.semanticPredicate.accept(this);
    //}

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
      throwIllegalState(definition, "AsmType of asm literal could not be resolved.");
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
      throwIllegalState(definition, "AsmType of group element could not be resolved.");
      return;
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
        throwIllegalState(definition, "Assigning to unknown local variable %s.".formatted(
            definition.attribute.name));
        return;
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
          throwIllegalState(definition, "AsmType of asm element could not be resolved.");
          return;
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
      throwIllegalState(definition,
          "AsmGrammarLiteral is not a StringLiteral "
              + "and does not reference a grammar rule / function / local variable.");
      return null;
    }

    var invocationSymbolOrigin = definition.symbolTable().resolveNode(definition.id.name);
    if (invocationSymbolOrigin == null) {
      throwIllegalState(definition, "Symbol %s used in grammar rule does not exist."
          .formatted(definition.id.name));
    } else if (invocationSymbolOrigin instanceof AsmGrammarRuleDefinition rule) {
      visitAsmRuleInvocation(definition, rule);
    } else if (invocationSymbolOrigin instanceof AsmGrammarLocalVarDefinition localVar) {
      visitAsmLocalVarUsage(definition, localVar);
    } else if (invocationSymbolOrigin instanceof FunctionDefinition function) {
      // TODO check input fits to types of arguments
      // TODO check return type and infer asm type
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

  private void visitAsmRuleInvocation(AsmGrammarLiteralDefinition definition,
                                      AsmGrammarRuleDefinition invokedRule) {
    if (invokedRule.asmType == null) {
      invokedRule.accept(this);
    }

    if (invokedRule.asmType == null) {
      throwIllegalState(definition,
          "Could not resolve AsmType of grammar rule %s.".formatted(invokedRule.identifier().name));
      return;
    }

    determineAsmTypeForEnclosingLiteral(definition, invokedRule.asmType);
  }

  private void visitAsmLocalVarUsage(AsmGrammarLiteralDefinition enclosingAsmLiteral,
                                     AsmGrammarLocalVarDefinition localVar) {
    if (localVar.asmType == null) {
      localVar.accept(this);
    }

    if (localVar.asmType == null) {
      throwIllegalState(enclosingAsmLiteral,
          "Could not resolve AsmType of local variable %s.".formatted(localVar.identifier().name));
      return;
    }

    determineAsmTypeForEnclosingLiteral(enclosingAsmLiteral, localVar.asmType);
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

  // NullAway claims that a nullable value is returned,
  // but if correspondingAsmType is null an exception is thrown
  @SuppressWarnings("NullAway")
  private AsmType getAsmTypeFromAsmTypeDefinition(AsmGrammarTypeDefinition definition) {
    var correspondingAsmType = AsmType.ASM_TYPES.get(definition.id.name);
    if (correspondingAsmType == null) {
      throwIllegalState(definition,
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
