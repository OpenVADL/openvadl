package vadl.ast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;
import vadl.error.Diagnostic;
import vadl.types.AsmType;
import vadl.utils.SourceLocation;

class SymbolTable {
  @Nullable
  SymbolTable parent = null;
  final List<SymbolTable> children = new ArrayList<>();
  final Map<String, Symbol> symbols = new HashMap<>();
  final Map<String, Symbol> macroSymbols = new HashMap<>();
  List<Diagnostic> errors = new ArrayList<>();

  void loadBuiltins() {
    for (String builtinFunction : Builtins.BUILTIN_FUNCTIONS) {
      defineSymbol(new GenericSymbol(builtinFunction, "BUILTIN"),
          SourceLocation.INVALID_SOURCE_LOCATION);
    }
  }

  void defineSymbol(Symbol symbol, SourceLocation loc) {
    if (symbol instanceof ModelTypeSymbol || symbol instanceof ModelSymbol
        || symbol instanceof RecordSymbol) {
      verifyMacroAvailable(symbol.name(), loc);
      macroSymbols.put(symbol.name(), symbol);
    } else {
      verifyAvailable(symbol.name(), loc);
      symbols.put(symbol.name(), symbol);
    }
  }

  SymbolTable createChild() {
    SymbolTable child = new SymbolTable();
    child.parent = this;
    child.errors = this.errors;
    this.children.add(child);
    return child;
  }

  void addModelDefinition(ModelDefinition modelDefinition) {
    defineSymbol(new ModelSymbol(modelDefinition.toMacro().name().name, modelDefinition,
        modelDefinition.toMacro()), modelDefinition.id.location());
  }

  @Nullable
  Macro getMacro(String name) {
    Symbol symbol = resolveMacroSymbol(name);
    if (symbol instanceof ModelSymbol modelSymbol) {
      return modelSymbol.macro();
    }
    return null;
  }

  @Nullable
  Symbol resolveSymbol(String name) {
    Symbol symbol = symbols.get(name);
    if (symbol != null) {
      return symbol;
    } else if (parent != null) {
      return parent.resolveSymbol(name);
    } else {
      return null;
    }
  }

  @Nullable
  Symbol resolveMacroSymbol(String name) {
    Symbol symbol = macroSymbols.get(name);
    if (symbol != null) {
      return symbol;
    } else if (parent != null) {
      return parent.resolveMacroSymbol(name);
    } else {
      return null;
    }
  }

  @Nullable
  FormatSymbol requireFormat(Identifier formatId) {
    var symbol = resolveSymbol(formatId.name);
    if (symbol instanceof FormatSymbol formatSymbol) {
      return formatSymbol;
    } else {
      reportError("Unresolved format " + formatId.name, formatId.location());
      return null;
    }
  }

  @Nullable
  PseudoInstructionDefinition findPseudoInstruction(Identifier pseudoInstrId) {
    var symbol = resolveSymbol(pseudoInstrId.name);
    if (symbol instanceof PseudoInstructionSymbol pseudoInstructionSymbol) {
      return pseudoInstructionSymbol.origin;
    } else {
      return null;
    }
  }

  @Nullable
  InstructionDefinition findInstruction(Identifier instrId) {
    var symbol = resolveSymbol(instrId.name);
    if (symbol instanceof InstructionSymbol instructionSymbol) {
      return instructionSymbol.origin;
    } else {
      return null;
    }
  }

  @Nullable
  FormatSymbol requireInstructionFormat(Identifier instrId) {
    var symbol = findInstructionFormat(instrId);
    if (symbol == null) {
      reportError("Unresolved instruction " + instrId.name, instrId.location());
      return null;
    }
    return symbol;
  }

  @Nullable
  FormatSymbol findInstructionFormat(Identifier instrId) {
    var instruction = findInstruction(instrId);
    if (instruction != null && instruction.typeIdentifier instanceof Identifier typeId) {
      return requireFormat(typeId);
    } else {
      return null;
    }
  }

  @Nullable
  InstructionSetDefinition requireIsa(Identifier isa) {
    var symbol = resolveSymbol(isa.name);
    if (symbol instanceof IsaSymbol isaSymbol) {
      return isaSymbol.origin;
    }
    reportError("Unresolved ISA " + isa.name, isa.location());
    return null;
  }

  @Nullable
  ApplicationBinaryInterfaceDefinition requireAbi(Identifier abi) {
    var symbol = resolveSymbol(abi.name);
    if (symbol instanceof AbiSymbol abiSymbol) {
      return abiSymbol.origin;
    }
    reportError("Unresolved ABI " + abi.name, abi.location());
    return null;
  }

  void addRecord(RecordTypeDefinition definition) {
    defineSymbol(new RecordSymbol(definition.name.name, definition), definition.name.location());
  }

