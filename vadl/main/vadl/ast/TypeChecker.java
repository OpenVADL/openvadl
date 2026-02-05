// SPDX-FileCopyrightText : © 2025-2026 TU Wien <vadl@tuwien.ac.at>
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
import static vadl.error.Diagnostic.error;
import static vadl.error.Diagnostic.warning;

import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import vadl.error.DeferredDiagnosticStore;
import vadl.error.Diagnostic;
import vadl.error.DiagnosticList;
import vadl.types.BitsType;
import vadl.types.BoolType;
import vadl.types.BuiltInTable;
import vadl.types.ConcreteRelationType;
import vadl.types.DataType;
import vadl.types.FetchResultType;
import vadl.types.InstructionType;
import vadl.types.MicroArchitectureType;
import vadl.types.SIntType;
import vadl.types.StatusType;
import vadl.types.StringType;
import vadl.types.TupleType;
import vadl.types.Type;
import vadl.types.UIntType;
import vadl.types.asmTypes.AsmType;
import vadl.types.asmTypes.GroupAsmType;
import vadl.types.asmTypes.InstructionAsmType;
import vadl.types.asmTypes.StringAsmType;
import vadl.utils.Levenshtein;
import vadl.utils.Pair;
import vadl.utils.SourceLocation;
import vadl.utils.WithLocation;
import vadl.viam.Constant;

/**
 * A experimental, temporary type-checker to verify expressions and attach types to the AST.
 *
 * <p>As the typesystem can depend on constants, the typechecker needs to evaluate (at least some
 * of) them.
 */
