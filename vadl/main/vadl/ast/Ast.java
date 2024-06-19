package vadl.ast;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import vadl.utils.SourceLocation;

/**
 * The abstract syntax tree for the vadl language.
 */
public class Ast {
  List<Definition> definitions = new ArrayList<>();

  /**
   * Convert the tree back into sourcecode.
   * The generated sourcecode might look quite different but is semantically equal. Some notable
   * details are however:
   * - All macros are expanded and macro definitions are no longer in the tree.
   * - Grouping with parenthesis might be lost.
   *
   * @return a source code resulting in the same AST.
   */
  public String prettyPrint() {
    StringBuilder builder = new StringBuilder();
    for (var definition : definitions) {
      definition.prettyPrint(0, builder);
    }
    return builder.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Ast that = (Ast) o;
    return Objects.equals(definitions, that.definitions);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(definitions);
  }
}

abstract class Node {
  protected String prettyIndentString(int indent) {
    var indentBy = 2;
    return " ".repeat(indentBy * indent);
  }

  abstract SourceLocation location();

  abstract SyntaxType syntaxType();

  abstract void prettyPrint(int indent, StringBuilder builder);
}

class Identifier extends Node {
  String name;
  SourceLocation loc;

  public Identifier(String name, SourceLocation location) {
    this.loc = location;
    this.name = name;
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return CoreType.Id();
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    builder.append(name);
  }

  @Override
  public String toString() {
    return "%s name: \"%s\"".formatted(this.getClass().getSimpleName(), this.name);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Identifier that = (Identifier) o;
    return name.equals(that.name);
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }
}