  SyntaxType findType(Identifier recordName) {
    var symbol = resolveMacroSymbol(recordName.name);
    if (symbol instanceof RecordSymbol recordSymbol) {
      return recordSymbol.origin.recordType;
    } else if (symbol instanceof ModelTypeSymbol modelTypeSymbol) {
      return modelTypeSymbol.origin.projectionType;
    }
    reportError("Unresolved record " + recordName.name, recordName.location());
    return BasicSyntaxType.INVALID;
  }

  void addModelType(ModelTypeDefinition definition) {
    defineSymbol(new ModelTypeSymbol(definition.name.name, definition), definition.name.location());
  }

  void extendBy(SymbolTable other) {
    symbols.putAll(other.symbols);
    macroSymbols.putAll(other.macroSymbols);
  }

  void importFrom(Ast moduleAst, List<List<Identifier>> importedSymbols) {
    for (List<Identifier> importedSymbolSegments : importedSymbols) {
      var importedSymbol = new StringBuilder();
      for (Identifier segment : importedSymbolSegments) {
        if (!importedSymbol.isEmpty()) {
          importedSymbol.append("::");
        }
        importedSymbol.append(segment.name);
      }
      var symbol = moduleAst.rootSymbolTable().resolveSymbol(importedSymbol.toString());
      var macroSymbol = moduleAst.rootSymbolTable().resolveMacroSymbol(importedSymbol.toString());
      var location = importedSymbolSegments.get(0).location()
          .join(importedSymbolSegments.get(importedSymbolSegments.size() - 1).location());
      if (symbol == null && macroSymbol == null) {
        reportError("Unresolved symbol " + importedSymbol, location);
      } else {
        if (symbol != null) {
          defineSymbol(symbol, location);
        }
        if (macroSymbol != null) {
          defineSymbol(macroSymbol, location);
        }
      }
    }
  }

  private SourceLocation getIdentifierLocation(Node node) {
    if (node instanceof AliasDefinition) {
      return ((AliasDefinition) node).id().location();
    } else if (node instanceof ApplicationBinaryInterfaceDefinition) {
      return ((ApplicationBinaryInterfaceDefinition) node).id.location();
    } else if (node instanceof AsmDescriptionDefinition) {
      return ((AsmDescriptionDefinition) node).id.location();
    } else if (node instanceof AsmGrammarRuleDefinition) {
      return ((AsmGrammarRuleDefinition) node).id.location();
    } else if (node instanceof AsmGrammarLocalVarDefinition) {
      return ((AsmGrammarLocalVarDefinition) node).id.location();
    } else if (node instanceof AssemblyDefinition) {
      var identifiers = ((AssemblyDefinition) node).identifiers;
      return identifiers.get(0).location().join(identifiers.get(identifiers.size() - 1).location());
    } else if (node instanceof CacheDefinition) {
      return ((CacheDefinition) node).id.location();
    } else if (node instanceof ConstantDefinition) {
      return ((ConstantDefinition) node).identifier.location();
    } else if (node instanceof CounterDefinition) {
      return ((CounterDefinition) node).identifier.location();
    } else if (node instanceof EncodingDefinition) {
      return ((EncodingDefinition) node).instrId().location();
    } else if (node instanceof EnumerationDefinition) {
      return ((EnumerationDefinition) node).id().location();
    } else if (node instanceof ExceptionDefinition) {
      return ((ExceptionDefinition) node).id().location();
    } else if (node instanceof FormatDefinition) {
      return ((FormatDefinition) node).identifier().location();
    } else if (node instanceof FunctionDefinition) {
      return ((FunctionDefinition) node).name().location();
    } else if (node instanceof InstructionDefinition) {
      return ((InstructionDefinition) node).id().location();
    } else if (node instanceof InstructionSetDefinition) {
      return ((InstructionSetDefinition) node).identifier.location();
    } else if (node instanceof LogicDefinition) {
      return ((LogicDefinition) node).id.location();
    } else if (node instanceof MemoryDefinition) {
      return ((MemoryDefinition) node).identifier().location();
    } else if (node instanceof MicroArchitectureDefinition) {
      return ((MicroArchitectureDefinition) node).id.location();
    } else if (node instanceof MicroProcessorDefinition) {
      return ((MicroProcessorDefinition) node).id.location();
    } else if (node instanceof ModelDefinition) {
      return ((ModelDefinition) node).id.location();
    } else if (node instanceof ModelTypeDefinition) {
      return ((ModelTypeDefinition) node).name.location();
    } else if (node instanceof OperationDefinition) {
      return ((OperationDefinition) node).name().location();
      // FIXME: Must be a Node to work.
      // } else if (node instanceof ParameterDefinition) {
      //  return ((ParameterDefinition) node).name().location();
    } else if (node instanceof PipelineDefinition) {
      return ((PipelineDefinition) node).id.location();
    } else if (node instanceof PortBehaviorDefinition) {
      return ((PortBehaviorDefinition) node).id.location();
    } else if (node instanceof ProcessDefinition) {
      return ((ProcessDefinition) node).name().location();
    } else if (node instanceof PseudoInstructionDefinition) {
      return ((PseudoInstructionDefinition) node).id().location();
    } else if (node instanceof RecordTypeDefinition) {
      return ((RecordTypeDefinition) node).name.location();
    } else if (node instanceof RegisterDefinition) {
      return ((RegisterDefinition) node).identifier().location();
    } else if (node instanceof RegisterFileDefinition) {
      return ((RegisterFileDefinition) node).identifier().location();
    } else if (node instanceof RelocationDefinition) {
      return ((RelocationDefinition) node).identifier.location();
    } else if (node instanceof SignalDefinition) {
      return ((SignalDefinition) node).id.location();
    } else if (node instanceof SourceDefinition) {
      return ((SourceDefinition) node).id.location();
    } else if (node instanceof StageDefinition) {
      return ((StageDefinition) node).id.location();
    } else if (node instanceof UsingDefinition) {
      return ((UsingDefinition) node).identifier().location();
    } else {
      return node.location();
    }
  }

