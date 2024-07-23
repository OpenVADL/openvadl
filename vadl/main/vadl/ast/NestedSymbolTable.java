package vadl.ast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import vadl.error.VadlError;
import vadl.utils.SourceLocation;

class NestedSymbolTable implements DefinitionVisitor<Void> {
  @Nullable
  NestedSymbolTable parent = null;
  final List<NestedSymbolTable> children = new ArrayList<>();
  final Map<String, Symbol> symbols = new HashMap<>();
  List<Requirement> requirements = new ArrayList<>();
  List<VadlError> errors = new ArrayList<>();

  void defineConstant(String name, SourceLocation loc) {
    defineSymbol(new ValuedSymbol(name, null, SymbolType.CONSTANT), loc);
  }

  void defineSymbol(String name, SymbolType type, SourceLocation loc) {
    defineSymbol(new BasicSymbol(name, type), loc);
  }

  void defineSymbol(Symbol symbol, SourceLocation loc) {
    verifyAvailable(symbol.name(), loc);
    symbols.put(symbol.name(), symbol);
  }

  NestedSymbolTable createChild() {
    NestedSymbolTable child = new NestedSymbolTable();
    child.parent = this;
    child.errors = this.errors;
    this.children.add(child);
    return child;
  }

  NestedSymbolTable createFormatScope(Identifier formatId) {
    NestedSymbolTable child = createChild();
    child.requirements.add(new FormatRequirement(formatId));
    return child;
  }

  NestedSymbolTable createInstructionScope(Identifier instrId) {
    NestedSymbolTable child = createChild();
    child.requirements.add(new InstructionRequirement(instrId));
    return child;
  }

  @Override
  public Void visit(ConstantDefinition definition) {
    defineConstant(definition.identifier.name, definition.location());
    return null;
  }

  @Override
  public Void visit(FormatDefinition definition) {
    verifyAvailable(definition.identifier.name, definition.location());
    symbols.put(definition.identifier.name,
        new FormatSymbol(definition.identifier.name, definition));
    return null;
  }

  @Override
  public Void visit(InstructionSetDefinition definition) {
    defineSymbol(definition.identifier.name, SymbolType.INSTRUCTION_SET, definition.location());
    return null;
  }

  @Override
  public Void visit(CounterDefinition definition) {
    var typeSymbol = resolveSymbol(definition.type.baseType.name);
    var typeDef = typeSymbol instanceof FormatSymbol s ? s.definition : null;
    defineSymbol(new ValuedSymbol(definition.identifier.name, typeDef, SymbolType.COUNTER),
        definition.location());
    return null;
  }

  @Override
  public Void visit(MemoryDefinition definition) {
    defineSymbol(definition.identifier.name, SymbolType.MEMORY, definition.location());
    return null;
  }

  @Override
  public Void visit(RegisterDefinition definition) {
    var typeSymbol = resolveSymbol(definition.type.baseType.name);
    var typeDef = typeSymbol instanceof FormatSymbol s ? s.definition : null;
    defineSymbol(new ValuedSymbol(definition.identifier.name, typeDef, SymbolType.REGISTER),
        definition.location());
    return null;
  }

  @Override
  public Void visit(RegisterFileDefinition definition) {
    defineSymbol(definition.identifier.name, SymbolType.REGISTER_FILE, definition.location());
    return null;
  }

  @Override
  public Void visit(InstructionDefinition definition) {
    if (definition.identifier instanceof Identifier id
        && definition.typeIdentifier instanceof Identifier typeId) {
      defineSymbol(new InstructionSymbol(id.name, definition), definition.loc);
      requirements.add(new SymbolRequirement(typeId.name, SymbolType.FORMAT, typeId.loc));
    }
    return null;
  }

  @Override
  public Void visit(EncodingDefinition definition) {
    var formatSymbol = requireInstructionFormat(definition.instrIdentifier);
    if (formatSymbol == null) {
      reportError("Unknown instruction " + definition.instrIdentifier.name, definition.location());
      return null;
    }
    var format = formatSymbol.definition;
    for (EncodingDefinition.Entry entry : definition.entries) {
      var name = entry.field().name;
      var field = format.fields.stream().filter(f -> f.identifier().name.equals(name)).findFirst();
      if (field.isEmpty()) {
        reportError("Unknown field %s in format %s".formatted(name, format.identifier.name),
            entry.field().location());
      }
    }
    return null;
  }

