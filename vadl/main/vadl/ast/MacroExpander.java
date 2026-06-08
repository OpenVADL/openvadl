// SPDX-FileCopyrightText : © 2025-2026 TU Wien <vadl@tuwien.ac.at>
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

import static java.util.Objects.requireNonNull;
import static vadl.error.Diagnostic.error;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;
import vadl.ast.nodes.AbiClangNumericTypeDefinition;
import vadl.ast.nodes.AbiClangTypeDefinition;
import vadl.ast.nodes.AbiSequenceDefinition;
import vadl.ast.nodes.AbiSpecialPurposeInstructionDefinition;
import vadl.ast.nodes.AliasDefinition;
import vadl.ast.nodes.AnnotationDefinition;
import vadl.ast.nodes.ApplicationBinaryInterfaceDefinition;
import vadl.ast.nodes.AsIdExpr;
import vadl.ast.nodes.AsStrExpr;
import vadl.ast.nodes.AsmDescriptionDefinition;
import vadl.ast.nodes.AsmDirectiveDefinition;
import vadl.ast.nodes.AsmGrammarAlternativesDefinition;
import vadl.ast.nodes.AsmGrammarElementDefinition;
import vadl.ast.nodes.AsmGrammarLiteralDefinition;
import vadl.ast.nodes.AsmGrammarLocalVarDefinition;
import vadl.ast.nodes.AsmGrammarRuleDefinition;
import vadl.ast.nodes.AsmGrammarTypeDefinition;
import vadl.ast.nodes.AsmModifierDefinition;
import vadl.ast.nodes.AssemblyDefinition;
import vadl.ast.nodes.AssignmentStatement;
import vadl.ast.nodes.BasicSyntaxType;
import vadl.ast.nodes.BinaryExpr;
import vadl.ast.nodes.BinaryLiteral;
import vadl.ast.nodes.BlockStatement;
import vadl.ast.nodes.BoolLiteral;
import vadl.ast.nodes.CacheDefinition;
import vadl.ast.nodes.CallIndexExpr;
import vadl.ast.nodes.CallStatement;
import vadl.ast.nodes.CastExpr;
import vadl.ast.nodes.ConstantDefinition;
import vadl.ast.nodes.CounterDefinition;
import vadl.ast.nodes.CpuFunctionDefinition;
import vadl.ast.nodes.CpuMemoryRegionDefinition;
import vadl.ast.nodes.CpuProcessDefinition;
import vadl.ast.nodes.Definition;
import vadl.ast.nodes.DefinitionList;
import vadl.ast.nodes.DefinitionVisitor;
import vadl.ast.nodes.DerivedFormatField;
import vadl.ast.nodes.EncodingDefinition;
import vadl.ast.nodes.EncodingFormatField;
import vadl.ast.nodes.EnumerationDefinition;
import vadl.ast.nodes.ExceptionDefinition;
import vadl.ast.nodes.ExistsInExpr;
import vadl.ast.nodes.ExistsInThenExpr;
import vadl.ast.nodes.ExpandedAliasDefSequenceCallExpr;
import vadl.ast.nodes.ExpandedSequenceCallExpr;
import vadl.ast.nodes.Expr;
import vadl.ast.nodes.ExprVisitor;
import vadl.ast.nodes.ForallExpr;
import vadl.ast.nodes.ForallIndex;
import vadl.ast.nodes.ForallStatement;
import vadl.ast.nodes.ForallThenExpr;
import vadl.ast.nodes.FormatDefinition;
import vadl.ast.nodes.FormatField;
import vadl.ast.nodes.FunctionDefinition;
import vadl.ast.nodes.GroupDefinition;
import vadl.ast.nodes.GroupedExpr;
import vadl.ast.nodes.Identifier;
import vadl.ast.nodes.IdentifierOrPlaceholder;
import vadl.ast.nodes.IdentifierPath;
import vadl.ast.nodes.IfExpr;
import vadl.ast.nodes.IfStatement;
import vadl.ast.nodes.ImportDefinition;
import vadl.ast.nodes.InstructionCallStatement;
import vadl.ast.nodes.InstructionDefinition;
import vadl.ast.nodes.InstructionSetDefinition;
import vadl.ast.nodes.IntegerLiteral;
import vadl.ast.nodes.IsBinOp;
import vadl.ast.nodes.IsEncs;
import vadl.ast.nodes.IsId;
import vadl.ast.nodes.IsUnOp;
import vadl.ast.nodes.LetExpr;
import vadl.ast.nodes.LetStatement;
import vadl.ast.nodes.LockStatement;
import vadl.ast.nodes.LogicDefinition;
import vadl.ast.nodes.Macro;
import vadl.ast.nodes.MacroInstanceDefinition;
import vadl.ast.nodes.MacroInstanceExpr;
import vadl.ast.nodes.MacroInstanceNode;
import vadl.ast.nodes.MacroInstanceStatement;
import vadl.ast.nodes.MacroInstructionDefinition;
import vadl.ast.nodes.MacroMatch;
import vadl.ast.nodes.MacroMatchDefinition;
import vadl.ast.nodes.MacroMatchExpr;
import vadl.ast.nodes.MacroMatchNode;
import vadl.ast.nodes.MacroMatchStatement;
import vadl.ast.nodes.MacroOrPlaceholder;
import vadl.ast.nodes.MacroPlaceholder;
import vadl.ast.nodes.MacroReference;
import vadl.ast.nodes.MatchExpr;
import vadl.ast.nodes.MatchStatement;
import vadl.ast.nodes.MemoryDefinition;
import vadl.ast.nodes.MicroArchitectureDefinition;
import vadl.ast.nodes.ModelDefinition;
import vadl.ast.nodes.ModelTypeDefinition;
import vadl.ast.nodes.Node;
import vadl.ast.nodes.OperationDefinition;
import vadl.ast.nodes.Parameter;
import vadl.ast.nodes.PatchDefinition;
import vadl.ast.nodes.PipelineDefinition;
import vadl.ast.nodes.PlaceholderDefinition;
import vadl.ast.nodes.PlaceholderExpr;
import vadl.ast.nodes.PlaceholderNode;
import vadl.ast.nodes.PlaceholderStatement;
import vadl.ast.nodes.PortBehaviorDefinition;
import vadl.ast.nodes.PredicateFormatField;
import vadl.ast.nodes.ProcessDefinition;
import vadl.ast.nodes.ProcessorDefinition;
import vadl.ast.nodes.PseudoInstructionDefinition;
import vadl.ast.nodes.RaiseStatement;
import vadl.ast.nodes.RangeExpr;
import vadl.ast.nodes.RangeFormatField;
import vadl.ast.nodes.RecordInstance;
import vadl.ast.nodes.RecordTypeDefinition;
import vadl.ast.nodes.RegisterDefinition;
import vadl.ast.nodes.RelocationDefinition;
import vadl.ast.nodes.ResourceReferenceExression;
import vadl.ast.nodes.SequenceCallExpr;
import vadl.ast.nodes.SignalDefinition;
import vadl.ast.nodes.SourceDefinition;
import vadl.ast.nodes.SpecialPurposeRegisterDefinition;
import vadl.ast.nodes.StageDefinition;
import vadl.ast.nodes.StageOutputDefinition;
import vadl.ast.nodes.Statement;
import vadl.ast.nodes.StatementList;
import vadl.ast.nodes.StatementVisitor;
import vadl.ast.nodes.StringLiteral;
import vadl.ast.nodes.SymbolExpr;
import vadl.ast.nodes.TemplateParam;
import vadl.ast.nodes.TypeLiteral;
import vadl.ast.nodes.TypedFormatField;
import vadl.ast.nodes.UnaryExpr;
import vadl.ast.nodes.UsingDefinition;
import vadl.ast.nodes.WildcardLiteral;
import vadl.error.Diagnostic;
import vadl.utils.RopeList;
import vadl.utils.SourceLocation;

