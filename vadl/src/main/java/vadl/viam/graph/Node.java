package vadl.viam.graph;

import com.google.errorprone.annotations.FormatMethod;
import com.google.errorprone.annotations.FormatString;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;

/**
 * The base node class of the VIAM {@link Graph}.
 *
 * <p>It provides extensive API to manipulate the graph and
 * contains implicitly updated information like predecessor
 * and usages.
 */
public abstract class Node {

  public final Id id;
  private @Nullable Graph graph;


  private ArrayList<Node> usages;
  private @Nullable Node predecessor;

  /**
   * Construction a new node with the initial id,
   * no usages and no predecessor.
   */
  public Node() {
    this.id = new Id();
    this.usages = new ArrayList<>();
    this.predecessor = null;
  }

  public Id id() {
    return this.id;
  }

  public boolean isActive() {
    return this.id.isActive();
  }

  public boolean isActiveIn(@Nullable Graph graph) {
    return this.isActive() && graph == this.graph;
  }

  public boolean isDeleted() {
    return this.id.isDeleted();
  }

  public boolean isUninitialized() {
    return this.id.isInit();
  }


  /**
   * A stream of all inputs of this node.
   */
  public final Stream<Node> inputs() {
    return this.inputList().stream();
  }

  /**
   * A stream of all inputs of this node that are of type T.
   */
  public final <T extends Node> Stream<Node> input(Class<T> clazz) {
    return inputs().filter(clazz::isInstance);
  }

  /**
   * Collects all successors in the provided list.
   *
   * <p><b>IMPORTANT</b>:
   * <li>This must be overridden by every node that has inputs
   * (annotated with {@link vadl.javaannotations.viam.Input}).</li>
   * <li>The subclass must call {@code super.collectInputs(collection)} before
   * adding its own inputs!</li>
   * <li>Optional successors must only be added if they are non-null</li>
   *
   * @param collection to add the successors to.
   */
  protected void collectInputs(List<Node> collection) { /* nothing to add */
  }

  protected final List<Node> inputList() {
    var collection = new ArrayList<Node>();
    collectInputs(collection);
    return collection;
  }

  /**
   * Returns an iterable of all successor edges of this node.
   */
  public final Stream<Node> successors() {
    return successorList().stream();
  }


  /**
   * Collects all successors in the provided list.
   *
   * <p><b>IMPORTANT</b>:
   * <li>This must be overridden by every node that has successors
   * (annotated with {@link vadl.javaannotations.viam.Successor})</li>
   * <li>The subclass must call {@code super.collectSuccessors(collection)} before
   * adding its own successors!</li>
   * <li>Optional successors must only be added if they are non-null</li>
   *
   * @param collection to add the successors to.
   */
  protected void collectSuccessors(List<Node> collection) { /* nothing to add */
  }


  protected final List<Node> successorList() {
    var collection = new ArrayList<Node>();
    collectSuccessors(collection);
    return collection;
  }

  /**
   * Collects all data in the provided list.
   *
   * <p><b>IMPORTANT</b>:
   * <li>This must be overridden by every node that has data
   * (annotated with {@link vadl.javaannotations.viam.DataValue})</li>
   * <li>The subclass must call {@code super.collectData(collection)} before
   * adding its own data!</li>
   * <li>Optional successors must be added even if they might be null!</li>
   *
   * @param collection to add the successors to.
   */
  protected void collectData(List<Object> collection) { /* nothing to add */
  }

  protected final List<Object> dataList() {
    var collection = new ArrayList<>();
    collectData(collection);
    return collection;
  }

  /**
   * Returns a stream of all {@link Node}s that have this node as
   * {@link vadl.javaannotations.viam.Input}.
   */
  public final Stream<Node> usages() {
    return usages.stream();
  }

  /**
   * Returns the {@link Node} that has this node as {@link vadl.javaannotations.viam.Successor}.
   */
  public final @Nullable Node predecessor() {
    return predecessor;
  }

  public final @Nullable Graph graph() {
    return this.graph;
  }

  public final int usageCount() {
    return usages.size();
  }


