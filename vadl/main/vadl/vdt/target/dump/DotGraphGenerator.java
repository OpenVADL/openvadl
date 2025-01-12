package vadl.vdt.target.dump;

import static vadl.vdt.target.dump.DotGraphGeneratorDispatcher.dispatch;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;
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
public class DotGraphGenerator implements Visitor<Pair<Integer, List<CharSequence>>> {

  /**
   * The node counter.
   */
  private static final ThreadLocal<AtomicInteger> counter =
      ThreadLocal.withInitial(AtomicInteger::new);

  /**
   * Generate a dot graph from the given decode tree.
   *
   * @param tree the decode tree
   * @return the dot graph
   */
  public static CharSequence generate(Node tree) {

    try {

      // Reset the counter
      counter.get().set(0);

      // Write the header
      var sb = new StringBuilder();
      sb.append("digraph G {\n");
      sb.append("    rankdir=TB;\n");
      sb.append("    node [shape=box];\n");
      sb.append("\n");

      // Recursively generate the graph
      var result = tree.accept(new DotGraphGenerator());

      // Append the result lines
      if (result != null) {
        result.right().forEach(sb::append);
      }

      sb.append("}\n");
      return sb;

    } finally {
      counter.remove();
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public @Nullable Pair<Integer, List<CharSequence>> visit(InnerNode node) {
    return (Pair<Integer, List<CharSequence>>) dispatch(this, node);
  }

  @Override
  @SuppressWarnings("unchecked")
  public @Nullable Pair<Integer, List<CharSequence>> visit(LeafNode node) {
    return (Pair<Integer, List<CharSequence>>) dispatch(this, node);
  }

  /**
   * Handle graph generation for {@link InnerNodeImpl}.
   *
   * @param node the inner node
   * @return the node id and the list of lines to add to the graph
   */
  @Handler
  public Pair<Integer, List<CharSequence>> handle(InnerNodeImpl node) {
    var id = counter.get().getAndIncrement();

    final List<CharSequence> lines = new ArrayList<>();
    var nodeDefinition = new StringBuilder()
        .append("    ")
        .append(id).append(" [label=\"")
        .append("Mask 0x").append(node.getMask().toValue().toString(16))
        .append("\"];\n");
    lines.add(nodeDefinition);

    // Handle default node
    if (node.getFallback() != null) {
      var childResult = node.getFallback().accept(this);
      if (childResult != null) {
        lines.addAll(childResult.right());
        var defaultEdge = new StringBuilder()
            .append("    ")
            .append(id).append(" -> ").append(childResult.left())
            .append(" [label=\"default\"];\n");
        lines.add(defaultEdge);
      }
    }

    // Handle other children
    node.getChildren().forEach((pattern, child) -> {

      var childResult = child.accept(this);
      if (childResult == null) {
        return;
      }

      lines.addAll(childResult.right());

      var edgeLine = new StringBuilder()
          .append("    ")
          .append(id).append(" -> ").append(childResult.left())
          .append(" [label=\"0x").append(pattern.toBitVector().toValue().toString(16))
          .append("\"];\n");
      lines.add(edgeLine);
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
  public Pair<Integer, List<CharSequence>> handle(LeafNodeImpl node) {
    var id = counter.get().getAndIncrement();
    var name = node.instruction().source().simpleName();

    var leafNode = new StringBuilder()
        .append("    ")
        .append(id).append(" [label=\"")
        .append(name)
        .append("\"];\n");

    return Pair.of(id, List.of(leafNode));
  }
}
