package vadl.viam.passes.constant_folding;

import java.util.HashSet;
import org.jetbrains.annotations.Nullable;
import vadl.viam.graph.Canonicalizable;
import vadl.viam.graph.Graph;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;

/**
 * Applies the canonical form of nodes to the graph.
 * Either it is applied to the whole graph, so the canonical form of all nodes is evaluated
 * and applied. Or it is applied to a subgraph (a node and all its inputs).
 */
public class Canonicalizer implements GraphVisitor<Object> {

  private final HashSet<Node> processedNodes;

  private Canonicalizer() {
    processedNodes = new HashSet<>();
  }

  /**
   * Applies the canonicalization on the whole graph.
   * It traverses all nodes (from leaves to roots) and replaces them by their
   * canonical form.
   */
  public static void canonicalize(Graph graph) {
    new Canonicalizer()
        .canonicalizeGraph(graph);
  }

  /**
   * Applies the canonicalization on a subgraph.
   * It traverses all inputs (from leaves to the given node) and replaces them by their
   * canonical form.
   */
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
      // node was already processed before
      return;
    }
    processedNodes.add(node);

    // first visit all inputs to receive their canonical form
    node.visitInputs(this);

    if (node instanceof Canonicalizable) {
      // retrieve the canonical form of node
      var canonicalized = ((Canonicalizable) node).canonical();
      if (canonicalized == node) {
        // canonical form is same as original one
        return;
      }

      // replace original one by new one
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
