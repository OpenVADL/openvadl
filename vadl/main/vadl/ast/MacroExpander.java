package vadl.ast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import vadl.error.VadlError;
import vadl.error.VadlException;

/**
 * Expands and copies a macro template.
 */
class MacroExpander implements ExprVisitor<Node>, DefinitionVisitor<Node> {
  Map<String, Node> args = new HashMap<>();
  List<VadlError> errors = new ArrayList<>();

  public Expr expandExpr(Expr expr, Map<String, Node> args) {
    this.args = args;
    var result = expr.accept(this);
    if (!errors.isEmpty()) {
      throw new VadlException(errors);
    }
    return (Expr) result;
  }

  public Node expandDefinition(Definition def, Map<String, Node> args) {
    this.args = args;
    var result = def.accept(this);
    if (!errors.isEmpty()) {
      throw new VadlException(errors);
    }
    return result;
  }

  @Override
  public Expr visit(BinaryExpr expr) {
    // FIXME: Only if parent is not a binary operator cause otherwise it is O(n^2)
    var result = new BinaryExpr((Expr) expr.left.accept(this), expr.operator, (Expr) expr.right.accept(this));
    return BinaryExpr.reorder(result);
  }

  @Override
  public Expr visit(GroupExpr expr) {
    return new GroupExpr((Expr) expr.accept(this));
  }

  @Override
  public Expr visit(IntegerLiteral expr) {
    return new IntegerLiteral(expr.token, expr.loc);
  }

  @Override
  public Expr visit(StringLiteral expr) {
    return new StringLiteral(expr.value, expr.loc);
  }

  @Override
  public Node visit(PlaceHolderExpr expr) {
    // FIXME: This could also be another macro
    var arg = args.get(expr.identifier.name);
    if (arg == null) {
      throw new IllegalStateException("The parser should already have checked that.");
    } else if (arg instanceof Identifier id) {
      return id;
    }

    return ((Expr) arg).accept(this);
  }

  @Override
  public Expr visit(RangeExpr expr) {
    return new RangeExpr((Expr) expr.from.accept(this), (Expr) expr.to.accept(this));
  }

  @Override
  public Expr visit(TypeLiteral expr) {
    var sizeExpression = expr.sizeExpression == null ? null : expr.sizeExpression.accept(this);
    return new TypeLiteral(expr.baseType, (Expr) sizeExpression, expr.loc);
  }

  @Override
  public Expr visit(VariableAccess expr) {
    return new VariableAccess(expr.identifier, expr.next);
  }

  @Override
  public Expr visit(UnaryExpr expr) {
    return new UnaryExpr(expr.operator, (Expr) expr.operand.accept(this));
  }

  @Override
  public Definition visit(ConstantDefinition definition) {
    return new ConstantDefinition(definition.identifier, definition.typeAnnotation, (Expr) definition.value.accept(this), definition.loc);
  }

  @Override
  public Definition visit(FormatDefinition definition) {
    return new FormatDefinition(definition.identifier, definition.typeAnnotation, definition.fields, definition.loc);
  }

  @Override
  public Definition visit(InstructionSetDefinition definition) {
    return definition;
  }

  @Override
  public Definition visit(CounterDefinition definition) {
    return definition;
  }

  @Override
  public Definition visit(MemoryDefinition definition) {
    return definition;
  }

  @Override
  public Definition visit(RegisterDefinition definition) {
    return definition;
  }

  @Override
  public Definition visit(RegisterFileDefinition definition) {
    return definition;
  }

  @Override
  public Definition visit(InstructionDefinition definition) {
    return new InstructionDefinition(
        definition.identifier instanceof PlaceHolderExpr p ? p.accept(this) : definition.identifier,
        definition.typeIdentifier instanceof PlaceHolderExpr p ? p.accept(this) : definition.identifier,
        definition.behavior,
        definition.loc
    );
  }

  @Override
  public Definition visit(EncodingDefinition definition) {
    return definition;
  }

  @Override
  public Definition visit(AssemblyDefinition definition) {
    return definition;
  }
}
