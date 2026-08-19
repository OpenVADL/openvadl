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

import java.util.Collections;
import javax.annotation.Nullable;
import vadl.ast.nodes.Definition;
import vadl.ast.nodes.Expr;
import vadl.ast.nodes.Node;
import vadl.ast.nodes.Statement;

/**
 * A pass over the AST that produces a textual representation of the AST.
 */
public class AstDumper {
  private StringBuilder builder = new StringBuilder();
  private AstDumpLabler labler = new AstDumpLabler();
  private int indent;

  /**
   * Dumps the AST into a textual representation.
   *
   * @param ast to dump.
   * @return a textual representation of the tree.
   */
  public String dump(Ast ast) {
    builder = new StringBuilder();
    indent = 0;

    for (var definition : ast.definitions) {
      dumpNode(definition);
    }
    return builder.toString();
  }

  private void dumpNode(Node node) {
    builder.append(indentString());

    @Nullable AstDumpLabler.DumpLabel label;
    if (node instanceof Definition def) {
      label = def.accept(labler);
    } else if (node instanceof Expr expr) {
      label = expr.accept(labler);
    } else if (node instanceof Statement statement) {
      label = statement.accept(labler);
    } else if (node == null) {
      label = new AstDumpLabler.DumpLabel("null", Collections.emptyList());
    } else {
      label = labler.visitNode(node);
    }

    builder.append(label.description());
    builder.append("\n");

    indent++;
    label.children().forEach(child -> dumpNode(child));
    indent--;
  }

  private String indentString() {
    var indentBy = 2;
    var indentCharacters = ". : ' | ";
    var indentLength = indent * indentBy;
    return indentCharacters.repeat(indentLength / indentCharacters.length())
        + indentCharacters.substring(0, indentLength % indentCharacters.length());
  }

}