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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import vadl.ast.nodes.AliasDefinition;
import vadl.ast.nodes.AnnotationDefinition;
import vadl.ast.nodes.ApplicationBinaryInterfaceDefinition;
import vadl.ast.nodes.AsmDescriptionDefinition;
import vadl.ast.nodes.AsmDirectiveDefinition;
import vadl.ast.nodes.AsmGrammarAlternativesDefinition;
import vadl.ast.nodes.AsmGrammarElementDefinition;
import vadl.ast.nodes.AsmGrammarLiteralDefinition;
import vadl.ast.nodes.AsmGrammarLocalVarDefinition;
import vadl.ast.nodes.AsmGrammarTypeDefinition;
import vadl.ast.nodes.AsmModifierDefinition;
import vadl.ast.nodes.AssemblyDefinition;
import vadl.ast.nodes.BinOp;
import vadl.ast.nodes.CpuMemoryRegionDefinition;
import vadl.ast.nodes.Definition;
import vadl.ast.nodes.EncodingDefinition;
import vadl.ast.nodes.EnumerationDefinition;
import vadl.ast.nodes.ExistsInExpr;
import vadl.ast.nodes.ExistsInThenExpr;
import vadl.ast.nodes.Expr;
import vadl.ast.nodes.FloatTypeDefinition;
import vadl.ast.nodes.ForallExpr;
import vadl.ast.nodes.ForallStatement;
import vadl.ast.nodes.ForallThenExpr;
import vadl.ast.nodes.FormatDefinition;
import vadl.ast.nodes.FormatField;
import vadl.ast.nodes.FunctionDefinition;
import vadl.ast.nodes.IdentifiableNode;
import vadl.ast.nodes.Identifier;
import vadl.ast.nodes.IdentifierOrPlaceholder;
import vadl.ast.nodes.IdentifierPath;
import vadl.ast.nodes.ImportDefinition;
import vadl.ast.nodes.InstructionCallStatement;
import vadl.ast.nodes.InstructionDefinition;
import vadl.ast.nodes.InstructionSetDefinition;
import vadl.ast.nodes.IsId;
import vadl.ast.nodes.LetExpr;
import vadl.ast.nodes.LetStatement;
import vadl.ast.nodes.Macro;
import vadl.ast.nodes.MemoryDefinition;
import vadl.ast.nodes.MicroArchitectureDefinition;
import vadl.ast.nodes.ModelDefinition;
import vadl.ast.nodes.ModelTypeDefinition;
import vadl.ast.nodes.NewLabelStatement;
import vadl.ast.nodes.Node;
import vadl.ast.nodes.Parameter;
import vadl.ast.nodes.ProcessorDefinition;
import vadl.ast.nodes.PseudoInstructionDefinition;
import vadl.ast.nodes.RecordTypeDefinition;
import vadl.ast.nodes.RecursiveAstVisitor;
import vadl.ast.nodes.RelocationDefinition;
import vadl.ast.nodes.Statement;
import vadl.ast.nodes.SyntaxType;
import vadl.ast.nodes.TypeLiteral;
import vadl.ast.nodes.UsingDefinition;
import vadl.error.Diagnostic;
import vadl.types.BuiltInTable;
import vadl.types.Type;
import vadl.types.asmTypes.AsmType;
import vadl.utils.Levenshtein;
import vadl.utils.SourceLocation;
import vadl.utils.WithLocation;

@SuppressWarnings("MissingJavadocType")
public class SymbolTable {

  // Collecting symbols is expensive and providing many suggestions makes levenshtein slower.
  // FIXME: Increase this limit if once the levenshtein algorightm is faster
  private static final int MAX_COLLECTED_SYMBOL_NAME_SUGGESTIONS = 100;

  /// Collecting names across the AST is quite expensive so to improve this
  /// we limit the amount of diagnostics that get that expensive treatment.
  /// This allows the compiler to stay responsive if a often used format is deleted or renamed
  /// by accident.
  private static final int MAX_DIAGNOSTICS_WITH_NAME_SUGGESTIONS = 1000;

  @Nullable
  SymbolTable parent;

  final Map<String, Node> symbols = new HashMap<>();
  final Map<String, Node> macroSymbols = new HashMap<>();
  // the errors list is the same obj as the parent's error list
  List<Diagnostic> errors;

  /**
   * Load all builtin function names into the scope so that the names are reserved and can be used
   * (in some contextes). However, we don't store any useful informations with them and the next
   * passes (the type-checker) needs to know on it's own how to deal with them.
   */
  static HashSet<String> builtinNames;

  static {
    builtinNames = BuiltInTable.builtIns()
        .map(BuiltInTable.BuiltIn::name)
        .collect(Collectors.toCollection(HashSet::new));

    // Add pseudo buildins
    builtinNames.add("VADL::mod");
    builtinNames.add("VADL::div");
    builtinNames.add("start");
    builtinNames.add("executable");
    builtinNames.add("halt");
    // TODO: Remove this once migration of decimal to sdec is done for all specs #409
    builtinNames.add("decimal");
  }

  public SymbolTable() {
    parent = null;
    errors = new ArrayList<>();
  }

  private SymbolTable(SymbolTable parent, List<Diagnostic> errors) {
    this.parent = parent;
    this.errors = errors;
  }

  /**
   * Imports all the symbols from the module specified into the current symbol-tabel.
   *
   * @param moduleAst       of the module from which you import.
   * @param importedSymbols to be imported.
   */
  void importFrom(Ast moduleAst, List<List<Identifier>> importedSymbols) {
    for (List<Identifier> importedSymbolSegments : importedSymbols) {
      var importedSymbol = new StringBuilder();
      for (Identifier segment : importedSymbolSegments) {
        if (!importedSymbol.isEmpty()) {
          importedSymbol.append("::");
        }
        importedSymbol.append(segment.name);
      }
      var name = importedSymbol.toString();
      var symbol = moduleAst.rootSymbolTable().symbols.get(name);
      var macroSymbol = moduleAst.rootSymbolTable().macroSymbols.get(name);
      var location = importedSymbolSegments.get(0).location()
          .join(importedSymbolSegments.get(importedSymbolSegments.size() - 1).location());
      if (symbol == null && macroSymbol == null) {
        errors.add(error("Unresolved symbol " + name, location).build());
      } else {
        if (symbol != null) {
          symbols.put(name, symbol);
        }
        if (macroSymbol != null) {
          macroSymbols.put(name, macroSymbol);
        }
      }
    }
  }


