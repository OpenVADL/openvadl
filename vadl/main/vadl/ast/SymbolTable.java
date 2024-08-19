package vadl.ast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;
import vadl.error.VadlError;
import vadl.utils.SourceLocation;

class SymbolTable {
  @Nullable
  SymbolTable parent = null;
  final List<SymbolTable> children = new ArrayList<>();
  final Map<String, Symbol> symbols = new HashMap<>();
  List<VadlError> errors = new ArrayList<>();

  void loadBuiltins() {
    defineSymbol(new ValuedSymbol("register", null, SymbolType.FUNCTION),
        SourceLocation.INVALID_SOURCE_LOCATION);
    defineSymbol(new ValuedSymbol("decimal", null, SymbolType.FUNCTION),
        SourceLocation.INVALID_SOURCE_LOCATION);
    defineSymbol(new ValuedSymbol("hex", null, SymbolType.FUNCTION),
        SourceLocation.INVALID_SOURCE_LOCATION);
    defineSymbol(new ValuedSymbol("mnemonic", null, SymbolType.CONSTANT),
        SourceLocation.INVALID_SOURCE_LOCATION);
    defineSymbol(new ValuedSymbol("VADL::div", null, SymbolType.FUNCTION),
        SourceLocation.INVALID_SOURCE_LOCATION);
    defineSymbol(new ValuedSymbol("VADL::mod", null, SymbolType.FUNCTION),
        SourceLocation.INVALID_SOURCE_LOCATION);
    defineSymbol(new ValuedSymbol("VADL::ror", null, SymbolType.FUNCTION),
        SourceLocation.INVALID_SOURCE_LOCATION);
  }

  void defineConstant(String name, SourceLocation loc) {
    defineSymbol(new ValuedSymbol(name, null, SymbolType.CONSTANT), loc);
  }

  void defineSymbol(Symbol symbol, SourceLocation loc) {
    verifyAvailable(symbol.name(), loc);
    symbols.put(symbol.name(), symbol);
  }

  SymbolTable createChild() {
    SymbolTable child = new SymbolTable();
    child.parent = this;
    child.errors = this.errors;
    this.children.add(child);
    return child;
  }

  void addMacro(Macro macro, SourceLocation loc) {
    verifyAvailable(macro.name().name, loc);
    symbols.put(macro.name().name, new MacroSymbol(macro.name().name, macro));
  }

  @Nullable
  Macro getMacro(String name) {
    Symbol symbol = resolveSymbol(name);
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
  FormatSymbol requireInstructionFormat(Identifier instrId) {
    var symbol = resolveSymbol(instrId.name);
    if (symbol instanceof InstructionSymbol instructionSymbol
        && instructionSymbol.definition.typeIdentifier instanceof Identifier typeId) {
      return requireFormat(typeId);
    } else {
      reportError("Unresolved instruction " + instrId.name, instrId.location());
      return null;
    }
  }

  @Nullable
  InstructionSetDefinition findIsa(Identifier isa) {
    var symbol = resolveSymbol(isa.name);
    if (symbol instanceof IsaSymbol isaSymbol) {
      return isaSymbol.definition;
    }
    reportError("Unresolved ISA " + isa.name, isa.location());
    return null;
  }

  void addRecord(Identifier name, RecordType recordType) {
    defineSymbol(new RecordSymbol(name.name, recordType), name.location());
  }

  SyntaxType findType(Identifier recordName) {
    var symbol = resolveSymbol(recordName.name);
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
  }

  private void verifyAvailable(String name, SourceLocation loc) {
    if (symbols.containsKey(name)) {
      reportError("Duplicate definition: " + name, loc);
    }
  }

  private void reportError(String error, SourceLocation location) {
    errors.add(new VadlError(error, location, null, null));
  }

  enum SymbolType {
    CONSTANT, COUNTER, FORMAT, INSTRUCTION, INSTRUCTION_SET, MEMORY, REGISTER, REGISTER_FILE,
    FORMAT_FIELD, MACRO, ALIAS, FUNCTION, ENUM_FIELD, EXCEPTION, RECORD, MODEL_TYPE
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
   */
  static class SymbolCollector {
    static void collectSymbols(Ast ast) {
      ast.rootSymbolTable = new SymbolTable();
      ast.rootSymbolTable.loadBuiltins();
      for (Definition definition : ast.definitions) {
        collectSymbols(ast.rootSymbolTable, definition);
      }
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
        for (FunctionDefinition.Parameter param : function.params) {
          function.symbolTable.defineConstant(param.name().name, param.name().location());
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
      }
    }

    static void collectSymbols(SymbolTable symbols, Statement stmt) {
      stmt.symbolTable = symbols;
      if (stmt instanceof BlockStatement block) {
        for (Statement inner : block.statements) {
          collectSymbols(symbols, inner);
        }
      } else if (stmt instanceof LetStatement let) {
        let.symbolTable = symbols.createChild();
        for (var identifier : let.identifiers) {
          let.symbolTable.defineConstant(identifier.name, identifier.location());
        }
        collectSymbols(symbols, let.valueExpression);
        collectSymbols(let.symbolTable, let.body);
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
      }
    }
  }

  /**
   * Verifies that identifiers used in expressions, as well as types used in definitions,
   * actually exist in the VADL file.
   * The AST is not modified in this pass, only errors are gathered.
   * Before & After: Ast is fully Macro-expanded and all relevant nodes have "symbolTable" set.
   */
  static class VerificationPass {
    static List<VadlError> verifyUsages(Ast ast) {
      for (Definition definition : ast.definitions) {
        verifyUsages(definition);
      }
      return Objects.requireNonNull(ast.rootSymbolTable).errors;
    }

    static void verifyUsages(Definition definition) {
      if (definition instanceof InstructionSetDefinition isa) {
        if (isa.extending != null) {
          var extending = isa.symbolTable().findIsa(isa.extending);
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
      } else if (definition instanceof AssemblyDefinition assembly) {
        for (IdentifierOrPlaceholder identifier : assembly.identifiers) {
          var format = assembly.symbolTable().requireInstructionFormat((Identifier) identifier);
          if (format != null) {
            assembly.symbolTable().copyFrom(format.definition().symbolTable());
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
