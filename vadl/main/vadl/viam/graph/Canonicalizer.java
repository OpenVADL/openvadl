package vadl.viam.graph;

import java.util.HashSet;
import org.jetbrains.annotations.Nullable;

public class Canonicalizer implements GraphVisitor {

  private HashSet<Node> processedNodes;

  private Canonicalizer() {
    processedNodes = new HashSet<>();
  }

  public static void canonicalize(Graph graph) {
    new Canonicalizer()
        .canonicalizeGraph(graph);
  }

  public static void canonicalizeSubGraph(Node node) {
    new Canonicalizer()
        .canonicalizeNode(node);
  }


  private void canonicalizeGraph(Graph graph) {
    for (var n : graph.getNodes()
        .filter(e -> e.usageCount() == 0) // only get nodes that are not used (root nodes)
        .toList()) {
      processNode(n);
    }
  }

  private void canonicalizeNode(Node node) {
    node.ensure(node.isActive(), "cannot canonicalize in active node");
    processNode(node);
  }

  private void processNode(Node node) {
    if (processedNodes.contains(node)) {
      return;
    }
    processedNodes.add(node);

    // first visit all inputs to receive their canonical form
    node.visitInputs(this);

    if (node instanceof Canonicalizable) {
      var canonicalized = ((Canonicalizable) node).canonical();
      if (canonicalized == node) {
        return;
      }

      node.replaceAndDelete(canonicalized);
    }
  }


  @Nullable
  @Override
  public Object visit(Node from, @Nullable Node to) {
    if (to != null) {
      processNode(to);
    }
    return null;
  }
}
