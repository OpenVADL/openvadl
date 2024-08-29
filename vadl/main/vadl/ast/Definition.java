package vadl.ast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import vadl.utils.SourceLocation;

/**
 * The Definition nodes inside the AST.
 * A definition defines part of the architecture, but has no side effects and doesn't evaluate to
 * anything.
 */
abstract class Definition extends Node {
  Annotations annotations = new Annotations();

  Definition withAnnotations(Annotations annotations) {
    this.annotations = annotations;
    return this;
  }

  abstract <R> R accept(DefinitionVisitor<R> visitor);
}

interface DefinitionVisitor<R> {
  R visit(ConstantDefinition definition);

  R visit(FormatDefinition definition);

  R visit(InstructionSetDefinition definition);

  R visit(CounterDefinition definition);

  R visit(MemoryDefinition definition);

  R visit(RegisterDefinition definition);

  R visit(RegisterFileDefinition definition);

  R visit(InstructionDefinition definition);

  R visit(PseudoInstructionDefinition definition);

  R visit(EncodingDefinition definition);

  R visit(AssemblyDefinition definition);

  R visit(UsingDefinition definition);

  R visit(FunctionDefinition definition);

  R visit(AliasDefinition definition);

  R visit(EnumerationDefinition definition);

  R visit(ExceptionDefinition definition);

  R visit(PlaceholderDefinition definition);

  R visit(MacroInstanceDefinition definition);

  R visit(MacroMatchDefinition definition);

  R visit(DefinitionList definition);

  R visit(ModelDefinition definition);

  R visit(RecordTypeDefinition definition);
}

class ConstantDefinition extends Definition {
  IdentifierOrPlaceholder identifier;

  @Nullable
  TypeLiteral type;

  Expr value;
  SourceLocation loc;

  ConstantDefinition(IdentifierOrPlaceholder identifier, @Nullable TypeLiteral type, Expr value,
                     SourceLocation location) {
    this.identifier = identifier;
    this.type = type;
    this.value = value;
    this.loc = location;
  }

  Identifier identifier() {
    return (Identifier) identifier;
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.ISA_DEFS;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    annotations.prettyPrint(indent, builder);
    builder.append(prettyIndentString(indent));
    builder.append("constant %s".formatted(identifier().name));
    if (type != null) {
      builder.append(": ");
      type.prettyPrint(indent, builder);
    }
    builder.append(" = ");
    value.prettyPrint(indent, builder);
    builder.append("\n");
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ConstantDefinition that = (ConstantDefinition) o;
    return Objects.equals(annotations, that.annotations)
        && Objects.equals(identifier, that.identifier)
        && Objects.equals(type, that.type)
        && Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    int result = Objects.hashCode(annotations);
    result = 31 * result + Objects.hashCode(identifier);
    result = 31 * result + Objects.hashCode(type);
    result = 31 * result + Objects.hashCode(value);
    return result;
  }
}

class FormatDefinition extends Definition {
  IdentifierOrPlaceholder identifier;
  TypeLiteral type;
  List<FormatField> fields;
  List<AuxiliaryField> auxiliaryFields;
  SourceLocation loc;

  interface FormatField {
    Identifier identifier();

    void prettyPrint(int indent, StringBuilder builder);
  }

  static class RangeFormatField extends Node implements FormatField {
    Identifier identifier;
    List<Expr> ranges;

    public RangeFormatField(Identifier identifier, List<Expr> ranges) {
      this.identifier = identifier;
      this.ranges = ranges;
    }

    @Override
    public Identifier identifier() {
      return identifier;
    }

    @Override
    SourceLocation location() {
      return identifier.location().join(ranges.get(ranges.size() - 1).location());
    }

    @Override
    SyntaxType syntaxType() {
      return BasicSyntaxType.INVALID;
    }

    @Override
    public void prettyPrint(int indent, StringBuilder builder) {
      identifier.prettyPrint(indent, builder);
      builder.append("\t [");
      ranges.get(0).prettyPrint(indent, builder);
      for (int i = 1; i < ranges.size(); i++) {
        builder.append(", ");
        ranges.get(i).prettyPrint(indent, builder);
      }
      builder.append("]");
    }

    @Override
    public String toString() {
      return this.getClass().getSimpleName();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      RangeFormatField that = (RangeFormatField) o;
      return Objects.equals(identifier, that.identifier)
          && Objects.equals(ranges, that.ranges);
    }

    @Override
    public int hashCode() {
      int result = Objects.hashCode(identifier);
      result = 31 * result + Objects.hashCode(ranges);
      return result;
    }
  }

  static class TypedFormatField extends Node implements FormatField {
    final Identifier identifier;
    final TypeLiteralOrPlaceholder type;

    public TypedFormatField(Identifier identifier, TypeLiteralOrPlaceholder type) {
      this.identifier = identifier;
      this.type = type;
    }

    @Override
    public Identifier identifier() {
      return identifier;
    }

    TypeLiteral type() {
      return (TypeLiteral) type;
    }

    @Override
    SourceLocation location() {
      return identifier().location().join(type().location());
    }

