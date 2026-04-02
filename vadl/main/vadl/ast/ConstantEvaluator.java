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

import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BinaryOperator;
import javax.annotation.Nullable;
import vadl.types.BitsType;
import vadl.types.BoolType;
import vadl.types.BuiltInTable;
import vadl.types.DataType;
import vadl.types.StringType;
import vadl.types.Type;
import vadl.utils.BigIntUtils;
import vadl.utils.SourceLocation;
import vadl.utils.WithLocation;
import vadl.viam.Constant;


/**
 * Can evaluate any expression.
 *
 * <p>The evaluator can only be called on expressions that are already typechecked
 * (including all of its sub-expressions).
 */
class ConstantEvaluator implements ExprVisitor<ConstantValue> {

  private final InterleavedTimingRecorder timingRecorder;
  private final IdentityHashMap<Expr, ConstantValue> cache = new IdentityHashMap<>();
  private final Deque<FunctionFrame> functionStack = new ArrayDeque<>();

  public ConstantEvaluator(InterleavedTimingRecorder timingRecorder) {
    this.timingRecorder = timingRecorder;
  }

  public boolean isConstant(Expr expr) {
    return timingRecorder.withPassTiming("Constant Evaluation", () -> {
      try {
        eval(expr);
        return true;
      } catch (EvaluationError e) {
        return false;
      }
    });
  }

  public ConstantValue eval(Expr expr) {
    return timingRecorder.withPassTiming("Constant Evaluation", () -> {
      // A simple optimization that avoids unneeded traversing the tree.
      if (expr.type instanceof ConstantType) {
        return new ConstantValue(((ConstantType) expr.type).getValue(), expr.type);
      }

      if (cache.containsKey(expr)) {
        return cache.get(expr);
      }

      var result = expr.accept(this);
      cache.put(expr, result);
      return result;
    });
  }

  public ConstantValue evalBuiltin(BuiltInTable.BuiltIn builtin, List<ConstantValue> args,
                                   WithLocation loc) {
    return timingRecorder.withPassTiming("Constant Evaluation", () -> {
      if (args.size() == 1) {
        var innerVal = args.getFirst();
        if (innerVal.type() instanceof ConstantType) {
          if (builtin == BuiltInTable.NEG) {
            return innerVal.withValue(innerVal.value().negate());
          } else if (builtin == BuiltInTable.NOT) {
            return innerVal.withValue(innerVal.value().not());
          }
        }
      }

      // Logical operations
      // And shifts with the left side constant
      // And all other operations with both side constant
      // ... are handled directly.
      if (args.size() == 2) {
        var leftVal = args.getFirst();
        var rightVal = args.getLast();

        // Some general checks that cannot be evaluated
        checkDivisionByZero(builtin, leftVal, rightVal, loc);

        if (BuiltInTable.logicalComparisons.contains(builtin)
            || (BuiltInTable.SHIFTING_BUILT_INS.contains(builtin) && args.getFirst()
            .type() instanceof ConstantType)
            || (leftVal.type() instanceof ConstantType
            && rightVal.type() instanceof ConstantType)) {

          var func = BinOpFuncs.get(builtin);
          if (func != null) {
            var val = func.apply(leftVal.value(), rightVal.value());
            var type = BuiltInTable.arithmeticOperators.contains(builtin) ? new ConstantType(val) :
                Type.bool();

            return new ConstantValue(val, type);
          }
        }
      }

      // NOTE: If you are seeing this issue, someone forgot to add the `compute` method for a
      // built-in function. Look to into BuiltInTable.
      var val = builtin
          .compute(args.stream().map(c -> (Constant) c.toViamConstant()).toList())
          .orElseThrow(() -> new EvaluationError(
              "Built-in function `%s` cannot be constant evaluated (yet).".formatted(
                  builtin.name()),
              loc));

      var areAllArgsConst = args.stream().allMatch(a -> a.type() instanceof ConstantType);
      Type type;
      if (BuiltInTable.arithmeticOperators.contains(builtin)) {
        type = areAllArgsConst ? new ConstantType(val.asVal().integer()) : args.getFirst().type();
      } else if (BuiltInTable.arithmeticComparisons.contains(builtin)) {
        type = Type.bool();
      } else if (args.size() == 1) {
        type = areAllArgsConst ? new ConstantType(val.asVal().integer()) : args.getFirst().type();
      } else {
        // Just throw so that we now we need to implement something, should never happen if we are
        // done.
        throw new IllegalStateException("Cannot find result type for builtin " + builtin.name());
      }

      var finalVal = val.asVal();
      if (!val.type().equals(type) && type instanceof DataType dataType) {
        finalVal = finalVal.castTo(dataType);
      }

      return new ConstantValue(finalVal.integer(), type);
    });
  }