  SymbolTable createChild() {
    SymbolTable child = new SymbolTable(this, this.errors);
    return child;
  }

  /**
   * This returns the parent symbol table and transfers all errors from this to the
   * parent symbol table.
   *
   * @return the parent symbol table
   * @throws IllegalStateException if parent is null
   */
  SymbolTable pop() {
    if (parent == null) {
      throw new IllegalStateException("Tried to pop symbol table, but parent is null");
    }
    return parent;
  }

  /**
   * Check if the identifier resolves to a builtin.
   *
   * @param isId to be resolved
   * @return true if the identifier resolves to a builtin, false otherwise
   */
  boolean isBuiltin(IsId isId) {
    var name = isId.pathToString();
    return builtinNames.contains(name);
  }

  /**
   * Define a symbol or macro symbol from a user definition.
   *
   * @param name   of the new symbol.
   * @param origin of the symbol definition.
   */
  void defineSymbol(String name, Node origin) {
    if (origin instanceof ModelDefinition || origin instanceof ModelTypeDefinition
        || origin instanceof RecordTypeDefinition) {
      verifyMacroAvailable(name, origin);
      macroSymbols.put(name, origin);
    } else {
      verifyAvailable(name, origin);
      symbols.put(name, origin);
    }
  }

  /**
   * A wrapper for {@link #defineSymbol(String, Node)} for {@link IdentifiableNode}.
   *
   * @param origin to be added to the scope, name will be automatically infered.
   */
  <T extends Node & IdentifiableNode> void defineSymbol(T origin) {
    var name = origin.identifier().name;
    defineSymbol(name, origin);
  }

  /**
   * A wrapper for {@link #defineSymbol(String, Node)} for {@link ModelDefinition}.
   *
   * @param modelDefinition to be added to the scope, name will be automatically infered.
   */
  void addModelDefinition(ModelDefinition modelDefinition) {
    // Note: We cannot use .identifier here because the identifier might not be initialized with
    // macros that generate macros.
    defineSymbol(modelDefinition.toMacro().name().name, modelDefinition);
  }

  /**
   * Internal use only.
   * Finds a symbol in the current scope.
   *
   * @param name of the symbol to resolve
   * @return the symbol if found, null otherwise
   */
  @Nullable
  private Node resolveName(String name) {
    var symbol = symbols.get(name);
    if (symbol != null) {
      return symbol;
    }

    if (parent != null) {
      return parent.resolveName(name);
    }

    return null;
  }

  /**
   * Internal use only.
   * Finds a symbol in the current scope by path
   *
   * @param path to the symbol to resolve
   * @return the symbol if found, null otherwise
   */
  @Nullable
  private Node resolvePath(List<String> path) {
    if (path.size() == 1) {
      return symbols.get(path.get(0));
    }

    var namespace = resolveName(path.get(0));
    if (namespace == null) {
      return null;
    }

    return namespace.symbolTable().resolvePath(path.subList(1, path.size()));
  }

  /**
   * Internal use only.
   * Finds an Identifier or IdentifierPath in the current scope or by path
   *
   * @param id to resolve
   * @return the symbol if found, null otherwise
   */
  @Nullable
  private Node find(IsId id) {
    return switch (id) {
      case Identifier ident -> {
        var symbol = resolveName(ident.name);
        if (symbol != null) {
          ident.target = symbol;
        }
        yield symbol;
      }
      case IdentifierPath path -> {
        var symbol = resolvePath(path.pathToSegments());
        if (symbol != null) {
          path.target = symbol;
        }
        yield symbol;
      }
      default -> throw new IllegalArgumentException("Illegal identifier type: " + id.getClass());
    };
  }

  /**
   * Internal use only.
   * Finds a macro symbol by name in the current scope or by path
   *
   * @param name to resolve
   * @return the origin if it could be found, null otherwise
   */
  @Nullable
  private Node resolveMacroName(String name) {
    var symbol = macroSymbols.get(name);
    if (symbol == null && parent != null) {
      return parent.resolveMacroName(name);
    }

    if (symbol == null) {
      return null;
    }

    return symbol;
  }

  /**
   * Finds a macro symbol by name in the current scope.
   *
   * @param usage the identifier that should be resolved
   * @param type  of the node to be resolved
   * @return the origin if it could be found, null otherwise
   */
  public <T extends Node> @Nullable T findAs(IsId usage, Class<T> type) {
    var symbol = find(usage);
    return type.isInstance(symbol) ? type.cast(symbol) : null;
  }

  ;


  /**
   * Finds a macro symbol by name in the current scope or by name.
   *
   * @param name of the macro, syntax type or record type.
   * @param type of to be searched
   * @return the found origin or null
   */
  @Nullable
  public <T extends Node> T findMacroAs(String name, Class<T> type) {
    var origin = resolveMacroName(name);
    return type.isInstance(origin) ? type.cast(origin) : null;
  }

  /**
   * Short cut to get the macro of a modeldefinition.
   *
   * @param name to be resolved.
   * @return the Macro if it exists otherwise null.
   */
  @Nullable
  Macro getMacro(String name) {
    var model = findMacroAs(name, ModelDefinition.class);
    if (model == null) {
      return null;
    }
    return model.toMacro();
  }

  /**
   * Resolve the provided identifier or report an error.
   *
   * @param usage the identifier that should be resolved
   * @param type  the type that the resolved node must have
   * @return the resolved node, or null if it could not be resolved with the given type
   */
  <T extends Node> @Nullable T requireAs(IsId usage, Class<T> type) {
    var origin = findAs(usage, type);
    if (origin != null) {
      return origin;
    }

    // FIXME: Add a custom message if the object was found but not the specified type.
    var suggestions = Levenshtein.suggestions(usage.pathToString(), allSymbolNamesOf(type));
    reportUnkownError(Node.nodeNameFor(type), usage.pathToString(), usage, suggestions);
    return null;
  }

