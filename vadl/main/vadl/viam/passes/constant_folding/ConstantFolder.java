package vadl.viam.passes.constant_folding;

import java.util.Optional;
import vadl.viam.graph.Graph;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ConstantNode;

/**
 * This class contains the main logic for the constant propagation.
 * It was extracted from the {@link ConstantFoldingPass} so it can be called in every
 * pass as a subsequent step.
 */
public class ConstantFolder {

  record Pair(Node oldNode, ConstantNode newNode) {

  }

  public static void run(Graph graph) {

    boolean hasChanged;
    do {
      hasChanged = false;
      var result = graph.getNodes()
          .filter(Node::isActive)
          // When `normalize` returns an Optional
          // then create a `Pair`
          .map(builtInNode -> builtInNode.normalize()
              .map(y -> new Pair(builtInNode, (ConstantNode) y)))
          .flatMap(Optional::stream)
          .toList();

      for (var pair : result) {
        var oldNode = pair.oldNode;
        var newNode = pair.newNode;

        graph.replaceNode(oldNode, newNode);
        hasChanged = true;
      }
    } while (hasChanged);
  }
}