    @Override
    SyntaxType syntaxType() {
      return BasicSyntaxType.INVALID;
    }

    @Override
    public void prettyPrint(int indent, StringBuilder builder) {
      identifier.prettyPrint(indent, builder);
      builder.append(" : ");
      type.prettyPrint(indent, builder);
    }

    @Override
    public String toString() {
      return this.getClass().getSimpleName();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      TypedFormatField that = (TypedFormatField) o;
      return Objects.equals(identifier, that.identifier)
          && Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
      int result = Objects.hashCode(identifier);
      result = 31 * result + Objects.hashCode(type);
      result = 31 * result + Objects.hashCode(symbolTable);
      return result;
    }
  }

  static class DerivedFormatField extends Node implements FormatField {
    Identifier identifier;
    Expr expr;

    public DerivedFormatField(Identifier identifier, Expr expr) {
      this.identifier = identifier;
      this.expr = expr;
    }

    @Override
    public Identifier identifier() {
      return identifier;
    }

    @Override
    SourceLocation location() {
      return identifier.location().join(expr.location());
    }

    @Override
    SyntaxType syntaxType() {
      return BasicSyntaxType.INVALID;
    }

    @Override
    public void prettyPrint(int indent, StringBuilder builder) {
      identifier.prettyPrint(indent, builder);
      builder.append(" = ");
      expr.prettyPrint(indent, builder);
    }

    @Override
    public String toString() {
      return this.getClass().getSimpleName();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      DerivedFormatField that = (DerivedFormatField) o;
      return Objects.equals(identifier, that.identifier)
          && Objects.equals(expr, that.expr);
    }

    @Override
    public int hashCode() {
      int result = Objects.hashCode(identifier);
      result = 31 * result + Objects.hashCode(expr);
      return result;
    }
  }

  static class AuxiliaryField extends Node {
    private final AuxiliaryFieldKind kind;
    private final List<AuxiliaryFieldEntry> entries;

    AuxiliaryField(AuxiliaryFieldKind kind, List<AuxiliaryFieldEntry> entries) {
      this.kind = kind;
      this.entries = entries;
    }

    @Override
    SourceLocation location() {
      return entries.get(0).id.location().join(entries.get(entries.size() - 1).expr.location());
    }

    @Override
    SyntaxType syntaxType() {
      return BasicSyntaxType.INVALID;
    }

    @Override
    public void prettyPrint(int indent, StringBuilder builder) {
      builder.append(prettyIndentString(indent));
      builder.append(
          switch (kind) {
            case PREDICATE -> ": predicate\n";
            case ENCODE -> ": encode\n";
          });
      var isFirst = true;
      for (var entry : entries) {
        if (isFirst) {
          builder.append(prettyIndentString(indent + 1)).append("{ ");
          isFirst = false;
        } else {
          builder.append(prettyIndentString(indent + 1)).append(", ");
        }
        entry.id.prettyPrint(0, builder);
        builder.append(" => ");
        entry.expr.prettyPrint(0, builder);
        builder.append("\n");
      }
      builder.append(prettyIndentString(indent + 1)).append("}\n");
    }

    public AuxiliaryFieldKind kind() {
      return kind;
    }

    public List<AuxiliaryFieldEntry> entries() {
      return entries;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (obj == null || obj.getClass() != this.getClass()) {
        return false;
      }
      var that = (AuxiliaryField) obj;
      return Objects.equals(this.kind, that.kind)
          && Objects.equals(this.entries, that.entries);
    }

    @Override
    public int hashCode() {
      return Objects.hash(kind, entries);
    }
  }

  record AuxiliaryFieldEntry(Identifier id, Expr expr) {
  }

  enum AuxiliaryFieldKind {
    PREDICATE, ENCODE
  }

  public FormatDefinition(IdentifierOrPlaceholder identifier, TypeLiteral type,
                          List<FormatField> fields, List<AuxiliaryField> auxiliaryFields,
                          SourceLocation location) {
    this.identifier = identifier;
    this.type = type;
    this.fields = fields;
    this.auxiliaryFields = auxiliaryFields;
    this.loc = location;
  }

  Identifier identifier() {
    return (Identifier) identifier;
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.ISA_DEFS;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    annotations.prettyPrint(indent, builder);
    builder.append(prettyIndentString(indent));
    builder.append("format ");
    identifier.prettyPrint(indent, builder);
    builder.append(": ");
    type.prettyPrint(indent, builder);

    if (fields.isEmpty()) {
      builder.append("\n");
      return;
    }

    builder.append(" =\n");

    builder.append(prettyIndentString(indent));
    builder.append("{ ");

    fields.get(0).prettyPrint(indent, builder);
    builder.append("\n");

    for (int i = 1; i < fields.size(); i++) {
      builder.append(prettyIndentString(indent));
      builder.append(", ");
      fields.get(i).prettyPrint(indent, builder);
      builder.append("\n");

    }

    for (var auxiliaryField : auxiliaryFields) {
      auxiliaryField.prettyPrint(indent, builder);
    }

    builder.append(prettyIndentString(indent));
    builder.append("}\n");
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    FormatDefinition that = (FormatDefinition) o;
    return annotations.equals(that.annotations)
        && identifier.equals(that.identifier)
        && type.equals(that.type)
        && fields.equals(that.fields)
        && auxiliaryFields.equals(that.auxiliaryFields);
  }