  @SuppressWarnings("UnusedVariable")
  private void checkDivisionByZero(BuiltInTable.BuiltIn builtIn, ConstantValue leftVal,
                                   ConstantValue rightVal, WithLocation loc) {
    var divisionFunctions = List.of(
        BuiltInTable.SDIV,
        BuiltInTable.SDIVS,
        BuiltInTable.SMOD,
        BuiltInTable.SMODS,
        BuiltInTable.UDIV,
        BuiltInTable.UDIVS,
        BuiltInTable.UMOD,
        BuiltInTable.UMODS
    );
    if (rightVal.value().equals(BigInteger.ZERO) && divisionFunctions.contains(builtIn)
    ) {
      throw new EvaluationError("Division by zero cannot be computed.", loc);
    }

  }

  private ConstantValue visitIdentifiable(Expr expr) {
    Node origin;

    if (expr instanceof Identifier identifier) {
      origin = requireNonNull(identifier.target());
    } else if (expr instanceof IdentifierPath path) {
      origin = requireNonNull(path.target());
    } else {
      throw new IllegalStateException();
    }

    if (origin instanceof ConstantDefinition constantDefinition) {
      return eval(constantDefinition.value);
    }

    if (origin instanceof FunctionDefinition functionDefinition) {
      return eval(functionDefinition.expr);
    }

    if (origin instanceof LetExpr letExpr) {
      // FIXME: implement tuple unpacking
      if (letExpr.identifiers.size() > 1) {
        throw new EvaluationError("Cannot evaluate tuple unpacking yet",
            letExpr.identifiers.getFirst().loc.join(
                letExpr.identifiers.getLast()
                    .loc));
      }

      return eval(letExpr.valueExpr);
    }

    if (origin instanceof Parameter parameter) {
      if (functionStack.isEmpty()) {
        throw new IllegalStateException("There cannot be parameters outside of functions");
      }
      return requireNonNull(functionStack.peek().arguments.get(parameter.name.name));
    }

    if (origin instanceof EnumerationDefinition.Entry entry) {
      return eval(Objects.requireNonNull(entry.value));
    }


    throw new EvaluationError(
        "Cannot evaluate identifier with origin of %s".formatted(
            Objects.requireNonNull(origin).getClass().getName()), expr);
  }

  private ConstantValue concat(List<ConstantValue> values) {
    var type = (DataType) values.getFirst().type();
    var value = type.isSigned()
        ? BigIntUtils.twosComplement(values.getFirst().value(), type.bitWidth())
        : values.getFirst().value();

    for (var i = 1; i < values.size(); i++) {
      var current = values.get(i);
      var isSigned = current.type().asDataType().isSigned();
      var currentBitWidth = ((DataType) current.type()).bitWidth();
      value = value
          .shiftLeft(((DataType) current.type()).bitWidth())
          // Change to two-compliment
          .or(isSigned
              ? BigIntUtils.twosComplement(current.value(), currentBitWidth)
              : current.value());
      type = Type.bits(type.bitWidth() + currentBitWidth);
    }
    return new ConstantValue(value, type);
  }

  private ConstantValue slice(ConstantValue target, Constant.BitSlice slice, DataType resultType) {
    var parts = slice.parts().map(part ->
            new ConstantValue(
                target.value()
                    // First we do the modulo to remove the left side we don't need.
                    .mod(BigInteger.ONE.shiftLeft(part.msb() + 1))
                    // Next shift right to remove the right part
                    .shiftRight(part.lsb()),
                Type.bits(part.size())
            )
        )
        .toList();

    var value = concat(parts).value();
    return new ConstantValue(
        BigIntUtils.fromTwosComplement(value, resultType.bitWidth(), resultType.isSigned()),
        resultType);
  }

  @Override
  public ConstantValue visit(Identifier expr) {
    return visitIdentifiable(expr);
  }

  private static final Map<BuiltInTable.BuiltIn, BinaryOperator<BigInteger>> BinOpFuncs =
      new HashMap<>();

