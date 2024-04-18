package vadl.viam.graph.helper;

import java.util.List;
import vadl.javaannotations.viam.Input;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;

public class TestNodeWithInput extends Node {

  public @Input Node input;

  TestNodeWithInput(Node input) {
    this.input = input;
  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    collection.add(input);
  }

  @Override
  public void applyOnInputs(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputs(visitor);
    input = visitor.apply(this, input);
  }
}