  @Override
  public int hashCode() {
    int result = annotations.hashCode();
    result = 31 * result + identifier.hashCode();
    result = 31 * result + type.hashCode();
    result = 31 * result + fields.hashCode();
    result = 31 * result + auxiliaryFields.hashCode();
    return result;
  }
}

class InstructionSetDefinition extends Definition {
  Identifier identifier;
  @Nullable
  Identifier extending;
  List<Definition> definitions;
  SourceLocation loc;

  InstructionSetDefinition(Identifier identifier, @Nullable Identifier extending,
                           List<Definition> statements, SourceLocation location) {
    this.identifier = identifier;
    this.extending = extending;
    this.definitions = statements;
    this.loc = location;
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.INVALID;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    annotations.prettyPrint(indent, builder);
    builder.append(prettyIndentString(indent));
    builder.append("instruction set architecture ").append(identifier.name);
    if (extending != null) {
      builder.append(" extending ").append(extending.name);
    }
    builder.append(" = {\n");
    Definition previousDefinition = null;
    for (Definition definition : definitions) {
      if (previousDefinition != null
          && !definition.getClass().equals(previousDefinition.getClass())) {
        builder.append("\n");
      }
      definition.prettyPrint(indent + 1, builder);
      previousDefinition = definition;
    }
    builder.append("}\n");
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    var that = (InstructionSetDefinition) o;
    return Objects.equals(annotations, that.annotations)
        && Objects.equals(identifier, that.identifier)
        && Objects.equals(extending, that.extending)
        && Objects.equals(definitions, that.definitions);
  }

  @Override
  public int hashCode() {
    int result = Objects.hashCode(annotations);
    result = 31 * result + Objects.hashCode(identifier);
    result = 31 * result + Objects.hashCode(extending);
    result = 31 * result + Objects.hashCode(definitions);
    return result;
  }
}

class CounterDefinition extends Definition {
  CounterKind kind;
  IdentifierOrPlaceholder identifier;
  TypeLiteral type;
  SourceLocation loc;

  enum CounterKind {
    PROGRAM,
    GROUP
  }

  public CounterDefinition(CounterKind kind, IdentifierOrPlaceholder identifier, TypeLiteral type,
                           SourceLocation location) {
    this.kind = kind;
    this.identifier = identifier;
    this.type = type;
    this.loc = location;
  }

  Identifier identifier() {
    return (Identifier) identifier;
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.ISA_DEFS;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    annotations.prettyPrint(indent, builder);
    builder.append(prettyIndentString(indent));
    builder.append("%s counter ".formatted(kind.toString().toLowerCase(Locale.ENGLISH)));
    identifier.prettyPrint(indent, builder);
    builder.append(": ");
    type.prettyPrint(indent, builder);
    builder.append("\n");
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return "%s kind: %s".formatted(this.getClass().getSimpleName(), kind.toString());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    CounterDefinition that = (CounterDefinition) o;
    return annotations.equals(that.annotations)
        && kind == that.kind
        && identifier.equals(that.identifier)
        && type.equals(that.type);
  }

  @Override
  public int hashCode() {
    int result = annotations.hashCode();
    result = 31 * result + kind.hashCode();
    result = 31 * result + identifier.hashCode();
    result = 31 * result + type.hashCode();
    return result;
  }
}

class MemoryDefinition extends Definition {
  IdentifierOrPlaceholder identifier;
  TypeLiteral addressType;
  TypeLiteral dataType;
  SourceLocation loc;

  public MemoryDefinition(IdentifierOrPlaceholder identifier, TypeLiteral addressType,
                          TypeLiteral dataType, SourceLocation loc) {
    this.identifier = identifier;
    this.addressType = addressType;
    this.dataType = dataType;
    this.loc = loc;
  }

  Identifier identifier() {
    return (Identifier) identifier;
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.ISA_DEFS;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    annotations.prettyPrint(indent, builder);
    builder.append(prettyIndentString(indent));
    builder.append("memory ");
    identifier.prettyPrint(indent, builder);
    builder.append(": ");
    addressType.prettyPrint(indent, builder);
    builder.append(" -> ");
    dataType.prettyPrint(indent, builder);
    builder.append("\n");
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    MemoryDefinition that = (MemoryDefinition) o;
    return annotations.equals(that.annotations)
        && identifier.equals(that.identifier)
        && addressType.equals(that.addressType)
        && dataType.equals(that.dataType);
  }

  @Override
  public int hashCode() {
    int result = annotations.hashCode();
    result = 31 * result + identifier.hashCode();
    result = 31 * result + addressType.hashCode();
    result = 31 * result + dataType.hashCode();
    return result;
  }
}

class RegisterDefinition extends Definition {
  IdentifierOrPlaceholder identifier;
  TypeLiteral type;
  SourceLocation loc;

