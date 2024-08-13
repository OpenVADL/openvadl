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
  List<VadlError> errors = new ArrayList<>();

  MacroExpander(Map<String, Node> args) {
    this.args = args;
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

  public Statement expandStatement(Statement statement) {
    var result = statement.accept(this);
    if (!errors.isEmpty()) {
      throw new VadlException(errors);
    }
    return result;
  }

  @Override
  public Expr visit(Identifier expr) {
    return new Identifier(expr.name, expr.location());
  }

  @Override
  public Expr visit(BinaryExpr expr) {
    // FIXME: Only if parent is not a binary operator cause otherwise it is O(n^2)
    var operator = expr.operator instanceof PlaceholderExpr p ? p.accept(this) : expr.operator;
    var result = new BinaryExpr(expr.left.accept(this), (OperatorOrPlaceholder) operator,
        expr.right.accept(this));
    return BinaryExpr.reorder(result);
  }

  @Override
  public Expr visit(GroupedExpr expr) {
    var expressions = new ArrayList<>(expr.expressions);
    expressions.replaceAll(expression -> expression.accept(this));
    return new GroupedExpr(expressions, expr.loc);
  }

  @Override
  public Expr visit(IntegerLiteral expr) {
    return expr;
  }

  @Override
  public Expr visit(BinaryLiteral expr) {
    return expr;
  }

  @Override
  public Expr visit(BoolLiteral expr) {
    return expr;
  }

  @Override
  public Expr visit(StringLiteral expr) {
    return expr;
  }

  @Override
  public Expr visit(PlaceholderExpr expr) {
    // TODO Proper handling of placeholders with format "$a.b.c"
    var arg = args.get(expr.placeholder.path().pathToString());
    if (arg == null) {
      throw new IllegalStateException("The parser should already have checked that.");
    } else if (arg instanceof Identifier id) {
      return id.accept(this);
    } else if (arg instanceof IntegerLiteral lit) {
      return lit;
    } else if (arg instanceof OperatorExpr op) {
      return op;
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
    List<List<Expr>> sizeIndices = new ArrayList<>(expr.sizeIndices);
    sizeIndices.replaceAll(sizes -> {
      var copy = new ArrayList<>(sizes);
      copy.replaceAll(size -> size.accept(this));
      return copy;
    });
    return new TypeLiteral(expr.baseType, sizeIndices, expr.loc);
  }

  @Override
  public Expr visit(IdentifierPath expr) {
    var segments = new ArrayList<>(expr.segments);
    segments.replaceAll(this::resolvePlaceholderOrIdentifier);
    return new IdentifierPath(segments);
  }

  @Override
  public Expr visit(UnaryExpr expr) {
    return new UnaryExpr(expr.operator, expr.operand.accept(this));
  }

  @Override
  public Expr visit(CallExpr expr) {
    var target = (IsSymExpr) ((Expr) expr.target).accept(this);
    var argsIndices = new ArrayList<>(expr.argsIndices);
    argsIndices.replaceAll(args -> {
      var copy = new ArrayList<>(args);
      copy.replaceAll(arg -> arg.accept(this));
      return copy;
    });
    var subCalls = new ArrayList<>(expr.subCalls);
    subCalls.replaceAll(subCall -> {
      var subCallArgsIndices = new ArrayList<>(subCall.argsIndices());
      subCallArgsIndices.replaceAll(args -> {
        var copy = new ArrayList<>(args);
        copy.replaceAll(arg -> arg.accept(this));
        return copy;
      });
      return new CallExpr.SubCall(subCall.id(), subCallArgsIndices);
    });
    return new CallExpr(target, argsIndices, subCalls, expr.location);
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
    var valueExpression = expr.valueExpr.accept(this);
    var body = expr.body.accept(this);
    return new LetExpr(expr.identifiers, valueExpression, body, expr.location);
  }

  @Override
  public Expr visit(CastExpr expr) {
    var value = expr.value.accept(this);
    var type = resolveTypeLiteral(expr.type);
    return new CastExpr(value, type);
  }

  @Override
  public Expr visit(SymbolExpr expr) {
    var size = expr.size.accept(this);
    return new SymbolExpr(expr.path, size, expr.location);
  }

  @Override
  public Expr visit(OperatorExpr expr) {
    return expr;
  }

  @Override
  public Definition visit(ConstantDefinition definition) {
    var id = resolvePlaceholderOrIdentifier(definition.identifier);
    var value = definition.value.accept(this);
    return new ConstantDefinition(id, definition.type, value, definition.loc);
  }

  @Override
  public Definition visit(FormatDefinition definition) {
    var fields = new ArrayList<>(definition.fields);
    fields.replaceAll(field -> {
      if (field instanceof FormatDefinition.DerivedFormatField derivedFormatField) {
        return new FormatDefinition.DerivedFormatField(derivedFormatField.identifier,
            derivedFormatField.expr.accept(this));
      } else if (field instanceof FormatDefinition.TypedFormatField typedFormatField) {
        return new FormatDefinition.TypedFormatField(typedFormatField.identifier,
            resolveTypeLiteral(typedFormatField.type));
      } else {
        return field;
      }
    });
    var id = resolvePlaceholderOrIdentifier(definition.identifier);
    return new FormatDefinition(id, definition.type, fields, definition.loc);
  }

  @Override
  public Definition visit(InstructionSetDefinition definition) {
    return definition;
  }

  @Override
  public Definition visit(CounterDefinition definition) {
    var id = resolvePlaceholderOrIdentifier(definition.identifier);
    return new CounterDefinition(definition.kind, id, definition.type, definition.loc);
  }

  @Override
  public Definition visit(MemoryDefinition definition) {
    var id = resolvePlaceholderOrIdentifier(definition.identifier);
    return new MemoryDefinition(id, definition.addressType, definition.dataType, definition.loc);
  }

  @Override
  public Definition visit(RegisterDefinition definition) {
    var id = resolvePlaceholderOrIdentifier(definition.identifier);
    return new RegisterDefinition(id, definition.type, definition.loc);
  }

  @Override
  public Definition visit(RegisterFileDefinition definition) {
    var id = resolvePlaceholderOrIdentifier(definition.identifier);
    return new RegisterFileDefinition(id, definition.indexType, definition.registerType,
        definition.loc);
  }

  @Override
  public Definition visit(InstructionDefinition definition) {
    var identifier = resolvePlaceholderOrIdentifier(definition.identifier);
    var typeId = resolvePlaceholderOrIdentifier(definition.typeIdentifier);
    var behavior = definition.behavior.accept(this);

    return new InstructionDefinition(identifier, typeId, behavior, definition.loc);
  }

  @Override
  public Definition visit(EncodingDefinition definition) {
    var instrId = resolvePlaceholderOrIdentifier(definition.instrIdentifier);
    var fieldEncodings = new ArrayList<>(resolveEncs(definition.fieldEncodings).encodings);
    fieldEncodings.replaceAll(enc ->
        new EncodingDefinition.FieldEncoding(enc.field(), enc.value().accept(this)));

    return new EncodingDefinition(instrId, new EncodingDefinition.FieldEncodings(fieldEncodings),
        definition.loc);
  }

  @Override
  public Definition visit(AssemblyDefinition definition) {
    var identifiers = new ArrayList<>(definition.identifiers);
    identifiers.replaceAll(this::resolvePlaceholderOrIdentifier);
    var expr = definition.expr.accept(this);
    return new AssemblyDefinition(identifiers, expr, definition.loc);
  }

  @Override
  public Definition visit(UsingDefinition definition) {
    var id = resolvePlaceholderOrIdentifier(definition.id);
    return new UsingDefinition(id, definition.type, definition.loc);
  }

  @Override
  public Definition visit(FunctionDefinition definition) {
    var name = resolvePlaceholderOrIdentifier(definition.name);
    return new FunctionDefinition(name, definition.params, definition.retType,
        definition.expr.accept(this), definition.loc);
  }

  @Override
  public Definition visit(PlaceholderDefinition definition) {
    var arg = Objects.requireNonNull(args.get(definition.placeholder.path().pathToString()));
    return (Definition) arg;
  }

  @Override
  public BlockStatement visit(BlockStatement blockStatement) {
    var statements = new ArrayList<>(blockStatement.statements);
    statements.replaceAll(statement -> statement.accept(this));
    return new BlockStatement(statements, blockStatement.location);
  }

  @Override
  public Statement visit(LetStatement letStatement) {
    var valueExpression = letStatement.valueExpression.accept(this);
    var body = letStatement.body.accept(this);
    return new LetStatement(letStatement.identifiers, valueExpression, body, letStatement.location);
  }

  @Override
  public Statement visit(IfStatement ifStatement) {
    var condition = ifStatement.condition.accept(this);
    var thenStmt = ifStatement.thenStmt.accept(this);
    var elseStmt = ifStatement.elseStmt == null ? null : ifStatement.elseStmt.accept(this);
    return new IfStatement(condition, thenStmt, elseStmt, ifStatement.location);
  }

  @Override
  public Statement visit(AssignmentStatement assignmentStatement) {
    var target = assignmentStatement.target.accept(this);
    var valueExpr = assignmentStatement.valueExpression.accept(this);
    return new AssignmentStatement(target, valueExpr);
  }

  private Identifier resolvePlaceholderOrIdentifier(IdentifierOrPlaceholder idOrPlaceholder) {
    if (idOrPlaceholder instanceof PlaceholderExpr p) {
      return (Identifier) p.accept(this);
    }
    if (idOrPlaceholder instanceof Identifier id) {
      return id;
    }
    throw new IllegalStateException("Unknown resolved placeholder type " + idOrPlaceholder);
  }

  private TypeLiteral resolveTypeLiteral(TypeLiteralOrPlaceholder type) {
    return type instanceof PlaceholderExpr p
        ? new TypeLiteral(resolvePlaceholderOrIdentifier(p))
        : (TypeLiteral) type;
  }

  private EncodingDefinition.FieldEncodings resolveEncs(FieldEncodingsOrPlaceholder encodings) {
    if (encodings instanceof PlaceholderExpr p) {
      var arg = (EncodingDefinition.FieldEncodings) args.get(p.placeholder.path().pathToString());
      return Objects.requireNonNull(arg);
    }
    return (EncodingDefinition.FieldEncodings) encodings;
  }
}
