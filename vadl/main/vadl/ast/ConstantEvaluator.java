package vadl.ast;

import java.math.BigInteger;
import java.util.List;
import java.util.Objects;
import vadl.types.BitsType;
import vadl.types.BoolType;
import vadl.types.BuiltInTable;
import vadl.types.DataType;
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

  @Override
  public ConstantValue visit(BinaryExpr expr) {
    throw new RuntimeException(
        "Constant evaluator cannot evaluate %s yet.".formatted(expr.getClass().getSimpleName()));
  }

  @Override
  public ConstantValue visit(GroupedExpr expr) {
    if (expr.expressions.size() != 1) {
      // FIXME: What does a `(1, 2, 3) even mean, is'nt that a tuple?
      throw new RuntimeException("Research what to do in that case");
    }
    return expr.expressions.get(0).accept(this);
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