  public RegisterDefinition(IdentifierOrPlaceholder identifier, TypeLiteral type,
                            SourceLocation location) {
    this.identifier = identifier;
    this.type = type;
    this.loc = location;
  }

  Identifier identifier() {
    return (Identifier) identifier;
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.ISA_DEFS;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    annotations.prettyPrint(indent, builder);
    builder.append(prettyIndentString(indent));
    builder.append("register ");
    identifier.prettyPrint(indent, builder);
    builder.append(": ");
    type.prettyPrint(indent, builder);
    builder.append("\n");
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    RegisterDefinition that = (RegisterDefinition) o;
    return annotations.equals(that.annotations)
        && identifier.equals(that.identifier)
        && type.equals(that.type);
  }

  @Override
  public int hashCode() {
    int result = annotations.hashCode();
    result = 31 * result + identifier.hashCode();
    result = 31 * result + type.hashCode();
    return result;
  }
}

class RegisterFileDefinition extends Definition {
  IdentifierOrPlaceholder identifier;
  TypeLiteral indexType;
  TypeLiteral registerType;
  SourceLocation loc;

  public RegisterFileDefinition(IdentifierOrPlaceholder identifier, TypeLiteral indexType,
                                TypeLiteral registerType, SourceLocation location) {
    this.identifier = identifier;
    this.indexType = indexType;
    this.registerType = registerType;
    this.loc = location;
  }

  Identifier identifier() {
    return (Identifier) identifier;
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.ISA_DEFS;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    annotations.prettyPrint(indent, builder);
    builder.append(prettyIndentString(indent));
    builder.append("register file ");
    identifier.prettyPrint(indent, builder);
    builder.append(": ");
    indexType.prettyPrint(indent, builder);
    builder.append(" -> ");
    registerType.prettyPrint(indent, builder);
    builder.append("\n");
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    RegisterFileDefinition that = (RegisterFileDefinition) o;
    return annotations.equals(that.annotations)
        && identifier.equals(that.identifier)
        && indexType.equals(that.indexType)
        && registerType.equals(that.registerType);
  }

  @Override
  public int hashCode() {
    int result = annotations.hashCode();
    result = 31 * result + identifier.hashCode();
    result = 31 * result + indexType.hashCode();
    result = 31 * result + registerType.hashCode();
    return result;
  }
}

class InstructionDefinition extends Definition {
  IdentifierOrPlaceholder identifier;
  IdentifierOrPlaceholder typeIdentifier;
  Statement behavior;
  final SourceLocation loc;

  InstructionDefinition(IdentifierOrPlaceholder identifier, IdentifierOrPlaceholder typeIdentifier,
                        Statement behavior, SourceLocation location) {
    this.identifier = identifier;
    this.typeIdentifier = typeIdentifier;
    this.behavior = behavior;
    this.loc = location;
  }

  Identifier id() {
    return (Identifier) identifier;
  }

  Identifier type() {
    return (Identifier) typeIdentifier;
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.ISA_DEFS;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    annotations.prettyPrint(indent, builder);
    builder.append(prettyIndentString(indent));
    builder.append("instruction ");
    identifier.prettyPrint(indent, builder);
    builder.append(" : ");
    typeIdentifier.prettyPrint(indent, builder);
    builder.append(" = ");
    behavior.prettyPrint(indent, builder);
    builder.append("\n");
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    var that = (InstructionDefinition) o;
    return Objects.equals(annotations, that.annotations)
        && Objects.equals(identifier, that.identifier)
        && Objects.equals(typeIdentifier, that.typeIdentifier)
        && Objects.equals(behavior, that.behavior);
  }

  @Override
  public int hashCode() {
    int result = Objects.hashCode(annotations);
    result = 31 * result + Objects.hashCode(identifier);
    result = 31 * result + Objects.hashCode(typeIdentifier);
    result = 31 * result + Objects.hashCode(behavior);
    return result;
  }
}

class PseudoInstructionDefinition extends Definition {
  IdentifierOrPlaceholder identifier;
  PseudoInstrKind kind;
  List<Param> params;
  List<InstructionCallStatement> statements;
  SourceLocation loc;

  PseudoInstructionDefinition(IdentifierOrPlaceholder identifier, PseudoInstrKind kind,
                              List<Param> params, List<InstructionCallStatement> statements,
                              SourceLocation loc) {
    this.identifier = identifier;
    this.kind = kind;
    this.params = params;
    this.statements = statements;
    this.loc = loc;
  }

