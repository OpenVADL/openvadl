package vadl.ast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;
import vadl.error.Diagnostic;
import vadl.utils.SourceLocation;

class SymbolTable {
  @Nullable
  SymbolTable parent = null;
  final List<SymbolTable> children = new ArrayList<>();
  final Map<String, Symbol> symbols = new HashMap<>();
  final Map<String, Symbol> macroSymbols = new HashMap<>();
  List<Diagnostic> errors = new ArrayList<>();

  void loadBuiltins() {
    defineSymbol(new ValuedSymbol("mnemonic", null, SymbolType.CONSTANT),
        SourceLocation.INVALID_SOURCE_LOCATION);
    for (String builtinFunction : Builtins.BUILTIN_FUNCTIONS) {
      defineSymbol(new ValuedSymbol(builtinFunction, null, SymbolType.FUNCTION),
          SourceLocation.INVALID_SOURCE_LOCATION);
    }
  }

  void defineConstant(String name, SourceLocation loc) {
    defineSymbol(new ValuedSymbol(name, null, SymbolType.CONSTANT), loc);
  }

  void defineSymbol(Symbol symbol, SourceLocation loc) {
    if (symbol instanceof ModelTypeSymbol || symbol instanceof MacroSymbol ||
        symbol instanceof RecordSymbol) {
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

  void addMacro(Macro macro, SourceLocation loc) {
    defineSymbol(new MacroSymbol(macro.name().name, macro), loc);
  }

  @Nullable
  Macro getMacro(String name) {
    Symbol symbol = resolveMacroSymbol(name);
    if (symbol instanceof MacroSymbol macroSymbol) {
      return macroSymbol.macro();
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
      formatSymbol.definition.fields.forEach(field -> symbols.put(field.identifier().name,
          new ValuedSymbol(field.identifier().name, null, SymbolType.FORMAT_FIELD)));
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
      return pseudoInstructionSymbol.definition;
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
    var symbol = resolveSymbol(instrId.name);
    if (symbol instanceof InstructionSymbol instructionSymbol
        && instructionSymbol.definition.typeIdentifier instanceof Identifier typeId) {
      return requireFormat(typeId);
    } else {
      return null;
    }
  }

  @Nullable
  InstructionSetDefinition requireIsa(Identifier isa) {
    var symbol = resolveSymbol(isa.name);
    if (symbol instanceof IsaSymbol isaSymbol) {
      return isaSymbol.definition;
    }
    reportError("Unresolved ISA " + isa.name, isa.location());
    return null;
  }

  @Nullable
  ApplicationBinaryInterfaceDefinition requireAbi(Identifier abi) {
    var symbol = resolveSymbol(abi.name);
    if (symbol instanceof AbiSymbol abiSymbol) {
      return abiSymbol.definition;
    }
    reportError("Unresolved ABI " + abi.name, abi.location());
    return null;
  }

  void addRecord(Identifier name, RecordType recordType) {
    defineSymbol(new RecordSymbol(name.name, recordType), name.location());
  }

  SyntaxType findType(Identifier recordName) {
    var symbol = resolveMacroSymbol(recordName.name);
    if (symbol instanceof RecordSymbol recordSymbol) {
      return recordSymbol.recordType();
    } else if (symbol instanceof ModelTypeSymbol modelTypeSymbol) {
      return modelTypeSymbol.projectionType();
    }
    reportError("Unresolved record " + recordName.name, recordName.location());
    return BasicSyntaxType.INVALID;
  }

  void addModelType(Identifier name, ProjectionType type) {
    defineSymbol(new ModelTypeSymbol(name.name, type), name.location());
  }

  void copyFrom(SymbolTable other) {
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

  private void verifyAvailable(String name, SourceLocation loc) {
    if (symbols.containsKey(name)) {
      reportError("Duplicate definition: " + name, loc);
    }
  }

  private void verifyMacroAvailable(String name, SourceLocation loc) {
    if (macroSymbols.containsKey(name)) {
      reportError("Duplicate definition: " + name, loc);
    }
  }

  private void reportError(String error, SourceLocation location) {
    errors.add(Diagnostic.error(error, location)
        .build());
  }

  enum SymbolType {
    ALIAS, APPLICATION_BINARY_INTERFACE, CACHE, CONSTANT, COUNTER, ENUM_FIELD, EXCEPTION, FORMAT,
    FORMAT_FIELD, FUNCTION, INSTRUCTION, INSTRUCTION_SET, MACRO, MEMORY, MICRO_PROCESSOR,
    MICRO_ARCHITECTURE, MODEL_TYPE, PARAMETER, PROCESS, PSEUDO_INSTRUCTION, RECORD, REGISTER,
    REGISTER_FILE, RELOCATION, SIGNAL
  }

  interface Symbol {
    String name();

    SymbolType type();
  }

  record IsaSymbol(String name, InstructionSetDefinition definition)
      implements Symbol {
    @Override
    public SymbolType type() {
      return SymbolType.INSTRUCTION_SET;
    }
  }

  record AbiSymbol(String name, ApplicationBinaryInterfaceDefinition definition)
      implements Symbol {
    @Override
    public SymbolType type() {
      return SymbolType.APPLICATION_BINARY_INTERFACE;
    }
  }

  record MipSymbol(String name, MicroProcessorDefinition definition)
      implements Symbol {
    @Override
    public SymbolType type() {
      return SymbolType.MICRO_PROCESSOR;
    }
  }

  record MiaSymbol(String name, MicroArchitectureDefinition definition)
      implements Symbol {
    @Override
    public SymbolType type() {
      return SymbolType.MICRO_ARCHITECTURE;
    }
  }

  record ValuedSymbol(String name, @Nullable Definition typeDefinition, SymbolType type)
      implements Symbol {
  }

  record MacroSymbol(String name, Macro macro) implements Symbol {
    @Override
    public SymbolType type() {
      return SymbolType.MACRO;
    }
  }

  record FormatSymbol(String name, FormatDefinition definition) implements Symbol {
    @Override
    public SymbolType type() {
      return SymbolType.FORMAT;
    }
  }

  record AliasSymbol(String name, TypeLiteral aliasType) implements Symbol {
    @Override
    public SymbolType type() {
      return SymbolType.ALIAS;
    }
  }

  record InstructionSymbol(String name, InstructionDefinition definition) implements Symbol {
    @Override
    public SymbolType type() {
      return SymbolType.INSTRUCTION;
    }
  }

  record PseudoInstructionSymbol(String name, PseudoInstructionDefinition definition)
      implements Symbol {
    @Override
    public SymbolType type() {
      return SymbolType.PSEUDO_INSTRUCTION;
    }
  }

  record RecordSymbol(String name, RecordType recordType) implements Symbol {
    @Override
    public SymbolType type() {
      return SymbolType.RECORD;
    }
  }

  record ModelTypeSymbol(String name, ProjectionType projectionType) implements Symbol {
    @Override
    public SymbolType type() {
      return SymbolType.MODEL_TYPE;
    }
  }

  /**
   * Distributes "SymbolTable" instances across the nodes in the AST.
   * For "let" expressions and statements, symbols for the declared variables are created here.
   * For "instruction" and "assembly" definitions, only an empty child table is created,
   * with a further pass {@link VerificationPass} actually gathering the fields declared
   * in the linked "format" definition.
   * Before: Ast is fully Macro-expanded
   * After: Ast is fully Macro-expanded and all relevant nodes have "symbolTable" set.
   *
   * @see VerificationPass
   */
  static class SymbolCollector {
    static void collectSymbols(Ast ast) {
      ast.rootSymbolTable = new SymbolTable();
      ast.rootSymbolTable.loadBuiltins();
      for (Definition definition : ast.definitions) {
        collectSymbols(ast.rootSymbolTable, definition);
      }
      ast.passTimings.add(new VadlParser.PassTimings(System.nanoTime(), "Symbol collection"));
    }

    static void collectSymbols(SymbolTable symbols, Definition definition) {
      definition.symbolTable = symbols;
      if (definition instanceof InstructionSetDefinition isa) {
        symbols.defineSymbol(new IsaSymbol(isa.identifier.name, isa), isa.identifier.location());
        isa.symbolTable = symbols.createChild();
        for (Definition childDef : isa.definitions) {
          collectSymbols(isa.symbolTable, childDef);
        }
      } else if (definition instanceof ConstantDefinition constant) {
        symbols.defineSymbol(
            new ValuedSymbol(constant.identifier().name, null, SymbolType.CONSTANT),
            constant.identifier().location());
        collectSymbols(symbols, constant.value);
      } else if (definition instanceof CounterDefinition counter) {
        symbols.defineSymbol(
            new ValuedSymbol(counter.identifier().name, null, SymbolType.COUNTER),
            counter.identifier().location());
      } else if (definition instanceof RegisterDefinition register) {
        symbols.defineSymbol(
            new ValuedSymbol(register.identifier().name, null, SymbolType.REGISTER),
            register.identifier().location());
      } else if (definition instanceof RegisterFileDefinition registerFile) {
        symbols.defineSymbol(
            new ValuedSymbol(registerFile.identifier().name, null, SymbolType.REGISTER_FILE),
            registerFile.identifier().location());
      } else if (definition instanceof MemoryDefinition memory) {
        symbols.defineSymbol(
            new ValuedSymbol(memory.identifier().name, null, SymbolType.MEMORY),
            memory.identifier().location());
      } else if (definition instanceof UsingDefinition using) {
        symbols.defineSymbol(new AliasSymbol(using.identifier().name, using.type), using.loc);
      } else if (definition instanceof FunctionDefinition function) {
        symbols.defineSymbol(new ValuedSymbol(function.name().name, null, SymbolType.FUNCTION),
            function.loc);
        function.symbolTable = symbols.createChild();
        for (Parameter param : function.params) {
          function.symbolTable.defineSymbol(
              new ValuedSymbol(param.name().name, null, SymbolType.PARAMETER),
              param.name().location());
        }
        collectSymbols(function.symbolTable, function.expr);
      } else if (definition instanceof FormatDefinition format) {
        format.symbolTable = symbols.createChild();
        symbols.defineSymbol(new FormatSymbol(format.identifier().name, format), format.location());
        for (FormatDefinition.FormatField field : format.fields) {
          format.symbolTable().defineSymbol(new ValuedSymbol(field.identifier().name, null,
              SymbolType.FORMAT_FIELD), field.identifier().location());
        }
      } else if (definition instanceof InstructionDefinition instr) {
        symbols.defineSymbol(new InstructionSymbol(instr.id().name, instr), instr.location());
        instr.symbolTable = symbols.createChild();
        collectSymbols(instr.symbolTable, instr.behavior);
      } else if (definition instanceof PseudoInstructionDefinition pseudo) {
        symbols.defineSymbol(new PseudoInstructionSymbol(pseudo.id().name, pseudo),
            pseudo.location());
        pseudo.symbolTable = symbols.createChild();
        for (var param : pseudo.params) {
          pseudo.symbolTable.defineSymbol(
              new ValuedSymbol(param.name().name, null, SymbolType.PARAMETER), param.name().loc);
        }
        for (InstructionCallStatement statement : pseudo.statements) {
          collectSymbols(pseudo.symbolTable, statement);
        }
      } else if (definition instanceof RelocationDefinition relocation) {
        symbols.defineSymbol(
            new ValuedSymbol(relocation.identifier.name, null, SymbolType.RELOCATION),
            relocation.loc);
        relocation.symbolTable = symbols.createChild();
        for (Parameter param : relocation.params) {
          relocation.symbolTable.defineSymbol(
              new ValuedSymbol(param.name().name, null, SymbolType.PARAMETER), param.name().loc);
        }
        collectSymbols(relocation.symbolTable, relocation.expr);
      } else if (definition instanceof AssemblyDefinition assembly) {
        assembly.symbolTable = symbols.createChild();
        collectSymbols(assembly.symbolTable, assembly.expr);
      } else if (definition instanceof EncodingDefinition encoding) {
        encoding.symbolTable = symbols.createChild();
        for (var fieldEncoding : encoding.fieldEncodings().encodings) {
          collectSymbols(encoding.symbolTable,
              ((EncodingDefinition.FieldEncoding) fieldEncoding).value());
        }
      } else if (definition instanceof AliasDefinition alias) {
        var type = switch (alias.kind) {
          case REGISTER -> SymbolType.REGISTER;
          case REGISTER_FILE -> SymbolType.REGISTER_FILE;
          case PROGRAM_COUNTER -> SymbolType.COUNTER;
        };
        symbols.defineSymbol(new ValuedSymbol(alias.id().name, null, type), alias.loc);
        collectSymbols(symbols, alias.value);
      } else if (definition instanceof EnumerationDefinition enumeration) {
        for (EnumerationDefinition.Entry entry : enumeration.entries) {
          String path = enumeration.id().name + "::" + entry.name().name;
          symbols.defineSymbol(new ValuedSymbol(path, null, SymbolType.ENUM_FIELD),
              entry.name().location());
          if (entry.value() != null) {
            collectSymbols(symbols, entry.value());
          }
          if (entry.behavior() != null) {
            collectSymbols(symbols, entry.behavior());
          }
        }
      } else if (definition instanceof ExceptionDefinition exception) {
        symbols.defineSymbol(new ValuedSymbol(exception.id().name, null, SymbolType.EXCEPTION),
            exception.loc);
        collectSymbols(symbols, exception.statement);
      } else if (definition instanceof ImportDefinition importDef) {
        symbols.importFrom(importDef.moduleAst, importDef.importedSymbols);
      } else if (definition instanceof ModelDefinition model) {
        symbols.addMacro(model.toMacro(), model.location());
      } else if (definition instanceof RecordTypeDefinition record) {
        symbols.addRecord(record.name, record.recordType);
      } else if (definition instanceof ModelTypeDefinition modelType) {
        symbols.addModelType(modelType.name, modelType.projectionType);
      } else if (definition instanceof ProcessDefinition process) {
        symbols.defineSymbol(new ValuedSymbol(process.name().name, null, SymbolType.PROCESS),
            process.loc);
        process.symbolTable = symbols.createChild();
        for (ProcessDefinition.TemplateParam templateParam : process.templateParams) {
          process.symbolTable.defineSymbol(
              new ValuedSymbol(templateParam.name().name, null, SymbolType.PARAMETER),
              templateParam.name().location());
        }
        for (Parameter input : process.inputs) {
          process.symbolTable.defineSymbol(
              new ValuedSymbol(input.name().name, null, SymbolType.PARAMETER),
              input.name().location());
        }
        for (Parameter output : process.outputs) {
          process.symbolTable.defineSymbol(
              new ValuedSymbol(output.name().name, null, SymbolType.PARAMETER),
              output.name().location());
        }
        collectSymbols(process.symbolTable, process.statement);
      } else if (definition instanceof ApplicationBinaryInterfaceDefinition abi) {
        symbols.defineSymbol(new AbiSymbol(abi.id.name, abi), abi.loc);
        abi.symbolTable = symbols.createChild();
        for (Definition def : abi.definitions) {
          collectSymbols(abi.symbolTable, def);
        }
      } else if (definition instanceof AbiSequenceDefinition abiSequence) {
        abiSequence.symbolTable = symbols.createChild();
        for (Parameter param : abiSequence.params) {
          abiSequence.symbolTable.defineSymbol(
              new ValuedSymbol(param.name().name, null, SymbolType.PARAMETER),
              param.name().loc
          );
        }
        for (InstructionCallStatement statement : abiSequence.statements) {
          collectSymbols(abiSequence.symbolTable, statement);
        }
      } else if (definition instanceof MicroProcessorDefinition mip) {
        symbols.defineSymbol(new MipSymbol(mip.id.name, mip), mip.loc);
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
              new ValuedSymbol(startupOutput.name().name, null, SymbolType.PARAMETER),
              startupOutput.name().loc
          );
        }
        collectSymbols(cpuProcess.symbolTable, cpuProcess.statement);
      } else if (definition instanceof MicroArchitectureDefinition mia) {
        symbols.defineSymbol(new MiaSymbol(mia.id.name, mia), mia.loc);
        mia.symbolTable = symbols.createChild();
        for (Definition def : mia.definitions) {
          collectSymbols(mia.symbolTable, def);
        }
      } else if (definition instanceof MacroInstructionDefinition macroInstruction) {
        macroInstruction.symbolTable = symbols.createChild();
        for (Parameter startupOutput : macroInstruction.inputs) {
          macroInstruction.symbolTable.defineSymbol(
              new ValuedSymbol(startupOutput.name().name, null, SymbolType.PARAMETER),
              startupOutput.name().loc
          );
        }
        for (Parameter startupOutput : macroInstruction.outputs) {
          macroInstruction.symbolTable.defineSymbol(
              new ValuedSymbol(startupOutput.name().name, null, SymbolType.PARAMETER),
              startupOutput.name().loc
          );
        }
        collectSymbols(macroInstruction.symbolTable, macroInstruction.statement);
      } else if (definition instanceof PortBehaviorDefinition portBehavior) {
        // TODO Clarify behavior - do special symbols "translation", "cachedData" exist?
        collectSymbols(symbols, portBehavior.statement);
      } else if (definition instanceof PipelineDefinition pipeline) {
        pipeline.symbolTable = symbols.createChild();
        pipeline.symbolTable.defineSymbol(new ValuedSymbol("stage", null, SymbolType.FUNCTION),
            SourceLocation.INVALID_SOURCE_LOCATION);
        for (Parameter output : pipeline.outputs) {
          pipeline.symbolTable.defineSymbol(
              new ValuedSymbol(output.name().name, null, SymbolType.PARAMETER),
              output.name().loc
          );
        }
        collectSymbols(pipeline.symbolTable, pipeline.statement);
      } else if (definition instanceof StageDefinition stage) {
        stage.symbolTable = symbols.createChild();
        for (Parameter output : stage.outputs) {
          stage.symbolTable.defineSymbol(
              new ValuedSymbol(output.name().name, null, SymbolType.PARAMETER),
              output.name().loc
          );
        }
        collectSymbols(stage.symbolTable, stage.statement);
      } else if (definition instanceof CacheDefinition cache) {
        symbols.defineSymbol(new ValuedSymbol(cache.id.name, null, SymbolType.CACHE), cache.loc);
      } else if (definition instanceof SignalDefinition signal) {
        symbols.defineSymbol(new ValuedSymbol(signal.id.name, null, SymbolType.SIGNAL),
            signal.loc);
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
          child.defineConstant(identifier.name, identifier.location());
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
          forall.symbolTable.defineSymbol(
              new ValuedSymbol(index.name().name, null, SymbolType.CONSTANT), index.name().loc);
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
          letExpr.symbolTable.defineConstant(identifier.name, identifier.location());
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
          forallThen.symbolTable().defineSymbol(
              new ValuedSymbol(index.id().pathToString(), null, SymbolType.PARAMETER),
              index.id().location());
          for (IsId operation : index.operations()) {
            ((Node) operation).symbolTable = symbols;
          }
        }
        collectSymbols(forallThen.symbolTable(), forallThen.thenExpr);
      } else if (expr instanceof ForallExpr forallExpr) {
        forallExpr.symbolTable = symbols.createChild();
        for (ForallExpr.Index index : forallExpr.indices) {
          forallExpr.symbolTable().defineSymbol(
              new ValuedSymbol(index.id().pathToString(), null, SymbolType.PARAMETER),
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
   * Verifies that identifiers used in expressions, as well as types used in definitions,
   * actually exist in the VADL file.
   * The AST is not modified in this pass, only errors are gathered.
   * Before & After: Ast is fully Macro-expanded and all relevant nodes have "symbolTable" set.
   */
  // TODO verify -> resolve, definition references
  static class VerificationPass {
    static List<Diagnostic> verifyUsages(Ast ast) {
      for (Definition definition : ast.definitions) {
        verifyUsages(definition);
      }
      ast.passTimings.add(new VadlParser.PassTimings(System.nanoTime(), "Symbol verification"));
      return Objects.requireNonNull(ast.rootSymbolTable).errors;
    }

    static void verifyUsages(Definition definition) {
      if (definition instanceof InstructionSetDefinition isa) {
        if (isa.extending != null) {
          var extending = isa.symbolTable().requireIsa(isa.extending);
          if (extending != null) {
            isa.symbolTable().copyFrom(extending.symbolTable());
          }
        }
        for (Definition childDef : isa.definitions) {
          verifyUsages(childDef);
        }
      } else if (definition instanceof ConstantDefinition constant) {
        verifyUsages(constant.value);
      } else if (definition instanceof FunctionDefinition function) {
        verifyUsages(function.expr);
      } else if (definition instanceof InstructionDefinition instr) {
        var format = instr.symbolTable().requireFormat(instr.type());
        if (format != null) {
          instr.symbolTable().copyFrom(format.definition().symbolTable());
        }
        verifyUsages(instr.behavior);
      } else if (definition instanceof PseudoInstructionDefinition pseudo) {
        for (InstructionCallStatement statement : pseudo.statements) {
          verifyUsages(statement);
        }
      } else if (definition instanceof RelocationDefinition relocation) {
        verifyUsages(relocation.expr);
      } else if (definition instanceof AssemblyDefinition assembly) {
        for (IdentifierOrPlaceholder identifier : assembly.identifiers) {
          var pseudoInstr = assembly.symbolTable().findPseudoInstruction((Identifier) identifier);
          if (pseudoInstr != null) {
            assembly.symbolTable().copyFrom(pseudoInstr.symbolTable());
          } else {
            var format = assembly.symbolTable().requireInstructionFormat((Identifier) identifier);
            if (format != null) {
              assembly.symbolTable().copyFrom(format.definition().symbolTable());
            }
          }
        }
        verifyUsages(assembly.expr);
      } else if (definition instanceof EncodingDefinition encoding) {
        var format = encoding.symbolTable().requireInstructionFormat(encoding.instrId());
        if (format != null) {
          var encodings = encoding.fieldEncodings().encodings;
          for (var enc : encodings) {
            var fieldEncoding = (EncodingDefinition.FieldEncoding) enc;
            var field = fieldEncoding.field();
            if (findField(format.definition, field.name) == null) {
              encoding.symbolTable()
                  .reportError("Format field %s not found".formatted(field.name), field.location());
            }
          }
        }
      } else if (definition instanceof AliasDefinition alias) {
        verifyUsages(alias.value);
      } else if (definition instanceof EnumerationDefinition enumeration) {
        for (EnumerationDefinition.Entry entry : enumeration.entries) {
          if (entry.value() != null) {
            verifyUsages(entry.value());
          }
          if (entry.behavior() != null) {
            verifyUsages(entry.behavior());
          }
        }
      } else if (definition instanceof ExceptionDefinition exception) {
        verifyUsages(exception.statement);
      } else if (definition instanceof ProcessDefinition process) {
        verifyUsages(process.statement);
      } else if (definition instanceof ApplicationBinaryInterfaceDefinition abi) {
        var isa = abi.symbolTable().requireIsa((Identifier) abi.isa);
        if (isa != null) {
          abi.symbolTable().copyFrom(isa.symbolTable());
          for (Definition def : abi.definitions) {
            verifyUsages(def);
          }
        }
      } else if (definition instanceof AbiSequenceDefinition abiSequence) {
        for (InstructionCallStatement statement : abiSequence.statements) {
          verifyUsages(statement);
        }
      } else if (definition instanceof MicroProcessorDefinition mip) {
        for (IsId implementedIsa : mip.implementedIsas) {
          mip.symbolTable().requireIsa((Identifier) implementedIsa);
        }
        var abi = mip.symbolTable().requireAbi((Identifier) mip.abi);
        if (abi != null) {
          mip.symbolTable().copyFrom(abi.symbolTable());
          for (Definition def : mip.definitions) {
            verifyUsages(def);
          }
        }
      } else if (definition instanceof SpecialPurposeRegisterDefinition specialPurposeRegister) {
        for (SequenceCallExpr call : specialPurposeRegister.calls) {
          verifyUsages(call);
        }
      } else if (definition instanceof CpuFunctionDefinition cpuFunction) {
        verifyUsages(cpuFunction.expr);
      } else if (definition instanceof CpuProcessDefinition cpuProcess) {
        verifyUsages(cpuProcess.statement);
      } else if (definition instanceof MicroArchitectureDefinition mia) {
        for (Definition def : mia.definitions) {
          verifyUsages(def);
        }
      } else if (definition instanceof MacroInstructionDefinition macroInstruction) {
        verifyUsages(macroInstruction.statement);
      } else if (definition instanceof PortBehaviorDefinition portBehavior) {
        verifyUsages(portBehavior.statement);
      } else if (definition instanceof PipelineDefinition pipeline) {
        verifyUsages(pipeline.statement);
      } else if (definition instanceof StageDefinition stage) {
        verifyUsages(stage.statement);
      }
    }

    static void verifyUsages(Statement stmt) {
      if (stmt instanceof BlockStatement block) {
        for (Statement inner : block.statements) {
          verifyUsages(inner);
        }
      } else if (stmt instanceof LetStatement let) {
        verifyUsages(let.valueExpression);
        verifyUsages(let.body);
      } else if (stmt instanceof IfStatement ifStmt) {
        verifyUsages(ifStmt.condition);
        verifyUsages(ifStmt.thenStmt);
        if (ifStmt.elseStmt != null) {
          verifyUsages(ifStmt.elseStmt);
        }
      } else if (stmt instanceof AssignmentStatement assignment) {
        verifyUsages(assignment.target);
        verifyUsages(assignment.valueExpression);
      } else if (stmt instanceof RaiseStatement raise) {
        verifyUsages(raise.statement);
      } else if (stmt instanceof CallStatement call) {
        verifyUsages(call.expr);
      } else if (stmt instanceof MatchStatement match) {
        verifyUsages(match.candidate);
        if (match.defaultResult != null) {
          verifyUsages(match.defaultResult);
        }
        for (MatchStatement.Case matchCase : match.cases) {
          verifyUsages(matchCase.result());
          for (Expr pattern : matchCase.patterns()) {
            verifyUsages(pattern);
          }
        }
      } else if (stmt instanceof InstructionCallStatement instructionCall) {
        var format = instructionCall.symbolTable().findInstructionFormat(instructionCall.id());
        if (format != null) {
          for (var namedArgument : instructionCall.namedArguments) {
            FormatDefinition.FormatField foundField = null;
            for (var field : format.definition().fields) {
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
            verifyUsages(namedArgument.value());
          }
        } else {
          var pseudoInstr =
              instructionCall.symbolTable().findPseudoInstruction(instructionCall.id());
          if (pseudoInstr != null) {
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
              verifyUsages(namedArgument.value());
            }
          } else {
            instructionCall.symbolTable()
                .reportError("Unknown instruction " + instructionCall.id().name,
                    instructionCall.loc);
          }
        }
        for (Expr unnamedArgument : instructionCall.unnamedArguments) {
          verifyUsages(unnamedArgument);
        }
      } else if (stmt instanceof LockStatement lock) {
        verifyUsages(lock.expr);
        verifyUsages(lock.statement);
      } else if (stmt instanceof ForallStatement forall) {
        for (ForallStatement.Index index : forall.indices) {
          verifyUsages(index.domain());
        }
        verifyUsages(forall.statement);
      }
    }

    static void verifyUsages(Expr expr) {
      if (expr instanceof LetExpr letExpr) {
        verifyUsages(letExpr.valueExpr);
        verifyUsages(letExpr.body);
      } else if (expr instanceof IfExpr ifExpr) {
        verifyUsages(ifExpr.condition);
        verifyUsages(ifExpr.thenExpr);
        verifyUsages(ifExpr.elseExpr);
      } else if (expr instanceof GroupedExpr group) {
        for (Expr inner : group.expressions) {
          verifyUsages(inner);
        }
      } else if (expr instanceof UnaryExpr unary) {
        verifyUsages(unary.operand);
      } else if (expr instanceof BinaryExpr binary) {
        verifyUsages(binary.left);
        verifyUsages(binary.right);
      } else if (expr instanceof CastExpr cast) {
        verifyUsages(cast.value);
      } else if (expr instanceof CallExpr call) {
        verifyUsages((Expr) call.target);
        for (List<Expr> argsIndex : call.argsIndices) {
          for (Expr index : argsIndex) {
            verifyUsages(index);
          }
        }
        for (CallExpr.SubCall subCall : call.subCalls) {
          for (List<Expr> argsIndex : subCall.argsIndices()) {
            for (Expr index : argsIndex) {
              verifyUsages(index);
            }
          }
        }
      } else if (expr instanceof SymbolExpr sym) {
        verifyUsages((Expr) sym.path());
        verifyUsages(sym.size);
      } else if (expr instanceof IsId id) {
        var symbol = expr.symbolTable().resolveSymbol(id.pathToString());
        if (symbol == null) {
          expr.symbolTable().reportError("Symbol not found: " + id.pathToString(), id.location());
        }
      } else if (expr instanceof MatchExpr match) {
        verifyUsages(match.candidate);
        verifyUsages(match.defaultResult);
        for (MatchExpr.Case matchCase : match.cases) {
          verifyUsages(matchCase.result());
          for (Expr pattern : matchCase.patterns()) {
            verifyUsages(pattern);
          }
        }
      } else if (expr instanceof ExistsInThenExpr existsInThen) {
        verifyUsages(existsInThen.thenExpr);
      } else if (expr instanceof ForallThenExpr forAllThen) {
        verifyUsages(forAllThen.thenExpr);
      }  else if (expr instanceof ForallExpr forallExpr) {
        for (ForallExpr.Index index : forallExpr.indices) {
          verifyUsages(index.domain());
        }
        verifyUsages(forallExpr.expr);
      } else if (expr instanceof SequenceCallExpr sequenceCall) {
        verifyUsages(sequenceCall.target);
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
