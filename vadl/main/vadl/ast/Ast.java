package vadl.ast;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The abstract syntax tree for the vadl language.
 */
public class Ast {
  List<Definition> definitions = new ArrayList<>();

  /**
   * Dump the AST into a tree like representation for debugging.
   *
   * @return a String with the dumped tree.
   */
  public String dump() {
    StringBuilder builder = new StringBuilder();
    for (var stmt : definitions) {
      stmt.dump(0, builder);
    }
    return builder.toString();
  }

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
    for (var stmt : definitions) {
      stmt.prettyPrint(0, builder);
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

    Ast ast = (Ast) o;
    return Objects.equals(definitions, ast.definitions);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(definitions);
  }
}

abstract class Node {
  protected String dumpIndentString(int indent) {
    var indentBy = 2;
    var indentCharacters = ". : ' | ";
    var indentLenght = indent * indentBy;
    return indentCharacters.repeat(indentLenght / indentCharacters.length())
        + indentCharacters.substring(0, indentLenght % indentCharacters.length());
  }

  protected String prettyIndentString(int indent) {
    var indentBy = 2;
    return " ".repeat(indentBy * indent);
  }

  abstract Location location();

  abstract void dump(int indent, StringBuilder builder);

  abstract void prettyPrint(int indent, StringBuilder builder);
}

class Identifier extends Node {
  String name;
  Location loc;

  public Identifier(String name, Location location) {
    this.loc = location;
    this.name = name;
  }

  @Override
  void dump(int indent, StringBuilder builder) {
    builder.append(dumpIndentString(indent));
    builder.append("Identifier \"%s\"\n".formatted(name));
  }

  @Override
  Location location() {
    return loc;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    builder.append(name);
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