  /**
   * Resolve the provided identifier or report an error.
   *
   * @param usage to be resolved.
   */
  void requireAny(IsId usage) {
    var origin = find(usage);
    if (origin != null || isBuiltin(usage)) {
      return;
    }

    var suggestions = Levenshtein.suggestions(usage.pathToString(), allSymbolNames());
    reportUnkownError("Symbol", usage.pathToString(), usage, suggestions);
  }

  /**
   * Load an instruction by name and return its format.
   *
   * @param instrId Identifier of the instruction.
   * @return the format of that instruction.
   */
  @Nullable
  FormatDefinition requireInstructionFormat(Identifier instrId) {
    var inst = requireAs(instrId, InstructionDefinition.class);
    if (inst == null || inst.formatNode == null) {
      return null;
    }

    return inst.formatNode;
  }

  @Nullable
  FormatDefinition findInstructionFormat(Identifier instrId) {
    var inst = findAs(instrId, InstructionDefinition.class);
    if (inst == null || inst.formatNode == null) {
      return null;
    }

    return inst.formatNode;
  }

  /**
   * Resolves an identifier to a user defined Syntax Type.
   * Throws {@link Diagnostic} if the type doesn't exist.
   *
   * @param identifier of the syntax type.
   * @return the syntax type it refers to
   */
  SyntaxType requireSyntaxType(Identifier identifier) {
    var symbol = resolveMacroName(identifier.name);
    if (symbol instanceof RecordTypeDefinition recordType) {
      return recordType.recordType;
    } else if (symbol instanceof ModelTypeDefinition modelType) {
      return modelType.projectionType;
    }

    // Unfortunately, we need this type to be correctly parsed because,
    // depending on it, we parse the body of the macro differently. So if we
    // don't know what it is, we must exit early.
    throw ParserUtils.unknownSyntaxTypeError(identifier.name, this, identifier.location());
  }

  /**
   * Internal use only.
   * Collects all symbol names in scope that satisfy the given predicate.
   * There is a hard limit described by {@link #MAX_COLLECTED_SYMBOL_NAME_SUGGESTIONS}.
   */
  private void collectAllSymbolNamesWhere(Collection<String> collector, Predicate<Node> pred) {
    symbols.entrySet().stream()
        .filter(entry -> entry.getValue() != null && pred.test(entry.getValue()))
        .map(Map.Entry::getKey)
        .limit(Math.max(0, MAX_COLLECTED_SYMBOL_NAME_SUGGESTIONS - collector.size()))
        .forEach(collector::add);

    if (collector.size() >= MAX_COLLECTED_SYMBOL_NAME_SUGGESTIONS) {
      return;
    }

    if (parent != null) {
      parent.collectAllSymbolNamesWhere(collector, pred);
    }
  }

  /**
   * Internal use only.
   * Collects all symbol names in scope that are instances of the given classes.
   * There is a hard limit described by {@link #MAX_COLLECTED_SYMBOL_NAME_SUGGESTIONS}.
   */
  private void collectAllSymbolNamesOf(Collection<String> collector,
                                       Class<? extends Node>... classes) {
    collectAllSymbolNamesWhere(
        collector,
        node -> Arrays.stream(classes).anyMatch(klass -> klass.isInstance(node))
    );
  }

  /**
   * Returns all symbol names in scope.
   *
   * @return the set of all available names.
   */
  List<String> allSymbolNames() {
    var symbols = new ArrayList<String>();
    collectAllSymbolNamesOf(symbols, Node.class);
    symbols.addAll(builtinNames);
    return symbols;
  }

  /**
   * Returns all symbol names in scope that point to the defined node classes.
   *
   * @param classes that are allowed.
   * @return the set of all available names.
   */
  @SafeVarargs
  final List<String> allSymbolNamesOf(Class<? extends Node>... classes) {
    if (errors.size() > MAX_DIAGNOSTICS_WITH_NAME_SUGGESTIONS) {
      return List.of();
    }

    var symbols = new ArrayList<String>();
    collectAllSymbolNamesOf(symbols, classes);
    return symbols;
  }

  /**
   * Returns all symbol names in scope that point to nodes satisfying the given predicate.
   *
   * @param predicate that must be satisfied.
   * @return the set of all available names.
   */
  final Set<String> allSymbolNamesWhere(Predicate<Node> predicate) {
    var symbols = new HashSet<String>();
    collectAllSymbolNamesWhere(symbols, predicate);
    return symbols;
  }

  /**
   * Returns all symbol names in scope that point to the defined node classes.
   *
   * @param classes that are allowed.
   * @return the set of all available names.
   */
  @SafeVarargs
  final List<String> allMacroSymbolNamesOf(Class<? extends Node>... classes) {
    var matchingNames = macroSymbols.entrySet().stream()
        .filter(entry -> Arrays.stream(classes)
            .anyMatch(klass -> klass.isInstance(entry.getValue())))
        .map(Map.Entry::getKey)
        .toList();

    var names = new ArrayList<>(matchingNames);
    if (parent != null) {
      names.addAll(parent.allMacroSymbolNamesOf(classes));
    }
    return names;
  }

  /**
   * Copies all symbols of the given symbol table into this symbol table.
   * It internally calls {@link #defineSymbol(String, Node)}, so it
   * will register an error in {@link #errors} if there are symbol name conflicts.
   */
  void extendBy(SymbolTable other) {
    // we have to check for each symbol that is is not already in this symbol table
    for (var entry : other.symbols.entrySet()) {
      var name = entry.getKey();
      var symbol = entry.getValue();
      defineSymbol(name, symbol);
    }
    // add macro symbols to this symbol table.
    // #defineSymbol will correctly assign symbol to macroSymbols
    for (var entry : other.macroSymbols.entrySet()) {
      var name = entry.getKey();
      var symbol = entry.getValue();
      defineSymbol(name, symbol);
    }
  }

