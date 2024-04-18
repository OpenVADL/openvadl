package vadl.viam.graph.helper;

import java.util.List;
import vadl.javaannotations.viam.Input;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;

public class TestNodeWithTwoInputs extends Node {

  public @Input Node input1;
  public @Input Node input2;

  public TestNodeWithTwoInputs(Node input1, Node input2) {
    this.input1 = input1;
    this.input2 = input2;
  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    collection.add(input1);
    collection.add(input2);
  }

  @Override
  public void applyOnInputs(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputs(visitor);
    input1 = visitor.apply(this, input1);
    input2 = visitor.apply(this, input2);
  }
}
