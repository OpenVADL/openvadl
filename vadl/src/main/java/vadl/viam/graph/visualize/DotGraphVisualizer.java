package vadl.viam.graph.visualize;

import java.util.Objects;
import javax.annotation.Nullable;
import vadl.viam.graph.Graph;
import vadl.viam.graph.Node;

/**
 * Visualizes a given Graph using the Dot graph language.
 */
public class DotGraphVisualizer implements GraphVisualizer<String, Graph> {

  private @Nullable Graph graph;

  @Override
  public DotGraphVisualizer load(Graph graph) {
    this.graph = graph;
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
          .append(" [label=\"%s\" %s]".formatted(node, nodeStyle(node)))
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
            .append(node)
            .append(" -> ")
            .append(successor.id())
            .append("[color=red];\n");
      });

    });


    dotBuilder.append("} \n");
    return dotBuilder.toString();

  }

  private String nodeStyle(Node node) {
    return "";
    //    if (node instanceof ControlNode) {
    //      return "shape=box";
    //    } else {
    //      if (node instanceof ExpressionNode) {
    //        if (node.isLeaf()) {
    //          return "style=filled fontcolor=\"#2f9e44\" color=\"#2f9e44\" fillcolor=\"#b2f2bb\"";
    //        } else {
    //          return "style=filled fontcolor=\"#1971c2\" color=\"#1971c2\" fillcolor=\"#a5d8ff\"";
    //        }
    //      }
  }

}
