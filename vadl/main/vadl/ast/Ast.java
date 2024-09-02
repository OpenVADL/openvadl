package vadl.ast;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import vadl.error.VadlError;
import vadl.error.VadlException;
import vadl.utils.SourceLocation;

/**
 * The abstract syntax tree for the vadl language.
 */
public class Ast {
  List<Definition> definitions = new ArrayList<>();
  URI fileUri = SourceLocation.INVALID_SOURCE_LOCATION.uri();

  @Nullable
  SymbolTable rootSymbolTable;

  SymbolTable rootSymbolTable() {
    return Objects.requireNonNull(rootSymbolTable, "Symbol collector has not been applied");
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
      throw new VadlException(List.of(
          new VadlError("Node " + this + " should have received a symbol table in a previous pass",
              location(), null, null)));
    }
    return symbolTable;
  }

  static String prettyIndentString(int indent) {
    var indentBy = 2;
    return " ".repeat(indentBy * indent);
  }

  static boolean isBlockLayout(Node n) {
    return n instanceof LetExpr || n instanceof IfExpr || n instanceof MatchExpr
        || n instanceof Statement || n instanceof Definition;
  }

  abstract SourceLocation location();

  abstract SyntaxType syntaxType();

  abstract void prettyPrint(int indent, StringBuilder builder);
}

class RecordInstance extends Node {
  RecordType type;
  List<Node> entries;
  SourceLocation sourceLocation;

  RecordInstance(RecordType type, List<Node> entries, SourceLocation sourceLocation) {
    this.type = type;
    this.entries = entries;
    this.sourceLocation = sourceLocation;
  }

  @Override
  SourceLocation location() {
    return sourceLocation;
  }

  @Override
  SyntaxType syntaxType() {
    return type;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    builder.append("(");
    var isFirst = true;
    for (Node entry : entries) {
      if (!isFirst) {
        builder.append(" ; ");
      }
      isFirst = false;
      entry.prettyPrint(0, builder);
    }
    builder.append(")");
  }
}

class MacroReference extends Node {
  Macro macro;
  ProjectionType type;
  SourceLocation sourceLocation;

  MacroReference(Macro macro, ProjectionType type, SourceLocation sourceLocation) {
    this.macro = macro;
    this.type = type;
    this.sourceLocation = sourceLocation;
  }

  @Override
  SourceLocation location() {
    return sourceLocation;
  }

  @Override
  SyntaxType syntaxType() {
    return type;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    macro.name().prettyPrint(indent, builder);
  }
}

final class PlaceholderNode extends Node implements OperatorOrPlaceholder,
    FieldEncodingOrPlaceholder {

  List<String> segments;
  SyntaxType syntaxType;
  SourceLocation sourceLocation;

  PlaceholderNode(List<String> segments, SyntaxType syntaxType, SourceLocation sourceLocation) {
    this.segments = segments;
    this.syntaxType = syntaxType;
    this.sourceLocation = sourceLocation;
  }

  @Override
  public SourceLocation location() {
    return sourceLocation;
  }

  @Override
  SyntaxType syntaxType() {
    return syntaxType;
  }

  @Override
  public void prettyPrint(int indent, StringBuilder builder) {
    builder.append("$");
    builder.append(String.join(".", segments));
  }
}
