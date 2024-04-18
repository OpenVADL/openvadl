package vadl.viam.graph.helper;

import java.util.List;
import vadl.javaannotations.viam.Successor;
import vadl.viam.graph.Node;

public class TestNodeWithSuccessor extends Node {

  public @Successor Node successor;

  public TestNodeWithSuccessor(Node successor) {
    this.successor = successor;
  }

  @Override
  protected void collectSuccessors(List<Node> collection) {
    super.collectSuccessors(collection);
    collection.add(successor);
  }
}