  Identifier id() {
    return (Identifier) identifier;
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.ISA_DEFS;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    annotations.prettyPrint(indent, builder);
    builder.append(prettyIndentString(indent));
    var kindStr = switch (kind) {
      case PSEUDO -> "pseudo";
      case COMPILER -> "compiler";
    };
    builder.append(kindStr).append(" instruction ");
    identifier.prettyPrint(indent, builder);
    if (!params.isEmpty()) {
      builder.append("(");
      var isFirst = true;
      for (var param : params) {
        if (!isFirst) {
          builder.append(", ");
        }
        isFirst = false;
        param.id.prettyPrint(0, builder);
        builder.append(" : ");
        param.type.prettyPrint(0, builder);
      }
      builder.append(")");
    }
    builder.append(" = {\n");
    for (InstructionCallStatement statement : statements) {
      statement.prettyPrint(indent + 1, builder);
    }
    builder.append(prettyIndentString(indent)).append("}\n");
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    var that = (PseudoInstructionDefinition) o;
    return Objects.equals(annotations, that.annotations)
        && Objects.equals(identifier, that.identifier)
        && Objects.equals(kind, that.kind)
        && Objects.equals(params, that.params)
        && Objects.equals(statements, that.statements);
  }

  @Override
  public int hashCode() {
    int result = Objects.hashCode(annotations);
    result = 31 * result + Objects.hashCode(identifier);
    result = 31 * result + Objects.hashCode(kind);
    result = 31 * result + Objects.hashCode(params);
    result = 31 * result + Objects.hashCode(statements);
    return result;
  }

  enum PseudoInstrKind {
    PSEUDO, COMPILER
  }

  record Param(Identifier id, TypeLiteral type) {
  }
}

sealed interface FieldEncodingOrPlaceholder
    permits EncodingDefinition.FieldEncoding, PlaceholderNode, MacroInstanceExpr, MacroMatchExpr {
  SourceLocation location();

  void prettyPrint(int indent, StringBuilder builder);
}

class EncodingDefinition extends Definition {
  IdentifierOrPlaceholder instrIdentifier;
  FieldEncodings fieldEncodings;
  SourceLocation loc;

  EncodingDefinition(IdentifierOrPlaceholder instrIdentifier, FieldEncodings fieldEncodings,
                     SourceLocation location) {
    this.instrIdentifier = instrIdentifier;
    this.fieldEncodings = fieldEncodings;
    this.loc = location;
  }

  Identifier instrId() {
    return (Identifier) instrIdentifier;
  }

  FieldEncodings fieldEncodings() {
    return fieldEncodings;
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.ISA_DEFS;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    annotations.prettyPrint(indent, builder);
    builder.append(prettyIndentString(indent));
    builder.append("encoding ");
    instrIdentifier.prettyPrint(0, builder);
    builder.append(" =\n");
    builder.append(prettyIndentString(indent)).append("{ ");
    fieldEncodings().prettyPrint(indent, builder);
    builder.append(prettyIndentString(indent)).append("}\n");
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    var that = (EncodingDefinition) o;
    return Objects.equals(annotations, that.annotations)
        && Objects.equals(instrIdentifier, that.instrIdentifier)
        && Objects.equals(fieldEncodings, that.fieldEncodings);
  }

  @Override
  public int hashCode() {
    int result = Objects.hashCode(annotations);
    result = 31 * result + Objects.hashCode(instrIdentifier);
    result = 31 * result + Objects.hashCode(fieldEncodings);
    return result;
  }

  static final class FieldEncodings extends Node {
    List<FieldEncodingOrPlaceholder> encodings;

    FieldEncodings(List<FieldEncodingOrPlaceholder> encodings) {
      this.encodings = encodings;
    }

    @Override
    SourceLocation location() {
      return encodings.get(0).location()
          .join(encodings.get(encodings.size() - 1).location());
    }

    @Override
    SyntaxType syntaxType() {
      return BasicSyntaxType.ENCS;
    }

    @Override
    void prettyPrint(int indent, StringBuilder builder) {
      boolean first = true;
      for (var entry : encodings) {
        if (!first) {
          builder.append(prettyIndentString(indent)).append(", ");
        }
        entry.prettyPrint(0, builder);
        builder.append("\n");
        first = false;
      }
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      FieldEncodings that = (FieldEncodings) o;
      return Objects.equals(encodings, that.encodings);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(encodings);
    }
  }

  record FieldEncoding(Identifier field, Expr value) implements FieldEncodingOrPlaceholder {
    @Override
    public SourceLocation location() {
      return field.location().join(value.location());
    }

    @Override
    public void prettyPrint(int indent, StringBuilder builder) {
      field.prettyPrint(0, builder);
      builder.append(" = ");
      value.prettyPrint(0, builder);
    }
  }
}

class AssemblyDefinition extends Definition {
  List<IdentifierOrPlaceholder> identifiers;
  Expr expr;
  SourceLocation loc;

  AssemblyDefinition(List<IdentifierOrPlaceholder> identifiers, Expr expr,
                     SourceLocation location) {
    this.identifiers = identifiers;
    this.expr = expr;
    this.loc = location;
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.ISA_DEFS;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    annotations.prettyPrint(indent, builder);
    builder.append(prettyIndentString(indent));
    builder.append("assembly ");
    var isFirst = true;
    for (IdentifierOrPlaceholder identifier : identifiers) {
      if (!isFirst) {
        builder.append(", ");
      }
      isFirst = false;
      identifier.prettyPrint(0, builder);
    }
    builder.append(" = ");
    expr.prettyPrint(indent, builder);
    builder.append("\n");
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    var that = (AssemblyDefinition) o;
    return Objects.equals(annotations, that.annotations)
        && Objects.equals(identifiers, that.identifiers)
        && Objects.equals(expr, that.expr);
  }

