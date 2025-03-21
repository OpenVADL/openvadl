// SPDX-FileCopyrightText : © 2025 TU Wien <vadl@tuwien.ac.at>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

package vadl.ast;

import static java.util.Objects.requireNonNull;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

  private final Set<Statement> checkedStatements =
      Collections.newSetFromMap(new IdentityHashMap<>());
  private final Set<Definition> checkedDefinitions =
      Collections.newSetFromMap(new IdentityHashMap<>());

  public TypeChecker() {
    constantEvaluator = new ConstantEvaluator();
  }

  /**
   * Typecheck the expression if not yet checked.
   *
   * @param expr to check.
   * @return the type of the expression.
   */
  private Type check(Expr expr) {
    // Expressions store their type so we can look at them to see if they were already evaluated.
    if (expr.type == null) {
      expr.accept(this);
    }

    return requireNonNull(expr.type);
  }

  /**
   * Typecheck the statement, if not yet checked.
   *
   * @param stmt to check.
   */
  private void check(Statement stmt) {
    if (!checkedStatements.contains(stmt)) {
      stmt.accept(this);
      checkedStatements.add(stmt);
    }
  }

  /**
   * Typecheck the definition, if not yet checked.
   *
   * @param def to check.
   */
  private void check(Definition def) {
    if (!checkedDefinitions.contains(def)) {
      def.accept(this);
      checkedDefinitions.add(def);
    }
  }

  /**
   * Verify that the program is well-typed.
   *
   * @param ast to verify
   * @throws Diagnostic if the program isn't well typed
   */
  public void verify(Ast ast) {
    ast.definitions.forEach(this::check);
  }

  private void throwUnimplemented(Node node) {
    throw new RuntimeException(
        "The typechecker doesn't know how to handle `%s` yet, found in %s".formatted(
            node.getClass().getSimpleName(), node.location().toIDEString()));
  }

  private Diagnostic typeMissmatchError(WithSourceLocation locatable, Type expected, Type actual) {
    return typeMissmatchError(locatable, "`%s`".formatted(expected), actual);
  }

  private Diagnostic typeMissmatchError(WithSourceLocation locatable, String expectation,
                                        Type actual) {
    return Diagnostic.error("Type Mismatch", locatable)
        .locationDescription(locatable, "Expected `%s` but got `%s`.",
            expectation, actual)
        .build();
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
        var value = fromConstant.getValue();
        return value.equals(BigInteger.ZERO) || value.equals(BigInteger.ONE);
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

    // FormatType<"?", T1> => T2 iff T1 => T2
    if (from.getClass() == FormatType.class) {
      return canImplicitCast(((FormatType) from).format.typeLiteral.type(), to);
    }

    // T1 => FormatType<"?", T2> iff T1 => T2
    if (to.getClass() == FormatType.class) {
      return canImplicitCast(from, ((FormatType) to).format.typeLiteral.type());
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
    var innerType = requireNonNull(inner.type);
    if (innerType.equals(to)) {
      return inner;
    }

    if (!canImplicitCast(innerType, to)) {
      if (!(innerType instanceof ConstantType innerConstTyp)) {
        return inner;
      }

      // For constant types we cast to them anyway to the clostest type to improve the error message
      return new CastExpr(inner, innerConstTyp.closestTo(to));
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
  private static BuiltInTable.BuiltIn getBuiltIn(String name, List<Type> argTypes) {
    // FIXME: Namespace should be propper resolved and not that hacky
    if (name.startsWith("VADL::")) {
      name = name.substring("VADL::".length());
    }

    // FIXME: We decided that in the future this behaivor will be removed and only the
    //  signed/unsigned versions are available.
    // Discussion: https://ea.complang.tuwien.ac.at/vadl/open-vadl/issues/287#issuecomment-23771

    // There are some pseudo functions that will get resolved to either the signed or unsinged one.
    var pseudoRewrites = Map.of("div", List.of("sdiv", "udiv"), "mod", List.of("smod", "umod"));
    if (pseudoRewrites.containsKey(name)) {
      var singed = argTypes.stream().anyMatch(t -> t instanceof SIntType);
      name = pseudoRewrites.get(name).get(singed ? 0 : 1);
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
    Type valType = check(definition.value);

    if (definition.typeLiteral == null) {
      // Do nothing on purpose
    } else {
      definition.typeLiteral.type =
          parseTypeLiteral(definition.typeLiteral, preferredBitWidthOf(valType));
      Type litType = requireNonNull(definition.typeLiteral.type);

      if (!canImplicitCast(valType, litType)) {
        throw Diagnostic.error("Type missmatch: expected %s, got %s".formatted(litType, valType),
            definition.value.location()
        ).build();
      }

      // Insert a cast if needed
      if (!litType.equals(valType)) {
        definition.value = new CastExpr(definition.value, definition.typeLiteral);
        check(definition.value);
      }
    }

    return null;
  }

  @Override
  public Void visit(FormatDefinition definition) {
    var type = check(definition.typeLiteral);
    if (!(type instanceof BitsType bitsType)) {
      throw typeMissmatchError(definition.typeLiteral, "bits type", type);
    }

    var bitWidth = bitsType.bitWidth();
    var bitsVerifier = new FormatBitsVerifier(bitWidth);
    var nextOccupiedBit = bitWidth - 1;

    for (var field : definition.fields) {
      if (field instanceof FormatDefinition.TypedFormatField typedField) {
        var fieldType = check(typedField.typeLiteral);

        if (!(fieldType instanceof BitsType fieldBitsType)) {
          throw Diagnostic.error("Bits Type expected", typedField.typeLiteral)
              .description("Format fields can only be assigned a bits type.")
              .build();
        }
        typedField.range = new FormatDefinition.BitRange(nextOccupiedBit,
            nextOccupiedBit - (fieldBitsType.bitWidth() - 1));
        nextOccupiedBit -= fieldBitsType.bitWidth();
        bitsVerifier.addType(fieldBitsType);

      } else if (field instanceof FormatDefinition.RangeFormatField rangeField) {
        if (rangeField.typeLiteral != null) {
          check(rangeField.typeLiteral);
          rangeField.type = requireNonNull(rangeField.typeLiteral.type);
        }

        int fieldBitWidth = 0;
        rangeField.computedRanges = new ArrayList<>();
        for (var range : rangeField.ranges) {
          range.accept(this);

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
        check(dfField.expr);
      } else {
        throw new RuntimeException("Unknown FormatField Class ".concat(field.getClass().getName()));
      }
    }

    if (bitsVerifier.hasViolations()) {
      throw Diagnostic.error("Invalid Format", definition)
          .description("%s", requireNonNull(bitsVerifier.getViolationsMessage()))
          .build();
    }

    return null;
  }

  @Override
  public Void visit(InstructionSetDefinition definition) {
    if (definition.extendingNode != null) {
      check(definition.extendingNode);
    }

    for (var def : definition.definitions) {
      check(def);
    }

    // FIXME: Verify at least one programcounter
    return null;
  }

  @Override
  public Void visit(CounterDefinition definition) {
    check(definition.typeLiteral);
    return null;
  }

  @Override
  public Void visit(MemoryDefinition definition) {
    check(definition.addressTypeLiteral);
    check(definition.dataTypeLiteral);
    definition.type = Type.concreteRelation(
        List.of(requireNonNull(definition.addressTypeLiteral.type)),
        requireNonNull(definition.dataTypeLiteral.type));
    return null;
  }

  @Override
  public Void visit(RegisterDefinition definition) {
    check(definition.typeLiteral);
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
    definition.typeLiteral.argTypes().forEach(this::check);
    check(definition.typeLiteral.resultType());
    definition.type = Type.concreteRelation(
        definition.typeLiteral.argTypes().stream().map(arg -> arg.type).toList(),
        requireNonNull(definition.typeLiteral.resultType().type));

    return null;
  }

  @Override
  public Void visit(InstructionDefinition definition) {
    if (requireNonNull(definition.formatNode).typeLiteral.type == null) {
      check(definition.formatNode);
    }

    check(definition.behavior);

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
    definition.params.forEach(param -> check(param.typeLiteral));

    // Check the statements
    definition.statements.forEach(this::check);

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
      check(param.typeLiteral);
    }

    check(definition.resultTypeLiteral);
    check(definition.expr);

    // Verify the types are compatible
    var definedType = requireNonNull(definition.resultTypeLiteral.type);
    definition.expr = wrapImplicitCast(definition.expr, definedType);
    var actualType = requireNonNull(definition.expr.type);
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

      check(encodingField.value);
      var fieldType = requireNonNull(
          requireNonNull(definition.formatNode)
              .getFieldType(encodingField.field.name));

      encodingField.value = wrapImplicitCast(encodingField.value, fieldType);
      var valueType = requireNonNull(encodingField.value.type);

      if (!fieldType.equals(valueType)) {
        throw typeMissmatchError(encodingField.value, fieldType, valueType);
      }
    }
    return null;
  }

  @Override
  public Void visit(AssemblyDefinition definition) {
    check(definition.expr);
    var exprType = requireNonNull(definition.expr.type);

    if (exprType.getClass() != StringType.class) {
      throw typeMissmatchError(definition.expr, "`String`", exprType);
    }
    return null;
  }

  @Override
  public Void visit(UsingDefinition definition) {
    check(definition.typeLiteral);
    return null;
  }

  @Override
  public Void visit(AbiPseudoInstructionDefinition abiPseudoInstructionDefinition) {
    // Isn't type checked on purpose because there is nothing to type check.
    return null;
  }

  @Override
  public Void visit(FunctionDefinition definition) {
    definition.params.forEach(param -> check(param.typeLiteral));
    check(definition.retType);
    check(definition.expr);

    var retType = requireNonNull(definition.retType.type);
    definition.expr = wrapImplicitCast(definition.expr, retType);
    var exprType = requireNonNull(definition.expr.type);

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
    // Isn't type checked on purpose because there is nothing to type check.
    return null;
  }

  @Override
  public Void visit(EnumerationDefinition definition) {
    var type = definition.enumType != null ? check(definition.enumType) : null;
    if (type != null && !(type instanceof BitsType bitsType)) {
      throw Diagnostic.error("Type mismatch", definition)
          .locationDescription(requireNonNull(definition.enumType),
              "Expected bits type but got `%s`", type)
          .note("In future there will be support for Strings and other types as well.")
          .build();
    }

    // check if there are enums
    if (definition.entries.isEmpty()) {
      throw Diagnostic.error("No enumeration entries", definition)
          .locationDescription(definition,
              "The enumeration has no entries, but at least one is required.")
          .build();
    }


    int nextVal = 0;
    for (var entry : definition.entries) {
      if (entry.value != null) {
        nextVal = constantEvaluator.eval(entry.value).value().intValueExact() + 1;
        continue;
      }

      // if value is not set, we use the last value + 1.
      entry.value =
          new IntegerLiteral(String.valueOf(nextVal), SourceLocation.INVALID_SOURCE_LOCATION);
      nextVal++;
    }

    definition.entries.forEach(e -> check(requireNonNull(e.value)));

    // Insert casts when type exists
    if (type != null) {
      for (var entry : definition.entries) {
        entry.value = wrapImplicitCast(requireNonNull(entry.value), type);
        if (!entry.value.type().equals(type)) {
          throw typeMissmatchError(entry.value, type, entry.value.type());
        }
      }
    }

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
    definition.items.forEach(this::check);
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
    definition.definitions.forEach(this::check);

    // Check the number of occurrences in the ABI.
    for (var entry : SpecialPurposeRegisterDefinition.Purpose.numberOfOccurrencesAbi.entrySet()) {
      var purpose = entry.getKey();
      var registers = definition.definitions.stream().filter(
              x -> x instanceof SpecialPurposeRegisterDefinition specialPurposeRegisterDefinition
                  && specialPurposeRegisterDefinition.purpose == purpose)
          .toList();

      switch (entry.getValue()) {
        case ONE -> {
          if (registers.isEmpty()) {
            throw Diagnostic.error(
                "No " + purpose.name() + " registers were declared but one was expected",
                definition.sourceLocation()).build();
          } else if (registers.size() != 1) {
            throw Diagnostic.error(
                "Multiple " + purpose.name() + " registers were declared but only one was expected",
                SourceLocation.join(registers.stream().map(Node::sourceLocation).toList())).build();
          }
        }
        case OPTIONAL -> {
          if (!(registers.isEmpty() || registers.size() == 1)) {
            throw Diagnostic.error(
                "Multiple " + purpose.name()
                    + " registers were declared but zero or one was expected",
                SourceLocation.join(registers.stream().map(Node::sourceLocation).toList())).build();
          }
        }
        case AT_LEAST_ONE -> {
          if (registers.isEmpty()) {
            throw Diagnostic.error(
                "Zero " + purpose.name() + " registers were declared but at least one was expected",
                definition.sourceLocation()).build();
          }
        }
        default -> throw new RuntimeException("enum variant not handled");
      }
    }

    // Check whether there exists just one pseudo instruction.
    for (var kind : AbiPseudoInstructionDefinition.Kind.values()) {
      var pseudoInstructions = definition.definitions
          .stream()
          .filter(x -> x instanceof AbiPseudoInstructionDefinition y && y.kind == kind)
          .toList();

      if (pseudoInstructions.size() > 1) {
        throw Diagnostic.error(
                "Multiple " + kind.name()
                    + " pseudo instructions were declared but only one was expected",
                SourceLocation.join(pseudoInstructions.stream().map(Node::sourceLocation).toList()))
            .build();
      }

      if (pseudoInstructions.isEmpty()) {
        throw Diagnostic.error(
            "No register purpose was defined for " + kind.name(), definition.loc).build();
      }
    }

    return null;
  }

  @Override
  public Void visit(AsmDescriptionDefinition definition) {
    definition.commonDefinitions.forEach(this::check);

    for (var rule : definition.rules) {
      // only visit rules that have not yet been visited,
      // as rules can be invoked by other rules and may already have an AsmType
      if (rule.asmType == null) {
        check(rule);
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
    if (definition.isBuiltinRule) {
      definition.asmType =
          getAsmTypeFromAsmTypeDefinition(requireNonNull(definition.asmTypeDefinition));
      return null;
    }

    if (!asmRuleInvocationChain.add(definition.identifier().name)) {
      var cycle =
          String.join(" -> ", asmRuleInvocationChain) + " -> " + definition.identifier().name;
      throw Diagnostic.error("Found a cycle in grammar rules: %s.".formatted(cycle),
          definition.sourceLocation()).build();
    }

    check(definition.alternatives);
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
          check(element);
        }

        appendToAsmGroupType(element, groupSubtypeMap, alreadyAssignedAttributes);
      } else {
        check(element);
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

    // flatten nested GroupAsmTypes from option blocks
    // repetition blocks do not have a type,
    // because their attributes always reference the enclosing block
    if (element.optionAlternatives != null) {
      if (element.asmType instanceof GroupAsmType elementAsmType) {
        elementAsmType.getSubtypeMap().keySet().forEach(
            key -> throwErrorOnNestedMultipleAttributeAssignment(element, groupSubtypeMap, key)
        );
        groupSubtypeMap.putAll(elementAsmType.getSubtypeMap());
      } else {
        // this case appears for option blocks with one attributed element like: [attr = some @type]
        // type of option block is lifted from @GroupAsmType(@type) to @type
        // so to flatten we need to get the name of this one attribute
        var attribute = element.optionAlternatives.alternatives.get(0).stream().filter(
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
      check(definition.localVar);
      definition.asmType = definition.localVar.asmType;
    }

    visitAsmLiteral(definition);

    visitGroupAlternatives(definition);

    if (definition.optionAlternatives != null) {
      check(definition.optionAlternatives);
      definition.asmType = definition.optionAlternatives.asmType;
    }
    if (definition.repetitionAlternatives != null) {
      check(definition.repetitionAlternatives);
      definition.asmType = definition.repetitionAlternatives.asmType;
    }

    if (definition.semanticPredicate != null) {
      check(definition.semanticPredicate);
      if (definition.semanticPredicate.type != Type.bool()) {
        throw Diagnostic.error("Semantic predicate expression does not evaluate to Boolean.",
            definition.semanticPredicate).build();
      }
    }

    // actions that depend on the resolved asm type of this element
    validateLocalVarAssignment(definition);
    validateAttributeAsmType(definition);

    return null;
  }

  private void visitAsmLiteral(AsmGrammarElementDefinition definition) {
    if (definition.asmLiteral == null) {
      return;
    }

    check(definition.asmLiteral);
    if (definition.asmLiteral.asmType == null) {
      throw buildIllegalStateException(definition, "AsmType of asm literal could not be resolved.");
    }
    definition.asmType = definition.asmLiteral.asmType;
  }

  private void visitGroupAlternatives(AsmGrammarElementDefinition definition) {
    if (definition.groupAlternatives == null) {
      return;
    }

    check(definition.groupAlternatives);
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

  private void validateLocalVarAssignment(AsmGrammarElementDefinition definition) {
    if (definition.attribute != null && definition.isAttributeLocalVar) {
      var localVarDefinition = (AsmGrammarLocalVarDefinition) definition.symbolTable()
          .resolveNode(definition.attribute.name);
      if (localVarDefinition == null) {
        throw buildIllegalStateException(definition,
            "Assigning to unknown local variable %s.".formatted(definition.attribute.name));
      }

      requireNonNull(definition.asmType);
      requireNonNull(localVarDefinition.asmType);
      if (localVarDefinition.asmType != definition.asmType) {
        throw Diagnostic.error("Type Mismatch", definition)
            .locationDescription(localVarDefinition, "Local variable %s is "
                    + "defined with AsmType %s.",
                definition.attribute.name, localVarDefinition.asmType)
            .locationDescription(definition, "Local variable %s is "
                    + "updated with another AsmType %s.",
                definition.attribute.name, definition.asmType)
            .build();
      }
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
      if (definition.asmType == null) {
        visitAsmStringLiteralUsage(definition);
      }
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
      check(invokedRule);
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
      check(localVar);
    }

    if (localVar.asmType == null) {
      throw buildIllegalStateException(enclosingAsmLiteral,
          "Could not resolve AsmType of local variable %s.".formatted(localVar.identifier().name));
    }

    determineAsmTypeForEnclosingLiteral(enclosingAsmLiteral, localVar.asmType);
  }

  private void visitAsmFunctionInvocation(AsmGrammarLiteralDefinition enclosingAsmLiteral,
                                          FunctionDefinition function) {
    check(function);

    if (enclosingAsmLiteral.parameters.size() != function.params.size()) {
      throw Diagnostic.error("Arguments Mismatch", enclosingAsmLiteral)
          .locationDescription(function, "Expected %d arguments.", function.params.size())
          .locationDescription(enclosingAsmLiteral, "But got %d arguments.",
              enclosingAsmLiteral.parameters.size())
          .build();
    }

    for (int i = 0; i < enclosingAsmLiteral.parameters.size(); i++) {
      var asmParam = enclosingAsmLiteral.parameters.get(i);
      check(asmParam);
      requireNonNull(asmParam.asmType);

      var argumentType = function.params.get(i).typeLiteral.type;
      requireNonNull(argumentType);

      if (!canImplicitCast(asmParam.asmType.toOperationalType(), argumentType)) {
        throw Diagnostic.error("Type Mismatch in function argument", enclosingAsmLiteral)
            .locationDescription(function.params.get(i), "Expected %s.", argumentType)
            .locationDescription(asmParam, "Got %s (from %s).",
                asmParam.asmType.toOperationalType(), asmParam.asmType)
            .build();
      }
    }

    requireNonNull(function.retType.type);
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
      if (definition.asmLiteral.asmTypeDefinition != null) {
        definition.asmType =
            getAsmTypeFromAsmTypeDefinition(definition.asmLiteral.asmTypeDefinition);
      } else {
        throw Diagnostic.error("Local variable without AsmType", definition)
            .note("Local variables declarations with value 'null' must have an AsmType.")
            .build();
      }
      return null;
    }

    check(definition.asmLiteral);
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
    definition.params.forEach(param -> check(param.typeLiteral));

    // Check the statements
    definition.statements.forEach(this::check);

    return null;
  }

  @Override
  public Void visit(SpecialPurposeRegisterDefinition definition) {
    // Check whether the number of registers is correct.
    // There can be only one stack pointer. However, there might be multiple argument registers.
    var actual =
        SpecialPurposeRegisterDefinition.Purpose.numberOfExpectedArguments.get(definition.purpose);

    if (actual == null) {
      throw Diagnostic.error("Cannot determine number of expected registers",
          definition.sourceLocation()).build();
    }

    if (actual == SpecialPurposeRegisterDefinition.Occurrence.ONE) {
      if (definition.exprs.size() != 1) {
        throw Diagnostic.error("Number of registers is incorrect. This definition expects only one",
            definition.sourceLocation()).build();
      }
    }

    if (actual == SpecialPurposeRegisterDefinition.Occurrence.ONE) {
      if (definition.exprs.isEmpty()) {
        throw Diagnostic.error(
            "Number of registers is incorrect. This definition expects at least one.",
            definition.sourceLocation()).build();
      }
    }

    return null;
  }

  @Override
  public Void visit(MicroProcessorDefinition definition) {
    definition.definitions.forEach(this::check);
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
      check(definition.expr);
      var exprType = requireNonNull(definition.expr.type);
      // FIXME: the type must fit into the memory index (address).
      if (!(exprType instanceof DataType)) {
        throw typeMissmatchError(definition.expr, "DataType", exprType);
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

  /**
   * Identifiers and IdentifierPaths are quite similar in what they do and how they should be
   * handled.
   *
   * @param expr is the identifier.
   */
  private void visitIdentifiable(Expr expr) {
    Node origin;
    String innerName;
    String fullName;

    if (expr instanceof Identifier identifier) {
      origin = requireNonNull(expr.symbolTable).requireAs(identifier, Node.class);
      innerName = identifier.name;
      fullName = identifier.name;
    } else if (expr instanceof IdentifierPath path) {
      origin = requireNonNull(expr.symbolTable).findAs(path, Node.class);
      var segments = path.pathToSegments();
      innerName = segments.get(segments.size() - 1);
      fullName = path.pathToString();
    } else {
      throw new IllegalStateException();
    }

    if (origin instanceof ConstantDefinition constDef) {
      check(constDef);
      expr.type = constDef.value.type;
      return;
    }

    if (origin instanceof FormatDefinition.RangeFormatField field) {
      // FIXME: Unfortonatley the format fields need to be specified in declare-after-use for now
      expr.type = field.type;
      return;
    }

    if (origin instanceof FormatDefinition.TypedFormatField field) {
      // FIXME: Unfortonatley the format fields need to be specified in declare-after-use for now
      expr.type = field.typeLiteral.type;
      return;
    }

    if (origin instanceof FormatDefinition.DerivedFormatField field) {
      check(field.expr);
      expr.type = field.expr.type;
      return;
    }

    if (origin instanceof Parameter parameter) {
      expr.type = parameter.typeLiteral.type;
      return;
    }

    if (origin instanceof CounterDefinition counter) {
      check(counter);
      expr.type = requireNonNull(counter.typeLiteral.type);
      return;
    }

    if (origin instanceof LetExpr letExpr) {
      // No need to check because this can only be the case if we are inside the let statement.
      expr.type = requireNonNull(letExpr.valueExpr.type);
      return;
    }

    if (origin instanceof LetStatement letStatement) {
      // No need to check because this can only be the case if we are inside the let statement.
      expr.type = requireNonNull(letStatement.getTypeOf(innerName));
      return;
    }

    if (origin instanceof FunctionDefinition functionDefinition) {
      // It's a call without arguments
      check(functionDefinition);

      if (!functionDefinition.params.isEmpty()) {
        throw Diagnostic.error("Invalid Function Call", expr)
            .description("Expected `%s` arguments but got `%s`", functionDefinition.params.size(),
                0)
            .build();
      }
      expr.type = functionDefinition.retType.type;
      return;
    }

    if (origin instanceof RegisterDefinition registerDefinition) {
      check(registerDefinition);
      expr.type = requireNonNull(registerDefinition.type);
      return;
    }

    if (origin instanceof EnumerationDefinition.Entry enumEntry) {
      expr.type = check(requireNonNull(enumEntry.value));
      return;
    }

    if (origin != null) {
      // It's not a builtin but we don't handle it yet.
      throw new RuntimeException("Don't handle class " + origin.getClass().getName());
    }

    // It's also possible to call functions without parenthesis if the function doesn't take any
    // arguments.
    var matchingBuiltins = BuiltInTable.builtIns()
        .filter(b -> b.signature().argTypeClasses().isEmpty())
        .filter(b -> b.name().toLowerCase().equals(innerName))
        .toList();

    if (matchingBuiltins.size() == 1) {
      expr.type = matchingBuiltins.get(0).returns(List.of());
      return;
    }

    // The symbol resolver should have caught that
    throw new IllegalStateException("Cannot find symbol %s".formatted(fullName));
  }

  @Override
  public Void visit(Identifier expr) {
    visitIdentifiable(expr);
    return null;
  }

  private void visitLogicalBinaryExpression(BinaryExpr expr) {
    var leftTyp = requireNonNull(expr.left.type);

    // Both sides must be boolean
    if (!(leftTyp instanceof BoolType) && !canImplicitCast(leftTyp, Type.bool())) {
      throw Diagnostic.error("Type Mismatch", expr)
          .locationDescription(expr, "Expected a `Bool` here but the left side was an `%s`",
              leftTyp)
          .description("The `%s` operator only works on booleans.", expr.operator())
          .build();
    }
    expr.left = wrapImplicitCast(expr.left, Type.bool());

    var rightTyp = requireNonNull(expr.right.type);
    if (!(rightTyp instanceof BoolType) && !canImplicitCast(rightTyp, Type.bool())) {
      throw Diagnostic.error("Type Mismatch", expr)
          .locationDescription(expr, "Expected a `Bool` here but the right side was an `%s`",
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
    var leftTyp = check(expr.left);
    var rightTyp = check(expr.right);

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
        leftTyp = requireNonNull(expr.left.type);
      }
      if (rightTyp.equals(Type.bool())) {
        expr.right = wrapImplicitCast(expr.right, Type.bits(1));
        rightTyp = requireNonNull(expr.right.type);
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
        rightTyp = requireNonNull(expr.right.type);
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
      leftTyp = requireNonNull(expr.left.type);
    } else if (rightTyp instanceof ConstantType rightConstType) {
      expr.right =
          new CastExpr(expr.right, rightConstType.closestTo(leftTyp));
      rightTyp = requireNonNull(expr.right.type);
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
      var target = requireNonNull(specialBinaryPattern.get(Pair.of(leftTyp, rightTyp)));
      if (!leftTyp.equals(target.left())) {
        expr.left = new CastExpr(expr.left, target.left());
        leftTyp = expr.left.type();
      } else {
        expr.right = new CastExpr(expr.right, target.right());
        rightTyp = expr.right.type();
      }
    }

    // Apply general implicit casting rules after specialised once.
    expr.left = wrapImplicitCast(expr.left, rightTyp);
    leftTyp = expr.left.type();

    expr.right = wrapImplicitCast(expr.right, leftTyp);
    rightTyp = expr.right.type();

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
      check(expr.expressions.get(0));
      expr.type = expr.expressions.get(0).type;
      return null;
    }

    var types = expr.expressions.stream().map(this::check).toList();

    // String concatination
    if (types.stream().allMatch(x -> x instanceof StringType)) {
      expr.type = Type.string();
      return null;
    }

    // Bits concatination
    if (types.stream().allMatch(x -> x instanceof BitsType)) {
      var width = types.stream().map(t -> ((BitsType) t).bitWidth()).reduce(0, Integer::sum);
      expr.type = Type.bits(width);
      return null;
    }

    throw Diagnostic.error("Type Mismatch", expr)
        .locationNote(expr, "Provided types: %s",
            String.join(", ", types.stream().map(Type::toString).toList()))
        .description(
            "The concatenation operation can only be applied on a set of strings or a set of bits.")
        .build();
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
    var fromType = check(expr.from);
    var toType = check(expr.to);

    if (!(fromType instanceof BitsType) && !(fromType instanceof ConstantType)) {
      throw Diagnostic.error("Type Mismatch", expr.from)
          .description("The from part of a range must be a number but was `%s`", fromType)
          .build();
    }

    if (!(toType instanceof BitsType) && !(toType instanceof ConstantType)) {
      throw Diagnostic.error("Type Mismatch", expr.to)
          .description("The to part of a range must be a number but was `%s`", fromType)
          .build();
    }

    var fromVal = constantEvaluator.eval(expr.from).value();
    var toVal = constantEvaluator.eval(expr.to).value();

    if (toVal.compareTo(fromVal) > 0) {
      throw Diagnostic.error("Invalid range", expr)
          .description("From is %s but to is %s, but ranges must be decreasing", fromVal, toVal)
          .build();
    }

    // FIXME: The type doesn't really make sense but we don't have a propper range type
    expr.type = Type.bits(fromVal.intValueExact() - toVal.intValueExact());
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

    if (base.equals("String")) {
      if (!expr.sizeIndices.isEmpty()) {
        throw Diagnostic.error("Invalid Type Notation", expr.location())
            .description("The String type doesn't use the size notation.")
            .build();
      }
      return Type.string();
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
        check(widthExpr);
        bitWidth = constantEvaluator.eval(widthExpr).value().intValueExact();

        if (bitWidth < 1) {
          throw Diagnostic.error("Invalid Type Notation", widthExpr.location())
              .locationDescription(widthExpr.location(),
                  "Width must of a %s must be greater than 1 but was %s", base, bitWidth)
              .build();
        }
      } else {
        bitWidth = requireNonNull(preferredBitWidth);
      }

      return switch (base) {
        case "SInt" -> Type.signedInt(bitWidth);
        case "UInt" -> Type.unsignedInt(bitWidth);
        case "Bits" -> Type.bits(bitWidth);
        default -> throw new IllegalStateException("Unexpected value: " + base);
      };
    }

    // Find the type from the symbol table
    var typeTarget = expr.symbolTable().findAs(((Identifier) expr.baseType), Node.class);
    if (typeTarget instanceof UsingDefinition usingDef) {
      return check(usingDef.typeLiteral);
    }

    if (typeTarget instanceof FormatDefinition formatDef) {
      check(formatDef);
      return new FormatType(formatDef);
    }

    var sb = new StringBuilder();
    expr.prettyPrint(0, sb);
    var typeName = sb.toString();
    throw Diagnostic.error("Unknown Type `%s`".formatted(typeName), expr)
        .description("No type with that name exists.")
        .build()
        ;
  }

  @Override
  public Void visit(TypeLiteral expr) {
    expr.type = parseTypeLiteral(expr, null);
    return null;
  }

  @Override
  public Void visit(IdentifierPath expr) {
    visitIdentifiable(expr);
    return null;
  }

  @Override
  public Void visit(UnaryExpr expr) {
    var innerType = check(expr.operand);

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

  /**
   * Visits one or multiple index and slice calls.
   *
   * <p>The index is necessary because at the point this is called some args might have already
   * been consumed.
   *
   * <p>Sets expr.type to the the type of the subcalls.
   *
   * @param expr            to visit
   * @param typeBeforeSlice is the type just before.
   * @param argumentsList   the list or arguments which hold slices or indexes
   */
  private void visitSliceIndexCall(CallIndexExpr expr, Type typeBeforeSlice,
                                   List<CallIndexExpr.Arguments> argumentsList) {
    if (argumentsList.isEmpty()) {
      expr.type = typeBeforeSlice;
      return;
    }
    var args = argumentsList.get(0);

    // FIXME: Adjust for vectors in the future
    if (!(typeBeforeSlice instanceof BitsType targetBitsType)) {
      var loc = expr.target.location().join(args.location);
      throw Diagnostic.error("Type Mismatch", loc)
          .description("Only bit types can be sliced but the target was a `%s`", typeBeforeSlice)
          .build();
    }

    // A range slice
    if (!args.values.isEmpty() && args.values.get(0) instanceof RangeExpr rangeExpr) {
      int from = constantEvaluator.eval(rangeExpr.from).value().intValueExact();
      int to = constantEvaluator.eval(rangeExpr.to).value().intValueExact();

      // NOTE: From is always larger than to
      var rangeSize = (from - to) + 1;
      if (rangeSize < 1) {
        throw Diagnostic.error("Invalid Range", rangeExpr)
            .description("Range must be >= 1 but was %s", rangeSize)
            .build();
      }

      if (from >= targetBitsType.bitWidth()) {
        throw Diagnostic.error("Invalid Range", rangeExpr)
            .description("Range start %d out of bounds for `%s`", from, targetBitsType)
            .build();
      }
      if (to < 0) {
        throw Diagnostic.error("Invalid Range", rangeExpr)
            .description("Range end must be at least zero but was %s", to)
            .build();
      }
      var type = targetBitsType.withBitWidth(rangeSize);
      args.type = type;
      args.computedBitRange = new FormatDefinition.BitRange(from, to);
      visitSliceIndexCall(expr, type, argumentsList.subList(1, argumentsList.size()));
      return;
    }

    // A index (slice)
    if (args.values.size() != 1) {
      var loc = expr.target.location().join(args.location);
      // FIXME: This is wrong, you can also do it with multiple and they get concatinated
      throw Diagnostic.error("Invalid call", loc)
          .description("You can only call `%s` with one argument", typeBeforeSlice)
          .build();
    }

    check(args.values.get(0));
    int sliceIndex = constantEvaluator.eval(args.values.get(0)).value().intValueExact();
    if (sliceIndex >= targetBitsType.bitWidth()) {
      throw Diagnostic.error("Invalid Index", args.values.get(0))
          .description("Index %d out of bounds for `%s`", sliceIndex, targetBitsType)
          .build();
    }
    if (sliceIndex < 0) {
      throw Diagnostic.error("Invalid Index", args.values.get(0))
          .description("Index must be at least zero but was %s", sliceIndex)
          .build();
    }

    var type = targetBitsType.withBitWidth(1);
    args.type = type;
    args.computedBitRange = new FormatDefinition.BitRange(sliceIndex, sliceIndex);
    visitSliceIndexCall(expr, type, argumentsList.subList(1, argumentsList.size()));
  }

  /**
   * Throws if a subcall exists.
   *
   * @param expr       with possibly subcalls.
   * @param callTarget to which is called.
   */
  private void verifyNoSubcall(CallIndexExpr expr, Definition callTarget) {
    verifyNoSubcall(expr, callTarget.getClass().getSimpleName());
  }

  /**
   * Throws if a subcall exists.
   *
   * @param expr       with possibly subcalls.
   * @param targetName to which is called.
   */
  private void verifyNoSubcall(CallIndexExpr expr, String targetName) {
    if (expr.subCalls.isEmpty()) {
      return;
    }

    throw Diagnostic.error("Invalid subcall", expr)
        .description("Calls to %s cannot have subcalls", targetName)
        .build();
  }

  /**
   * At this point we already know what is called but there are still some dangling subcalls that
   * need to be resolved and which might result in a different type.
   *
   * <p>Modiefies the provided expr.
   *
   * @param expr of the call.
   */
  private void visitSubCall(CallIndexExpr expr, Type typeBeforeSubCall) {
    if (expr.subCalls.isEmpty()) {
      expr.type = typeBeforeSubCall;
      return;
    }

    // Might be a format access
    Type type = typeBeforeSubCall;
    for (var subCall : expr.subCalls) {
      var fieldName = subCall.id.name;
      if (!(type instanceof FormatType formatType)) {
        // FIXME: Better error message
        throw Diagnostic.error("Cannot resolve `%s`".formatted(fieldName), expr)
            .description("Because the type up until it is not a format but `%s`",
                requireNonNull(type))
            .build();
      }
      check(formatType.format);

      var fieldType = formatType.format.getFieldType(fieldName);
      if (fieldType == null) {
        var formatName = formatType.format.identifier().name;
        throw Diagnostic.error("Unknown format field `%s`".formatted(fieldName), expr)
            .description("Format `%s` doesn't have any field named `%s`", formatName, fieldName)
            .build();
      }

      subCall.computedFormatFieldBitRange = formatType.format.getFieldRange(fieldName);
      subCall.formatFieldType = fieldType;
      visitSliceIndexCall(expr, subCall.formatFieldType, subCall.argsIndices);
      type = expr.type;
    }
  }

  @Override
  public Void visit(CallIndexExpr expr) {

    // The first call of the multicalls depends on the thing that is beeing called.
    // However since the definitions aren't part of the typesystem we need to resolve them
    // manually.
    // If no target matches, we can assume a slice and index call (depending on the type).

    var callTarget = requireNonNull(expr.symbolTable)
        .findAs(expr.target.path().pathToString(), Definition.class);

    // Handle register File
    if (callTarget instanceof RegisterFileDefinition registerFile) {
      if (expr.argsIndices.isEmpty() || expr.argsIndices.get(0).values.size() != 1) {
        throw Diagnostic.error("Invalid Register Usage", expr)
            .description("A register call must have exactly one argument.")
            .build();
      }

      var argList = expr.argsIndices.get(0);
      var arg = argList.values.get(0);
      check(arg);

      check(registerFile);
      var requiredArgType =
          requireNonNull(requireNonNull(registerFile.type).argTypes().get(0));
      argList.values.set(0, wrapImplicitCast(arg, requiredArgType));
      arg = argList.values.get(0);
      var actualArgType = arg.type();

      if (!actualArgType.equals(requiredArgType)) {
        throw typeMissmatchError(expr, requiredArgType, actualArgType);
      }

      expr.computedTarget = registerFile;
      var typeBeforeIndex = registerFile.type().resultType();
      argList.type = typeBeforeIndex;
      visitSliceIndexCall(expr, typeBeforeIndex,
          expr.argsIndices.subList(1, expr.argsIndices.size()));
      visitSubCall(expr, expr.type());
      return null;
    }

    // Handle memory
    if (callTarget instanceof MemoryDefinition memDef) {
      if (expr.argsIndices.size() != 1 || expr.argsIndices.get(0).values.size() != 1) {
        throw Diagnostic.error("Invalid Memory Usage", expr)
            .description("Memory access must have exactly one argument.")
            .build();
      }
      verifyNoSubcall(expr, memDef);

      var argList = expr.argsIndices.get(0);
      var arg = argList.values.get(0);
      check(arg);

      check(memDef);
      var requiredArgType =
          requireNonNull(requireNonNull(memDef.type).argTypes().get(0));

      argList.values.set(0, wrapImplicitCast(arg, requiredArgType));
      arg = argList.values.get(0);
      var actualArgType = arg.type();

      if (!actualArgType.equals(requiredArgType)) {
        throw typeMissmatchError(expr, requiredArgType, actualArgType);
      }

      var callType = memDef.type().resultType();
      if (expr.target instanceof SymbolExpr targetSymbol) {
        int multiplier = constantEvaluator.eval(targetSymbol.size).value().intValueExact();
        if (!(callType instanceof BitsType callBitsType)) {
          throw new IllegalStateException();
        }

        callType = callBitsType.scaleBy(multiplier);
      }

      expr.computedTarget = memDef;
      expr.type = callType;
      visitSliceIndexCall(expr, expr.type(), expr.argsIndices.subList(1, expr.argsIndices.size()));
      visitSubCall(expr, expr.type());
      return null;
    }

    // Handle Counter
    if (callTarget instanceof CounterDefinition counterDef) {
      check(counterDef);
      var counterType = counterDef.typeLiteral.type;
      expr.computedTarget = counterDef;
      expr.type = counterType;

      if (!expr.argsIndices.isEmpty()) {
        throw Diagnostic.error("Invalid Counter Usage", expr)
            .description("A counter isn't a callable thing.")
            .build();
      }

      // FIXME: Handle slicing and format subcall propperly
      if (counterDef.kind == CounterDefinition.CounterKind.PROGRAM) {
        var allowedSubcalls = List.of("next");
        // FIXME: better error message
        if (expr.subCalls.stream().anyMatch(s -> !allowedSubcalls.contains(s.id.name))) {
          throw Diagnostic.error("Unknown counter access", expr)
              .description("Unknown counter access, only the following are allowed %s",
                  allowedSubcalls)
              .build();
        }
        if (expr.subCalls.stream().anyMatch(s -> !s.argsIndices.isEmpty())) {
          throw Diagnostic.error("Invalid next of counter", expr)
              .description("`.next` doesn't take any arguments")
              .build();
        }
      } else {
        throw new RuntimeException("Don't know how to handle group counters yet");
      }

      //visitSliceIndexCall(expr, expr.type(), expr.argsIndices);
      //visitSubCall(expr, expr.type());
      return null;
    }

    // User defined functions
    if (callTarget instanceof FunctionDefinition functionDef) {
      check(functionDef);
      var funcType = functionDef.type();
      var expectedArgCount = funcType.argTypes().size();
      var actualArgCount = expr.argsIndices.get(0).values.size();
      if (expectedArgCount != actualArgCount) {
        throw Diagnostic.error("Invalid Function Call", expr)
            .description("Expected `%s` arguments but got `%s`", expectedArgCount, actualArgCount)
            .build();
      }

      var args = expr.argsIndices.get(0);
      args.values.forEach(this::check);
      for (int i = 0; i < expectedArgCount; i++) {
        var arg = args.values.get(i);
        args.values.set(i, wrapImplicitCast(arg, funcType.argTypes().get(i)));
        arg = args.values.get(i);
        if (!arg.type().equals(funcType.argTypes().get(i))) {
          throw typeMissmatchError(expr, funcType.argTypes().get(i), arg.type());
        }
      }

      expr.computedTarget = functionDef;
      expr.type = funcType.resultType();
      visitSliceIndexCall(expr, expr.type(), expr.argsIndices.subList(1, expr.argsIndices.size()));
      visitSubCall(expr, expr.type());
      return null;
    }

    // Relocation call (similar to function)
    if (callTarget instanceof RelocationDefinition relocationDef) {
      check(relocationDef);
      var relocationType = relocationDef.type();
      var expectedArgCount = relocationType.argTypes().size();
      var actualArgCount = expr.argsIndices.get(0).values.size();
      if (expectedArgCount != actualArgCount) {
        throw Diagnostic.error("Invalid Function Call", expr)
            .description("Expected %s arguments but got `%s`", expectedArgCount, actualArgCount)
            .build();
      }

      var args = expr.argsIndices.get(0);
      args.values.forEach(this::check);
      for (int i = 0; i < expectedArgCount; i++) {
        var arg = args.values.get(i);
        args.values.set(i, wrapImplicitCast(arg, relocationType.argTypes().get(i)));
        arg = args.values.get(i);
        if (!arg.type().equals(relocationType.argTypes().get(i))) {
          throw typeMissmatchError(expr, relocationType.argTypes().get(i), arg.type());
        }
      }

      expr.computedTarget = relocationDef;
      expr.type = relocationType.resultType();
      visitSliceIndexCall(expr, expr.type(), expr.argsIndices.subList(1, expr.argsIndices.size()));
      visitSubCall(expr, expr.type());
      return null;
    }


    // Builtin function
    List<Expr> args =
        !expr.argsIndices.isEmpty() ? expr.argsIndices.get(0).values : new ArrayList<>();
    var argTypes = args.stream().map(this::check).toList();
    var builtin = getBuiltIn(expr.target.path().pathToString(), argTypes);
    if (builtin != null) {
      // FIXME: Find a better solution that is universal enough for binary operations and builtin
      // functions.

      // If the function is also a unary operation, we instead type check it as if it were a unary
      // operation which has some special type rules.
      if (builtin.operator() != null && builtin.signature().argTypeClasses().size() == 1) {
        var operatorSymbol = requireNonNull(builtin.operator());
        if (operatorSymbol.equals("~") && args.get(0).type instanceof BoolType) {
          operatorSymbol = "!";
        }
        var operator = UnaryOperator.fromSymbol(operatorSymbol);
        var fakeUnExpr =
            new UnaryExpr(new UnOp(operator, expr.location), args.get(0));
        check(fakeUnExpr);

        // Set type and arguments since they might have been wrapped in type casts
        expr.argsIndices.get(0).values.set(0, fakeUnExpr.operand);
        expr.type = fakeUnExpr.type;
      }

      // If the function is also a binary operation, we instead type check it as if it were a binary
      // operation which has some special type rules.
      if (builtin.operator() != null && builtin.signature().argTypeClasses().size() == 2) {
        var operator = requireNonNull(Operator.fromString(builtin.operator()));
        var fakeBinExpr =
            new BinaryExpr(expr.argsIndices.get(0).values.get(0),
                new BinOp(operator, expr.location),
                expr.argsIndices.get(0).values.get(1));
        check(fakeBinExpr);

        // Set type and arguments since they might have been wraped in type casts
        expr.replaceArgsFor(0, List.of(fakeBinExpr.left, fakeBinExpr.right));
        expr.type = fakeBinExpr.type;
      }

      argTypes = requireNonNull(expr.argsIndices.get(0).values.stream().map(v -> v.type)).toList();
      if (!builtin.takes(argTypes)) {
        // FIXME: Better format that error
        throw Diagnostic.error("Type Mismatch", expr)
            .description("Expected %s but got `%s`", builtin.signature().argTypeClasses(), argTypes)
            .build();
      }

      // Note: cannot set the computed type because builtins aren't a definition.
      expr.computedBuiltIn = builtin;
      if (expr.type == null) {
        expr.type = builtin.returns(argTypes);
      }
      visitSliceIndexCall(expr, expr.type(), expr.argsIndices.subList(1, expr.argsIndices.size()));
      visitSubCall(expr, expr.type());
      return null;
    }


    // If nothing else, assume slicing and subcall
    expr.type = check((Identifier) expr.target);
    expr.computedTarget = callTarget;
    visitSliceIndexCall(expr, expr.type(), expr.argsIndices);
    visitSubCall(expr, expr.type());
    return null;
  }

  @SuppressWarnings("UnusedVariable")
  @Override
  public Void visit(IfExpr expr) {
    check(expr.condition);
    expr.condition = wrapImplicitCast(expr.condition, Type.bool());
    var condType = expr.condition.type();
    if (condType != Type.bool()) {
      throw typeMissmatchError(expr, Type.bool(), condType);
    }

    var thenType = check(expr.thenExpr);
    var elseType = check(expr.elseExpr);

    // Apply general implicit casting rules after specialised once.
    expr.thenExpr = wrapImplicitCast(expr.thenExpr, elseType);
    thenType = expr.thenExpr.type();
    expr.elseExpr = wrapImplicitCast(expr.elseExpr, thenType);
    elseType = expr.elseExpr.type();

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
    check(expr.valueExpr);
    expr.type = check(expr.body);
    return null;
  }

  @Override
  public Void visit(CastExpr expr) {
    var valType = check(expr.value);

    expr.typeLiteral.type = parseTypeLiteral(expr.typeLiteral, preferredBitWidthOf(valType));
    var litType = expr.typeLiteral.type();

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
    var candidateType = check(expr.candidate);
    var firstResultType = check(expr.cases.get(0).result);
    for (var kase : expr.cases) {
      kase.patterns.forEach(this::check);
      kase.patterns.replaceAll(p -> wrapImplicitCast(p, candidateType));
      for (var pattern : kase.patterns) {
        var patternType = pattern.type();
        if (!candidateType.equals(patternType)) {
          throw Diagnostic.error("Type Mismatch", pattern)
              .locationDescription(pattern, "Expected `%s`, but got `%s`", candidateType,
                  patternType)
              .note("The type of the candidate and the pattern must be the same.")
              .build();
        }
      }

      // Check that all branches have the same type
      check(kase.result);
      kase.result = wrapImplicitCast(kase.result, firstResultType);
      var resultType = check(kase.result);
      if (!resultType.equals(firstResultType)) {
        throw Diagnostic.error("Type Mismatch", kase.result)
            .locationNote(kase.result, "All results before were of type `%s`, but this is `%s`",
                firstResultType, resultType)
            .description("All branches of a match must have the same type")
            .build();
      }
    }

    check(expr.defaultResult);
    expr.defaultResult = wrapImplicitCast(expr.defaultResult, firstResultType);
    var defaultResultType = expr.defaultResult.type();
    if (!defaultResultType.equals(firstResultType)) {
      throw Diagnostic.error("Type Mismatch", expr.defaultResult)
          .locationNote(expr.defaultResult,
              "All results before were of type `%s`, but this is `%s`",
              firstResultType, defaultResultType)
          .description("All branches of a match must have the same type")
          .build();

    }

    expr.type = firstResultType;
    return null;
  }

  @Override
  public Void visit(ExtendIdExpr expr) {
    throw new IllegalStateException(
        "No %s should ever reach the Typechecker".formatted(expr.getClass().getSimpleName()));
  }

  @Override
  public Void visit(IdToStrExpr expr) {
    throw new IllegalStateException(
        "No %s should ever reach the Typechecker".formatted(expr.getClass().getSimpleName()));
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
    statement.statements.forEach(this::check);
    return null;
  }

  @Override
  public Void visit(LetStatement statement) {
    if (statement.identifiers.size() == 1) {
      check(statement.valueExpr);
      check(statement.body);
      return null;
    }

    throw new RuntimeException("Cannot handle tuple unpacking yet");
  }

  @Override
  public Void visit(IfStatement statement) {
    check(statement.condition);
    statement.condition = wrapImplicitCast(statement.condition, Type.bool());
    var condType = statement.condition.type();
    if (condType != Type.bool()) {
      throw typeMissmatchError(statement.condition, Type.bool(), condType);
    }

    check(statement.thenStmt);
    if (statement.elseStmt != null) {
      check(statement.elseStmt);
    }
    return null;
  }

  @Override
  public Void visit(AssignmentStatement statement) {
    check(statement.target);
    check(statement.valueExpression);

    var targetType = statement.target.type();
    var valueType = statement.valueExpression.type();

    if (!targetType.equals(valueType) && canImplicitCast(valueType, targetType)) {
      statement.valueExpression =
          new CastExpr(statement.valueExpression, targetType);
      valueType = targetType;
    }

    if (!targetType.equals(valueType)) {
      throw typeMissmatchError(statement.valueExpression, targetType, valueType);
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
    statement.items.forEach(this::check);
    return null;
  }

  @Override
  public Void visit(InstructionCallStatement statement) {
    // FIXME: Is that true?
    // Ok my assumption is that when we point to an instruction we are gonna need named arguments
    // and if we call an pseudo instruction we take unnamed (positional) arguments

    if (statement.instrDef instanceof InstructionDefinition instrDef) {
      check(instrDef);

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
        var format = requireNonNull(instrDef.formatNode);

        var arg = statement.namedArguments.get(i);
        // FIXME: better error
        var targetType = requireNonNull(format.getFieldType(arg.name.name));

        check(arg.value);

        statement.namedArguments.set(i,
            new InstructionCallStatement.NamedArgument(arg.name,
                wrapImplicitCast(arg.value, targetType)));
        arg = statement.namedArguments.get(i);
        var actualType = arg.value.type();

        if (!targetType.equals(actualType)) {
          throw typeMissmatchError(arg, targetType, actualType);
        }
      }


    } else if (statement.instrDef instanceof PseudoInstructionDefinition pseudoDef) {
      check(pseudoDef);

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
        var targetType = pseudoDef.params.get(i).typeLiteral.type();
        var arg = statement.unnamedArguments.get(i);
        check(arg);
        statement.unnamedArguments.set(i,
            wrapImplicitCast(arg, targetType));
        arg = statement.unnamedArguments.get(i);
        var actualType = statement.unnamedArguments.get(i).type();

        if (!targetType.equals(actualType)) {
          throw typeMissmatchError(arg, targetType, actualType);
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