  private SourceLocation getIdentifierLocation(Node node) {
    if (node instanceof IdentifiableNode identifiableNode) {
      return identifiableNode.identifier().location();
    }

    return node.location();
  }

  private void verifyAvailable(String name, Node origin) {
    if (Type.builtinTypeBases.contains(name) && (origin instanceof UsingDefinition
        || origin instanceof FormatDefinition)) {
      var originLoc = getIdentifierLocation(origin);
      var error = error("Symbol name already used: " + name, originLoc)
          .locationDescription(originLoc, "This name is already claimed by a built-in type.")
          .build();
      errors.add(error);
      return;
    }

    if (!symbols.containsKey(name)) {
      return;
    }

    var otherSymbol = symbols.get(name);
    if (otherSymbol == origin) {
      // if the other origin is the same node, the "redefinition" is ok.
      // this can happen when we have a diamond pattern like isa0 -> abi -> superisa
      // and isa0 -> superisa.
      return;
    }

    var originLoc = getIdentifierLocation(origin);

    var error = error("Symbol name already used: " + name, originLoc)
        .locationDescription(originLoc, "Second definition here.")
        .note("All symbols must have a unique name.");

    if (otherSymbol != null) {
      var otherLoc = getIdentifierLocation(otherSymbol);
      error.locationDescription(otherLoc, "First defined here.");
    }

    errors.add(error.build());
  }

  private void verifyMacroAvailable(String name, Node origin) {
    if (!macroSymbols.containsKey(name)) {
      return;
    }

    var other = macroSymbols.get(name);
    if (other == origin) {
      // if the other origin is the same node, the "redefinition" is ok.
      // this can happen when we have a diamond pattern like isa0 -> abi -> superisa
      // and isa0 -> superisa.
      return;
    }

    var originLocation = getIdentifierLocation(origin);
    var error = error("Macro name already used: " + name, originLocation)
        .locationDescription(originLocation, "Second definition here.")
        .note("All macros must have a unique name.");

    var otherLoc = getIdentifierLocation(other);
    error.locationDescription(otherLoc, "First defined here.");

    errors.add(error.build());
  }

  private void reportUnkownError(String type, String actual, WithLocation locatable,
                                 @Nullable List<String> suggestions) {

    errors.add(error("Unknown %s: \"%s\"".formatted(type, actual), locatable)
        .locationDescription(locatable,
            "No %s with this name exists.", type
        )
        .applyIf(errors.size() < MAX_DIAGNOSTICS_WITH_NAME_SUGGESTIONS
                && suggestions != null
                && !suggestions.isEmpty(),
            (builder) -> builder.suggestions(requireNonNull(suggestions)))
        .build()
    );
  }

  private void reportAlreadyDefined(String error, SourceLocation location,
                                    SourceLocation firstOccurence) {
    errors.add(Diagnostic.error(error, location)
        .locationNote(firstOccurence, "Already defined here.")
        .build());
  }

  /**
   * In VADL symbol resolution has to be done in two passes, collection and resolution.
   * This method makes it easy to run both at once.
   *
   * @param ast for which all symbols should be resolved.
   * @return a list with diagnostics of violations.
   */
  static List<Diagnostic> collectAndResolveSymbols(Ast ast) {
    return ast.timingRecorder.withPassTiming("Symbol Resolution", () -> {
      SymbolCollector.collectSymbols(ast);
      return SymbolResolver.resolveSymbols(ast);
    });
  }

  /**
   * Distributes "SymbolTable" instances across the nodes in the AST.
   * For "let" expressions and statements, symbols for the declared variables are created here.
   * For "instruction" and "assembly" definitions, only an empty child table is created,
   * with a further pass {@link SymbolResolver} actually gathering the fields declared
   * in the linked "format" definition.
   * Before: Ast is fully Macro-expanded
   * After: Ast is fully Macro-expanded and all relevant nodes have "symbolTable" set.
   *
   * @see SymbolResolver
   */
  static class SymbolCollector extends RecursiveAstVisitor {
    private Deque<String> viamPath = new ArrayDeque<>();
    private Deque<SymbolTable> symbolTables = new ArrayDeque<>();

    private static void collectSymbols(Ast ast) {
      var collector = new SymbolCollector();
      ast.definitions.forEach(
          definition -> collector.withSymbols(ast.rootSymbolTable(),
              () -> definition.accept(collector))
      );
    }

    private SymbolCollector() {
    }

    private SymbolTable currentSymbols() {
      return symbolTables.peekLast();
    }

    /**
     * Temporarily substitutes the current symbol table with a provided one, runs the given
     * operation, and restores the original symbol table afterwards.
     *
     * @param symbols  the new symbol table to be used temporarily during runnable execution
     * @param runnable the operation to execute while the provided symbol table is active
     */
    private void withSymbols(SymbolTable symbols, Runnable runnable) {
      this.symbolTables.addLast(symbols);
      try {
        runnable.run();
      } finally {
        this.symbolTables.pollLast();
      }
    }

    /**
     * In most cases the nodes in a definition are in their own scope but in rare cases that's not
     * the desired behavior.
     *
     * @param definition which shouldn't create a new scope.
     */
    private void beforeTravelWithoutScope(Definition definition) {
      if (definition instanceof IdentifiableNode idNode) {
        var name = idNode.identifier().name;
        currentSymbols().defineSymbol(name, definition);
        viamPath.addLast(name);
      } else {
        viamPath.addLast("unknown");
      }
      definition.viamId = new ArrayList<>(viamPath);

      definition.symbolTable = currentSymbols();
    }

    /**
     * In most cases the nodes in a definition are in their own scope but in rare cases that's not
     * the desired behavior.
     *
     * @param definition which shouldn't create a new scope.
     */
    @SuppressWarnings("UnusedVariable")
    private void afterTravelWithoutScope(Definition definition) {
      viamPath.pollLast();
    }


    @Override
    public void beforeTravel(Expr expr) {
      if (expr instanceof IdentifiableNode idNode) {
        currentSymbols().defineSymbol(idNode.identifier().name, expr);
      }

      expr.symbolTable = currentSymbols();
    }

