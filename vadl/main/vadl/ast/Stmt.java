package vadl.ast;

import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;

/**
 * The Statement nodes inside the AST.
 */
abstract class Stmt extends Node {
}

class ConstantDefinitionStmt extends Stmt {
  final Identifier identifier;

  @Nullable
  final TypeLiteral typeAnnotation;

  final Expr value;
  final Location loc;

  ConstantDefinitionStmt(Identifier identifier, @Nullable TypeLiteral typeAnnotation, Expr value,
                         Location location) {
    this.identifier = identifier;
    this.typeAnnotation = typeAnnotation;
    this.value = value;
    this.loc = location;
  }

  @Override
  Location location() {
    return loc;
  }

  @Override
  void dump(int indent, StringBuilder builder) {
    builder.append(dumpIndentString(indent));
    builder.append("ConstantDefinitionStmt\n");
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
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ConstantDefinitionStmt that = (ConstantDefinitionStmt) o;
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

class FormatDefinitionStmt extends Stmt {
  Identifier identifier;
  TypeLiteral typeAnnotation;
  List<FormatField> fields;
  Location loc;

  static class FormatField extends Node {
    Identifier identifier;
    List<RangeExpr> ranges;

    public FormatField(Identifier identifier, List<RangeExpr> ranges) {
      this.identifier = identifier;
      this.ranges = ranges;
    }

    @Override
    Location location() {
      return new Location(identifier.location(), ranges.get(ranges.size() - 1).location());
    }

    @Override
    void dump(int indent, StringBuilder builder) {
      builder.append(dumpIndentString(indent));
      builder.append("FormatField\n");
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
      return Objects.equals(identifier, that.identifier) &&
          Objects.equals(ranges, that.ranges);
    }

    @Override
    public int hashCode() {
      int result = Objects.hashCode(identifier);
      result = 31 * result + Objects.hashCode(ranges);
      return result;
    }
  }

  public FormatDefinitionStmt(Identifier identifier, TypeLiteral typeAnnotation,
                              List<FormatField> fields, Location location) {
    this.identifier = identifier;
    this.typeAnnotation = typeAnnotation;
    this.fields = fields;
    this.loc = location;
  }

  @Override
  Location location() {
    return loc;
  }

  @Override
  void dump(int indent, StringBuilder builder) {
    builder.append(dumpIndentString(indent));
    builder.append("FormatDefinitionStmt\n");
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
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    FormatDefinitionStmt that = (FormatDefinitionStmt) o;
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


class IndexDefinitionStmt extends Stmt {
  IndexKind kind;
  Identifier identifier;
  TypeLiteral type;
  Location loc;

  enum IndexKind {
    PROGRAM,
    GROUP
  }

  public IndexDefinitionStmt(IndexKind kind, Identifier identifier, TypeLiteral type,
                             Location location) {
    this.kind = kind;
    this.identifier = identifier;
    this.type = type;
    this.loc = location;
  }

  @Override
  Location location() {
    return loc;
  }

  @Override
  void dump(int indent, StringBuilder builder) {
    builder.append(dumpIndentString(indent));
    builder.append("IndexDeclarationStmt (kind: %s)\n".formatted(kind.toString()));
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
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    IndexDefinitionStmt that = (IndexDefinitionStmt) o;
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

class MemoryDefinitionStmt extends Stmt {
  Identifier identifier;
  TypeLiteral addressType;
  TypeLiteral dataType;
  Location loc;

  public MemoryDefinitionStmt(Identifier identifier, TypeLiteral addressType, TypeLiteral dataType,
                              Location loc) {
    this.identifier = identifier;
    this.addressType = addressType;
    this.dataType = dataType;
    this.loc = loc;
  }

  @Override
  Location location() {
    return loc;
  }

  @Override
  void dump(int indent, StringBuilder builder) {
    builder.append(dumpIndentString(indent));
    builder.append("MemoryDefinitionStmt\n");
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
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    MemoryDefinitionStmt that = (MemoryDefinitionStmt) o;
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

class RegisterDefinitionStmt extends Stmt {
  Identifier identifier;
  TypeLiteral type;
  Location loc;

  public RegisterDefinitionStmt(Identifier identifier, TypeLiteral type,
                                Location location) {
    this.identifier = identifier;
    this.type = type;
    this.loc = location;
  }

  @Override
  Location location() {
    return loc;
  }

  @Override
  void dump(int indent, StringBuilder builder) {
    builder.append(dumpIndentString(indent));
    builder.append("RegisterDefinitionStmt\n");
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
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    RegisterDefinitionStmt that = (RegisterDefinitionStmt) o;
    return identifier.equals(that.identifier) && type.equals(that.type);
  }

  @Override
  public int hashCode() {
    int result = identifier.hashCode();
    result = 31 * result + type.hashCode();
    return result;
  }
}

class RegisterFileDefinitionStmt extends Stmt {
  Identifier identifier;
  TypeLiteral addressType;
  TypeLiteral registerType;
  Location loc;

  public RegisterFileDefinitionStmt(Identifier identifier, TypeLiteral addressType,
                                    TypeLiteral registerType,
                                    Location location) {
    this.identifier = identifier;
    this.addressType = addressType;
    this.registerType = registerType;
    this.loc = location;
  }

  @Override
  Location location() {
    return loc;
  }

  @Override
  void dump(int indent, StringBuilder builder) {
    builder.append(dumpIndentString(indent));
    builder.append("RegisterFileDefinitionStmt\n");
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
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    RegisterFileDefinitionStmt that = (RegisterFileDefinitionStmt) o;
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
