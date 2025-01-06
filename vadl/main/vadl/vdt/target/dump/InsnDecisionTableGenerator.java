package vadl.vdt.target.dump;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import vadl.javaannotations.DispatchFor;
import vadl.javaannotations.Handler;
import vadl.vdt.impl.theiling.InnerNodeImpl;
import vadl.vdt.model.InnerNode;
import vadl.vdt.model.LeafNode;
import vadl.vdt.model.Node;
import vadl.vdt.model.Visitor;

/**
 * Generates a simple text table to list the decision path for each instruction.
 */
@DispatchFor(value = InnerNode.class, include = {"vadl.vdt"}, returnType = List.class)
public class InsnDecisionTableGenerator implements Visitor<List<List<String>>> {

  /**
   * Generates the path table for the given tree.
   *
   * @param tree the tree
   * @return the path table (a list of columns) or an empty list if the tree is empty
   */
  public static List<List<String>> generate(Node tree) {
    var rows = tree.accept(new InsnDecisionTableGenerator());
    if (rows == null) {
      return List.of();
    }

    // Transform: Rows -> Columns (required for the dump table enricher)
    var table = new ArrayList<List<String>>();
    for (int i = 0; i < rows.size(); i++) {
      var row = rows.get(i);
      for (int j = 0; j < row.size(); j++) {
        if (table.size() <= j) {
          table.add(new ArrayList<>());
        }
        while (table.get(j).size() < i) {
          table.get(j).add("");
        }
        table.get(j).add(i, row.get(j));
      }
    }

    if (table.isEmpty()) {
      return table;
    }

    // Add a header to each column
    table.get(0).add(0, "Instruction");
    for (int i = 1; i < table.size(); i++) {
      table.get(i).add(0, "DL " + (i - 1));
    }

    return table;
  }

  @Nullable
  @Override
  public List<List<String>> visit(LeafNode node) {
    return List.of(new ArrayList<>(List.of(node.instruction().source().simpleName())));
  }

  @Nullable
  @Override
  @SuppressWarnings("unchecked")
  public List<List<String>> visit(InnerNode node) {
    return (List<List<String>>) InsnDecisionTableGeneratorDispatcher.dispatch(this, node);
  }

  /**
   * Handler for {@link InnerNodeImpl}.
   *
   * @param node the inner node
   * @return the path table
   */
  @Handler
  public List<List<String>> handle(InnerNodeImpl node) {

    var label = new StringBuilder();
    label.append("insn & 0x").append(node.getMask().toValue().toString(16));

    var result = new ArrayList<List<String>>();

    // Default
    var defaultNode = node.getFallback();
    if (defaultNode != null) {
      var childLines = defaultNode.accept(this);
      if (childLines != null) {
        childLines.forEach(l -> l.add(1, label + " (default)"));
      }
      result.addAll(childLines);
    }

    // Decisions
    for (var entry : node.getChildren().entrySet()) {

      var childLines = entry.getValue().accept(this);
      if (childLines == null) {
        continue;
      }

      var childLabel = label + " == 0x" + entry.getKey().toBitVector().toValue().toString(16);
      childLines.forEach(l -> l.add(1, childLabel));
      result.addAll(childLines);
    }

    return result;
  }
}
