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
      errors.add(Diagnostic.error("Unknown name " + usage.name, usage).build());
    } else {
      // FIXME: write about how this is the wrong type.
      errors.add(Diagnostic.error("Unknown name " + usage.name, usage).build());
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

    var error = Diagnostic.error("Symbol name already used: " + name, originLoc)
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
    var error = Diagnostic.error("Macro name already used: " + name, originLocation)
        .locationDescription(originLocation, "Second definition here.")
        .note("All macros must have a unique name.");

    var other = macroSymbols.get(name).origin();
    var otherLoc = getIdentifierLocation(other);
    error.locationDescription(otherLoc, "First defined here.");

    errors.add(error.build());
  }

  private void reportError(String error, SourceLocation location) {
    errors.add(Diagnostic.error(error, location)
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
  static class SymbolCollector {
    Deque<String> viamPath;

    public SymbolCollector(String fileName) {
      this.viamPath = new ArrayDeque<>();
    }

    void collectSymbols(SymbolTable symbols, Definition definition) {
      if (definition instanceof IdentifiableNode idNode) {
        viamPath.offerLast(idNode.identifier().name);
      } else {
        viamPath.offerLast("unknown");
      }
      definition.viamId = String.join("::", viamPath);

      definition.symbolTable = symbols;
      if (definition instanceof InstructionSetDefinition isa) {
        symbols.defineSymbol(isa);
        isa.symbolTable = symbols.createChild();
        for (Definition childDef : isa.definitions) {
          collectSymbols(isa.symbolTable, childDef);
        }
      } else if (definition instanceof ConstantDefinition constant) {
        symbols.defineSymbol(constant);
        if (constant.typeLiteral != null) {
          collectSymbols(symbols, constant.typeLiteral);
        }
        collectSymbols(symbols, constant.value);
      } else if (definition instanceof CounterDefinition counter) {
        symbols.defineSymbol(counter);
        collectSymbols(symbols, counter.typeLiteral);
      } else if (definition instanceof RegisterDefinition register) {
        symbols.defineSymbol(register);
        collectSymbols(symbols, register.typeLiteral);
      } else if (definition instanceof RegisterFileDefinition registerFile) {
        symbols.defineSymbol(registerFile);
        for (var argType : registerFile.typeLiteral.argTypes()) {
          collectSymbols(symbols, argType);
        }
        collectSymbols(symbols, registerFile.typeLiteral.resultType());
      } else if (definition instanceof MemoryDefinition memory) {
        symbols.defineSymbol(memory);
        collectSymbols(symbols, memory.addressTypeLiteral);
        collectSymbols(symbols, memory.dataTypeLiteral);
      } else if (definition instanceof UsingDefinition using) {
        symbols.defineSymbol(using);
        collectSymbols(symbols, using.typeLiteral);
      } else if (definition instanceof FunctionDefinition function) {
        symbols.defineSymbol(function);
        collectSymbols(symbols, function.retType);
        function.symbolTable = symbols.createChild();
        for (Parameter param : function.params) {
          function.symbolTable.defineSymbol(param);
          collectSymbols(symbols, param.typeLiteral);
        }
        collectSymbols(function.symbolTable, function.expr);
      } else if (definition instanceof FormatDefinition format) {
        format.symbolTable = symbols.createChild();
        symbols.defineSymbol(format);
        collectSymbols(symbols, format.typeLiteral);
        for (FormatDefinition.FormatField field : format.fields) {
          format.symbolTable().defineSymbol(field.identifier().name, (Node) field);

          if (field instanceof FormatDefinition.RangeFormatField rangeField) {
            if (rangeField.typeLiteral != null) {
              collectSymbols(symbols, rangeField.typeLiteral);
            }
          } else if (field instanceof FormatDefinition.TypedFormatField typedField) {
            collectSymbols(symbols, typedField.typeLiteral);
          } else if (field instanceof FormatDefinition.DerivedFormatField dfField) {
            collectSymbols(format.symbolTable, dfField.expr);
          } else {
            throw new RuntimeException("Unknown class");
          }
          // FIXME: Add symboltables to all the fields and their children.
        }
      } else if (definition instanceof InstructionDefinition instr) {
        symbols.defineSymbol(instr);
        instr.symbolTable = symbols.createChild();
        collectSymbols(instr.symbolTable, instr.behavior);
      } else if (definition instanceof InstructionSequenceDefinition
          instructionSequenceDefinition) {
        if (instructionSequenceDefinition instanceof PseudoInstructionDefinition pseudo) {
          symbols.defineSymbol(pseudo);
        }
        instructionSequenceDefinition.symbolTable = symbols.createChild();
        for (var param : instructionSequenceDefinition.params) {
          instructionSequenceDefinition.symbolTable.defineSymbol(param);
          collectSymbols(symbols, param.typeLiteral);
        }
        for (InstructionCallStatement statement : instructionSequenceDefinition.statements) {
          collectSymbols(instructionSequenceDefinition.symbolTable, statement);
        }
      } else if (definition instanceof RelocationDefinition relocation) {
        symbols.defineSymbol(relocation);
        relocation.symbolTable = symbols.createChild();
        collectSymbols(symbols, relocation.resultTypeLiteral);
        for (Parameter param : relocation.params) {
          relocation.symbolTable.defineSymbol(param);
          collectSymbols(symbols, param.typeLiteral);
        }
        collectSymbols(relocation.symbolTable, relocation.expr);
      } else if (definition instanceof AssemblyDefinition assembly) {
        assembly.symbolTable = symbols.createChild();
        collectSymbols(assembly.symbolTable, assembly.expr);
      } else if (definition instanceof EncodingDefinition encoding) {
        encoding.symbolTable = symbols.createChild();
        for (var fieldEncoding : encoding.encodings.items) {
          collectSymbols(encoding.symbolTable,
              ((EncodingDefinition.EncodingField) fieldEncoding).value);
        }
      } else if (definition instanceof AliasDefinition alias) {
        symbols.defineSymbol(alias);
        collectSymbols(symbols, alias.value);
        if (alias.aliasType != null) {
          collectSymbols(symbols, alias.aliasType);
        }
        if (alias.targetType != null) {
          collectSymbols(symbols, alias.targetType);
        }
      } else if (definition instanceof EnumerationDefinition enumeration) {
        enumeration.symbolTable = symbols.createChild();
        symbols.defineSymbol(enumeration);
        if (enumeration.enumType != null) {
          collectSymbols(symbols, enumeration.enumType);
        }
        for (EnumerationDefinition.Entry entry : enumeration.entries) {
          enumeration.symbolTable().defineSymbol(entry.name.name, entry);
          if (entry.value != null) {
            collectSymbols(symbols, entry.value);
          }
        }
      } else if (definition instanceof ExceptionDefinition exception) {
        symbols.defineSymbol(exception);
        collectSymbols(symbols, exception.statement);
      } else if (definition instanceof ImportDefinition importDef) {
        symbols.importFrom(importDef.moduleAst, importDef.importedSymbols);
      } else if (definition instanceof ModelDefinition model) {
        symbols.defineSymbol(model);
      } else if (definition instanceof RecordTypeDefinition record) {
        symbols.defineSymbol(record);
      } else if (definition instanceof ModelTypeDefinition modelType) {
        symbols.defineSymbol(modelType);
      } else if (definition instanceof ProcessDefinition process) {
        symbols.defineSymbol(process);
        process.symbolTable = symbols.createChild();
        for (TemplateParam templateParam : process.templateParams) {
          process.symbolTable.defineSymbol(templateParam);
          collectSymbols(symbols, templateParam.type);
        }
        for (Parameter input : process.inputs) {
          process.symbolTable.defineSymbol(input);
          collectSymbols(symbols, input.typeLiteral);
        }
        for (Parameter output : process.outputs) {
          process.symbolTable.defineSymbol(output);
          collectSymbols(symbols, output.typeLiteral);
        }
        collectSymbols(process.symbolTable, process.statement);
      } else if (definition instanceof ApplicationBinaryInterfaceDefinition abi) {
        symbols.defineSymbol(abi);
        abi.symbolTable = symbols.createChild();
        for (Definition def : abi.definitions) {
          collectSymbols(abi.symbolTable, def);
        }
      } else if (definition instanceof AbiSequenceDefinition abiSequence) {
        abiSequence.symbolTable = symbols.createChild();
        for (Parameter param : abiSequence.params) {
          abiSequence.symbolTable.defineSymbol(param);
        }
        for (InstructionCallStatement statement : abiSequence.statements) {
          collectSymbols(abiSequence.symbolTable, statement);
        }
      } else if (definition instanceof MicroProcessorDefinition mip) {
        symbols.defineSymbol(mip);
        mip.symbolTable = symbols.createChild();
        for (Definition def : mip.definitions) {
          collectSymbols(mip.symbolTable, def);
        }
      } else if (definition instanceof SpecialPurposeRegisterDefinition specialPurposeRegister) {
        specialPurposeRegister.symbolTable = symbols.createChild();
      } else if (definition instanceof AbiPseudoInstructionDefinition
          abiPseudoInstructionDefinition) {
        abiPseudoInstructionDefinition.symbolTable = symbols.createChild();
      } else if (definition instanceof CpuFunctionDefinition cpuFunction) {
        collectSymbols(symbols, cpuFunction.expr);
      } else if (definition instanceof CpuProcessDefinition cpuProcess) {
        cpuProcess.symbolTable = symbols.createChild();
        for (Parameter startupOutput : cpuProcess.startupOutputs) {
          cpuProcess.symbolTable.defineSymbol(startupOutput);
        }
        collectSymbols(cpuProcess.symbolTable, cpuProcess.statement);
      } else if (definition instanceof MicroArchitectureDefinition mia) {
        symbols.defineSymbol(mia);
        mia.symbolTable = symbols.createChild();
        for (Definition def : mia.definitions) {
          collectSymbols(mia.symbolTable, def);
        }
      } else if (definition instanceof MacroInstructionDefinition macroInstruction) {
        macroInstruction.symbolTable = symbols.createChild();
        for (Parameter input : macroInstruction.inputs) {
          macroInstruction.symbolTable.defineSymbol(input);
        }
        for (Parameter output : macroInstruction.outputs) {
          macroInstruction.symbolTable.defineSymbol(output);
        }
        collectSymbols(macroInstruction.symbolTable, macroInstruction.statement);
      } else if (definition instanceof PortBehaviorDefinition portBehavior) {
        // TODO Clarify behavior - do special symbols "translation", "cachedData" exist?
        collectSymbols(symbols, portBehavior.statement);
      } else if (definition instanceof PipelineDefinition pipeline) {
        pipeline.symbolTable = symbols.createChild();
        pipeline.symbolTable.defineSymbol("stage", pipeline);
        for (Parameter output : pipeline.outputs) {
          pipeline.symbolTable.defineSymbol(output);
        }
        collectSymbols(pipeline.symbolTable, pipeline.statement);
      } else if (definition instanceof StageDefinition stage) {
        stage.symbolTable = symbols.createChild();
        for (Parameter output : stage.outputs) {
          stage.symbolTable.defineSymbol(output);
        }
        collectSymbols(stage.symbolTable, stage.statement);
      } else if (definition instanceof CacheDefinition cache) {
        symbols.defineSymbol(cache);
        collectSymbols(symbols, cache.sourceType);
        collectSymbols(symbols, cache.targetType);
      } else if (definition instanceof SignalDefinition signal) {
        symbols.defineSymbol(signal);
        collectSymbols(symbols, signal.type);
      } else if (definition instanceof AsmDescriptionDefinition asmDescription) {
        symbols.defineSymbol(asmDescription);
        var asmDescSymbolTable = symbols.createChild();
        asmDescription.symbolTable = asmDescSymbolTable;

        var modifierSymbols = asmDescSymbolTable.createChild();
        asmDescription.modifiers.forEach(
            modifier -> collectSymbols(modifierSymbols, modifier));

        var directiveSymbols = asmDescSymbolTable.createChild();
        asmDescription.directives.forEach(
            directive -> collectSymbols(directiveSymbols, directive));

        // add integer negation function to common definitions if not already defined
        // this function is used in the grammar default rules
        if (asmDescription.commonDefinitions.stream().noneMatch(
            def -> def instanceof FunctionDefinition functionDef
                && functionDef.name.path().pathToString()
                .equals(AsmGrammarDefaultRules.BUILTIN_ASM_NEG))) {
          asmDescription.commonDefinitions.add(AsmGrammarDefaultRules.asmNegFunctionDefinition());
        }
        asmDescription.commonDefinitions.forEach(
            commonDef -> collectSymbols(asmDescSymbolTable, commonDef));
        asmDescription.rules.forEach(rule -> collectSymbols(asmDescSymbolTable, rule));

        // get default rules that are not yet defined,
        // collect their symbols and add them to assembly description
        var defaultRules = AsmGrammarDefaultRules.notIncludedDefaultRules(asmDescription.rules);
        defaultRules.forEach(rule -> collectSymbols(asmDescSymbolTable, rule));
        asmDescription.rules.addAll(defaultRules);
      } else if (definition instanceof AsmModifierDefinition modifier) {
        symbols.defineSymbol(modifier.stringLiteral.toString(), modifier);
      } else if (definition instanceof AsmDirectiveDefinition directive) {
        symbols.defineSymbol(directive.stringLiteral.toString(), directive);
      } else if (definition instanceof AsmGrammarRuleDefinition rule) {
        symbols.defineSymbol(rule);
        collectSymbols(symbols, rule.alternatives);
        if (rule.asmTypeDefinition != null) {
          collectSymbols(symbols, rule.asmTypeDefinition);
        }
      } else if (definition instanceof AsmGrammarAlternativesDefinition alternativesDef) {
        alternativesDef.alternatives.forEach(alternative -> {
          // each sequence of elements has its own scope
          var elementsSymbolTable = symbols.createChild();
          alternative.forEach(element -> collectSymbols(elementsSymbolTable, element));
        });
      } else if (definition instanceof AsmGrammarElementDefinition element) {
        if (element.localVar != null) {
          collectSymbols(symbols, element.localVar);
        }
        if (element.asmLiteral != null) {
          collectSymbols(symbols, element.asmLiteral);
        }
        if (element.semanticPredicate != null) {
          collectSymbols(symbols, element.semanticPredicate);
        }
        if (element.groupAlternatives != null) {
          collectSymbols(symbols, element.groupAlternatives);
        }
        if (element.optionAlternatives != null) {
          collectSymbols(symbols, element.optionAlternatives);
        }
        if (element.repetitionAlternatives != null) {
          collectSymbols(symbols, element.repetitionAlternatives);
        }
        if (element.groupAsmTypeDefinition != null) {
          collectSymbols(symbols, element.groupAsmTypeDefinition);
        }
      } else if (definition instanceof AsmGrammarLocalVarDefinition localVar) {
        symbols.defineSymbol(localVar);
        if (localVar.asmLiteral != null) {
          collectSymbols(symbols, localVar.asmLiteral);
        }
      } else if (definition instanceof AsmGrammarLiteralDefinition asmLiteral) {
        if (!asmLiteral.parameters.isEmpty()) {
          asmLiteral.parameters.forEach(param -> collectSymbols(symbols, param));
        }
        if (asmLiteral.asmTypeDefinition != null) {
          collectSymbols(symbols, asmLiteral.asmTypeDefinition);
        }
      }

      viamPath.pollLast();
    }

    void collectSymbols(SymbolTable symbols, Statement stmt) {
      if (stmt.symbolTable != null) {
        throw new IllegalStateException("Tried to populate already set symbol table " + stmt);
      }
      stmt.symbolTable = symbols;
      if (stmt instanceof BlockStatement block) {
        for (Statement inner : block.statements) {
          collectSymbols(symbols, inner);
        }
      } else if (stmt instanceof LetStatement let) {
        collectSymbols(symbols, let.valueExpr);
        var child = symbols.createChild();
        for (var identifier : let.identifiers) {
          child.defineSymbol(identifier.name, let);
        }
        collectSymbols(child, let.body);
      } else if (stmt instanceof IfStatement ifStmt) {
        collectSymbols(symbols, ifStmt.condition);
        collectSymbols(symbols, ifStmt.thenStmt);
        if (ifStmt.elseStmt != null) {
          collectSymbols(symbols, ifStmt.elseStmt);
        }
      } else if (stmt instanceof AssignmentStatement assignment) {
        collectSymbols(symbols, assignment.target);
        collectSymbols(symbols, assignment.valueExpression);
      } else if (stmt instanceof RaiseStatement raise) {
        raise.viamId = String.join("::", viamPath);
        collectSymbols(symbols, raise.statement);
      } else if (stmt instanceof CallStatement call) {
        collectSymbols(symbols, call.expr);
      } else if (stmt instanceof MatchStatement match) {
        collectSymbols(symbols, match.candidate);
        if (match.defaultResult != null) {
          collectSymbols(symbols, match.defaultResult);
        }
        for (MatchStatement.Case matchCase : match.cases) {
          collectSymbols(symbols, matchCase.result);
          for (Expr pattern : matchCase.patterns) {
            collectSymbols(symbols, pattern);
          }
        }
      } else if (stmt instanceof InstructionCallStatement instructionCall) {
        for (var namedArgument : instructionCall.namedArguments) {
          collectSymbols(symbols, namedArgument.value);
        }
        for (var unnamedArgument : instructionCall.unnamedArguments) {
          collectSymbols(symbols, unnamedArgument);
        }
      } else if (stmt instanceof LockStatement lock) {
        collectSymbols(symbols, lock.expr);
        collectSymbols(symbols, lock.statement);
      } else if (stmt instanceof ForallStatement forall) {
        forall.symbolTable = symbols.createChild();
        for (ForallStatement.Index index : forall.indices) {
          forall.symbolTable.defineSymbol(index);
          collectSymbols(symbols, index.domain);
        }
        collectSymbols(forall.symbolTable, forall.statement);
      }
    }

    void collectSymbols(SymbolTable symbols, Expr expr) {
      expr.symbolTable = symbols;
      if (expr instanceof LetExpr letExpr) {
        letExpr.symbolTable = symbols.createChild();
        for (var identifier : letExpr.identifiers) {
          letExpr.symbolTable.defineSymbol(identifier.name, letExpr);
        }
        collectSymbols(symbols, letExpr.valueExpr);
        collectSymbols(letExpr.symbolTable, letExpr.body);
      } else if (expr instanceof IfExpr ifExpr) {
        collectSymbols(symbols, ifExpr.condition);
        collectSymbols(symbols, ifExpr.thenExpr);
        collectSymbols(symbols, ifExpr.elseExpr);
      } else if (expr instanceof GroupedExpr group) {
        for (Expr inner : group.expressions) {
          collectSymbols(symbols, inner);
        }
      } else if (expr instanceof UnaryExpr unary) {
        collectSymbols(symbols, unary.operand);
      } else if (expr instanceof BinaryExpr binary) {
        collectSymbols(symbols, binary.left);
        collectSymbols(symbols, binary.right);
      } else if (expr instanceof CastExpr cast) {
        collectSymbols(symbols, cast.value);
        collectSymbols(symbols, cast.typeLiteral);
      } else if (expr instanceof CallIndexExpr call) {
        collectSymbols(symbols, (Expr) call.target);
        for (CallIndexExpr.Arguments argsIndex : call.argsIndices) {
          for (Expr index : argsIndex.values) {
            collectSymbols(symbols, index);
          }
        }
        for (CallIndexExpr.SubCall subCall : call.subCalls) {
          for (CallIndexExpr.Arguments argsIndex : subCall.argsIndices) {
            for (Expr index : argsIndex.values) {
              collectSymbols(symbols, index);
            }
          }
        }
      } else if (expr instanceof SymbolExpr sym) {
        collectSymbols(symbols, (Expr) sym.path());
        collectSymbols(symbols, sym.size);
      } else if (expr instanceof IdentifierPath path) {
        for (IdentifierOrPlaceholder segment : path.segments) {
          collectSymbols(symbols, (Expr) segment);
        }
      } else if (expr instanceof MatchExpr match) {
        collectSymbols(symbols, match.candidate);
        collectSymbols(symbols, match.defaultResult);
        for (MatchExpr.Case matchCase : match.cases) {
          collectSymbols(symbols, matchCase.result);
          for (Expr pattern : matchCase.patterns) {
            collectSymbols(symbols, pattern);
          }
        }
      } else if (expr instanceof ExistsInExpr existsIn) {
        for (IsId operation : existsIn.operations) {
          ((Node) operation).symbolTable = symbols;
        }
      } else if (expr instanceof ExistsInThenExpr existsInThen) {
        for (ExistsInThenExpr.Condition condition : existsInThen.conditions) {
          ((Node) condition.id()).symbolTable = symbols;
          for (IsId operation : condition.operations()) {
            ((Node) operation).symbolTable = symbols;
          }
        }
        collectSymbols(symbols, existsInThen.thenExpr);
      } else if (expr instanceof ForallThenExpr forallThen) {
        forallThen.symbolTable = symbols.createChild();
        for (ForallThenExpr.Index index : forallThen.indices) {
          forallThen.symbolTable().defineSymbol(index);
          for (IsId operation : index.operations) {
            ((Node) operation).symbolTable = symbols;
          }
        }
        collectSymbols(forallThen.symbolTable(), forallThen.thenExpr);
      } else if (expr instanceof ForallExpr forallExpr) {
        forallExpr.symbolTable = symbols.createChild();
        for (ForallExpr.Index index : forallExpr.indices) {
          forallExpr.symbolTable().defineSymbol(index);
          collectSymbols(symbols, index.domain);
        }
        collectSymbols(forallExpr.symbolTable(), forallExpr.expr);
      } else if (expr instanceof SequenceCallExpr sequenceCall) {
        collectSymbols(symbols, sequenceCall.target);
        if (sequenceCall.range != null) {
          collectSymbols(symbols, sequenceCall.range);
        }
      } else if (expr instanceof TypeLiteral typeLiteral) {
        for (var sizeExprList : typeLiteral.sizeIndices) {
          for (Expr sizeExpr : sizeExprList) {
            collectSymbols(symbols, sizeExpr);
          }
        }
      } else if (expr instanceof RangeExpr rangeExpr) {
        collectSymbols(symbols, rangeExpr.from);
        collectSymbols(symbols, rangeExpr.to);
      }

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
        for (FormatDefinition.FormatField field : format.fields) {
          if (field instanceof FormatDefinition.RangeFormatField rangeField) {
            if (rangeField.typeLiteral != null) {
              resolveSymbols(rangeField.typeLiteral);
            }
          } else if (field instanceof FormatDefinition.TypedFormatField typedField) {
            resolveSymbols(typedField.typeLiteral);
          } else if (field instanceof FormatDefinition.DerivedFormatField dfField) {
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
              assembly.symbolTable().reportError(
                  "Encoding for %s pseudo instruction is already defined".formatted(
                      identifier),
                  identifier.location());
            }
            pseudoInstr.assemblyDefinition = assembly;
          } else {
            var instr =
                assembly.symbolTable().findAs((Identifier) identifier, InstructionDefinition.class);
            if (instr != null) {
              assembly.instructionNodes.add(instr);

              if (instr.assemblyDefinition != null) {
                assembly.symbolTable().reportError(
                    "Encoding for %s instruction is already defined".formatted(
                        identifier),
                    identifier.location());
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
            encoding.symbolTable().reportError(
                "Encoding for %s instruction is already defined".formatted(encoding.identifier()),
                encoding.location());
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
            FormatDefinition.FormatField foundField = null;
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
        resolveSymbols(forall.statement);
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
        resolveSymbols(forallExpr.expr);
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
    private static FormatDefinition.FormatField findField(FormatDefinition format, String name) {
      for (FormatDefinition.FormatField f : format.fields) {
        if (f.identifier().name.equals(name)) {
          return f;
        }
      }
      return null;
    }
  }
}
