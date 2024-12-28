package vadl.ast;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BinaryOperator;
import vadl.types.BitsType;
import vadl.types.BoolType;
import vadl.types.BuiltInTable;
import vadl.types.DataType;
import vadl.types.SIntType;
import vadl.types.Type;
import vadl.viam.Constant;


/**
 * Can evaluate any expression.
 *
 * <p>The evaluator can only be called on expressions that are already typechecked
 * (including all of its sub-expressions).
 */
class ConstantEvaluator implements ExprVisitor<ConstantValue> {


  public ConstantValue eval(Expr expr) {
    // A simple optimization that avoids unneeded traversing the tree.
    if (expr.type instanceof ConstantType) {
      return new ConstantValue(((ConstantType) expr.type).getValue(), expr.type);
    }

    return expr.accept(this);
  }

  @Override
  public ConstantValue visit(Identifier expr) {
    var origin =
        Objects.requireNonNull(Objects.requireNonNull(expr.symbolTable).resolveNode(expr.name));

    if (origin instanceof ConstantDefinition constantDefinition) {
      return eval(constantDefinition.value);
    }

    throw new RuntimeException(
        "Constant evaluator cannot evaluate identifier with origin of %s yet.".formatted(
            expr.getClass().getSimpleName()));
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
    //BinOpFuncs.put(Operator.LongMultiply, BigInteger::add);
    //BinOpFuncs.put(Operator.In, BigInteger::add);
    //BinOpFuncs.put(Operator.NotIn, BigInteger::add);
    //BinOpFuncs.put(Operator.ElementOf, BigInteger::add);
    //BinOpFuncs.put(Operator.NotElementOf, BigInteger::add);
  }

  @Override
  public ConstantValue visit(BinaryExpr expr) {
    var leftVal = eval(expr.left);
    var rightVal = eval(expr.right);

    // Logical operations and constant types are handled directly
    if (Operator.logicalComparisions.contains(expr.operator()) ||
        (leftVal.type() instanceof ConstantType && rightVal.type() instanceof ConstantType)) {

      var func = Objects.requireNonNull(BinOpFuncs.get(expr.operator()));
      var val = func.apply(leftVal.value(), rightVal.value());
      var type = Operator.arithmeticOperators.contains(expr.operator()) ? new ConstantType(val) :
          Type.bool();

      return new ConstantValue(val, type);
    }

    var builtIns = BuiltInTable.builtIns()
        .filter(b -> Objects.equals(b.operator(), expr.operator().symbol))
        .toList();

    // Sometimes there are a singed and unsigned version of builtin operation
    var builtIn = switch (builtIns.size()) {
      case 0 -> throw new IllegalStateException(
          "Couldn't get any matching builtin for %s".formatted(expr.operator));
      case 1 -> builtIns.get(0);
      case 2 -> {
        var singed = leftVal.type().getClass() == SIntType.class;
        builtIns = builtIns.stream()
            .filter(b -> (b.signature().argTypeClasses().get(0) == SIntType.class) == singed)
            .toList();
        if (builtIns.size() != 1) {
          throw new IllegalStateException("Couldn't find a builtin function");
        }
        yield builtIns.get(0);
      }
      default -> throw new IllegalStateException(
          "Too many matching builtin (%d) for %s".formatted(builtIns.size(), expr.operator));
    };
    System.out.println(builtIn.name());

    var val = builtIn
        .compute(List.of(leftVal.toViamConstant(), rightVal.toViamConstant()))
        .orElseThrow();

    Type type;
    if (Operator.arithmeticOperators.contains(expr.operator())) {
      type = leftVal.type();
    } else if (Operator.artihmeticComparisons.contains(expr.operator())) {
      type = Type.bool();
    } else {
      // Just throw so that we now we need to implement something, should never happen if we are done.
      throw new IllegalStateException("Cannot find result type for operator " + expr.operator());
    }

    return new ConstantValue(val.asVal().integer(), type);
  }

  @Override
  public ConstantValue visit(GroupedExpr expr) {
    if (expr.expressions.size() != 1) {
      // FIXME: What does a `(1, 2, 3) even mean, is'nt that a tuple?
      throw new RuntimeException("Research what to do in that case");
    }
    return eval(expr.expressions.get(0));
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
    throw new RuntimeException(
        "Constant evaluator cannot evaluate %s yet.".formatted(expr.getClass().getSimpleName()));
  }

  @Override
  public ConstantValue visit(MacroInstanceExpr expr) {
    throw new RuntimeException(
        "Constant evaluator cannot evaluate %s yet.".formatted(expr.getClass().getSimpleName()));

  }

  @Override
  public ConstantValue visit(RangeExpr expr) {
    throw new RuntimeException(
        "Constant evaluator cannot evaluate %s yet.".formatted(expr.getClass().getSimpleName()));
  }

  @Override
  public ConstantValue visit(TypeLiteral expr) {
    throw new RuntimeException(
        "Constant evaluator cannot evaluate %s yet.".formatted(expr.getClass().getSimpleName()));
  }

  @Override
  public ConstantValue visit(IdentifierPath expr) {
    throw new RuntimeException(
        "Constant evaluator cannot evaluate %s yet.".formatted(expr.getClass().getSimpleName()));
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

    // Concrete types (with fixed bit width) are evaluated with the builtin functions.
    var computeFunc = switch (expr.unOp().operator) {
      case NEGATIVE -> BuiltInTable.NEG;
      case COMPLEMENT, LOG_NOT -> BuiltInTable.NOT;
    };

    return ConstantValue.fromViam(
        (
            (Constant.Value) computeFunc.compute(List.of(innerVal.toViamConstant())).get()
        ).castTo((DataType) innerVal.type())
    );
  }

  @Override
  public ConstantValue visit(CallExpr expr) {
    throw new RuntimeException(
        "Constant evaluator cannot evaluate %s yet.".formatted(expr.getClass().getSimpleName()));

  }

  @Override
  public ConstantValue visit(IfExpr expr) {
    throw new RuntimeException(
        "Constant evaluator cannot evaluate %s yet.".formatted(expr.getClass().getSimpleName()));

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
    throw new RuntimeException(
        "Constant evaluator cannot evaluate %s yet.".formatted(expr.getClass().getSimpleName()));
  }

  @Override
  public ConstantValue visit(MatchExpr expr) {
    throw new RuntimeException(
        "Constant evaluator cannot evaluate %s yet.".formatted(expr.getClass().getSimpleName()));
  }

  @Override
  public ConstantValue visit(ExtendIdExpr expr) {
    throw new RuntimeException(
        "Constant evaluator cannot evaluate %s yet.".formatted(expr.getClass().getSimpleName()));
  }

  @Override
  public ConstantValue visit(IdToStrExpr expr) {
    throw new RuntimeException(
        "Constant evaluator cannot evaluate %s yet.".formatted(expr.getClass().getSimpleName()));
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
      var isNegative = value.compareTo(BigInteger.ZERO) >= 0;
      var bitWidth = value.bitLength() + (isNegative ? 1 : 0);
      var closestType = isNegative ? Type.unsignedInt(bitWidth) : Type.signedInt(bitWidth);
      return Constant.Value.fromInteger(value, closestType);
    }

    if (this.type instanceof DataType dataType) {
      return Constant.Value.fromInteger(value, dataType);
    }

    throw new IllegalStateException(
        "Constant evaluator cannot convert type %s yet.".formatted(this.type));
  }
}