    @Override
    public void beforeTravel(Statement statement) {
      if (statement instanceof IdentifiableNode idNode) {
        currentSymbols().defineSymbol(idNode.identifier().name, statement);
      }

      statement.symbolTable = currentSymbols();
    }

    @Override
    public void beforeTravel(Definition definition) {
      if (definition instanceof IdentifiableNode idNode) {
        var name = idNode.identifier().name;
        currentSymbols().defineSymbol(name, definition);
        viamPath.addLast(name);
      } else {
        viamPath.addLast("unknown");
      }
      definition.viamId = new ArrayList<>(viamPath);

      definition.symbolTable = currentSymbols().createChild();
      symbolTables.addLast(definition.symbolTable);
    }

    @Override
    public void afterTravel(Definition definition) {
      viamPath.pollLast();
      symbolTables.pollLast();
    }

    @Override
    public Void visit(AsmDescriptionDefinition definition) {
      // More complex tasks like this require custom handling.
      beforeTravel(definition);
      definition.annotations.forEach(annotation -> annotation.accept(this));

      definition.abi.accept(this);

      var modifierSymbols = currentSymbols().createChild();
      withSymbols(modifierSymbols,
          () -> definition.modifiers.forEach(modifier -> modifier.accept(this)));

      var directiveSymbols = currentSymbols().createChild();
      withSymbols(directiveSymbols,
          () -> definition.directives.forEach(directive -> directive.accept(this)));

      // add integer negation function to common definitions if not already defined
      // this function is used in the grammar default rules
      if (definition.commonDefinitions.stream().noneMatch(
          def -> def instanceof FunctionDefinition functionDef
              && functionDef.name.path().pathToString()
              .equals(AsmGrammarDefaultRules.BUILTIN_ASM_NEG))) {
        definition.commonDefinitions.add(
            AsmGrammarDefaultRules.asmNegFunctionDefinition(definition.location()));
      }
      definition.commonDefinitions.forEach(
          commonDef -> commonDef.accept(this));
      definition.rules.forEach(rule -> rule.accept(this));

      // get default rules that are not yet defined,
      // collect their symbols and add them to assembly description
      var defaultRules = AsmGrammarDefaultRules.notIncludedDefaultRules(definition.rules);
      defaultRules.forEach(rule -> rule.accept(this));
      definition.rules.addAll(defaultRules);

      afterTravel(definition);
      return null;
    }

    @Override
    public Void visit(AsmDirectiveDefinition definition) {
      // Avoid creating a new scope since the directives must be visible in the parent scope
      beforeTravelWithoutScope(definition);
      definition.annotations.forEach(annotation -> annotation.accept(this));
      currentSymbols().defineSymbol(definition.stringLiteral.toString(), definition);
      afterTravelWithoutScope(definition);
      return null;
    }

    @Override
    public Void visit(AsmGrammarAlternativesDefinition definition) {
      beforeTravel(definition);
      definition.annotations.forEach(annotation -> annotation.accept(this));

      // Each sequence of elements has its own scope
      definition.alternatives.forEach(alternative -> {
        var elementsSymbolTable = currentSymbols().createChild();
        withSymbols(elementsSymbolTable,
            () -> alternative.forEach(element -> element.accept(this)));
      });


      afterTravel(definition);
      return null;
    }

    @Override
    public Void visit(UsingDefinition definition) {
      //
      return super.visit(definition);
    }

    @Override
    public Void visit(AsmGrammarElementDefinition definition) {
      // Avoid creating a new scope since the elements should share the same scope
      beforeTravelWithoutScope(definition);
      definition.symbolTable = currentSymbols();
      definition.forEachChild(this::travel);
      afterTravelWithoutScope(definition);
      return null;
    }

    @Override
    public Void visit(AsmModifierDefinition definition) {
      beforeTravelWithoutScope(definition);
      definition.symbolTable = currentSymbols();

      // This isn't a identifyableNode so we need to add custom handling here.
      currentSymbols().defineSymbol(definition.stringLiteral.toString(), definition);

      definition.forEachChild(c -> {
        if (c != definition.stringLiteral) {
          travel(c);
        }
      });

      afterTravelWithoutScope(definition);
      return null;
    }

    @Override
    public Void visit(EnumerationDefinition definition) {
      beforeTravel(definition);
      definition.annotations.forEach(annotation -> annotation.accept(this));

      // Insert all fields into the symbol table.
      if (definition.enumType != null) {
        definition.enumType.accept(this);
      }
      for (EnumerationDefinition.Entry entry : definition.entries) {
        entry.identifier().symbolTable = currentSymbols();
        currentSymbols().defineSymbol(entry.identifier().name, entry);
        if (entry.value != null) {
          entry.value.accept(this);
        }
      }

      afterTravel(definition);
      return null;
    }

    @Override
    public Void visit(ImportDefinition definition) {
      // This isn't a identifyableNode so we need to add custom handling here.
      currentSymbols().importFrom(definition.moduleAst, definition.importedSymbols);
      return null;
    }

    @Override
    public Void visit(LetStatement statement) {
      beforeTravel(statement);

      // The identifiers of the let must be visible in it's children
      var childTable = currentSymbols().createChild();
      statement.symbolTable = childTable;
      statement.identifiers().forEach(identifier -> {
        childTable.defineSymbol(identifier.name, statement);
      });
      withSymbols(childTable, () -> statement.forEachChild(this::travel));

      afterTravel(statement);
      return null;
    }

    @Override
    public Void visit(ForallStatement statement) {
      beforeTravel(statement);

      // The identifiers of the for must be visible in it's children
      var childTable = currentSymbols().createChild();
      statement.symbolTable = childTable;
      statement.indices.forEach(index -> {
        index.identifier().symbolTable = childTable;
        childTable.defineSymbol(index.identifier().name, statement);
        if (index.typeLiteral != null) {
          index.typeLiteral.accept(this);
        }
        index.domain.accept(this);
      });
      withSymbols(childTable, () -> statement.body.accept(this));

      afterTravel(statement);
      return null;
    }

