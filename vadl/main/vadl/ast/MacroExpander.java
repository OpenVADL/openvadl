// SPDX-FileCopyrightText : © 2025 TU Wien <vadl@tuwien.ac.at>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

package vadl.ast;

import static vadl.error.Diagnostic.error;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;
import vadl.error.Diagnostic;
import vadl.error.DiagnosticList;
import vadl.utils.SourceLocation;

/**
 * Expands any usage of macro instances in the AST (including overrides passed via CLI).
 * Also, since binary expression reordering can depend on operator placeholders, this class also
 * reorders any encountered binary expressions.<br>
 * Before: An AST optionally containing macro instances, placeholders etc.
 * Any instances of BinaryExpr must be left-sided, as originally parsed.<br>
 * After: An AST containing no special nodes (macro instance, placeholder, node lists).
 * Any instances of BinaryExpr are ordered according to operator precedence.
 *
 * @see BinaryExpr#reorder(BinaryExpr)
 */
class MacroExpander
    implements ExprVisitor<Expr>, DefinitionVisitor<Definition>, StatementVisitor<Statement> {
  final Map<String, Node> args;
  final Map<String, Identifier> macroOverrides;
  final List<Diagnostic> errors = new ArrayList<>();
  @Nullable
  final SourceLocation expandingFrom;

  MacroExpander(Map<String, Node> args, Map<String, Identifier> macroOverrides,
                @Nullable SourceLocation expandingFrom) {
    this.args = args;
    this.macroOverrides = macroOverrides;
    this.expandingFrom = expandingFrom;
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
      throw new DiagnosticList(errors);
    }
    if (result instanceof BinaryExpr binaryExpr && !binaryExpr.hasBeenReordered) {
      return ParserUtils.reorderBinaryExpr(binaryExpr);
    }
    return result;
  }

  public CallIndexExpr.Arguments expandArgs(CallIndexExpr.Arguments args) {
    return new CallIndexExpr.Arguments(
        args.values.stream()
            .map(this::expandExpr)
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll),
        copyLoc(args.location));
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
      throw new DiagnosticList(errors);
    }
    return result;
  }

  public Statement expandStatement(Statement statement) {
    var result = statement.accept(this);
    if (!errors.isEmpty()) {
      throw new DiagnosticList(errors);
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
        expandExpr(annotation.expr), annotation.type, annotation.property == null ? null :
        resolvePlaceholderOrIdentifier(annotation.property)));
    return new Annotations(list);
  }

  public Node expandNode(Node node) {
    if (node instanceof Expr expr) {
      return expandExpr(expr);
    } else if (node instanceof Definition definition) {
      return expandDefinition(definition);
    } else if (node instanceof Statement statement) {
      return expandStatement(statement);
    } else if (node instanceof RecordInstance recordInstance) {
      var entries = new ArrayList<>(recordInstance.entries);
      entries.replaceAll(this::expandNode);
      return new RecordInstance(recordInstance.type, entries, recordInstance.sourceLocation);
    } else if (node instanceof EncodingDefinition.EncsNode encs) {
      return resolveEncs(encs);
    } else if (node instanceof PlaceholderNode placeholderNode) {
      return expand(placeholderNode);
    } else if (node instanceof MacroInstanceNode macroInstanceNode) {
      return expand(macroInstanceNode);
    } else if (node instanceof MacroMatchNode macroMatchNode) {
      return expand(macroMatchNode);
    } else {
      return node;
    }
  }

  public List<Node> expandNodes(List<Node> nodes) {
    var copy = new ArrayList<>(nodes);
    copy.replaceAll(this::expandNode);
    return copy;
  }

  @Override
  public Expr visit(Identifier expr) {
    return new Identifier(expr.name, copyLoc(expr.location()));
  }

  @Override
  public Expr visit(BinaryExpr expr) {
    var expanded = new BinaryExpr(expr.left.accept(this),
        (IsBinOp) expandNode((Node) expr.operator), expr.right.accept(this));
    expanded.hasBeenReordered = expr.hasBeenReordered;
    return expanded;
  }

  @Override
  public Expr visit(GroupedExpr expr) {
    return new GroupedExpr(expandExprs(expr.expressions), copyLoc(expr.loc));
  }

  @Override
  public Expr visit(IntegerLiteral expr) {
    return new IntegerLiteral(expr.token, copyLoc(expr.loc));
  }

  @Override
  public Expr visit(BinaryLiteral expr) {
    return new BinaryLiteral(expr.token, copyLoc(expr.loc));
  }

  @Override
  public Expr visit(BoolLiteral expr) {
    return new BoolLiteral(expr.value, copyLoc(expr.loc));
  }

  @Override
  public Expr visit(StringLiteral expr) {
    return new StringLiteral(expr.token, copyLoc(expr.loc));
  }

  @Override
  public Expr visit(PlaceholderExpr expr) {
    Node arg = resolveArg(expr.segments);
    return Objects.requireNonNullElse((Expr) arg, expr);
  }

  @Override
  public Expr visit(MacroInstanceExpr expr) {
    var macro = resolveMacro(expr.macro);
    if (macro == null) {
      // Macro reference passed down multiple layers - let parent layer expand
      var arguments = new ArrayList<>(expr.arguments);
      arguments.replaceAll(this::expandNode);
      var placeholder = (MacroPlaceholder) expr.macro;
      var resolved = resolveArg(placeholder.segments());
      var newSegments =
          resolved == null ? placeholder.segments() : ((PlaceholderNode) resolved).segments;
      return new MacroInstanceExpr(new MacroPlaceholder(placeholder.syntaxType(), newSegments),
          arguments, copyLoc(expr.loc));
    }

    // Overrides can be passed via the CLI or the API
    if (macro.returnType().equals(BasicSyntaxType.ID)
        && macroOverrides.containsKey(macro.name().name)) {
      return macroOverrides.get(macro.name().name);
    }
    try {
      assertValidMacro(macro, expr.location());
      var arguments = collectMacroParameters(macro, expr.arguments, expr.location());
      var body = (Expr) macro.body();
      var subpass = new MacroExpander(arguments, macroOverrides, copyLoc(expr.loc));
      var expanded = subpass.expandExpr(body);
      if (macro.returnType().equals(BasicSyntaxType.EX)) {
        var group = new GroupedExpr(new ArrayList<>(), expanded.location());
        group.expressions.add(expanded);
        return group;
      } else {
        return expanded;
      }
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
    var baseType = (IsId) expandExpr((Expr) expr.baseType);
    var sizeIndices = new ArrayList<>(expr.sizeIndices);
    sizeIndices.replaceAll(this::expandExprs);
    return new TypeLiteral(baseType, sizeIndices, copyLoc(expr.location()));
  }

  @Override
  public Expr visit(IdentifierPath expr) {
    var segments = new ArrayList<>(expr.segments);
    segments.replaceAll(this::resolvePlaceholderOrIdentifier);
    return new IdentifierPath(segments);
  }

  @Override
  public Expr visit(UnaryExpr expr) {
    return new UnaryExpr((IsUnOp) expandNode((Node) expr.operator), expandExpr(expr.operand));
  }

  @Override
  public Expr visit(CallIndexExpr expr) {
    var argsIndices = new ArrayList<>(expr.argsIndices);
    argsIndices.replaceAll(this::expandArgs);
    var subCalls = new ArrayList<>(expr.subCalls);
    subCalls.replaceAll(subCall -> {
      var subCallArgsIndices = new ArrayList<>(subCall.argsIndices);
      subCallArgsIndices.replaceAll(this::expandArgs);
      return new CallIndexExpr.SubCall(subCall.id, subCallArgsIndices);
    });
    var target = (IsSymExpr) expandExpr((Expr) expr.target);
    return new CallIndexExpr(target, argsIndices, subCalls, copyLoc(expr.location));
  }

  @Override
  public Expr visit(IfExpr expr) {
    return new IfExpr(
        expandExpr(expr.condition),
        expandExpr(expr.thenExpr),
        expandExpr(expr.elseExpr),
        copyLoc(expr.location)
    );
  }

  @Override
  public Expr visit(LetExpr expr) {
    var valueExpression = expandExpr(expr.valueExpr);
    var body = expandExpr(expr.body);
    return new LetExpr(expr.identifiers, valueExpression, body, copyLoc(expr.location));
  }

  @Override
  public Expr visit(CastExpr expr) {
    var value = expandExpr(expr.value);
    var type = expandExpr(expr.typeLiteral);
    return new CastExpr(value, (TypeLiteral) type);
  }

  @Override
  public Expr visit(SymbolExpr expr) {
    var path = (IsId) expandExpr((Expr) expr.path);
    var size = expr.size.accept(this);
    return new SymbolExpr(path, size, copyLoc(expr.location));
  }

  @Override
  public Expr visit(MacroMatchExpr expr) {
    var macroMatch = expandMacroMatch(expr.macroMatch);
    var resolved = resolveMacroMatch(macroMatch);
    if (resolved != null) {
      return (Expr) resolved;
    } else {
      return new MacroMatchExpr(macroMatch);
    }
  }

  @Override
  public Expr visit(MatchExpr expr) {
    var candidate = expandExpr(expr.candidate);
    var defaultResult = expandExpr(expr.defaultResult);
    var cases = new ArrayList<>(expr.cases);
    cases.replaceAll(matchCase -> new MatchExpr.Case(expandExprs(matchCase.patterns),
        expandExpr(matchCase.result)));
    return new MatchExpr(candidate, cases, defaultResult, copyLoc(expr.loc));
  }

  @Override
  public Expr visit(ExtendIdExpr expr) {
    var name = new StringBuilder();
    var expressions = (GroupedExpr) expr.expr.accept(this);
    for (var inner : expressions.expressions) {
      if (inner instanceof Identifier id) {
        name.append(id.name);
      } else if (inner instanceof StringLiteral string) {
        name.append(string.value);
      } else if (inner instanceof PlaceholderExpr
          || inner instanceof ExtendIdExpr || inner instanceof IdToStrExpr) {
        // Will be expanded as soon as the used placeholders are bound
        return new ExtendIdExpr(expressions, copyLoc(expr.location()));
      } else {
        reportError("Unsupported 'ExtendId' parameter " + inner, inner.location());
        name.append(inner);
      }
    }
    return new Identifier(name.toString(), copyLoc(expr.location()));
  }

  @Override
  public Expr visit(IdToStrExpr expr) {
    var idOrPlaceholder = resolvePlaceholderOrIdentifier(expr.id);
    if (idOrPlaceholder instanceof Identifier id) {
      return new StringLiteral(id, copyLoc(expr.location()));
    } else {
      return new IdToStrExpr(idOrPlaceholder, copyLoc(expr.location()));
    }
  }

  @Override
  public Expr visit(ExistsInExpr expr) {
    var operations = new ArrayList<>(expr.operations);
    operations.replaceAll(id -> (IsId) expandExpr((Expr) id));
    return new ExistsInExpr(operations, copyLoc(expr.loc));
  }

  @Override
  public Expr visit(ExistsInThenExpr expr) {
    var conditions = new ArrayList<>(expr.conditions);
    conditions.replaceAll(condition -> {
      var operations = new ArrayList<>(condition.operations());
      operations.replaceAll(id -> (IsId) expandExpr((Expr) id));
      return new ExistsInThenExpr.Condition((IsId) expandExpr((Expr) condition.id()), operations);
    });
    return new ExistsInThenExpr(conditions, expandExpr(expr.thenExpr), copyLoc(expr.loc));
  }

  @Override
  public Expr visit(ForallThenExpr expr) {
    var indices = new ArrayList<>(expr.indices);
    indices.replaceAll(index -> {
      var operations = new ArrayList<>(index.operations);
      operations.replaceAll(id -> (IsId) expandExpr((Expr) id));
      return new ForallThenExpr.Index((IsId) expandExpr((Expr) index.identifier()), operations);
    });
    return new ForallThenExpr(indices, expandExpr(expr.thenExpr), copyLoc(expr.loc));
  }

  @Override
  public Expr visit(ForallExpr expr) {
    var indices = new ArrayList<>(expr.indices);
    indices.replaceAll(index -> new ForallExpr.Index((IsId) expandExpr((Expr) index.identifier()),
        expandExpr(index.domain)));
    return new ForallExpr(indices, expr.operation, expr.foldOperator, expandExpr(expr.expr),
        copyLoc(expr.loc));
  }

  @Override
  public Expr visit(SequenceCallExpr expr) {
    return new SequenceCallExpr(expr.target, expr.range == null ? null : expr.range.accept(this),
        expr.loc);
  }

  @Override
  public Definition visit(ConstantDefinition definition) {
    var id = resolvePlaceholderOrIdentifier(definition.identifier);
    var value = expandExpr(definition.value);
    return new ConstantDefinition(id, definition.typeLiteral, value, copyLoc(definition.loc))
        .withAnnotations(expandAnnotations(definition.annotations));
  }

  @Override
  public Definition visit(FormatDefinition definition) {
    var fields = definition.fields.stream().map(f -> (FormatField) f.accept(this)).toList();
    var auxFields = new ArrayList<>(definition.auxiliaryFields);
    auxFields.replaceAll(auxField -> {
      var entries = new ArrayList<>(auxField.entries());
      entries.replaceAll(entry ->
          new FormatDefinition.AuxiliaryFieldEntry(entry.id, expandExpr(entry.expr)));
      return new FormatDefinition.AuxiliaryField(auxField.kind(), entries);
    });
    var id = resolvePlaceholderOrIdentifier(definition.identifier);
    return new FormatDefinition(id, definition.typeLiteral, fields, auxFields,
        copyLoc(definition.loc))
        .withAnnotations(expandAnnotations(definition.annotations));
  }

  @Override
  public Definition visit(DerivedFormatField definition) {
    return new DerivedFormatField(definition.identifier,
        expandExpr(definition.expr));
  }

  @Override
  public Definition visit(RangeFormatField definition) {
    return new RangeFormatField(
        definition.identifier,
        definition.ranges.stream().map(e -> e.accept(this)).toList(),
        definition.typeLiteral == null ? null : (TypeLiteral) definition.typeLiteral.accept(this)
    );
  }

  @Override
  public Definition visit(TypedFormatField definition) {
    return new TypedFormatField(definition.identifier,
        (TypeLiteral) expandExpr(definition.typeLiteral));
  }

  @Override
  public Definition visit(InstructionSetDefinition definition) {
    return new InstructionSetDefinition(definition.identifier, definition.extending,
        expandDefinitions(definition.definitions), copyLoc(definition.location()));
  }

  @Override
  public Definition visit(CounterDefinition definition) {
    var id = resolvePlaceholderOrIdentifier(definition.identifier);
    return new CounterDefinition(definition.kind, id, definition.typeLiteral,
        copyLoc(definition.loc))
        .withAnnotations(expandAnnotations(definition.annotations));
  }

  @Override
  public Definition visit(MemoryDefinition definition) {
    var id = resolvePlaceholderOrIdentifier(definition.identifier);
    return new MemoryDefinition(id, definition.addressTypeLiteral, definition.dataTypeLiteral,
        copyLoc(definition.loc))
        .withAnnotations(expandAnnotations(definition.annotations));
  }

  @Override
  public Definition visit(RegisterDefinition definition) {
    var id = resolvePlaceholderOrIdentifier(definition.identifier);
    return new RegisterDefinition(id, definition.typeLiteral, copyLoc(definition.loc))
        .withAnnotations(expandAnnotations(definition.annotations));
  }

  @Override
  public Definition visit(RegisterFileDefinition definition) {
    var id = resolvePlaceholderOrIdentifier(definition.identifier);
    return new RegisterFileDefinition(id, definition.typeLiteral,
        copyLoc(definition.loc)).withAnnotations(expandAnnotations(definition.annotations));
  }

  @Override
  public Definition visit(InstructionDefinition definition) {
    var identifier = resolvePlaceholderOrIdentifier(definition.identifier);
    var typeId = resolvePlaceholderOrIdentifier(definition.typeIdentifier);
    var behavior = definition.behavior.accept(this);

    return new InstructionDefinition(identifier, typeId, behavior, copyLoc(definition.loc))
        .withAnnotations(expandAnnotations(definition.annotations));
  }

  @Override
  public Definition visit(PseudoInstructionDefinition definition) {
    var identifier = resolvePlaceholderOrIdentifier(definition.identifier);
    var statements = new ArrayList<>(definition.statements);
    statements.replaceAll(this::visit);

    return new PseudoInstructionDefinition(identifier, definition.kind,
        expandParams(definition.params), statements, copyLoc(definition.loc)
    ).withAnnotations(expandAnnotations(definition.annotations));
  }

  @Override
  public Definition visit(RelocationDefinition definition) {
    return new RelocationDefinition(definition.identifier, expandParams(definition.params),
        definition.resultTypeLiteral, expandExpr(definition.expr), copyLoc(definition.loc)
    ).withAnnotations(expandAnnotations(definition.annotations));
  }

  @Override
  public Definition visit(EncodingDefinition definition) {
    var instrId = resolvePlaceholderOrIdentifier(definition.instrIdentifier);
    var fieldEncodings = resolveEncs(definition.encodings);

    return new EncodingDefinition(instrId, fieldEncodings, copyLoc(definition.loc))
        .withAnnotations(expandAnnotations(definition.annotations));
  }

  @Override
  public Definition visit(AssemblyDefinition definition) {
    var identifiers = new ArrayList<>(definition.identifiers);
    identifiers.replaceAll(this::resolvePlaceholderOrIdentifier);
    return new AssemblyDefinition(identifiers, expandExpr(definition.expr), copyLoc(definition.loc))
        .withAnnotations(expandAnnotations(definition.annotations));
  }

  @Override
  public Definition visit(UsingDefinition definition) {
    var id = resolvePlaceholderOrIdentifier(definition.id);
    return new UsingDefinition(id, definition.typeLiteral, copyLoc(definition.loc))
        .withAnnotations(expandAnnotations(definition.annotations));
  }

  @Override
  public Definition visit(AbiPseudoInstructionDefinition definition) {
    return new AbiPseudoInstructionDefinition(definition.kind,
        resolvePlaceholderOrIdentifier(definition.target),
        copyLoc(definition.loc)
    ).withAnnotations(expandAnnotations(definition.annotations));
  }

  @Override
  public Definition visit(FunctionDefinition definition) {
    var name = resolvePlaceholderOrIdentifier(definition.name);
    var retType = (TypeLiteral) expandExpr(definition.retType);
    return new FunctionDefinition(name, expandParams(definition.params), retType,
        expandExpr(definition.expr), copyLoc(definition.loc)
    ).withAnnotations(expandAnnotations(definition.annotations));
  }

  @Override
  public Definition visit(AliasDefinition definition) {
    var id = resolvePlaceholderOrIdentifier(definition.id);
    var value = expandExpr(definition.value);
    return new AliasDefinition(id, definition.kind, definition.aliasType, definition.targetType,
        value, copyLoc(definition.loc)).withAnnotations(definition.annotations);
  }

  @Override
  public Definition visit(EnumerationDefinition definition) {
    var id = resolvePlaceholderOrIdentifier(definition.id);
    var entries = new ArrayList<>(definition.entries);
    entries.replaceAll(entry -> new EnumerationDefinition.Entry(entry.name,
        entry.value == null ? null : expandExpr(entry.value)));
    return new EnumerationDefinition(id, definition.enumType, entries, copyLoc(definition.loc))
        .withAnnotations(definition.annotations);
  }

  @Override
  public Definition visit(ExceptionDefinition definition) {
    var id = resolvePlaceholderOrIdentifier(definition.id);
    return new ExceptionDefinition(id, expandParams(definition.params),
        definition.statement.accept(this), copyLoc(definition.loc))
        .withAnnotations(definition.annotations);
  }

  @Override
  public Definition visit(PlaceholderDefinition definition) {
    var arg = resolveArg(definition.segments);
    return Objects.requireNonNullElse((Definition) arg, definition);
  }

  @Override
  public Definition visit(MacroInstanceDefinition definition) {
    try {
      var macro = resolveMacro(definition.macro);
      if (macro == null) {
        var arguments = expandNodes(definition.arguments);
        var placeholder = (MacroPlaceholder) definition.macro;
        var resolved = resolveArg(placeholder.segments());
        var newSegments =
            resolved == null ? placeholder.segments() : ((PlaceholderNode) resolved).segments;
        return new MacroInstanceDefinition(
            new MacroPlaceholder(placeholder.syntaxType(), newSegments), arguments,
            copyLoc(definition.loc));
      }
      assertValidMacro(macro, definition.location());
      var arguments =
          collectMacroParameters(macro, definition.arguments, definition.location());
      var body = (Definition) macro.body();
      var subpass = new MacroExpander(arguments, macroOverrides, copyLoc(definition.location()));
      return body.accept(subpass);
    } catch (MacroExpansionException e) {
      reportError(e.message, e.sourceLocation);
      return definition;
    }
  }

  @Override
  public Definition visit(MacroMatchDefinition definition) {
    var macroMatch = expandMacroMatch(definition.macroMatch);
    var resolved = resolveMacroMatch(macroMatch);
    if (resolved != null) {
      return (Definition) resolved;
    } else {
      return new MacroMatchDefinition(macroMatch);
    }
  }

  @Override
  public Definition visit(DefinitionList definition) {
    var items = expandDefinitions(definition.items);
    return new DefinitionList(items, definition.syntaxType, copyLoc(definition.location));
  }

  @Override
  public Definition visit(ModelDefinition definition) {
    var id = resolvePlaceholderOrIdentifier(definition.id);
    var boundModel = new ModelDefinition(id, definition.params, definition.body,
        definition.returnType, copyLoc(definition.loc));
    boundModel.boundArguments = new HashMap<>(definition.boundArguments);
    boundModel.boundArguments.putAll(args);
    return boundModel;
  }

  @Override
  public Definition visit(RecordTypeDefinition definition) {
    return new RecordTypeDefinition(definition.name, definition.recordType,
        copyLoc(definition.loc));
  }

  @Override
  public Definition visit(ModelTypeDefinition definition) {
    return new ModelTypeDefinition(definition.name, definition.projectionType,
        copyLoc(definition.loc));
  }

  @Override
  public Definition visit(ImportDefinition importDefinition) {
    return importDefinition;
  }

  @Override
  public Definition visit(ProcessDefinition processDefinition) {
    var id = resolvePlaceholderOrIdentifier(processDefinition.name);
    var templateParams = new ArrayList<>(processDefinition.templateParams);
    templateParams.replaceAll(
        templateParam -> new TemplateParam(templateParam.identifier(),
            templateParam.type,
            templateParam.value == null ? null : expandExpr(templateParam.value)));
    return new ProcessDefinition(id, templateParams, expandParams(processDefinition.inputs),
        expandParams(processDefinition.outputs), processDefinition.statement.accept(this),
        copyLoc(processDefinition.loc)).withAnnotations(
        expandAnnotations(processDefinition.annotations));
  }

  @Override
  public Definition visit(OperationDefinition operationDefinition) {
    var name = resolvePlaceholderOrIdentifier(operationDefinition.name);
    var resources = new ArrayList<>(operationDefinition.resources);
    resources.replaceAll(id -> (IsId) expandExpr((Expr) id));
    return new OperationDefinition(name, resources, copyLoc(operationDefinition.loc))
        .withAnnotations(expandAnnotations(operationDefinition.annotations));
  }

  @Override
  public Definition visit(Parameter definition) {
    var name = (Identifier) resolvePlaceholderOrIdentifier(definition.name);
    var type = (TypeLiteral) expandExpr(definition.typeLiteral);
    return new Parameter(name, type);
  }

  @Override
  public Definition visit(GroupDefinition groupDefinition) {
    return new GroupDefinition(
        resolvePlaceholderOrIdentifier(groupDefinition.name),
        groupDefinition.type == null ? null : (TypeLiteral) expandExpr(groupDefinition.type),
        groupDefinition.groupSequence,
        copyLoc(groupDefinition.loc)
    ).withAnnotations(expandAnnotations(groupDefinition.annotations));
  }

  @Override
  public Definition visit(ApplicationBinaryInterfaceDefinition definition) {
    return new ApplicationBinaryInterfaceDefinition(definition.id, definition.isa,
        expandDefinitions(definition.definitions), copyLoc(definition.loc)
    ).withAnnotations(expandAnnotations(definition.annotations));
  }

  @Override
  public Definition visit(AbiSequenceDefinition definition) {
    var statements = new ArrayList<>(definition.statements);
    statements.replaceAll(stmt -> (InstructionCallStatement) expandStatement(stmt));
    return new AbiSequenceDefinition(definition.kind, expandParams(definition.params), statements,
        copyLoc(definition.loc)
    ).withAnnotations(expandAnnotations(definition.annotations));
  }

  @Override
  public Definition visit(SpecialPurposeRegisterDefinition definition) {
    return new SpecialPurposeRegisterDefinition(
        definition.purpose, definition.exprs, copyLoc(definition.loc)
    ).withAnnotations(expandAnnotations(definition.annotations));
  }

  @Override
  public Definition visit(MicroProcessorDefinition definition) {
    var definitions = expandDefinitions(definition.definitions);
    return new MicroProcessorDefinition(definition.id, definition.implementedIsas, definition.abi,
        definitions, copyLoc(definition.loc)
    ).withAnnotations(expandAnnotations(definition.annotations));
  }

  @Override
  public Definition visit(PatchDefinition definition) {
    return new PatchDefinition(definition.generator, definition.handle, definition.reference,
        definition.source, copyLoc(definition.loc)
    ).withAnnotations(expandAnnotations(definition.annotations));
  }

  @Override
  public Definition visit(SourceDefinition definition) {
    return new SourceDefinition(definition.id, definition.source, copyLoc(definition.loc))
        .withAnnotations(expandAnnotations(definition.annotations));
  }

  @Override
  public Definition visit(CpuFunctionDefinition definition) {
    return new CpuFunctionDefinition(definition.id, definition.kind, definition.stopWithReference,
        expandExpr(definition.expr), copyLoc(definition.loc)
    ).withAnnotations(expandAnnotations(definition.annotations));
  }

  @Override
  public Definition visit(CpuProcessDefinition definition) {
    return new CpuProcessDefinition(definition.kind, definition.startupOutputs,
        definition.statement.accept(this), copyLoc(definition.loc)
    ).withAnnotations(expandAnnotations(definition.annotations));
  }

  @Override
  public Definition visit(MicroArchitectureDefinition definition) {
    return new MicroArchitectureDefinition(definition.id, definition.processor,
        expandDefinitions(definition.definitions), copyLoc(definition.loc)
    ).withAnnotations(expandAnnotations(definition.annotations));
  }

  @Override
  public Definition visit(MacroInstructionDefinition definition) {
    return new MacroInstructionDefinition(definition.kind, expandParams(definition.inputs),
        expandParams(definition.outputs), definition.statement.accept(this),
        copyLoc(definition.loc)
    ).withAnnotations(expandAnnotations(definition.annotations));
  }

  @Override
  public Definition visit(PortBehaviorDefinition definition) {
    return new PortBehaviorDefinition(definition.id, definition.kind,
        expandParams(definition.inputs), expandParams(definition.outputs),
        definition.statement.accept(this), copyLoc(definition.loc)
    ).withAnnotations(expandAnnotations(definition.annotations));
  }

  @Override
  public Definition visit(PipelineDefinition definition) {
    return new PipelineDefinition(definition.id, expandParams(definition.outputs),
        definition.statement.accept(this), copyLoc(definition.loc)
    ).withAnnotations(expandAnnotations(definition.annotations));
  }

  @Override
  public Definition visit(StageDefinition definition) {
    return new StageDefinition(definition.id, expandParams(definition.outputs),
        definition.statement.accept(this), copyLoc(definition.loc)
    ).withAnnotations(expandAnnotations(definition.annotations));
  }

  @Override
  public Definition visit(CacheDefinition definition) {
    return new CacheDefinition(definition.id, definition.sourceType, definition.targetType,
        copyLoc(definition.loc)
    ).withAnnotations(definition.annotations);
  }

  @Override
  public Definition visit(LogicDefinition definition) {
    return new LogicDefinition(definition.id, copyLoc(definition.loc))
        .withAnnotations(expandAnnotations(definition.annotations));
  }

  @Override
  public Definition visit(SignalDefinition definition) {
    return new SignalDefinition(definition.id, definition.type, copyLoc(definition.loc))
        .withAnnotations(expandAnnotations(definition.annotations));
  }

  @Override
  public Definition visit(AsmDescriptionDefinition definition) {
    return new AsmDescriptionDefinition(definition.id, definition.abi, definition.modifiers,
        definition.directives, definition.rules, definition.commonDefinitions,
        copyLoc(definition.loc)).withAnnotations(expandAnnotations(definition.annotations));
  }

  @Override
  public Definition visit(AsmModifierDefinition definition) {
    return new AsmModifierDefinition(definition.stringLiteral, definition.isa,
        definition.relocation, copyLoc(definition.loc));
  }

  @Override
  public Definition visit(AsmDirectiveDefinition definition) {
    return new AsmDirectiveDefinition(definition.stringLiteral, definition.builtinDirective,
        copyLoc(definition.loc));
  }

  @Override
  public Definition visit(AsmGrammarRuleDefinition definition) {
    return new AsmGrammarRuleDefinition(definition.id, definition.asmTypeDefinition,
        definition.alternatives,
        copyLoc(definition.loc));
  }

  @Override
  public Definition visit(AsmGrammarAlternativesDefinition definition) {
    return new AsmGrammarAlternativesDefinition(definition.alternatives,
        copyLoc(definition.loc));
  }

  @Override
  public Definition visit(AsmGrammarElementDefinition definition) {
    return new AsmGrammarElementDefinition(definition.localVar, definition.attribute,
        definition.isPlusEqualsAttributeAssign, definition.asmLiteral, definition.groupAlternatives,
        definition.optionAlternatives, definition.repetitionAlternatives,
        definition.semanticPredicate, definition.groupAsmTypeDefinition, copyLoc(definition.loc));
  }

  @Override
  public Definition visit(AsmGrammarLocalVarDefinition definition) {
    return new AsmGrammarLocalVarDefinition(definition.id, definition.asmLiteral,
        copyLoc(definition.loc));
  }

  @Override
  public Definition visit(AsmGrammarLiteralDefinition definition) {
    return new AsmGrammarLiteralDefinition(definition.id, definition.parameters,
        definition.stringLiteral, definition.asmTypeDefinition, copyLoc(definition.loc));
  }

  @Override
  public Definition visit(AsmGrammarTypeDefinition definition) {
    return new AsmGrammarTypeDefinition(definition.id, copyLoc(definition.loc));
  }

  @Override
  public BlockStatement visit(BlockStatement blockStatement) {
    return new BlockStatement(expandStatements(blockStatement.statements),
        copyLoc(blockStatement.location));
  }

  @Override
  public Statement visit(LetStatement letStatement) {
    var valueExpression = expandExpr(letStatement.valueExpr);
    var body = letStatement.body.accept(this);
    return new LetStatement(letStatement.identifiers, valueExpression, body,
        copyLoc(letStatement.location));
  }

  @Override
  public Statement visit(IfStatement ifStatement) {
    var condition = expandExpr(ifStatement.condition);
    var thenStmt = ifStatement.thenStmt.accept(this);
    var elseStmt = ifStatement.elseStmt == null ? null : ifStatement.elseStmt.accept(this);
    return new IfStatement(condition, thenStmt, elseStmt, copyLoc(ifStatement.location));
  }

  @Override
  public Statement visit(AssignmentStatement assignmentStatement) {
    var target = expandExpr(assignmentStatement.target);
    var valueExpr = expandExpr(assignmentStatement.valueExpression);
    return new AssignmentStatement(target, valueExpr);
  }

  @Override
  public Statement visit(RaiseStatement raiseStatement) {
    return new RaiseStatement(raiseStatement.statement.accept(this),
        copyLoc(raiseStatement.location));
  }

  @Override
  public Statement visit(CallStatement callStatement) {
    return new CallStatement(expandExpr(callStatement.expr));
  }

  @Override
  public Statement visit(PlaceholderStatement statement) {
    var arg = resolveArg(statement.segments);
    return Objects.requireNonNullElse((Statement) arg,
        new PlaceholderStatement(statement.segments, statement.type, copyLoc(statement.loc)));
  }

  @Override
  public Statement visit(MacroInstanceStatement stmt) {
    try {
      var macro = resolveMacro(stmt.macro);
      if (macro == null) {
        var arguments = expandNodes(stmt.arguments);
        var placeholder = (MacroPlaceholder) stmt.macro;
        var resolved = resolveArg(placeholder.segments());
        var newSegments =
            resolved == null ? placeholder.segments() : ((PlaceholderNode) resolved).segments;
        return new MacroInstanceStatement(
            new MacroPlaceholder(placeholder.syntaxType(), newSegments), arguments,
            copyLoc(stmt.loc));
      }
      assertValidMacro(macro, copyLoc(stmt.location()));
      var arguments = collectMacroParameters(macro, stmt.arguments, copyLoc(stmt.location()));
      var body = (Statement) macro.body();
      var subpass = new MacroExpander(arguments, macroOverrides, copyLoc(stmt.location()));
      return body.accept(subpass);
    } catch (MacroExpansionException e) {
      reportError(e.message, e.sourceLocation);
      return stmt;
    }
  }

  @Override
  public Statement visit(MacroMatchStatement macroMatchStatement) {
    var macroMatch = expandMacroMatch(macroMatchStatement.macroMatch);
    var resolved = resolveMacroMatch(macroMatch);
    if (resolved != null) {
      return (Statement) resolved;
    } else {
      return new MacroMatchStatement(macroMatch);
    }
  }

  @Override
  public Statement visit(MatchStatement matchStatement) {
    var candidate = expandExpr(matchStatement.candidate);
    var defaultResult =
        matchStatement.defaultResult == null ? null : matchStatement.defaultResult.accept(this);
    var cases = new ArrayList<>(matchStatement.cases);
    cases.replaceAll(matchCase -> new MatchStatement.Case(expandExprs(matchCase.patterns),
        matchCase.result.accept(this)));
    return new MatchStatement(candidate, cases, defaultResult, copyLoc(matchStatement.loc));
  }

  @Override
  public Statement visit(StatementList statementList) {
    var items = expandStatements(statementList.items);
    return new StatementList(items, copyLoc(statementList.location()));
  }

  @Override
  public InstructionCallStatement visit(InstructionCallStatement instructionCallStatement) {
    var id = resolvePlaceholderOrIdentifier(instructionCallStatement.id);
    var namedArguments = new ArrayList<>(instructionCallStatement.namedArguments);
    namedArguments.replaceAll(namedArgument ->
        new InstructionCallStatement.NamedArgument(namedArgument.name,
            expandExpr(namedArgument.value)));
    var unnamedArguments = expandExprs(instructionCallStatement.unnamedArguments);
    return new InstructionCallStatement(id, namedArguments, unnamedArguments,
        copyLoc(instructionCallStatement.loc));
  }

  @Override
  public Statement visit(LockStatement lockStatement) {
    return new LockStatement(
        expandExpr(lockStatement.expr),
        expandStatement(lockStatement.statement),
        copyLoc(lockStatement.loc)
    );
  }

  @Override
  public Statement visit(ForallStatement forallStatement) {
    var indices = new ArrayList<>(forallStatement.indices);
    indices.replaceAll(
        index -> new ForallStatement.Index(index.identifier(), expandExpr(index.domain)));
    var statement = expandStatement(forallStatement.statement);
    return new ForallStatement(indices, statement, copyLoc(forallStatement.loc));
  }

  private void assertValidMacro(Macro macro, SourceLocation sourceLocation)
      throws MacroExpansionException {
    if (macro.returnType() == BasicSyntaxType.INVALID) {
      throw new MacroExpansionException(
          "Skipped expanding macro %s due to previous error".formatted(macro.name().name),
          sourceLocation);
    }
  }

  Map<String, Node> collectMacroParameters(Macro macro, List<Node> actualParams,
                                           SourceLocation instanceLoc)
      throws MacroExpansionException {
    var formalParams = macro.params();
    if (formalParams.size() != actualParams.size()) {
      throw new MacroExpansionException(
          "The macro `%s` expects %d arguments but %d were provided.".formatted(macro.name().name,
              formalParams.size(), actualParams.size()), instanceLoc);
    }
    var arguments = new HashMap<>(macro.boundArguments());
    for (int i = 0; i < formalParams.size(); i++) {
      var formalParam = formalParams.get(i);
      var actualParam = expandNode(actualParams.get(i));
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

  private IdentifierOrPlaceholder resolvePlaceholderOrIdentifier(
      IdentifierOrPlaceholder idOrPlaceholder) {
    if (idOrPlaceholder instanceof Identifier id) {
      return (IdentifierOrPlaceholder) expandExpr(id);
    } else if (idOrPlaceholder instanceof Expr expr) {
      return (IdentifierOrPlaceholder) expandExpr(expr);
    }
    throw new IllegalStateException("Unknown resolved placeholder type " + idOrPlaceholder);
  }

  private EncodingDefinition.EncsNode resolveEncs(EncodingDefinition.EncsNode encs) {
    var encodings = new ArrayList<IsEncs>(encs.items.size());
    for (var enc : encs.items) {
      encodings.addAll(resolveEnc(enc));
    }
    return new EncodingDefinition.EncsNode(encodings, encs.loc);
  }

  private List<IsEncs> resolveEnc(IsEncs encoding) {
    if (encoding instanceof EncodingDefinition.EncodingField encodingField) {
      return List.of(new EncodingDefinition.EncodingField(encodingField.field,
          expandExpr(encodingField.value)));
    } else if (encoding instanceof EncodingDefinition.EncsNode encodings) {
      var encs = new ArrayList<IsEncs>(encodings.items.size());
      for (IsEncs enc : encodings.items) {
        encs.addAll(resolveEnc(enc));
      }
      return encs;
    } else if (encoding instanceof PlaceholderNode placeholder) {
      var expanded = expand(placeholder);
      if (expanded instanceof EncodingDefinition.EncsNode encs) {
        return encs.items;
      }
    } else if (encoding instanceof MacroMatchNode macroMatchNode) {
      var expanded = expand(macroMatchNode);
      if (expanded instanceof EncodingDefinition.EncsNode encs) {
        return encs.items;
      } else {
        return List.of((IsEncs) expanded);
      }
    } else if (encoding instanceof MacroInstanceNode macroInstanceNode) {
      var expanded = expand(macroInstanceNode);
      if (expanded instanceof EncodingDefinition.EncsNode encs) {
        return encs.items;
      } else {
        return List.of((IsEncs) expanded);
      }
    }
    return List.of(encoding);
  }

  private MacroMatch expandMacroMatch(MacroMatch macroMatch) {
    var choices = new ArrayList<>(macroMatch.choices());
    choices.replaceAll(choice -> {
      var patterns = new ArrayList<>(choice.patterns());
      patterns.replaceAll(pattern -> new MacroMatch.Pattern(
          expandNode(pattern.candidate()), pattern.comparison(), expandNode(pattern.match()))
      );
      return new MacroMatch.Choice(patterns, expandNode(choice.result()));
    });
    var defChoice = expandNode(macroMatch.defaultChoice());
    return new MacroMatch(macroMatch.resultType(), choices, defChoice,
        copyLoc(macroMatch.sourceLocation()));
  }

  private @Nullable Node resolveMacroMatch(MacroMatch macroMatch) {
    for (var choice : macroMatch.choices()) {
      for (var pattern : choice.patterns()) {
        var candidate = expandNode(pattern.candidate());
        if (isReplacementNode(candidate)) {
          // Cannot fully evaluate macro match, as at least one placeholder remains
          return null;
        }
        var equals = candidate.equals(pattern.match());
        var shouldEqual = pattern.comparison() == MacroMatch.Comparison.EQUAL;
        if (equals == shouldEqual) {
          return expandNode(choice.result());
        }
      }
    }
    return expandNode(macroMatch.defaultChoice());
  }

  private @Nullable Node resolveArg(List<String> segments) {
    Node arg = args.get(segments.get(0));
    if (arg == null) {
      return null;
    }
    if (segments.size() > 1 && !(arg instanceof RecordInstance)) {
      return null;
    }
    for (int i = 1; i < segments.size(); i++) {
      var nextName = segments.get(i);
      var tuple = (RecordInstance) arg;
      for (int j = 0; j < tuple.type.entries.size(); j++) {
        if (tuple.type.entries.get(j).name().equals(nextName)) {
          arg = tuple.entries.get(j);
          break;
        }
      }
    }
    return arg;
  }

  private @Nullable Macro resolveMacro(MacroOrPlaceholder macroOrPlaceholder) {
    if (macroOrPlaceholder instanceof Macro macro) {
      return macro;
    }
    var arg = resolveArg(((MacroPlaceholder) macroOrPlaceholder).segments());
    if (arg instanceof MacroReference macroReference) {
      return macroReference.macro;
    }
    return null;
  }

  private Node expand(PlaceholderNode node) {
    return Objects.requireNonNullElse(resolveArg(node.segments), node);
  }

  private Node expand(MacroInstanceNode node) {
    var macro = resolveMacro(node.macro);
    if (macro == null) {
      // Macro reference passed down multiple layers - let parent layer expand
      var arguments = expandNodes(node.arguments);
      var placeholder = (MacroPlaceholder) node.macro;
      var resolved = resolveArg(placeholder.segments());
      var newSegments =
          resolved == null ? placeholder.segments() : ((PlaceholderNode) resolved).segments;
      return new MacroInstanceNode(new MacroPlaceholder(placeholder.syntaxType(), newSegments),
          arguments, node.loc);
    }

    try {
      assertValidMacro(macro, node.location());
      var arguments = collectMacroParameters(macro, node.arguments, node.location());
      var subpass = new MacroExpander(arguments, macroOverrides, copyLoc(node.loc));
      return subpass.expandNode(macro.body());
    } catch (MacroExpansionException e) {
      reportError(e.message, e.sourceLocation);
      return node;
    }
  }

  private Node expand(MacroMatchNode node) {
    var macroMatch = expandMacroMatch(node.macroMatch);
    var resolved = resolveMacroMatch(macroMatch);
    return Objects.requireNonNullElseGet(resolved, () -> new MacroMatchNode(macroMatch));
  }

  private List<Parameter> expandParams(List<Parameter> params) {
    var expandedParams = new ArrayList<>(params);
    expandedParams.replaceAll(param ->
        new Parameter(param.identifier(), (TypeLiteral) expandExpr(param.typeLiteral)));
    return expandedParams;
  }

  private void reportError(String error, SourceLocation location) {
    errors.add(error(error, location).build());
  }

  private boolean isReplacementNode(Node node) {
    return node instanceof PlaceholderNode || node instanceof PlaceholderDefinition
        || node instanceof PlaceholderExpr || node instanceof PlaceholderStatement
        || node instanceof MacroMatchNode || node instanceof MacroMatchDefinition
        || node instanceof MacroMatchExpr || node instanceof MacroMatchStatement
        || node instanceof MacroInstanceNode || node instanceof MacroInstanceDefinition
        || node instanceof MacroInstanceExpr || node instanceof MacroInstanceStatement
        || node instanceof ExtendIdExpr || node instanceof IdToStrExpr;

  }

  /**
   * Copies the location and annotates with the location of the macro we are currently expanding.
   *
   * @param loc to be copied
   */
  private SourceLocation copyLoc(SourceLocation loc) {
    // FIXME: At the time of writing we sometimes issued the pass twice resulting in double
    // reporting of expandedFrom
    if (expandingFrom == null || Objects.equals(loc, expandingFrom)) {
      return loc;
    }

    if (loc.expandedFrom() == null) {
      return new SourceLocation(loc.uri(), loc.begin(), loc.end(), expandingFrom);
    }

    return new SourceLocation(loc.uri(), loc.begin(), loc.end(), copyLoc(loc.expandedFrom()));
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
