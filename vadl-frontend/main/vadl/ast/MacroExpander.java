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
import java.util.List;
import java.util.Map;
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
import vadl.ast.nodes.BinOp;
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
import vadl.ast.nodes.FloatTypeDefinition;
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
import vadl.ast.nodes.MacroCall;
import vadl.ast.nodes.MacroMatch;
import vadl.ast.nodes.MacroMatchDefinition;
import vadl.ast.nodes.MacroMatchExpr;
import vadl.ast.nodes.MacroMatchNode;
import vadl.ast.nodes.MacroMatchStatement;
import vadl.ast.nodes.MacroReference;
import vadl.ast.nodes.MatchExpr;
import vadl.ast.nodes.MatchStatement;
import vadl.ast.nodes.MemoryDefinition;
import vadl.ast.nodes.MicroArchitectureDefinition;
import vadl.ast.nodes.ModelDefinition;
import vadl.ast.nodes.ModelTypeDefinition;
import vadl.ast.nodes.NewLabelStatement;
import vadl.ast.nodes.Node;
import vadl.ast.nodes.OperationDefinition;
import vadl.ast.nodes.Parameter;
import vadl.ast.nodes.PatchDefinition;
import vadl.ast.nodes.PipelineDefinition;
import vadl.ast.nodes.PortBehaviorDefinition;
import vadl.ast.nodes.PredicateFormatField;
import vadl.ast.nodes.ProcessDefinition;
import vadl.ast.nodes.ProcessorDefinition;
import vadl.ast.nodes.ProjectionType;
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
import vadl.ast.nodes.UnOp;
import vadl.ast.nodes.UnaryExpr;
import vadl.ast.nodes.UsingDefinition;
import vadl.ast.nodes.WildcardLiteral;
import vadl.error.Diagnostic;
import vadl.error.DiagnosticList;
import vadl.utils.RopeList;
import vadl.utils.SourceLocation;

/**
 * Expands macro invocations and placeholders with the expanded body of a macro.
 * The caller has to make sure that the macro calls are valid, the expander only expects valid,
 * calls and will crash otherwise. This measn every syntax type has to be right. And all macros
 * passed to it can be resolved.
 * The only exception to this is the {@link AsIdExpr} and {@link AsStrExpr} which may contain
 * expressions, that cannot actually be converted and which will result in diagnostics.
 * Additionally, for the former it is possible that the generated identifier clashes with a keyword
 * or is otherwise invalid which also generates a diagnostic.
 *
 * <p>Id-returning macros can be overridden via CLI arguments or with import statements.
 * Since binary expression reordering can depend on operator placeholders, this class also
 * reorders any encountered binary expressions.
 */