  @Override
  public int hashCode() {
    int result = Objects.hashCode(annotations);
    result = 31 * result + Objects.hashCode(identifiers);
    result = 31 * result + Objects.hashCode(expr);
    return result;
  }
}

class UsingDefinition extends Definition {
  final IdentifierOrPlaceholder id;
  final TypeLiteral type;
  final SourceLocation loc;

  UsingDefinition(IdentifierOrPlaceholder id, TypeLiteral type, SourceLocation location) {
    this.id = id;
    this.type = type;
    this.loc = location;
  }

  Identifier identifier() {
    return (Identifier) id;
  }


  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.ISA_DEFS;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    builder.append(prettyIndentString(indent));
    builder.append("using ");
    id.prettyPrint(indent, builder);
    builder.append(" = ");
    type.prettyPrint(indent, builder);
    builder.append("\n");
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    var that = (UsingDefinition) o;
    return Objects.equals(annotations, that.annotations)
        && Objects.equals(id, that.id)
        && Objects.equals(type, that.type);
  }

  @Override
  public int hashCode() {
    int result = Objects.hashCode(annotations);
    result = 31 * result + Objects.hashCode(id);
    result = 31 * result + Objects.hashCode(type);
    return result;
  }
}

class FunctionDefinition extends Definition {
  IdentifierOrPlaceholder name;
  List<Parameter> params;
  TypeLiteral retType;
  Expr expr;
  SourceLocation loc;

  FunctionDefinition(IdentifierOrPlaceholder name, List<Parameter> params, TypeLiteral retType,
                     Expr expr, SourceLocation location) {
    this.name = name;
    this.params = params;
    this.retType = retType;
    this.expr = expr;
    this.loc = location;
  }

  Identifier name() {
    return (Identifier) name;
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.ISA_DEFS;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    builder.append(prettyIndentString(indent));
    builder.append("function ");
    name.prettyPrint(indent, builder);
    if (!params.isEmpty()) {
      builder.append("(");
      var isFirst = true;
      for (var param : params) {
        if (!isFirst) {
          builder.append(", ");
        }
        isFirst = false;
        param.name.prettyPrint(0, builder);
        builder.append(" : ");
        param.type.prettyPrint(0, builder);
      }
      builder.append(")");
    }
    builder.append(" -> ");
    retType.prettyPrint(0, builder);
    if (!isBlockLayout(expr)) {
      builder.append(" = ");
    } else {
      builder.append(" =\n");
    }
    expr.prettyPrint(indent + 1, builder);
    builder.append("\n");
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    var that = (FunctionDefinition) o;
    return Objects.equals(annotations, that.annotations)
        && Objects.equals(name, that.name)
        && Objects.equals(params, that.params)
        && Objects.equals(retType, that.retType)
        && Objects.equals(expr, that.expr);
  }

  @Override
  public int hashCode() {
    int result = Objects.hashCode(annotations);
    result = 31 * result + Objects.hashCode(name);
    result = 31 * result + Objects.hashCode(params);
    result = 31 * result + Objects.hashCode(retType);
    result = 31 * result + Objects.hashCode(expr);
    return result;
  }

  record Parameter(Identifier name, TypeLiteral type) {
  }
}

class AliasDefinition extends Definition {
  IdentifierOrPlaceholder id;
  AliasKind kind;
  @Nullable
  TypeLiteral aliasType;
  @Nullable
  TypeLiteral targetType;
  Expr value;
  SourceLocation loc;

  AliasDefinition(IdentifierOrPlaceholder id, AliasKind kind,
                  @Nullable TypeLiteral aliasType, @Nullable TypeLiteral targetType, Expr value,
                  SourceLocation location) {
    this.id = id;
    this.kind = kind;
    this.aliasType = aliasType;
    this.targetType = targetType;
    this.value = value;
    this.loc = location;
  }

  Identifier id() {
    return (Identifier) id;
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.ISA_DEFS;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    builder.append(prettyIndentString(indent)).append("alias ");
    switch (kind) {
      case REGISTER -> builder.append("register ");
      case REGISTER_FILE -> builder.append("register file ");
      case PROGRAM_COUNTER -> builder.append("program counter ");
      default -> {
      }
    }
    id.prettyPrint(0, builder);
    if (aliasType != null) {
      builder.append(" : ");
      aliasType.prettyPrint(0, builder);
      if (targetType != null) {
        builder.append(" -> ");
        targetType.prettyPrint(0, builder);
      }
    }
    builder.append(" = ");
    value.prettyPrint(0, builder);
    builder.append("\n");
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    var that = (AliasDefinition) o;
    return Objects.equals(annotations, that.annotations)
        && Objects.equals(id, that.id)
        && Objects.equals(kind, that.kind)
        && Objects.equals(aliasType, that.aliasType)
        && Objects.equals(targetType, that.targetType)
        && Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    int result = Objects.hashCode(annotations);
    result = 31 * result + Objects.hashCode(id);
    result = 31 * result + Objects.hashCode(kind);
    result = 31 * result + Objects.hashCode(aliasType);
    result = 31 * result + Objects.hashCode(targetType);
    result = 31 * result + Objects.hashCode(value);
    return result;
  }