    @Override
    public Void visit(AliasDefinition definition) {
      beforeTravel(definition);
      definition.forEachChild(this::travel);
      afterTravel(definition);
      return null;
    }

    @Override
    public Void visit(LetExpr expr) {
      beforeTravel(expr);

      // The identifiers of the let must be visible in it's children
      var childTable = currentSymbols().createChild();
      expr.symbolTable = childTable;
      expr.identifiers().forEach(identifier -> {
        childTable.defineSymbol(identifier.name, expr);
      });
      withSymbols(childTable, () -> expr.forEachChild(this::travel));

      afterTravel(expr);
      return null;
    }

    @Override
    public Void visit(ForallExpr expr) {
      beforeTravel(expr);

      // The identifiers of the for must be visible in it's children
      var childTable = currentSymbols().createChild();
      expr.symbolTable = childTable;
      expr.indices.forEach(index -> {
        childTable.defineSymbol(index.identifier().name, expr);
        index.identifier().symbolTable = childTable;
        if (index.typeLiteral != null) {
          index.typeLiteral.accept(this);
        }
        index.domain.accept(this);
      });
      if (expr.foldAction != null) {
        switch (expr.foldAction) {
          case Identifier id -> id.accept(this);
          case IdentifierPath id -> id.accept(this);
          case BinOp binOp -> {
          }
          default -> throw new IllegalStateException("Unknown fold action: " + expr.foldAction);
        }
      }
      withSymbols(childTable, () -> expr.body.accept(this));

      afterTravel(expr);
      return null;
    }

    @Override
    public Void visit(ForallThenExpr expr) {
      beforeTravel(expr);

      // The identifiers of the forall must be visible in its children
      var childTable = currentSymbols().createChild();
      expr.symbolTable = childTable;
      expr.indices.forEach(index -> {
        childTable.defineSymbol(index.identifier().name, expr);
        index.symbolTable = childTable;
        index.identifier().symbolTable = childTable;

        for (IsId o : index.operations) {
          withSymbols(currentSymbols(), () -> ((Identifier) o).accept(this));
        }
      });
      withSymbols(childTable, () -> expr.thenExpr.accept(this));

      afterTravel(expr);
      return null;
    }

    @Override
    public Void visit(ExistsInExpr expr) {
      beforeTravel(expr);
      for (IsId o : expr.operations) {
        ((Identifier) o).accept(this);
      }
      afterTravel(expr);
      return null;
    }

    @Override
    public Void visit(ExistsInThenExpr expr) {
      beforeTravel(expr);

      // The identifiers of the exists must be visible in its children
      var childTable = currentSymbols().createChild();
      expr.symbolTable = childTable;
      expr.indices.forEach(index -> {
        childTable.defineSymbol(index.identifier().name, expr);
        index.symbolTable = childTable;
        index.identifier().symbolTable = childTable;

        for (IsId o : index.operations) {
          withSymbols(currentSymbols(), () -> ((Identifier) o).accept(this));
        }
      });
      withSymbols(childTable, () -> expr.thenExpr.accept(this));

      afterTravel(expr);
      return null;
    }

    @Override
    public Void visit(NewLabelStatement statement) {
      beforeTravel(statement);
      statement.symbolTable = currentSymbols();
      statement.labelId().symbolTable = currentSymbols();
      currentSymbols().defineSymbol(statement.labelId.pathToString(), statement);
      afterTravel(statement);
      return null;
    }
  }

  /**
   * Resolves identifiers used in expressions, as well as types used in definitions,
   * and verifies that they actually exist in the VADL file.
   * Before: AST is fully Macro-expanded and all relevant nodes have "symbolTable" set.
   * After: AST nodes have their resolved node references set.
   */
  static class SymbolResolver extends RecursiveAstVisitor {

    public static List<Diagnostic> resolveSymbols(Ast ast) {
      var resolver = new SymbolResolver();
      for (Definition definition : ast.definitions) {
        definition.accept(resolver);
      }
      return requireNonNull(ast.rootSymbolTable).errors;
    }

    private SymbolResolver() {
    }

    @Override
    public Void visit(Identifier expr) {
      expr.symbolTable().requireAny(expr);
      return null;
    }

    @Override
    public Void visit(IdentifierPath expr) {
      expr.symbolTable().requireAny(expr);
      return null;
    }

    @Override
    public Void visit(ModelDefinition expr) {
      // Skip Model Definitions at all.
      // They will be resolved once they are expanded.
      return null;
    }

    @Override
    public Void visit(AnnotationDefinition definition) {
      definition.annotation = AnnotationTable.createAnnotation(definition);


      if (definition.annotation == null) {
        var suggestions = Levenshtein.suggestions(
            definition.name(),
            AnnotationTable.availableAnnotationDeclarations(definition.target.getClass()),
            AnnotationDeclaration::name
        ).stream().map(AnnotationDeclaration::usageString).toList();

        var diagnostic =
            error("Unknown Annotation: `%s`".formatted(definition.name()), definition)
                .locationDescription(definition.location(),
                    "No annotation with this name exists on %s",
                    definition.target)
                .suggestions(suggestions);

        definition.symbolTable().errors.add(diagnostic.build());
        return null;
      }

      try {
        definition.annotation.resolveName(definition, this);
      } catch (Diagnostic d) {
        definition.symbolTable().errors.add(d);
      }
      return null;
    }

    @Override
    public Void visit(TypeLiteral expr) {
      // Skip the basetype of the expr and let the typechecker verify it's correct.
      beforeTravel(expr);

      // Only visit the baseType if we are certain that we'll find it.
      // Because the typechecker does the real resolution, checking and error reporting here, but
      // it is nice for the lsp to already have some resolution if we can get to it easily.
      if (expr.symbolTable().findAs(expr.baseType, Definition.class) != null) {
        expr.symbolTable().find(expr.baseType);
      }

      expr.sizeIndices.forEach(index -> index.accept(this));

      afterTravel(expr);
      return null;
    }

