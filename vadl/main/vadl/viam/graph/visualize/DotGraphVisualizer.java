// SPDX-FileCopyrightText : © 2025 TU Wien <vadl@tuwien.ac.at>
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

package vadl.viam.graph.visualize;

import java.util.Objects;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import vadl.viam.graph.Graph;
import vadl.viam.graph.Node;
import vadl.viam.graph.control.ControlNode;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * Visualizes a given Graph using the Dot graph language.
 */
// TODO: Refactor this class
public class DotGraphVisualizer implements GraphVisualizer<String, Graph> {

  private @Nullable Graph graph;
  private boolean withSourceLocation = false;
  private @Nullable Predicate<Node> nodeFilter;
  private String name = "unnamed";

  @Override
  public DotGraphVisualizer load(Graph graph) {
    this.graph = graph;
    this.name = graph.name;
    return this;
  }

  public DotGraphVisualizer withName(String name) {
    this.name = name;
    return this;
  }

  public DotGraphVisualizer withSourceLocation(boolean option) {
    withSourceLocation = option;
    return this;
  }

  public DotGraphVisualizer withNodeFilter(Predicate<Node> nodeFilter) {
    this.nodeFilter = nodeFilter;
    return this;
  }

  private boolean nodeFilter(Node node) {
    if (nodeFilter == null) {
      return true;
    }
    return nodeFilter.test(node);
  }

  @Override
  public String visualize() {
    Objects.requireNonNull(graph);

    StringBuilder dotBuilder = new StringBuilder();
    dotBuilder.append("digraph G {\n");
    dotBuilder.append("    label=%s\n".formatted(wrapStr(name)));
    dotBuilder.append("\n");

    var nodes = graph.getNodes(Node.class).filter(this::nodeFilter);

    nodes.forEach(node -> {
      dotBuilder
          .append("     ")
          .append(wrapStr(node.id()))
          .append(" [label=%s %s]".formatted(wrapStr(label(node)), nodeStyle(node)))
          .append(";\n");

      node.inputs().filter(this::nodeFilter).forEach((input) -> {
        dotBuilder.append("     ")
            .append(wrapStr(input.id))
            .append(" -> ")
            .append(wrapStr(node.id))
            .append("[dir=back arrowtail=empty];\n");
      });

      node.successors().filter(this::nodeFilter).forEach(successor -> {

        dotBuilder.append("     ")
            .append(wrapStr(node.id))
            .append(" -> ")
            .append(wrapStr(successor.id()))
            .append("[color=red];\n");
      });

    });

    dotBuilder.append("} \n");
    return dotBuilder.toString();

  }

  private static String wrapStr(Object str) {
    return "\"%s\"".formatted(str.toString().replace("\"", "\\\""));
  }

  protected String label(Node node) {
    var label = new StringBuilder();
    label.append(node.toString());
    if (node instanceof ExpressionNode) {
      label.append(" -> ");
      label.append(((ExpressionNode) node).type().name());
    }
    if (withSourceLocation && node.sourceLocation().isValid()) {
      label
          .append("\\n@ ")
          .append(node.sourceLocation().toConciseString());
    }
    return label.toString();
  }

  private String nodeStyle(Node node) {
    if (node instanceof ControlNode) {
      return "shape=box";
    }

    if (node instanceof ExpressionNode) {
      if (node.isLeaf()) {
        return "style=filled fontcolor=\"#2f9e44\" color=\"#2f9e44\" fillcolor=\"#b2f2bb\"";
      } else {
        return "style=filled fontcolor=\"#1971c2\" color=\"#1971c2\" fillcolor=\"#a5d8ff\"";
      }
    }

    return "";
  }

}
