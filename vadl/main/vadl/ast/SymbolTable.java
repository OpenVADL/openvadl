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

import static java.util.Objects.requireNonNull;
import static vadl.error.Diagnostic.error;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import vadl.error.Diagnostic;
import vadl.error.DiagnosticBuilder;
import vadl.types.BuiltInTable;
import vadl.types.asmTypes.AsmType;
import vadl.utils.Levenshtein;
import vadl.utils.SourceLocation;
import vadl.utils.WithLocation;

class SymbolTable {
  @Nullable
  SymbolTable parent = null;
  final List<SymbolTable> children = new ArrayList<>();
  final Map<String, Symbol> symbols = new HashMap<>();
  final Map<String, AstSymbol> macroSymbols = new HashMap<>();
  // the errors list is the same obj as the parent's error list
  List<Diagnostic> errors = new ArrayList<>();

  sealed interface Symbol {
  }

  record AstSymbol(Node origin) implements Symbol {
  }

  record BuiltInSymbol() implements Symbol {
  }

  /**
   * Load all builtin function names into the scope so that the names are reserved and can be used
   * (in some contextes). However, we don't store any useful informations with them and the next
   * passes (the type-checker) needs to know on it's own how to deal with them.
   */
  void loadBuiltins() {
    // Load all "real" builtins
    BuiltInTable.builtIns().map(BuiltInTable.BuiltIn::name)
        .forEach(name -> symbols.put(name, new BuiltInSymbol()));

    // Add pseudo buildins
    symbols.put("VADL::mod", new BuiltInSymbol());
    symbols.put("VADL::div", new BuiltInSymbol());
    symbols.put("start", new BuiltInSymbol());
    symbols.put("executable", new BuiltInSymbol());
    symbols.put("halt", new BuiltInSymbol());
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
        reportError("Unresolved symbol " + name, location);
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
    SymbolTable child = new SymbolTable();
    child.parent = this;
    child.errors = this.errors;
    this.children.add(child);
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


  void defineSymbol(String name, Node origin) {
    if (origin instanceof ModelDefinition || origin instanceof ModelTypeDefinition
        || origin instanceof RecordTypeDefinition) {
      verifyMacroAvailable(name, origin);
      macroSymbols.put(name, new AstSymbol(origin));
    } else {
      verifyAvailable(name, origin);
      symbols.put(name, new AstSymbol(origin));
    }
  }

  <T extends Node & IdentifiableNode> void defineSymbol(T origin) {
    var name = origin.identifier().name;
    defineSymbol(name, origin);
  }

  void addModelDefinition(ModelDefinition modelDefinition) {
    // Note: We cannot use .identifier here because the identifier might not be initialized with
    // macros that generate macros.
    defineSymbol(modelDefinition.toMacro().name().name, modelDefinition);
  }

  @Nullable
  private Symbol resolveSymbol(String name) {
    var symbol = symbols.get(name);

    if (symbol != null) {
      return symbol;
    }

    if (parent != null) {
      return parent.resolveSymbol(name);
    }

    return null;
  }

  @Nullable
  private Symbol resolveBuiltinSymbol(String name) {
    var root = this;
    while (root.parent != null) {
      root = root.parent;
    }

    // FIXME: I don't think the namespace prefix should be in here
    var symbol = root.resolveSymbol(name);
    if (symbol == null) {
      symbol = root.resolveSymbol("VADL::" + name);
    }

    if (symbol instanceof BuiltInSymbol) {
      return symbol;
    }
    return null;
  }

  @Nullable
  private Symbol resolveSymbolPath(List<String> path) {
    // The vadl namespace is a pseudo namespace and points to the root and its buitlin functions
    if (path.size() == 2 && path.get(0).equalsIgnoreCase("vadl")) {
      return resolveBuiltinSymbol(path.get(1));
    }

    if (path.size() == 1) {
      return symbols.get(path.get(0));
    }

    var namespace = (AstSymbol) resolveSymbol(path.get(0));
    if (namespace == null) {
      return null;
    }

    return namespace.origin.symbolTable().resolveSymbolPath(path.subList(1, path.size()));
  }

  @Nullable
  private Symbol resolve(Identifier ident) {
    var symbol = resolveSymbol(ident.name);
    if (symbol instanceof AstSymbol(Node origin)) {
      ident.target = origin;
    }
    return symbol;
  }

  @Nullable
  private Symbol resolve(IdentifierPath path) {
    var symbol = resolveSymbolPath(path.pathToSegments());
    if (symbol instanceof AstSymbol(Node origin)) {
      path.target = origin;
    }
    return symbol;
  }

  @Nullable
  private Symbol resolve(IsId id) {
    return switch (id) {
      case Identifier ident -> resolve(ident);
      case IdentifierPath path -> resolve(path);
      default -> throw new IllegalArgumentException("Illegal identifier type: " + id.getClass());
    };
  }

  @Nullable
  private Node resolveMacroSymbol(String name) {
    var symbol = macroSymbols.get(name);
    if (symbol == null && parent != null) {
      return parent.resolveMacroSymbol(name);
    }

    if (symbol == null) {
      return null;
    }

    return symbol.origin;
  }

  @Nullable
  Macro getMacro(String name) {
    var origin = resolveMacroSymbol(name);
    if (origin instanceof ModelDefinition) {
      return ((ModelDefinition) origin).toMacro();
    }

    return null;
  }


  <T extends Node> @Nullable T findAs(IsId usage, Class<T> type) {
    var symbol = resolve(usage);
    return switch (symbol) {
      case null -> null;
      case AstSymbol astSymbol ->
          type.isInstance(astSymbol.origin) ? type.cast(astSymbol.origin) : null;
      case BuiltInSymbol ignored -> null;
    };
  }

  /**
   * This will add an error to {@link #errors} if the identifier couldn't be resolved
   * to a node of the given type.
   * In this case it will return null, so the user must check the result before continuing.
   *
   * @param usage the identifier that should be resolved
   * @param type  the type that the resolved node must have
   * @return the resolved node, or null if it could not be resolved with the given type
   */
  // FIXME: I don't like how it's called require but still returns null
  private <T extends Node> @Nullable T requireAs(IsId usage, Class<T> type) {
    var origin = findAs(usage, type);
    if (origin != null) {
      return origin;
    }
    errors.add(error("Unknown name " + usage.pathToString(), usage).build());
    return null;
  }

  /**
   * Finds the node for the given Id and throws the error provided by the error builder
   * if the node could not be found.
   */
  Node require(IsId name, Supplier<DiagnosticBuilder> errorBuilder) {
    var node = requireAs(name, Node.class);
    if (node == null) {
      throw errorBuilder.get().build();
    }
    return node;
  }

  /**
   * Finds the symbol for a given {@link IsId} and throws the error provided by the error
   * builder if the symbol was not found.
   */
  Symbol requireSymbol(IsId name, Supplier<DiagnosticBuilder> errorBuilder) {
    var symbol = resolve(name);
    if (symbol == null) {
      throw errorBuilder.get().build();
    }
    return symbol;
  }

  /**
   * This allows the {@link Parser} to find an ISA during parsing.
   * This is only possible for ISA definitions and only used by the parser.
   */
  @Nullable
  InstructionSetDefinition requireIsaDef(IsId usage) {
    return requireAs(usage, InstructionSetDefinition.class);
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
    var symbol = resolveMacroSymbol(identifier.name);
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
   * Returns all symbol names in scope.
   *
   * @return the set of all available names.
   */
  Set<String> allSymbolNames() {
    var names = new HashSet<>(symbols.keySet());
    if (parent != null) {
      names.addAll(parent.allSymbolNames());
    }
    return names;
  }

  /**
   * Returns all symbol names in scope that point to the defined node classes.
   *
   * @param classes that are allowed.
   * @return the set of all available names.
   */
  @SafeVarargs
  final Set<String> allSymbolNamesOf(Class<? extends Node>... classes) {
    var matchingNames = symbols.entrySet().stream()
        .filter(entry -> entry.getValue() instanceof AstSymbol astSymbol
            && Arrays.stream(classes).anyMatch(klass -> klass.isInstance(astSymbol.origin)))
        .map(Map.Entry::getKey)
        .toList();

    var names = new HashSet<>(matchingNames);
    if (parent != null) {
      names.addAll(parent.allSymbolNamesOf(classes));
    }
    return names;
  }

  /**
   * Returns all symbol names in scope that point to the defined node classes.
   *
   * @param classes that are allowed.
   * @return the set of all available names.
   */
  @SafeVarargs
  final Set<String> allMacroSymbolNamesOf(Class<? extends Node>... classes) {
    var matchingNames = macroSymbols.entrySet().stream()
        .filter(entry -> Arrays.stream(classes)
            .anyMatch(klass -> klass.isInstance(entry.getValue().origin)))
        .map(Map.Entry::getKey)
        .toList();

    var names = new HashSet<>(matchingNames);
    if (parent != null) {
      names.addAll(parent.allSymbolNamesOf(classes));
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
      switch (symbol) {
        case AstSymbol astSymbol -> defineSymbol(name, astSymbol.origin);
        case BuiltInSymbol ignored -> { /* do nothing, already defined */ }
      }
    }
    // add macro symbols to this symbol table.
    // #defineSymbol will correctly assign symbol to macroSymbols
    for (var entry : other.macroSymbols.entrySet()) {
      var name = entry.getKey();
      AstSymbol symbol = entry.getValue();
      defineSymbol(name, symbol.origin);
    }
  }

  private SourceLocation getIdentifierLocation(Node node) {
    if (node instanceof IdentifiableNode identifiableNode) {
      return identifiableNode.identifier().location();
    }

    return node.location();
  }

  private void verifyAvailable(String name, Node origin) {
    if (!symbols.containsKey(name)) {
      return;
    }

    var otherSymbol = symbols.get(name);
    if (otherSymbol instanceof AstSymbol astSymbol
        && astSymbol.origin == origin) {
      // if the other origin is the same node, the "redefinition" is ok.
      // this can happen when we have a diamond pattern like isa0 -> abi -> superisa
      // and isa0 -> superisa.
      return;
    }

    var originLoc = getIdentifierLocation(origin);

    var error = error("Symbol name already used: " + name, originLoc)
        .locationDescription(originLoc, "Second definition here.")
        .note("All symbols must have a unique name.");


    if (otherSymbol instanceof BuiltInSymbol) {
      error.description("`%s` is a builtin and cannot be used as a name", name);
    } else if (otherSymbol instanceof AstSymbol astSymbol) {
      var other = astSymbol.origin;
      var otherLoc = getIdentifierLocation(other);
      error.locationDescription(otherLoc, "First defined here.");
    }

    errors.add(error.build());
  }

  private void verifyMacroAvailable(String name, Node origin) {
    if (!macroSymbols.containsKey(name)) {
      return;
    }

    var other = macroSymbols.get(name).origin();
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

    var diagnostic = error("Unknown %s: \"%s\"".formatted(type, actual), locatable)
        .locationDescription(locatable,
            "No %s with this name exists.", type.toLowerCase(Locale.US)
        );

    if (suggestions != null && !suggestions.isEmpty()) {
      diagnostic =
          diagnostic.suggestions(suggestions);
    }

    errors.add(diagnostic.build());
  }

  private void reportError(String error, SourceLocation location) {
    errors.add(error(error, location)
        .build());
  }

  private void reportAlreadyDefined(String error, SourceLocation location,
                                    SourceLocation firstOccurence) {
    errors.add(Diagnostic.error(error, location)
        .locationNote(firstOccurence, "Already defined here.")
        .build());
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

    public SymbolCollector() {
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

    void collectSymbols(SymbolTable symbols, Definition definition) {
      withSymbols(symbols, () -> definition.accept(this));
    }

    void collectSymbols(SymbolTable symbols, Statement stmt) {
      withSymbols(symbols, () -> stmt.accept(this));
    }

    void collectSymbols(SymbolTable symbols, Expr expr) {
      withSymbols(symbols, () -> expr.accept(this));
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
      definition.viamId = String.join("::", viamPath);

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
      definition.viamId = String.join("::", viamPath);

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
        definition.commonDefinitions.add(AsmGrammarDefaultRules.asmNegFunctionDefinition());
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
      definition.children().forEach(this::travel);
      afterTravelWithoutScope(definition);
      return null;
    }

    @Override
    public Void visit(AsmModifierDefinition definition) {
      beforeTravelWithoutScope(definition);
      definition.symbolTable = currentSymbols();

      // This isn't a identifyableNode so we need to add custom handling here.
      currentSymbols().defineSymbol(definition.stringLiteral.toString(), definition);

      definition.children().stream()
          .filter(c -> c != definition.stringLiteral)
          .forEach(this::travel);

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
        entry.name.symbolTable = currentSymbols();
        currentSymbols().defineSymbol(entry.name.name, entry);
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
      statement.identifiers.forEach(identifier -> {
        childTable.defineSymbol(identifier.name, statement);
      });
      withSymbols(childTable, () -> statement.children().forEach(this::travel));

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
        childTable.defineSymbol(index.name.name, statement);
        index.domain.accept(this);
      });
      withSymbols(childTable, () -> statement.body.accept(this));

      afterTravel(statement);
      return null;
    }

    @Override
    public Void visit(LetExpr expr) {
      beforeTravel(expr);

      // The identifiers of the let must be visible in it's children
      var childTable = currentSymbols().createChild();
      expr.symbolTable = childTable;
      expr.identifiers.forEach(identifier -> {
        childTable.defineSymbol(identifier.name, expr);
      });
      withSymbols(childTable, () -> expr.children().forEach(this::travel));

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
        index.domain.accept(this);
      });
      withSymbols(childTable, () -> expr.body.accept(this));

      afterTravel(expr);
      return null;
    }