@SuppressWarnings("checkstyle:OverloadMethodsDeclarationOrder")
public class TypeChecker
    implements DefinitionVisitor<Void>, StatementVisitor<Void>, ExprVisitor<Void> {

  private BranchStrategy branchStrategy = BranchStrategy.ALL;

  /**
   * Describes whether all branches are checked and the result of all branches must be equal, or
   * if we must evaluate the condition and propagate the type from the chosen branch.
   */
  enum BranchStrategy {
    ALL,
    ONE,
  }

  /**
   * A custom exception to be thrown to partially stop evaluating to recover to a point where it
   * makes sense to resume typechecking.
   */
  static class StopPartialCheckingSignal extends RuntimeException {
  }

  /**
   * Recoverable errors are stored in this list.
   */
  private final List<Diagnostic> errors = new ArrayList<>();

  /**
   * Often the typechecker needs to evaluate constants in type literals.
   */
  final ConstantEvaluator constantEvaluator;

  /**
   * We are keeping a list of all the nodes (well, the identities of them) we are currently
   * visiting. This helps us detect cycles, which aren't allowed and so we can abort early with an
   * error instead of causing a crash due to a stack overflow.
   */
  private final Deque<Integer> currentlyVisiting = new ArrayDeque<Integer>();

  /**
   * There is no point in checking a statement or definition twice, so these sets record which
   * nodes we already visited. For expressions, we can simply check if the type is set.
   */
  private final Set<Statement> checkedStatements =
      Collections.newSetFromMap(new IdentityHashMap<>());
  private final Set<Definition> checkedDefinitions =
      Collections.newSetFromMap(new IdentityHashMap<>());

  /**
   * If checking a statments that cannot be correctly checked will be added to these lists.
   * Checking them again will throw a {@link StopPartialCheckingSignal}.
   */
  private final Set<Statement> erroredStatements =
      Collections.newSetFromMap(new IdentityHashMap<>());
  private final Set<Definition> erroredDefinitions =
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
  Type check(Expr expr) {
    // Expressions store their type so we can look at them to see if they were already evaluated.
    if (expr.type != null) {
      if (expr.type instanceof InternalErrorType) {
        throw new StopPartialCheckingSignal();
      }
      return requireNonNull(expr.type);
    }

    var nodeId = System.identityHashCode(expr);
    if (currentlyVisiting.contains(nodeId)) {
      throw error("Infinite Recursion", expr)
          .description("The node is defined by itself.")
          .build();
    }

    currentlyVisiting.add(nodeId);
    try {
      expr.accept(this);
    } catch (StopPartialCheckingSignal signal) {
      if (expr.type == null) {
        expr.type = new InternalErrorType();
      }
      throw signal;
    } catch (EvaluationError error) {
      errors.add(
          error("Constant value required", error.location)
              .locationDescription(error.location, "%s", requireNonNull(error.getMessage()))
              .build()
      );
      if (expr.type == null) {
        expr.type = new InternalErrorType();
      }
      throw new StopPartialCheckingSignal();
    } finally {
      currentlyVisiting.pop();
    }
    return expr.type();
  }

  /**
   * Typecheck the statement, if not yet checked.
   *
   * @param stmt to check.
   */
  void check(Statement stmt) {
    if (erroredStatements.contains(stmt)) {
      throw new StopPartialCheckingSignal();
    }

    if (checkedStatements.contains(stmt)) {
      return;
    }

    var nodeId = System.identityHashCode(stmt);
    if (currentlyVisiting.contains(nodeId)) {
      throw error("Infinite Recursion", stmt)
          .description("The node is defined by itself.")
          .build();
    }

    currentlyVisiting.add(nodeId);
    try {
      stmt.accept(this);
    } catch (StopPartialCheckingSignal signal) {
      // Add the statement to remember that it wasn't sucessfully checked and continue with the next
      // one on purpose.
      erroredStatements.add(stmt);
    }
    currentlyVisiting.pop();
    checkedStatements.add(stmt);
  }

  private void verifyAnnotations(Definition def) {
    // NOTE: This could have been done in the symbol resolver
    // Disallow the same annotation multiple times
    Map<String, AnnotationDefinition> annotationNames = new HashMap<>();
    def.annotations.forEach(annotation -> {
      if (annotationNames.containsKey(annotation.name())) {
        errors.add(error("Duplicate Annotation", def)
            .locationNote(annotationNames.get(annotation.name()), "First used here")
            .build()
        );
      }

      // check annotation definition itself
      check(annotation);

      annotationNames.put(annotation.name(), annotation);
    });

    // Find annotations in groups and execute the check of the groups.
    AnnotationTable.groupings(def).forEach((group, annotations) -> {
      group.check(def, annotations, this);
      group.applyAst(def, annotations);
    });
  }

  /**
   * Typecheck the definition, if not yet checked.
   *
   * @param def to check.
   */
  private void check(Definition def) {
    if (erroredDefinitions.contains(def)) {
      throw new StopPartialCheckingSignal();
    }

    if (checkedDefinitions.contains(def)) {
      return;
    }

    var nodeId = System.identityHashCode(def);
    if (currentlyVisiting.contains(nodeId)) {
      String message = "The node is defined by itself.";
      if (def instanceof IdentifiableNode identifiableNode) {
        message =
            "Definition `%s` is defined by itself.".formatted(identifiableNode.identifier().name);
      }

      throw error("Infinite Recursion", def)
          .description("%s", message)
          .build();
    }

    // Visit the definitions
    currentlyVisiting.add(nodeId);
    try {
      def.accept(this);
    } catch (StopPartialCheckingSignal signal) {
      // Add the node to the list of errored nodes and continue with the next one.
      erroredDefinitions.add(def);
    }
    currentlyVisiting.pop();
    checkedDefinitions.add(def);

    verifyAnnotations(def);
  }

  /**
   * Verify that the program is well-typed.
   *
   * <p>This also modiefies the AST and adds types to all expressions and possibly attaches other
   * data to AST nodes that might be usefull for lowering.
   *
   * @param ast to verify
   * @throws DiagnosticList if the program isn't well typed.
   */
  public void verify(Ast ast) {
    var startTime = System.nanoTime();
    try {
      ast.definitions.forEach(this::check);
    } catch (Diagnostic error) {
      errors.add(error);
    } catch (StopPartialCheckingSignal signal) {
      // do nothing on purpose.
    }
    ast.passTimings.add(
        new Ast.PassTimings("Type Checking", (System.nanoTime() - startTime) / 1_000_000));

    if (!errors.isEmpty()) {
      throw new DiagnosticList(errors);
    }
  }

  private void throwUnimplemented(Node node) {
    throw new RuntimeException(
        "The typechecker doesn't know how to handle `%s` yet, found in %s".formatted(
            node.getClass().getSimpleName(), node.location().toIDEString()));
  }

  /**
   * Add a recoverable error to the internal list and throw a StopPartialCheckingSignal.
   *
   * @param error to be recorded.
   * @return StopPartialCheckingSignal is never actually returned but thrown, however sometimes it
   *     is used to trick the compiler.
   * @throws StopPartialCheckingSignal always.
   */
  private StopPartialCheckingSignal addErrorAndStopChecking(Diagnostic error) {
    errors.add(error);
    throw new StopPartialCheckingSignal();
  }

  private static Diagnostic typeMismatchError(WithLocation locatable, Type expected, Type actual) {
    return typeMismatchError(locatable, "`%s`".formatted(expected), actual);
  }

  private static Diagnostic typeMismatchError(WithLocation locatable, String expectation,
                                              Type actual) {
    return error("Type Mismatch", locatable)
        .locationDescription(locatable, "Expected %s but got `%s`.",
            expectation, actual)
        .build();
  }

  private IllegalStateException buildIllegalStateException(Node node, String message) {
    return new IllegalStateException(
        "The typechecker encountered an invalid state in `%s` at %s: %s".formatted(
            node.getClass().getSimpleName(), node.location().toIDEString(), message));
  }

  private void throwInvalidAsmCast(AsmType from, AsmType to, WithLocation location) {
    addErrorAndStopChecking(error("Type Mismatch", location)
        .description("Invalid cast from `%s` to `%s`.", from, to)
        .build());
  }

  /**
   * Execute the operation with the specified strategy set, and reset it afterwards again.
   *
   * @param strategy  to be active during the execution of the operation
   * @param operation to be executed
   * @return the result of the operation
   */
  private <T> T withBranchingStrategy(BranchStrategy strategy, Supplier<T> operation) {
    var oldStrategy = branchStrategy;
    try {
      branchStrategy = strategy;
      return operation.get();
    } finally {
      branchStrategy = oldStrategy;
    }
  }

  /**
   * Tests whether a type can explicit be cast to another.
   *
   * @param from is the source type.
   * @param to   is the target type.
   * @return true if the cast can happen explicitly, false otherwise.
   */
  private static boolean canExplicitCast(Type from, Type to) {
    if (from.equals(to)) {
      return true;
    }

    // Tensors need special rules for casting
    if (from instanceof TensorType fromTensor && to instanceof TensorType toTensor) {
      return fromTensor.flattenBitsType().equals(toTensor.flattenBitsType());
    }
    if (from instanceof TensorType fromTensor && to instanceof BitsType toBits) {
      return fromTensor.flattenBitsType().equals(toBits);
    }
    if (from instanceof BitsType fromBits && to instanceof TensorType toTensor) {
      return toTensor.flattenBitsType().bitWidth() == fromBits.bitWidth();
    }

    // Casting rules for basic types
    var castTable = Map.of(
        ConstantType.class, List.of(BitsType.class, BoolType.class),
        BitsType.class, List.of(BitsType.class, BoolType.class),
        BoolType.class, List.of(BoolType.class, BitsType.class),
        StringType.class, List.of(StringType.class)
    );

    var key =
        castTable.keySet().stream().filter(k -> k.isInstance(from)).findFirst().orElse(null);
    if (key == null) {
      return false;
    }
    var allowedTargets = requireNonNull(castTable.get(key));
    return allowedTargets.stream().anyMatch(t -> t.isInstance(to));
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
   * Wraps the expr provided with an explicit cast if it is possible, and not useless.
   *
   * @param inner expression to wrap.
   * @param to    which the expression should be casted.
   * @return the original expression, possibly wrapped.
   */
  private static Expr wrapExplicitCast(Expr inner, Type to) {
    var innerType = requireNonNull(inner.type);
    if (innerType.equals(to)) {
      return inner;
    }

    if (!canExplicitCast(innerType, to)) {
      if (!(innerType instanceof ConstantType innerConstTyp) || to instanceof ConstantType) {
        return inner;
      }

      // For constant types we cast to them anyway to the clostest type to improve the error message
      return new CastExpr(inner, innerConstTyp.closestTo(to));
    }

    return new CastExpr(inner, to);
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
      if (!(innerType instanceof ConstantType innerConstTyp) || to instanceof ConstantType) {
        return inner;
      }

      // For constant types we cast to them anyway to the clostest type to improve the error message
      return new CastExpr(inner, innerConstTyp.closestTo(to));
    }

    return new CastExpr(inner, to);
  }

  /**
   * Wraps the expr provided with an explicit cast if it is possible, and not useless.
   * However, in comparison to {@link #wrapExplicitCast(Expr, Type)}, this will throw a
   * type mismatch exception,
   * if the inner expression could not implicitly cast to the given type.
   *
   * @param inner expression to wrap.
   * @param to    which the expression should be cast.
   * @return the original expression.
   * @throws StopPartialCheckingSignal if the inner expression cannot be implicitly cast to type.
   */
  private Expr tryWrapExplicitCast(Expr inner, Type to) {
    if (inner.type == null) {
      throw new IllegalStateException("The type of the inner expression must be known.");
    }
    var wrapped = wrapExplicitCast(inner, to);
    if (!wrapped.type().equals(to)) {
      addErrorAndStopChecking(typeMismatchError(inner, to, inner.type()));
    }
    return wrapped;
  }

  /**
   * Wraps the expr provided with an implicit cast if it is possible, and not useless.
   * However, in comparison to {@link #wrapImplicitCast(Expr, Type)}, this will throw a
   * type mismatch exception,
   * if the inner expression could not implicitly cast to the given type.
   *
   * @param inner expression to wrap.
   * @param to    which the expression should be cast.
   * @return the original expression.
   * @throws StopPartialCheckingSignal if the inner expression cannot be implicitly cast to type.
   */
  private Expr tryWrapImplicitCast(Expr inner, Type to) {
    if (inner.type == null) {
      throw new IllegalStateException("The type of the inner expression must be known.");
    }
    var wrapped = wrapImplicitCast(inner, to);
    if (!wrapped.type().equals(to)) {
      addErrorAndStopChecking(typeMismatchError(inner, to, inner.type()));
    }
    return wrapped;
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

  record BuiltInCheckResult(Type type, @Nullable List<Expr> castedArgs) {

  }

  private BuiltInCheckResult checkBuiltin(BuiltInTable.BuiltIn builtIn, List<Expr> args,
                                          WithLocation location) {
    if (args.size() == 1) {
      var innerType = args.getFirst().type();

      if (builtIn == BuiltInTable.NEG && !(innerType instanceof BitsType)
          && !(innerType instanceof ConstantType)) {
        addErrorAndStopChecking(error("Type Mismatch", location)
            .description("Expected a numerical type but got `%s`", innerType)
            .build());
      }

      if (args.get(0).type() instanceof ConstantType) {
        var type = constantEvaluator.evalBuiltin(builtIn,
            args.stream().map(a -> constantEvaluator.eval(a)).toList(), location).type();
        return new BuiltInCheckResult(type, null);
      }

      if (List.of(BuiltInTable.NEG, BuiltInTable.NOT).contains(builtIn)) {
        return new BuiltInCheckResult(args.getFirst().type(), null);
      }
    }

    if (args.size() == 2 && (BuiltInTable.arithmeticOperators.contains(builtIn)
        || BuiltInTable.arithmeticComparisons.contains(builtIn))) {
      var left = args.getFirst();
      var right = args.getLast();

      // Verify the rough shapes of the input parameters
      // This however doesn't check if the types relate to each other.
      if (left.type().equals(Type.bool())) {
        left = wrapImplicitCast(left, Type.bits(1));
      }
      if (right.type().equals(Type.bool())) {
        right = wrapImplicitCast(right, Type.bits(1));
      }

      // Special concat on strings
      if (builtIn == BuiltInTable.CONCATENATE_STRINGS) {
        return new BuiltInCheckResult(Type.string(), List.of(left, right));
      }

      if (!(left.type() instanceof BitsType) && !(left.type() instanceof ConstantType)) {
        addErrorAndStopChecking(error("Type Mismatch", location)
            .locationDescription(location, "Expected a number here but the left side was an `%s`",
                left.type())
            .build());
      }
      if (!(right.type() instanceof BitsType) && !(right.type() instanceof ConstantType)) {
        addErrorAndStopChecking(error("Type Mismatch", location)
            .locationDescription(location,
                "Expected a number here but the right side was an `%s`",
                right.type())
            .build());
      }

      // Shifts and rotates require that the right type is uint and the left can be anything.
      var requireRightUInt =
          List.of("<<", ">>", "<<>", "<>>");
      if (requireRightUInt.contains(builtIn.operator())) {

        Type closestUIntType;
        if (right.type() instanceof BitsType bitsRightType) {
          closestUIntType = Type.unsignedInt(bitsRightType.bitWidth());
        } else if (right.type() instanceof ConstantType constantRightType) {
          closestUIntType = constantRightType.closestUInt();
        } else {
          throw new IllegalStateException("Don't handle buitlin " + builtIn.name());
        }

        if (!(right.type() instanceof UIntType || right.type() instanceof BitsType)
            && !canImplicitCast(right.type(), closestUIntType)) {
          addErrorAndStopChecking(error("Type Mismatch", location)
              .locationNote(location, "The right type must be unsigned but is %s", right.type())
              .build());
        }

        if (!(right.type() instanceof UIntType)) {
          right = new CastExpr(right, closestUIntType);
        }

        // Only the left side decides the output type
        if (left.type() instanceof ConstantType) {

          if (List.of("<>>", "<<>").contains(builtIn.operator())) {
            addErrorAndStopChecking(error("Type Mismatch", location)
                .locationNote(location, "The left side must be a concrete type but was %s",
                    right.type())
                .description("Rotate operations require a type with a fixed bit width.")
                .build());
          }

          if (constantEvaluator.isConstant(right)) {
            var result = constantEvaluator.evalBuiltin(builtIn,
                List.of(left, right).stream().map(a -> constantEvaluator.eval(a)).toList(),
                location);
            return new BuiltInCheckResult(result.type(), List.of(left, right));
          }

          // The left side is constant but the right isn't
          // lets throw an error because it doesn't make sense to cast the left side to the right
          // side if the types are independent of each other.
          throw addErrorAndStopChecking(error("Type Mismatch", location)
              .description(
                  "%s",
                  "Cannot infer a type for the result of this operation, because the left side is "
                      + "constant but the right side is not: `%s`.".formatted(right.type())
              )
              .help("You can cast the left argument to an explicit type.")
              .build());
        }

        return new BuiltInCheckResult(left.type(), List.of(left, right));
      }

      // Const types are a special case
      if (left.type() instanceof ConstantType && right.type() instanceof ConstantType) {
        var result = constantEvaluator.evalBuiltin(builtIn,
            List.of(left, right).stream().map(a -> constantEvaluator.eval(a)).toList(), location);
        return new BuiltInCheckResult(result.type(), List.of(left, right));
      }

      // If only one type is const, cast it to it's partner (or as close as possible)
      if (left.type() instanceof ConstantType leftConstType) {
        left = new CastExpr(left, leftConstType.closestTo(right.type()));
      } else if (right.type() instanceof ConstantType rightConstType) {
        right = new CastExpr(right, rightConstType.closestTo(left.type()));
      }

      // Long Multiply has different rules than all other arithmetic operations
      if (builtIn.operator() != null && builtIn.operator().equals("*#")) {
        // At this point both must be Bits or a subtype
        var leftBitWidth = ((BitsType) left.type()).bitWidth();
        var rightBitWidth = ((BitsType) right.type()).bitWidth();
        if (leftBitWidth != rightBitWidth) {
          addErrorAndStopChecking(error("Type Mismatch", location)
              .description(
                  "Both sides must have the same width but left is `%s` while right is `%s`",
                  left.type(), right.type())
              .build());
        }

        // Rules determining the return type (switched input operators omitted because of
        // commutative property)
        // SInt<N> +# SInt<N> -> SInt<2*N>
        // SInt<N> +# UInt<N> -> SInt<2*N>
        // SInt<N> +# Bits<N> -> SInt<2*N>
        // UInt<N> +# UInt<N> -> UInt<2*N>
        // UInt<N> +# Bits<N> -> UInt<2*N>
        // Bits<N> +# Bits<N> -> Bits<2*N>
        if (left.type() instanceof SIntType || right.type() instanceof SIntType) {
          var type = Type.signedInt(leftBitWidth * 2);
          return new BuiltInCheckResult(type, List.of(left, right));
        } else if (left.type() instanceof UIntType || right.type() instanceof UIntType) {
          var type = Type.unsignedInt(leftBitWidth * 2);
          return new BuiltInCheckResult(type, List.of(left, right));
        }
        var type = Type.bits(leftBitWidth * 2);
        return new BuiltInCheckResult(type, List.of(left, right));
      }

      var bitWidth = ((BitsType) left.type()).bitWidth();
      var sizedUInt = Type.unsignedInt(bitWidth);
      var sizedSInt = Type.signedInt(bitWidth);
      var sizedBits = Type.bits(bitWidth);
      var specialBinaryPattern = Map.of(
          Pair.of(sizedUInt, sizedBits), Pair.of(sizedUInt, sizedUInt),
          Pair.of(sizedBits, sizedUInt), Pair.of(sizedUInt, sizedUInt),
          Pair.of(sizedSInt, sizedBits), Pair.of(sizedSInt, sizedSInt),
          Pair.of(sizedBits, sizedSInt), Pair.of(sizedSInt, sizedSInt)
      );

      if (((BitsType) left.type()).bitWidth() == ((BitsType) right.type()).bitWidth()
          && specialBinaryPattern.containsKey(Pair.of(left.type(), right.type()))) {
        var target = requireNonNull(specialBinaryPattern.get(Pair.of(left.type(), right.type())));
        if (!left.type().equals(target.left())) {
          left = new CastExpr(left, target.left());
        } else {
          right = new CastExpr(right, target.right());
        }
      }

      // Apply general implicit casting rules after specialised once.
      left = wrapImplicitCast(left, right.type());
      right = wrapImplicitCast(right, left.type());

      if (!left.type().equals(right.type())) {
        addErrorAndStopChecking(error("Type Mismatch", location)
            .locationNote(location, "The left type is %s while right is %s", left.type(),
                right.type())
            .description(
                "Both types on the left and right side of an binary operation should be equal.")
            .build());
      }

      if (BuiltInTable.arithmeticComparisons.contains(builtIn)) {
        // Output type depends on type of operation
        var type = Type.bool();
        return new BuiltInCheckResult(type, List.of(left, right));
      }
      if (BuiltInTable.arithmeticOperators.contains(builtIn)) {
        // Note: No that isn't the same as leftTyp
        var type = left.type();
        return new BuiltInCheckResult(type, List.of(left, right));
      }

      //throw new RuntimeException("Don't yet know how to handle " + builtIn);
      // Fallback: This concludes all the special handling we do on functions with two arguments.
      // Now revert to the generic handling of functions.
    }

    var argTypes = args.stream().map(Expr::type).toList();
    var areAllArgs = argTypes.stream().allMatch(ConstantType.class::isInstance);
    if (areAllArgs) {
      var type = constantEvaluator
          .evalBuiltin(builtIn, args.stream().map(constantEvaluator::eval).toList(), location)
          .type();
      return new BuiltInCheckResult(type, null);
    }

    if (!builtIn.takes(argTypes)) {
      // FIXME: Better format that error
      addErrorAndStopChecking(error("Type Mismatch", location)
          .description("Expected %s but got `%s`", builtIn.signature().argTypeClasses(), argTypes)
          .build());
    }

    // Note: cannot set the computed type because builtins aren't a definition.
    return new BuiltInCheckResult(builtIn.returns(argTypes), null);
  }

  @Override
  public Void visit(ConstantDefinition definition) {
    Type valType = withBranchingStrategy(BranchStrategy.ONE, () -> check(definition.value));

    if (definition.typeLiteral == null) {
      // Do nothing on purpose
    } else {
      definition.typeLiteral.type =
          parseTypeLiteral(definition.typeLiteral, preferredBitWidthOf(valType));
      Type litType = requireNonNull(definition.typeLiteral.type);

      if (!canImplicitCast(valType, litType)) {
        addErrorAndStopChecking(typeMismatchError(definition.value, litType, valType));
      }

      // Insert a cast if needed
      if (!litType.equals(valType)) {
        definition.value = new CastExpr(definition.value, definition.typeLiteral);
        check(definition.value);
      }
    }

    // Evaluate the constant (like all constants must be able to be evaluated)
    try {
      constantEvaluator.eval(definition.value);
    } catch (EvaluationError e) {
      addErrorAndStopChecking(error("Invalid constant value", definition.value)
          .locationDescription(e.location, "%s", requireNonNull(e.getMessage()))
          .description("All constants must be able to be evaluated")
          .build());
    }

    return null;
  }

  private void setFormatDefinitionEmptyFieldsToError(FormatDefinition definition) {
    definition.fields.forEach(field -> {
      switch (field) {
        case TypedFormatField typedField -> {
          if (typedField.typeLiteral.type == null) {
            typedField.typeLiteral.type = new InternalErrorType();
          }
        }
        case RangeFormatField rangeField -> {
          if (rangeField.type == null) {
            rangeField.type = new InternalErrorType();
          }
        }

        case DerivedFormatField derivedField -> {
          if (derivedField.expr.type == null) {
            derivedField.expr.type = new InternalErrorType();
          }
        }

        default -> throw new IllegalArgumentException(
            "Unknown format field type: " + field.getClass().getSimpleName());
      }

    });
  }

  @Override
  public Void visit(FormatDefinition definition) {
    var type = check(definition.typeLiteral);
    if (!(type instanceof BitsType bitsType)) {
      definition.typeLiteral.type = new InternalErrorType();
      setFormatDefinitionEmptyFieldsToError(definition);
      // Not actually thrown here but used to signal that this if will never suceed.
      throw addErrorAndStopChecking(typeMismatchError(definition.typeLiteral, "bits type", type));
    }

    var bitWidth = bitsType.bitWidth();
    var bitsVerifier = new FormatBitsVerifier(bitWidth);
    var nextOccupiedBit = bitWidth - 1;

    for (var field : definition.fields.stream()
        .filter(field -> !(field instanceof DerivedFormatField)).toList()) {
      if (field instanceof TypedFormatField typedField) {
        var fieldType = check(typedField.typeLiteral);

        if (!(fieldType instanceof BitsType fieldBitsType)) {
          setFormatDefinitionEmptyFieldsToError(definition);
          throw addErrorAndStopChecking(error("Bits Type expected", typedField.typeLiteral)
              .description("Format fields can only be assigned a bits type.")
              .build());
        }
        typedField.range = new FormatDefinition.BitRange(nextOccupiedBit,
            nextOccupiedBit - (fieldBitsType.bitWidth() - 1));
        nextOccupiedBit -= fieldBitsType.bitWidth();
        bitsVerifier.addType(fieldBitsType);

      } else if (field instanceof RangeFormatField rangeField) {
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
            setFormatDefinitionEmptyFieldsToError(definition);
            addErrorAndStopChecking(error("Invalid Range", range)
                .locationDescription(range, "Range must span more than one bit but was %s",
                    fieldBitWidth)
                .locationNote(range,
                    "Ranges are specified as `from..to` where from is always larger than to.")
                .build());
          }

          // Check range is not out of bounds.
          if (from < 0 || from >= bitWidth || to < 0 || to > bitWidth) {
            setFormatDefinitionEmptyFieldsToError(definition);
            addErrorAndStopChecking(error("Invalid Range", range)
                .locationDescription(range,
                    "Provided range `%d..%d` out of bounds for available range `%d..0`",
                    from, to, bitWidth - 1)
                .build());
          }

          fieldBitWidth += rangeSize;
          rangeField.computedRanges.add(new FormatDefinition.BitRange(from, to));
          bitsVerifier.addRange(from, to);
        }

        if (fieldBitWidth < 1) {
          setFormatDefinitionEmptyFieldsToError(definition);
          addErrorAndStopChecking(error("Invalid Field", rangeField)
              .description("Field must be at least one bit but was %s", fieldBitWidth)
              .build());
        }

        if (rangeField.type == null) {
          // Set the type
          rangeField.type = Type.bits(fieldBitWidth);
        } else {
          // Verify the received type with the one provided in the literal.
          var rangeBitsType = Type.bits(fieldBitWidth);
          if (!canImplicitCast(rangeField.type, rangeBitsType)) {
            setFormatDefinitionEmptyFieldsToError(definition);
            addErrorAndStopChecking(error("Type Mismatch", rangeField)
                .description("Type declared as `%s`, but the range is `%s`", rangeField.type,
                    rangeBitsType)
                .build());
          }
        }
      } else {
        throw new RuntimeException("Unknown FormatField Class ".concat(field.getClass().getName()));
      }
    }

    definition.fields.stream()
        .filter(field -> field instanceof DerivedFormatField)
        .map(field -> (DerivedFormatField) field)
        .forEach(dfField -> check(dfField.expr));

    // check that there are not multiple predicates for the same access field
    var derivedFieldsWithPredicate =
        new HashMap<DerivedFormatField, FormatDefinition.AuxiliaryField>();
    for (var aux : definition.auxiliaryFields) {
      check(aux);
      if (aux.kind == FormatDefinition.AuxiliaryField.AuxKind.PREDICATE) {
        var conflict = derivedFieldsWithPredicate.get(aux.fieldDef());
        if (conflict != null) {
          addErrorAndStopChecking(error("Conflicting field access predicates", aux.field)
              .locationDescription(aux.field,
                  "A predicate for the field access `%s` already exists.", aux.field.name)
              .locationHelp(conflict, "Predicate already defined here.")
              .build());
        }
        derivedFieldsWithPredicate.put((DerivedFormatField) aux.fieldDef(), aux);
      }
    }

    if (bitsVerifier.hasViolations()) {
      addErrorAndStopChecking(error("Invalid Format", definition)
          .description("%s", requireNonNull(bitsVerifier.getViolationsMessage()))
          .build());
    }

    return null;
  }

  @Override
  public Void visit(DerivedFormatField definition) {
    // Do nothing on purpose for now, this definition is checked when visiting FormatDefinition.
    return null;
  }

  @Override
  public Void visit(RangeFormatField definition) {
    // Do nothing on purpose for now, this definition is checked when visiting FormatDefinition.
    return null;
  }

  @Override
  public Void visit(TypedFormatField definition) {
    // Do nothing on purpose for now, this definition is checked when visiting FormatDefinition.
    return null;
  }

  @Override
  public Void visit(FormatDefinition.AuxiliaryField definition) {
    check(definition.expr);
    switch (definition.kind) {
      case PREDICATE -> checkFieldAccessPredicate(definition);
      case ENCODING -> checkFieldEncoding(definition);
    }
    return null;
  }

  private void checkFieldEncoding(FormatDefinition.AuxiliaryField auxiliaryField) {
    var field = auxiliaryField.field.target();
    var fieldType = switch (field) {
      case RangeFormatField r -> requireNonNull(r.type);
      case TypedFormatField f -> f.typeLiteral.type();
      default ->
          throw addErrorAndStopChecking(error("Invalid format field encoding", auxiliaryField.field)
              .locationDescription(auxiliaryField.field,
                  "The encoding must reference a format field.")
              .build());
    };
    auxiliaryField.expr = tryWrapImplicitCast(auxiliaryField.expr, fieldType);
  }

  private void checkFieldAccessPredicate(FormatDefinition.AuxiliaryField auxiliaryField) {
    var field = auxiliaryField.field.target();
    if (!(field instanceof DerivedFormatField)) {
      throw error("Invalid format field predicate", auxiliaryField.field)
          .locationDescription(auxiliaryField.field,
              "The predicate must reference a field access function.")
          .build();
    }
    if (auxiliaryField.expr.type() != Type.bool()) {
      addErrorAndStopChecking(error("Invalid field access predicate", auxiliaryField.field)
          .locationDescription(auxiliaryField.field,
              "The predicate must be a `Bool` expression, but was `%s`.",
              auxiliaryField.expr.type()).build());
    }
  }

  @Override
  public Void visit(InstructionSetDefinition definition) {
    for (var extension : definition.extendingNodes()) {
      check(extension);
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
    definition.typeLiteral.argTypes().forEach(this::check);
    check(definition.typeLiteral.resultType());

    var type = definition.typeLiteral.resultType().type();
    if (!(type instanceof DataType)) {
      throw error("Invalid Type", definition)
          .description("Expected register type to be one of Bits, SInt, UInt or Bool.")
          .note("Type was %s.", type)
          .build();
    }

    // In case the user wrote the relational type syntax, let's remap it to tensor type which makes
    // more sense for lowering and VIAM.
    // Example:
    //    Bits<n> -> Bit<m_1>...<m_k>     ==>     Bits<2^n><m_1>...<m_k>
    //    ^^^^^^ Relation Type ^^^^^^   becomes   ^^^^^ Tensor Type ^^^^

    // FIXME: Don't know what to do with multiple args, figure out later.
    if (definition.typeLiteral.argTypes().size() > 1) {
      throw new IllegalStateException("Multiple arguments for type in register not yet defined");
    }

    definition.typeLiteral.argTypes().forEach((argType) -> {
      var argTypeType = argType.type();
      if (!(argTypeType instanceof DataType)) {
        throw error("Invalid Type", definition)
            .description("Expected register type to be one of Bits, SInt, UInt or Bool.")
            .note("Type was %s.", argTypeType)
            .build();
      }
    });
    if (!definition.typeLiteral.argTypes().isEmpty()) {
      var argType = (DataType) definition.typeLiteral.argTypes().getFirst().type();
      var newIndex = 1 << argType.bitWidth(); // A fancy 2 ^ bitwidth
      if (type instanceof TensorType tensorType) {
        type = new TensorType(List.of(newIndex), tensorType);
      } else {
        if (!(type instanceof BitsType bitsType)) {
          throw error("Type Mismatch", definition.typeLiteral.resultType())
              .description(
                  "Expected result type to be either a tensor or a bits type, but received `%s`",
                  type)
              .note(
                  "For type literals in the relation syntax (with the arrow syntax) the result "
                      + "type must be a bits type.")
              .build();
        }

        type = new TensorType(List.of(newIndex), bitsType);
      }
    }

    definition.type = type;
    return null;
  }

  @Override
  public Void visit(InstructionDefinition definition) {
    if (requireNonNull(definition.formatNode).typeLiteral.type == null) {
      check(definition.formatNode);
    }

    check(definition.behavior);

    if (definition.assemblyDefinition == null) {
      throw error("Missing Assembly", definition.identifier())
          .description("Every instruction needs an matching assembly definition.")
          .build();
    }
    if (definition.encodingDefinition == null) {
      throw error("Missing Encoding", definition.identifier())
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
      throw error("Missing Assembly", definition.identifier())
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
      throw error("Type Mismatch", definition.expr)
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
        throw typeMismatchError(encodingField.value, fieldType, valueType);
      }
    }
    return null;
  }

  @Override
  public Void visit(AssemblyDefinition definition) {
    if (definition.identifiers.size() != 1) {
      // All assembly definitions with multiple identifiers should have already been expanded to
      // multiple definitions by the parser, with each only containing a single identifier.
      throw new IllegalStateException(
          "No Assemblydefinition should have multiple identifiers in the typechecker.");
    }

    var exprType = check(definition.expr);

    if (exprType.getClass() != StringType.class) {
      throw typeMismatchError(definition.expr, "`String`", exprType);
    }
    return null;
  }

  @Override
  public Void visit(UsingDefinition definition) {
    check(definition.typeLiteral);
    return null;
  }

  @Override
  public Void visit(AbiClangTypeDefinition abiClangTypeDefinition) {
    // Check nothing on purpose
    return null;
  }

  @Override
  public Void visit(AbiClangNumericTypeDefinition abiClangNumericTypeDefinition) {
    check(abiClangNumericTypeDefinition.size);
    var ty = abiClangNumericTypeDefinition.size.type();
    if (!(ty instanceof ConstantType)) {
      throw error("Type Mismatch", abiClangNumericTypeDefinition.size)
          .description("Expected a number as data type")
          .build();
    }

    return null;
  }

  @Override
  public Void visit(StageOutputDefinition stageOutputDefinition) {
    check(stageOutputDefinition.typeLiteral);
    return null;
  }

  @Override
  public Void visit(AbiSpecialPurposeInstructionDefinition definition) {
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
      throw error("Type Mismatch", definition.expr)
          .locationDescription(definition.retType, "Return type defined here as `%s`", retType)
          .locationDescription(definition.expr, "Expected `%s` but got `%s`", retType, exprType)
          .build();
    }


    var argTypes = definition.params.stream().map(p -> p.typeLiteral.type).toList();
    definition.type = Type.concreteRelation(argTypes, retType);
    return null;
  }

  /**
   * Checks whether or not the alias can be cast to the types provided.
   *
   * <p>There are two rules enforced on type casting:
   * 1) The bitwidth of both sides must be equal.
   * 2) Only the innermost index can be expaned or the most n innermost indices can be compressed.
   *
   * <pre>
   *  register X<128><8><16>
   *  alias    A<128><8><2><8> // valid: expanded
   *  alias    A<128><128>     // valid: compressed
   *  alias    A<16384>        // valid: compressed
   *  alias    A<128><16><8>   // invalid: neither compressed nor expanded but switched.
   * </pre>
   */
  private boolean canAliasCastType(DataType from, DataType to) {
    if (from.bitWidth() != to.bitWidth()) {
      return false;
    }

    var fromIndecies = new ArrayList<>(
        switch (from) {
          case TensorType t ->
              Stream.concat(t.indexDims().stream(), List.of(t.innerType().bitWidth()).stream())
                  .toList();
          default -> List.of(from.bitWidth());
        });

    var toIndecies = new ArrayList(
        switch (to) {
          case TensorType t ->
              Stream.concat(t.indexDims().stream(), List.of(t.innerType().bitWidth()).stream())
                  .toList();
          default -> List.of(to.bitWidth());
        });

    // Eliminate the equal indecies
    while (fromIndecies.size() > 0 && toIndecies.size() > 0 && fromIndecies.getFirst()
        .equals(toIndecies.getFirst())) {
      fromIndecies.removeFirst();
      toIndecies.removeFirst();
    }

    return fromIndecies.size() == 1 || toIndecies.size() == 1;
  }

  @Override
  public Void visit(AliasDefinition definition) {

    var targetIdent = switch (definition.value) {
      case Identifier ident -> ident;
      case CallIndexExpr expr -> expr.target.path();
      default -> throw error("Invalid alias", definition.value)
          .locationDescription(definition.value, "The target must be a direct register access.")
          .build();
    };

    definition.computedFixedArgs = List.of();
    if (definition.kind == AliasDefinition.AliasKind.REGISTER) {
      var reg = definition.symbolTable().findAs(targetIdent, RegisterDefinition.class);
      if (reg == null) {
        // if this does not directly reference a register,
        // it might reference an other alias definition
        var alias = definition.symbolTable().findAs(targetIdent, AliasDefinition.class);
        if (alias == null || alias.kind != AliasDefinition.AliasKind.REGISTER) {
          throw error("Unknown alias source register", targetIdent.location())
              .locationDescription(targetIdent.location(), "Unknown register `%s`.", targetIdent)
              .build();
        }
        check(alias);
        reg = (RegisterDefinition) requireNonNull(alias.computedTarget);
      }

      check(reg);
      definition.computedTarget = reg;

      if (definition.targetType != null) {
        // FIXME: Support relational alias types on registers
        throw new IllegalStateException(
            "Relational alias types are not yet supported, found at: %s".formatted(
                definition.loc.toIDEString()));
      }

      // we have to check the CallIndexExpr "manually" as the normal check cannot handle
      // the wildcards in the alias definition
      if (definition.value instanceof Identifier targetReg) {
        check(targetReg);
      } else if (definition.value instanceof CallIndexExpr expr) {
        var i = 0;
        var wildcardIndices = new ArrayList<Integer>();
        CallIndexExpr.Arguments slice = null;
        var dummyArgs = new ArrayList<CallIndexExpr.Arguments>();

        for (var arg : expr.argsIndices) {
          if (arg.values.size() != 1) {
            addErrorAndStopChecking(
                error("All arguments must have exactly one value", definition.value)
                    .build()
            );
          }
          var argVal = arg.values.getFirst();
          if (argVal instanceof WildcardLiteral) {
            wildcardIndices.add(i);
          } else if (argVal instanceof RangeExpr) {
            if (i != expr.argsIndices.size() - 1) {
              addErrorAndStopChecking(
                  error("Slices can only be done on the innermost dimension.", argVal).build());
            }
            slice = arg;
          } else {
            if (!wildcardIndices.isEmpty()) {
              addErrorAndStopChecking(
                  error("Constant accesses cannot occure after param accesses.",
                      argVal).build());
            }
            dummyArgs.add(arg);
          }

          i++;
        }

        // Determine type based on the dummyArgs (none-wildcards or slice args)
        if (!dummyArgs.isEmpty()) {
          var dummyCallIndexExpr =
              new CallIndexExpr(targetIdent, dummyArgs, List.of(), expr.location);
          dummyCallIndexExpr.symbolTable = expr.symbolTable;
          expr.type = check(dummyCallIndexExpr);
          expr.typeBeforeSlice = dummyCallIndexExpr.typeBeforeSlice;
        } else {
          expr.type = reg.type;
          expr.typeBeforeSlice = reg.type;
        }

        // If the target is a call index expression, we get all fixed arguments of the
        // alias register call.
        definition.computedFixedArgs = AstUtils.flatArguments(dummyArgs);
        definition.computedFixedArgs.forEach(this::check);

        if (slice != null) {
          var typeBeforeSlice = switch (expr.type()) {
            case TensorType type -> {
              if (type.numberOfIndexDims() < wildcardIndices.size()) {
                addErrorAndStopChecking(error("Invalid number of parameters", expr).build());
              }
              yield type.innerType();
            }
            case ConcreteRelationType type -> {
              if (type.argTypes().size() != wildcardIndices.size()) {
                addErrorAndStopChecking(error("Invalid number of parameters", expr).build());
              }
              yield type.resultType();
            }
            default -> {
              if (!wildcardIndices.isEmpty()) {
                addErrorAndStopChecking(error("Params can only acess tensor types", expr).build());
              }
              yield expr.type();
            }
          };

          var oldType = expr.type();
          visitSliceIndexCall(expr, typeBeforeSlice, List.of(slice));
          var innerMostType = (BitsType) expr.type();
          expr.type = setInnerMostType(oldType, innerMostType);
          definition.slice = slice.computedstaticBitSlice;
        }
      }

      var valType = definition.value.type();
      if (definition.aliasType != null) {
        var aliasType = check(definition.aliasType);
        definition.type = aliasType;

        // We have special casting rules here.
        // They aren't as strict as implicit casting and not as lax as explicit casting.
        // FIXME: Should we allow same rules as explicit casts here?
        if (canImplicitCast(valType, aliasType)) {
          definition.value = tryWrapImplicitCast(definition.value, definition.type);
        } else if (aliasType instanceof DataType aliasDataType
            && valType instanceof DataType valDataType
            && canAliasCastType(valDataType, aliasDataType)
        ) {
          definition.value = new CastExpr(definition.value, aliasType);
        } else {
          throw typeMismatchError(definition.value, aliasType, valType);
        }
      } else {
        definition.type = valType;
      }

      return null;
    }

    throw new IllegalStateException(
        "Kind %s not yet implemented, found at: %s".formatted(definition.kind,
            definition.loc.toIDEString()));
  }

  private Type setInnerMostType(Type type, BitsType innerType) {
    return switch (type) {
      case TensorType tensorType -> new TensorType(tensorType.indexDims(), innerType);
      case ConcreteRelationType relType -> Type.concreteRelation(relType.argTypes(), innerType);
      default -> innerType;
    };
  }

  @Override
  public Void visit(AnnotationDefinition definition) {
    // NOTE: I have the suspicion that we might have to delay the typechecking until the definition
    // on which the annotation is placed is completely typed checked.
    if (definition.annotation == null) {
      System.out.printf("");
    }
    requireNonNull(definition.annotation).typeCheck(definition, this);
    return null;
  }

  @Override
  public Void visit(EnumerationDefinition definition) {
    var type = definition.enumType != null ? check(definition.enumType) : null;
    if (type != null && !(type instanceof BitsType)) {
      throw error("Type mismatch", definition)
          .locationDescription(requireNonNull(definition.enumType),
              "Expected bits type but got `%s`", type)
          .note("In future there will be support for Strings and other types as well.")
          .build();
    }

    // check if there are enums
    if (definition.entries.isEmpty()) {
      throw error("No enumeration entries", definition)
          .locationDescription(definition,
              "The enumeration has no entries, but at least one is required.")
          .build();
    }

    int nextVal = 0;
    for (var entry : definition.entries) {
      if (entry.value != null) {
        check(entry.value);
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
          throw typeMismatchError(entry.value, type, entry.value.type());
        }
      }
    }

    return null;
  }

  @Override
  public Void visit(ExceptionDefinition definition) {
    var types = definition.params.stream().map(param -> check(param.typeLiteral)).toList();
    check(definition.statement);
    definition.type = Type.concreteRelation(types, Type.void_());
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
    importDefinition.moduleAst.definitions.forEach(this::check);
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
  public Void visit(Parameter definition) {
    check(definition.typeLiteral);
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
            addErrorAndStopChecking(error(
                "No " + purpose.name() + " registers were declared but one was expected",
                definition.location()).build());
          } else if (registers.size() != 1) {
            addErrorAndStopChecking(error(
                "Multiple " + purpose.name() + " registers were declared but only one was expected",
                SourceLocation.join(registers.stream().map(Node::location).toList())).build());
          }
        }
        case OPTIONAL -> {
          if (!(registers.isEmpty() || registers.size() == 1)) {
            addErrorAndStopChecking(error(
                "Multiple " + purpose.name()
                    + " registers were declared but zero or one was expected",
                SourceLocation.join(registers.stream().map(Node::location).toList())).build());
          }
        }
        case AT_LEAST_ONE -> {
          if (registers.isEmpty()) {
            addErrorAndStopChecking(error(
                "Zero " + purpose.name() + " registers were declared but at least one was expected",
                definition.location()).build());
          }
        }
        default -> throw new RuntimeException("enum variant not handled");
      }
    }

    // Check whether there exists just one pseudo instruction.
    for (var entry :
        AbiSpecialPurposeInstructionDefinition.Kind.numberOfOccurrencesAbi.entrySet()) {
      var kind = entry.getKey();
      var pseudoInstructions = definition.definitions
          .stream()
          .filter(x -> x instanceof AbiSpecialPurposeInstructionDefinition y && y.kind == kind)
          .toList();

      var noValues = error(
          "No " + kind.name() + " was declared but one was expected",
          definition.location()).build();
      var multipleValues = error(
          "Multiple " + kind.name() + " were declared but one was expected",
          definition.location()).build();

      switch (entry.getValue()) {
        case ONE -> {
          if (pseudoInstructions.isEmpty()) {
            throw noValues;
          } else if (pseudoInstructions.size() > 1) {
            throw multipleValues;
          }
        }
        case OPTIONAL -> {
          if (pseudoInstructions.size() > 1) {
            throw multipleValues;
          }
        }
        case AT_LEAST_ONE -> {
          if (pseudoInstructions.isEmpty()) {
            throw noValues;
          }
        }
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
      throw error("Found a cycle in grammar rules: %s.".formatted(cycle),
          definition.location()).build();
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
              throw error(
                  "Local variable declaration is not at the beginning of a block.",
                  element.location())
                  .locationDescription(element.localVar.location(),
                      "Local variable declared here.")
                  .locationDescription(elements.get(0).location(), "Block starts here.")
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
        throw error("Found multiple assignments to attribute.", element)
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
      throw error("Found invalid attribute assignment.", element)
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
      throw error(
          "Found asm alternatives with differing AsmTypes.", definition.location())
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
        throw error("Semantic predicate expression does not evaluate to Boolean.",
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
          .findAs(definition.attribute, Node.class);
      if (localVarDefinition == null) {
        throw buildIllegalStateException(definition,
            "Assigning to unknown local variable %s.".formatted(definition.attribute.name));
      }

      requireNonNull(definition.asmType);
      requireNonNull(localVarDefinition.asmType);
      if (localVarDefinition.asmType != definition.asmType) {
        throw error("Type Mismatch", definition)
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
        throw error("'+=' assignments are only allowed inside of repetition blocks.",
            definition.location()).build();
      }

      if (definition.isWithinRepetitionBlock) {
        if (!definition.isPlusEqualsAttributeAssign) {
          throw error("Only '+=' assignments are allowed in repetition blocks.",
              definition.location()).build();
        }

        var parentAttributeElement = attributesAssignedInParent.get(definition.attribute.name);
        if (parentAttributeElement == null || parentAttributeElement.asmType == null) {
          throw error(
              "'%s' does not exist in the surrounding block."
                  .formatted(definition.attribute.name), definition.location())
              .note("'+=' assignments have to reference an attribute in the surrounding block.")
              .build();
        }

        if (definition.asmType == null) {
          throw buildIllegalStateException(definition,
              "AsmType of asm element could not be resolved.");
        }

        if (!definition.asmType.canBeCastTo(parentAttributeElement.asmType)) {
          throw error(
              "Element of AsmType %s cannot be '+=' assigned to attribute %s of AsmType %s."
                  .formatted(definition.asmType, definition.attribute.name,
                      parentAttributeElement.asmType), definition.location())
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

    var invocationSymbolOrigin = definition.id.target();
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
      throw error(("Symbol %s used in grammar rule does not reference a grammar rule "
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
      throw error("Local variable with parameters.", enclosingAsmLiteral)
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
      throw error("Arguments Mismatch", enclosingAsmLiteral)
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
        throw error("Type Mismatch in function argument", enclosingAsmLiteral)
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
        throw error("Local variable without AsmType", definition)
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
      errors.add(error("Cannot determine number of expected registers",
          definition.location()).build());
    }

    if (actual == Occurrence.ONE) {
      if (definition.exprs.size() != 1) {
        errors.add(error("Number of registers is incorrect. This definition expects only one",
            definition.location()).build());
      }
    }

    if (actual == Occurrence.ONE) {
      if (definition.exprs.isEmpty()) {
        errors.add(error(
            "Number of registers is incorrect. This definition expects at least one.",
            definition.location()).build());
      }
    }

    return null;
  }

  @Override
  public Void visit(ProcessorDefinition definition) {
    definition.definitions.forEach(this::check);
    check(definition.implementedIsaNode());

    // FIXME: Do we need to limit certain operations here?
    //  (like Resource access -- except memory write of course)

    BiConsumer<Definition, String> addConflictDiag =
        (def, name) -> errors.add(
            error("Conflicting definitions.", definition.identifier())
                .locationDescription(definition.identifier(), "Contains multiple `%s` definitions.",
                    name)
                .note("Only one `%s` definition is allowed.", name)
                .build());

    // check for multiple process definitions
    for (var type : CpuProcessDefinition.ProcessKind.values()) {
      var procCount = definition.findCpuProcDef(type).count();
      if (procCount > 1) {
        addConflictDiag.accept(definition, type.keyword);
      }
    }

    // check for multiple function definitions
    for (var type : CpuFunctionDefinition.BehaviorKind.values()) {
      var procCount = definition.findCpuFuncDef(type).count();
      if (procCount > 1) {
        addConflictDiag.accept(definition, type.keyword);
      }
    }

    var start = definition.findCpuProcDef(CpuProcessDefinition.ProcessKind.RESET)
        .findFirst().orElse(null);
    if (start == null) {
      DeferredDiagnosticStore.add(
          warning("Missing `reset` definition.", definition.identifier())
              .description(
                  "Without `reset`, the program counter and every other "
                      + "register is initialized with 0x0 by default. ")
              .build()
      );
    }

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
    throw new IllegalStateException("Not implemented behavior kind: " + definition.kind);
  }

  @Override
  public Void visit(CpuMemoryRegionDefinition definition) {
    if (definition.stmt != null) {
      check(definition.stmt);
    }
    return null;
  }

  @Override
  public Void visit(CpuProcessDefinition definition) {
    switch (definition.kind) {
      case RESET -> check(definition.statement);
      default -> throwUnimplemented(definition);
    }
    return null;
  }

  @Override
  public Void visit(MicroArchitectureDefinition definition) {
    if (!(definition.isa.target() instanceof InstructionSetDefinition)) {
      throw error("ISA required", definition.isa)
          .locationDescription(definition.isa, "A MIA implements an ISA but this points to a %s",
              requireNonNull(definition.isa.target()).getClass().getSimpleName()).build();
    }
    definition.definitions.forEach(this::check);
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
    definition.outputs.forEach(this::check);
    definition.outputs.forEach(output -> {
      if (!(output.type() instanceof InstructionType)
          && !(output.type() instanceof FetchResultType)) {
        addErrorAndStopChecking(
            error("Type Mismatch", output)
                .description(
                    "The type of a stage output must be an InstructionType or a FetchResultType.")
                .build());
      }
    });

    check(definition.statement);

    return null;
  }

  @Override
  public Void visit(CacheDefinition definition) {
    throwUnimplemented(definition);
    return null;
  }

  @Override
  public Void visit(LogicDefinition definition) {
    var logicTypeString = definition.logicTypeIdentifiers.stream().map(i -> i.name).collect(
        Collectors.joining(" "));
    var logicTypeMapping = Map.of(
        "branch prediction", LogicDefinition.LogicType.BranchPrediction,
        "control", LogicDefinition.LogicType.Control,
        "forwarding", LogicDefinition.LogicType.Forwarding
    );
    if (!logicTypeMapping.containsKey(logicTypeString)) {
      addErrorAndStopChecking(
          error("Unknown logic type: `%s`".formatted(logicTypeString), definition)
              .suggestions(Levenshtein.sortAll(logicTypeString, logicTypeMapping.keySet()))
              .build());
    }
    definition.logicType = logicTypeMapping.get(logicTypeString);
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
      origin = identifier.target();
      innerName = identifier.name;
      fullName = identifier.name;
    } else if (expr instanceof IdentifierPath path) {
      origin = path.target();
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

    if (origin instanceof RangeFormatField field) {
      // FIXME: Unfortonatley the format fields need to be specified in declare-after-use for now
      expr.type = field.type;
      return;
    }

    if (origin instanceof TypedFormatField field) {
      // FIXME: Unfortonatley the format fields need to be specified in declare-after-use for now
      expr.type = field.typeLiteral.type;
      return;
    }

    if (origin instanceof DerivedFormatField field) {
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
      expr.type = requireNonNull(letExpr.getTypeOf(innerName));
      return;
    }

    if (origin instanceof LetStatement letStatement) {
      // No need to check because this can only be the case if we are inside the let statement.
      expr.type = requireNonNull(letStatement.getTypeOf(innerName));
      return;
    }

    if (origin instanceof ForallStatement forallStatement) {
      // No need to check because this can only be the case if we are inside the for statement.
      expr.type =
          forallStatement.indices.stream()
              .filter(index -> index.identifier().name.equals(innerName))
              .findFirst()
              .orElseThrow()
              .identifier().type();
      return;
    }

    if (origin instanceof ForallExpr forallExpr) {
      // No need to check because this can only be the case if we are inside the for statement.
      expr.type =
          forallExpr.indices.stream()
              .filter(index -> index.identifier().name.equals(innerName))
              .findFirst()
              .orElseThrow()
              .identifier().type();
      return;
    }

    if (origin instanceof FunctionDefinition functionDefinition) {
      // It's a call without arguments
      check(functionDefinition);

      if (!functionDefinition.params.isEmpty()) {
        // We can still continue after the error here, no need to stop checking. We simply assume
        // it returns the type of the function. This is only possible because we don't have
        // function overloading.
        errors.add(error("Invalid Function Call", expr)
            .description("Expected `%s` arguments but got `%s`", functionDefinition.params.size(),
                0)
            .build());
      }
      expr.type = functionDefinition.retType.type;
      return;
    }

    if (origin instanceof ExceptionDefinition exceptionDef) {
      // TODO: Check if exception was called with a raise
      // It's a call without arguments
      check(exceptionDef);

      if (!exceptionDef.params.isEmpty()) {
        // No need to stop evaluation we can still continue.
        errors.add(error("Invalid Exception Raise", expr)
            .description("Expected `%s` arguments but got `%s`", exceptionDef.params.size(),
                0)
            .build());
      }

      expr.type = Type.void_();
      return;
    }

    if (origin instanceof RegisterDefinition
        || (origin instanceof AliasDefinition aliasDef && aliasDef.kind.equals(
        AliasDefinition.AliasKind.REGISTER))) {
      var originDef = (Definition) origin;
      check(originDef);
      expr.type = ((TypedNode) originDef).type();
      return;
    }

    if (origin instanceof EnumerationDefinition.Entry enumEntry) {
      check(enumEntry.enumDef);
      expr.type = check(requireNonNull(enumEntry.value));
      return;
    }

    if (origin instanceof StageOutputDefinition output) {
      check(output);
      expr.type = output.type();
      return;
    }

    if (origin != null) {
      // It's not a builtin but we don't handle it yet.
      // We might be here from a call expr and it might be necessary to handle the call for another
      // definition.

      throw addErrorAndStopChecking(error("Invalid Expression", expr)
          .locationDescription(expr,
              "The name '%s' points to a `%s` which cannot be used as an expression.", fullName,
              origin.getClass().getSimpleName())
          .build());
    }

    // It's also possible to call functions without parenthesis if the function doesn't take any
    // arguments.
    var matchingBuiltins = BuiltInTable.builtIns()
        .filter(b -> b.signature().argTypeClasses().isEmpty())
        .filter(b -> b.name().equals(innerName))
        .toList();

    if (matchingBuiltins.size() == 1) {
      expr.type = matchingBuiltins.get(0).returns(List.of());
      return;
    }

    throw new IllegalStateException(
        "Cannot find symbol `%s` found at: %s (The symbol resolver should already have caught that)"
            .formatted(fullName, expr.location().toIDEString()));
  }

  @Override
  public Void visit(Identifier expr) {
    visitIdentifiable(expr);
    return null;
  }

  private BuiltInCheckResult checkLogicalBuiltIn(Expr left,
                                                 Expr right, WithLocation location) {

    // Both sides must be boolean
    if (!(left.type() instanceof BoolType) && !canImplicitCast(left.type(), Type.bool())) {
      // We can still continue here, the expression still returns a boolean.
      errors.add(error("Type Mismatch", location)
          .locationDescription(location, "Expected a `Bool` here but the left side was an `%s`",
              left.type())
          //.description("The `%s` operator only works on booleans.", builtIn.operator())
          .build());
    }
    left = wrapImplicitCast(left, Type.bool());

    if (!(right.type() instanceof BoolType) && !canImplicitCast(right.type(), Type.bool())) {
      // We can still continue here, the expression still returns a boolean.
      errors.add(error("Type Mismatch", location)
          .locationDescription(location, "Expected a `Bool` here but the right side was an `%s`",
              right.type())
          //.description("The `%s` operator only works on booleans.", builtIn.operator())
          .build());
    }
    right = wrapImplicitCast(right, Type.bool());

    // Return is always boolean
    var type = Type.bool();
    return new BuiltInCheckResult(type, List.of(left, right));
  }

  @Override
  public Void visit(BinaryExpr expr) {
    check(expr.left);
    check(expr.right);

    var builtin =
        AstUtils.getOperatorBuiltIn(expr.operator(), List.of(expr.left.type(), expr.right.type()));

    BuiltInCheckResult checkResult;
    if (Operator.logicalComparisions.contains(expr.operator())) {
      // Unfortunatley we cannot do this in the checkBuiltin because this information is only in the
      // operator and not in the builtin function we want to call.
      checkResult = checkLogicalBuiltIn(expr.left, expr.right, expr);
    } else {
      checkResult = checkBuiltin(builtin, List.of(expr.left, expr.right), expr);
    }

    if (checkResult.castedArgs != null) {
      expr.left = checkResult.castedArgs.get(0);
      expr.right = checkResult.castedArgs.get(1);
    }
    expr.type = checkResult.type;
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

    // If there are constants values and only one "concrete" type we can cast the constants to that
    // one.
    if (types.stream().anyMatch(x -> x instanceof ConstantType)) {
      var concreteTypes =
          types.stream().filter(x -> !(x instanceof ConstantType)).distinct().toList();
      if (concreteTypes.isEmpty()) {
        addErrorAndStopChecking(error("Type Mismatch", expr)
            .locationDescription(expr,
                "At least one value has to have a concrete bitwidth for a concatination")
            .build());
      }
      if (concreteTypes.size() > 1) {
        addErrorAndStopChecking(error("Type Mismatch", expr)
            .locationDescription(expr,
                "The concatination operation can only concat bits or strings.")
            .locationNote(expr, "Provided types: %s",
                String.join(", ", types.stream().map(Type::toString).toList()))
            .locationHelp(expr,
                "Constant types can be implicitly casted to a concrete type if only one such "
                    + "concrete type appears.")
            .build());
      }

      expr.expressions.replaceAll(e -> wrapImplicitCast(e, concreteTypes.get(0)));
      types = expr.expressions.stream().map(this::check).toList();
    }

    if (types.stream().allMatch(x -> x instanceof DataType)) {
      var width = types.stream().map(t -> ((DataType) t).bitWidth()).reduce(0, Integer::sum);
      expr.type = Type.bits(width);
      return null;
    }

    throw addErrorAndStopChecking(error("Type Mismatch", expr)
        .locationNote(expr, "Provided types: %s",
            String.join(", ", types.stream().map(Type::toString).toList()))
        .description(
            "The concatenation operation can only be applied on a set of strings or a set of bits.")
        .build());
  }

  @Override
  public Void visit(IntegerLiteral expr) {
    expr.type = new ConstantType(expr.number);
    return null;
  }

  @Override
  public Void visit(WildcardLiteral expr) {
    // if a node contains a potential wildcard literal expression, it must ensure that
    // it is not type checked.
    throw new IllegalStateException("The wildcard literal should never be typechecked.");
  }

  @Override
  public Void visit(BinaryLiteral expr) {
    expr.type = new ConstantType(expr.number);
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
      addErrorAndStopChecking(error("Type Mismatch", expr.from)
          .description("The from part of a range must be a number but was `%s`", fromType)
          .build());
    }

    if (!(toType instanceof BitsType) && !(toType instanceof ConstantType)) {
      addErrorAndStopChecking(error("Type Mismatch", expr.to)
          .description("The to part of a range must be a number but was `%s`", fromType)
          .build());
    }

    var fromVal = constantEvaluator.eval(expr.from).value();
    var toVal = constantEvaluator.eval(expr.to).value();

    if (toVal.compareTo(fromVal) > 0) {
      addErrorAndStopChecking(error("Invalid range", expr)
          .description("From is %s but to is %s, but ranges must be decreasing", fromVal, toVal)
          .build());
    }

    // FIXME: The type doesn't really make sense but we don't have a propper range type
    expr.type = Type.bits(fromVal.intValueExact() - toVal.intValueExact() + 1);
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

    // 1. Check whether the base exists.
    var customTarget = expr.symbolTable().findAs(expr.baseType, Node.class);
    if (!(customTarget instanceof UsingDefinition) && !(customTarget instanceof FormatDefinition)) {
      customTarget = null;
    }

    if (!Type.builtinTypeBases.contains(base) && customTarget == null) {
      var candidateTypes = new ArrayList<>(Type.builtinTypeBases);
      candidateTypes.addAll(
          expr.symbolTable().allSymbolNamesOf(FormatDefinition.class, UsingDefinition.class));
      var suggestions =
          Levenshtein.suggestions(expr.baseType.pathToString(), candidateTypes);

      addErrorAndStopChecking(error("Unknown Type `%s`".formatted(base), expr)
          .locationDescription(expr, "No type with that name exists.")
          .suggestions(suggestions)
          .build());
    }

    // 2. Calculate the sizes
    var sizes = expr.sizeIndices.stream().map(sizeExpr -> {
      check(sizeExpr);
      var size = constantEvaluator.eval(sizeExpr).value().intValueExact();

      if (size < 1) {
        addErrorAndStopChecking(error("Invalid Type Notation", sizeExpr.location())
            .locationDescription(sizeExpr.location(),
                "Width must of a %s must be greater or equal 1 but was %d", base, size)
            .build());
      }

      return size;
    }).toList();


    // For the builtin bits types we can infer the size (sometimes)
    if (List.of("Bits", "SInt", "UInt").contains(base)
        && expr.sizeIndices.isEmpty()
        && preferredBitWidth != null) {

      sizes = List.of(preferredBitWidth);
    }

    // 3. Create the builtin types
    Map<String, Supplier<Type>> unSizedBuiltins = Map.of(
        "Bool", Type::bool,
        "String", Type::string,
        "Instruction", MicroArchitectureType::instruction,
        "FetchResult", MicroArchitectureType::fetchResult
    );

    if (unSizedBuiltins.containsKey(base)) {
      if (!sizes.isEmpty()) {
        addErrorAndStopChecking(error("Invalid Type Notation", expr.location())
            .description("The %s type doesn't use the size notation.", base)
            .help("Try removing the size parameter here.")
            .build());
      }
      return unSizedBuiltins.get(base).get();
    }

    Map<String, Function<Integer, BitsType>> sizedBuiltins = Map.of(
        "Bits", Type::bits,
        "SInt", Type::signedInt,
        "UInt", Type::unsignedInt
    );

    if (sizedBuiltins.containsKey(base)) {
      if (sizes.isEmpty()) {
        addErrorAndStopChecking(error("Invalid Type Notation", expr.location())
            .description(
                "Unsized `%s` can only be used in special places when it's obvious what the bit"
                    + " width should be.",
                base)
            .help("Try adding a size parameter here.")
            .build());
      }

      if (sizes.size() == 1) {
        return sizedBuiltins.get(base).apply(sizes.getFirst());
      }

      return new TensorType(sizes.subList(0, sizes.size() - 1),
          sizedBuiltins.get(base).apply(sizes.getLast()));
    }

    // 4. Create the custom types
    Type customTargetType = switch (requireNonNull(customTarget)) {
      case UsingDefinition usingDef -> check(usingDef.typeLiteral);
      case FormatDefinition formatDef -> {
        check(formatDef);
        yield new FormatType(formatDef);
      }
      default -> throw new IllegalStateException("Unexpected value: " + customTarget);
    };

    if (sizes.isEmpty()) {
      return customTargetType;
    }

    // Only some types can be used to create a tensor.
    if (customTargetType instanceof BitsType customBits) {
      return new TensorType(sizes, customBits);
    }

    if (customTargetType instanceof TensorType customTensor) {
      return new TensorType(sizes, customTensor);
    }

    throw addErrorAndStopChecking(error("Invalid Tensor Type", expr)
        .locationDescription(expr, "You can only create tensors from data types.")
        .build());
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

    var builtin = switch (expr.unOp().operator) {
      case NEGATIVE -> BuiltInTable.NEG;
      case COMPLEMENT -> {
        if (!(innerType instanceof BitsType)) {
          addErrorAndStopChecking(error("Type Mismatch", expr)
              .description("Expected a numerical type with fixed bit-width but got `%s`", innerType)
              .build());
        }
        yield BuiltInTable.NOT;
      }
      case LOG_NOT -> {
        if (!innerType.equals(Type.bool())) {
          addErrorAndStopChecking(
              error("Type Mismatch: expected `Bool`, got `%s`".formatted(innerType), expr)
                  .help("For numerical types you can negate them with a minus `-`")
                  .build());
        }
        yield BuiltInTable.NOT;
      }
    };
    expr.computedTarget = builtin;

    var result = checkBuiltin(builtin, List.of(expr.operand), expr);
    if (result.castedArgs != null) {
      expr.operand = result.castedArgs.get(0);
    }
    expr.type = result.type();

    return null;
  }

  private Constant.BitSlice.Part checkSliceRange(RangeExpr range, BitsType valueType) {
    int from = constantEvaluator.eval(range.from).value().intValueExact();
    int to = constantEvaluator.eval(range.to).value().intValueExact();

    // NOTE: From is always larger than to
    var rangeSize = (from - to) + 1;
    if (rangeSize < 1) {
      addErrorAndStopChecking(error("Invalid Range", range)
          .description("Range must be >= 1 but was %s", rangeSize)
          .build());
    }

    if (from >= valueType.bitWidth()) {
      addErrorAndStopChecking(error("Invalid Range", range)
          .description("Range start %d out of bounds for `%s`", from, valueType)
          .build());
    }
    if (to < 0) {
      addErrorAndStopChecking(error("Invalid Range", range)
          .description("Range end must be at least zero but was %s", to)
          .build());
    }

    return new Constant.BitSlice.Part(from, to);
  }

  private Constant.BitSlice.Part checkIndexSlice(Expr indexExpr, BitsType valueType) {
    check(indexExpr);
    int sliceIndex = constantEvaluator.eval(indexExpr).value().intValueExact();
    if (sliceIndex >= valueType.bitWidth()) {
      addErrorAndStopChecking(error("Invalid Index", indexExpr)
          .description("Index %d out of bounds for `%s`", sliceIndex, valueType)
          .build());
    }
    if (sliceIndex < 0) {
      addErrorAndStopChecking(error("Invalid Index", indexExpr)
          .description("Index must be at least zero but was %s", sliceIndex)
          .build());
    }
    return new Constant.BitSlice.Part(sliceIndex, sliceIndex);
  }

  /**
   * Visits one or multiple index and slice calls.
   *
   * <p>The index is necessary because at the point this is called some args might have already
   * been consumed.
   *
   * <p>Sets expr.type to the the type of the subcalls.
   *
   * @param expr            to visit.
   * @param typeBeforeSlice is the type just before.
   * @param sliceGroups     the list or arguments which hold slices or indexes
   */
  private void visitSliceIndexCall(CallIndexExpr expr, Type typeBeforeSlice,
                                   List<CallIndexExpr.Arguments> sliceGroups) {
    if (sliceGroups.isEmpty()) {
      expr.type = typeBeforeSlice;
      return;
    }

    var lastSlice = sliceGroups.getLast();

    if (!(typeBeforeSlice instanceof BitsType) && !(typeBeforeSlice instanceof TensorType)) {
      var loc = expr.target.location().join(lastSlice.location);
      addErrorAndStopChecking(error("Type Mismatch", loc)
          .description("Only bit types can be sliced but the target was a `%s`", typeBeforeSlice)
          .build());
    }

    Type currType = typeBeforeSlice;
    for (var slice : sliceGroups) {
      if (currType instanceof BitsType currBitsType) {
        // construct BitSlice for each slice group
        var parts = new ArrayList<Constant.BitSlice.Part>();
        for (var partExpr : slice.values) {
          // for each part construct a BitSlice.Part
          var part = partExpr instanceof RangeExpr rangeSlice
              ? checkSliceRange(rangeSlice, currBitsType)
              : checkIndexSlice(partExpr, currBitsType);
          parts.add(part);
        }

        var bitSlice = new Constant.BitSlice(parts.toArray(new Constant.BitSlice.Part[0]));
        if (bitSlice.hasOverlappingParts()) {
          // FIXME: Currently, we don't allow overlapping slices for both slices on read values
          // and write targets.
          // In the future we might want to allow overlapping slices on read values.
          // For written values (`X(1, 1) := 2`) this must not be allowed, as the same value
          // position is written twice.
          addErrorAndStopChecking(error("Overlapping slice parts", slice.location)
              .locationDescription(slice.location, "Some parts of the slice are overlapping.")
              .note("Slices must have distinct, non-overlapping parts.")
              .build());
        }

        currType = Type.bits(bitSlice.bitSize());
        slice.computedstaticBitSlice = bitSlice;
        slice.type = currType;
        expr.type = currType;
      }
      if (currType instanceof TensorType currTensoType) {
        if (slice.values.size() != 1) {
          var loc = slice.values.getFirst().location().join(slice.values.getLast().location());
          addErrorAndStopChecking(error("Invalid Tensor Indexing", loc)
              .locationDescription(loc,
                  "Indexing tensors only allows one argument per parenthesis group.")
              .build());
        }

        var indexExpr = slice.values.getFirst();
        if (indexExpr instanceof RangeExpr) {
          addErrorAndStopChecking(error("Invalid Tensor Slice", indexExpr)
              .locationDescription(indexExpr, "Tensors cannot be sliced.")
              .build());
        }
        check(indexExpr);

        @Nullable Integer staticIndex = null;
        if (constantEvaluator.isConstant(indexExpr)) {
          staticIndex = constantEvaluator.eval(indexExpr).value().intValueExact();
        }

        currType = currTensoType.pop();
        expr.type = currType;

        // If a static type is declared, verify it's within the bounds and
        if (staticIndex != null) {
          if (staticIndex < 0
              || staticIndex >= currTensoType.outerMostDimension()) {
            addErrorAndStopChecking(error("Tensor Index out of bounds", indexExpr)
                .locationDescription(indexExpr,
                    "Invalid index `%d` for tensor `%s`", staticIndex, currTensoType)
                .build());
          }

          // Note: the computed bitslice here is already for the lowering where we assume all
          // tensors are flattened
          var bitWidth = switch (currType) {
            case BitsType bt -> bt.bitWidth();
            case TensorType tt -> tt.flattenBitsType().bitWidth();
            default -> throw new IllegalStateException();
          };
          slice.computedstaticBitSlice =
              Constant.BitSlice.of(bitWidth * staticIndex + bitWidth - 1, bitWidth * staticIndex);
          slice.type = currType;

        } else {
          /* For dynamic indexes we cannot really check anything.
             Consider the following example:
             X: Bits<8><64>
             X(idx) -> Expr has the type Bits<64>
               ^^^
               With the type Bits<3> this would fit perfectly because 3^2 == 8

             However for the following example there doesn't exist any type at all that would fit
             X: Bits<7><64>, because there isn't any x such that x^2 == 7

             Obviously we could add a lot of special cases but we will never be able to solve the
             underlying problem because our typesystem isn't strong enough (and I would argue it
             also shouldn't be that strong/complex).
           */
          slice.type = currType;
        }
      }
    }
  }

  /**
   * At this point we already know what is called but there are still some dangling subcalls that
   * need to be resolved and which might result in a different type.
   *
   * <p>Modiefies the provided expr.
   *
   * @param expr of the call.
   */
  @SuppressWarnings("NullAway") // required as there is a false positive in the switch
  private void visitSubCall(CallIndexExpr expr, Type typeBeforeSubCall) {
    if (expr.subCalls.isEmpty()) {
      expr.type = typeBeforeSubCall;
      return;
    }


    // FIXME: Make this more generic
    switch (expr.target.path().target()) {
      case CounterDefinition counter -> {
        if (!expr.slices().isEmpty()) {
          addErrorAndStopChecking(error("Invalid counter sub-call", expr)
              .locationDescription(expr, "Cannot do sub call and slice on counter.").build());
        }
        var validSubCalls = List.of("next");
        if (expr.subCalls.size() != 1) {
          addErrorAndStopChecking(error("Invalid counter sub-call", expr)
              .locationDescription(expr, "Only a single sub-call expected.").build());
        }
        var subCall = expr.subCalls.getFirst();
        if (!validSubCalls.contains(subCall.id.name)) {
          addErrorAndStopChecking(error("Invalid counter sub-call", subCall.id)
              .locationDescription(expr, "Counter has no sub-call named `%s`", subCall.id.name)
              .build());
        }
        return;
      }
      case null, default -> {
      }
    }


    // Might be a format or status access
    Type type = typeBeforeSubCall;
    for (var subCall : expr.subCalls) {
      var fieldName = subCall.id.name;
      if (type instanceof FormatType formatType) {
        check(formatType.format);

        var fieldType = formatType.format.getFieldType(fieldName);
        if (fieldType == null) {
          var formatName = formatType.format.identifier().name;
          var suggestions = Levenshtein.suggestions(
              fieldName,
              formatType.format.fields.stream()
                  .map(f -> f.identifier().name).toList());

          addErrorAndStopChecking(error("Unknown format field `%s`".formatted(fieldName), expr)
              .description("Format `%s` doesn't have any field with this name", formatName)
              .suggestions(suggestions)
              .build());
        }

        // FIXME: @flofriday replace once computed field ranges are Constant.BitSlice
        var fieldRange = requireNonNull(formatType.format.getFieldRange(fieldName));
        var slicePart = new Constant.BitSlice.Part(fieldRange.from(), fieldRange.to());
        subCall.computedBitSlice = new Constant.BitSlice(slicePart);
        subCall.formatFieldType = fieldType;
        visitSliceIndexCall(expr, subCall.formatFieldType, subCall.argsIndices);
        type = expr.type;
      } else if (type instanceof StatusType) {
        var allowedStatusfields = List.of("negative", "zero", "carry", "overflow");
        if (!allowedStatusfields.contains(fieldName)) {
          var suggestions = Levenshtein.sortAll(fieldName, allowedStatusfields);
          addErrorAndStopChecking(error("Unknown status field `%s`".formatted(fieldName), expr)
              .help("Maybe you meant one of these: %s", String.join(", ", suggestions))
              .build());
        }
        var fieldType = Type.bool();
        subCall.computedStatusIndex = allowedStatusfields.indexOf(fieldName);
        visitSliceIndexCall(expr, fieldType, subCall.argsIndices);
        type = expr.type;
      } else if (type instanceof InstructionType) {
        var allowedStatusfields =
            List.of("address", "read", "unknown", "compute", "verify", "write", "readOrForward",
                "results");
        if (!allowedStatusfields.contains(fieldName)) {
          var suggestions = Levenshtein.sortAll(fieldName, allowedStatusfields);
          addErrorAndStopChecking(
              error("Unknown status or subcall field `%s`".formatted(fieldName), expr)
                  .help("Maybe you meant one of these: %s", String.join(", ", suggestions))
                  .build());
        }

        // FIXME: Research type rules here
        // Is the order important? Are the types of the arguments important? What rulse do even
        // aply. Are all the functions vararg?
        expr.type = fieldName.equals("unknown") ? Type.bool() : Type.void_();
        var argumentfreeFields = List.of("unknown", "compute", "verify");
        if (argumentfreeFields.contains(fieldName)) {
          if (!subCall.argsIndices.isEmpty()) {
            addErrorAndStopChecking(
                error("Wrong Argument Number",
                    SourceLocation.join(subCall.argsIndices.stream().map(a -> a.location).toList()))
                    .description("This subcall doesn't take any arguments.")
                    .build());
          }
          return;
        } else {
          if (subCall.argsIndices.size() == 0) {
            addErrorAndStopChecking(
                error("Wrong Argument Number",
                    subCall.id)
                    .description("This subcall expects at least one argument.")
                    .build());
          }
        }
      } else if (expr.target instanceof Identifier id
          && id.target() instanceof StageDefinition stageDef) {
        var output =
            stageDef.outputs.stream().filter(o -> o.identifier.name.equals(subCall.id.name))
                .findFirst();
        if (output.isEmpty()) {
          var availableOutputs = stageDef.outputs.stream().map(o -> o.identifier.name).toList();
          addErrorAndStopChecking(error("Unknown stage output", subCall.id)
              .suggestions(Levenshtein.sortAll(subCall.id.name, availableOutputs))
              .build());
        }

        if (!subCall.argsIndices.isEmpty()) {
          addErrorAndStopChecking(
              error("Wrong Argument Number",
                  SourceLocation.join(subCall.argsIndices.stream().map(a -> a.location).toList()))
                  .description("This subcall doesn't take any arguments.")
                  .build());
        }

        expr.type = output.get().type();
      } else {
        addErrorAndStopChecking(error("Cannot resolve `%s`".formatted(fieldName), expr)
            .description("No subcall `%s` exists for the type `%s`",
                fieldName,
                requireNonNull(type))
            .build());
      }
    }
  }

  @Override
  public Void visit(CallIndexExpr expr) {
    // try to find target symbol in symbol table.
    // as identifiers only store AstSymbol origins, and the call expr might refer to a
    // BuiltInSymbol, we must do a separate request to the symbol table an can't rely on the
    // expression's identifier target.
    var targetSymbol = expr.symbolTable().requireSymbol(expr.target.path(), () -> {
      var suggestions = Levenshtein.suggestions(expr.target.path().pathToString(),
          expr.symbolTable().allSymbolNames());

      return error("Unknown call target", expr.target)
          .locationNote(expr.target, "Nothing found that can be called with this name.")
          .suggestions(suggestions);
    });


    // A hack for stage definitions since they don't fit into our typesystem
    if (targetSymbol instanceof SymbolTable.AstSymbol astSymbol
        && astSymbol.origin() instanceof StageDefinition stageDef) {
      processStageCall(expr, stageDef);
      return null;
    }

    switch (targetSymbol) {
      case SymbolTable.AstSymbol astSymbol -> processCallOfTarget(expr, astSymbol.origin());
      case SymbolTable.BuiltInSymbol ignored -> processCallOfBuiltIn(expr);
    }

    var slices = expr.slices();
    // all further argument indices are slice calls
    visitSliceIndexCall(expr, expr.typeBeforeSlice(), slices);
    // after the index slices we might have a type that can be called like .next
    visitSubCall(expr, expr.type());

    return null;
  }

  private void processCallOfBuiltIn(CallIndexExpr expr) {
    // Builtin function
    List<Expr> args =
        !expr.argsIndices.isEmpty() ? expr.argsIndices.getFirst().values : new ArrayList<>();
    var argTypes = args.stream().map(this::check).toList();
    var builtin = AstUtils.getBuiltIn(expr.target.path().pathToString(), argTypes);

    if (builtin == null) {
      throw addErrorAndStopChecking(error("Invalid call target", expr.target)
          .locationNote(expr.target, "Couldn't find builtin function `%s`",
              expr.target.path())
          .build());
    }

    expr.computedBuiltIn = builtin;

    var checkResult = checkBuiltin(builtin, args, expr);
    if (checkResult.castedArgs != null) {
      expr.replaceArgsFor(0, checkResult.castedArgs);
    }
    expr.typeBeforeSlice = checkResult.type;
    expr.argsIndices.get(0).type = checkResult.type;
  }

  private void processStageCall(CallIndexExpr expr, StageDefinition callTarget) {
    check(callTarget);

    var availableOutputs = callTarget.outputs.stream().map(o -> o.identifier.name).toList();
    if (expr.subCalls.isEmpty()) {
      addErrorAndStopChecking(error("Missing stage output", expr)
          .description(
              "Stages describe outputs and you cannot just refer to a whole stage but to one of "
                  + "the outputs.")
          .suggestions(availableOutputs)
          .build());
    }

    if (expr.subCalls.size() > 1) {
      addErrorAndStopChecking(error("Too many stage outputs", expr)
          .description(
              "You can only refer to one stage output at a time.")
          .build());
    }

    var subcall = expr.subCalls.getFirst();
    var subcallName = subcall.id.name;

    var output = callTarget.outputs.stream().filter(o -> o.identifier.name.equals(subcallName))
        .findFirst();

    if (output.isEmpty()) {
      addErrorAndStopChecking(error("Unknown stage output", subcall.id)
          .suggestions(Levenshtein.sortAll(subcallName, availableOutputs))
          .build());
    }

    expr.type = output.get().type();
  }

  private void processCallOfTarget(CallIndexExpr expr, Node callTarget) {


    // if the target is not a typed node, we just assume that it is some expression
    // that can be sliced.
    // if it is a let expr, we must also only check the target
    if (!(callTarget instanceof TypedNode typedNode) || callTarget instanceof LetExpr) {
      expr.typeBeforeSlice = check((Expr) expr.target);
      return;
    }

    switch (typedNode) {
      case Expr e -> check(e);
      case Definition e -> check(e);
      default -> throw new IllegalStateException("Unexpected value: " + typedNode);
    }

    var argGroups = expr.args();
    var argExprs = AstUtils.flatArguments(argGroups);

    for (var argExpr : argExprs) {
      if (argExpr instanceof RangeExpr) {
        addErrorAndStopChecking(error("Invalid argument", argExpr)
            .locationNote(argExpr, "Expected argument value but got a range expression.")
            .build());
      }
    }

    var type = typedNode.type();
    if (type instanceof ConcreteRelationType relType && relType.argTypes().isEmpty()) {
      // if a relation type expects no arguments, no argument group is considered
      expr.typeBeforeSlice = relType.resultType();
    } else if (type instanceof ConcreteRelationType relType) {
      if (relType.argTypes().size() != argExprs.size()) {
        addErrorAndStopChecking(error("Invalid number of arguments", expr)
            .locationNote(expr, "Expected %s arguments but got %s.", relType.argTypes().size(),
                argExprs.size()).build());
      }

      if (argGroups.size() != 1) {
        // concrete relations have exactly one group
        throw new IllegalStateException(
            "At this point, we should know that there is exactly one argument group.");
      }

      // concrete relation types have only a single arg group (e.g. functions)
      var argGroup = argGroups.getFirst();
      for (int i = 0; i < argGroup.values.size(); i++) {
        var arg = argGroup.values.get(i);
        check(arg);
        argGroup.values.set(i, tryWrapImplicitCast(arg, relType.argTypes().get(i)));
      }

      // set the type, this is overridden if there are slice or subcalls that get processed next
      expr.typeBeforeSlice = relType.resultType();
      // set the arg group type (representing the call result)
      argGroups.getFirst().type = relType.resultType();

    } else if ((expr.computedTarget() instanceof RegisterDefinition
        || expr.computedTarget() instanceof AliasDefinition)
        && type instanceof TensorType tensorType
        && !argGroups.isEmpty()) {
      // FIXME: We don't do any typechecking here as the type rules would be a bit murky in my
      // opinion and hard for users to understand.
      // However, the VIAM does expect quite explicit types, so we cast it to that type.
      for (int i = 0; i < argGroups.size(); i++) {
        var argGroup = argGroups.get(i);
        if (argGroup.values.size() != 1) {
          addErrorAndStopChecking(error("Invalid tensor index", argGroup.location)
              .locationDescription(argGroup.location,
                  "Tensor indexing expects exactly one argument.")
              .build());
        }
        var arg = argGroup.values.getFirst();
        check(arg);
        var indexType = Type.bits(BitsType.indexWidthFor(tensorType.indexDims().get(i)));
        argGroup.values.set(0, tryWrapExplicitCast(arg, indexType));
      }

      expr.typeBeforeSlice = ((TensorType) type).pop(argGroups.size());
    } else {
      if (!argGroups.isEmpty()) {
        // if there are argument groups, there was some logic failure.
        // NOTE: This will change onces we have tensor registers.
        throw new IllegalStateException(
            "Non concrete relation types must not have any arguments.");
      }
      expr.typeBeforeSlice = typedNode.type();
    }

    var targetSizeExpr = expr.target.size();
    if (targetSizeExpr != null) {
      if (!(expr.typeBeforeSlice() instanceof BitsType exprType)) {
        throw addErrorAndStopChecking(error("Invalid scaling type", targetSizeExpr)
            .locationDescription(targetSizeExpr, "Result type `%s` cannot be scaled.",
                expr.typeBeforeSlice())
            .build());
      }
      // handle the targetSize expression if defined
      int multiplier = constantEvaluator.eval(targetSizeExpr).value().intValueExact();

      if (callTarget instanceof MemoryDefinition) {
        // in case of a memory definition, scale the result type
        expr.typeBeforeSlice = exprType.scaleBy(multiplier);
      } else {
        addErrorAndStopChecking(error("Invalid call size", expr)
            .locationDescription(expr.target, "The call target doesn't support a size expression.")
            .build());
      }
    }


  }


  @SuppressWarnings("UnusedVariable")
  @Override
  public Void visit(IfExpr expr) {
    check(expr.condition);
    expr.condition = wrapImplicitCast(expr.condition, Type.bool());
    var condType = expr.condition.type();
    if (condType != Type.bool()) {
      // We can still proceed checking the rest of the program.
      errors.add(typeMismatchError(expr, Type.bool(), condType));
    }

    var thenType = check(expr.thenExpr);
    var elseType = check(expr.elseExpr);

    // If only one branch should be checked, directly propergate the type, and don't check whether
    // the branches are compatible.
    // This is intended: https://github.com/OpenVADL/open-vadl/issues/47#issuecomment-2725475475
    if (branchStrategy.equals(BranchStrategy.ONE)) {
      var condVal = constantEvaluator.eval(expr.condition).value().intValueExact();
      expr.type = condVal == 1 ? thenType : elseType;
      return null;
    }

    // FIXME: Fix this with bidirectional checking in the future
    if (thenType instanceof ConstantType && elseType instanceof ConstantType
        && !thenType.equals(elseType)) {
      addErrorAndStopChecking(error("Type Mismatch", expr)
          .description("Both branches return different constant types.")
          .help("Add an explicit cast on one of the branches.")
          .note("In the future this will work without a cast.")
          .build());
    }

    // Apply general implicit casting rules after specialised once.
    expr.thenExpr = wrapImplicitCast(expr.thenExpr, elseType);
    thenType = expr.thenExpr.type();
    expr.elseExpr = wrapImplicitCast(expr.elseExpr, thenType);
    elseType = expr.elseExpr.type();

    if (!thenType.equals(elseType)) {
      addErrorAndStopChecking(error("Type Mismatch", expr)
          .description(
              "Both the than and else branch should have the same type "
                  + "but than is `%s` and else is `%s`.",
              thenType, elseType)
          .build());
    }

    expr.type = thenType;
    return null;
  }

  @Override
  public Void visit(LetExpr expr) {
    var valType = check(expr.valueExpr);

    if (expr.identifiers.size() > 1) {
      if (!(valType instanceof TupleType valTupleType)) {
        var loc = expr.identifiers.get(0).loc.join(expr.valueExpr.location());
        throw addErrorAndStopChecking(error("Type Mismatch", loc)
            .description("Tuple unpacking only works on tuples but the type was `%s`", valType)
            .build());
      }

      if (expr.identifiers.size() != valTupleType.size()) {
        var loc = expr.identifiers.get(0).loc.join(expr.valueExpr.location());
        addErrorAndStopChecking(error("Invalid Tuple Unpacking", loc)
            .description("Cannot unpack %d values form a `%s`.", expr.identifiers.size(),
                valType)
            .build());
      }

      for (int i = 0; i < expr.identifiers.size(); i++) {
        expr.identifiers.get(i).type = valTupleType.get(i);
      }
    } else {
      expr.identifiers.getFirst().type = valType;
    }

    expr.type = check(expr.body);
    return null;
  }

  @Override
  public Void visit(CastExpr expr) {
    var valType = check(expr.value);

    expr.typeLiteral.type = parseTypeLiteral(expr.typeLiteral, preferredBitWidthOf(valType));
    var litType = expr.typeLiteral.type();

    if (!canExplicitCast(valType, litType)) {
      // No need to stop checking we can just assume it works and assign the declared type.
      errors.add(error("Invalid cast", expr)
          .locationDescription(expr, "Cannot cast `%s` to `%s`.", valType, litType)
          .build());
    }

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

    // Check all entities
    var candidateType = check(expr.candidate);
    for (var kase : expr.cases) {
      kase.patterns.forEach(this::check);
      kase.patterns.replaceAll(p -> wrapImplicitCast(p, candidateType));
      for (var pattern : kase.patterns) {
        var patternType = pattern.type();
        if (!candidateType.equals(patternType)) {
          addErrorAndStopChecking(error("Type Mismatch", pattern)
              .locationDescription(pattern, "Expected `%s`, but got `%s`", candidateType,
                  patternType)
              .note("The type of the candidate and the pattern must be the same.")
              .build());
        }
        check(kase.result);
      }
    }
    check(expr.defaultResult);

    // If only one branch should be checked, directly propergate the type, and don't check whether
    // the branches are compatible.
    // This is intended: https://github.com/OpenVADL/open-vadl/issues/47#issuecomment-2725475475
    if (branchStrategy.equals(BranchStrategy.ONE)) {
      var candidateConstant = constantEvaluator.eval(expr.candidate);
      for (var kase : expr.cases) {
        if (kase.patterns.stream()
            .map(constantEvaluator::eval)
            .allMatch(candidateConstant::equals)) {
          expr.type = kase.result.type();
          return null;
        }
      }

      expr.type = expr.defaultResult.type();
      return null;
    }

    // Check that all branches have the same type
    var firstResultType = check(expr.cases.get(0).result);
    for (var kase : expr.cases) {
      kase.result = wrapImplicitCast(kase.result, firstResultType);
      var resultType = kase.result.type();
      if (!resultType.equals(firstResultType)) {
        addErrorAndStopChecking(error("Type Mismatch", kase.result)
            .locationNote(kase.result, "All previous branches were of type `%s`, but this is `%s`",
                firstResultType, resultType)
            .description("All branches of a match must have the same type")
            .build());
      }
    }

    expr.defaultResult = wrapImplicitCast(expr.defaultResult, firstResultType);
    var defaultResultType = expr.defaultResult.type();
    if (!defaultResultType.equals(firstResultType)) {
      addErrorAndStopChecking(error("Type Mismatch", expr.defaultResult)
          .locationNote(expr.defaultResult,
              "All previous branches were of type `%s`, but this is `%s`",
              firstResultType, defaultResultType)
          .description("All branches of a match must have the same type")
          .build());

    }

    expr.type = firstResultType;
    return null;
  }

  @Override
  public Void visit(AsIdExpr expr) {
    throw new IllegalStateException(
        "No %s should ever reach the Typechecker".formatted(expr.getClass().getSimpleName()));
  }

  @Override
  public Void visit(AsStrExpr expr) {
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
  public Void visit(ForallExpr expr) {
    // FIXME: multiple indexes are hard to lower so let's throw an temporary error
    if (expr.indices.size() > 1) {
      addErrorAndStopChecking(error("Not Supported", expr)
          .locationDescription(expr, "Multiple indicies aren't yet supported.")
          .locationHelp(expr, "You can try a workaround with nested forall expressions.")
          .build());
    }

    expr.indices.forEach(index -> {
      // FIXME: Until we have bidirectional typechecking we need this explicit cast
      if (index.typeLiteral == null) {
        throw addErrorAndStopChecking(error("Type Mismatch", index)
            .locationDescription(index,
                "A explicit type cast is required here, like: `forall i: Bits<32> in ...`.")
            .locationNote(index, "In the future this won't be necessary.")
            .build());
      }

      index.identifier().type = check(index.typeLiteral);

      // Check as expression
      if (index.domain instanceof RangeExpr rangeExpr) {
        index.computedFrom = constantEvaluator.eval(rangeExpr.from).value().intValueExact();
        index.computedTo = constantEvaluator.eval(rangeExpr.to).value().intValueExact();

      } else {
        index.computedFrom = constantEvaluator.eval(index.domain).value().intValueExact();
        index.computedTo = index.computedFrom;
      }
    });

    var bodyType = check(expr.body);
    if (!(bodyType instanceof DataType bodyDataType)) {
      throw addErrorAndStopChecking(
          typeMismatchError(expr.body, "Expected a datatype but got", bodyType));
    }

    var totalIterationSpan = expr.indices.stream()
        .mapToInt(
            index -> requireNonNull(index.computedTo) - requireNonNull(index.computedFrom) + 1)
        .sum();

    if (expr.operation == ForallExpr.Operation.FOLD) {
      var builtIn =
          AstUtils.getOperatorBuiltIn(expr.getFoldOperator(), List.of(bodyType, bodyType));

      // FIXME: In the future try a more sophisticated approach that determines the allowed
      // functions based on the types, but this will require a larger rewrite of the
      // builtin-typechecking to allow such uses.
      var allowedFoldBuiltins = Set.of(
          BuiltInTable.ADD,
          BuiltInTable.MUL,
          BuiltInTable.AND,
          BuiltInTable.OR,
          BuiltInTable.XOR
      );

      if (!allowedFoldBuiltins.contains(builtIn)) {
        // We can continue with this error
        var location = requireNonNull(((BinOp) expr.foldOperator)).location;
        errors.add(
            error("Invalid Fold Operator", location)
                .locationDescription(location,
                    "The operator `%s` isn't allowed for a forall fold. ", expr.getFoldOperator())
                .locationNote(location, "%s",
                    "Fold operators must be commutative and associative binary operators which "
                        + "have a neutral element.")
                .build());
      }
      expr.computedFoldBuiltin = builtIn;
    }

    expr.type = switch (expr.operation) {
      case FOLD -> bodyType;
      case TENSOR -> Type.bits(bodyDataType.bitWidth() * totalIterationSpan);
    };

    return null;
  }

  @Override
  public Void visit(SequenceCallExpr expr) {
    throwUnimplemented(expr);
    return null;
  }

  @Override
  public Void visit(ExpandedSequenceCallExpr expr) {
    throwUnimplemented(expr);
    return null;
  }

  @Override
  public Void visit(ExpandedAliasDefSequenceCallExpr expr) {
    throwUnimplemented(expr);
    return null;
  }

  @Override
  public Void visit(ResourceReferenceExression expr) {
    // There isn't really any type that fits here because it basically just a reference to a
    // resource but it cannot be used like a the resource itself so it's not the type of the
    // target resource.
    expr.type = Type.void_();
    return null;
  }

  @Override
  public Void visit(BlockStatement statement) {
    statement.statements.forEach(this::check);
    return null;
  }

  @Override
  public Void visit(LetStatement statement) {
    var valType = check(statement.valueExpr);

    if (statement.identifiers.size() > 1) {
      if (!(valType instanceof TupleType valTupleType)) {
        var loc = statement.identifiers.get(0).loc.join(statement.valueExpr.location());
        throw addErrorAndStopChecking(error("Type Mismatch", loc)
            .description("Tuple unpacking only works on tuples but the type was `%s`", valType)
            .build());
      }

      if (statement.identifiers.size() != valTupleType.size()) {
        var loc = statement.identifiers.get(0).loc.join(statement.valueExpr.location());
        addErrorAndStopChecking(error("Invalid Tuple Unpacking", loc)
            .description("Cannot unpack %d values form a `%s`.", statement.identifiers.size(),
                valType)
            .build());
      }

      for (int i = 0; i < statement.identifiers.size(); i++) {
        statement.identifiers.get(i).type = valTupleType.get(i);
      }
    } else {
      statement.identifiers.getFirst().type = valType;
    }

    check(statement.body);
    return null;
  }

  @Override
  public Void visit(IfStatement statement) {
    check(statement.condition);
    statement.condition = wrapImplicitCast(statement.condition, Type.bool());
    var condType = statement.condition.type();
    if (condType != Type.bool()) {
      // We can continue typechecking with this error.
      errors.add(typeMismatchError(statement.condition, Type.bool(), condType));
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
      // We can continue after this.
      errors.add(typeMismatchError(statement.valueExpression, targetType, valueType));
    }

    var targetSource = switch (statement.target) {
      case Identifier id -> id.target;
      case CallIndexExpr call -> call.computedTarget();
      default -> null;
    };
    var assignableDefinitions =
        List.of(RegisterDefinition.class, CounterDefinition.class, MemoryDefinition.class,
            AliasDefinition.class, FormatField.class, StageOutputDefinition.class);
    if (targetSource == null || !assignableDefinitions.stream()
        .anyMatch(klass -> klass.isInstance(targetSource))) {
      var message = "This is not writable";
      if (targetSource != null) {
        message += " (originates from a %s)".formatted(targetSource.getClass().getSimpleName());
      }
      message += ", but a static value.";
      var diagnostic = error("Cannot Write To Static Target", statement.target)
          .locationDescription(statement.target, "%s", message)
          .locationNote(statement.target,
              "Only registers, counters, alias and memory are writable.")
          .build();
      // We can continue after this.
      errors.add(diagnostic);
    }

    return null;
  }

  @Override
  public Void visit(RaiseStatement statement) {
    check(statement.statement);
    return null;
  }

  @Override
  public Void visit(CallStatement statement) {
    check(statement.expr);
    if (statement.expr.type != Type.void_()) {
      errors.add(
          // We can continue directly
          typeMismatchError(statement.expr, Type.void_(), requireNonNull(statement.expr.type)));
    }
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
    var candidateType = check(statement.candidate);
    for (var kase : statement.cases) {
      kase.patterns.forEach(this::check);
      kase.patterns.replaceAll(p -> wrapImplicitCast(p, candidateType));
      for (var pattern : kase.patterns) {
        var patternType = pattern.type();
        if (!candidateType.equals(patternType)) {
          addErrorAndStopChecking(error("Type Mismatch", pattern)
              .locationDescription(pattern, "Expected `%s`, but got `%s`", candidateType,
                  patternType)
              .note("The type of the candidate and the pattern must be the same.")
              .build());
        }
        check(kase.result);
      }
    }
    if (statement.defaultResult != null) {
      check(statement.defaultResult);
    }
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
        addErrorAndStopChecking(error("Invalid Arguments", loc)
            .description("Calls to instructions only accept named arguments")
            .build());
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
          addErrorAndStopChecking(typeMismatchError(arg, targetType, actualType));
        }
      }


    } else if (statement.instrDef instanceof PseudoInstructionDefinition pseudoDef) {
      check(pseudoDef);

      if (!statement.namedArguments.isEmpty()) {
        var loc = statement.namedArguments.get(0).location()
            .join(statement.namedArguments.get(statement.namedArguments.size() - 1).location());
        addErrorAndStopChecking(error("Invalid Arguments", loc)
            .description("Calls to pseudo instructions only accept unnamed (positional) arguments")
            .build());
      }

      // Check the argument and parameter count
      var paramCount = pseudoDef.params.size();
      var argCount = statement.unnamedArguments.size();
      if (paramCount != argCount) {
        addErrorAndStopChecking(error("Arguments Mismatch", statement.location())
            .description("Expected %s arguments but got %s", paramCount, argCount)
            .build());
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
          errors.add(typeMismatchError(arg, targetType, actualType));
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
    // FIXME: multiple indexes are hard to lower so let's throw an temporary error

    if (statement.indices.size() > 1) {
      addErrorAndStopChecking(error("Not Supported", statement)
          .locationDescription(statement, "Multiple indicies aren't yet supported.")
          .build());
    }

    statement.indices.forEach(index -> {
      // FIXME: Until we have bidirectional typechecking we need this explicit cast
      if (index.typeLiteral == null) {
        throw addErrorAndStopChecking(error("Type Mismatch", index)
            .locationDescription(index,
                "A explicit type cast is required here, like: `forall i: Bits<32> in ...`.")
            .locationNote(index, "In the future this won't be necessary.")
            .build());
      }

      index.identifier().type = check(index.typeLiteral);

      // Check as expression
      if (index.domain instanceof RangeExpr rangeExpr) {
        index.computedFrom = constantEvaluator.eval(rangeExpr.from).value().intValueExact();
        index.computedTo = constantEvaluator.eval(rangeExpr.to).value().intValueExact();
      } else {
        index.computedFrom = constantEvaluator.eval(index.domain).value().intValueExact();
        index.computedTo = index.computedFrom;
      }
    });

    check(statement.body);
    return null;
  }
}
