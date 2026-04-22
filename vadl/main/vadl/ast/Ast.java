// SPDX-FileCopyrightText : © 2025-2026 TU Wien <vadl@tuwien.ac.at>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

package vadl.ast;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import vadl.types.Type;
import vadl.utils.SourceLocation;
import vadl.utils.WithLocation;

/**
 * The abstract syntax tree for the vadl language.
 */
public class Ast {
  List<Definition> definitions = new ArrayList<>();

  @Nullable
  Path filePath = null;

  public InterleavedTimingRecorder timingRecorder = new InterleavedTimingRecorder();


  @Nullable
  SymbolTable rootSymbolTable;


  SymbolTable rootSymbolTable() {
    return Objects.requireNonNull(rootSymbolTable, "Symbol collector has not been applied");
  }

  public <T> T withPassTiming(String name, Supplier<T> pass) {
    return timingRecorder.withPassTiming(name, pass);
  }

  public void withPassTiming(String name, Runnable pass) {
    timingRecorder.withPassTiming(name, pass);
  }

  /**
   * Returns the list of all files that are included from this AST.
   *
   * @param recursive if true, recursively include files from imported modules.
   * @return the list of paths.
   */
  public List<Path> includedFiles(boolean recursive) {
    return definitions.stream()
        .filter(def -> def instanceof ImportDefinition)
        .flatMap(def -> {
          var ast = ((ImportDefinition) def).moduleAst;
          var stream = Stream.of(ast.filePath);
          if (recursive) {
            stream = Stream.concat(stream, ast.includedFiles(true).stream());
          }
          return stream;
        })
        .toList();
  }

  /**
   * Returns the list of all files that were read to generate this AST.
   *
   * @return the list of paths.
   */
  public List<Path> allReadFiles() {
    var paths = new ArrayList<>(includedFiles(true));
    paths.addFirst(filePath);
    return paths;
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
  public CharSequence prettyPrint() {
    StringBuilder builder = new StringBuilder();
    Definition.prettyPrintDefinitions(0, builder, definitions);
    return builder;
  }

  /**
   * Convert the tree back into sourcecode.
   * The generated sourcecode might look quite different but is semantically equal. Some notable
   * details are however:
   * <li> All macros are expanded and macro definitions are no longer in the tree.
   * <li> Grouping with parenthesis might be lost.
   *
   * <p>Consider prettyPrint() if performance is important.
   *
   * @return a source code resulting in the same AST.
   */
  public String prettyPrintToString() {
    return prettyPrint().toString();
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

abstract class Node implements WithLocation {
  @Nullable
  SymbolTable symbolTable;

  SymbolTable symbolTable() {
    if (symbolTable == null) {
      throw new IllegalStateException(
          "Node `%s` should have received a symbol table in a previous pass, found at: %s"
              .formatted(toString(), location().toConciseString()));
    }
    return symbolTable;
  }

  static String nodeNameFor(Class<? extends Node> nodeClass) {
    var hardCodedNames = Map.of(
        AsIdExpr.class, "AsId expr",
        AsStrExpr.class, "AsStr expr",
        BinOp.class, "binary operator",
        UnOp.class, "unary operator"
    );
    if (hardCodedNames.containsKey(nodeClass)) {
      return hardCodedNames.get(nodeClass);
    }

    var words =  nodeClass.getSimpleName()
        .replaceAll("([a-z])([A-Z])", "$1 $2")
        .toLowerCase()
        .split(" ");

    var replacements = Map.of(
        "asm", "assembly",
        "expr", "expression",
        "stmt", "statement"
    );

    return Arrays.stream(words)
        .map(word -> replacements.getOrDefault(word, word))
        .collect(Collectors.joining(" "));
  }

  String nodeName() {
    return nodeNameFor(getClass());
  }

  static String prettyIndentString(int indent) {
    var indentBy = 2;
    return " ".repeat(indentBy * indent);
  }

  /// Print multiple nodes seperated by the seperator to the proivded builder
  static <T extends Node> void prettyPrintJoin(String separator, List<T> nodes, int indent,
                                               StringBuilder builder) {
    for (int i = 0; i < nodes.size(); i++) {
      var node = nodes.get(i);
      if (i > 0) {
        builder.append(separator);
      }
      node.prettyPrint(indent, builder);
    }
  }

  static boolean isBlockLayout(Node n) {
    return n instanceof LetExpr || n instanceof IfExpr || n instanceof MatchExpr
        || n instanceof Statement || n instanceof Definition;
  }


  abstract SyntaxType syntaxType();

  abstract void prettyPrint(int indent, StringBuilder builder);

  /**
   * Returns all children "owned" by this node.
   *
   * <p>Either automatically generated by annotating children with the
   * {@link vadl.javaannotations.ast.Child} annotation or by overriding this method.
   *
   * @return a list of children.
   */
  List<Node> children() {
    return NodeChildrenRegistry.getChildren(this);
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName();
  }
}

/**
 * A node that can be identified by an identifier.
 */
interface IdentifiableNode {
  Identifier identifier();
}

interface TypedNode {
  Type type();
}

final class BinOp extends Node implements IsBinOp {

  Operator operator;
  SourceLocation location;

  BinOp(Operator operator, SourceLocation location) {
    this.operator = operator;
    this.location = location;
  }

  @Override
  public SourceLocation location() {
    return location;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.BIN_OP;
  }

  @Override
  public void prettyPrint(int indent, StringBuilder builder) {
    builder.append(operator.symbol);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + " " + operator.symbol;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BinOp that = (BinOp) o;
    return Objects.equals(operator, that.operator);
  }

  @Override
  public int hashCode() {
    return operator.hashCode();
  }
}

final class UnOp extends Node implements IsUnOp {

  UnaryOperator operator;
  SourceLocation location;

  UnOp(UnaryOperator operator, SourceLocation location) {
    this.operator = operator;
    this.location = location;
  }

  @Override
  public SourceLocation location() {
    return location;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.UN_OP;
  }

  @Override
  public void prettyPrint(int indent, StringBuilder builder) {
    builder.append(operator.symbol);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + " " + operator.symbol;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UnOp that = (UnOp) o;
    return Objects.equals(operator, that.operator);
  }

  @Override
  public int hashCode() {
    return operator.hashCode();
  }
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
  public SourceLocation location() {
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
  public SourceLocation location() {
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

final class PlaceholderNode extends Node implements IsBinOp, IsUnOp, IsEncs {

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

final class MacroInstanceNode extends Node implements IsMacroInstance, IsEncs, IsBinOp, IsUnOp {

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
final class MacroMatchNode extends Node implements IsMacroMatch, IsEncs, IsBinOp, IsUnOp {
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