  enum AliasKind {
    REGISTER, REGISTER_FILE, PROGRAM_COUNTER
  }
}

final class EnumerationDefinition extends Definition {
  IdentifierOrPlaceholder id;
  @Nullable
  TypeLiteral enumType;
  List<Entry> entries;
  SourceLocation loc;

  EnumerationDefinition(IdentifierOrPlaceholder id, @Nullable TypeLiteral enumType,
                        List<Entry> entries, SourceLocation location) {
    this.id = id;
    this.enumType = enumType;
    this.entries = entries;
    this.loc = location;
  }

  Identifier id() {
    return (Identifier) id;
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.ISA_DEFS;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    builder.append(prettyIndentString(indent)).append("enumeration ");
    id.prettyPrint(0, builder);
    if (enumType != null) {
      builder.append(" : ");
      enumType.prettyPrint(0, builder);
    }
    builder.append(" =\n");
    builder.append(prettyIndentString(indent + 1)).append("{ ");
    var isFirst = true;
    for (var entry : entries) {
      if (!isFirst) {
        builder.append(prettyIndentString(indent + 1)).append(", ");
      }
      isFirst = false;
      entry.name.prettyPrint(0, builder);
      if (entry.value != null) {
        builder.append(" = ");
        entry.value.prettyPrint(0, builder);
      }
      if (entry.behavior != null) {
        builder.append(" => ");
        entry.behavior.prettyPrint(0, builder);
      }
      builder.append("\n");
    }
    builder.append(prettyIndentString(indent + 1)).append("}\n");
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    var that = (EnumerationDefinition) o;
    return Objects.equals(annotations, that.annotations)
        && Objects.equals(id, that.id)
        && Objects.equals(enumType, that.enumType)
        && Objects.equals(entries, that.entries);
  }

  @Override
  public int hashCode() {
    int result = Objects.hashCode(annotations);
    result = 31 * result + Objects.hashCode(id);
    result = 31 * result + Objects.hashCode(enumType);
    result = 31 * result + Objects.hashCode(entries);
    return result;
  }

  record Entry(Identifier name, @Nullable Expr value, @Nullable Expr behavior) {
  }
}

final class ExceptionDefinition extends Definition {
  IdentifierOrPlaceholder id;
  Statement statement;
  SourceLocation loc;

  ExceptionDefinition(IdentifierOrPlaceholder id, Statement statement, SourceLocation location) {
    this.id = id;
    this.statement = statement;
    this.loc = location;
  }

  Identifier id() {
    return (Identifier) id;
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.ISA_DEFS;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    builder.append(prettyIndentString(indent)).append("exception ");
    id.prettyPrint(0, builder);
    builder.append(" = ");
    statement.prettyPrint(indent + 1, builder);
    builder.append("\n");
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    var that = (ExceptionDefinition) o;
    return Objects.equals(annotations, that.annotations)
        && Objects.equals(id, that.id)
        && Objects.equals(statement, that.statement);
  }

  @Override
  public int hashCode() {
    int result = Objects.hashCode(annotations);
    result = 31 * result + Objects.hashCode(id);
    result = 31 * result + Objects.hashCode(statement);
    return result;
  }
}

final class PlaceholderDefinition extends Definition {

  List<String> segments;
  SyntaxType type;
  SourceLocation loc;

  PlaceholderDefinition(List<String> segments, SyntaxType type, SourceLocation loc) {
    this.segments = segments;
    this.type = type;
    this.loc = loc;
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return type;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    builder.append("$");
    builder.append(String.join(".", segments));
    builder.append("\n");
  }
}

/**
 * An internal temporary placeholder of macro instantiations.
 * This node should never leave the parser.
 */
final class MacroInstanceDefinition extends Definition {
  MacroOrPlaceholder macro;
  List<Node> arguments;
  SourceLocation loc;

  public MacroInstanceDefinition(MacroOrPlaceholder macro, List<Node> arguments,
                                 SourceLocation loc) {
    this.macro = macro;
    this.arguments = arguments;
    this.loc = loc;
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return macro.returnType();
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    builder.append(prettyIndentString(indent));
    builder.append("$");
    if (macro instanceof Macro m) {
      builder.append(m.name().name);
    } else if (macro instanceof MacroPlaceholder mp) {
      builder.append(String.join(".", mp.segments()));
    }
    builder.append("(");
    var isFirst = true;
    for (var arg : arguments) {
      if (!isFirst) {
        builder.append(" ; ");
      }
      isFirst = false;
      arg.prettyPrint(0, builder);
    }
    builder.append(")\n");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    MacroInstanceDefinition that = (MacroInstanceDefinition) o;
    return macro.equals(that.macro)
        && arguments.equals(that.arguments);
  }

  @Override
  public int hashCode() {
    int result = macro.hashCode();
    result = 31 * result + arguments.hashCode();
    return result;
  }
}

