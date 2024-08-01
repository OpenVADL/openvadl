package vadl.ast;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import vadl.error.VadlError;
import vadl.error.VadlException;

/**
 * Expands and copies a macro template.
 */
class MacroExpander
    implements ExprVisitor<Expr>, DefinitionVisitor<Definition>, StatementVisitor<Statement> {
  final Map<String, Node> args;
  NestedSymbolTable symbols;
  List<VadlError> errors = new ArrayList<>();

  MacroExpander(Map<String, Node> args, NestedSymbolTable symbols) {
    this.args = args;
    this.symbols = symbols;
  }

  public Expr expandExpr(Expr expr) {
    var result = expr.accept(this);
    if (!errors.isEmpty()) {
      throw new VadlException(errors);
    }
    return result;
  }

  public Definition expandDefinition(Definition def) {
    var result = def.accept(this);
    if (!errors.isEmpty()) {
      throw new VadlException(errors);
    }
    return result;
  }

  @Override
  public Expr visit(BinaryExpr expr) {
    // FIXME: Only if parent is not a binary operator cause otherwise it is O(n^2)
    var result = new BinaryExpr(
        expr.left.accept(this), expr.operator, expr.right.accept(this));
    return BinaryExpr.reorder(result);
  }

  @Override
  public Expr visit(GroupExpr expr) {
    return new GroupExpr(expr.accept(this));
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
  public Expr visit(PlaceholderExpr expr) {
    // FIXME: This could also be another macro
    var arg = args.get(expr.identifierChain.identifier.name);
    if (arg == null) {
      throw new IllegalStateException("The parser should already have checked that.");
    } else if (arg instanceof Identifier id) {
      return new IdentifierChain(id, null);
    }

    return ((Expr) arg).accept(this);
  }

  @Override
  public Expr visit(MacroInstanceExpr expr) {
    var arg = args.get(expr.identifier.name);
    if (arg == null) {
      throw new IllegalStateException("The parser should already have checked that.");
    } else if (arg instanceof Identifier id) {
      return new IdentifierChain(id, null);
    }

    return ((Expr) arg).accept(this);
  }

  @Override
  public Expr visit(RangeExpr expr) {
    return new RangeExpr(expr.from.accept(this), expr.to.accept(this));
  }

  @Override
  public Expr visit(TypeLiteral expr) {
    var sizeExpression = expr.sizeExpression == null ? null : expr.sizeExpression.accept(this);
    return new TypeLiteral(expr.baseType, sizeExpression, expr.loc);
  }

  @Override
  public Expr visit(IdentifierChain expr) {
    symbols.requireValue(expr);
    return new IdentifierChain(expr.identifier, expr.next);
  }

  @Override
  public Expr visit(UnaryExpr expr) {
    return new UnaryExpr(expr.operator, expr.operand.accept(this));
  }

  @Override
  public Expr visit(CallExpr expr) {
    expr.target = (SymbolExpr) expr.target.accept(this);
    var invocations = expr.invocations;
    expr.invocations = new ArrayList<>(invocations.size());
    for (var invocation : invocations) {
      var args = new ArrayList<Expr>(invocation.size());
      for (var arg : invocation) {
        args.add(arg.accept(this));
      }
      expr.invocations.add(args);
    }
    return expr;
  }

  @Override
  public Expr visit(IfExpr expr) {
    return new IfExpr(
        expr.condition.accept(this),
        expr.thenExpr.accept(this),
        expr.elseExpr.accept(this),
        expr.location
    );
  }

  @Override
  public Expr visit(LetExpr expr) {
    return new LetExpr(
        expr.identifier,
        expr.valueExpr.accept(this),
        expr.body.accept(this),
        expr.location
    );
  }

  @Override
  public Expr visit(CastExpr expr) {
    expr.value = expr.value.accept(this);
    expr.type = (TypeLiteral) expr.type.accept(this);
    return expr;
  }

  @Override
  public Expr visit(SymbolExpr expr) {
    expr.address = expr.address == null ? null : expr.address.accept(this);
    return expr;
  }

  @Override
  public Definition visit(ConstantDefinition definition) {
    return new ConstantDefinition(definition.identifier, definition.typeAnnotation,
        definition.value.accept(this), definition.loc);
  }

  @Override
  public Definition visit(FormatDefinition definition) {
    return new FormatDefinition(definition.identifier, definition.typeAnnotation, definition.fields,
        definition.loc);
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
    definition.identifier = resolvePlaceholderOrIdentifier(definition.identifier);
    var typeId = resolvePlaceholderOrIdentifier(definition.typeIdentifier);
    definition.typeIdentifier = typeId;

    symbols = symbols.createFormatScope(typeId);
    definition.behavior = visit(definition.behavior);
    symbols = Objects.requireNonNull(symbols.parent);

    return definition;
  }

  @Override
  public Definition visit(EncodingDefinition definition) {
    return definition;
  }

  @Override
  public Definition visit(AssemblyDefinition definition) {
    return definition;
  }

  @Override
  public BlockStatement visit(BlockStatement blockStatement) {
    symbols = symbols.createChild();
    blockStatement.statements = blockStatement.statements.stream()
        .map(s -> s.accept(this))
        .toList();
    symbols = Objects.requireNonNull(symbols.parent);
    return blockStatement;
  }

  @Override
  public Statement visit(LetStatement letStatement) {
    symbols = symbols.createChild();
    symbols.defineConstant(letStatement.identifier.name, letStatement.identifier.loc);
    letStatement.valueExpression = letStatement.valueExpression.accept(this);
    letStatement.body = letStatement.body.accept(this);
    symbols = Objects.requireNonNull(symbols.parent);
    return letStatement;
  }

  @Override
  public Statement visit(IfStatement ifStatement) {
    ifStatement.condition = ifStatement.condition.accept(this);
    ifStatement.thenStmt = ifStatement.thenStmt.accept(this);
    if (ifStatement.elseStmt != null) {
      ifStatement.elseStmt = ifStatement.elseStmt.accept(this);
    }
    return ifStatement;
  }

  @Override
  public Statement visit(AssignmentStatement assignmentStatement) {
    if (assignmentStatement.target instanceof IdentifierChain chain) {
      symbols.requireValue(chain);
    }
    assignmentStatement.valueExpression = assignmentStatement.valueExpression.accept(this);
    return assignmentStatement;
  }

  private Identifier resolvePlaceholderOrIdentifier(Node n) {
    if (n instanceof PlaceholderExpr p) {
      return ((IdentifierChain) p.accept(this)).identifier;
    }
    if (n instanceof Identifier id) {
      return id;
    }
    throw new IllegalStateException("Unknown resolved placeholder type " + n);
  }
}
