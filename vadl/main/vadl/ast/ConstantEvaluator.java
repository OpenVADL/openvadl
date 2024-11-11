package vadl.ast;

import java.math.BigInteger;
import vadl.types.Type;

record ConstantValue(Type type, BigInteger value) {
}

class ConstantEvaluator implements ExprVisitor<ConstantValue> {

  private final TypeChecker typeChecker;

  public ConstantEvaluator(TypeChecker typeChecker) {
    this.typeChecker = typeChecker;
  }

  public ConstantValue eval(Expr expr) {
    return expr.accept(this);
  }

  @Override
  public ConstantValue visit(Identifier expr) {
    throw new RuntimeException(
        "Constant evaluator cannot evaluate %s yet.".formatted(expr.getClass().getSimpleName()));
  }

  @Override
  public ConstantValue visit(BinaryExpr expr) {
    throw new RuntimeException(
        "Constant evaluator cannot evaluate %s yet.".formatted(expr.getClass().getSimpleName()));
  }

  @Override
  public ConstantValue visit(GroupedExpr expr) {
    throw new RuntimeException(
        "Constant evaluator cannot evaluate %s yet.".formatted(expr.getClass().getSimpleName()));
  }

  @Override
  public ConstantValue visit(IntegerLiteral expr) {
    return new ConstantValue(new ConstantType(expr.number), expr.number);
  }

  @Override
  public ConstantValue visit(BinaryLiteral expr) {
    throw new RuntimeException(
        "Constant evaluator cannot evaluate %s yet.".formatted(expr.getClass().getSimpleName()));
  }

  @Override
  public ConstantValue visit(BoolLiteral expr) {
    return new ConstantValue(Type.bool(), BigInteger.valueOf(expr.value ? 1 : 0));
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
    throw new RuntimeException(
        "Constant evaluator cannot evaluate %s yet.".formatted(expr.getClass().getSimpleName()));

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
    throw new RuntimeException(
        "Constant evaluator cannot evaluate %s yet.".formatted(expr.getClass().getSimpleName()));

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