  @Override
  public Void visit(AssemblyDefinition definition) {
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

  @Nullable
  FormatSymbol requireFormat(Identifier formatId) {
    var symbol = resolveSymbol(formatId.name);
    if (symbol instanceof FormatSymbol formatSymbol) {
      formatSymbol.definition.fields.forEach(field -> symbols.put(field.identifier().name,
          new ValuedSymbol(field.identifier().name, null, SymbolType.FORMAT_FIELD)));
      return formatSymbol;
    } else {
      errors.add(
          new VadlError("Unresolved format " + formatId.name, formatId.location(),
              null, null));
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
      errors.add(
          new VadlError("Unresolved instruction " + instrId.name, instrId.location(),
              null, null));
      return null;
    }
  }

  void requireValue(VariableAccess var) {
    requirements.add(new VariableAccessRequirement(var));
  }

  void validate() {
    for (Requirement requirement : requirements) {
      if (requirement instanceof SymbolRequirement req) {
        var symbol = resolveSymbol(req.name);
        if (symbol == null) {
          errors.add(
              new VadlError("Unresolved definition " + req.name, req.loc, null,
                  null));
        } else if (symbol.type() != req.type()) {
          errors.add(new VadlError(
              "Mismatched type %s: required %s, found %s".formatted(req.name, req.type.name(),
                  symbol.type().name()), req.loc, null, null));
        }
      } else if (requirement instanceof VariableAccessRequirement req) {
        validateValueAccess(req);
      } else if (requirement instanceof FormatRequirement req) {
        requireFormat(req.formatId);
      } else if (requirement instanceof InstructionRequirement req) {
        requireInstructionFormat(req.instrId);
      }
    }
    children.forEach(NestedSymbolTable::validate);
  }

  void validateValueAccess(VariableAccessRequirement variableAccessRequirement) {
    var var = variableAccessRequirement.variableAccess;
    var symbol = resolveSymbol(var.identifier.name);
    if (symbol == null) {
      reportError("Unresolved definition " + var.identifier.name, var.location());
      return;
    }
    if (symbol instanceof ValuedSymbol valSymbol) {
      if (var.next == null) {
        return;
      }
      if (!(valSymbol.typeDefinition instanceof FormatDefinition formatDefinition)) {
        reportError("Invalid usage: symbol %s of type %s does not have a record type".formatted(
            var.identifier.name, symbol.type()), var.location());
        return;
      }
      verifyFormatAccess(formatDefinition, var.next);
    } else {
      reportError(
          "Invalid usage: symbol %s of type %s does not have a value".formatted(var.identifier.name,
              symbol.type()), var.location());
    }
  }

  @Nullable
  private FormatDefinition.FormatField findField(FormatDefinition format, String name) {
    return format.fields.stream()
        .filter(f -> f.identifier().name.equals(name))
        .findFirst().orElse(null);
  }

  private void verifyFormatAccess(FormatDefinition format, VariableAccess access) {
    var field = findField(format, access.identifier.name);
    if (field == null) {
      reportError(
          "Invalid usage: format %s does not have field %s".formatted(format.identifier.name,
              access.identifier.name), access.location());
    } else if (field instanceof FormatDefinition.RangeFormatField && access.next != null) {
      reportError("Invalid usage: field %s resolves to a range, does not provide fields to access"
          .formatted(field.identifier().name), access.location());
    } else if (field instanceof FormatDefinition.TypedFormatField f) {
      if (isValuedAnnotation(f.typeAnnotation)) {
        if (access.next != null) {
          reportError(
              "Invalid usage: field %s resolves to %s, does not provide fields to access".formatted(
                  field.identifier().name, f.typeAnnotation.baseType), access.location());
        }
      } else if (access.next == null) {
        reportError(
            "Invalid usage: field %s resolves to %s, which does not provide a value".formatted(
                field.identifier().name, f.typeAnnotation.baseType.name), access.location());
      } else {
        var typeSymbol = f.symbolTable.resolveSymbol(f.typeAnnotation.baseType.name);
        if (typeSymbol instanceof FormatSymbol formatSymbol) {
          verifyFormatAccess(formatSymbol.definition, access.next);
        } else if (typeSymbol == null) {
          reportError("Invalid usage: type %s not found".formatted(f.identifier.name),
              f.location());
        } else {
          reportError("Invalid usage: symbol %s of type %s does not have a record type".formatted(
              f.identifier.name, typeSymbol.type().name()), access.location());
        }
      }
    }
  }

  private boolean isValuedAnnotation(TypeLiteral typeAnnotation) {
    return typeAnnotation.baseType.name.equals("Bool")
        || typeAnnotation.baseType.name.equals("Bits");
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
    FORMAT_FIELD, MACRO;
  }

  interface Symbol {
    String name();

    SymbolType type();
  }

  record BasicSymbol(String name, SymbolType type) implements Symbol {
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

  record InstructionSymbol(String name, InstructionDefinition definition) implements Symbol {
    @Override
    public SymbolType type() {
      return SymbolType.INSTRUCTION;
    }
  }

  interface Requirement {

  }


  record SymbolRequirement(String name, SymbolType type, SourceLocation loc)
      implements Requirement {
  }

  record VariableAccessRequirement(VariableAccess variableAccess) implements Requirement {
  }

  record FormatRequirement(Identifier formatId) implements Requirement {
  }

  record InstructionRequirement(Identifier instrId) implements Requirement {
  }
}
