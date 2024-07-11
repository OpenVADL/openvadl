package vadl.ast;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumSet;
import java.util.List;
import javax.annotation.Nullable;
import vadl.error.VadlError;
import vadl.utils.SourceLocation;

class StackBasedSymbolTable implements DefinitionVisitor<Void> {
  final List<Symbol> symbols = new ArrayList<>();
  final Deque<Integer> offsets = new ArrayDeque<>();
  List<VadlError> errors = new ArrayList<>();

  StackBasedSymbolTable() {
    offsets.push(0);
  }

  void openScope() {
    offsets.push(symbols.size());
  }

  void closeScope() {
    var offset = offsets.pop();
    symbols.subList(offset, symbols.size()).clear();
  }

  void defineConstant(String name, SourceLocation loc) {
    defineSymbol(name, SymbolType.CONSTANT, loc);
  }

  void defineSymbol(String name, SymbolType type, SourceLocation loc) {
    verifyAvailable(name, loc);
    symbols.add(new BasicSymbol(name, type));
  }

  @Override
  public Void visit(ConstantDefinition definition) {
    defineConstant(definition.identifier.name, definition.location());
    return null;
  }

  @Override
  public Void visit(FormatDefinition definition) {
    verifyAvailable(definition.identifier.name, definition.location());
    symbols.add(new FormatSymbol(definition.identifier.name, definition));
    return null;
  }

  @Override
  public Void visit(InstructionSetDefinition definition) {
    defineSymbol(definition.identifier.name, SymbolType.INSTRUCTION_SET, definition.location());
    return null;
  }

  @Override
  public Void visit(CounterDefinition definition) {
    defineSymbol(definition.identifier.name, SymbolType.COUNTER, definition.location());
    return null;
  }

  @Override
  public Void visit(MemoryDefinition definition) {
    defineSymbol(definition.identifier.name, SymbolType.MEMORY, definition.location());
    return null;
  }

  @Override
  public Void visit(RegisterDefinition definition) {
    defineSymbol(definition.identifier.name, SymbolType.REGISTER, definition.location());
    return null;
  }

  @Override
  public Void visit(RegisterFileDefinition definition) {
    defineSymbol(definition.identifier.name, SymbolType.REGISTER_FILE, definition.location());
    return null;
  }

  @Override
  public Void visit(InstructionDefinition definition) {
    verifyAvailable(definition.identifier.name, definition.location());
    symbols.add(new InstructionSymbol(definition.identifier.name, definition));
    return null;
  }

  @Override
  public Void visit(EncodingDefinition definition) {
    var format = resolveInstructionFormat(definition.instrIdentifier.name);
    if (format == null) {
      errors.add(
          new VadlError("Unknown instruction " + definition.instrIdentifier.name,
              definition.location(), null, null));
      return null;
    }
    for (EncodingDefinition.Entry entry : definition.entries) {
      var name = entry.field().name;
      var field = format.fields.stream().filter(f -> f.identifier.name.equals(name)).findFirst();
      if (field.isEmpty()) {
        errors.add(
            new VadlError("Unknown field %s in format %s".formatted(name, format.identifier.name),
                entry.field().location(), null, null));
      }
    }
    return null;
  }

  void addMacro(Macro macro, SourceLocation loc) {
    verifyAvailable(macro.name().name, loc);
    symbols.add(new MacroSymbol(macro.name().name, macro));
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
    return symbols.stream().filter(s -> s.name().equals(name)).findFirst().orElse(null);
  }

  void loadFormat(Identifier formatIdentifier) {
    var format = resolveSymbol(formatIdentifier.name);
    if (format instanceof FormatSymbol formatSymbol) {
      formatSymbol.definition.fields.forEach(field ->
          defineSymbol(field.identifier.name, SymbolType.FORMAT_FIELD, field.location()));
    } else {
      errors.add(new VadlError(
          "Unknown format " + formatIdentifier.name, formatIdentifier.location(), null, null
      ));
    }
  }

  @Nullable
  FormatDefinition resolveInstructionFormat(String name) {
    var instruction = resolveSymbol(name);
    if (instruction instanceof InstructionSymbol instructionSymbol) {
      var format = resolveSymbol(instructionSymbol.definition.typeIdentifier.name);
      if (format instanceof FormatSymbol formatSymbol) {
        return formatSymbol.definition;
      }
    }
    return null;
  }

  void requireValue(String name, SourceLocation loc) {
    var symbol = resolveSymbol(name);
    if (symbol == null) {
      errors.add(new VadlError("Unresolved definition " + name, loc, null, null));
    } else if (!SymbolType.valuedTypes.contains(symbol.type())) {
      errors.add(new VadlError(
          "Invalid usage: symbol %s of type %s does not have a value".formatted(name,
              symbol.type()), loc, null, null));
    }
  }

  private void verifyAvailable(String name, SourceLocation loc) {
    var symbol = resolveSymbol(name);
    var currentScopeOffset = offsets.peek();
    if (symbol != null && (currentScopeOffset == null ||
        symbols.indexOf(resolveSymbol(name)) >= currentScopeOffset)) {
      errors.add(new VadlError("Duplicate definition " + name, loc, null, null));
    }
  }

  enum SymbolType {
    // TODO Maybe unify with syntax types / core types?
    CONSTANT, COUNTER, FORMAT, INSTRUCTION, INSTRUCTION_SET, MEMORY, REGISTER, REGISTER_FILE,
    FORMAT_FIELD, MACRO;

    static final EnumSet<SymbolType> valuedTypes =
        EnumSet.of(CONSTANT, COUNTER, REGISTER, FORMAT_FIELD);
  }

  interface Symbol {
    String name();

    SymbolType type();
  }

  record BasicSymbol(String name, SymbolType type) implements Symbol {
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

  record InstructionSymbol(String name, InstructionDefinition definition) implements Symbol {
    @Override
    public SymbolType type() {
      return SymbolType.INSTRUCTION;
    }
  }
}
