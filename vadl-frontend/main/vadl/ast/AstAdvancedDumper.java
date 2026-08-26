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

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;
import vadl.ast.nodes.Definition;
import vadl.ast.nodes.Expr;
import vadl.ast.nodes.Node;
import vadl.ast.nodes.Statement;
import vadl.template.TemplateRenderer;
import vadl.utils.VirtualFileSystem;
import vadl.utils.WithLocation;

/**
 * Generates a interactive HTML dump of the AST and Sourcecode.
 */
public class AstAdvancedDumper {

  private AstDumpLabler labler = new AstDumpLabler();

  /**
   * Dumps the AST into a textual representation.
   *
   * @param ast to dump.
   * @return an HTML representation of the tree.
   */
  public static String dump(Ast ast, VirtualFileSystem vfs, String timeString) {
    String source = null;
    try {
      source = new String(vfs.getInputStream(Objects.requireNonNull(ast.filePath)).readAllBytes(),
          StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    var dumper = new AstAdvancedDumper();
    var renderedDump = new StringWriter();
    TemplateRenderer.render("astDump/advanced.html",
        Map.of("ast", dumper.astToMaps(ast), "source", source, "timestamp", timeString),
        renderedDump);
    return renderedDump.toString();
  }

  /**
   * Convert the AST to a nested Map structure so that we no longer need to rely on reflection on
   * custom classes, which caused issues with GraalVM Native Images.
   */
  private List<Map<String, Object>> astToMaps(Ast ast) {
    return ast.definitions.stream().map(this::nodeToMap).toList();
  }

  private Map<String, Object> nodeToMap(Node node) {
    var map = new HashMap<String, Object>();
    var label = label(node);
    map.put("description", label.description());
    map.put("children", label.children().stream().map(this::nodeToMap).toList());
    map.put("location", locationToMap(node));
    map.put("expandedFrom", node.location().expandedFromStack().stream()
        .map(this::locationToMap).toList());
    return map;
  }

  @Nullable
  private Map<String, Object> locationToMap(WithLocation locatable) {
    var location = locatable.location();
    if (location == null || !location.isValid()) {
      return null;
    }

    var map = new HashMap<String, Object>();
    map.put("path", location.path());
    map.put("startLine", location.begin().line());
    map.put("startColumn", location.begin().column());
    map.put("endLine", location.end().line());
    map.put("endColumn", location.end().column());
    return map;
  }

  /**
   * Generate the label/description for a node.
   *
   * @param node to label.
   * @return the text to put into the dump.
   */
  public AstDumpLabler.DumpLabel label(Node node) {
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

    return label;
  }

}
