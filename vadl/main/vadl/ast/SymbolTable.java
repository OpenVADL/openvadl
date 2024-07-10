package vadl.ast;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import vadl.error.VadlError;
import vadl.error.VadlException;
import vadl.utils.SourceLocation;

class SymbolTable implements DefinitionVisitor<Void> {
  @Nullable
  SymbolTable parent = null;
  final List<SymbolTable> children = new ArrayList<>();
  final Map<String, Symbol> symbols = new HashMap<>();
  final Map<String, FormatDefinition> formats = new HashMap<>();
  final Map<String, FormatDefinition> instructionFormats = new HashMap<>();
  List<VadlError> errors = new ArrayList<>();

  void defineConstant(String name, SourceLocation loc) {
    defineSymbol(name, SymbolType.CONSTANT, loc);
  }

  void defineSymbol(String name, SymbolType type, SourceLocation loc) {
    verifyAvailable(name, loc);
    symbols.put(name, new BasicSymbol(name, type));
  }

  SymbolTable createChild() {
    SymbolTable child = new SymbolTable();
    child.parent = this;
    child.errors = this.errors;
    this.children.add(child);
    return child;
  }

  @Override
  public Void visit(ConstantDefinition definition) {
    defineConstant(definition.identifier.name, definition.location());
    return null;
  }

  @Override
  public Void visit(FormatDefinition definition) {
    defineSymbol(definition.identifier.name, SymbolType.FORMAT, definition.location());
    formats.put(definition.identifier.name, definition);
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
    defineSymbol(definition.identifier.name, SymbolType.INSTRUCTION, definition.location());
    instructionFormats.put(definition.identifier.name,
        resolveFormat(definition.typeIdentifier.name));
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

  void loadFormat(Identifier formatIdentifier) {
    var format = resolveFormat(formatIdentifier.name);
    if (format != null) {
      format.fields.forEach(field ->
          defineSymbol(field.identifier.name, SymbolType.FORMAT_FIELD, field.location()));
    } else {
      errors.add(new VadlError(
          "Unknown format " + formatIdentifier.name, formatIdentifier.location(), null, null
      ));
    }
  }

  @Nullable
  FormatDefinition resolveInstructionFormat(String name) {
    var format = instructionFormats.get(name);
    if (format != null) {
      return format;
    } else if (parent != null) {
      return parent.resolveInstructionFormat(name);
    } else {
      return null;
    }
  }

  @Nullable
  FormatDefinition resolveFormat(String name) {
    var format = formats.get(name);
    if (format != null) {
      return format;
    } else if (parent != null) {
      return parent.resolveFormat(name);
    } else {
      return null;
    }
  }

  void requireValue(String name, SourceLocation loc) {
    var symbol = resolveSymbol(name);
    if (symbol == null) {
      throw new VadlException(
          List.of(new VadlError("Unresolved definition " + name, loc, null, null)));
    }
    if (!SymbolType.valuedTypes.contains(symbol.type())) {
      throw new VadlException(
          List.of(new VadlError(
              "Invalid usage: symbol %s of type %s does not have a value".formatted(name,
                  symbol.type()), loc, null, null)));
    }
  }

  private void verifyAvailable(String name, SourceLocation loc) {
    if (symbols.containsKey(name)) {
      throw new VadlException(
          List.of(new VadlError("Duplicate definition " + name, loc, null, null)));
    }
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
