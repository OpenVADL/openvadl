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
    // TODO Proper handling of placeholders with format "$a.b.c"
    var arg = args.get(expr.identifierPath.segments.get(0).name);
    if (arg == null) {
      throw new IllegalStateException("The parser should already have checked that.");
    } else if (arg instanceof Identifier id) {
      return new IdentifierPath(List.of(id));
    }

    return ((Expr) arg).accept(this);
  }

  @Override
  public Expr visit(MacroInstanceExpr expr) {
    var arg = args.get(expr.identifier.name);
    if (arg == null) {
      throw new IllegalStateException("The parser should already have checked that.");
    } else if (arg instanceof Identifier id) {
      return new IdentifierPath(List.of(id));
    }

    return ((Expr) arg).accept(this);
  }

  @Override
  public Expr visit(RangeExpr expr) {
    return new RangeExpr(expr.from.accept(this), expr.to.accept(this));
  }

  @Override
  public Expr visit(TypeLiteral expr) {
    for (int i = 0; i < expr.sizeIndices.size(); i++) {
      var sizes = expr.sizeIndices.get(i);
      for (int j = 0; j < sizes.size(); j++) {
        sizes.set(j, sizes.get(j).accept(this));
      }
    }
    return expr;
  }

  @Override
  public Expr visit(IdentifierPath expr) {
    return expr;
  }

  @Override
  public Expr visit(UnaryExpr expr) {
    return new UnaryExpr(expr.operator, expr.operand.accept(this));
  }

  @Override
  public Expr visit(CallExpr expr) {
    expr.target = (SymbolExpr) expr.target.accept(this);
    var argsIndices = expr.argsIndices;
    expr.argsIndices = new ArrayList<>(argsIndices.size());
    for (var entry : argsIndices) {
      var args = new ArrayList<Expr>(entry.size());
      for (var arg : entry) {
        args.add(arg.accept(this));
      }
      expr.argsIndices.add(args);
    }
    var subCalls = expr.subCalls;
    expr.subCalls = new ArrayList<>(subCalls.size());
    for (var subCall : subCalls) {
      argsIndices = new ArrayList<>(subCall.argsIndices().size());
      for (var entry : subCall.argsIndices()) {
        var args = new ArrayList<Expr>(entry.size());
        for (var arg : entry) {
          args.add(arg.accept(this));
        }
        argsIndices.add(args);
      }
      expr.subCalls.add(new CallExpr.SubCall(subCall.id(), argsIndices));
    }
    symbols.requireValue(expr);
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
    expr.size = expr.size == null ? null : expr.size.accept(this);
    return expr;
  }

  @Override
  public Definition visit(ConstantDefinition definition) {
    return new ConstantDefinition(definition.identifier, definition.type,
        definition.value.accept(this), definition.loc);
  }

  @Override
  public Definition visit(FormatDefinition definition) {
    return new FormatDefinition(definition.identifier, definition.type, definition.fields,
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
    if (assignmentStatement.target instanceof IdentifierPath path) {
      symbols.requireValue(
          new CallExpr(new SymbolExpr(path, null, path.location()), List.of(), List.of(),
              path.location()));
    }
    assignmentStatement.valueExpression = assignmentStatement.valueExpression.accept(this);
    return assignmentStatement;
  }

  private Identifier resolvePlaceholderOrIdentifier(Node n) {
    if (n instanceof PlaceholderExpr p) {
      var path = (IdentifierPath) p.accept(this);
      return path.segments.get(path.segments.size() - 1);
    }
    if (n instanceof Identifier id) {
      return id;
    }
    throw new IllegalStateException("Unknown resolved placeholder type " + n);
  }
}
