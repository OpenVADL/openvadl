package vadl.ast;

import java.util.ArrayList;
import java.util.List;

/** The abstract syntax tree for the vadl language.
 *
 */
public class Ast {
  List<Stmt> statements = new ArrayList<>();

  /** Dump the AST into a tree like representation for debugging.
   *
   * @return a String with the dumped tree.
   */
  public String dump() {
    StringBuilder builder = new StringBuilder();
    builder.append("=== AST DUMP ===\n");
    for (var stmt : statements) {
      stmt.dump(0, builder);
    }
    return builder.toString();
  }

  /** Convert the tree back into sourcecode.
   * The generated sourcecode might look quite different but is semantically equal. Some notable
   * details are however:
   * - All macros are expanded and macro definitions are no longer in the tree.
   * - Grouping with parenthesis might be lost.
   *
   * @return a source code resulting in the same AST.
   */
  public String prettyPrint() {
    StringBuilder builder = new StringBuilder();
    builder.append("=== AST PRETTY PRINT ===\n");
    for (var stmt : statements) {
      stmt.prettyPrint(0, builder);
    }
    return builder.toString();
  }
}

abstract class Node {
  protected String indentString(int indent) {
    var indentBy = 2;
    var indentCharacters = ". : ' | ";
    var indentLenght = indent * indentBy;
    return indentCharacters.repeat(indentLenght / indentCharacters.length())
        + indentCharacters.substring(0, indentLenght % indentCharacters.length());
  }

  abstract Location location();

  abstract void dump(int indent, StringBuilder builder);

  abstract void prettyPrint(int indent, StringBuilder builder);
}