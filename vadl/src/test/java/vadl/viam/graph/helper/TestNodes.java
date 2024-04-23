package vadl.viam.graph.helper;

import java.util.List;
import vadl.javaannotations.viam.DataValue;
import vadl.javaannotations.viam.Input;
import vadl.javaannotations.viam.Successor;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.UniqueNode;

public class TestNodes {

  public static abstract class TestNode extends Node {
  }

  public static class Plain extends TestNode {

  }

  public static class PlainUnique extends TestNode implements UniqueNode {
  }

  public static class WithInput extends TestNode {

    public @Input Node input;

    public WithInput(Node input) {
      this.input = input;
    }

    @Override
    protected void collectInputs(List<Node> collection) {
      super.collectInputs(collection);
      collection.add(input);
    }

    @Override
    public void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
      super.applyOnInputsUnsafe(visitor);
      input = visitor.apply(this, input);
    }
  }

  public static class WithInputUnique extends WithInput implements UniqueNode {

    public WithInputUnique(Node input) {
      super(input);
    }
  }

  public static class WithSuccessor extends TestNode {

    public @Successor Node successor;

    public WithSuccessor(Node successor) {
      this.successor = successor;
    }

    @Override
    protected void collectSuccessors(List<Node> collection) {
      super.collectSuccessors(collection);
      collection.add(successor);
    }
  }

  public static class WithSuccessorUnique extends WithSuccessor implements UniqueNode {

    public WithSuccessorUnique(Node successor) {
      super(successor);
    }
  }

  public static class WithTwoInputs extends TestNode {

    public @Input Node input1;
    public @Input Node input2;

    public WithTwoInputs(Node input1, Node input2) {
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
    public void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
      super.applyOnInputsUnsafe(visitor);
      input1 = visitor.apply(this, input1);
      input2 = visitor.apply(this, input2);
    }
  }

  public static class WithTwoInputsUnique extends WithTwoInputs implements UniqueNode {

    public WithTwoInputsUnique(Node input1, Node input2) {
      super(input1, input2);
    }
  }

  public static class WithData extends TestNode {
    public @DataValue int val;

    public WithData(int val) {
      this.val = val;
    }

    @Override
    protected void collectData(List<Object> collection) {
      super.collectData(collection);
      collection.add(val);
    }
  }

  public static class WithDataUnique extends WithData implements UniqueNode {
    public WithDataUnique(int val) {
      super(val);
    }
  }

}