  static {
    // FIXME: Fill in the missing functions
    // Logical operations (bitwise)
    BinOpFuncs.put(BuiltInTable.OR, BigInteger::or);
    BinOpFuncs.put(BuiltInTable.XOR, BigInteger::xor);
    BinOpFuncs.put(BuiltInTable.AND, BigInteger::and);

    // Comparison operations
    BinOpFuncs.put(BuiltInTable.EQU, (a, b) -> a.equals(b) ? BigInteger.ONE : BigInteger.ZERO);
    BinOpFuncs.put(BuiltInTable.NEQ, (a, b) -> !a.equals(b) ? BigInteger.ONE : BigInteger.ZERO);
    BinOpFuncs.put(BuiltInTable.SGEQ,
        (a, b) -> a.compareTo(b) >= 0 ? BigInteger.ONE : BigInteger.ZERO);
    BinOpFuncs.put(BuiltInTable.UGEQ,
        (a, b) -> a.compareTo(b) >= 0 ? BigInteger.ONE : BigInteger.ZERO);
    BinOpFuncs.put(BuiltInTable.SGTH,
        (a, b) -> a.compareTo(b) > 0 ? BigInteger.ONE : BigInteger.ZERO);
    BinOpFuncs.put(BuiltInTable.UGTH,
        (a, b) -> a.compareTo(b) > 0 ? BigInteger.ONE : BigInteger.ZERO);
    BinOpFuncs.put(BuiltInTable.SLEQ,
        (a, b) -> a.compareTo(b) <= 0 ? BigInteger.ONE : BigInteger.ZERO);
    BinOpFuncs.put(BuiltInTable.ULEQ,
        (a, b) -> a.compareTo(b) <= 0 ? BigInteger.ONE : BigInteger.ZERO);
    BinOpFuncs.put(BuiltInTable.SLTH,
        (a, b) -> a.compareTo(b) < 0 ? BigInteger.ONE : BigInteger.ZERO);
    BinOpFuncs.put(BuiltInTable.ULTH,
        (a, b) -> a.compareTo(b) < 0 ? BigInteger.ONE : BigInteger.ZERO);

    // Rotation (TODO: implement proper rotation logic)
    //BinOpFuncs.put(BuiltInTable.ROR, BigInteger::add);
    //BinOpFuncs.put(BuiltInTable.ROL, BigInteger::add);

    // Shifting
    BinOpFuncs.put(BuiltInTable.LSL, (a, b) -> a.shiftLeft(b.intValueExact()));
    BinOpFuncs.put(BuiltInTable.LSR, (a, b) -> a.shiftRight(b.intValueExact()));
    BinOpFuncs.put(BuiltInTable.ASR, (a, b) -> a.shiftRight(b.intValueExact()));

    // Arithmetic
    BinOpFuncs.put(BuiltInTable.ADD, BigInteger::add);
    BinOpFuncs.put(BuiltInTable.SUB, BigInteger::subtract);
    // TODO: implement saturating arithmetic
    //BinOpFuncs.put(BuiltInTable.SSATADD, BigInteger::add);
    //BinOpFuncs.put(BuiltInTable.USATADD, BigInteger::add);
    //BinOpFuncs.put(BuiltInTable.SSATSUB, BigInteger::subtract);
    //BinOpFuncs.put(BuiltInTable.USATSUB, BigInteger::subtract);
    BinOpFuncs.put(BuiltInTable.MUL, BigInteger::multiply);
    BinOpFuncs.put(BuiltInTable.SDIV, BigInteger::divide);
    BinOpFuncs.put(BuiltInTable.UDIV, BigInteger::divide);
    BinOpFuncs.put(BuiltInTable.SMOD, BigInteger::mod);
    BinOpFuncs.put(BuiltInTable.UMOD, BigInteger::mod);
    BinOpFuncs.put(BuiltInTable.SMULL, BigInteger::multiply);
    BinOpFuncs.put(BuiltInTable.UMULL, BigInteger::multiply);
    BinOpFuncs.put(BuiltInTable.SUMULL, BigInteger::multiply);
  }

  @Override
  public ConstantValue visit(BinaryExpr expr) {
    var builtin =
        AstUtils.getOperatorBuiltIn(expr.operator(), List.of(expr.left.type(), expr.right.type()));
    return evalBuiltin(builtin, List.of(eval(expr.left), eval(expr.right)), expr);
  }

  @Override
  public ConstantValue visit(GroupedExpr expr) {
    if (expr.expressions.size() == 1) {
      return eval(expr.expressions.getFirst());
    }

    if (expr.expressions.stream().anyMatch(e -> e.type() instanceof StringType)) {
      throw new EvaluationError("Cannot evaluate strings.", expr);
    }

    var args = expr.expressions.stream()
        .map(this::eval)
        .toList();
    return concat(args);
  }

  @Override
  public ConstantValue visit(IntegerLiteral expr) {
    return new ConstantValue(expr.number, new ConstantType(expr.number));
  }

  @Override
  public ConstantValue visit(WildcardLiteral expr) {
    throw new EvaluationError(
        "The constant evaluator cannot evaluate %s.".formatted(expr.getClass().getSimpleName()),
        expr);
  }