/**
 * An internal temporary placeholder of a macro-level "match" construct.
 * This node should never leave the parser.
 */
final class MacroMatchDefinition extends Definition {
  MacroMatch macroMatch;

  MacroMatchDefinition(MacroMatch macroMatch) {
    this.macroMatch = macroMatch;
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  public SourceLocation location() {
    return macroMatch.sourceLocation();
  }

  @Override
  SyntaxType syntaxType() {
    return macroMatch.resultType();
  }

  @Override
  public void prettyPrint(int indent, StringBuilder builder) {
    macroMatch.prettyPrint(indent, builder);
    builder.append("\n");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    MacroMatchDefinition that = (MacroMatchDefinition) o;
    return macroMatch.equals(that.macroMatch);
  }

  @Override
  public int hashCode() {
    return macroMatch.hashCode();
  }
}

record Annotations(List<Annotation> annotations) {
  Annotations() {
    this(new ArrayList<>());
  }

  void prettyPrint(int indent, StringBuilder builder) {
    annotations.forEach(annotation -> annotation.prettyPrint(indent, builder));
  }
}

record Annotation(Expr expr, @Nullable TypeLiteral type, @Nullable Identifier property) {
  void prettyPrint(int indent, StringBuilder builder) {
    builder.append(Node.prettyIndentString(indent));
    builder.append('[');
    expr.prettyPrint(indent, builder);
    if (type != null) {
      builder.append(" : ");
      type.prettyPrint(indent, builder);
    }
    if (property != null) {
      builder.append(' ');
      property.prettyPrint(indent, builder);
    }
    builder.append(" ]\n");
  }
}

class DefinitionList extends Definition {

  List<Definition> items;
  SourceLocation location;

  DefinitionList(List<Definition> items, SourceLocation location) {
    this.items = items;
    this.location = location;
  }

  @Override
  SourceLocation location() {
    return location;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.ISA_DEFS;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    items.forEach(item -> item.prettyPrint(indent, builder));
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }
}

/**
 * An internal temporary definition of a model.
 * This node should never leave the parser.
 */
final class ModelDefinition extends Definition {
  /**
   * Either a concrete identifier for this model or, if it is a model-in-model, a placeholder ID.
   */
  IdentifierOrPlaceholder id;

  /**
   * The list of formal parameters for the represented macro.
   */
  List<MacroParam> params;

  /**
   * The actual macro body to be templated.
   */
  Node body;

  /**
   * A macro return type - should only be a {@link BasicSyntaxType}.
   */
  SyntaxType returnType;

  /**
   * In a model-in-model situation, the parent model's arguments can be referenced in the inner
   * model. To preserve their value during macro expansion, the bound arguments field is used.
   *
   * @see MacroExpander#visit(ModelDefinition)
   * @see MacroExpander#collectMacroParameters(Macro, List, SourceLocation)
   */
  Map<String, Node> boundArguments = new HashMap<>();
  SourceLocation loc;

  ModelDefinition(IdentifierOrPlaceholder id, List<MacroParam> params, Node body,
                  SyntaxType returnType, SourceLocation loc) {
    this.id = id;
    this.params = params;
    this.body = body;
    this.returnType = returnType;
    this.loc = loc;
  }

  Macro toMacro() {
    return new Macro(new Identifier(id.pathToString(), id.location()), params, body, returnType,
        boundArguments);
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.ISA_DEFS;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    builder.append(prettyIndentString(indent)).append("model ");
    id.prettyPrint(0, builder);
    builder.append("(");
    var isFirst = true;
    for (var param : params) {
      if (!isFirst) {
        builder.append(", ");
      }
      isFirst = false;
      param.name().prettyPrint(0, builder);
      builder.append(" : ").append(param.type().print());
    }
    builder.append(") : ");
    builder.append(returnType.print());
    builder.append(" = {\n");
    body.prettyPrint(indent + 1, builder);
    builder.append(prettyIndentString(indent)).append("}\n");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ModelDefinition that = (ModelDefinition) o;
    return Objects.equals(id, that.id) && Objects.equals(params, that.params)
        && Objects.equals(body, that.body)
        && Objects.equals(returnType, that.returnType)
        && Objects.equals(boundArguments, that.boundArguments);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, params, body, returnType, boundArguments);
  }
}

/**
 * An internal temporary placeholder of record type.
 * This node should never leave the parser.
 */
final class RecordTypeDefinition extends Definition {
  RecordType recordType;
  SourceLocation loc;

  RecordTypeDefinition(RecordType recordType, SourceLocation loc) {
    this.recordType = recordType;
    this.loc = loc;
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.ISA_DEFS;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    builder.append(prettyIndentString(indent));
    builder.append("record ");
    builder.append(recordType.print());
    builder.append(recordType.entries.stream()
        .map(entry -> entry.name() + " : " + entry.type().print())
        .collect(Collectors.joining(",", "(", ")")));
    builder.append("\n");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RecordTypeDefinition that = (RecordTypeDefinition) o;
    return Objects.equals(recordType, that.recordType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(recordType);
  }
}
