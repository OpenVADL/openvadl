package vadl.ast;

import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import vadl.utils.SourceLocation;

/**
 * The Definition nodes inside the AST.
 * A definition defines part of the architecture, but has no sideeffects and doesn't evaulate to
 * anything.
 */
abstract class Definition extends Node {
  abstract <R> R accept(DefinitionVisitor<R> visitor);
}

interface DefinitionVisitor<R> {
  R visit(ConstantDefinition definition);

  R visit(FormatDefinition definition);

  R visit(InstructionSetDefinition definition);

  R visit(IndexDefinition definition);

  R visit(MemoryDefinition definition);

  R visit(RegisterDefinition definition);

  R visit(RegisterFileDefinition definition);
}

class ConstantDefinition extends Definition {
  final Identifier identifier;

  @Nullable
  final TypeLiteral typeAnnotation;

  final Expr value;
  final SourceLocation loc;

  ConstantDefinition(Identifier identifier, @Nullable TypeLiteral typeAnnotation, Expr value,
                     SourceLocation location) {
    this.identifier = identifier;
    this.typeAnnotation = typeAnnotation;
    this.value = value;
    this.loc = location;
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  void dump(int indent, StringBuilder builder) {
    builder.append(dumpIndentString(indent));
    builder.append(this.getClass().getSimpleName());
    builder.append("\n");
    if (typeAnnotation != null) {
      typeAnnotation.dump(indent + 1, builder);
    }
    identifier.dump(indent + 1, builder);
    value.dump(indent + 1, builder);
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    builder.append(prettyIndentString(indent));
    builder.append("constant %s".formatted(identifier.name));
    if (typeAnnotation != null) {
      builder.append(": ");
      typeAnnotation.prettyPrint(indent, builder);
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
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ConstantDefinition that = (ConstantDefinition) o;
    return Objects.equals(identifier, that.identifier)
        && Objects.equals(typeAnnotation, that.typeAnnotation)
        && Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    int result = Objects.hashCode(identifier);
    result = 31 * result + Objects.hashCode(typeAnnotation);
    result = 31 * result + Objects.hashCode(value);
    return result;
  }
}

class FormatDefinition extends Definition {
  Identifier identifier;
  TypeLiteral typeAnnotation;
  List<FormatField> fields;
  SourceLocation loc;

  static class FormatField extends Node {
    Identifier identifier;
    List<RangeExpr> ranges;

    public FormatField(Identifier identifier, List<RangeExpr> ranges) {
      this.identifier = identifier;
      this.ranges = ranges;
    }

    @Override
    SourceLocation location() {
      return identifier.location().join(ranges.get(ranges.size() - 1).location());
    }

    @Override
    void dump(int indent, StringBuilder builder) {
      builder.append(dumpIndentString(indent));
      builder.append(this.getClass().getSimpleName());
      builder.append("\n");
      identifier.dump(indent + 1, builder);
      for (RangeExpr r : ranges) {
        r.dump(indent + 1, builder);
      }
    }

    @Override
    void prettyPrint(int indent, StringBuilder builder) {
      identifier.prettyPrint(indent, builder);
      builder.append("\t [");
      ranges.get(0).prettyPrint(indent, builder);
      for (int i = 1; i < ranges.size(); i++) {
        ranges.get(i).prettyPrint(indent, builder);
      }
      builder.append("]");
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      FormatField that = (FormatField) o;
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

  public FormatDefinition(Identifier identifier, TypeLiteral typeAnnotation,
                          List<FormatField> fields, SourceLocation location) {
    this.identifier = identifier;
    this.typeAnnotation = typeAnnotation;
    this.fields = fields;
    this.loc = location;
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  void dump(int indent, StringBuilder builder) {
    builder.append(dumpIndentString(indent));
    builder.append(this.getClass().getSimpleName());
    builder.append("\n");
    identifier.dump(indent + 1, builder);
    typeAnnotation.dump(indent + 1, builder);
    for (var field : fields) {
      field.dump(indent + 1, builder);
    }
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    builder.append(prettyIndentString(indent));
    builder.append("format ");
    identifier.prettyPrint(indent, builder);
    builder.append(": ");
    typeAnnotation.prettyPrint(indent, builder);

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
    builder.append("}");
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
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
    return identifier.equals(that.identifier) && typeAnnotation.equals(that.typeAnnotation)
        && fields.equals(that.fields);
  }

  @Override
  public int hashCode() {
    int result = identifier.hashCode();
    result = 31 * result + typeAnnotation.hashCode();
    result = 31 * result + fields.hashCode();
    return result;
  }
}

class InstructionSetDefinition extends Definition {
  final Identifier identifier;
  final SourceLocation loc;
  List<Definition> statements;

  InstructionSetDefinition(Identifier identifier, List<Definition> statements,
                           SourceLocation location) {
    this.identifier = identifier;
    this.statements = statements;
    this.loc = location;
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  void dump(int indent, StringBuilder builder) {
    builder.append(dumpIndentString(indent));
    builder.append("%s \"%s\"\n".formatted(this.getClass().getSimpleName(), identifier.name));

    for (Definition definition : statements) {
      definition.dump(indent + 1, builder);
    }
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    builder.append(dumpIndentString(indent));
    builder.append("instruction set architecture %s = {\n".formatted(identifier.name));
    for (Definition definition : statements) {
      definition.prettyPrint(indent + 1, builder);
    }
    builder.append("}\n\n");
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
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
    return Objects.equals(identifier, that.identifier)
        && Objects.equals(statements, that.statements);
  }

  @Override
  public int hashCode() {
    int result = Objects.hashCode(identifier);
    result = 31 * result + Objects.hashCode(statements);
    return result;
  }
}

class IndexDefinition extends Definition {
  IndexKind kind;
  Identifier identifier;
  TypeLiteral type;
  SourceLocation loc;

  enum IndexKind {
    PROGRAM,
    GROUP
  }

  public IndexDefinition(IndexKind kind, Identifier identifier, TypeLiteral type,
                         SourceLocation location) {
    this.kind = kind;
    this.identifier = identifier;
    this.type = type;
    this.loc = location;
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  void dump(int indent, StringBuilder builder) {
    builder.append(dumpIndentString(indent));
    builder.append("%s (kind: %s)\n".formatted(this.getClass().getSimpleName(), kind.toString()));
    identifier.dump(indent + 1, builder);
    type.dump(indent + 1, builder);
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    builder.append(prettyIndentString(indent));
    builder.append("%s counter ".formatted(kind.toString().toLowerCase()));
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
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    IndexDefinition that = (IndexDefinition) o;
    return kind == that.kind && identifier.equals(that.identifier) && type.equals(that.type);
  }

  @Override
  public int hashCode() {
    int result = kind.hashCode();
    result = 31 * result + identifier.hashCode();
    result = 31 * result + type.hashCode();
    return result;
  }
}

class MemoryDefinition extends Definition {
  Identifier identifier;
  TypeLiteral addressType;
  TypeLiteral dataType;
  SourceLocation loc;

  public MemoryDefinition(Identifier identifier, TypeLiteral addressType, TypeLiteral dataType,
                          SourceLocation loc) {
    this.identifier = identifier;
    this.addressType = addressType;
    this.dataType = dataType;
    this.loc = loc;
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  void dump(int indent, StringBuilder builder) {
    builder.append(dumpIndentString(indent));
    builder.append(this.getClass().getSimpleName());
    builder.append("\n");
    addressType.dump(indent + 1, builder);
    dataType.dump(indent + 1, builder);
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
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
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    MemoryDefinition that = (MemoryDefinition) o;
    return identifier.equals(that.identifier) && addressType.equals(that.addressType)
        && dataType.equals(that.dataType);
  }

  @Override
  public int hashCode() {
    int result = identifier.hashCode();
    result = 31 * result + addressType.hashCode();
    result = 31 * result + dataType.hashCode();
    return result;
  }
}

class RegisterDefinition extends Definition {
  Identifier identifier;
  TypeLiteral type;
  SourceLocation loc;

  public RegisterDefinition(Identifier identifier, TypeLiteral type,
                            SourceLocation location) {
    this.identifier = identifier;
    this.type = type;
    this.loc = location;
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  void dump(int indent, StringBuilder builder) {
    builder.append(dumpIndentString(indent));
    builder.append(this.getClass().getSimpleName());
    builder.append("\n");
    identifier.dump(indent + 1, builder);
    type.dump(indent + 1, builder);
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
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
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    RegisterDefinition that = (RegisterDefinition) o;
    return identifier.equals(that.identifier) && type.equals(that.type);
  }

  @Override
  public int hashCode() {
    int result = identifier.hashCode();
    result = 31 * result + type.hashCode();
    return result;
  }
}

class RegisterFileDefinition extends Definition {
  Identifier identifier;
  TypeLiteral addressType;
  TypeLiteral registerType;
  SourceLocation loc;

  public RegisterFileDefinition(Identifier identifier, TypeLiteral addressType,
                                TypeLiteral registerType,
                                SourceLocation location) {
    this.identifier = identifier;
    this.addressType = addressType;
    this.registerType = registerType;
    this.loc = location;
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  void dump(int indent, StringBuilder builder) {
    builder.append(dumpIndentString(indent));
    builder.append(this.getClass().getSimpleName());
    builder.append("\n");
    identifier.dump(indent + 1, builder);
    addressType.dump(indent + 1, builder);
    registerType.dump(indent + 1, builder);
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    builder.append(prettyIndentString(indent));
    builder.append("register file ");
    identifier.prettyPrint(indent, builder);
    builder.append(": ");
    addressType.prettyPrint(indent, builder);
    builder.append(" -> ");
    registerType.prettyPrint(indent, builder);
    builder.append("\n");
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
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
    return identifier.equals(that.identifier) && addressType.equals(that.addressType)
        && registerType.equals(that.registerType);
  }

  @Override
  public int hashCode() {
    int result = identifier.hashCode();
    result = 31 * result + addressType.hashCode();
    result = 31 * result + registerType.hashCode();
    return result;
  }
}
