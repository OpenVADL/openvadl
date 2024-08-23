package vadl.ast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import vadl.error.VadlError;
import vadl.error.VadlException;
import vadl.utils.SourceLocation;

/**
 * Expands any usage of macro instances in the AST (including overrides passed via CLI).
 * Also, since binary expression reordering can depend on operator placeholders, this class also
 * reorders any encountered binary expressions.<br>
 * Before: An AST optionally containing macro instances, placeholders etc.
 *         Any instances of BinaryExpr must be left-sided, as originally parsed.<br>
 * After: An AST containing no special nodes (macro instance, placeholder, node lists).
 *        Any instances of BinaryExpr are ordered according to operator precedence.
 *
 * @see BinaryExpr#reorder(BinaryExpr)
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

  /**
   * Expands the given expr and, if applicable, performs binary expression reordering on it.
   * Since binary expression reordering is absolutely necessary to preserve the original semantics,
   * prefer this method to calling {@code expr.accept(this);} directly.
   * However, to  prevent O(n²) performance, this should never be called during the macro expansion
   * of a binary expression itself.
   *
   * @param expr The expression to perform macro expansion on
   * @return An expanded and optionally reorder expression
   * @see BinaryExpr#reorder(BinaryExpr)
   */
  public Expr expandExpr(Expr expr) {
    var result = expr.accept(this);
    if (!errors.isEmpty()) {
      throw new VadlException(errors);
    }
    if (result instanceof BinaryExpr binaryExpr) {
      return BinaryExpr.reorder(binaryExpr);
    }
    return result;
  }

  public List<Expr> expandExprs(List<Expr> expressions) {
    var copy = new ArrayList<>(expressions);
    copy.replaceAll(this::expandExpr);
    return copy;
  }

  /**
   * Expands all definitions in the given list.
   * If a definition expands to a {@link DefinitionList}, its items are flattened into the result.
   *
   * @param definitions The list of definitions to expand
   * @return A list of expanded and flattened definitions
   */
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

  /**
   * Expands all statements in the given list.
   * If a definition expands to a {@link StatementList}, its items are flattened into the result.
   *
   * @param statements The list of statements to expand
   * @return A list of expanded and flattened statements
   */
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

  Annotations expandAnnotations(Annotations annotations) {
    var list = new ArrayList<>(annotations.annotations());
    list.replaceAll(annotation -> new Annotation(
        expandExpr(annotation.expr()), annotation.type(), annotation.property()));
    return new Annotations(list);
  }

  public Node expandNode(Node node) {
    if (node instanceof Expr expr) {
      return expandExpr(expr);
    } else if (node instanceof Definition definition) {
      return expandDefinition(definition);
    } else if (node instanceof Statement statement) {
      return expandStatement(statement);
    } else if (node instanceof Tuple tuple) {
      var entries = new ArrayList<>(tuple.entries);
      entries.replaceAll(this::expandNode);
      return new Tuple(tuple.type, entries, tuple.sourceLocation);
    } else if (node instanceof EncodingDefinition.FieldEncodings encs) {
      return resolveEncs(encs);
    } else {
      return node;
    }
  }

  @Override
  public Expr visit(Identifier expr) {
    return new Identifier(expr.name, expr.location());
  }

  @Override
  public Expr visit(BinaryExpr expr) {
    var operator = expr.operator instanceof PlaceholderNode p
        ? resolveArg(p.segments) : expr.operator;
    return new BinaryExpr(expr.left.accept(this), (OperatorOrPlaceholder) operator,
        expr.right.accept(this));
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
    return ((Expr) arg);
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
    return new RangeExpr(expandExpr(expr.from), expandExpr(expr.to));
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
    return new UnaryExpr(expr.operator, expandExpr(expr.operand));
  }

  @Override
  public Expr visit(CallExpr expr) {
    var namedArguments = new ArrayList<>(expr.namedArguments);
    namedArguments.replaceAll(namedArgument ->
        new CallExpr.NamedArgument(namedArgument.name(), expandExpr(namedArgument.value())));
    var argsIndices = new ArrayList<>(expr.argsIndices);
    argsIndices.replaceAll(this::expandExprs);
    var subCalls = new ArrayList<>(expr.subCalls);
    subCalls.replaceAll(subCall -> {
      var subCallArgsIndices = new ArrayList<>(subCall.argsIndices());
      subCallArgsIndices.replaceAll(this::expandExprs);
      return new CallExpr.SubCall(subCall.id(), subCallArgsIndices);
    });
    var target = (IsSymExpr) expandExpr((Expr) expr.target);
    return new CallExpr(target, namedArguments, argsIndices, subCalls, expr.location);
  }

  @Override
  public Expr visit(IfExpr expr) {
    return new IfExpr(
        expandExpr(expr.condition),
        expandExpr(expr.thenExpr),
        expandExpr(expr.elseExpr),
        expr.location
    );
  }

  @Override
  public Expr visit(LetExpr expr) {
    var valueExpression = expandExpr(expr.valueExpr);
    var body = expandExpr(expr.body);
    return new LetExpr(expr.identifiers, valueExpression, body, expr.location);
  }

  @Override
  public Expr visit(CastExpr expr) {
    var value = expandExpr(expr.value);
    var type = resolveTypeLiteral(expr.type);
    return new CastExpr(value, type);
  }

  @Override
  public Expr visit(SymbolExpr expr) {
    var path = (IsId) expandExpr((Expr) expr.path);
    var size = expr.size.accept(this);
    return new SymbolExpr(path, size, expr.location);
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
    var candidate = expandExpr(expr.candidate);
    var defaultResult = expandExpr(expr.defaultResult);
    var cases = new ArrayList<>(expr.cases);
    cases.replaceAll(matchCase -> new MatchExpr.Case(expandExprs(matchCase.patterns()),
        expandExpr(matchCase.result())));
    return new MatchExpr(candidate, cases, defaultResult, expr.loc);
  }

  @Override
  public Expr visit(ExtendIdExpr expr) {
    var name = new StringBuilder();
    var expressions = expandExprs(expr.expr.expressions);
    for (var inner : expressions) {
      if (inner instanceof Identifier id) {
        name.append(id.name);
      } else if (inner instanceof StringLiteral string) {
        name.append(string.value);
      } else {
        reportError("Unsupported 'ExtendId' parameter " + inner, inner.location());
        name.append(inner);
      }
    }
    return new Identifier(name.toString(), expr.location());
  }

  @Override
  public Expr visit(IdToStrExpr expr) {
    var id = resolvePlaceholderOrIdentifier(expr.id);
    return new StringLiteral(id, expr.location());
  }

  @Override
  public Definition visit(ConstantDefinition definition) {
    var id = resolvePlaceholderOrIdentifier(definition.identifier);
    var value = expandExpr(definition.value);
    return new ConstantDefinition(id, definition.type, value, definition.loc)
        .withAnnotations(expandAnnotations(definition.annotations));
  }

  @Override
  public Definition visit(FormatDefinition definition) {
    var fields = new ArrayList<>(definition.fields);
    fields.replaceAll(field -> {
      if (field instanceof FormatDefinition.DerivedFormatField derivedFormatField) {
        return new FormatDefinition.DerivedFormatField(derivedFormatField.identifier,
            expandExpr(derivedFormatField.expr));
      } else if (field instanceof FormatDefinition.TypedFormatField typedFormatField) {
        return new FormatDefinition.TypedFormatField(typedFormatField.identifier,
            resolveTypeLiteral(typedFormatField.type));
      } else {
        return field;
      }
    });
    var auxFields = new ArrayList<>(definition.auxiliaryFields);
    auxFields.replaceAll(auxField -> {
      var entries = new ArrayList<>(auxField.entries());
      entries.replaceAll(entry ->
          new FormatDefinition.AuxiliaryFieldEntry(entry.id(), expandExpr(entry.expr())));
      return new FormatDefinition.AuxiliaryField(auxField.kind(), entries);
    });
    var id = resolvePlaceholderOrIdentifier(definition.identifier);
    return new FormatDefinition(id, definition.type, fields, auxFields, definition.loc)
        .withAnnotations(expandAnnotations(definition.annotations));
  }

  @Override
  public Definition visit(InstructionSetDefinition definition) {
    return new InstructionSetDefinition(definition.identifier, definition.extending,
        expandDefinitions(definition.definitions), definition.location());
  }

  @Override
  public Definition visit(CounterDefinition definition) {
    var id = resolvePlaceholderOrIdentifier(definition.identifier);
    return new CounterDefinition(definition.kind, id, definition.type, definition.loc)
        .withAnnotations(expandAnnotations(definition.annotations));
  }

  @Override
  public Definition visit(MemoryDefinition definition) {
    var id = resolvePlaceholderOrIdentifier(definition.identifier);
    return new MemoryDefinition(id, definition.addressType, definition.dataType, definition.loc)
        .withAnnotations(expandAnnotations(definition.annotations));
  }

  @Override
  public Definition visit(RegisterDefinition definition) {
    var id = resolvePlaceholderOrIdentifier(definition.identifier);
    return new RegisterDefinition(id, definition.type, definition.loc)
        .withAnnotations(expandAnnotations(definition.annotations));
  }

  @Override
  public Definition visit(RegisterFileDefinition definition) {
    var id = resolvePlaceholderOrIdentifier(definition.identifier);
    return new RegisterFileDefinition(id, definition.indexType, definition.registerType,
        definition.loc).withAnnotations(expandAnnotations(definition.annotations));
  }

  @Override
  public Definition visit(InstructionDefinition definition) {
    var identifier = resolvePlaceholderOrIdentifier(definition.identifier);
    var typeId = resolvePlaceholderOrIdentifier(definition.typeIdentifier);
    var behavior = definition.behavior.accept(this);

    return new InstructionDefinition(identifier, typeId, behavior, definition.loc)
        .withAnnotations(expandAnnotations(definition.annotations));
  }

  @Override
  public Definition visit(PseudoInstructionDefinition definition) {
    var identifier = resolvePlaceholderOrIdentifier(definition.identifier);
    var behavior = definition.behavior.accept(this);

    return new PseudoInstructionDefinition(identifier, definition.kind, definition.params, behavior,
        definition.loc).withAnnotations(expandAnnotations(definition.annotations));
  }

  @Override
  public Definition visit(EncodingDefinition definition) {
    var instrId = resolvePlaceholderOrIdentifier(definition.instrIdentifier);
    var fieldEncodings = resolveEncs(definition.fieldEncodings);

    return new EncodingDefinition(instrId, fieldEncodings, definition.loc)
        .withAnnotations(expandAnnotations(definition.annotations));
  }

  @Override
  public Definition visit(AssemblyDefinition definition) {
    var identifiers = new ArrayList<>(definition.identifiers);
    identifiers.replaceAll(this::resolvePlaceholderOrIdentifier);
    return new AssemblyDefinition(identifiers, expandExpr(definition.expr), definition.loc)
        .withAnnotations(expandAnnotations(definition.annotations));
  }

  @Override
  public Definition visit(UsingDefinition definition) {
    var id = resolvePlaceholderOrIdentifier(definition.id);
    return new UsingDefinition(id, definition.type, definition.loc)
        .withAnnotations(expandAnnotations(definition.annotations));
  }

  @Override
  public Definition visit(FunctionDefinition definition) {
    var name = resolvePlaceholderOrIdentifier(definition.name);
    return new FunctionDefinition(name, definition.params, definition.retType,
        expandExpr(definition.expr), definition.loc
    ).withAnnotations(expandAnnotations(definition.annotations));
  }

  @Override
  public Definition visit(AliasDefinition definition) {
    var id = resolvePlaceholderOrIdentifier(definition.id);
    var value = expandExpr(definition.value);
    return new AliasDefinition(id, definition.kind, definition.aliasType, definition.targetType,
        value, definition.loc);
  }

  @Override
  public Definition visit(EnumerationDefinition definition) {
    var id = resolvePlaceholderOrIdentifier(definition.id);
    var entries = new ArrayList<>(definition.entries);
    entries.replaceAll(entry -> new EnumerationDefinition.Entry(entry.name(),
        entry.value() == null ? null : expandExpr(entry.value()),
        entry.behavior() == null ? null : expandExpr(entry.behavior())));
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
      return body.accept(subpass);
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
  public Definition visit(DefinitionList definition) {
    var items = expandDefinitions(definition.items);
    return new DefinitionList(items, definition.location);
  }

  @Override
  public BlockStatement visit(BlockStatement blockStatement) {
    return new BlockStatement(expandStatements(blockStatement.statements), blockStatement.location);
  }

  @Override
  public Statement visit(LetStatement letStatement) {
    var valueExpression = expandExpr(letStatement.valueExpression);
    var body = letStatement.body.accept(this);
    return new LetStatement(letStatement.identifiers, valueExpression, body, letStatement.location);
  }

  @Override
  public Statement visit(IfStatement ifStatement) {
    var condition = expandExpr(ifStatement.condition);
    var thenStmt = ifStatement.thenStmt.accept(this);
    var elseStmt = ifStatement.elseStmt == null ? null : ifStatement.elseStmt.accept(this);
    return new IfStatement(condition, thenStmt, elseStmt, ifStatement.location);
  }

  @Override
  public Statement visit(AssignmentStatement assignmentStatement) {
    var target = expandExpr(assignmentStatement.target);
    var valueExpr = expandExpr(assignmentStatement.valueExpression);
    return new AssignmentStatement(target, valueExpr);
  }

  @Override
  public Statement visit(RaiseStatement raiseStatement) {
    return new RaiseStatement(raiseStatement.statement.accept(this), raiseStatement.location);
  }

  @Override
  public Statement visit(CallStatement callStatement) {
    return new CallStatement(expandExpr(callStatement.expr));
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
      return body.accept(subpass);
    } catch (MacroExpansionException e) {
      reportError(e.message, e.sourceLocation);
      return stmt;
    }
  }

  @Override
  public Statement visit(MacroMatchStatement macroMatchStatement) {
    return (Statement) resolveMacroMatch(macroMatchStatement.macroMatch);
  }

  @Override
  public Statement visit(MatchStatement matchStatement) {
    var candidate = expandExpr(matchStatement.candidate);
    var defaultResult = matchStatement.defaultResult.accept(this);
    var cases = new ArrayList<>(matchStatement.cases);
    cases.replaceAll(matchCase -> new MatchStatement.Case(expandExprs(matchCase.patterns()),
        matchCase.result().accept(this)));
    return new MatchStatement(candidate, cases, defaultResult, matchStatement.loc);
  }

  @Override
  public Statement visit(StatementList statementList) {
    var items = expandStatements(statementList.items);
    return new StatementList(items, statementList.location());
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
      if (actualParam instanceof PlaceholderNode placeholderNode) {
        actualParam = expandNode(resolveArg(placeholderNode.segments));
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
    if (idOrPlaceholder instanceof Identifier id) {
      return (Identifier) expandExpr(id);
    } else if (idOrPlaceholder instanceof Expr expr) {
      return (Identifier) expandExpr(expr);
    }
    throw new IllegalStateException("Unknown resolved placeholder type " + idOrPlaceholder);
  }

  private TypeLiteral resolveTypeLiteral(TypeLiteralOrPlaceholder type) {
    var typeLiteral = type instanceof PlaceholderExpr p
        ? new TypeLiteral(resolvePlaceholderOrIdentifier(p))
        : (TypeLiteral) type;
    var baseType = (Identifier) expandExpr((Expr) typeLiteral.baseType);
    var sizeIndices = new ArrayList<>(typeLiteral.sizeIndices);
    sizeIndices.replaceAll(this::expandExprs);
    return new TypeLiteral(baseType, sizeIndices, typeLiteral.location());
  }


  private EncodingDefinition.FieldEncodings resolveEncs(EncodingDefinition.FieldEncodings encs) {
    var fieldEncodings = new ArrayList<FieldEncodingOrPlaceholder>(encs.encodings.size());
    for (var enc : encs.encodings) {
      fieldEncodings.addAll(resolveEnc(enc));
    }
    return new EncodingDefinition.FieldEncodings(fieldEncodings);
  }

  private List<FieldEncodingOrPlaceholder> resolveEnc(FieldEncodingOrPlaceholder encoding) {
    if (encoding instanceof EncodingDefinition.FieldEncoding fieldEncoding) {
      return List.of(new EncodingDefinition.FieldEncoding(fieldEncoding.field(),
          expandExpr(fieldEncoding.value())));
    } else if (encoding instanceof PlaceholderNode p) {
      var arg = (EncodingDefinition.FieldEncodings) resolveArg(p.segments);
      return arg.encodings;
    } else if (encoding instanceof MacroMatchExpr macroMatchExpr) {
      var match = (EncodingDefinition.FieldEncodings) resolveMacroMatch(macroMatchExpr.macroMatch);
      return match.encodings;
    }
    return List.of(encoding);
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
    return expandNode(arg);
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
