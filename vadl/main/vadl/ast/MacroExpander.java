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

  public Statement expandStatement(Statement statement) {
    var result = statement.accept(this);
    if (!errors.isEmpty()) {
      throw new VadlException(errors);
    }
    return result;
  }

  @Override
  public Expr visit(Identifier expr) {
    symbols.requireValue(expr);
    return expr;
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
  public Expr visit(GroupExpr expr) {
    var expressions = new ArrayList<>(expr.expressions);
    expressions.replaceAll(expression -> expression.accept(this));
    return new GroupExpr(expressions, expr.loc);
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
    var arg = args.get(expr.identifierPath.pathToString());
    if (arg == null) {
      throw new IllegalStateException("The parser should already have checked that.");
    } else if (arg instanceof Identifier id) {
      return id;
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
    symbols.requireValue(expr);
    return expr;
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
    var expandedExpr = new CallExpr(target, argsIndices, subCalls, expr.location);
    symbols.requireValue(expandedExpr);
    return expandedExpr;
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
    symbols = symbols.createChild();
    for (var identifier : expr.identifiers) {
      symbols.defineConstant(identifier.name, identifier.loc);
    }
    var valueExpression = expr.valueExpr.accept(this);
    var body = expr.body.accept(this);
    symbols = Objects.requireNonNull(symbols.parent);
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
    expr.size = expr.size.accept(this);
    return expr;
  }

  @Override
  public Expr visit(OperatorExpr expr) {
    return expr;
  }

  @Override
  public Definition visit(ConstantDefinition definition) {
    var id = resolvePlaceholderOrIdentifier(definition.identifier);
    var value = definition.value.accept(this);
    symbols.defineConstant(id.name, definition.loc);
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
            resolveTypeLiteral(typedFormatField.type), typedFormatField.symbolTable);
      } else {
        return field;
      }
    });
    var id = resolvePlaceholderOrIdentifier(definition.identifier);
    var def = new FormatDefinition(id, definition.type, fields, definition.loc);
    def.accept(symbols);
    return def;
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
    var def = new MemoryDefinition(id, definition.addressType, definition.dataType, definition.loc);
    def.accept(symbols);
    return def;
  }

  @Override
  public Definition visit(RegisterDefinition definition) {
    var id = resolvePlaceholderOrIdentifier(definition.identifier);
    var def = new RegisterDefinition(id, definition.type, definition.loc);
    def.accept(symbols);
    return def;
  }

  @Override
  public Definition visit(RegisterFileDefinition definition) {
    var id = resolvePlaceholderOrIdentifier(definition.identifier);
    var def = new RegisterFileDefinition(id, definition.indexType, definition.registerType,
        definition.loc);
    def.accept(symbols);
    return def;
  }

  @Override
  public Definition visit(InstructionDefinition definition) {
    var identifier = resolvePlaceholderOrIdentifier(definition.identifier);
    var typeId = resolvePlaceholderOrIdentifier(definition.typeIdentifier);

    symbols = symbols.createFormatScope(typeId);
    var behavior = definition.behavior.accept(this);
    symbols = Objects.requireNonNull(symbols.parent);

    var def = new InstructionDefinition(identifier, typeId, behavior, definition.loc);
    def.accept(symbols);
    return def;
  }

  @Override
  public Definition visit(EncodingDefinition definition) {
    var instrId = resolvePlaceholderOrIdentifier(definition.instrIdentifier);
    var fieldEncodings = new ArrayList<>(resolveEncs(definition.fieldEncodings).encodings);
    fieldEncodings.replaceAll(enc ->
        new EncodingDefinition.FieldEncoding(enc.field(), enc.value().accept(this)));

    var def = new EncodingDefinition(instrId, new EncodingDefinition.FieldEncodings(fieldEncodings),
        definition.loc);
    def.accept(symbols);
    return def;
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
    var def = new UsingDefinition(id, definition.type, definition.loc);
    def.accept(symbols);
    return def;
  }

  @Override
  public Definition visit(FunctionDefinition definition) {
    var name = resolvePlaceholderOrIdentifier(definition.name);
    var def = new FunctionDefinition(name, definition.params, definition.retType,
        definition.expr.accept(this), definition.loc);
    def.accept(symbols);
    return def;
  }

  @Override
  public Definition visit(PlaceholderDefinition definition) {
    var arg = Objects.requireNonNull(args.get(definition.identifierPath.pathToString()));
    return (Definition) arg;
  }

  @Override
  public BlockStatement visit(BlockStatement blockStatement) {
    symbols = symbols.createChild();
    var statements = new ArrayList<>(blockStatement.statements);
    statements.replaceAll(statement -> statement.accept(this));
    symbols = Objects.requireNonNull(symbols.parent);
    return new BlockStatement(statements, blockStatement.location);
  }

  @Override
  public Statement visit(LetStatement letStatement) {
    symbols = symbols.createChild();
    for (var identifier : letStatement.identifiers) {
      symbols.defineConstant(identifier.name, identifier.loc);
    }
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
      var arg = (EncodingDefinition.FieldEncodings) args.get(p.identifierPath.pathToString());
      return Objects.requireNonNull(arg);
    }
    return (EncodingDefinition.FieldEncodings) encodings;
  }
}
