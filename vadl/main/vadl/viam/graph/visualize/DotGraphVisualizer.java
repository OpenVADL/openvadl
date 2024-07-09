package vadl.viam.graph.visualize;

import java.util.Objects;
import javax.annotation.Nullable;
import vadl.viam.graph.Graph;
import vadl.viam.graph.Node;
import vadl.viam.graph.control.AbstractControlNode;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * Visualizes a given Graph using the Dot graph language.
 */
// TODO: Refactor this class
public class DotGraphVisualizer implements GraphVisualizer<String, Graph> {

  private @Nullable Graph graph;
  private boolean withSourceLocation = false;

  @Override
  public DotGraphVisualizer load(Graph graph) {
    this.graph = graph;
    return this;
  }

  public DotGraphVisualizer withSourceLocation(boolean option) {
    withSourceLocation = option;
    return this;
  }

  @Override
  public String visualize() {
    Objects.requireNonNull(graph);

    StringBuilder dotBuilder = new StringBuilder();
    dotBuilder.append("digraph G {\n");
    dotBuilder.append("    label=\"%s\"\n".formatted(graph.name));
    dotBuilder.append("\n");

    var nodes = graph.getNodes(Node.class);

    nodes.forEach(node -> {
      dotBuilder
          .append("     ")
          .append(node.id())
          .append(" [label=\"%s\" %s]".formatted(label(node), nodeStyle(node)))
          .append(";\n");

      node.inputs().forEach((input) -> {
        dotBuilder.append("     ")
            .append(input.id)
            .append(" -> ")
            .append(node.id)
            .append("[dir=back arrowtail=empty];\n");
      });

      node.successors().forEach(successor -> {

        dotBuilder.append("     ")
            .append(node.id)
            .append(" -> ")
            .append(successor.id())
            .append("[color=red];\n");
      });

    });


    dotBuilder.append("} \n");
    return dotBuilder.toString();

  }

  private String label(Node node) {
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
    if (node instanceof AbstractControlNode) {
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