  @Override
  public ConstantValue visit(BinaryLiteral expr) {
    return new ConstantValue(expr.number, Type.bits(expr.bitWidth));
  }

  @Override
  public ConstantValue visit(BoolLiteral expr) {
    return new ConstantValue(BigInteger.valueOf(expr.value ? 1 : 0), Type.bool());
  }

  @Override
  public ConstantValue visit(StringLiteral expr) {
    throw new EvaluationError("Cannot evaluate strings (yet).", expr);
  }

  @Override
  public ConstantValue visit(PlaceholderExpr expr) {
    throw new IllegalStateException(
        "The constant evaluator should never see a %s".formatted(expr.getClass().getSimpleName()));
  }

  @Override
  public ConstantValue visit(MacroInstanceExpr expr) {
    throw new IllegalStateException(
        "The constant evaluator should never see a %s".formatted(expr.getClass().getSimpleName()));
  }

  @Override
  public ConstantValue visit(RangeExpr expr) {
    throw new EvaluationError(
        "The constant evaluator cannot evaluate %s.".formatted(expr.getClass().getSimpleName()),
        expr);
  }

  @Override
  public ConstantValue visit(TypeLiteral expr) {
    throw new EvaluationError("Cannot evaluate %s.".formatted(expr.getClass().getSimpleName()),
        expr);
  }

  @Override
  public ConstantValue visit(IdentifierPath expr) {
    return visitIdentifiable(expr);
  }

  @Override
  public ConstantValue visit(UnaryExpr expr) {
    var inner = eval(expr.operand);
    return evalBuiltin(requireNonNull(expr.computedTarget), List.of(inner), expr);
  }

  @Override
  public ConstantValue visit(CallIndexExpr expr) {

    List<Expr> args =
        !expr.args().isEmpty() ? expr.args().stream().flatMap(a -> a.values.stream()).toList() :
            new ArrayList<>();
    var argTypes = args.stream().map(Expr::type).toList();

    @Nullable ConstantValue result = null;

    // Bultin Functions
    var builtin = AstUtils.getBuiltIn(expr.target.path().pathToString(), argTypes);
    // FIXME: Validate the implementation
    if (builtin != null) {
      // FIXME: verify no subcalls or slicing here
      if (expr.argsIndices.size() != 1 || !expr.subCalls.isEmpty()) {
        throw new EvaluationError(
            "The constant evaluator cannot handle subcalls or indexing/slicing", expr);
      }
      result = evalBuiltin(builtin, args.stream().map(this::eval).toList(), expr);
    }

    // User Defined Functions
    var computedTarget = expr.target.path().target();
    if (computedTarget instanceof FunctionDefinition functionDef) {
      var arguments = new HashMap<String, ConstantValue>();
      for (var i = 0; i < expr.args().size(); i++) {
        arguments.put(functionDef.params.get(i).name.name, eval(expr.args().get(i).values.get(0)));
      }
      functionStack.push(new FunctionFrame(functionDef, arguments));
      try {
        result = eval(functionDef.expr);
      } finally {
        functionStack.pop();
      }
    }

    // If the expr is just a slice, let's load it.
    if (result == null) {
      result = eval((Expr) expr.target);
    }

    // Slicing/Indexing
    for (var sliceArg : expr.slices()) {
      result = slice(result, requireNonNull(sliceArg.computedstaticBitSlice),
          (DataType) sliceArg.type());
    }

    return result;
  }

  @Override
  public ConstantValue visit(IfExpr expr) {
    var valCondition = eval(expr.condition);
    if (!valCondition.value().equals(BigInteger.ZERO)) {
      return eval(expr.thenExpr);
    } else {
      return eval(expr.elseExpr);
    }
  }

  @Override
  public ConstantValue visit(LetExpr expr) {
    // No need to setup anything the variable will be evaluated when the identifier is visited.
    return eval(expr.body);
  }

  @Override
  public ConstantValue visit(CastExpr expr) {
    var innerVal = expr.value.accept(this);

    var targetType = Objects.requireNonNull(expr.type);
    if (targetType instanceof TensorType tensorType) {
      targetType = tensorType.flattenBitsType();
    }

    var viamVal = innerVal.toViamConstant()
        .castTo((DataType) targetType);
    return ConstantValue.fromViam(viamVal);
  }

  @Override
  public ConstantValue visit(SymbolExpr expr) {
    throw new EvaluationError(
        "The constant evaluator cannot evaluate %s.".formatted(expr.getClass().getSimpleName()),
        expr);

  }

