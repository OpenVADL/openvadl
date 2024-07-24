package vadl.ast;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import vadl.error.VadlError;
import vadl.error.VadlException;

/**
 * Expands and copies a macro template.
 */
class MacroExpander
    implements ExprVisitor<Node>, DefinitionVisitor<Node>, StatementVisitor<Statement> {
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
    return (Expr) result;
  }

  public Node expandDefinition(Definition def) {
    var result = def.accept(this);
    if (!errors.isEmpty()) {
      throw new VadlException(errors);
    }
    return result;
  }

  @Override
  public Expr visit(BinaryExpr expr) {
    // FIXME: Only if parent is not a binary operator cause otherwise it is O(n^2)
    var result = new BinaryExpr((Expr) expr.left.accept(this), expr.operator,
        (Expr) expr.right.accept(this));
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
  public Expr visit(IdentifierChain expr) {
    symbols.requireValue(expr);
    return new IdentifierChain(expr.identifier, expr.next);
  }

  @Override
  public Expr visit(UnaryExpr expr) {
    return new UnaryExpr(expr.operator, (Expr) expr.operand.accept(this));
  }

  @Override
  public Node visit(CallExpr expr) {
    return new CallExpr(expr.identifier, (Expr) expr.argument.accept(this));
  }

  @Override
  public Node visit(IfExpr expr) {
    return new IfExpr(
        (Expr) expr.condition.accept(this),
        (Expr) expr.thenExpr.accept(this),
        (Expr) expr.elseExpr.accept(this),
        expr.location
    );
  }

  @Override
  public Node visit(LetExpr expr) {
    return new LetExpr(
        expr.identifier,
        (Expr) expr.valueExpr.accept(this),
        (Expr) expr.body.accept(this),
        expr.location
    );
  }

  @Override
  public Definition visit(ConstantDefinition definition) {
    return new ConstantDefinition(definition.identifier, definition.typeAnnotation,
        (Expr) definition.value.accept(this), definition.loc);
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
    var identifier = resolvePlaceholderOrIdentifier(definition.identifier);
    var typeId = resolvePlaceholderOrIdentifier(definition.typeIdentifier);
    symbols = symbols.createFormatScope(typeId);
    var result = new InstructionDefinition(
        identifier,
        typeId,
        visit(definition.behavior),
        definition.loc
    );
    symbols = Objects.requireNonNull(symbols.parent);
    return result;
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
    var result = new BlockStatement(
        blockStatement.statements().stream().map(s -> s.accept(this)).toList()
    );
    symbols = Objects.requireNonNull(symbols.parent);
    return result;
  }

  @Override
  public Statement visit(LetStatement letStatement) {
    symbols = symbols.createChild();
    symbols.defineConstant(letStatement.identifier().name, letStatement.identifier().loc);
    var result = new LetStatement(
        letStatement.identifier(),
        (Expr) letStatement.valueExpression().accept(this),
        letStatement.body().accept(this)
    );
    symbols = Objects.requireNonNull(symbols.parent);
    return result;
  }

  @Override
  public Statement visit(IfStatement ifStatement) {
    return new IfStatement(
        (Expr) ifStatement.condition().accept(this),
        ifStatement.thenStmt().accept(this),
        Optional.ofNullable(ifStatement.elseStmt()).map(s -> s.accept(this)).orElse(null)
    );
  }

  @Override
  public Statement visit(AssignmentStatement assignmentStatement) {
    if (assignmentStatement.target() instanceof IdentifierChain chain) {
      symbols.requireValue(chain);
    }
    return new AssignmentStatement(
        assignmentStatement.target(),
        (Expr) assignmentStatement.valueExpression().accept(this)
    );
  }

  private Identifier resolvePlaceholderOrIdentifier(Node n) {
    if (n instanceof PlaceHolderExpr p) {
      return (Identifier) p.accept(this);
    }
    return (Identifier) n;
  }
}
