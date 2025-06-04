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
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BinaryOperator;
import vadl.types.BitsType;
import vadl.types.BoolType;
import vadl.types.BuiltInTable;
import vadl.types.DataType;
import vadl.types.StringType;
import vadl.types.Type;
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

  private final IdentityHashMap<Expr, ConstantValue> cache = new IdentityHashMap<>();

  public ConstantValue eval(Expr expr) {
    // A simple optimization that avoids unneeded traversing the tree.
    if (expr.type instanceof ConstantType) {
      return new ConstantValue(((ConstantType) expr.type).getValue(), expr.type);
    }

    if (cache.containsKey(expr)) {
      return (ConstantValue) cache.get(expr);
    }

    var result = expr.accept(this);
    cache.put(expr, result);
    return result;
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

    if (origin instanceof EnumerationDefinition.Entry entry) {
      return eval(Objects.requireNonNull(entry.value));
    }

    throw new EvaluationError(
        "Cannot evaluate identifier with origin of %s yet, found in: %s.".formatted(
            Objects.requireNonNull(origin).getClass().getName(),
            expr.location().toIDEString()), expr);
  }

  @Override
  public ConstantValue visit(Identifier expr) {
    return visitIdentifiable(expr);
  }

  private static final Map<Operator, BinaryOperator<BigInteger>> BinOpFuncs = new HashMap<>();

  static {
    // FIXME: Fill in the missing functions
    BinOpFuncs.put(Operator.LogicalOr, BigInteger::or);
    BinOpFuncs.put(Operator.LogicalAnd, BigInteger::and);
    BinOpFuncs.put(Operator.Or, BigInteger::or);
    BinOpFuncs.put(Operator.Xor, BigInteger::xor);
    BinOpFuncs.put(Operator.And, BigInteger::and);
    BinOpFuncs.put(Operator.Equal, (a, b) -> a.equals(b) ? BigInteger.ONE : BigInteger.ZERO);
    BinOpFuncs.put(Operator.NotEqual, (a, b) -> !a.equals(b) ? BigInteger.ONE : BigInteger.ZERO);
    BinOpFuncs.put(Operator.GreaterEqual,
        (a, b) -> a.compareTo(b) >= 0 ? BigInteger.ONE : BigInteger.ZERO);
    BinOpFuncs.put(Operator.Greater,
        (a, b) -> a.compareTo(b) > 0 ? BigInteger.ONE : BigInteger.ZERO);
    BinOpFuncs.put(Operator.LessEqual,
        (a, b) -> a.compareTo(b) <= 0 ? BigInteger.ONE : BigInteger.ZERO);
    BinOpFuncs.put(Operator.Less,
        (a, b) -> a.compareTo(b) < 0 ? BigInteger.ONE : BigInteger.ZERO);
    //BinOpFuncs.put(Operator.RotateRight, BigInteger::add);
    //BinOpFuncs.put(Operator.RotateLeft, BigInteger::add);
    BinOpFuncs.put(Operator.ShiftLeft, (a, b) -> a.shiftLeft(b.intValueExact()));
    BinOpFuncs.put(Operator.ShiftRight, (a, b) -> a.shiftRight(b.intValueExact()));
    BinOpFuncs.put(Operator.Add, BigInteger::add);
    BinOpFuncs.put(Operator.Subtract, BigInteger::subtract);
    //BinOpFuncs.put(Operator.SaturatedAdd, BigInteger::add);
    //BinOpFuncs.put(Operator.SaturatedSubtract, BigInteger::add);
    BinOpFuncs.put(Operator.Multiply, BigInteger::multiply);
    BinOpFuncs.put(Operator.Divide, BigInteger::divide);
    BinOpFuncs.put(Operator.Modulo, BigInteger::mod);
    BinOpFuncs.put(Operator.LongMultiply, BigInteger::multiply);
    //BinOpFuncs.put(Operator.In, BigInteger::add);
    //BinOpFuncs.put(Operator.NotIn, BigInteger::add);
    //BinOpFuncs.put(Operator.ElementOf, BigInteger::add);
    //BinOpFuncs.put(Operator.NotElementOf, BigInteger::add);
  }

  @Override
  public ConstantValue visit(BinaryExpr expr) {
    var leftVal = eval(expr.left);
    var rightVal = eval(expr.right);

    // Logical operations 
    // And shifts with the left side constant
    // And all other operations with both side constant
    // ... are handled directly.
    if (Operator.logicalComparisions.contains(expr.operator())
        || (List.of(Operator.ShiftLeft, Operator.ShiftRight).contains(expr.operator())
        && leftVal.type() instanceof ConstantType)
        || (leftVal.type() instanceof ConstantType && rightVal.type() instanceof ConstantType)) {

      var func = Objects.requireNonNull(BinOpFuncs.get(expr.operator()));
      var val = func.apply(leftVal.value(), rightVal.value());
      var type = Operator.arithmeticOperators.contains(expr.operator()) ? new ConstantType(val) :
          Type.bool();

      return new ConstantValue(val, type);
    }


    var builtIn = AstUtils.getBinOpBuiltIn(expr);
    var val = builtIn
        .compute(List.of(leftVal.toViamConstant(), rightVal.toViamConstant()))
        .orElseThrow();

    Type type;
    if (Operator.arithmeticOperators.contains(expr.operator())) {
      type = leftVal.type();
    } else if (Operator.artihmeticComparisons.contains(expr.operator())) {
      type = Type.bool();
    } else {
      // Just throw so that we now we need to implement something, should never happen if we are
      // done.
      throw new IllegalStateException("Cannot find result type for operator " + expr.operator());
    }

    return new ConstantValue(val.asVal().integer(), type);
  }

  @Override
  public ConstantValue visit(GroupedExpr expr) {
    if (expr.expressions.size() == 1) {
      return eval(expr.expressions.get(0));
    }

    if (expr.expressions.stream().anyMatch(e -> e.type() instanceof StringType)) {
      throw new EvaluationError("Cannot evaluate strings.", expr);
    }

    List<Constant> args = expr.expressions.stream()
        .map(this::eval)
        .map(ConstantValue::toViamConstant)
        .map(c -> (Constant) c)
        .toList();
    var res = args.stream()
        .reduce((a, b) -> BuiltInTable.CONCATENATE_BITS.compute(List.of(a, b)).orElseThrow())
        .orElseThrow();
    return ConstantValue.fromViam((Constant.Value) res);

    //throw new RuntimeException("Research what to do in that case");
  }

  @Override
  public ConstantValue visit(IntegerLiteral expr) {
    return new ConstantValue(expr.number, new ConstantType(expr.number));
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
    throw new RuntimeException(
        "Constant evaluator cannot evaluate %s yet.".formatted(expr.getClass().getSimpleName()));
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
    throw new RuntimeException(
        "Constant evaluator cannot evaluate %s yet.".formatted(expr.getClass().getSimpleName()));
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
    var innerVal = eval(expr.operand);

    // Constant's (with variable bit width) types are evaluated directly.
    if (innerVal.type() instanceof ConstantType) {
      return switch (expr.unOp().operator) {
        case NEGATIVE -> innerVal.withValue(innerVal.value().negate());
        case COMPLEMENT -> innerVal.withValue(innerVal.value().not());
        case LOG_NOT -> innerVal.withValue(innerVal.value().xor(BigInteger.ONE));
      };
    }

    return ConstantValue.fromViam(
        (
            (Constant.Value) Objects.requireNonNull(expr.computedTarget)
                .compute(List.of(innerVal.toViamConstant())).get()
        ).castTo((DataType) innerVal.type())
    );
  }

  @Override
  public ConstantValue visit(CallIndexExpr expr) {

    List<Expr> args =
        !expr.argsIndices.isEmpty() ? expr.argsIndices.get(0).values : new ArrayList<>();
    var argTypes = args.stream().map(Expr::type).toList();
    var builtin = AstUtils.getBuiltIn(expr.target.path().pathToString(), argTypes);
    if (builtin != null) {
      // FIXME: verify no subcalls or slicing here
      if (expr.argsIndices.size() != 1 || !expr.subCalls.isEmpty()) {
        throw new EvaluationError(
            "The constant evaluator cannot handle subcalls or indexing/slicing", expr);
      }

      if (builtin.operator() != null && builtin.signature().argTypeClasses().size() == 1) {

        var fakeUnExpr = AstUtils.getBuiltinUnOp(expr, builtin);
        return eval(fakeUnExpr);
      }

      // If the function is also a binary operation, we instead type check it as if it were a binary
      // operation which has some special type rules.
      if (builtin.operator() != null && builtin.signature().argTypeClasses().size() == 2) {
        var fakeBinExpr = AstUtils.getBuiltinBinOp(expr, builtin);
        return eval(fakeBinExpr);
      }

      throw new RuntimeException(
          "At the moment the constant evaluator can only evaluate builtins that are also binary or"
              + " unary operators.");
    }


    // FIXME: Add functions

    throw new RuntimeException(
        "The constant evaluator cannot handle such calls");
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
    throw new RuntimeException(
        "Constant evaluator cannot evaluate %s yet.".formatted(expr.getClass().getSimpleName()));

  }

  @Override
  public ConstantValue visit(CastExpr expr) {
    var innerVal = expr.value.accept(this);

    var viamVal =
        innerVal.toViamConstant()
            .castTo((DataType) Objects.requireNonNull(expr.type));
    return ConstantValue.fromViam(viamVal);
  }

  @Override
  public ConstantValue visit(SymbolExpr expr) {
    throw new RuntimeException(
        "Constant evaluator cannot evaluate %s yet.".formatted(expr.getClass().getSimpleName()));

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
    throw new RuntimeException(
        "Constant evaluator cannot evaluate %s yet.".formatted(expr.getClass().getSimpleName()));

  }

  @Override
  public ConstantValue visit(ExistsInThenExpr expr) {
    throw new RuntimeException(
        "Constant evaluator cannot evaluate %s yet.".formatted(expr.getClass().getSimpleName()));

  }

  @Override
  public ConstantValue visit(ForallThenExpr expr) {
    throw new RuntimeException(
        "Constant evaluator cannot evaluate %s yet.".formatted(expr.getClass().getSimpleName()));

  }

  @Override
  public ConstantValue visit(ForallExpr expr) {
    throw new RuntimeException(
        "Constant evaluator cannot evaluate %s yet.".formatted(expr.getClass().getSimpleName()));

  }

  @Override
  public ConstantValue visit(SequenceCallExpr expr) {
    throw new RuntimeException(
        "Constant evaluator cannot evaluate %s yet.".formatted(expr.getClass().getSimpleName()));

  }

  @Override
  public ConstantValue visit(ExpandedSequenceCallExpr expr) {
    throw new RuntimeException(
        "Constant evaluator cannot evaluate %s yet.".formatted(expr.getClass().getSimpleName()));
  }

  @Override
  public ConstantValue visit(ExpandedAliasDefSequenceCallExpr expr) {
    throw new RuntimeException(
        "Constant evaluator cannot evaluate %s yet.".formatted(expr.getClass().getSimpleName()));
  }
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
        System.out.println();
        throw e;
      }
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
