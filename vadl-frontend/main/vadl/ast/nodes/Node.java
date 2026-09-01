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

package vadl.ast.nodes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import vadl.ast.SymbolTable;
import vadl.utils.WithLocation;

@SuppressWarnings({"MissingJavadocType", "MissingJavadocMethod"})
public abstract class Node implements WithLocation {
  @Nullable
  public SymbolTable symbolTable;

  public SymbolTable symbolTable() {
    if (symbolTable == null) {
      throw new IllegalStateException(
          "Node `%s` should have received a symbol table in a previous pass, found at: %s"
              .formatted(toString(), location().toConciseString()));
    }
    return symbolTable;
  }

  public static String nodeNameFor(Class<? extends Node> nodeClass) {
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

  public String nodeName() {
    return nodeNameFor(getClass());
  }

  public static String prettyIndentString(int indent) {
    var indentBy = 2;
    return " ".repeat(indentBy * indent);
  }

  /// Print multiple nodes seperated by the seperator to the proivded builder
  public static <T extends Node> void prettyPrintJoin(String separator, List<T> nodes, int indent,
                                               StringBuilder builder) {
    for (int i = 0; i < nodes.size(); i++) {
      var node = nodes.get(i);
      if (i > 0) {
        builder.append(separator);
      }
      node.prettyPrint(indent, builder);
    }
  }

  public static boolean isBlockLayout(Node n) {
    return n instanceof LetExpr || n instanceof IfExpr || n instanceof MatchExpr
        || n instanceof Statement || n instanceof Definition;
  }


  public abstract SyntaxType syntaxType();

  public abstract void prettyPrint(int indent, StringBuilder builder);

  /**
   * Invokes {@code action} for each child owned by this node.
   *
   * @implSpec Implementations should always remember to call {@code super.forEachChild}.
   */
  public void forEachChild(Consumer<Node> action) {
    // Intentionally left blank to end the recursion up the class hierarchy.
  }

  /**
   * Returns all children "owned" by this node.
   *
   * @return a list of children.
   */
  public final List<Node> children() {
    var result = new ArrayList<Node>();
    forEachChild(result::add);
    return result;
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName();
  }
}