    @Override
    public Void visit(InstructionSetDefinition definition) {
      // Import all symbols from the extending ISA.
      beforeTravel(definition);

      for (var isa : definition.extending) {
        var extending = definition.symbolTable().requireAs(isa, InstructionSetDefinition.class);
        if (extending != null) {
          definition.symbolTable().extendBy(extending.symbolTable());
        }
      }

      definition.forEachChild(this::travel);
      afterTravel(definition);
      return null;
    }

    @Override
    public Void visit(FloatTypeDefinition definition) {
      beforeTravel(definition);
      // FloatTypeDefinition has no @Child fields, so it's not in NodeChildrenRegistry,
      // and we need to manually visit annotations
      definition.annotations.forEach(this::travel);
      afterTravel(definition);
      return null;
    }

    @Override
    public Void visit(InstructionDefinition definition) {
      // Import all symbols from the format.
      beforeTravel(definition);

      var format =
          definition.symbolTable().requireAs(definition.typeIdentifier(), FormatDefinition.class);
      if (format != null) {
        definition.symbolTable().extendBy(format.symbolTable());
        definition.formatNode = format;
      }

      definition.forEachChild(this::travel);
      afterTravel(definition);
      return null;
    }

    @Override
    public Void visit(AssemblyDefinition definition) {
      // Link instruction and import all symbols from the instruction format.
      beforeTravel(definition);

      for (IdentifierOrPlaceholder identifier : definition.identifiers) {
        var pseudoInstr = definition.symbolTable()
            .findAs((Identifier) identifier, PseudoInstructionDefinition.class);
        if (pseudoInstr != null) {
          definition.instructionNodes.add(pseudoInstr);
          definition.symbolTable().extendBy(pseudoInstr.symbolTable());
          if (pseudoInstr.assemblyDefinition != null) {
            definition.symbolTable().reportAlreadyDefined(
                "Assembly for %s pseudo instruction is already defined".formatted(
                    identifier),
                identifier.location(), pseudoInstr.assemblyDefinition.location());
          }
          pseudoInstr.assemblyDefinition = definition;
        } else {
          var instr =
              definition.symbolTable().findAs((Identifier) identifier, InstructionDefinition.class);
          if (instr != null) {
            definition.instructionNodes.add(instr);

            if (instr.assemblyDefinition != null) {
              definition.symbolTable().reportAlreadyDefined(
                  "Assembly for %s instruction is already defined".formatted(
                      identifier),
                  identifier.location(), instr.assemblyDefinition.location());
            }
            instr.assemblyDefinition = definition;
          }
          var format = definition.symbolTable().requireInstructionFormat((Identifier) identifier);
          // FIXME: Isn't there a bug if an assembly inherits from multiple instructions?
          // Because I think this code would just import all symbols but actually none of the
          // formats should be imported because they wouldn't be visible in all instructinos.
          if (format != null) {
            definition.symbolTable().extendBy(format.symbolTable());
          }
        }
      }

      definition.forEachChild(this::travel);
      afterTravel(definition);
      return null;
    }

    @Override
    public Void visit(EncodingDefinition definition) {
      // Link instruction and import all symbols from the instruction format.
      beforeTravel(definition);

      var inst =
          definition.symbolTable().requireAs(definition.identifier(), InstructionDefinition.class);
      if (inst != null) {
        if (inst.encodingDefinition != null) {
          definition.symbolTable().reportAlreadyDefined(
              "Encoding for %s instruction is already defined".formatted(definition.identifier()),
              definition.location(), inst.encodingDefinition.location());
        } else {
          inst.encodingDefinition = definition;
        }
      }

      var format = definition.symbolTable().requireInstructionFormat(definition.identifier());
      if (format != null) {
        definition.formatNode = format;
        for (var item : definition.encodings.items) {
          var fieldEncoding = (EncodingDefinition.EncodingField) item;

          // Verify that the field specified really is a field in the encoding
          var field = fieldEncoding.identifier();
          if (!format.hasField(field.name)) {
            var suggestions = Levenshtein.suggestions(
                field.name,
                format.fields.stream()
                    .map(f -> f.identifier().name).toList());

            definition.symbolTable()
                .reportUnkownError("Field", field.name, field.location(), suggestions);
          }

          // Verify that the value is visited.
          fieldEncoding.value.accept(this);
        }
      }

      definition.annotations.forEach(this::travel);
      afterTravel(definition);
      return null;
    }

    @Override
    public Void visit(ApplicationBinaryInterfaceDefinition definition) {
      beforeTravel(definition);

      var isa =
          definition.symbolTable()
              .requireAs(definition.isa, InstructionSetDefinition.class);
      if (isa != null) {
        definition.isaNode = isa;
        definition.symbolTable().extendBy(isa.symbolTable());
      }

      definition.forEachChild(this::travel);
      afterTravel(definition);
      return null;
    }

    @Override
    public Void visit(MicroArchitectureDefinition definition) {
      beforeTravel(definition);

      var isa = definition.symbolTable().requireAs(definition.isa, InstructionSetDefinition.class);
      if (isa != null) {
        //definition.isaNode = isa;
        definition.symbolTable().extendBy(isa.symbolTable());
      }

      definition.forEachChild(this::travel);
      afterTravel(definition);
      return null;
    }

    @Override
    public Void visit(ProcessorDefinition definition) {
      beforeTravel(definition);


      InstructionSetDefinition isa = definition.symbolTable()
          .requireAs(definition.implementedIsa, InstructionSetDefinition.class);
      if (isa != null) {
        definition.symbolTable().extendBy(isa.symbolTable());
      }

      if (definition.abi != null) {
        var abi = definition.symbolTable()
            .requireAs(definition.abi, ApplicationBinaryInterfaceDefinition.class);
        if (abi != null) {
          definition.symbolTable().extendBy(abi.symbolTable());
        }
      }

      definition.forEachChild(this::travel);
      afterTravel(definition);
      return null;
    }

    @Override
    public Void visit(CpuMemoryRegionDefinition def) {
      beforeTravel(def);

      def.symbolTable().requireAs(def.memoryRef, MemoryDefinition.class);

      def.forEachChild(this::travel);
      afterTravel(def);
      return null;
    }