/**
 * Expands macro invocations and placeholders with the expanded body of a macro.
 * Id-returning macros can be overridden via CLI arguments or with import statements.
 * Since binary expression reordering can depend on operator placeholders, this class also
 * reorders any encountered binary expressions.<br>
 * Before: An AST optionally containing macro instances, placeholders, etc.
 * Any instances of BinaryExpr must be left-sided, as originally parsed.<br>
 * After: An AST containing no special nodes (macro instance, placeholder, node lists).
 * Any instances of BinaryExpr are ordered according to operator precedence.
 *
 * @see BinaryExpr#reorder(BinaryExpr)
 */
@SuppressWarnings("OverloadMethodsDeclarationOrder")
class MacroExpander
    implements ExprVisitor<Expr>, DefinitionVisitor<Definition>, StatementVisitor<Statement> {
  final Map<String, Node> args;
  final Map<String, Identifier> macroOverrides;
  final List<Diagnostic> errors = new ArrayList<>();
  @Nullable
  final RopeList<SourceLocation.DirectLocation> expandingFrom;

  MacroExpander(Map<String, Node> args, Map<String, Identifier> macroOverrides,
                @Nullable List<SourceLocation.DirectLocation> expandingFrom) {
    this.args = args;
    this.macroOverrides = macroOverrides;
    this.expandingFrom = expandingFrom == null || expandingFrom.isEmpty()
        ? null
        : RopeList.of(expandingFrom);
  }

  /**
   * Expands the given expr and, if applicable, performs binary expression reordering on it.
   * Since binary expression reordering is absolutely necessary to preserve the original semantics,
   * prefer this method to calling {@code expr.accept(this);} directly.
   * However, to prevent O(n²) performance, this should never be called during the macro expansion
   * of a binary expression itself.
   *
   * @param expr The expression to perform macro expansion on
   * @return An expanded and optionally reorder expression
   * @see BinaryExpr#reorder(BinaryExpr)
   */
  private Expr expandExprRaw(Expr expr) {
    var result = expr.accept(this);
    //if (!errors.isEmpty()) {
    // throw new DiagnosticList(errors);
    //}
    if (result instanceof BinaryExpr binaryExpr && !binaryExpr.hasBeenReordered) {
      return ParserUtils.reorderBinaryExpr(binaryExpr);
    }
    return result;
  }

  @SuppressWarnings("unchecked")
  private <T> T expandExpr(T expr) {
    return (T) expandExprRaw((Expr) expr);
  }

  private <T> List<T> expandExprs(List<T> expressions) {
    var result = new ArrayList<T>(expressions.size());
    for (var expr : expressions) {
      result.add(expandExpr(expr));
    }
    return result;
  }

  @SuppressWarnings("unchecked")
  private <T extends Statement> T expandStatement(T statement) {
    var result = statement.accept(this);
    return (T) result;
  }

  /**
   * Expands all statements in the given list.
   * If a definition expands to a {@link StatementList}, its items are flattened into the result.
   *
   * @param statements The list of statements to expand
   * @return A list of expanded and flattened statements
   */
  @SuppressWarnings("unchecked")
  private <T extends Statement> List<T> expandStatements(List<T> statements) {
    var stmts = new ArrayList<T>(statements.size());
    for (var statement : statements) {
      var expanded = expandStatement(statement);
      if (expanded instanceof StatementList list) {
        stmts.addAll((List<T>) list.items);
      } else {
        stmts.add(expanded);
      }
    }
    return stmts;
  }

  @SuppressWarnings("unchecked")
  private <T extends Definition> T expandDefinition(T def) {
    var result = def.accept(this)
        .withAnnotations(expandAnnotations(def.annotations));
    return (T) result;
  }

  /**
   * Expands all definitions in the given list.
   * If a definition expands to a {@link DefinitionList}, its items are flattened into the result.
   *
   * @param definitions The list of definitions to expand
   * @return A list of expanded and flattened definitions
   */
  @SuppressWarnings("unchecked")
  private <T extends Definition> List<T> expandDefinitions(List<T> definitions) {
    var defs = new ArrayList<Definition>(definitions.size());
    for (var def : definitions) {
      var expanded = expandDefinition(def);
      if (expanded instanceof DefinitionList list) {
        defs.addAll(list.items);
      } else {
        defs.add(expanded);
      }
    }
    return (List<T>) defs;
  }

  private CallIndexExpr.Arguments expandArgs(CallIndexExpr.Arguments args) {
    var values = new ArrayList<Expr>(args.values.size());
    for (var value : args.values) {
      values.add(expandExpr(value));
    }
    return new CallIndexExpr.Arguments(values, copyLoc(args.location));
  }

  private List<AnnotationDefinition> expandAnnotations(List<AnnotationDefinition> annotations) {
    var result = new ArrayList<AnnotationDefinition>(annotations.size());
    for (var a : annotations) {
      result.add((AnnotationDefinition) a.accept(this));
    }
    return result;
  }

  public <T extends Node> T expandNode(T node, Ast ast) {
    return ast.withPassTiming("Macro Expanding", () -> expandNode(node));
  }

  @SuppressWarnings("unchecked")
  private <T extends Node> T expandNode(T node) {
    return (T) switch (node) {
      case Expr expr -> expandExpr(expr);
      case Definition definition -> expandDefinition(definition);
      case Statement statement -> expandStatement(statement);
      case RecordInstance recordInstance -> {
        var entries = new ArrayList<Node>(recordInstance.entries.size());
        for (var entry : recordInstance.entries) {
          entries.add(expandNode(entry));
        }
        yield new RecordInstance(recordInstance.type, entries, recordInstance.sourceLocation);
      }
      case EncodingDefinition.EncsNode encs -> expandEncs(encs);
      case PlaceholderNode placeholderNode -> expand(placeholderNode);
      case MacroInstanceNode macroInstanceNode -> expand(macroInstanceNode);
      case MacroMatchNode macroMatchNode -> expand(macroMatchNode);
      case null, default -> node;
    };
  }

  private List<Node> expandNodes(List<Node> nodes) {
    var copy = new ArrayList<>(nodes);
    copy.replaceAll(this::expandNode);
    return copy;
  }

  /**
   * Expands assembly definitions with multiple identifiers into multiple definitions.
   * Not quite a macro to expand, but the assemblies with multiple identifiers kinda behave like
   * macros.
   *
   * @param definition to be expanded.
   * @return a list of the expanded definitions.
   */
  public List<AssemblyDefinition> expandAssemblies(AssemblyDefinition definition) {
    var result = new ArrayList<AssemblyDefinition>(definition.identifiers.size());
    for (var identifier : definition.identifiers) {
      result.add(new AssemblyDefinition(
          new ArrayList<>(List.of(identifier)),
          expandExpr(definition.expr),
          definition.loc
      ));
    }
    return result;
  }

  @Override
  public Expr visit(Identifier expr) {
    return new Identifier(expr.name, copyLoc(expr.location()));
  }

  @Override
  public Expr visit(BinaryExpr expr) {
    var expanded = new BinaryExpr(
        expandExpr(expr.left),
        (IsBinOp) expandNode((Node) expr.operator),
        expandExpr(expr.right)
    );

    expanded.hasBeenReordered = expr.hasBeenReordered;
    return expanded;
  }

  @Override
  public Expr visit(GroupedExpr expr) {
    return new GroupedExpr(expandExprs(expr.expressions), copyLoc(expr.loc));
  }

  @Override
  public Expr visit(IntegerLiteral expr) {
    return expr.copyWithLocation(copyLoc(expr.location()));
  }

  @Override
  public Expr visit(WildcardLiteral expr) {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public Expr visit(BinaryLiteral expr) {
    return expr.copyWithLocation(copyLoc(expr.loc));
  }

  @Override
  public Expr visit(BoolLiteral expr) {
    return new BoolLiteral(expr.value, copyLoc(expr.loc));
  }

  @Override
  public Expr visit(StringLiteral expr) {
    return expr.copyWithLocation(copyLoc(expr.loc));
  }

  @Override
  public Expr visit(PlaceholderExpr expr) {
    Node arg = resolveArg(expr.segments);
    if (!(arg instanceof Expr argExpr)) {
      return expr;
    }
    return argExpr;
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
      var subpass =
          new MacroExpander(arguments, macroOverrides, copyLoc(expr.loc).fullExpandedFromStack());
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
    return new TypeLiteral(
        expandExpr(expr.baseType),
        expandExprs(expr.sizeIndices),
        copyLoc(expr.location()));
  }

  @Override
  public Expr visit(IdentifierPath expr) {
    return new IdentifierPath(expandExprs(expr.segments));
  }

  @Override
  public Expr visit(UnaryExpr expr) {
    return new UnaryExpr((IsUnOp) expandNode((Node) expr.operator), expandExpr(expr.operand));
  }

  @Override
  public Expr visit(CallIndexExpr expr) {
    var argsIndices = new ArrayList<CallIndexExpr.Arguments>(expr.argsIndices.size());
    for (var argsIndex : expr.argsIndices) {
      argsIndices.add(expandArgs(argsIndex));
    }
    var subCalls = new ArrayList<CallIndexExpr.SubCall>(expr.subCalls.size());
    for (var subCall : expr.subCalls) {
      var subCallArgsIndices = new ArrayList<CallIndexExpr.Arguments>(subCall.argsIndices.size());
      for (var argsIndex : subCall.argsIndices) {
        subCallArgsIndices.add(expandArgs(argsIndex));
      }
      subCalls.add(new CallIndexExpr.SubCall(expandExpr(subCall.id), subCallArgsIndices));
    }

    return new CallIndexExpr(
        expandExpr(expr.target),
        argsIndices,
        subCalls,
        copyLoc(expr.location)
    );
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
    return new LetExpr(
        expandExprs(expr.identifiers),
        expandExpr(expr.valueExpr),
        expandExpr(expr.body),
        copyLoc(expr.location));
  }

  @Override
  public Expr visit(CastExpr expr) {
    return new CastExpr(expandExpr(expr.value), expandExpr(requireNonNull(expr.typeLiteral)));
  }

  @Override
  public Expr visit(SymbolExpr expr) {
    return new SymbolExpr(expandExpr(expr.path), expandExpr(expr.size), copyLoc(expr.location));
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
    var cases = new ArrayList<MatchExpr.Case>(expr.cases.size());
    for (var matchCase : expr.cases) {
      cases.add(new MatchExpr.Case(expandExprs(matchCase.patterns), expandExpr(matchCase.result)));
    }
    return new MatchExpr(
        expandExpr(expr.candidate),
        cases,
        expandExpr(expr.defaultResult),
        copyLoc(expr.loc)
    );
  }

  @Nullable
  private String concatStringifyExpressions(Expr origin, List<Expr> expressions) {
    var nameBuilder = new StringBuilder();
    for (var inner : expressions) {
      if (inner instanceof Identifier id) {
        nameBuilder.append(id.name);
      } else if (inner instanceof StringLiteral string) {
        nameBuilder.append(string.value);
      } else if (inner instanceof IntegerLiteral integerLiteral) {
        nameBuilder.append(integerLiteral.token);
      } else if (inner instanceof BinaryLiteral binaryLiteral) {
        nameBuilder.append(binaryLiteral.token);
      } else if (inner instanceof BoolLiteral bool) {
        nameBuilder.append(bool.value);
      } else if (inner instanceof PlaceholderExpr
          || inner instanceof AsIdExpr || inner instanceof AsStrExpr) {
        // Will be expanded as soon as the used placeholders are bound
        return null;
      } else {
        reportError("Unsupported '%s' parameter %s".formatted(origin.nodeName(), inner.nodeName()),
            inner.location());
        nameBuilder.append(inner);
      }
    }

    return nameBuilder.toString();
  }

  @Override
  public Expr visit(AsIdExpr expr) {
    var expressions = expandExprs(expr.exprs);
    var unprocessedName = concatStringifyExpressions(expr, expressions);
    if (unprocessedName == null) {
      // Will be expanded as soon as the used placeholders are bound
      return new AsIdExpr(expressions, copyLoc(expr.location()));
    }
    var name = unprocessedName.trim();

    if (!ParserUtils.isValidIdentifier(name)) {
      var clashesWithKeyword = ParserUtils.isKeyword(name);
      var invalidFormat = !clashesWithKeyword && !name.isEmpty();
      this.errors.add(error("Invalid Identifier: `%s`".formatted(name), expr)
          .applyIf(name.isEmpty(),
              builder -> builder.locationNote(expr, "Identifiers cannot be empty."))
          .applyIf(clashesWithKeyword, builder -> builder.locationNote(expr,
              "This expands to `%s` which clashes with a keyword.", name))
          .applyIf(invalidFormat, builder -> builder.locationNote(expr,
              "Identifiers must start with a character, "
                  + "followed by characters, numbers and or underscore, but `%s` doesn't.",
              name))
          .build());
    }

    return new Identifier(name, copyLoc(expr.location()));
  }

  @Override
  public Expr visit(AsStrExpr expr) {
    var expressions = expandExprs(expr.exprs);
    var name = concatStringifyExpressions(expr, expressions);
    if (name == null)  {
      // Will be expanded as soon as the used placeholders are bound
      return new AsStrExpr(expressions, copyLoc(expr.location()));
    }

    var token = "\"%s\"".formatted(name);
    return new StringLiteral(token, name, copyLoc(expr.location()));
  }

  @Override
  public Expr visit(ExistsInExpr expr) {
    return new ExistsInExpr(expandExprs(expr.operations), copyLoc(expr.loc));
  }

  @Override
  public Expr visit(ExistsInThenExpr expr) {
    var indices = new ArrayList<>(expr.indices);
    indices.replaceAll(index -> {
      var operations = new ArrayList<>(index.operations);
      operations.replaceAll(id -> (IsId) expandExpr((Expr) id));
      return new ExistsInThenExpr.Index(expandExpr(index.identifier()), operations);
    });
    return new ExistsInThenExpr(indices, expandExpr(expr.thenExpr), copyLoc(expr.loc));
  }

  @Override
  public Expr visit(ForallThenExpr expr) {
    var indices = new ArrayList<>(expr.indices);
    indices.replaceAll(index -> {
      var operations = new ArrayList<>(index.operations);
      operations.replaceAll(id -> (IsId) expandExpr((Expr) id));
      return new ForallThenExpr.Index(expandExpr(index.identifier()), operations);
    });
    return new ForallThenExpr(indices, expandExpr(expr.thenExpr), copyLoc(expr.loc));
  }

  @Override
  public Expr visit(ForallExpr expr) {
    var indices = new ArrayList<ForallIndex>(expr.indices.size());
    for (var index : expr.indices) {
      indices.add(new ForallIndex(
          expandExpr(index.name),
          index.typeLiteral == null ? null : expandExpr(index.typeLiteral),
          expandExpr(index.domain)));
    }

    return new ForallExpr(
        indices,
        expr.operation,
        expr.foldAction != null ? expandNode(expr.foldAction) : null,
        expandExpr(expr.body),
        copyLoc(expr.loc));
  }

  @Override
  public Expr visit(SequenceCallExpr expr) {
    return new SequenceCallExpr(
        expandExpr(expr.target),
        expr.range != null ? expandExpr(expr.range) : null,
        copyLoc(expr.loc)
    );
  }

  @Override
  public Expr visit(ExpandedSequenceCallExpr expr) {
    return new ExpandedSequenceCallExpr(
        expandExpr(expr.target),
        copyLoc(expr.loc)
    );
  }

  @Override
  public Expr visit(ExpandedAliasDefSequenceCallExpr expr) {
    return new ExpandedSequenceCallExpr(
        expandExpr(expr.target),
        copyLoc(expr.loc)
    );
  }

  @Override
  public Expr visit(ResourceReferenceExression expr) {
    return new ResourceReferenceExression(
        expandExpr(expr.resource),
        copyLoc(expr.location)
    );
  }

  @Override
  public Definition visit(ConstantDefinition definition) {
    return new ConstantDefinition(
        expandExpr(definition.identifier),
        definition.typeLiteral != null ? expandExpr(definition.typeLiteral) : null,
        expandExpr(definition.value),
        copyLoc(definition.loc)
    );
  }

  @Override
  public Definition visit(FormatDefinition definition) {
    var fields = new ArrayList<FormatField>(definition.fields.size());
    for (var field : definition.fields) {
      fields.add(expandDefinition(field));
    }

    return new FormatDefinition(
        expandExpr(definition.identifier),
        expandExpr(definition.typeLiteral),
        fields,
        copyLoc(definition.loc)
    );
  }

  @Override
  public Definition visit(DerivedFormatField definition) {
    return new DerivedFormatField(
        expandExpr(definition.identifier),
        expandExpr(definition.expr)
    );
  }

  @Override
  public Definition visit(RangeFormatField definition) {
    var ranges = new ArrayList<Expr>(definition.ranges.size());
    for (var range : definition.ranges) {
      ranges.add(expandExpr(range));
    }
    return new RangeFormatField(
        expandExpr(definition.identifier),
        ranges,
        definition.typeLiteral == null ? null : expandExpr(definition.typeLiteral)
    );
  }

  @Override
  public Definition visit(TypedFormatField definition) {
    return new TypedFormatField(
        expandExpr(definition.identifier),
        expandExpr(definition.typeLiteral)
    );
  }

  @Override
  public Definition visit(EncodingFormatField definition) {
    return new EncodingFormatField(
        expandExpr(definition.identifier),
        expandExpr(definition.expr)
    );
  }

  @Override
  public Definition visit(PredicateFormatField definition) {
    return new PredicateFormatField(
        expandExpr(definition.identifier),
        expandExpr(definition.expr)
    );
  }

  @Override
  public Definition visit(InstructionSetDefinition definition) {
    return new InstructionSetDefinition(
        expandExpr(definition.identifier),
        expandExprs(definition.extending),
        expandDefinitions(definition.definitions),
        copyLoc(definition.location())
    );
  }

  @Override
  public Definition visit(CounterDefinition definition) {
    return new CounterDefinition(
        definition.kind,
        expandExpr(definition.identifier),
        expandExpr(definition.typeLiteral),
        copyLoc(definition.loc)
    );
  }

  @Override
  public Definition visit(MemoryDefinition definition) {
    return new MemoryDefinition(
        expandExpr(definition.identifier),
        expandExpr(definition.addressTypeLiteral),
        expandExpr(definition.dataTypeLiteral),
        copyLoc(definition.loc)
    );
  }

  @Override
  public Definition visit(RegisterDefinition definition) {
    return new RegisterDefinition(
        expandExpr(definition.identifier),
        expandExpr(definition.typeLiteral),
        copyLoc(definition.loc)
    );
  }

  @Override
  public Definition visit(InstructionDefinition definition) {
    return new InstructionDefinition(
        expandExpr(definition.identifier),
        expandExpr(definition.typeIdentifier),
        expandStatement(definition.behavior),
        copyLoc(definition.loc)
    );
  }

  @Override
  public Definition visit(PseudoInstructionDefinition definition) {
    return new PseudoInstructionDefinition(
        expandExpr(definition.identifier),
        definition.kind,
        expandParams(definition.params),
        expandStatements(definition.statements),
        copyLoc(definition.loc)
    );
  }

  @Override
  public Definition visit(RelocationDefinition definition) {
    return new RelocationDefinition(
        expandExpr(definition.identifier),
        expandParams(definition.params),
        expandExpr(definition.resultTypeLiteral),
        expandExpr(definition.expr),
        copyLoc(definition.loc)
    );
  }

  @Override
  public Definition visit(EncodingDefinition definition) {
    return new EncodingDefinition(
        expandExpr(definition.instrIdentifier),
        expandEncs(definition.encodings),
        copyLoc(definition.loc)
    );
  }

  @Override
  public Definition visit(AssemblyDefinition definition) {
    return new AssemblyDefinition(
        expandExprs(definition.identifiers),
        expandExpr(definition.expr),
        copyLoc(definition.loc)
    );
  }

  @Override
  public Definition visit(UsingDefinition definition) {
    return new UsingDefinition(
        expandExpr(definition.id),
        expandExpr(definition.typeLiteral),
        copyLoc(definition.loc)
    );
  }

  @Override
  public Definition visit(AbiClangTypeDefinition abiClangTypeDefinition) {
    return new AbiClangTypeDefinition(
        abiClangTypeDefinition.typeName,
        abiClangTypeDefinition.typeSize,
        copyLoc(abiClangTypeDefinition.loc)
    );
  }

  @Override
  public Definition visit(AbiClangNumericTypeDefinition abiClangNumericTypeDefinition) {
    return new AbiClangNumericTypeDefinition(
        abiClangNumericTypeDefinition.typeName,
        expandExpr(abiClangNumericTypeDefinition.size),
        copyLoc(abiClangNumericTypeDefinition.loc)
    );
  }

  @Override
  public Definition visit(StageOutputDefinition definition) {
    return new StageOutputDefinition(
        expandExpr(definition.identifier),
        expandExpr(definition.typeLiteral)
    );
  }

  @Override
  public Definition visit(AbiSpecialPurposeInstructionDefinition definition) {
    return new AbiSpecialPurposeInstructionDefinition(
        definition.kind,
        expandExpr(definition.target),
        copyLoc(definition.loc)
    );
  }

  @Override
  public Definition visit(FunctionDefinition definition) {
    return new FunctionDefinition(
        expandExpr(definition.name),
        expandParams(definition.params),
        expandExpr(definition.retType),
        expandExpr(definition.expr),
        copyLoc(definition.loc)
    );
  }

  @Override
  public Definition visit(AliasDefinition definition) {
    return new AliasDefinition(
        expandExpr(definition.id),
        definition.kind,
        definition.aliasType != null ? expandExpr(definition.aliasType) : null,
        definition.targetType != null ? expandExpr(definition.targetType) : null,
        expandExpr(definition.value),
        copyLoc(definition.loc)
    );
  }

  @Override
  public Definition visit(AnnotationDefinition definition) {
    var keywords = new ArrayList<IdentifierOrPlaceholder>(definition.keywords.size());
    for (var keyword : definition.keywords) {
      keywords.add(expandExpr(keyword));
    }
    var values = new ArrayList<Expr>(definition.values.size());
    for (var value : definition.values) {
      values.add(expandExpr(value));
    }
    return new AnnotationDefinition(keywords, values, copyLoc(definition.loc));
  }

  @Override
  public Definition visit(EnumerationDefinition definition) {
    var entries = new ArrayList<EnumerationDefinition.Entry>(definition.entries.size());
    for (var entry : definition.entries) {
      entries.add(new EnumerationDefinition.Entry(
          expandExpr(entry.name),
          entry.value == null ? null : expandExpr(entry.value)));
    }

    return new EnumerationDefinition(
        expandExpr(definition.id),
        definition.enumType != null ? expandExpr(definition.enumType) : null,
        entries,
        copyLoc(definition.loc));
  }

  @Override
  public Definition visit(ExceptionDefinition definition) {
    return new ExceptionDefinition(
        expandExpr(definition.id),
        expandParams(definition.params),
        expandStatement(definition.statement),
        copyLoc(definition.loc));
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
      var subpass = new MacroExpander(arguments, macroOverrides,
          copyLoc(definition.location()).fullExpandedFromStack());
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
    return new DefinitionList(
        expandDefinitions(definition.items),
        definition.syntaxType,
        copyLoc(definition.location)
    );
  }

  @Override
  public Definition visit(ModelDefinition definition) {
    var boundModel = new ModelDefinition(
        expandExpr(definition.id),
        definition.params,
        definition.body,
        definition.returnType,
        copyLoc(definition.loc));
    boundModel.boundArguments = new HashMap<>(definition.boundArguments);
    boundModel.boundArguments.putAll(args);
    return boundModel;
  }

  @Override
  public Definition visit(RecordTypeDefinition definition) {
    return new RecordTypeDefinition(
        expandExpr(definition.name),
        definition.recordType,
        copyLoc(definition.loc));
  }

  @Override
  public Definition visit(ModelTypeDefinition definition) {
    return new ModelTypeDefinition(
        expandExpr(definition.name),
        definition.projectionType,
        copyLoc(definition.loc));
  }

  @Override
  public Definition visit(ImportDefinition importDefinition) {
    return importDefinition;
  }

  @Override
  public Definition visit(ProcessDefinition processDefinition) {
    var templateParams = new ArrayList<TemplateParam>(processDefinition.templateParams.size());
    for (var templateParam : processDefinition.templateParams) {
      templateParams.add(new TemplateParam(templateParam.identifier(),
          templateParam.type,
          templateParam.value == null ? null : expandExpr(templateParam.value)));
    }

    return new ProcessDefinition(
        expandExpr(processDefinition.name),
        templateParams,
        expandParams(processDefinition.inputs),
        expandParams(processDefinition.outputs),
        expandStatement(processDefinition.statement),
        copyLoc(processDefinition.loc));
  }

  @Override
  public Definition visit(OperationDefinition operationDefinition) {
    return new OperationDefinition(
        expandExpr(operationDefinition.name),
        expandExprs(operationDefinition.resources),
        copyLoc(operationDefinition.loc)
    );
  }

  @Override
  public Definition visit(Parameter definition) {
    return new Parameter(expandExpr(definition.name), expandExpr(definition.typeLiteral));
  }

  @Override
  public Definition visit(GroupDefinition groupDefinition) {
    return new GroupDefinition(
        expandExpr(groupDefinition.name),
        groupDefinition.type != null ? expandExpr(groupDefinition.type) : null,
        groupDefinition.groupSequence,
        copyLoc(groupDefinition.loc)
    );
  }

  @Override
  public Definition visit(ApplicationBinaryInterfaceDefinition definition) {
    return new ApplicationBinaryInterfaceDefinition(
        expandExpr(definition.id),
        expandExpr(definition.isa),
        expandDefinitions(definition.definitions),
        copyLoc(definition.loc)
    );
  }

  @Override
  public Definition visit(AbiSequenceDefinition definition) {
    return new AbiSequenceDefinition(
        definition.kind,
        expandParams(definition.params),
        expandStatements(definition.statements),
        copyLoc(definition.loc)
    );
  }

  @Override
  public Definition visit(SpecialPurposeRegisterDefinition definition) {
    return new SpecialPurposeRegisterDefinition(
        definition.purpose,
        definition.exprs,
        copyLoc(definition.loc)
    );
  }

  @Override
  public Definition visit(ProcessorDefinition definition) {
    return new ProcessorDefinition(
        expandExpr(definition.id),
        expandExpr(definition.implementedIsa),
        definition.abi != null ? expandExpr(definition.abi) : null,
        expandDefinitions(definition.definitions),
        copyLoc(definition.loc)
    );
  }

  @Override
  public Definition visit(PatchDefinition definition) {
    return new PatchDefinition(
        expandExpr(definition.generator),
        expandExpr(definition.handle),
        definition.reference != null ? expandExpr(definition.reference) : null,
        definition.source,
        copyLoc(definition.loc)
    );
  }

  @Override
  public Definition visit(SourceDefinition definition) {
    return new SourceDefinition(
        expandExpr(definition.id),
        definition.source,
        copyLoc(definition.loc)
    );
  }

  @Override
  public Definition visit(CpuFunctionDefinition definition) {
    return new CpuFunctionDefinition(
        expandExpr(definition.id),
        definition.kind,
        definition.stopWithReference != null ? expandExpr(definition.stopWithReference) : null,
        expandExpr(definition.expr),
        copyLoc(definition.loc)
    );
  }

  @Override
  public Definition visit(CpuMemoryRegionDefinition definition) {
    return new CpuMemoryRegionDefinition(
        expandExpr(definition.id),
        definition.kind,
        expandExpr(definition.memoryRef),
        definition.stmt == null ? null : expandStatement(definition.stmt),
        copyLoc(definition.loc)
    );
  }

  @Override
  public Definition visit(CpuProcessDefinition definition) {
    return new CpuProcessDefinition(
        definition.kind,
        expandParams(definition.startupOutputs),
        expandStatement(definition.statement),
        copyLoc(definition.loc)
    );
  }

  @Override
  public Definition visit(MicroArchitectureDefinition definition) {
    return new MicroArchitectureDefinition(
        expandExpr(definition.id),
        expandExpr(definition.isa),
        expandDefinitions(definition.definitions),
        copyLoc(definition.loc)
    );
  }

  @Override
  public Definition visit(MacroInstructionDefinition definition) {
    return new MacroInstructionDefinition(
        definition.kind,
        expandParams(definition.inputs),
        expandParams(definition.outputs),
        expandStatement(definition.statement),
        copyLoc(definition.loc)
    );
  }

  @Override
  public Definition visit(PortBehaviorDefinition definition) {
    return new PortBehaviorDefinition(
        expandExpr(definition.id),
        definition.kind,
        expandParams(definition.inputs),
        expandParams(definition.outputs),
        expandStatement(definition.statement),
        copyLoc(definition.loc)
    );
  }

  @Override
  public Definition visit(PipelineDefinition definition) {
    return new PipelineDefinition(
        expandExpr(definition.id),
        expandParams(definition.outputs),
        expandStatement(definition.statement),
        copyLoc(definition.loc)
    );
  }


  @Override
  public Definition visit(StageDefinition definition) {
    return new StageDefinition(
        expandExpr(definition.id),
        expandStageOutputs(definition.outputs),
        expandStatement(definition.statement),
        copyLoc(definition.loc)
    );
  }

  @Override
  public Definition visit(CacheDefinition definition) {
    return new CacheDefinition(
        expandExpr(definition.id),
        expandExpr(definition.sourceType),
        expandExpr(definition.targetType),
        copyLoc(definition.loc)
    );
  }

  @Override
  public Definition visit(LogicDefinition definition) {
    return new LogicDefinition(
        expandExpr(definition.id),
        expandExprs(definition.logicTypeIdentifiers),
        copyLoc(definition.loc)
    );
  }

  @Override
  public Definition visit(SignalDefinition definition) {
    return new SignalDefinition(
        expandExpr(definition.id),
        expandExpr(definition.type),
        copyLoc(definition.loc)
    );
  }

  @Override
  public Definition visit(AsmDescriptionDefinition definition) {
    return new AsmDescriptionDefinition(
        definition.id,
        definition.abi,
        definition.modifiers,
        definition.directives,
        definition.rules,
        definition.commonDefinitions,
        copyLoc(definition.loc)
    );
  }

  @Override
  public Definition visit(AsmModifierDefinition definition) {
    return new AsmModifierDefinition(
        expandExpr(definition.stringLiteral),
        expandExpr(definition.isa),
        expandExpr(definition.relocation),
        copyLoc(definition.loc)
    );
  }

  @Override
  public Definition visit(AsmDirectiveDefinition definition) {
    return new AsmDirectiveDefinition(
        expandExpr(definition.stringLiteral),
        expandExpr(definition.builtinDirective),
        copyLoc(definition.loc)
    );
  }

  @Override
  public Definition visit(AsmGrammarRuleDefinition definition) {
    return new AsmGrammarRuleDefinition(
        expandExpr(definition.id),
        definition.asmTypeDefinition != null
            ? expandDefinition(definition.asmTypeDefinition)
            : null,
        expandDefinition(definition.alternatives),
        copyLoc(definition.loc));
  }

  @Override
  public Definition visit(AsmGrammarAlternativesDefinition definition) {
    var alternatives =
        new ArrayList<List<AsmGrammarElementDefinition>>(definition.alternatives.size());
    for (var alternative : definition.alternatives) {
      alternatives.add(expandDefinitions(alternative));
    }
    return new AsmGrammarAlternativesDefinition(alternatives, copyLoc(definition.loc));
  }

  @Override
  public Definition visit(AsmGrammarElementDefinition definition) {
    return new AsmGrammarElementDefinition(
        definition.localVar != null ? expandDefinition(definition.localVar) : null,
        definition.attribute != null ? expandExpr(definition.attribute) : null,
        definition.isPlusEqualsAttributeAssign,
        definition.asmLiteral != null ? expandDefinition(definition.asmLiteral) : null,
        definition.groupAlternatives != null
            ? expandDefinition(definition.groupAlternatives)
            : null,
        definition.optionAlternatives != null
            ? expandDefinition(definition.optionAlternatives)
            : null,
        definition.repetitionAlternatives != null
            ? expandDefinition(definition.repetitionAlternatives)
            : null,
        definition.semanticPredicate != null ? expandExpr(definition.semanticPredicate) : null,
        definition.groupAsmTypeDefinition != null
            ? expandDefinition(definition.groupAsmTypeDefinition)
            : null,
        copyLoc(definition.loc));
  }

  @Override
  public Definition visit(AsmGrammarLocalVarDefinition definition) {
    return new AsmGrammarLocalVarDefinition(
        expandExpr(definition.id),
        expandDefinition(definition.asmLiteral),
        copyLoc(definition.loc)
    );
  }

  @Override
  public Definition visit(AsmGrammarLiteralDefinition definition) {
    return new AsmGrammarLiteralDefinition(
        definition.id != null ? expandExpr(definition.id) : null,
        expandDefinitions(definition.parameters),
        definition.stringLiteral != null ? expandExpr(definition.stringLiteral) : null,
        definition.asmTypeDefinition != null
            ? expandDefinition(definition.asmTypeDefinition)
            : null,
        copyLoc(definition.loc)
    );
  }

  @Override
  public Definition visit(AsmGrammarTypeDefinition definition) {
    return new AsmGrammarTypeDefinition(
        expandExpr(definition.id),
        copyLoc(definition.loc)
    );
  }

  @Override
  public BlockStatement visit(BlockStatement blockStatement) {
    return new BlockStatement(
        expandStatements(blockStatement.statements),
        copyLoc(blockStatement.location)
    );
  }

  @Override
  public Statement visit(LetStatement letStatement) {
    return new LetStatement(
        expandExprs(letStatement.identifiers),
        expandExpr(letStatement.valueExpr),
        expandStatement(letStatement.body),
        copyLoc(letStatement.location)
    );
  }

  @Override
  public Statement visit(IfStatement ifStatement) {
    return new IfStatement(
        expandExpr(ifStatement.condition),
        expandStatement(ifStatement.thenStmt),
        ifStatement.elseStmt == null ? null : expandStatement(ifStatement.elseStmt),
        copyLoc(ifStatement.location)
    );
  }

  @Override
  public Statement visit(AssignmentStatement assignmentStatement) {
    return new AssignmentStatement(
        expandExpr(assignmentStatement.target),
        expandExpr(assignmentStatement.valueExpression)
    );
  }

  @Override
  public Statement visit(RaiseStatement raiseStatement) {
    return new RaiseStatement(
        expandStatement(raiseStatement.statement),
        copyLoc(raiseStatement.location)
    );
  }

  @Override
  public Statement visit(CallStatement callStatement) {
    return new CallStatement(expandExpr(callStatement.expr));
  }

  @Override
  public Statement visit(PlaceholderStatement statement) {
    var arg = resolveArg(statement.segments);
    return Objects.requireNonNullElse((Statement) arg,
        new PlaceholderStatement(statement.segments, statement.syntaxType, copyLoc(statement.loc)));
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
      var subpass = new MacroExpander(arguments, macroOverrides,
          copyLoc(stmt.location()).fullExpandedFromStack());
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
    var cases = new ArrayList<MatchStatement.Case>(matchStatement.cases.size());
    for (var matchCase : matchStatement.cases) {
      cases.add(new MatchStatement.Case(
          expandExprs(matchCase.patterns),
          expandStatement(matchCase.result)));
    }

    return new MatchStatement(
        expandExpr(matchStatement.candidate),
        cases,
        matchStatement.defaultResult == null ? null : expandStatement(matchStatement.defaultResult),
        copyLoc(matchStatement.loc)
    );
  }

  @Override
  public Statement visit(StatementList statementList) {
    return new StatementList(
        expandStatements(statementList.items),
        copyLoc(statementList.location())
    );
  }

  @Override
  public InstructionCallStatement visit(InstructionCallStatement instructionCallStatement) {
    var namedArguments =
        new ArrayList<InstructionCallStatement.NamedArgument>(
            instructionCallStatement.namedArguments.size());
    for (var namedArgument : instructionCallStatement.namedArguments) {
      namedArguments.add(new InstructionCallStatement.NamedArgument(
          expandExpr(namedArgument.name),
          expandExpr(namedArgument.value)));
    }

    return new InstructionCallStatement(
        expandExpr(instructionCallStatement.id),
        namedArguments,
        expandExprs(instructionCallStatement.unnamedArguments),
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
    var indices = new ArrayList<ForallIndex>(forallStatement.indices.size());
    for (var index : forallStatement.indices) {
      indices.add(new ForallIndex(
          expandExpr(index.name),
          index.typeLiteral != null ? expandExpr(index.typeLiteral) : null,
          expandExpr(index.domain)));
    }

    return new ForallStatement(
        indices,
        expandStatement(forallStatement.body),
        copyLoc(forallStatement.loc));
  }

  private void assertValidMacro(Macro macro, SourceLocation sourceLocation)
      throws MacroExpansionException {
    if (macro.returnType() == BasicSyntaxType.INVALID) {
      throw new MacroExpansionException(
          "Skipped expanding macro %s due to previous error".formatted(macro.name().name),
          sourceLocation);
    }
  }

  Map<String, Node> collectMacroParameters(Macro macro, List<Node> actualArguments,
                                           SourceLocation instanceLoc)
      throws MacroExpansionException {
    var formalParams = macro.params();
    if (formalParams.size() != actualArguments.size()) {
      throw new MacroExpansionException(
          "The macro `%s` expects %d arguments but %d were provided.".formatted(macro.name().name,
              formalParams.size(), actualArguments.size()), instanceLoc);
    }
    var arguments = new HashMap<>(macro.boundArguments());
    for (int i = 0; i < formalParams.size(); i++) {
      var formalParam = formalParams.get(i);
      var actualParam = expandNode(actualArguments.get(i));
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

  private EncodingDefinition.EncsNode expandEncs(EncodingDefinition.EncsNode encs) {
    var encodings = new ArrayList<IsEncs>(encs.items.size());
    for (var enc : encs.items) {
      encodings.addAll(expandEnc(enc));
    }
    return new EncodingDefinition.EncsNode(encodings, copyLoc(encs.loc));
  }

  private List<IsEncs> expandEnc(IsEncs encoding) {
    if (encoding instanceof EncodingDefinition.EncodingField encodingField) {
      return List.of(new EncodingDefinition.EncodingField(expandExpr(encodingField.field),
          expandExpr(encodingField.value)));
    } else if (encoding instanceof EncodingDefinition.EncsNode encodings) {
      var encs = new ArrayList<IsEncs>(encodings.items.size());
      for (IsEncs enc : encodings.items) {
        encs.addAll(expandEnc(enc));
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
    Node arg = args.get(segments.getFirst());
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

    // Need to copy the arguments becuase otherwise all usages will point to the same
    //  instance, but depending on their usage, they can have different names etc.
    if (AstUtils.isFullyExpanded(arg)) {
      arg = expandNode(arg);
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
      var subpass =
          new MacroExpander(arguments, macroOverrides, copyLoc(node.loc).fullExpandedFromStack());
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
        new Parameter(param.identifier(), expandExpr(param.typeLiteral)));
    return expandedParams;
  }

  private List<StageOutputDefinition> expandStageOutputs(List<StageOutputDefinition> outputs) {
    var expanded = new ArrayList<>(outputs);
    expanded.replaceAll(param ->
        new StageOutputDefinition(param.identifier(), expandExpr(param.typeLiteral)));
    return expanded;
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
        || node instanceof AsIdExpr || node instanceof AsStrExpr;

  }

  /**
   * Copies the location and annotates with the location of the macro we are currently expanding.
   *
   * @param loc to be copied
   */
  private SourceLocation copyLoc(SourceLocation loc) {
    // FIXME: At the time of writing we sometimes issued the pass twice resulting in double
    // reporting of expandedFrom
    if (expandingFrom == null) {
      return loc;
    }

    return loc.copyWithAppendedExpandedFrom(expandingFrom);
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
