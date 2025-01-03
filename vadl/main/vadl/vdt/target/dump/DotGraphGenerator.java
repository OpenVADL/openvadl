package vadl.vdt.target.dump;

import static vadl.vdt.target.dump.DotGraphGeneratorDispatcher.dispatch;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;
import vadl.javaannotations.DispatchFor;
import vadl.javaannotations.Handler;
import vadl.utils.Pair;
import vadl.vdt.impl.theiling.InnerNodeImpl;
import vadl.vdt.impl.theiling.LeafNodeImpl;
import vadl.vdt.model.InnerNode;
import vadl.vdt.model.LeafNode;
import vadl.vdt.model.Node;
import vadl.vdt.model.Visitor;

/**
 * Generates a simple dot graph from a VDT.
 */
@DispatchFor(value = Node.class, include = {"vadl.vdt"}, returnType = Pair.class)
public class DotGraphGenerator implements Visitor<Pair<String, List<String>>> {

  /**
   * The node counter.
   */
  int ii = 0;

  /**
   * Generate a dot graph from the given decode tree.
   *
   * @param tree the decode tree
   * @return the dot graph
   */
  public static String generate(Node tree) {
    var sb = new StringBuilder();
    sb.append("digraph G {\n");
    sb.append("    rankdir=TB;\n");
    sb.append("    node [shape=box];\n");
    sb.append("\n");

    var result = tree.accept(new DotGraphGenerator());

    if (result != null) {
      result.right().forEach(sb::append);
    }

    sb.append("}\n");
    return sb.toString();
  }

  @Override
  @SuppressWarnings("unchecked")
  public @Nullable Pair<String, List<String>> visit(InnerNode node) {
    return (Pair<String, List<String>>) dispatch(this, node);
  }

  @Override
  @SuppressWarnings("unchecked")
  public @Nullable Pair<String, List<String>> visit(LeafNode node) {
    return (Pair<String, List<String>>) dispatch(this, node);
  }

  /**
   * Handle graph generation for {@link InnerNodeImpl}.
   *
   * @param node the inner node
   * @return the node id and the list of lines to add to the graph
   */
  @Handler
  public Pair<String, List<String>> handle(InnerNodeImpl node) {
    var id = "node_" + ii++;

    var label = new StringBuilder();
    BigInteger mask = node.getMask().toValue();
    label.append("Mask 0x").append(mask.toString(16));

    final List<String> lines = new ArrayList<>();
    lines.add("    %s [label=\"%s\"];\n".formatted(id, label));

    // Handle default node
    if (node.getFallback() != null) {
      var childResult = node.getFallback().accept(this);
      if (childResult != null) {
        lines.addAll(childResult.right());
        lines.add("    %s -> %s [label=\"default\"];\n".formatted(id, childResult.left()));
      }
    }

    // Handle other children
    node.getChildren().forEach((pattern, child) -> {

      var childResult = child.accept(this);
      if (childResult == null) {
        return;
      }

      lines.addAll(childResult.right());

      var edgeLabel = new StringBuilder();
      edgeLabel.append("0x").append(pattern.toBitVector().toValue().toString(16));

      lines.add("    %s -> %s [label=\"%s\"];\n".formatted(id, childResult.left(), edgeLabel));
    });

    return Pair.of(id, lines);
  }

  /**
   * Handle graph generation for {@link LeafNodeImpl}.
   *
   * @param node the leaf node
   * @return the node id and the list of lines to add to the graph
   */
  @Handler
  public Pair<String, List<String>> handle(LeafNodeImpl node) {
    var id = "node_" + ii++;
    var name = node.instruction().source().simpleName();
    return Pair.of(id, List.of("    %s [label=\"%s\"];\n".formatted(id, name)));
  }
}