  @Override
  public ConstantValue visit(MacroMatchExpr expr) {
    throw new IllegalStateException(
        "The constant evaluator should never see a %s".formatted(expr.getClass().getSimpleName()));
  }

  @Override
  public ConstantValue visit(MatchExpr expr) {
    var candidateVal = eval(expr.candidate);

    for (var kase : expr.cases) {
      for (var pattern : kase.patterns) {
        var pattenrVal = eval(pattern);
        if (candidateVal.equals(pattenrVal)) {
          return eval(kase.result);
        }
      }
    }

    return eval(expr.defaultResult);
  }

  @Override
  public ConstantValue visit(AsIdExpr expr) {
    throw new IllegalStateException(
        "The constant evaluator should never see a %s".formatted(expr.getClass().getSimpleName()));
  }

  @Override
  public ConstantValue visit(AsStrExpr expr) {
    throw new IllegalStateException(
        "The constant evaluator should never see a %s".formatted(expr.getClass().getSimpleName()));
  }

  @Override
  public ConstantValue visit(ExistsInExpr expr) {
    throw new EvaluationError(
        "The constant evaluator cannot evaluate %s.".formatted(expr.getClass().getSimpleName()),
        expr);

  }

  @Override
  public ConstantValue visit(ExistsInThenExpr expr) {
    throw new EvaluationError(
        "The constant evaluator cannot evaluate %s.".formatted(expr.getClass().getSimpleName()),
        expr);

  }


  @Override
  public ConstantValue visit(ForallExpr expr) {
    throw new EvaluationError(
        "The constant evaluator cannot evaluate %s.".formatted(expr.getClass().getSimpleName()),
        expr);

  }

  @Override
  public ConstantValue visit(SequenceCallExpr expr) {
    throw new EvaluationError(
        "The constant evaluator cannot evaluate %s.".formatted(expr.getClass().getSimpleName()),
        expr);

  }

  @Override
  public ConstantValue visit(ExpandedSequenceCallExpr expr) {
    throw new EvaluationError(
        "The constant evaluator cannot evaluate %s.".formatted(expr.getClass().getSimpleName()),
        expr);
  }

  @Override
  public ConstantValue visit(ExpandedAliasDefSequenceCallExpr expr) {
    throw new EvaluationError(
        "The constant evaluator cannot evaluate %s.".formatted(expr.getClass().getSimpleName()),
        expr);
  }

  @Override
  public ConstantValue visit(ResourceReferenceExression expr) {
    // This will never be evaluated
    throw new RuntimeException(
        "Constant evaluator cannot evaluate %s yet.".formatted(expr.getClass().getSimpleName()));
  }

  record FunctionFrame(FunctionDefinition functionDef, HashMap<String, ConstantValue> arguments) {}
}

/**
 * This class is quite similar to VIAM's Constant.Value but unfortunatley that cannot handle values
 * of types which bit widths aren't known, as it stores negative values 2-compliment encoded.
 *
 * <p>The ConstantValue instead stores the sign bit just as a native BigInteger would.
 */
record ConstantValue(BigInteger value, Type type) {


  static ConstantValue fromViam(Constant.Value v) {
    if (v.type() instanceof BitsType) {
      return new ConstantValue(v.integer(), v.type());
    } else if (v.type() instanceof BoolType) {
      return new ConstantValue(v.integer(), v.type());
    }

    throw new IllegalArgumentException();
  }

  ConstantValue withValue(BigInteger value) {
    var type = this.type;
    if (this.type instanceof ConstantType) {
      type = new ConstantType(value);
    }

    return new ConstantValue(value, type);
  }

  public Constant.Value toViamConstant() {
    if (this.type instanceof ConstantType) {
      var isNegative = value.compareTo(BigInteger.ZERO) < 0;
      var bitWidth = Math.max(value.bitLength(), 1) + (isNegative ? 1 : 0);
      var closestType = isNegative ? Type.signedInt(bitWidth) : Type.unsignedInt(bitWidth);
      try {
        return Constant.Value.fromInteger(value, closestType);
      } catch (Exception e) {
        throw e;
      }
    }

    if (this.type instanceof TensorType tensorType) {
      return Constant.Value.fromInteger(value, tensorType.flattenBitsType());
    }

    if (this.type instanceof DataType dataType) {
      return Constant.Value.fromInteger(value, dataType);
    }

    throw new IllegalStateException(
        "Constant evaluator cannot convert type %s yet.".formatted(this.type));
  }
}

class EvaluationError extends RuntimeException {

  SourceLocation location;

  public EvaluationError(String message, WithLocation location) {
    super(message);
    this.location = location.location();
  }
}
