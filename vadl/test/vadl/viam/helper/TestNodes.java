package vadl.viam.helper;

import java.util.List;
import java.util.stream.Collectors;
import vadl.javaannotations.viam.DataValue;
import vadl.javaannotations.viam.Input;
import vadl.javaannotations.viam.Successor;
import vadl.viam.graph.GraphEdgeVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.UniqueNode;

/**
 * TestNodes is a class that contains several nested classes
 * representing different types of test nodes.
 * These nodes are used for testing purposes in a graph structure.
 */
public class TestNodes {


  /**
   * The TestNode class is an abstract class that represents a node in a test graph structure.
   * It extends the Node class.
   */
  public abstract static class TestNode extends Node {
  }

  /**
   * The Plain class represents a plain test node in a graph structure.
   * It extends the TestNode class.
   */
  public static class Plain extends TestNode {

    @Override
    public Node copy() {
      return new Plain();
    }

    @Override
    public Node shallowCopy() {
      return new Plain();
    }
  }

  /**
   * The PlainUnique class represents a node in a test graph structure
   * that is marked as unique.
   * It extends the TestNode class and implements the UniqueNode interface.
   *
   * @see UniqueNode
   */
  public static class PlainUnique extends TestNode implements UniqueNode {
    @Override
    public Node copy() {
      return new PlainUnique();
    }

    @Override
    public Node shallowCopy() {
      return new PlainUnique();
    }
  }

  /**
   * The WithInput class is a subclass of TestNode that represents a node with an input field.
   * It extends the TestNode class and adds an input field marked with the @Input annotation.
   * When creating an instance of WithInput, a Node input object must be provided.
   */
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
    public void applyOnInputsUnsafe(GraphEdgeVisitor.Applier<Node> visitor) {
      super.applyOnInputsUnsafe(visitor);
      input = visitor.apply(this, input);
    }

    @Override
    public Node copy() {
      return new WithInput(input.copy());
    }

    @Override
    public Node shallowCopy() {
      return new WithInput(input);
    }
  }

  /**
   * This class represents a subclass of TestNode that represents a node with an input field.
   * It extends the TestNode class and adds an input field marked with the @Input annotation.
   *
   * @see WithInput
   * @see UniqueNode
   */
  public static class WithInputUnique extends WithInput implements UniqueNode {

    public WithInputUnique(Node input) {
      super(input);
    }
  }

  /**
   * The WithSuccessor class represents a test node that has a successor node.
   * It extends the TestNode class.
   */
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

    @Override
    public Node copy() {
      return new WithSuccessor(successor.copy());
    }

    @Override
    public Node shallowCopy() {
      return new WithSuccessor(successor);
    }
  }

  /**
   * The WithSuccessorUnique class represents a test node that has a successor node
   * and is marked as unique in the graph.
   * It extends the WithSuccessor class and implements the UniqueNode interface.
   */
  public static class WithSuccessorUnique extends WithSuccessor implements UniqueNode {

    public WithSuccessorUnique(Node successor) {
      super(successor);
    }
  }

  /**
   * The WithTwoInputs class is a subclass of TestNode that represents a node with two input nodes.
   * It stores references to the input nodes and provides methods to interact with them.
   */
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
    public void applyOnInputsUnsafe(GraphEdgeVisitor.Applier<Node> visitor) {
      super.applyOnInputsUnsafe(visitor);
      input1 = visitor.apply(this, input1);
      input2 = visitor.apply(this, input2);
    }

    @Override
    public Node copy() {
      return new WithTwoInputs(input1.copy(), input2.copy());
    }

    @Override
    public Node shallowCopy() {
      return new WithTwoInputs(input1, input2);
    }
  }


  /**
   * The WithTwoInputsUnique class is a subclass of WithTwoInputs that represents a
   * unique node with two input nodes.
   * It implements the UniqueNode interface, which marks the node as unique in the graph.
   */
  public static class WithTwoInputsUnique extends WithTwoInputs implements UniqueNode {

    public WithTwoInputsUnique(Node input1, Node input2) {
      super(input1, input2);
    }
  }

  /**
   * The WithNodeListInput class is a subclass of TestNode that represents a node
   * with arbitrary many input nodes.
   */
  public static class WithNodeListInput extends TestNode {

    public @Input NodeList<Node> inputs;

    public WithNodeListInput(NodeList<Node> inputs) {
      this.inputs = inputs;
    }

    @Override
    protected void collectInputs(List<Node> collection) {
      super.collectInputs(collection);
      collection.addAll(inputs);
    }

    @Override
    public void applyOnInputsUnsafe(GraphEdgeVisitor.Applier<Node> visitor) {
      super.applyOnInputsUnsafe(visitor);
      inputs = inputs.stream()
          .map(e -> visitor.apply(this, e))
          .collect(Collectors.toCollection(NodeList::new));
    }

    @Override
    public Node copy() {
      return new WithNodeListInput(
          new NodeList<>(this.inputs.stream().map(Node::copy).toList()));
    }

    @Override
    public Node shallowCopy() {
      return new WithNodeListInput(this.inputs);
    }
  }


  /**
   * The WithNodeListInputUnique class is a subclass of WithNodeListInput that represents a
   * unique node with arbitrary many nodes.
   * It implements the UniqueNode interface, which marks the node as unique in the graph.
   */
  public static class WithNodeListInputUnique extends WithNodeListInput implements UniqueNode {

    public WithNodeListInputUnique(NodeList<Node> inputs) {
      super(inputs);
    }
  }

  /**
   * The WithData class represents a node in a test graph structure that contains a data value.
   * It extends the TestNode class.
   */
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

    @Override
    public Node copy() {
      return new WithData(val);
    }

    @Override
    public Node shallowCopy() {
      return new WithData(val);
    }
  }

  /**
   * The WithDataUnique class represents a node in a test graph
   * structure that contains a data value.
   * It extends the WithData class and implements the UniqueNode interface.
   */
  public static class WithDataUnique extends WithData implements UniqueNode {
    public WithDataUnique(int val) {
      super(val);
    }
  }

}