  /**
   * Applies visitor output on all inputs.
   * This is unsafe, as it may lead to an inconsistent graph, if the usages are not
   * updated accordingly. Use {@link Node#applyOnInputs(GraphVisitor.Applier)} to
   * let this be handled automatically.
   *
   * <p><b>IMPORTANT</b>:
   * <li>This must be overridden by every node that has inputs
   * (annotated with {@link vadl.javaannotations.viam.Input}).</li>
   * <li>The subclass must call {@code super.applyOnInputs(visitor)} before
   * adding its own inputs!</li>
   *
   * @param visitor that produces new value for input.
   */
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    // default none, must be overridden by subtypes
  }

  /**
   * Applies the visitor's output on each input of this node.
   * If the new input node differs from the old one, this method will automatically handle
   * the usage transfer.
   */
  public void applyOnInputs(GraphVisitor.Applier<Node> visitor) {
    applyOnInputsUnsafe((s, input) -> {
      var newInput = visitor.applyNullable(s, input);
      if (newInput != null) {
        transferUsageOfThis(input, newInput);
      } else {
        if (input != null) {
          input.removeUsage(this);
        }
      }
      return newInput;
    });
  }

  /**
   * For each input of the node it calls {@code visitor.visit(node, input)}.
   *
   * @param visitor the visitor that gets visited
   */
  public void visitInputs(GraphVisitor visitor) {
    applyOnInputsUnsafe((from, to) -> {
      visitor.visit(from, to);
      return to;
    });
  }

  /**
   * Initializes the node for the given graph.
   * This is typically called by the graph itself.
   */
  protected void initialize(Graph graph) {
    graph.include(this);
    this.graph = graph;
    inputs().forEach(e -> e.addUsage(this));
    successors().forEach(e -> e.setPredecessor(this));
  }

  /**
   * Deletes this node in a safe way, such that all inputs and
   * successors remain consistent after deletion is done.
   *
   * @throws ViamGraphError if some other node still uses this as input or
   *                        successor
   */
  public void safeDelete() {
    ensureDeleteIsPossible();
    clearInputsUsageOfThis();
    clearSuccessorsUsageOfThis();
    if (graph != null) {
      graph.remove(this);
    }
  }

  /**
   * Replaces the old input with the new input in the graph by modifying the
   * input edges of the nodes.
   * If the replacement is successful, the usage of the old input is
   * transferred to the new input.
   *
   * @param oldInput the node to be replaced
   * @param newInput the node to replace the old input with
   */
  public void replaceInput(Node oldInput, Node newInput) {
    AtomicBoolean replaced = new AtomicBoolean(false);
    applyOnInputsUnsafe((self, input) -> {
      if (input == oldInput) {
        replaced.set(true);
        return newInput;
      } else {
        return input;
      }
    });

    if (replaced.get()) {
      if (isUninitialized()) {
        // if this node is not yet initialized, we will add it as usage
        // to the new node. This is done as asoon as this node gets added to the graph
        oldInput.removeUsage(this);
      } else {
        transferUsageOfThis(oldInput, newInput);
      }
    }
  }

  protected void addUsage(Node usage) {
    ensure(usage.isActiveIn(graph), "usage must be active: %s", usage);
    usages.add(usage);
  }

  protected final void removeUsage(Node usage) {
    usages.remove(usage);
  }

  protected final void setPredecessor(@Nullable Node predecessor) {
    this.predecessor = predecessor;
  }

  /**
   * Removes this as usage from the {@code from} node and adds this
   * as usage to the {@code to} node.
   *
   * @param from node that gets {@code this} removed
   * @param to   node that gets {@code this} added
   */
  public final void transferUsageOfThis(@Nullable Node from, Node to) {
    ensure(isActive(), "node must be active on usage transfer");
    ensure(this.id.isInit() || to.isActiveIn(graph),
        "cannot transfer usage to inactive node %s", to);
    if (from == to) {
      return;
    }
    if (from != null) {
      from.removeUsage(this);
    }
    to.addUsage(this);
  }

  /**
   * This checks if the inputs are equal to the other
   * node `o`.
   *
   * @return true if all inputs are equal, false otherwise.
   */
  protected boolean equalInputs(Node o) {
    var thisIter = inputs().iterator();
    var otherIter = o.inputs().iterator();
    while (thisIter.hasNext() && otherIter.hasNext()) {
      var thisInput = thisIter.next();
      var otherInput = otherIter.next();
      if (!thisInput.equals(otherInput)) {
        return false;
      }
    }
    return !thisIter.hasNext() && !otherIter.hasNext();
  }

  protected boolean equalData(Node o) {
    return dataList().equals(o.dataList());
  }

  /**
   * Returns the name of the type of node.
   *
   * @return name of node
   */
  public String nodeName() {
    // uses the class name and removes the "Node" suffix if existing
    var className = this.getClass().getSimpleName();
    var postfix = "Node";
    if (className.endsWith(postfix)) {
      return className.substring(0, className.length() - postfix.length());
    }
    return className;
  }

  @Override
  public String toString() {
    var dataList = dataList();
    var data = !dataList.isEmpty()
        ? "<%s>".formatted(
        dataList.stream().map(Object::toString).collect(Collectors.joining(", "))) :
        "";
    return "(%s) %s%s".formatted(id, nodeName(), data);
  }


  /**
   * Removes {@code this} from the usages of all of its inputs.
   */
  private void clearInputsUsageOfThis() {
    ensure(isActive(), "node must be active on input clear");
    inputs().forEach(e -> e.removeUsage(this));
  }

  /**
   * Removes {@code this} as predecessor from all successor nodes.
   */
  private void clearSuccessorsUsageOfThis() {
    ensure(isActive(), "node must be active on successor clear");
    successors().forEach(e -> e.setPredecessor(null));
  }

  /// GRAPH VERIFICATION METHODS

  /**
   * Verifies the consistency of this node to all its
   * inputs, successors, usages and predecessor.
   */
  public final void verify() {
    ensure(isActive(), "node is not active");
    ensure(graph != null, "graph of node is null");

    verifyAllEdges();
  }

  private void verifyAllEdges() {
    inputs().forEach(this::verifyInput);
    usages().forEach(this::verifyUsage);
    successors().forEach(this::verifySuccessor);
    verifyPredecessor();
  }

  private void verifySuccessor(Node successor) {
    ensure(successor != null,
        "successor is null, but currently no optional successors are supported!");
    ensure(successor.isActive(), "successor is not active %s", successor);
    ensure(successor.graph() == graph, "successor is not in same graph %s", successor);

    // check if input has this node registered as input
    ensure(successor.predecessor != null, "successor's predecessor is null %s", successor);
    ensure(successor.predecessor == this, "this node is not this %s", successor);
  }

  private void verifyPredecessor() {
    var pred = predecessor;
    if (pred == null) {
      return;
    }

    ensure(pred.isActive(), "predecessor is not active %s", pred);
    ensure(pred.graph() == graph, "predecessor is not in same graph %s", pred);

    // check if input has this node registered as input
    ensure(pred.successorList().contains(this),
        "Predecessor's successors do not contain this node %s", pred);
  }

  private void verifyInput(Node input) {
    ensure(input != null, "input is null, but currently no optional inputs are supported!");
    ensure(input.isActive(), "input is not active %s", input);
    ensure(input.graph() == graph, "input is not in same graph %s", input);

    // check if input has this node registered as input
    ensure(input.usages.contains(this), "this node is not a user of input %s", input);
  }

  private void verifyUsage(Node usage) {
    ensure(usage != null, "usage is null");
    ensure(usage.isActive(), "usage is not active %s", usage);
    ensure(usage.graph() == graph, "usage is in other graph %s", usage);

    var usageContainsThis = usage
        .inputList()
        .contains(this);

    ensure(usageContainsThis, "user does not contain this node as input %s", usage);
  }

  /// RUNTIME CHECK HELPERS

  @FormatMethod
  protected final void ensure(boolean condition, @FormatString String format,
                              @Nullable Object... args) {
    if (!condition) {
      throw new ViamGraphError(format, args)
          .addContext(this)
          .addContext(this.graph)
          .shrinkStacktrace(1);
    }
  }

  protected final void ensureDeleteIsPossible() {
    ensure(isActive(), "cannot delete: node is not active");
    ensure(this.usages.isEmpty(), "cannot delete: user of this node exist");
    ensure(predecessor == null, "cannot delete: predecessor exist");
  }

  /**
   * The node's id class. It can be in Init, Active and Deleted state.
   */
  public class Id {
    private IdState state;
    private int numericId;

    private Id() {
      state = IdState.INIT;
      numericId = -1;
    }

    boolean isInit() {
      return state == IdState.INIT;
    }

    boolean isActive() {
      return state == IdState.ACTIVE;
    }

    boolean isDeleted() {
      return state == IdState.DELETED;
    }

    protected void turnActive(int numericId) {
      ensure(state == IdState.INIT, "cannot turn active from %s state", state);
      this.state = IdState.ACTIVE;
      this.numericId = numericId;
    }

    protected void turnDeleted() {
      ensure(state == IdState.ACTIVE, "cannot turn deleted from %s state", state);
      this.state = IdState.DELETED;
    }

    protected int numericId() {
      ensure(state != IdState.INIT, "id in Init state has no numeric id");
      return numericId;
    }

    private enum IdState {
      INIT,
      ACTIVE,
      DELETED
    }


    @Override
    public String toString() {
      return switch (state) {
        case INIT -> "i";
        case ACTIVE -> "" + numericId;
        case DELETED -> "d(%s)".formatted(numericId);
      };
    }
  }

}
