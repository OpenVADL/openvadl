package vadl.ast;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import vadl.utils.SourceLocation;

/**
 * The abstract syntax tree for the vadl language.
 */
public class Ast {
  List<Definition> definitions = new ArrayList<>();
  @Nullable SymbolTable rootSymbolTable;

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
  @Nullable
  SymbolTable symbolTable;

  SymbolTable symbolTable() {
    if (symbolTable == null) {
      throw new IllegalStateException(
          "Node " + this + " should have received a symbol table in a previous pass");
    }
    return symbolTable;
  }

  static String prettyIndentString(int indent) {
    var indentBy = 2;
    return " ".repeat(indentBy * indent);
  }

  abstract SourceLocation location();

  abstract SyntaxType syntaxType();

  abstract void prettyPrint(int indent, StringBuilder builder);
}