    @Override
    public Void visit(ForallThenExpr expr) {
      beforeTravel(expr);

      // The identifiers of the for must be visible in it's children
      var childTable = currentSymbols().createChild();
      expr.symbolTable = childTable;
      expr.indices.forEach(index -> {
        childTable.defineSymbol(index.identifier().name, expr);
      });
      withSymbols(childTable, () -> expr.thenExpr.accept(this));

      afterTravel(expr);
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

    public List<Diagnostic> resolveSymbols(Ast ast) {
      var resolveStartTime = System.nanoTime();
      for (Definition definition : ast.definitions) {
        definition.accept(this);
      }
      ast.passTimings.add(new Ast.PassTimings("Symbol resolution",
          (System.nanoTime() - resolveStartTime) / 1000_000));
      return requireNonNull(ast.rootSymbolTable).errors;
    }


    @Override
    public Void visit(Identifier expr) {
      var symbol = expr.symbolTable().resolve(expr);
      if (symbol == null) {
        var suggestions =
            Levenshtein.suggestions(expr.pathToString(), expr.symbolTable().allSymbolNames());

        expr.symbolTable()
            .reportUnkownError("Symbol", expr.pathToString(), expr.location(), suggestions);
      }
      return null;
    }

    @Override
    public Void visit(IdentifierPath expr) {
      var symbol = expr.symbolTable().resolve(expr);
      if (symbol == null) {
        var suggestions =
            Levenshtein.suggestions(expr.pathToString(), expr.symbolTable().allSymbolNames());

        expr.symbolTable()
            .reportUnkownError("Symbol", expr.pathToString(), expr.location(), suggestions);
      }
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
        requireNonNull(definition.symbolTable).errors.add(d);
      }
      return null;
    }