    @Override
    public Void visit(InstructionCallStatement statement) {
      beforeTravel(statement);

      var instr =
          statement.symbolTable().findAs(statement.id(), InstructionDefinition.class);
      var format = statement.symbolTable().findInstructionFormat(statement.id());
      if (format != null) {
        statement.instrDef = instr;
        for (var namedArgument : statement.namedArguments) {
          FormatField foundField = format.getField(namedArgument.identifier().name);
          if (foundField == null) {
            var suggestions = Levenshtein.suggestions(namedArgument.identifier().name,
                format.fieldsWithoutEncodingPredicate()
                    .map(f -> f.identifier().name).toList());

            statement.symbolTable().reportUnkownError(
                "Field",
                namedArgument.identifier().name,
                namedArgument.location(),
                suggestions);
          }
          namedArgument.value.accept(this);
        }
      } else {
        var pseudoInstr =
            statement.symbolTable()
                .findAs(statement.id(), PseudoInstructionDefinition.class);
        if (pseudoInstr != null) {
          statement.instrDef = pseudoInstr;
          for (var namedArgument : statement.namedArguments) {
            Parameter foundParam = null;
            for (var param : pseudoInstr.params) {
              if (param.identifier().name.equals(namedArgument.identifier().name)) {
                foundParam = param;
                break;
              }
            }
            if (foundParam == null) {
              var suggestions =
                  Levenshtein.suggestions(namedArgument.identifier().name,
                      pseudoInstr.params.stream().map(p -> p.identifier().name).toList());
              statement.symbolTable()
                  .reportUnkownError("Instruction Parameter", namedArgument.identifier().name,
                      namedArgument.name, suggestions);
            }
            namedArgument.value.accept(this);
          }
        } else {
          // FIXME: Limit suggestions to instructions
          var suggestions = Levenshtein.suggestions(statement.id().name,
              statement.symbolTable().allSymbolNamesOf(InstructionDefinition.class,
                  PseudoInstructionDefinition.class));

          statement.symbolTable()
              .reportUnkownError("Instruction", statement.id().name, statement.location(),
                  suggestions);
        }
      }
      for (Expr unnamedArgument : statement.unnamedArguments) {
        unnamedArgument.accept(this);
      }

      afterTravel(statement);
      return null;
    }

    @Override
    public Void visit(AsmDescriptionDefinition definition) {
      beforeTravel(definition);

      var abi = definition.symbolTable()
          .requireAs(definition.abi, ApplicationBinaryInterfaceDefinition.class);
      if (abi != null) {
        definition.symbolTable().extendBy(abi.symbolTable());
      }

      definition.forEachChild(this::travel);
      afterTravel(definition);
      return null;
    }

    @Override
    public Void visit(AsmModifierDefinition definition) {
      beforeTravel(definition);

      var relocation = definition.relocation;
      definition.symbolTable().requireAs(relocation, RelocationDefinition.class);
      definition.forEachChild(this::travel);
      afterTravel(definition);
      return null;
    }


    @Override
    public Void visit(AsmDirectiveDefinition definition) {
      beforeTravel(definition);
      definition.annotations.forEach(this::travel);

      // Only do rudimentary checks here, the rest is done in the typechecker.
      if (!AsmDirective.isAsmDirective(definition.builtinDirective.name)) {
        var suggestions = Levenshtein.suggestions(definition.builtinDirective.name,
            Arrays.stream(AsmDirective.values()).map(Enum::toString).toList()
        );

        definition.symbolTable()
            .reportUnkownError("Asm Directive", definition.builtinDirective.name,
                definition.builtinDirective, suggestions);
      }

      afterTravel(definition);
      return null;
    }

    @Override
    public Void visit(AsmGrammarElementDefinition definition) {
      beforeTravel(definition);

      // The attribute needs special handling here
      if (definition.attribute != null) {
        // If attrSymbol is not null, attribute refers to local variable
        // Else attribute is handled by matching in the AsmParser
        var attrSymbol = definition.symbolTable().findAs(definition.attribute, Definition.class);
        definition.isAttributeLocalVar = attrSymbol instanceof AsmGrammarLocalVarDefinition;
      }

      // All other children have the default handling
      definition.forEachChild(c -> {
        if (c != definition.attribute) {
          travel(c);
        }
      });

      afterTravel(definition);
      return null;
    }

    @Override
    public Void visit(AsmGrammarLocalVarDefinition definition) {
      beforeTravel(definition);
      definition.annotations.forEach(this::travel);

      // FIXME: @benjaminkasper99 should we maybe make "null" a symbol that is always in the
      // symboltable so we can avoid this special treatment here?
      if (definition.asmLiteral.id != null && !definition.asmLiteral.id.name.equals("null")) {
        definition.asmLiteral.accept(this);
      }

      afterTravel(definition);
      return null;
    }

    @Override
    public Void visit(AsmGrammarLiteralDefinition definition) {
      beforeTravel(definition);

      // Id needs special treatment
      if (definition.id != null) {
        var idSymbol = definition.symbolTable().find(definition.id);
        if (idSymbol == null) {
          var suggestions = Levenshtein.suggestions(
              definition.id.name,
              definition.symbolTable().allSymbolNames());

          definition.symbolTable()
              .reportUnkownError("Asm Grammar Rule", definition.id.name, definition.id,
                  suggestions);
        }
      }


      // Resolve all other children like always
      // FIXME: At the moment id isn't even a child but I'm not sure if it should be so check in
      // later once we know it.
      definition.forEachChild(c -> {
        if (c != definition.id) {
          travel(c);
        }
      });


      afterTravel(definition);
      return null;
    }

    @Override
    public Void visit(AsmGrammarTypeDefinition definition) {
      beforeTravel(definition);
      definition.annotations.forEach(this::travel);

      if (!AsmType.isInputAsmType(definition.id.name)) {
        var suggestions = Levenshtein.suggestions(definition.id.name,
            AsmType.ASM_TYPES.values().stream().map(AsmType::name).toList());

        definition.symbolTable()
            .reportUnkownError("Asm Type", definition.id.name, definition.id, suggestions);
      }

      afterTravel(definition);
      return null;
    }


  }
}
