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
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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

  private final AstDumpLabler labler = new AstDumpLabler();
  private final Path sourcePath;
  private final String source;
  private final int[] lineStarts;
  private final List<SourceIndexEntry> sourceIndex = new ArrayList<>();
  private int nextNodeId;

  private record SourceIndexEntry(int start, int end, int nodeId, int depth) {
  }

  private AstAdvancedDumper(Path sourcePath, String source) {
    this.sourcePath = sourcePath.normalize();
    this.source = source;
    this.lineStarts = findLineStarts(source);
  }

  /**
   * Dumps the AST into a writer.
   *
   * @param ast to dump.
   * @param writer destination for the HTML representation of the tree
   */
  public static void dump(Ast ast, VirtualFileSystem vfs, String timeString, Writer writer) {
    String source = null;
    try {
      source = new String(vfs.getInputStream(Objects.requireNonNull(ast.filePath)).readAllBytes(),
          StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    var dumper = new AstAdvancedDumper(Objects.requireNonNull(ast.filePath), source);
    var astMaps = dumper.astToMaps(ast);
    TemplateRenderer.render("astDump/advanced.html",
        Map.of("ast", astMaps, "source", source, "timestamp", timeString,
            "sourceIndex", dumper.sourceIndexAsJavaScript()),
        writer);
  }

  /**
   * Convert the AST to a nested Map structure so that we no longer need to rely on reflection on
   * custom classes, which caused issues with GraalVM Native Images.
   */
  private List<Map<String, Object>> astToMaps(Ast ast) {
    return ast.definitions.stream().map(node -> nodeToMap(node, 0)).toList();
  }

  private Map<String, Object> nodeToMap(Node node, int depth) {
    var map = new HashMap<String, Object>();
    var label = label(node);
    var nodeId = nextNodeId++;
    var location = node.location();
    map.put("htmlId", "ast-node-" + nodeId);
    map.put("description", label.description());
    map.put("children",
        label.children().stream().map(child -> nodeToMap(child, depth + 1)).toList());
    map.put("location", locationToMap(location));
    map.put("expandedFrom", location == null ? List.of() : location.expandedFromStack().stream()
        .map(this::locationToMap).toList());
    addToSourceIndex(location, nodeId, depth);
    return map;
  }

  @Nullable
  private Map<String, Object> locationToMap(@Nullable WithLocation locatable) {
    var location = locatable == null ? null : locatable.location();
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

  private void addToSourceIndex(@Nullable WithLocation locatable, int nodeId, int depth) {
    var location = locatable == null ? null : locatable.location();
    if (location == null || !location.isValid() || location.path() == null
        || !location.path().normalize().equals(sourcePath)) {
      return;
    }

    var start = offsetAt(location.begin().line(), location.begin().column());
    var endOffset = offsetAt(location.end().line(), location.end().column());
    var end = endOffset < 0 ? -1 : Math.min(source.length(), endOffset + 1);
    if (start < 0 || end <= start) {
      return;
    }

    sourceIndex.add(new SourceIndexEntry(start, end, nodeId, depth));
  }

  private int offsetAt(int line, int column) {
    if (line < 1 || line > lineStarts.length || column < 1) {
      return -1;
    }

    var lineStart = lineStarts[line - 1];
    var lineEnd = line < lineStarts.length ? lineStarts[line] - 1 : source.length();
    if (column > lineEnd - lineStart + 1) {
      return -1;
    }
    return lineStart + column - 1;
  }

  private String sourceIndexAsJavaScript() {
    sourceIndex.sort(Comparator.comparingInt(SourceIndexEntry::start)
        .thenComparing(Comparator.comparingInt(SourceIndexEntry::end).reversed())
        .thenComparingInt(SourceIndexEntry::nodeId));

    var builder = new StringBuilder("[");
    var maxEnd = 0;
    for (var index = 0; index < sourceIndex.size(); index++) {
      var entry = sourceIndex.get(index);
      maxEnd = Math.max(maxEnd, entry.end());
      if (index > 0) {
        builder.append(',');
      }
      builder.append('[')
          .append(entry.start()).append(',')
          .append(entry.end()).append(',')
          .append(entry.nodeId()).append(',')
          .append(entry.depth()).append(',')
          .append(maxEnd)
          .append(']');
    }
    return builder.append(']').toString();
  }

  private static int[] findLineStarts(String source) {
    var starts = new ArrayList<Integer>();
    starts.add(0);
    for (var index = 0; index < source.length(); index++) {
      if (source.charAt(index) == '\n') {
        starts.add(index + 1);
      }
    }
    return starts.stream().mapToInt(Integer::intValue).toArray();
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
