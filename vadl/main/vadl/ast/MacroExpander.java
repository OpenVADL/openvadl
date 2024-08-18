package vadl.ast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import vadl.error.VadlError;
import vadl.error.VadlException;
import vadl.utils.SourceLocation;

/**
 * Expands any usage of macro instances in the AST (including overrides passed via CLI).
 * Before: An AST optionally containing macro instances, placeholders etc.
 * After: An AST containing no special nodes (macro instance, placeholder, node lists).
 */
class MacroExpander
    implements ExprVisitor<Expr>, DefinitionVisitor<Definition>, StatementVisitor<Statement> {
  final Map<String, Node> args;
  final Map<String, Identifier> macroOverrides;
  final List<VadlError> errors = new ArrayList<>();

  MacroExpander(Map<String, Node> args, Map<String, Identifier> macroOverrides) {
    this.args = args;
    this.macroOverrides = macroOverrides;
  }

  static void expandAst(Ast ast, Map<String, Identifier> macroOverrides) {
    var instance = new MacroExpander(new HashMap<>(), macroOverrides);
    ast.definitions = instance.expandDefinitions(ast.definitions);
  }

  public Expr expandExpr(Expr expr) {
    var result = expr.accept(this);
    if (!errors.isEmpty()) {
      throw new VadlException(errors);
    }
    return result;
  }

  public List<Expr> expandExprs(List<Expr> expressions) {
    var copy = new ArrayList<>(expressions);
    copy.replaceAll(this::expandExpr);
    return copy;
  }

  public List<Definition> expandDefinitions(List<Definition> definitions) {
    var defs = new ArrayList<Definition>(definitions.size());
    for (var def : definitions) {
      var expanded = expandDefinition(def);
      if (expanded instanceof DefinitionList list) {
        defs.addAll(list.items);
      } else {
        defs.add(expanded);
      }
    }
    return defs;
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

  public Node expandNode(Node node) {
    if (node instanceof Expr expr) {
      return expandExpr(expr);
    } else if (node instanceof DefinitionList definitionList) {
      var definitions = expandDefinitions(definitionList.items);
      return new DefinitionList(definitions, definitionList.location());
    } else if (node instanceof Definition definition) {
      return expandDefinition(definition);
    } else if (node instanceof StatementList statementList) {
      var statements = expandStatements(statementList.items);
      return new StatementList(statements, statementList.location());
    } else if (node instanceof Statement statement) {
      return expandStatement(statement);
    } else {
      return node;
    }
  }

  public List<Statement> expandStatements(List<Statement> statements) {
    var stmts = new ArrayList<Statement>(statements.size());
    for (var statement : statements) {
      var expanded = expandStatement(statement);
      if (expanded instanceof StatementList list) {
        stmts.addAll(list.items);
      } else {
        stmts.add(expanded);
      }
    }
    return stmts;
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
    return new GroupedExpr(expandExprs(expr.expressions), expr.loc);
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
    Node arg = resolveArg(expr.segments);
    if (arg instanceof Identifier id) {
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
    var macro = resolveMacro(expr.macro);

    // Overrides can be passed via the CLI or the API
    if (macro.returnType().equals(BasicSyntaxType.ID)
        && macroOverrides.containsKey(macro.name().name)) {
      return macroOverrides.get(macro.name().name);
    }
    try {
      assertValidMacro(macro, expr.location());
      var arguments = collectMacroParameters(macro, expr.arguments, expr.location());
      var body = (Expr) macro.body();
      var subpass = new MacroExpander(arguments, macroOverrides);
      var expanded = subpass.expandExpr(body);
      var group = new GroupedExpr(new ArrayList<>(), expr.location());
      group.expressions.add(expanded);
      return group;
    } catch (MacroExpansionException e) {
      reportError(e.message, e.sourceLocation);
      return expr;
    }
  }

  @Override
  public Expr visit(RangeExpr expr) {
    return new RangeExpr(expr.from.accept(this), expr.to.accept(this));
  }

  @Override
  public Expr visit(TypeLiteral expr) {
    List<List<Expr>> sizeIndices = new ArrayList<>(expr.sizeIndices);
    sizeIndices.replaceAll(this::expandExprs);
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
    argsIndices.replaceAll(this::expandExprs);
    var subCalls = new ArrayList<>(expr.subCalls);
    subCalls.replaceAll(subCall -> {
      var subCallArgsIndices = new ArrayList<>(subCall.argsIndices());
      subCallArgsIndices.replaceAll(this::expandExprs);
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
  public Expr visit(MacroMatchExpr expr) {
    return (Expr) resolveMacroMatch(expr.macroMatch);
  }

  @Override
  public Expr visit(MatchExpr expr) {
    var candidate = expr.candidate.accept(this);
    var defaultResult = expr.defaultResult.accept(this);
    var cases = new ArrayList<>(expr.cases);
    cases.replaceAll(matchCase -> new MatchExpr.Case(expandExprs(matchCase.patterns()),
        matchCase.result().accept(this)));
    return new MatchExpr(candidate, cases, defaultResult, expr.loc);
  }

  @Override
  public Definition visit(ConstantDefinition definition) {
    var id = resolvePlaceholderOrIdentifier(definition.identifier);
    var value = definition.value.accept(this);
    return new ConstantDefinition(id, definition.type, value, definition.loc)
        .withAnnotations(definition.annotations);
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
    return new FormatDefinition(id, definition.type, fields, definition.loc)
        .withAnnotations(definition.annotations);
  }

  @Override
  public Definition visit(InstructionSetDefinition definition) {
    definition.definitions = expandDefinitions(definition.definitions);
    return definition;
  }

  @Override
  public Definition visit(CounterDefinition definition) {
    var id = resolvePlaceholderOrIdentifier(definition.identifier);
    return new CounterDefinition(definition.kind, id, definition.type, definition.loc)
        .withAnnotations(definition.annotations);
  }

  @Override
  public Definition visit(MemoryDefinition definition) {
    var id = resolvePlaceholderOrIdentifier(definition.identifier);
    return new MemoryDefinition(id, definition.addressType, definition.dataType, definition.loc)
        .withAnnotations(definition.annotations);
  }

  @Override
  public Definition visit(RegisterDefinition definition) {
    var id = resolvePlaceholderOrIdentifier(definition.identifier);
    return new RegisterDefinition(id, definition.type, definition.loc)
        .withAnnotations(definition.annotations);
  }

  @Override
  public Definition visit(RegisterFileDefinition definition) {
    var id = resolvePlaceholderOrIdentifier(definition.identifier);
    return new RegisterFileDefinition(id, definition.indexType, definition.registerType,
        definition.loc).withAnnotations(definition.annotations);
  }

  @Override
  public Definition visit(InstructionDefinition definition) {
    var identifier = resolvePlaceholderOrIdentifier(definition.identifier);
    var typeId = resolvePlaceholderOrIdentifier(definition.typeIdentifier);
    var behavior = definition.behavior.accept(this);

    return new InstructionDefinition(identifier, typeId, behavior, definition.loc)
        .withAnnotations(definition.annotations);
  }

  @Override
  public Definition visit(EncodingDefinition definition) {
    var instrId = resolvePlaceholderOrIdentifier(definition.instrIdentifier);
    var fieldEncodings = new ArrayList<>(resolveEncs(definition.fieldEncodings).encodings);
    fieldEncodings.replaceAll(enc ->
        new EncodingDefinition.FieldEncoding(enc.field(), enc.value().accept(this)));

    return new EncodingDefinition(instrId, new EncodingDefinition.FieldEncodings(fieldEncodings),
        definition.loc).withAnnotations(definition.annotations);
  }

  @Override
  public Definition visit(AssemblyDefinition definition) {
    var identifiers = new ArrayList<>(definition.identifiers);
    identifiers.replaceAll(this::resolvePlaceholderOrIdentifier);
    var expr = definition.expr.accept(this);
    return new AssemblyDefinition(identifiers, expr, definition.loc)
        .withAnnotations(definition.annotations);
  }

  @Override
  public Definition visit(UsingDefinition definition) {
    var id = resolvePlaceholderOrIdentifier(definition.id);
    return new UsingDefinition(id, definition.type, definition.loc)
        .withAnnotations(definition.annotations);
  }

  @Override
  public Definition visit(FunctionDefinition definition) {
    var name = resolvePlaceholderOrIdentifier(definition.name);
    return new FunctionDefinition(name, definition.params, definition.retType,
        definition.expr.accept(this), definition.loc).withAnnotations(definition.annotations);
  }

  @Override
  public Definition visit(AliasDefinition definition) {
    var id = resolvePlaceholderOrIdentifier(definition.id);
    var value = definition.value.accept(this);
    return new AliasDefinition(id, definition.kind, definition.aliasType, definition.targetType,
        value, definition.loc);
  }

  @Override
  public Definition visit(EnumerationDefinition definition) {
    var id = resolvePlaceholderOrIdentifier(definition.id);
    var entries = new ArrayList<>(definition.entries);
    entries.replaceAll(entry -> new EnumerationDefinition.Entry(entry.name(),
        entry.value() == null ? null : entry.value().accept(this),
        entry.behavior() == null ? null : entry.behavior().accept(this)));
    return new EnumerationDefinition(id, definition.enumType, entries, definition.loc);
  }

  @Override
  public Definition visit(ExceptionDefinition definition) {
    var id = resolvePlaceholderOrIdentifier(definition.id);
    return new ExceptionDefinition(id, definition.statement.accept(this), definition.loc);
  }

  @Override
  public Definition visit(PlaceholderDefinition definition) {
    var arg = resolveArg(definition.segments);
    return (Definition) arg;
  }

  @Override
  public Definition visit(MacroInstanceDefinition definition) {
    try {
      var macro = resolveMacro(definition.macro);
      assertValidMacro(macro, definition.location());
      var arguments =
          collectMacroParameters(macro, definition.arguments, definition.location());
      var body = (Definition) macro.body();
      var subpass = new MacroExpander(arguments, macroOverrides);
      if (body instanceof DefinitionList definitionList) {
        var definitions = subpass.expandDefinitions(definitionList.items);
        return new DefinitionList(definitions, definitionList.location);
      } else {
        return subpass.expandDefinition(body);
      }
    } catch (MacroExpansionException e) {
      reportError(e.message, e.sourceLocation);
      return definition;
    }
  }

  @Override
  public Definition visit(MacroMatchDefinition definition) {
    return (Definition) resolveMacroMatch(definition.macroMatch);
  }

  @Override
  public BlockStatement visit(BlockStatement blockStatement) {
    return new BlockStatement(expandStatements(blockStatement.statements), blockStatement.location);
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

  @Override
  public Statement visit(RaiseStatement raiseStatement) {
    return new RaiseStatement(raiseStatement.statement.accept(this), raiseStatement.location);
  }

  @Override
  public Statement visit(CallStatement callStatement) {
    return new CallStatement(callStatement.expr.accept(this));
  }

  @Override
  public Statement visit(PlaceholderStatement statement) {
    var arg = resolveArg(statement.segments);
    return (Statement) arg;
  }

  @Override
  public Statement visit(MacroInstanceStatement stmt) {
    try {
      var macro = resolveMacro(stmt.macro);
      assertValidMacro(macro, stmt.location());
      var arguments = collectMacroParameters(macro, stmt.arguments, stmt.location());
      var body = (Statement) macro.body();
      var subpass = new MacroExpander(arguments, macroOverrides);
      if (body instanceof StatementList statementList) {
        var statements = subpass.expandStatements(statementList.items);
        return new StatementList(statements, statementList.location);
      } else {
        return subpass.expandStatement(body);
      }
    } catch (MacroExpansionException e) {
      reportError(e.message, e.sourceLocation);
      return stmt;
    }
  }

  @Override
  public Statement visit(MacroMatchStatement macroMatchStatement) {
    return (Statement) resolveMacroMatch(macroMatchStatement.macroMatch);
  }

  private void assertValidMacro(Macro macro, SourceLocation sourceLocation)
      throws MacroExpansionException {
    if (macro.returnType() == BasicSyntaxType.INVALID) {
      throw new MacroExpansionException(
          "Skipped expanding macro %s due to previous error".formatted(macro.name().name),
          sourceLocation);
    }
  }

  private Map<String, Node> collectMacroParameters(Macro macro, List<Node> actualParams,
                                                   SourceLocation instanceLoc)
      throws MacroExpansionException {
    var formalParams = macro.params();
    if (formalParams.size() != actualParams.size()) {
      throw new MacroExpansionException(
          "The macro `%s` expects %d arguments but %d were provided.".formatted(macro.name().name,
              formalParams.size(), actualParams.size()), instanceLoc);
    }
    var arguments = new HashMap<>(args);
    for (int i = 0; i < formalParams.size(); i++) {
      var formalParam = formalParams.get(i);
      var actualParam = expandNode(actualParams.get(i));
      if (actualParam instanceof PlaceholderRecord placeholderRecord) {
        actualParam = resolveArg(placeholderRecord.segments);
      }
      if (actualParam.syntaxType().isSubTypeOf(formalParam.type())) {
        arguments.put(formalParam.name().name, actualParam);
      } else {
        throw new MacroExpansionException(
            "Macro %s expects parameter %s to be of type %s, got %s instead".formatted(
                macro.name().name, formalParam.name().name, formalParam.type(),
                actualParam.syntaxType()), instanceLoc);
      }
    }
    return arguments;
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
      var arg = (EncodingDefinition.FieldEncodings) resolveArg(p.segments);
      return Objects.requireNonNull(arg);
    }
    return (EncodingDefinition.FieldEncodings) encodings;
  }

  private Node resolveMacroMatch(MacroMatch macroMatch) {
    for (var choice : macroMatch.choices()) {
      var candidate = expandNode(choice.candidate());
      var equals = candidate.equals(choice.match());
      var shouldEqual = choice.comparison() == MacroMatch.Comparison.EQUAL;
      if (equals == shouldEqual) {
        return expandNode(choice.result());
      }
    }
    return expandNode(macroMatch.defaultChoice());
  }

  private Node resolveArg(List<String> segments) {
    Node arg = args.get(segments.get(0));
    if (arg == null) {
      throw new IllegalStateException("Could not resolve argument " + segments);
    }
    for (int i = 1; i < segments.size(); i++) {
      var nextName = segments.get(i);
      var tuple = (Tuple) arg;
      for (int j = 0; j < tuple.type.entries.size(); j++) {
        if (tuple.type.entries.get(j).name().equals(nextName)) {
          arg = tuple.entries.get(j);
          break;
        }
      }
    }
    return arg;
  }

  private Macro resolveMacro(MacroOrPlaceholder macroOrPlaceholder) {
    if (macroOrPlaceholder instanceof Macro macro) {
      return macro;
    }
    return ((MacroReference) resolveArg(((MacroPlaceholder) macroOrPlaceholder).segments())).macro;
  }

  private void reportError(String error, SourceLocation location) {
    errors.add(new VadlError(error, location, null, null));
  }

  static class MacroExpansionException extends Exception {
    String message;
    SourceLocation sourceLocation;

    MacroExpansionException(String message, SourceLocation sourceLocation) {
      super(message);
      this.message = message;
      this.sourceLocation = sourceLocation;
    }
  }
}