  private void verifyAvailable(String name, SourceLocation loc) {
    if (!symbols.containsKey(name)) {
      return;
    }

    var error = Diagnostic.error("Symbol name already used: " + name, loc)
        .locationDescription(loc, "Second definition here.")
        .note("All symbols must have a unique name.");

    var other = symbols.get(name).origin();
    if (other instanceof Node) {
      var otherLoc = getIdentifierLocation((Node) other);
      error.locationDescription(otherLoc, "First defined here.");
    }

    errors.add(error.build());
  }

  private void verifyMacroAvailable(String name, SourceLocation loc) {
    if (!macroSymbols.containsKey(name)) {
      return;
    }

    var error = Diagnostic.error("Macro name already used: " + name, loc)
        .locationDescription(loc, "Second definition here.")
        .note("All macros must have a unique name.");

    var other = macroSymbols.get(name).origin();
    if (other instanceof Node) {
      var otherLoc = getIdentifierLocation((Node) other);
      error.locationDescription(otherLoc, "First defined here.");
    }

    errors.add(error.build());
  }

  private void reportError(String error, SourceLocation location) {
    errors.add(Diagnostic.error(error, location)
        .build());
  }

  interface Symbol {
    String name();

    Object origin();
  }

  record IsaSymbol(String name, InstructionSetDefinition origin)
      implements Symbol {
  }

  record AbiSymbol(String name, ApplicationBinaryInterfaceDefinition origin)
      implements Symbol {
  }

  record MipSymbol(String name, MicroProcessorDefinition origin)
      implements Symbol {
  }

  record MiaSymbol(String name, MicroArchitectureDefinition origin)
      implements Symbol {
  }

  // TODO origin should always be a node
  record GenericSymbol(String name, Object origin) implements Symbol {
  }

  record ModelSymbol(String name, ModelDefinition origin, Macro macro) implements Symbol {
  }

  record FormatSymbol(String name, FormatDefinition origin) implements Symbol {
  }

  record AliasSymbol(String name, UsingDefinition origin) implements Symbol {
    TypeLiteral aliasType() {
      return origin.type;
    }
  }

  record InstructionSymbol(String name, InstructionDefinition origin) implements Symbol {
  }

  record PseudoInstructionSymbol(String name, PseudoInstructionDefinition origin)
      implements Symbol {
  }

  record RecordSymbol(String name, RecordTypeDefinition origin) implements Symbol {
  }

