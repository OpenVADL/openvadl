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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;
import vadl.error.Diagnostic;
import vadl.types.BuiltInTable;
import vadl.types.asmTypes.AsmType;
import vadl.utils.SourceLocation;

class SymbolTable {
  @Nullable
  SymbolTable parent = null;
  final List<SymbolTable> children = new ArrayList<>();
  final Map<String, Symbol> symbols = new HashMap<>();
  final Map<String, AstSymbol> macroSymbols = new HashMap<>();
  List<Diagnostic> errors = new ArrayList<>();

  interface Symbol {
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
    // Load all "real" buildins
    BuiltInTable.builtIns().map(BuiltInTable.BuiltIn::name)
        .forEach(name -> symbols.put(name, new BuiltInSymbol()));

    // Add pseudo buildins
    symbols.put("VADL::mod", new BuiltInSymbol());
    symbols.put("VADL::div", new BuiltInSymbol());
    symbols.put("start", new BuiltInSymbol());
    symbols.put("executable", new BuiltInSymbol());
    symbols.put("halt", new BuiltInSymbol());
    symbols.put("firmware", new BuiltInSymbol());
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
  Symbol resolveSymbol(String name) {
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
  Symbol resolveBuiltinSymbol(String name) {
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
  Symbol resolveSymbolPath(List<String> path) {
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
  Node resolveNode(String name) {
    var symbol = resolveSymbol(name);
    if (!(symbol instanceof AstSymbol astSymbol)) {
      return null;
    }

    return astSymbol.origin;
  }

  @Nullable
  Node resolveNodePath(List<String> path) {
    var symbol = resolveSymbolPath(path);
    if (!(symbol instanceof AstSymbol astSymbol)) {
      return null;
    }

    return astSymbol.origin;
  }

  @Nullable
  Node resolveMacroSymbol(String name) {
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

  <T extends Node> @Nullable T findAs(IdentifierPath usage, Class<T> type) {
    var origin = resolveNodePath(usage.pathToSegments());
    if (type.isInstance(origin)) {
      return type.cast(origin);
    }
    return null;
  }

  <T extends Node> @Nullable T findAs(Identifier usage, Class<T> type) {
    return findAs(usage.name, type);
  }

  <T extends Node> @Nullable T findAs(String name, Class<T> type) {
    var origin = resolveNode(name);
    if (type.isInstance(origin)) {
      return type.cast(origin);
    }
    return null;
  }

  // FIXME: I don't like how it's called require but still returns null
  <T extends Node> @Nullable T requireAs(Identifier usage, Class<T> type) {
    var origin = resolveNode(usage.name);
    if (type.isInstance(origin)) {
      return type.cast(origin);
    }

    if (origin == null) {
      errors.add(error("Unknown name " + usage.name, usage).build());
    } else {
      // FIXME: write about how this is the wrong type.
      errors.add(error("Unknown name " + usage.name, usage).build());
    }
    return null;
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

  SyntaxType requireSyntaxType(Identifier recordName) {
    var symbol = resolveMacroSymbol(recordName.name);
    if (symbol instanceof RecordTypeDefinition recordType) {
      return recordType.recordType;
    } else if (symbol instanceof ModelTypeDefinition modelType) {
      return modelType.projectionType;
    }
    reportError("Unresolved record " + recordName.name, recordName.location());
    return BasicSyntaxType.INVALID;
  }


  void extendBy(SymbolTable other) {
    symbols.putAll(other.symbols);
    macroSymbols.putAll(other.macroSymbols);
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

    var originLoc = getIdentifierLocation(origin);

    var error = error("Symbol name already used: " + name, originLoc)
        .locationDescription(originLoc, "Second definition here.")
        .note("All symbols must have a unique name.");


    var otherSymbol = symbols.get(name);
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

    var originLocation = getIdentifierLocation(origin);
    var error = error("Macro name already used: " + name, originLocation)
        .locationDescription(originLocation, "Second definition here.")
        .note("All macros must have a unique name.");

    var other = macroSymbols.get(name).origin();
    var otherLoc = getIdentifierLocation(other);
    error.locationDescription(otherLoc, "First defined here.");

    errors.add(error.build());
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
   * with a further pass {@link ResolutionPass} actually gathering the fields declared
   * in the linked "format" definition.
   * Before: Ast is fully Macro-expanded
   * After: Ast is fully Macro-expanded and all relevant nodes have "symbolTable" set.
   *
   * @see ResolutionPass
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
      currentSymbols().defineSymbol(definition.stringLiteral.toString(), definition);
      definition.symbolTable = currentSymbols();
      return null;
    }

    @Override
    public Void visit(AsmGrammarAlternativesDefinition definition) {
      beforeTravel(definition);

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
    public Void visit(AsmGrammarElementDefinition definition) {
      // Avoid creating a new scope since the elements should share the same scope
      definition.symbolTable = currentSymbols();
      definition.children().forEach(this::travel);
      return null;
    }

    @Override
    public Void visit(AsmModifierDefinition definition) {
      // This isn't a identifyableNode so we need to add custom handling here.
      currentSymbols().defineSymbol(definition.stringLiteral.toString(), definition);
      definition.symbolTable = currentSymbols();
      return null;
    }

    @Override
    public Void visit(EnumerationDefinition definition) {
      beforeTravel(definition);

      // Insert all fields into the symbol table.
      if (definition.enumType != null) {
        definition.enumType.accept(this);
      }
      for (EnumerationDefinition.Entry entry : definition.entries) {
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
  static class ResolutionPass {
    static List<Diagnostic> resolveSymbols(Ast ast) {
      var resolveStartTime = System.nanoTime();
      for (Definition definition : ast.definitions) {
        resolveSymbols(definition);
      }
      ast.passTimings.add(new Ast.PassTimings("Symbol resolution",
          (System.nanoTime() - resolveStartTime) / 1000_000));
      return Objects.requireNonNull(ast.rootSymbolTable).errors;
    }

    static void resolveSymbols(Definition definition) {
      if (definition instanceof InstructionSetDefinition isa) {
        if (isa.extending != null) {
          var extending =
              isa.symbolTable().requireAs(isa.extending, InstructionSetDefinition.class);
          isa.extendingNode = extending;
          if (extending != null) {
            isa.symbolTable().extendBy(extending.symbolTable());
          }
        }
        for (Definition childDef : isa.definitions) {
          resolveSymbols(childDef);
        }
      } else if (definition instanceof ConstantDefinition constant) {
        if (constant.typeLiteral != null) {
          resolveSymbols(constant.typeLiteral);
        }
        resolveSymbols(constant.value);
      } else if (definition instanceof CounterDefinition counterDefinition) {
        resolveSymbols(counterDefinition.typeLiteral);
      } else if (definition instanceof RegisterDefinition registerDefinition) {
        resolveSymbols(registerDefinition.typeLiteral);
      } else if (definition instanceof RegisterFileDefinition registerFile) {
        for (var argType : registerFile.typeLiteral.argTypes()) {
          resolveSymbols(argType);
        }
        resolveSymbols(registerFile.typeLiteral.resultType());
      } else if (definition instanceof MemoryDefinition memory) {
        resolveSymbols(memory.addressTypeLiteral);
        resolveSymbols(memory.dataTypeLiteral);
      } else if (definition instanceof UsingDefinition using) {
        resolveSymbols(using.typeLiteral);
      } else if (definition instanceof FunctionDefinition function) {
        for (Parameter param : function.params) {
          resolveSymbols(param.typeLiteral);
        }
        resolveSymbols(function.retType);
        resolveSymbols(function.expr);
      } else if (definition instanceof FormatDefinition format) {
        resolveSymbols(format.typeLiteral);
        for (FormatField field : format.fields) {
          if (field instanceof RangeFormatField rangeField) {
            if (rangeField.typeLiteral != null) {
              resolveSymbols(rangeField.typeLiteral);
            }
          } else if (field instanceof TypedFormatField typedField) {
            resolveSymbols(typedField.typeLiteral);
          } else if (field instanceof DerivedFormatField dfField) {
            resolveSymbols(dfField.expr);
          } else {
            throw new RuntimeException("Unknown class");
          }
        }
      } else if (definition instanceof InstructionDefinition instr) {
        var format = instr.symbolTable().requireAs(instr.typeIdentifier(), FormatDefinition.class);
        if (format != null) {
          instr.symbolTable().extendBy(format.symbolTable());
          instr.formatNode = format;
        }
        resolveSymbols(instr.behavior);
      } else if (definition instanceof InstructionSequenceDefinition
          instructionSequenceDefinition) {
        for (InstructionCallStatement statement : instructionSequenceDefinition.statements) {
          resolveSymbols(statement);
        }
      } else if (definition instanceof RelocationDefinition relocation) {
        resolveSymbols(relocation.expr);
      } else if (definition instanceof AssemblyDefinition assembly) {
        for (IdentifierOrPlaceholder identifier : assembly.identifiers) {
          var pseudoInstr = assembly.symbolTable()
              .findAs((Identifier) identifier, PseudoInstructionDefinition.class);
          if (pseudoInstr != null) {
            assembly.instructionNodes.add(pseudoInstr);
            assembly.symbolTable().extendBy(pseudoInstr.symbolTable());
            if (pseudoInstr.assemblyDefinition != null) {
              assembly.symbolTable().reportAlreadyDefined(
                  "Assembly for %s pseudo instruction is already defined".formatted(
                      identifier),
                  identifier.location(), pseudoInstr.assemblyDefinition.location());
            }
            pseudoInstr.assemblyDefinition = assembly;
          } else {
            var instr =
                assembly.symbolTable().findAs((Identifier) identifier, InstructionDefinition.class);
            if (instr != null) {
              assembly.instructionNodes.add(instr);

              if (instr.assemblyDefinition != null) {
                assembly.symbolTable().reportAlreadyDefined(
                    "Assembly for %s instruction is already defined".formatted(
                        identifier),
                    identifier.location(), instr.assemblyDefinition.location());
              }
              instr.assemblyDefinition = assembly;
            }
            var format = assembly.symbolTable().requireInstructionFormat((Identifier) identifier);
            if (format != null) {
              assembly.symbolTable().extendBy(format.symbolTable());
            }
          }
        }
        resolveSymbols(assembly.expr);
      } else if (definition instanceof EncodingDefinition encoding) {
        var inst =
            encoding.symbolTable().requireAs(encoding.identifier(), InstructionDefinition.class);
        if (inst != null) {
          if (inst.encodingDefinition != null) {
            encoding.symbolTable().reportAlreadyDefined(
                "Encoding for %s instruction is already defined".formatted(encoding.identifier()),
                encoding.location(), inst.encodingDefinition.location());
          } else {
            inst.encodingDefinition = encoding;
          }
        }

        var format = encoding.symbolTable().requireInstructionFormat(encoding.identifier());
        if (format != null) {
          encoding.formatNode = format;
          for (var item : encoding.encodings.items) {
            var fieldEncoding = (EncodingDefinition.EncodingField) item;
            var field = fieldEncoding.field;
            if (findField(format, field.name) == null) {
              encoding.symbolTable()
                  .reportError("Format field %s not found".formatted(field.name), field.location());
            }
          }
        }
      } else if (definition instanceof AliasDefinition alias) {
        resolveSymbols(alias.value);
      } else if (definition instanceof EnumerationDefinition enumeration) {
        for (EnumerationDefinition.Entry entry : enumeration.entries) {
          if (entry.value != null) {
            resolveSymbols(entry.value);
          }
        }
      } else if (definition instanceof ExceptionDefinition exception) {
        resolveSymbols(exception.statement);
      } else if (definition instanceof ProcessDefinition process) {
        resolveSymbols(process.statement);
      } else if (definition instanceof ApplicationBinaryInterfaceDefinition abi) {
        var isa =
            abi.symbolTable().requireAs((Identifier) abi.isa, InstructionSetDefinition.class);
        if (isa != null) {
          abi.isaNode = isa;
          abi.symbolTable().extendBy(isa.symbolTable());
          for (Definition def : abi.definitions) {
            resolveSymbols(def);
          }
        }
      } else if (definition instanceof MicroProcessorDefinition mip) {
        for (IsId implementedIsa : mip.implementedIsas) {
          InstructionSetDefinition isa = mip.symbolTable()
              .requireAs((Identifier) implementedIsa, InstructionSetDefinition.class);
          if (isa != null) {
            mip.implementedIsaNodes.add(isa);
          }
        }
        if (mip.abi != null) {
          var abi = mip.symbolTable()
              .requireAs((Identifier) mip.abi, ApplicationBinaryInterfaceDefinition.class);
          if (abi != null) {
            mip.abiNode = abi;
            mip.symbolTable().extendBy(abi.symbolTable());
            for (Definition def : mip.definitions) {
              resolveSymbols(def);
            }
          }
        }
      } else if (definition instanceof CpuFunctionDefinition cpuFunction) {
        resolveSymbols(cpuFunction.expr);
      } else if (definition instanceof CpuProcessDefinition cpuProcess) {
        resolveSymbols(cpuProcess.statement);
      } else if (definition instanceof MicroArchitectureDefinition mia) {
        for (Definition def : mia.definitions) {
          resolveSymbols(def);
        }
      } else if (definition instanceof MacroInstructionDefinition macroInstruction) {
        resolveSymbols(macroInstruction.statement);
      } else if (definition instanceof PortBehaviorDefinition portBehavior) {
        resolveSymbols(portBehavior.statement);
      } else if (definition instanceof PipelineDefinition pipeline) {
        resolveSymbols(pipeline.statement);
      } else if (definition instanceof StageDefinition stage) {
        resolveSymbols(stage.statement);
      } else if (definition instanceof AsmDescriptionDefinition asmDescription) {
        var abi = asmDescription.symbolTable()
            .requireAs(asmDescription.abi, ApplicationBinaryInterfaceDefinition.class);
        if (abi != null) {
          asmDescription.symbolTable().extendBy(abi.symbolTable());
        }
        asmDescription.modifiers.forEach(ResolutionPass::resolveSymbols);
        asmDescription.directives.forEach(ResolutionPass::resolveSymbols);
        asmDescription.rules.forEach(ResolutionPass::resolveSymbols);
      } else if (definition instanceof AsmModifierDefinition modifier) {
        var relocation = modifier.relocation;
        var symbol = modifier.symbolTable().resolveSymbol(relocation.pathToString());
        if (symbol == null) {
          modifier.symbolTable()
              .reportError("Unknown relocation symbol: " + relocation.pathToString(),
                  relocation.location());
        }
      } else if (definition instanceof AsmDirectiveDefinition directive) {
        if (!AsmDirective.isAsmDirective(directive.builtinDirective.name)) {
          directive.symbolTable()
              .reportError("Unknown asm directive: " + directive.builtinDirective.name,
                  directive.builtinDirective.location());
        }
      } else if (definition instanceof AsmGrammarRuleDefinition rule) {
        resolveSymbols(rule.alternatives);
        if (rule.asmTypeDefinition != null) {
          resolveSymbols(rule.asmTypeDefinition);
        }
      } else if (definition instanceof AsmGrammarAlternativesDefinition alternativesDefinition) {
        alternativesDefinition.alternatives.forEach(
            alternative -> alternative.forEach(ResolutionPass::resolveSymbols));
      } else if (definition instanceof AsmGrammarElementDefinition element) {
        if (element.localVar != null) {
          resolveSymbols(element.localVar);
        }
        if (element.semanticPredicate != null) {
          resolveSymbols(element.semanticPredicate);
        }
        if (element.groupAlternatives != null) {
          resolveSymbols(element.groupAlternatives);
        }
        if (element.optionAlternatives != null) {
          resolveSymbols(element.optionAlternatives);
        }
        if (element.repetitionAlternatives != null) {
          resolveSymbols(element.repetitionAlternatives);
        }
        if (element.asmLiteral != null) {
          resolveSymbols(element.asmLiteral);
        }
        if (element.groupAsmTypeDefinition != null) {
          resolveSymbols(element.groupAsmTypeDefinition);
        }
        if (element.attribute != null) {
          // if attrSymbol is not null, attribute refers to local variable
          // else attribute is handled by matching in the AsmParser
          var attrSymbol = element.symbolTable().resolveNode(element.attribute.name);
          element.isAttributeLocalVar = attrSymbol instanceof AsmGrammarLocalVarDefinition;
        }
      } else if (definition instanceof AsmGrammarLocalVarDefinition localVar) {
        if (localVar.asmLiteral.id != null && !localVar.asmLiteral.id.name.equals("null")) {
          resolveSymbols(localVar.asmLiteral);
        }
      } else if (definition instanceof AsmGrammarLiteralDefinition asmLiteral) {
        if (asmLiteral.id != null) {
          var idSymbol = asmLiteral.symbolTable().resolveSymbol(asmLiteral.id.name);
          if (idSymbol == null) {
            asmLiteral.symbolTable()
                .reportError("Unknown symbol in asm grammar rule: " + asmLiteral.id.name,
                    asmLiteral.id.location());
          }
        }
        if (asmLiteral.asmTypeDefinition != null) {
          resolveSymbols(asmLiteral.asmTypeDefinition);
        }
        asmLiteral.parameters.forEach(ResolutionPass::resolveSymbols);
      } else if (definition instanceof AsmGrammarTypeDefinition asmTypeDefinition) {
        if (!AsmType.isInputAsmType(asmTypeDefinition.id.name)) {
          asmTypeDefinition.symbolTable()
              .reportError("Unknown asm type: " + asmTypeDefinition.id.name,
                  asmTypeDefinition.id.location());
        }
      }
    }

    static void resolveSymbols(Statement stmt) {
      if (stmt instanceof BlockStatement block) {
        for (Statement inner : block.statements) {
          resolveSymbols(inner);
        }
      } else if (stmt instanceof LetStatement let) {
        resolveSymbols(let.valueExpr);
        resolveSymbols(let.body);
      } else if (stmt instanceof IfStatement ifStmt) {
        resolveSymbols(ifStmt.condition);
        resolveSymbols(ifStmt.thenStmt);
        if (ifStmt.elseStmt != null) {
          resolveSymbols(ifStmt.elseStmt);
        }
      } else if (stmt instanceof AssignmentStatement assignment) {
        resolveSymbols(assignment.target);
        resolveSymbols(assignment.valueExpression);
      } else if (stmt instanceof RaiseStatement raise) {
        resolveSymbols(raise.statement);
      } else if (stmt instanceof CallStatement call) {
        resolveSymbols(call.expr);
      } else if (stmt instanceof MatchStatement match) {
        resolveSymbols(match.candidate);
        if (match.defaultResult != null) {
          resolveSymbols(match.defaultResult);
        }
        for (MatchStatement.Case matchCase : match.cases) {
          resolveSymbols(matchCase.result);
          for (Expr pattern : matchCase.patterns) {
            resolveSymbols(pattern);
          }
        }
      } else if (stmt instanceof InstructionCallStatement instructionCall) {
        var instr =
            instructionCall.symbolTable().findAs(instructionCall.id(), InstructionDefinition.class);
        var format = instructionCall.symbolTable().findInstructionFormat(instructionCall.id());
        if (format != null) {
          instructionCall.instrDef = instr;
          for (var namedArgument : instructionCall.namedArguments) {
            FormatField foundField = null;
            for (var field : format.fields) {
              if (field.identifier().name.equals(namedArgument.name.name)) {
                foundField = field;
                break;
              }
            }
            if (foundField == null) {
              instructionCall.symbolTable()
                  .reportError("Unknown format field " + namedArgument.name.name,
                      namedArgument.name.location());
            }
            resolveSymbols(namedArgument.value);
          }
        } else {
          var pseudoInstr =
              instructionCall.symbolTable()
                  .findAs(instructionCall.id(), PseudoInstructionDefinition.class);
          if (pseudoInstr != null) {
            instructionCall.instrDef = pseudoInstr;
            for (var namedArgument : instructionCall.namedArguments) {
              Parameter foundParam = null;
              for (var param : pseudoInstr.params) {
                if (param.identifier().name.equals(namedArgument.name.name)) {
                  foundParam = param;
                  break;
                }
              }
              if (foundParam == null) {
                instructionCall.symbolTable()
                    .reportError(
                        "Unknown instruction param %s (%s)".formatted(namedArgument.name.name,
                            pseudoInstr.identifier().name),
                        namedArgument.name.location());
              }
              resolveSymbols(namedArgument.value);
            }
          } else {
            instructionCall.symbolTable()
                .reportError("Unknown instruction " + instructionCall.id().name,
                    instructionCall.loc);
          }
        }
        for (Expr unnamedArgument : instructionCall.unnamedArguments) {
          resolveSymbols(unnamedArgument);
        }
      } else if (stmt instanceof LockStatement lock) {
        resolveSymbols(lock.expr);
        resolveSymbols(lock.statement);
      } else if (stmt instanceof ForallStatement forall) {
        for (ForallStatement.Index index : forall.indices) {
          resolveSymbols(index.domain);
        }
        resolveSymbols(forall.body);
      }
    }

    static void resolveSymbols(Expr expr) {
      if (expr instanceof LetExpr letExpr) {
        resolveSymbols(letExpr.valueExpr);
        resolveSymbols(letExpr.body);
      } else if (expr instanceof IfExpr ifExpr) {
        resolveSymbols(ifExpr.condition);
        resolveSymbols(ifExpr.thenExpr);
        resolveSymbols(ifExpr.elseExpr);
      } else if (expr instanceof GroupedExpr group) {
        for (Expr inner : group.expressions) {
          resolveSymbols(inner);
        }
      } else if (expr instanceof UnaryExpr unary) {
        resolveSymbols(unary.operand);
      } else if (expr instanceof BinaryExpr binary) {
        resolveSymbols(binary.left);
        resolveSymbols(binary.right);
      } else if (expr instanceof CastExpr cast) {
        resolveSymbols(cast.value);
      } else if (expr instanceof CallIndexExpr call) {
        resolveSymbols((Expr) call.target);
        for (var argsIndex : call.argsIndices) {
          for (Expr index : argsIndex.values) {
            resolveSymbols(index);
          }
        }
        for (CallIndexExpr.SubCall subCall : call.subCalls) {
          for (var argsIndex : subCall.argsIndices) {
            for (Expr index : argsIndex.values) {
              resolveSymbols(index);
            }
          }
        }
      } else if (expr instanceof SymbolExpr sym) {
        resolveSymbols((Expr) sym.path());
        resolveSymbols(sym.size);
      } else if (expr instanceof IdentifierPath path) {
        var symbol = expr.symbolTable().resolveSymbolPath(path.pathToSegments());
        if (symbol == null) {
          expr.symbolTable()
              .reportError("Symbol not found: " + path.pathToString(), path.location());
        }
      } else if (expr instanceof IsId id) {
        var symbol = expr.symbolTable().resolveSymbol(id.pathToString());
        if (symbol == null) {
          expr.symbolTable().reportError("Symbol not found: " + id.pathToString(), id.location());
        }
      } else if (expr instanceof MatchExpr match) {
        resolveSymbols(match.candidate);
        resolveSymbols(match.defaultResult);
        for (MatchExpr.Case matchCase : match.cases) {
          resolveSymbols(matchCase.result);
          for (Expr pattern : matchCase.patterns) {
            resolveSymbols(pattern);
          }
        }
      } else if (expr instanceof ExistsInThenExpr existsInThen) {
        resolveSymbols(existsInThen.thenExpr);
      } else if (expr instanceof ForallThenExpr forAllThen) {
        resolveSymbols(forAllThen.thenExpr);
      } else if (expr instanceof ForallExpr forallExpr) {
        for (ForallExpr.Index index : forallExpr.indices) {
          resolveSymbols(index.domain);
        }
        resolveSymbols(forallExpr.body);
      } else if (expr instanceof SequenceCallExpr sequenceCall) {
        resolveSymbols(sequenceCall.target);
      } else if (expr instanceof TypeLiteral typeLiteral) {
        for (var sizeExprList : typeLiteral.sizeIndices) {
          for (Expr sizeExpr : sizeExprList) {
            resolveSymbols(sizeExpr);
          }
        }
      }
    }

    @Nullable
    private static FormatField findField(FormatDefinition format, String name) {
      for (FormatField f : format.fields) {
        if (f.identifier().name.equals(name)) {
          return f;
        }
      }
      return null;
    }
  }
}
