package vadl.ast;

import java.net.URI;
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
  URI fileUri = SourceLocation.INVALID_SOURCE_LOCATION.uri();
  List<VadlParser.PassTimings> passTimings = new ArrayList<>();

  @Nullable
  SymbolTable rootSymbolTable;

  SymbolTable rootSymbolTable() {
    return Objects.requireNonNull(rootSymbolTable, "Symbol collector has not been applied");
  }

  /**
   * Convert the tree back into sourcecode.
   * The generated sourcecode might look quite different but is semantically equal. Some notable
   * details are however:
   * <li> All macros are expanded and macro definitions are no longer in the tree.
   * <li> Grouping with parenthesis might be lost.
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

final class PlaceholderNode extends Node implements IsBinOp, IsUnOp, FieldEncodingOrPlaceholder {

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

final class MacroInstanceNode extends Node implements MacroInstance, FieldEncodingOrPlaceholder {

  MacroOrPlaceholder macro;
  List<Node> arguments;
  SourceLocation loc;

  public MacroInstanceNode(MacroOrPlaceholder macro, List<Node> arguments, SourceLocation loc) {
    this.macro = macro;
    this.arguments = arguments;
    this.loc = loc;
  }

  @Override
  public SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return macro.returnType();
  }

  @Override
  public void prettyPrint(int indent, StringBuilder builder) {
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
    builder.append(")");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    MacroInstanceNode that = (MacroInstanceNode) o;
    return macro.equals(that.macro)
        && arguments.equals(that.arguments);
  }

  @Override
  public int hashCode() {
    int result = macro.hashCode();
    result = 31 * result + arguments.hashCode();
    return result;
  }

  @Override
  public MacroOrPlaceholder macroOrPlaceholder() {
    return macro;
  }
}

/**
 * An internal temporary placeholder of a macro-level "match" construct.
 * This node should never leave the parser.
 */
final class MacroMatchNode extends Node implements FieldEncodingOrPlaceholder {
  MacroMatch macroMatch;

  MacroMatchNode(MacroMatch macroMatch) {
    this.macroMatch = macroMatch;
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
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    MacroMatchNode that = (MacroMatchNode) o;
    return macroMatch.equals(that.macroMatch);
  }

  @Override
  public int hashCode() {
    return macroMatch.hashCode();
  }
}