  record ModelTypeSymbol(String name, ModelTypeDefinition origin) implements Symbol {
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
    static void collectSymbols(SymbolTable symbols, Definition definition) {
      definition.symbolTable = symbols;
      if (definition instanceof InstructionSetDefinition isa) {
        symbols.defineSymbol(new IsaSymbol(isa.identifier.name, isa), isa.identifier.location());
        isa.symbolTable = symbols.createChild();
        for (Definition childDef : isa.definitions) {
          collectSymbols(isa.symbolTable, childDef);
        }
      } else if (definition instanceof ConstantDefinition constant) {
        symbols.defineSymbol(new GenericSymbol(constant.identifier().name, constant),
            constant.identifier().location());
        collectSymbols(symbols, constant.value);
      } else if (definition instanceof CounterDefinition counter) {
        symbols.defineSymbol(new GenericSymbol(counter.identifier().name, counter),
            counter.identifier().location());
      } else if (definition instanceof RegisterDefinition register) {
        symbols.defineSymbol(new GenericSymbol(register.identifier().name, register),
            register.identifier().location());
      } else if (definition instanceof RegisterFileDefinition registerFile) {
        symbols.defineSymbol(new GenericSymbol(registerFile.identifier().name, registerFile),
            registerFile.identifier().location());
      } else if (definition instanceof MemoryDefinition memory) {
        symbols.defineSymbol(new GenericSymbol(memory.identifier().name, memory),
            memory.identifier().location());
      } else if (definition instanceof UsingDefinition using) {
        symbols.defineSymbol(new AliasSymbol(using.identifier().name, using),
            using.identifier().loc);
      } else if (definition instanceof FunctionDefinition function) {
        symbols.defineSymbol(new GenericSymbol(function.name().name, function),
            function.name.location());
        function.symbolTable = symbols.createChild();
        for (Parameter param : function.params) {
          function.symbolTable.defineSymbol(new GenericSymbol(param.name().name, param),
              param.name().location());
        }
        collectSymbols(function.symbolTable, function.expr);
      } else if (definition instanceof FormatDefinition format) {
        format.symbolTable = symbols.createChild();
        symbols.defineSymbol(new FormatSymbol(format.identifier().name, format),
            format.identifier.location());
        for (FormatDefinition.FormatField field : format.fields) {
          format.symbolTable().defineSymbol(new GenericSymbol(field.identifier().name, field),
              field.identifier().location());
        }
      } else if (definition instanceof InstructionDefinition instr) {
        symbols.defineSymbol(new InstructionSymbol(instr.id().name, instr), instr.id().location());
        instr.symbolTable = symbols.createChild();
        collectSymbols(instr.symbolTable, instr.behavior);
      } else if (definition instanceof PseudoInstructionDefinition pseudo) {
        symbols.defineSymbol(new PseudoInstructionSymbol(pseudo.id().name, pseudo),
            pseudo.id().location());
        pseudo.symbolTable = symbols.createChild();
        for (var param : pseudo.params) {
          pseudo.symbolTable.defineSymbol(new GenericSymbol(param.name().name, param),
              param.name().loc);
        }
        for (InstructionCallStatement statement : pseudo.statements) {
          collectSymbols(pseudo.symbolTable, statement);
        }
      } else if (definition instanceof RelocationDefinition relocation) {
        symbols.defineSymbol(new GenericSymbol(relocation.identifier.name, relocation),
            relocation.identifier.loc);
        relocation.symbolTable = symbols.createChild();
        for (Parameter param : relocation.params) {
          relocation.symbolTable.defineSymbol(new GenericSymbol(param.name().name, param),
              param.name().loc);
        }
        collectSymbols(relocation.symbolTable, relocation.expr);
      } else if (definition instanceof AssemblyDefinition assembly) {
        assembly.symbolTable = symbols.createChild();
        collectSymbols(assembly.symbolTable, assembly.expr);
      } else if (definition instanceof EncodingDefinition encoding) {
        encoding.symbolTable = symbols.createChild();
        for (var fieldEncoding : encoding.encodings.items) {
          collectSymbols(encoding.symbolTable,
              ((EncodingDefinition.EncodingField) fieldEncoding).value());
        }
      } else if (definition instanceof AliasDefinition alias) {
        symbols.defineSymbol(new GenericSymbol(alias.id().name, alias), alias.id().loc);
        collectSymbols(symbols, alias.value);
      } else if (definition instanceof EnumerationDefinition enumeration) {
        for (EnumerationDefinition.Entry entry : enumeration.entries) {
          String path = enumeration.id().name + "::" + entry.name().name;
          symbols.defineSymbol(new GenericSymbol(path, entry), entry.name().location());
          if (entry.value() != null) {
            collectSymbols(symbols, entry.value());
          }
          if (entry.behavior() != null) {
            collectSymbols(symbols, entry.behavior());
          }
        }
      } else if (definition instanceof ExceptionDefinition exception) {
        symbols.defineSymbol(new GenericSymbol(exception.id().name, exception), exception.id().loc);
        collectSymbols(symbols, exception.statement);
      } else if (definition instanceof ImportDefinition importDef) {
        symbols.importFrom(importDef.moduleAst, importDef.importedSymbols);
      } else if (definition instanceof ModelDefinition model) {
        symbols.addModelDefinition(model);
      } else if (definition instanceof RecordTypeDefinition record) {
        symbols.addRecord(record);
      } else if (definition instanceof ModelTypeDefinition modelType) {
        symbols.addModelType(modelType);
      } else if (definition instanceof ProcessDefinition process) {
        symbols.defineSymbol(new GenericSymbol(process.name().name, process), process.name().loc);
        process.symbolTable = symbols.createChild();
        for (ProcessDefinition.TemplateParam templateParam : process.templateParams) {
          process.symbolTable.defineSymbol(
              new GenericSymbol(templateParam.name().name, templateParam),
              templateParam.name().location());
        }
        for (Parameter input : process.inputs) {
          process.symbolTable.defineSymbol(new GenericSymbol(input.name().name, input),
              input.name().location());
        }
        for (Parameter output : process.outputs) {
          process.symbolTable.defineSymbol(new GenericSymbol(output.name().name, output),
              output.name().location());
        }
        collectSymbols(process.symbolTable, process.statement);
      } else if (definition instanceof ApplicationBinaryInterfaceDefinition abi) {
        symbols.defineSymbol(new AbiSymbol(abi.id.name, abi), abi.id.loc);
        abi.symbolTable = symbols.createChild();
        for (Definition def : abi.definitions) {
          collectSymbols(abi.symbolTable, def);
        }
      } else if (definition instanceof AbiSequenceDefinition abiSequence) {
        abiSequence.symbolTable = symbols.createChild();
        for (Parameter param : abiSequence.params) {
          abiSequence.symbolTable.defineSymbol(new GenericSymbol(param.name().name, param),
              param.name().loc);
        }
        for (InstructionCallStatement statement : abiSequence.statements) {
          collectSymbols(abiSequence.symbolTable, statement);
        }
      } else if (definition instanceof MicroProcessorDefinition mip) {
        symbols.defineSymbol(new MipSymbol(mip.id.name, mip), mip.id.loc);
        mip.symbolTable = symbols.createChild();
        for (Definition def : mip.definitions) {
          collectSymbols(mip.symbolTable, def);
        }
      } else if (definition instanceof SpecialPurposeRegisterDefinition specialPurposeRegister) {
        for (SequenceCallExpr call : specialPurposeRegister.calls) {
          collectSymbols(symbols, call);
        }
      } else if (definition instanceof CpuFunctionDefinition cpuFunction) {
        collectSymbols(symbols, cpuFunction.expr);
      } else if (definition instanceof CpuProcessDefinition cpuProcess) {
        cpuProcess.symbolTable = symbols.createChild();
        for (Parameter startupOutput : cpuProcess.startupOutputs) {
          cpuProcess.symbolTable.defineSymbol(
              new GenericSymbol(startupOutput.name().name, cpuProcess),
              startupOutput.name().loc
          );
        }
        collectSymbols(cpuProcess.symbolTable, cpuProcess.statement);
      } else if (definition instanceof MicroArchitectureDefinition mia) {
        symbols.defineSymbol(new MiaSymbol(mia.id.name, mia), mia.id.loc);
        mia.symbolTable = symbols.createChild();
        for (Definition def : mia.definitions) {
          collectSymbols(mia.symbolTable, def);
        }
      } else if (definition instanceof MacroInstructionDefinition macroInstruction) {
        macroInstruction.symbolTable = symbols.createChild();
        for (Parameter input : macroInstruction.inputs) {
          macroInstruction.symbolTable.defineSymbol(new GenericSymbol(input.name().name, input),
              input.name().loc);
        }
        for (Parameter output : macroInstruction.outputs) {
          macroInstruction.symbolTable.defineSymbol(new GenericSymbol(output.name().name, output),
              output.name().loc);
        }
        collectSymbols(macroInstruction.symbolTable, macroInstruction.statement);
      } else if (definition instanceof PortBehaviorDefinition portBehavior) {
        // TODO Clarify behavior - do special symbols "translation", "cachedData" exist?
        collectSymbols(symbols, portBehavior.statement);
      } else if (definition instanceof PipelineDefinition pipeline) {
        pipeline.symbolTable = symbols.createChild();
        pipeline.symbolTable.defineSymbol(new GenericSymbol("stage", pipeline),
            SourceLocation.INVALID_SOURCE_LOCATION);
        for (Parameter output : pipeline.outputs) {
          pipeline.symbolTable.defineSymbol(new GenericSymbol(output.name().name, output),
              output.name().loc);
        }
        collectSymbols(pipeline.symbolTable, pipeline.statement);
      } else if (definition instanceof StageDefinition stage) {
        stage.symbolTable = symbols.createChild();
        for (Parameter output : stage.outputs) {
          stage.symbolTable.defineSymbol(new GenericSymbol(output.name().name, output),
              output.name().loc);
        }
        collectSymbols(stage.symbolTable, stage.statement);
      } else if (definition instanceof CacheDefinition cache) {
        symbols.defineSymbol(new GenericSymbol(cache.id.name, cache), cache.id.loc);
      } else if (definition instanceof SignalDefinition signal) {
        symbols.defineSymbol(new GenericSymbol(signal.id.name, signal), signal.id.loc);
      } else if (definition instanceof AsmDescriptionDefinition asmDescription) {
        symbols.defineSymbol(new GenericSymbol(asmDescription.id.name, asmDescription),
            asmDescription.id.location());
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
        symbols.defineSymbol(new GenericSymbol(modifier.stringLiteral.toString(), modifier),
            modifier.location());
      } else if (definition instanceof AsmDirectiveDefinition directive) {
        symbols.defineSymbol(new GenericSymbol(directive.stringLiteral.toString(), directive),
            directive.location());
      } else if (definition instanceof AsmGrammarRuleDefinition rule) {
        symbols.defineSymbol(new GenericSymbol(rule.id.name, rule), rule.id.location());
        collectSymbols(symbols, rule.alternatives);
        if (rule.asmType != null) {
          collectSymbols(symbols, rule.asmType);
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
        if (element.groupAlternatives != null) {
          collectSymbols(symbols, element.groupAlternatives);
        }
        if (element.optionAlternatives != null) {
          collectSymbols(symbols, element.optionAlternatives);
        }
        if (element.repetitionAlternatives != null) {
          collectSymbols(symbols, element.repetitionAlternatives);
        }
        if (element.groupAsmType != null) {
          collectSymbols(symbols, element.groupAsmType);
        }
      } else if (definition instanceof AsmGrammarLocalVarDefinition localVar) {
        symbols.defineSymbol(new GenericSymbol(localVar.id.name, localVar), localVar.id.location());
        if (localVar.asmLiteral != null) {
          collectSymbols(symbols, localVar.asmLiteral);
        }
      } else if (definition instanceof AsmGrammarLiteralDefinition asmLiteral) {
        if (!asmLiteral.parameters.isEmpty()) {
          asmLiteral.parameters.forEach(param -> collectSymbols(symbols, param));
        }
        if (asmLiteral.asmType != null) {
          collectSymbols(symbols, asmLiteral.asmType);
        }
      }
    }

    static void collectSymbols(SymbolTable symbols, Statement stmt) {
      if (stmt.symbolTable != null) {
        throw new IllegalStateException("Tried to populate already set symbol table " + stmt);
      }
      stmt.symbolTable = symbols;
      if (stmt instanceof BlockStatement block) {
        for (Statement inner : block.statements) {
          collectSymbols(symbols, inner);
        }
      } else if (stmt instanceof LetStatement let) {
        collectSymbols(symbols, let.valueExpression);
        var child = symbols.createChild();
        for (var identifier : let.identifiers) {
          child.defineSymbol(new GenericSymbol(identifier.name, identifier), identifier.location());
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
        collectSymbols(symbols, raise.statement);
      } else if (stmt instanceof CallStatement call) {
        collectSymbols(symbols, call.expr);
      } else if (stmt instanceof MatchStatement match) {
        collectSymbols(symbols, match.candidate);
        if (match.defaultResult != null) {
          collectSymbols(symbols, match.defaultResult);
        }
        for (MatchStatement.Case matchCase : match.cases) {
          collectSymbols(symbols, matchCase.result());
          for (Expr pattern : matchCase.patterns()) {
            collectSymbols(symbols, pattern);
          }
        }
      } else if (stmt instanceof InstructionCallStatement instructionCall) {
        for (var namedArgument : instructionCall.namedArguments) {
          collectSymbols(symbols, namedArgument.value());
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
          forall.symbolTable.defineSymbol(new GenericSymbol(index.name().name, index),
              index.name().loc);
          collectSymbols(symbols, index.domain());
        }
        collectSymbols(forall.symbolTable, forall.statement);
      }
    }

    static void collectSymbols(SymbolTable symbols, Expr expr) {
      expr.symbolTable = symbols;
      if (expr instanceof LetExpr letExpr) {
        letExpr.symbolTable = symbols.createChild();
        for (var identifier : letExpr.identifiers) {
          letExpr.symbolTable.defineSymbol(new GenericSymbol(identifier.name, identifier),
              identifier.location());
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
      } else if (expr instanceof CallExpr call) {
        collectSymbols(symbols, (Expr) call.target);
        for (List<Expr> argsIndex : call.argsIndices) {
          for (Expr index : argsIndex) {
            collectSymbols(symbols, index);
          }
        }
        for (CallExpr.SubCall subCall : call.subCalls) {
          for (List<Expr> argsIndex : subCall.argsIndices()) {
            for (Expr index : argsIndex) {
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
          collectSymbols(symbols, matchCase.result());
          for (Expr pattern : matchCase.patterns()) {
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
          forallThen.symbolTable().defineSymbol(new GenericSymbol(index.id().pathToString(), index),
              index.id().location());
          for (IsId operation : index.operations()) {
            ((Node) operation).symbolTable = symbols;
          }
        }
        collectSymbols(forallThen.symbolTable(), forallThen.thenExpr);
      } else if (expr instanceof ForallExpr forallExpr) {
        forallExpr.symbolTable = symbols.createChild();
        for (ForallExpr.Index index : forallExpr.indices) {
          forallExpr.symbolTable().defineSymbol(new GenericSymbol(index.id().pathToString(), index),
              index.id().location());
          collectSymbols(symbols, index.domain());
        }
        collectSymbols(forallExpr.symbolTable(), forallExpr.expr);
      } else if (expr instanceof SequenceCallExpr sequenceCall) {
        collectSymbols(symbols, sequenceCall.target);
        if (sequenceCall.range != null) {
          collectSymbols(symbols, sequenceCall.range);
        }
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
      for (Definition definition : ast.definitions) {
        resolveSymbols(definition);
      }
      ast.passTimings.add(new VadlParser.PassTimings(System.nanoTime(), "Symbol resolution"));
      return Objects.requireNonNull(ast.rootSymbolTable).errors;
    }

    static void resolveSymbols(Definition definition) {
      if (definition instanceof InstructionSetDefinition isa) {
        if (isa.extending != null) {
          var extending = isa.symbolTable().requireIsa(isa.extending);
          isa.extendingNode = extending;
          if (extending != null) {
            isa.symbolTable().extendBy(extending.symbolTable());
          }
        }
        for (Definition childDef : isa.definitions) {
          resolveSymbols(childDef);
        }
      } else if (definition instanceof ConstantDefinition constant) {
        resolveSymbols(constant.value);
      } else if (definition instanceof FunctionDefinition function) {
        resolveSymbols(function.expr);
      } else if (definition instanceof InstructionDefinition instr) {
        var format = instr.symbolTable().requireFormat(instr.type());
        if (format != null) {
          instr.symbolTable().extendBy(format.origin.symbolTable());
          instr.formatNode = format.origin;
        }
        resolveSymbols(instr.behavior);
      } else if (definition instanceof PseudoInstructionDefinition pseudo) {
        for (InstructionCallStatement statement : pseudo.statements) {
          resolveSymbols(statement);
        }
      } else if (definition instanceof RelocationDefinition relocation) {
        resolveSymbols(relocation.expr);
      } else if (definition instanceof AssemblyDefinition assembly) {
        for (IdentifierOrPlaceholder identifier : assembly.identifiers) {
          var pseudoInstr = assembly.symbolTable().findPseudoInstruction((Identifier) identifier);
          if (pseudoInstr != null) {
            assembly.instructionNodes.add(pseudoInstr);
            assembly.symbolTable().extendBy(pseudoInstr.symbolTable());
          } else {
            var instr = assembly.symbolTable().findInstruction((Identifier) identifier);
            if (instr != null) {
              assembly.instructionNodes.add(instr);
            }
            var format = assembly.symbolTable().requireInstructionFormat((Identifier) identifier);
            if (format != null) {
              assembly.symbolTable().extendBy(format.origin.symbolTable());
            }
          }
        }
        resolveSymbols(assembly.expr);
      } else if (definition instanceof EncodingDefinition encoding) {
        var format = encoding.symbolTable().requireInstructionFormat(encoding.instrId());
        if (format != null) {
          encoding.formatNode = format.origin;
          for (var item : encoding.encodings.items) {
            var fieldEncoding = (EncodingDefinition.EncodingField) item;
            var field = fieldEncoding.field();
            if (findField(format.origin, field.name) == null) {
              encoding.symbolTable()
                  .reportError("Format field %s not found".formatted(field.name), field.location());
            }
          }
        }
      } else if (definition instanceof AliasDefinition alias) {
        resolveSymbols(alias.value);
      } else if (definition instanceof EnumerationDefinition enumeration) {
        for (EnumerationDefinition.Entry entry : enumeration.entries) {
          if (entry.value() != null) {
            resolveSymbols(entry.value());
          }
          if (entry.behavior() != null) {
            resolveSymbols(entry.behavior());
          }
        }
      } else if (definition instanceof ExceptionDefinition exception) {
        resolveSymbols(exception.statement);
      } else if (definition instanceof ProcessDefinition process) {
        resolveSymbols(process.statement);
      } else if (definition instanceof ApplicationBinaryInterfaceDefinition abi) {
        var isa = abi.symbolTable().requireIsa((Identifier) abi.isa);
        if (isa != null) {
          abi.isaNode = isa;
          abi.symbolTable().extendBy(isa.symbolTable());
          for (Definition def : abi.definitions) {
            resolveSymbols(def);
          }
        }
      } else if (definition instanceof AbiSequenceDefinition abiSequence) {
        for (InstructionCallStatement statement : abiSequence.statements) {
          resolveSymbols(statement);
        }
      } else if (definition instanceof MicroProcessorDefinition mip) {
        for (IsId implementedIsa : mip.implementedIsas) {
          InstructionSetDefinition isa = mip.symbolTable().requireIsa((Identifier) implementedIsa);
          if (isa != null) {
            mip.implementedIsaNodes.add(isa);
          }
        }
        var abi = mip.symbolTable().requireAbi((Identifier) mip.abi);
        if (abi != null) {
          mip.abiNode = abi;
          mip.symbolTable().extendBy(abi.symbolTable());
          for (Definition def : mip.definitions) {
            resolveSymbols(def);
          }
        }
      } else if (definition instanceof SpecialPurposeRegisterDefinition specialPurposeRegister) {
        for (SequenceCallExpr call : specialPurposeRegister.calls) {
          resolveSymbols(call);
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
        var abi = asmDescription.symbolTable().requireAbi(asmDescription.abi);
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
        if (rule.asmType != null) {
          resolveSymbols(rule.asmType);
        }
      } else if (definition instanceof AsmGrammarAlternativesDefinition alternativesDefinition) {
        alternativesDefinition.alternatives.forEach(
            alternative -> alternative.forEach(ResolutionPass::resolveSymbols));
      } else if (definition instanceof AsmGrammarElementDefinition element) {
        if (element.localVar != null) {
          resolveSymbols(element.localVar);
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
        if (element.groupAsmType != null) {
          resolveSymbols(element.groupAsmType);
        }
        if (element.attribute != null) {
          // if attrSymbol is not null, attribute refers to local variable
          // else attribute is handled by matching in the AsmParser
          var attrSymbol = element.symbolTable().resolveSymbol(element.attribute.name);
          element.isAttributeLocalVar = attrSymbol != null;
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
        if (asmLiteral.asmType != null) {
          resolveSymbols(asmLiteral.asmType);
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
        resolveSymbols(let.valueExpression);
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
          resolveSymbols(matchCase.result());
          for (Expr pattern : matchCase.patterns()) {
            resolveSymbols(pattern);
          }
        }
      } else if (stmt instanceof InstructionCallStatement instructionCall) {
        var instr = instructionCall.symbolTable().findInstruction(instructionCall.id());
        var format = instructionCall.symbolTable().findInstructionFormat(instructionCall.id());
        if (format != null) {
          instructionCall.instrNode = instr;
          for (var namedArgument : instructionCall.namedArguments) {
            FormatDefinition.FormatField foundField = null;
            for (var field : format.origin.fields) {
              if (field.identifier().name.equals(namedArgument.name().name)) {
                foundField = field;
                break;
              }
            }
            if (foundField == null) {
              instructionCall.symbolTable()
                  .reportError("Unknown format field " + namedArgument.name().name,
                      namedArgument.name().location());
            }
            resolveSymbols(namedArgument.value());
          }
        } else {
          var pseudoInstr =
              instructionCall.symbolTable().findPseudoInstruction(instructionCall.id());
          if (pseudoInstr != null) {
            instructionCall.instrNode = pseudoInstr;
            for (var namedArgument : instructionCall.namedArguments) {
              Parameter foundParam = null;
              for (var param : pseudoInstr.params) {
                if (param.name().name.equals(namedArgument.name().name)) {
                  foundParam = param;
                  break;
                }
              }
              if (foundParam == null) {
                instructionCall.symbolTable()
                    .reportError(
                        "Unknown instruction param %s (%s)".formatted(namedArgument.name().name,
                            pseudoInstr.id().name),
                        namedArgument.name().location());
              }
              resolveSymbols(namedArgument.value());
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
          resolveSymbols(index.domain());
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
      } else if (expr instanceof CallExpr call) {
        resolveSymbols((Expr) call.target);
        for (List<Expr> argsIndex : call.argsIndices) {
          for (Expr index : argsIndex) {
            resolveSymbols(index);
          }
        }
        for (CallExpr.SubCall subCall : call.subCalls) {
          for (List<Expr> argsIndex : subCall.argsIndices()) {
            for (Expr index : argsIndex) {
              resolveSymbols(index);
            }
          }
        }
      } else if (expr instanceof SymbolExpr sym) {
        resolveSymbols((Expr) sym.path());
        resolveSymbols(sym.size);
      } else if (expr instanceof IsId id) {
        var symbol = expr.symbolTable().resolveSymbol(id.pathToString());
        if (symbol == null) {
          expr.symbolTable().reportError("Symbol not found: " + id.pathToString(), id.location());
        } else if (id instanceof Identifier identifier) {
          identifier.refNode = symbol.origin();
        } else if (id instanceof IdentifierPath identifierPath) {
          identifierPath.refNode = symbol.origin();
        }
      } else if (expr instanceof MatchExpr match) {
        resolveSymbols(match.candidate);
        resolveSymbols(match.defaultResult);
        for (MatchExpr.Case matchCase : match.cases) {
          resolveSymbols(matchCase.result());
          for (Expr pattern : matchCase.patterns()) {
            resolveSymbols(pattern);
          }
        }
      } else if (expr instanceof ExistsInThenExpr existsInThen) {
        resolveSymbols(existsInThen.thenExpr);
      } else if (expr instanceof ForallThenExpr forAllThen) {
        resolveSymbols(forAllThen.thenExpr);
      } else if (expr instanceof ForallExpr forallExpr) {
        for (ForallExpr.Index index : forallExpr.indices) {
          resolveSymbols(index.domain());
        }
        resolveSymbols(forallExpr.expr);
      } else if (expr instanceof SequenceCallExpr sequenceCall) {
        resolveSymbols(sequenceCall.target);
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
