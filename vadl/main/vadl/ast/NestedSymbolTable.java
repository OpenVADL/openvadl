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

  void loadBuiltins() {
    defineSymbol(new ValuedSymbol("register", null, SymbolType.FUNCTION),
        SourceLocation.INVALID_SOURCE_LOCATION);
    defineSymbol(new ValuedSymbol("decimal", null, SymbolType.FUNCTION),
        SourceLocation.INVALID_SOURCE_LOCATION);
    defineSymbol(new ValuedSymbol("hex", null, SymbolType.FUNCTION),
        SourceLocation.INVALID_SOURCE_LOCATION);
    defineSymbol(new ValuedSymbol("addr", null, SymbolType.FUNCTION),
        SourceLocation.INVALID_SOURCE_LOCATION);
  }

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

  NestedSymbolTable createFunctionScope(List<FunctionDefinition.Parameter> params) {
    NestedSymbolTable child = createChild();
    for (FunctionDefinition.Parameter param : params) {
      defineSymbol(new ValuedSymbol(param.name().name, null, SymbolType.CONSTANT),
          param.name().loc);
    }
    return child;
  }

  @Override
  public Void visit(ConstantDefinition definition) {
    defineConstant(definition.identifier().name, definition.location());
    return null;
  }

  @Override
  public Void visit(FormatDefinition definition) {
    verifyAvailable(definition.identifier().name, definition.location());
    symbols.put(definition.identifier().name,
        new FormatSymbol(definition.identifier().name, definition));
    return null;
  }

  @Override
  public Void visit(InstructionSetDefinition definition) {
    defineSymbol(definition.identifier.name, SymbolType.INSTRUCTION_SET, definition.location());
    return null;
  }

  @Override
  public Void visit(CounterDefinition definition) {
    var typeSymbol = resolveSymbol(definition.type.baseType.pathToString());
    var typeDef = typeSymbol instanceof FormatSymbol s ? s.definition : null;
    defineSymbol(new ValuedSymbol(definition.identifier().name, typeDef, SymbolType.COUNTER),
        definition.location());
    return null;
  }

  @Override
  public Void visit(MemoryDefinition definition) {
    defineSymbol(new ValuedSymbol(definition.identifier().name, definition, SymbolType.MEMORY),
        definition.location());
    return null;
  }

  @Override
  public Void visit(RegisterDefinition definition) {
    var typeSymbol = resolveSymbol(definition.type.baseType.pathToString());
    var typeDef = typeSymbol instanceof FormatSymbol s ? s.definition : null;
    defineSymbol(new ValuedSymbol(definition.identifier().name, typeDef, SymbolType.REGISTER),
        definition.location());
    return null;
  }

  @Override
  public Void visit(RegisterFileDefinition definition) {
    defineSymbol(
        new ValuedSymbol(definition.identifier().name, definition, SymbolType.REGISTER_FILE),
        definition.location());
    return null;
  }

  @Override
  public Void visit(InstructionDefinition definition) {
    defineSymbol(new InstructionSymbol(definition.id().name, definition), definition.loc);
    requirements.add(new SymbolRequirement(definition.type().name, SymbolType.FORMAT,
        definition.type().loc));
    return null;
  }

  @Override
  public Void visit(EncodingDefinition definition) {
    var formatSymbol = requireInstructionFormat(definition.instrId());
    if (formatSymbol == null) {
      reportError("Unknown instruction " + definition.instrId().name, definition.location());
      return null;
    }
    var format = formatSymbol.definition;
    for (EncodingDefinition.FieldEncoding entry : definition.fieldEncodings) {
      var name = entry.field().name;
      var field = format.fields.stream().filter(f -> f.identifier().name.equals(name)).findFirst();
      if (field.isEmpty()) {
        reportError("Unknown field %s in format %s".formatted(name, format.identifier().name),
            entry.field().location());
      }
    }
    return null;
  }

  @Override
  public Void visit(AssemblyDefinition definition) {
    return null;
  }

  @Override
  public Void visit(UsingDefinition definition) {
    defineSymbol(new AliasSymbol(definition.identifier().name, definition.type), definition.loc);
    return null;
  }

  @Override
  public Void visit(FunctionDefinition definition) {
    defineSymbol(new ValuedSymbol(definition.name().name, null, SymbolType.FUNCTION),
        definition.loc);
    return null;
  }

  @Override
  public Void visit(PlaceholderDefinition definition) {
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

  void requireValue(IsCallExpr callExpr) {
    requirements.add(new ValueRequirement(callExpr));
  }

  List<VadlError> validate() {
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
      } else if (requirement instanceof ValueRequirement req) {
        validateValueAccess(req);
      } else if (requirement instanceof FormatRequirement req) {
        requireFormat(req.formatId);
      } else if (requirement instanceof InstructionRequirement req) {
        requireInstructionFormat(req.instrId);
      }
    }
    children.forEach(NestedSymbolTable::validate);
    return errors;
  }

  void validateValueAccess(ValueRequirement requirement) {
    var expr = requirement.callExpr;
    var path = expr.path().pathToString();
    var symbol = resolveSymbol(path);
    if (symbol == null) {
      reportError("Unresolved definition " + path, expr.location());
      return;
    }

    if (symbol instanceof ValuedSymbol valSymbol) {
      if (expr.subCalls().isEmpty()) {
        return;
      }
      if (!(valSymbol.typeDefinition instanceof FormatDefinition formatDefinition)) {
        reportError("Invalid usage: symbol %s of type %s does not have a record type".formatted(
            path, symbol.type()), expr.location());
        return;
      }
      verifyFormatAccess(formatDefinition, expr.subCalls());
    } else {
      reportError("Invalid usage: symbol %s of type %s does not have a value"
          .formatted(path, symbol.type()), expr.location());
    }
  }

  @Nullable
  private FormatDefinition.FormatField findField(FormatDefinition format, String name) {
    return format.fields.stream()
        .filter(f -> f.identifier().name.equals(name))
        .findFirst().orElse(null);
  }

  private void verifyFormatAccess(FormatDefinition format, List<CallExpr.SubCall> subCalls) {
    if (subCalls.isEmpty()) {
      return;
    }
    var next = subCalls.get(0).id();
    var field = findField(format, next.name);
    if (field == null) {
      reportError(
          "Invalid usage: format %s does not have field %s".formatted(format.identifier().name,
              next.name), next.location());
    } else if (field instanceof FormatDefinition.RangeFormatField && subCalls.size() > 1) {
      reportError("Invalid usage: field %s resolves to a range, does not provide fields to chain"
          .formatted(field.identifier().name), next.location());
    } else if (field instanceof FormatDefinition.DerivedFormatField && subCalls.size() > 1) {
      reportError("Invalid usage: field %s is derived, does not provide fields to chain"
          .formatted(field.identifier().name), next.location());
    } else if (field instanceof FormatDefinition.TypedFormatField f) {
      if (isValuedAnnotation(f.type())) {
        if (subCalls.size() > 1) {
          reportError(
              "Invalid usage: field %s resolves to %s, does not provide fields to chain".formatted(
                  field.identifier().name, f.type().baseType), next.location());
        }
      } else if (subCalls.size() > 1) {
        var typeSymbol = resolveAlias(f.symbolTable.resolveSymbol(f.type().baseType.pathToString()));
        if (typeSymbol instanceof FormatSymbol formatSymbol) {
          verifyFormatAccess(formatSymbol.definition, subCalls.subList(1, subCalls.size()));
        } else if (typeSymbol == null) {
          reportError("Invalid usage: type %s not found".formatted(f.identifier.name),
              f.location());
        } else {
          reportError("Invalid usage: symbol %s of type %s does not have a record type".formatted(
              f.identifier.name, typeSymbol.type().name()), next.location());
        }
      }
    }
  }

  private @Nullable Symbol resolveAlias(@Nullable Symbol symbol) {
    if (symbol instanceof AliasSymbol alias) {
      return resolveAlias(resolveSymbol(alias.aliasType.baseType.pathToString()));
    }
    return symbol;
  }

  private boolean isValuedAnnotation(TypeLiteral type) {
    String baseType = type.baseType.pathToString();
    if (resolveSymbol(baseType) instanceof AliasSymbol alias) {
      return isValuedAnnotation(alias.aliasType);
    }
    // TODO Built-in types should be configurable, not hardcoded
    return baseType.equals("Bool")
        || baseType.equals("Bits")
        || baseType.equals("SInt");
  }

  private void verifyAvailable(String name, SourceLocation loc) {
    if (symbols.containsKey(name)) {
      // TODO Fix duplicate definitions if only one of the definitions will be accepted into the AST
      // reportError("Duplicate definition: " + name, loc);
    }
  }

  private void reportError(String error, SourceLocation location) {
    errors.add(new VadlError(error, location, null, null));
  }

  enum SymbolType {
    CONSTANT, COUNTER, FORMAT, INSTRUCTION, INSTRUCTION_SET, MEMORY, REGISTER, REGISTER_FILE,
    FORMAT_FIELD, MACRO, ALIAS, FUNCTION
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

  interface Requirement {

  }


  record SymbolRequirement(String name, SymbolType type, SourceLocation loc)
      implements Requirement {
  }

  record ValueRequirement(IsCallExpr callExpr) implements Requirement {
  }

  record FormatRequirement(Identifier formatId) implements Requirement {
  }

  record InstructionRequirement(Identifier instrId) implements Requirement {
  }
}
