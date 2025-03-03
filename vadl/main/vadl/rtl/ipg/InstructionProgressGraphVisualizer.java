package vadl.rtl.ipg;

import java.util.stream.Collectors;
import javax.annotation.Nullable;
import vadl.viam.Definition;
import vadl.viam.graph.Graph;
import vadl.viam.graph.Node;
import vadl.viam.graph.visualize.DotGraphVisualizer;

/**
 * Visualize the instruction progress graph as a DOT language graph.
 *
 * <p>Includes the instructions associated with each node in the visualization.
 */
public class InstructionProgressGraphVisualizer extends DotGraphVisualizer {

  private @Nullable InstructionProgressGraph ipg;
  private boolean withInstructions = true;

  public DotGraphVisualizer withInstructions(boolean option) {
    withInstructions = option;
    return this;
  }

  @Override
  public DotGraphVisualizer load(Graph graph) {
    this.ipg = null;
    if (graph instanceof InstructionProgressGraph instructionProgressGraph) {
      this.ipg = instructionProgressGraph;
    }
    return super.load(graph);
  }

  @Override
  protected String label(Node node) {
    var label = new StringBuilder(super.label(node));

    if (withInstructions && ipg != null) {
      var context = ipg.getContext(node);
      if (context != null && !context.instructions().isEmpty()
          && !context.instructions().containsAll(ipg.instructions())) {
        var instructionsNotContained = ipg.instructions().stream()
                .filter(i -> !context.instructions().contains(i)).collect(Collectors.toSet());
        if (instructionsNotContained.size() < context.instructions().size()) {
          label.append("\\n!{")
              .append(instructionsNotContained.stream()
                  .map(Definition::simpleName).collect(Collectors.joining(", ")))
              .append("}");
        } else {
          label.append("\\n{")
              .append(context.instructions().stream()
                  .map(Definition::simpleName).collect(Collectors.joining(", ")))
              .append("}");
        }
      }
    }

    return label.toString();
  }
}
