package vadl.ast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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

  R visit(EncodingDefinition definition);

  R visit(AssemblyDefinition definition);

  R visit(UsingDefinition definition);

  R visit(FunctionDefinition definition);
}

class ConstantDefinition extends Definition {
  final IdentifierOrPlaceholder identifier;

  @Nullable
  final TypeLiteral type;

  final Expr value;
  final SourceLocation loc;

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
    return BasicSyntaxType.IsaDefs();
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
  SourceLocation loc;

  interface FormatField {
    Identifier identifier();

    void prettyPrint(int indent, StringBuilder builder);
  }

  static class RangeFormatField extends Node implements FormatField {
    Identifier identifier;
    List<RangeExpr> ranges;

    public RangeFormatField(Identifier identifier, List<RangeExpr> ranges) {
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
      return BasicSyntaxType.Invalid();
    }

    @Override
    public void prettyPrint(int indent, StringBuilder builder) {
      identifier.prettyPrint(indent, builder);
      builder.append("\t [");
      ranges.get(0).prettyPrint(indent, builder);
      for (int i = 1; i < ranges.size(); i++) {
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
    final TypeLiteral type;
    final NestedSymbolTable symbolTable;

    public TypedFormatField(Identifier identifier, TypeLiteral type,
                            NestedSymbolTable symbolTable) {
      this.identifier = identifier;
      this.type = type;
      this.symbolTable = symbolTable;
    }

    @Override
    public Identifier identifier() {
      return identifier;
    }

    @Override
    SourceLocation location() {
      return identifier.location().join(type.location());
    }

    @Override
    SyntaxType syntaxType() {
      return BasicSyntaxType.Invalid();
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
      return BasicSyntaxType.Invalid();
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

  public FormatDefinition(IdentifierOrPlaceholder identifier, TypeLiteral type,
                          List<FormatField> fields, SourceLocation location) {
    this.identifier = identifier;
    this.type = type;
    this.fields = fields;
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
    return BasicSyntaxType.IsaDefs();
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
        && fields.equals(that.fields);
  }

  @Override
  public int hashCode() {
    int result = annotations.hashCode();
    result = 31 * result + identifier.hashCode();
    result = 31 * result + type.hashCode();
    result = 31 * result + fields.hashCode();
    return result;
  }
}

class InstructionSetDefinition extends Definition {
  final Identifier identifier;
  final SourceLocation loc;
  List<Definition> definitions;

  InstructionSetDefinition(Identifier identifier, List<Definition> statements,
                           SourceLocation location) {
    this.identifier = identifier;
    this.definitions = statements;
    this.loc = location;
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.Invalid();
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    annotations.prettyPrint(indent, builder);
    builder.append(prettyIndentString(indent));
    builder.append("instruction set architecture %s = {\n".formatted(identifier.name));
    for (Definition definition : definitions) {
      definition.prettyPrint(indent + 1, builder);
    }
    builder.append("}\n\n");
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
        && Objects.equals(definitions, that.definitions);
  }

  @Override
  public int hashCode() {
    int result = Objects.hashCode(annotations);
    result = 31 * result + Objects.hashCode(identifier);
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
    return BasicSyntaxType.IsaDefs();
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
    return BasicSyntaxType.IsaDefs();
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
    return BasicSyntaxType.IsaDefs();
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
    return BasicSyntaxType.IsaDefs();
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
    return BasicSyntaxType.IsaDefs();
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

class EncodingDefinition extends Definition {
  final IdentifierOrPlaceholder instrIdentifier;
  final List<FieldEncoding> fieldEncodings;
  final SourceLocation loc;

  EncodingDefinition(IdentifierOrPlaceholder instrIdentifier, List<FieldEncoding> fieldEncodings,
                     SourceLocation location) {
    this.instrIdentifier = instrIdentifier;
    this.fieldEncodings = fieldEncodings;
    this.loc = location;
  }

  Identifier instrId() {
    return (Identifier) instrIdentifier;
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.IsaDefs();
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    annotations.prettyPrint(indent, builder);
    builder.append(prettyIndentString(indent));
    builder.append("encoding %s =\n".formatted(instrId().name));
    builder.append(prettyIndentString(indent)).append("{ ");
    boolean first = true;
    for (FieldEncoding entry : fieldEncodings) {
      if (!first) {
        builder.append(prettyIndentString(indent)).append(", ");
      }
      entry.field.prettyPrint(0, builder);
      builder.append(" = ");
      entry.value.prettyPrint(0, builder);
      builder.append("\n");
      first = false;
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

  record FieldEncoding(Identifier field, Expr value) {
  }
}

class AssemblyDefinition extends Definition {
  final List<IdentifierOrPlaceholder> identifiers;
  final boolean isMnemonic;
  final List<Expr> segments;
  final SourceLocation loc;

  AssemblyDefinition(List<IdentifierOrPlaceholder> identifiers, boolean isMnemonic,
                     List<Expr> segments, SourceLocation location) {
    this.identifiers = identifiers;
    this.isMnemonic = isMnemonic;
    this.segments = segments;
    this.loc = location;
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.IsaDefs();
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    annotations.prettyPrint(indent, builder);
    builder.append(prettyIndentString(indent));
    builder.append("assembly ");
    builder.append(identifiers.stream().map(id -> ((Identifier) id).name)
        .collect(Collectors.joining(", ")));
    builder.append(" = (");
    if (isMnemonic) {
      builder.append("mnemonic");
    }
    var isFirst = !isMnemonic;
    for (Node node : segments) {
      if (!isFirst) {
        builder.append(", ");
      }
      node.prettyPrint(indent + 1, builder);
      isFirst = false;
    }
    builder.append(")\n");
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
        && isMnemonic == that.isMnemonic
        && Objects.equals(segments, that.segments);
  }

  @Override
  public int hashCode() {
    int result = Objects.hashCode(annotations);
    result = 31 * result + Objects.hashCode(identifiers);
    result = 31 * result + Boolean.hashCode(isMnemonic);
    result = 31 * result + Objects.hashCode(segments);
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
    return BasicSyntaxType.IsaDefs();
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
    return BasicSyntaxType.IsaDefs();
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
    builder.append(" = ");
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
    builder.append(" ]");
  }
}

class DefinitionList extends Node {

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
    return BasicSyntaxType.IsaDefs();
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    items.forEach(item -> item.prettyPrint(indent, builder));
  }
}
