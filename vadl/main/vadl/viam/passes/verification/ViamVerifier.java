package vadl.viam.passes.verification;

import vadl.viam.Definition;
import vadl.viam.DefinitionVisitor;
import vadl.viam.graph.Node;
import vadl.viam.passes.GraphProcessor;
import vadl.viam.passes.dummyAbi.DummyAbi;

/**
 * Calls the verification method on all definitions in the given one and all its
 * member definitions.
 */
public class ViamVerifier extends DefinitionVisitor.Recursive {

  private ViamVerifier() {
  }

  public static void verifyAllIn(Definition toVerify) {
    new ViamVerifier().verifyDefinition(toVerify);
  }

  private void verifyDefinition(Definition toVerify) {
    toVerify.accept(this);
  }

  @Override
  public void afterTraversal(Definition definition) {
    super.afterTraversal(definition);
    definition.verify();
  }

  @Override
  public void visit(DummyAbi dummyAbi) {

  }


  /**
   * Calls the verify method on all nodes in the given subgraph.
   */
  public static class Graph extends GraphProcessor<Node> {

    public static void verifySubGraph(Node node) {
      new Graph().processNode(node);
    }

    @Override
    protected Node processUnprocessedNode(Node toProcess) {
      // verify inputs
      toProcess.visitInputs(this);
      toProcess.verify();
      return toProcess;
    }
  }
}
