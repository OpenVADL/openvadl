package vadl.vdt.target.dump;

import java.math.BigInteger;
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
 * Generates a simple text tree representation of the VDT.
 */
@DispatchFor(value = InnerNode.class, include = {"vadl.vdt"}, returnType = List.class)
public class TextGraphGenerator implements Visitor<List<StringBuilder>> {

  /**
   * Generate a text representation of the given tree.
   *
   * @param tree the tree
   * @return the text representation
   */
  public static String generate(Node tree) {

    var sb = new StringBuilder();

    var result = tree.accept(new TextGraphGenerator());

    if (result != null) {
      result.forEach(l -> sb.append(l).append("\n"));
    }

    return sb.toString();
  }

  @Nullable
  @Override
  public List<StringBuilder> visit(LeafNode node) {
    return List.of(new StringBuilder(node.instruction().source().simpleName()));
  }

  @Nullable
  @Override
  @SuppressWarnings("unchecked")
  public List<StringBuilder> visit(InnerNode node) {
    return (List<StringBuilder>) TextGraphGeneratorDispatcher.dispatch(this, node);
  }

  /**
   * Handler for {@link InnerNodeImpl}.
   *
   * @param node the inner node
   * @return the text representations
   */
  @Handler
  public List<StringBuilder> handle(InnerNodeImpl node) {
    var result = new ArrayList<StringBuilder>();

    var label = new StringBuilder();
    BigInteger mask = node.getMask().toValue();
    label.append("insn & 0x").append(mask.toString(16));

    result.add(label);

    // Default
    Node defaultNode = node.getFallback();
    if (defaultNode != null) {
      var childLines = defaultNode.accept(this);
      if (childLines != null) {
        result.add(new StringBuilder("  |- default"));
        childLines.stream().map(l -> l.insert(0, "|  ")).forEach(result::add);
      }
    }

    // Children
    for (var child : node.getChildren().entrySet()) {
      var childNode = child.getValue();

      var childLines = childNode.accept(this);
      if (childLines == null) {
        continue;

      }

      var edgeLabel = new StringBuilder("  |- ");
      edgeLabel.append("0x").append(child.getKey().toBitVector().toValue().toString(16));

      result.add(edgeLabel);

      if (childLines.size() == 1) {
        edgeLabel.append(" -> ").append(childLines.get(0));
      } else {
        childLines.stream().map(l -> l.insert(0, "  |  ")).forEach(result::add);
      }
    }

    return result;
  }
}