    @Override
    public Void visit(TypeLiteral expr) {
      // Skip the basetype of the expr and let the typechecker verify it's correct.
      beforeTravel(expr);

      expr.sizeIndices.forEach(index -> index.forEach(e -> e.accept(this)));

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

      definition.children().forEach(this::travel);
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

      definition.children().forEach(this::travel);
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

      definition.children().forEach(this::travel);
      afterTravel(definition);
      return null;
    }

    @Override
    public Void visit(EncodingDefinition definition) {
      // Link instruction and import all symbols from the instruction format.
      beforeTravel(definition);
      definition.annotations.forEach(this::travel);

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
          var field = fieldEncoding.field;
          if (format.getField(field.name) == null) {
            var suggestions = Levenshtein.suggestions(
                field.name,
                format.fields.stream().map(f -> f.identifier().name).toList());

            definition.symbolTable()
                .reportUnkownError("Field", field.name, field.location(), suggestions);
          }

          // Verify that the value is visited.
          fieldEncoding.value.accept(this);
        }
      }

      afterTravel(definition);
      return null;
    }

    @Override
    public Void visit(ApplicationBinaryInterfaceDefinition definition) {
      beforeTravel(definition);

      var isa =
          definition.symbolTable()
              .requireAs((Identifier) definition.isa, InstructionSetDefinition.class);
      if (isa != null) {
        definition.isaNode = isa;
        definition.symbolTable().extendBy(isa.symbolTable());
      }

      definition.children().forEach(this::travel);
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

      definition.children().forEach(this::travel);
      afterTravel(definition);
      return null;
    }

    @Override
    public Void visit(CpuMemoryRegionDefinition def) {
      beforeTravel(def);

      def.symbolTable().requireAs(def.memoryRef, MemoryDefinition.class);

      def.children().forEach(this::travel);
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
          FormatField foundField = null;
          for (var field : format.fields) {
            if (field.identifier().name.equals(namedArgument.name.name)) {
              foundField = field;
              break;
            }
          }
          if (foundField == null) {
            var suggestions = Levenshtein.suggestions(namedArgument.name.name,
                format.fields.stream().map(f -> f.identifier().name).toList());

            statement.symbolTable()
                .reportUnkownError("Field", namedArgument.name.name, namedArgument.location(),
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
              if (param.identifier().name.equals(namedArgument.name.name)) {
                foundParam = param;
                break;
              }
            }
            if (foundParam == null) {
              var suggestions =
                  Levenshtein.suggestions(namedArgument.name.name,
                      pseudoInstr.params.stream().map(p -> p.identifier().name).toList());
              statement.symbolTable()
                  .reportUnkownError("Instruction Parameter", namedArgument.name.name,
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

      definition.children().forEach(this::travel);
      afterTravel(definition);
      return null;
    }

    @Override
    public Void visit(AsmModifierDefinition definition) {
      beforeTravel(definition);

      var relocation = definition.relocation;
      var symbol = definition.symbolTable().resolve(relocation);
      if (symbol == null) {
        var suggestions = Levenshtein.suggestions(
            relocation.name,
            definition.symbolTable().allSymbolNames());

        definition.symbolTable()
            .reportUnkownError("Relocation", relocation.pathToString(), relocation, suggestions);
      }

      definition.children().forEach(this::travel);
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
      definition.children().stream()
          .filter(c -> c != definition.attribute)
          .forEach(this::travel);

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
        var idSymbol = definition.symbolTable().resolve(definition.id);
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
      definition.children().stream()
          .filter(c -> c != definition.id)
          .forEach(this::travel);


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