@SuppressWarnings("OverloadMethodsDeclarationOrder")
class MacroExpander
    implements ExprVisitor<Expr>, DefinitionVisitor<Definition>, StatementVisitor<Statement> {
  final SymbolTable macroTable;
  final Map<String, Identifier> macroOverrides;
  final List<Diagnostic> errors = new ArrayList<>();
  @Nullable
  final RopeList<SourceLocation.DirectLocation> expandingFrom;

  private MacroExpander(SymbolTable macroTable,
                        Map<String, Identifier> macroOverrides,
                        @Nullable RopeList<SourceLocation.DirectLocation> expandingFrom) {
    this.macroTable = macroTable;
    this.macroOverrides = macroOverrides;
    this.expandingFrom = expandingFrom;
  }

  /**
   * Expand a model and the corresponding call to it.
   * The arguments in the call have to be already expanded.
   *
   * @param model which is the template.
   * @param call that will be expanded to the template.
   * @return the expanded nodes.
   * @throws DiagnosticList if a macro produces something invalid.
   */
  public static Node expandMacroCall(ModelDefinition model, MacroCall call) {

    if (model.params.size() != call.arguments().size()) {
      throw new IllegalStateException("Argument count mismatch");
    }

    // Create a new scope and inject all the arguments.
    var macroTable = model.macroTable.createChild();
    var positionalArgs = call.arguments();
    for (int i = 0; i < model.params.size(); i++) {
      var name = model.params.get(i).name().name;
      var value = positionalArgs.get(i);
      macroTable.defineSymbol(name, value);
    }

    var expander = new MacroExpander(
        macroTable,
        model.macroOverrides,
        call.location().fullExpandedRope());

    // Models can be overwritten in the CLI/import
    var overriddenBody = model.macroOverrides.get(model.identifier().name);
    var result = overriddenBody == null
        ? expander.expandNode(model.body)
        : expander.expandExpr(overriddenBody);

    if (!expander.errors.isEmpty()) {
      throw new DiagnosticList(expander.errors);
    }

    return result;
  }

  /**
   * Expand the {@link AsIdExpr} node.
   * The arguments in the call have to be already expanded.
   *
   * @param asId node to be expanded.
   * @return the generated identifier.
   * @throws DiagnosticList if a macro produces something invalid.
   */
  public static Node expandAsId(AsIdExpr asId) {
    var expander = new MacroExpander(new SymbolTable(), Map.of(), null);

    var result = asId.accept(expander);

    if (!expander.errors.isEmpty()) {
      throw new DiagnosticList(expander.errors);
    }

    return result;
  }

  /**
   * Expand the {@link AsStrExpr} node.
   * The arguments in the call have to be already expanded.
   *
   * @param asStr node to be expanded.
   * @return the generated identifier.
   * @throws DiagnosticList if a macro produces something invalid.
   */
  public static Node expandAsStr(AsStrExpr asStr) {
    var expander = new MacroExpander(new SymbolTable(), Map.of(), null);

    var result = asStr.accept(expander);

    if (!expander.errors.isEmpty()) {
      throw new DiagnosticList(expander.errors);
    }

    return result;
  }

  /**
   * Expand a {@link AssemblyDefinition} with multiple targets into one for each.
   * The children of the node have to be already expanded.
   *
   * <p>Example:
   * <pre>{@code
   *  assembly One, Two, Three = (mnemonic, " ", decimal(field))
   * }</pre>
   * Assemblies like this will return now three assemblies where each one will only have one
   * target in the end.
   *
   * @param def to be expanded.
   * @return a list of expanded nodes.
   */
  public static List<AssemblyDefinition> expandAssemblyDefs(AssemblyDefinition def) {
    var expander = new MacroExpander(new SymbolTable(), Map.of(), null);
    var result = expander.expandAssemblies(def);

    if (!expander.errors.isEmpty()) {
      throw new DiagnosticList(expander.errors);
    }

    return result;
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
    for (var i = 0; i < expressions.size(); i++) {
      result.add(expandExpr(expressions.get(i)));
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
    for (var i = 0; i < args.values.size(); i++) {
      var value = args.values.get(i);
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
        yield new RecordInstance(recordInstance.type, entries,
            copyLoc(recordInstance.sourceLocation));
      }
      case EncodingDefinition.EncsNode encs -> expandEncs(encs);
      case MacroCall.Node macroCallNode -> expand(macroCallNode);
      case MacroMatchNode macroMatchNode -> expand(macroMatchNode);
      case BinOp binOp -> new BinOp(binOp.operator, copyLoc(binOp.location));
      case UnOp unOp -> new UnOp(unOp.operator, copyLoc(unOp.location));
      case null, default -> node;
    };
  }

  /**
   * Expands assembly definitions with multiple identifiers into multiple definitions.
   * Not quite a macro to expand, but the assemblies with multiple identifiers kinda behave like
   * macros.
   *
   * @param definition to be expanded.
   * @return a list of the expanded definitions.
   */
  private List<AssemblyDefinition> expandAssemblies(AssemblyDefinition definition) {
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

  /**
   * Macro arguments have a different semantic for expansion as not all of them always have to be
   * expanded, depending on the type expected.
   *
   * <p>Example:
   * <pre>{@code
   * // This is a snapshot both $num and $AddModel are model parameters
   * // type for model M0: (Ex, (Ex, Ex) -> Ex)
   * $M0($num, $AddModel)
   *     ^^^^   ^^^^^^^^^
   * expanded   not expanded (because we don't want to call it but reference it)
   * }</pre>
   *
   * @param model to be called.
   * @param args to be allied to the call.
   * @return of expanded arguments.
   */
  private List<Node> expandMacroArgs(ModelDefinition model, List<Node> args) {
    var result = new ArrayList<Node>(args.size());
    for (int i = 0; i < args.size(); i++) {
      var arg =  args.get(i);
      var param = model.params.get(i);
      final Node expanded;

      // Don't expand if it is a reference to
      if (param.type() instanceof ProjectionType) {
        if (arg instanceof MacroCall call) {
          var value = this.macroTable.findAs(call.name(), Node.class);
          if (value == null) {
            throw new IllegalStateException();
          }
          expanded = value;
        } else {
          expanded = arg;
        }
      } else {
        expanded = expandNode(arg);
      }

      result.add(expanded);
    }

    return result;
  }

  private Node expand(MacroCall macroCall) {
    // Resolve the model
    Node value = macroTable.findMacroAs(macroCall.name().name, Node.class);
    if (value == null) {
      value = macroTable.findAs(macroCall.name(), Node.class);
    }
    if (value == null) {
      throw new IllegalStateException("Macro " + macroCall.name().name + " not found");
    }

    // Resolve subcalls of records
    for (int i = 0; i < macroCall.subcalls().size(); i++) {
      var subcall = macroCall.subcalls().get(i);
      if (!(value instanceof RecordInstance record)) {
        throw new IllegalStateException("Macro " + macroCall.name().name + " not found");
      }
      value = record.findEntry(subcall);
      if (value == null) {
        throw new IllegalStateException("Macro " + macroCall.name().name + " not found");
      }
    }

    // Resolve macro references
    if (value instanceof MacroReference reference) {
      value = reference.model;
    }

    if (value instanceof ModelDefinition model) {
      // Ignore broken models
      if (model.returnType == BasicSyntaxType.INVALID) {
        return (Node) macroCall;
      }

      // Args need to be exanded because they might contain other macro calls.
      var expandedArgs = expandMacroArgs(model, macroCall.arguments());
      var patchedCall =
          MacroCall.of(macroCall.name(), macroCall.subcalls(), expandedArgs, macroCall.syntaxType(),
              copyLoc(macroCall.location()));

      return MacroExpander.expandMacroCall(model, patchedCall);
    }

    if (!macroCall.arguments().isEmpty()) {
      throw new IllegalStateException("Macro " + macroCall.name().name + " not callable");
    }

    return expandNode(value);
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
  public Expr visit(MacroCall.Expr expr) {
    return (Expr) expand(expr);
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
    for (var i = 0; i < expr.argsIndices.size(); i++) {
      var argsIndex = expr.argsIndices.get(i);
      argsIndices.add(expandArgs(argsIndex));
    }
    var subCalls = new ArrayList<CallIndexExpr.SubCall>(expr.subCalls.size());
    for (var i = 0; i < expr.subCalls.size(); i++) {
      var subCall = expr.subCalls.get(i);
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
    var symbolArgs = new ArrayList<Expr>(expr.symbolArgs.size());
    for (var i = 0; i < expr.symbolArgs.size(); i++) {
      var symbolArg = expr.symbolArgs.get(i);
      symbolArgs.add(expandExpr(symbolArg));
    }
    return new SymbolExpr(expandExpr(expr.path), symbolArgs, copyLoc(expr.location));
  }

  @Override
  public Expr visit(MacroMatchExpr expr) {
    var macroMatch = expandMacroMatch(expr.macroMatch);
    return (Expr) resolveMacroMatch(macroMatch);
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

  private String concatStringifyExpressions(Expr origin, List<Expr> expressions) {
    var nameBuilder = new StringBuilder(expressions.size());
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
      } else {
        errors.add(
            error("Unsupported `%s` parameter `%s`".formatted(origin.nodeName(), inner.nodeName()),
                inner.location()).build());
        nameBuilder.append(inner);
      }
    }

    return nameBuilder.toString();
  }

  @Override
  public Expr visit(AsIdExpr expr) {
    var expressions = expandExprs(expr.exprs);
    var unprocessedName = concatStringifyExpressions(expr, expressions);
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
  public Definition visit(FloatTypeDefinition definition) {
    return new FloatTypeDefinition(
        expandExpr(definition.identifier),
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
    var relationTypeLiteral = new RegisterDefinition.RelationTypeLiteral(
        expandExprs(definition.typeLiteral.argTypes),
        expandExpr(definition.typeLiteral.resultType)
    );

    return new RegisterDefinition(
        expandExpr(definition.identifier),
        relationTypeLiteral,
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
  public Definition visit(MacroCall.Definition definition) {
    return (Definition) expand(definition);
  }

  @Override
  public Definition visit(MacroMatchDefinition definition) {
    var macroMatch = expandMacroMatch(definition.macroMatch);
    return (Definition) resolveMacroMatch(macroMatch);
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
    // Use the current macro table so the inner model has access to all the arguments of the current
    // model.
    return new ModelDefinition(
        expandExpr(definition.id),
        definition.params,
        definition.body,
        definition.returnType,
        macroTable,
        definition.macroOverrides,
        copyLoc(definition.loc));
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
      templateParams.add(new TemplateParam(expandExpr(templateParam.identifier()),
          expandExpr(templateParam.type),
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
        expandExprs(definition.exprs),
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
        expandExpr(definition.id),
        expandExpr(definition.abi),
        expandDefinitions(definition.modifiers),
        expandDefinitions(definition.directives),
        expandDefinitions(definition.rules),
        expandDefinitions(definition.commonDefinitions),
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
  public Statement visit(MacroCall.Statement stmt) {
    return (Statement) expand(stmt);
  }

  @Override
  public Statement visit(MacroMatchStatement macroMatchStatement) {
    var macroMatch = expandMacroMatch(macroMatchStatement.macroMatch);
    return (Statement) resolveMacroMatch(macroMatch);
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
  public NewLabelStatement visit(NewLabelStatement newLabelStatement) {
    return new NewLabelStatement(
        expandExpr(newLabelStatement.labelId),
        copyLoc(newLabelStatement.loc)
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
    } else if (encoding instanceof MacroMatchNode macroMatchNode) {
      var expanded = expand(macroMatchNode);
      if (expanded instanceof EncodingDefinition.EncsNode encs) {
        return encs.items;
      } else {
        return List.of((IsEncs) expanded);
      }
    } else if (encoding instanceof MacroCall.Node macroCallNode) {
      var expanded = expand(macroCallNode);
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

  private Node resolveMacroMatch(MacroMatch macroMatch) {
    for (var choice : macroMatch.choices()) {
      for (var pattern : choice.patterns()) {
        var candidate = expandNode(pattern.candidate());
        var equals = candidate.equals(pattern.match());
        var shouldEqual = pattern.comparison() == MacroMatch.Comparison.EQUAL;
        if (equals == shouldEqual) {
          return expandNode(choice.result());
        }
      }
    }
    return expandNode(macroMatch.defaultChoice());
  }

  private Node expand(MacroCall.Node node) {
    return expand((MacroCall) node);
  }

  private Node expand(MacroMatchNode node) {
    var macroMatch = expandMacroMatch(node.macroMatch);
    return resolveMacroMatch(macroMatch);
  }

  private List<Parameter> expandParams(List<Parameter> params) {
    var expandedParams = new ArrayList<>(params);
    expandedParams.replaceAll(param ->
        new Parameter(expandExpr(param.identifier()), expandExpr(param.typeLiteral)));
    return expandedParams;
  }

  private List<StageOutputDefinition> expandStageOutputs(List<StageOutputDefinition> outputs) {
    var expanded = new ArrayList<>(outputs);
    expanded.replaceAll(param ->
        new StageOutputDefinition(expandExpr(param.identifier()), expandExpr(param.typeLiteral)));
    return expanded;
  }

  /**
   * Copies the location and annotates with the location of the macro we are currently expanding.
   *
   * @param loc to be copied
   */
  private SourceLocation copyLoc(SourceLocation loc) {
    if (expandingFrom == null) {
      return loc;
    }

    return loc.copyWithAppendedExpandedFrom(expandingFrom);
  }
}
