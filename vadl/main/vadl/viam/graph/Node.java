package vadl.viam.graph;

import com.google.errorprone.annotations.FormatMethod;
import com.google.errorprone.annotations.FormatString;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.jetbrains.annotations.Contract;
import vadl.utils.SourceLocation;
import vadl.viam.graph.dependency.DependencyNode;

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

  private SourceLocation sourceLocation;

  /**
   * Construction a new node with the initial id,
   * no usages and no predecessor.
   */
  public Node() {
    this.id = new Id();
    this.usages = new ArrayList<>();
    this.predecessor = null;
    this.sourceLocation = SourceLocation.INVALID_SOURCE_LOCATION;
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

  public SourceLocation sourceLocation() {
    return sourceLocation;
  }

  public void setSourceLocation(SourceLocation sourceLocation) {
    this.sourceLocation = sourceLocation;
  }

  /**
   * Sets the source location of the node if it wasn't already set.
   */
  public void setSourceLocationIfNotSet(SourceLocation sourceLocation) {
    if (this.sourceLocation == SourceLocation.INVALID_SOURCE_LOCATION) {
      this.sourceLocation = sourceLocation;
    }
  }

  /**
   * Checks if the node is a leaf node, such that it a {@link DependencyNode} and
   * has no further inputs.
   *
   * @return true if the node is a leaf node, false otherwise
   */
  public final boolean isLeaf() {
    if (!(this instanceof DependencyNode)) {
      return false;
    }

    return inputList().isEmpty();
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


  /**
   * Collects all successors in the provided list and it's children's successors.
   *
   * @param collection to add the successors to.
   */
  public final void collectInputsWithChildren(List<Node> collection) {
    var sublist = new ArrayList<Node>();
    this.collectInputs(sublist);
    collection.addAll(sublist);

    // Only iterate over the newly visited inputs and ignore the rest.
    sublist.forEach(input -> input.collectInputsWithChildren(collection));
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

  public final boolean hasUsages() {
    return usageCount() != 0;
  }


  /**
   * Applies visitor output on all inputs.
   * This is unsafe, as it may lead to an inconsistent graph, if the usages aren’t
   * updated accordingly. Use {@link Node#applyOnInputs(GraphVisitor.Applier)} to
   * let this be handled automatically.
   *
   * <p><b>IMPORTANT</b>:
   * <li>This must be overridden by every node that has inputs
   * (annotated with {@link vadl.javaannotations.viam.Input}).</li>
   * <li>The subclass must call {@code super.applyOnInputsUnsafe(visitor)} before
   * adding its own inputs!</li>
   *
   * @param visitor that produces new value for input.
   */
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    // default none, must be overridden by subtypes
  }

  /**
   * Applies visitor output on all successors.
   * This is unsafe, as it may lead to an inconsistent graph, if the predecessors aren’t
   * updated accordingly. Use {@link Node#applyOnSuccessors(GraphVisitor.Applier)} to
   * let this be handled automatically.
   *
   * <p><b>IMPORTANT</b>:
   * <li>This must be overridden by every node that has successors
   * (annotated with {@link vadl.javaannotations.viam.Successor}).</li>
   * <li>The subclass must call {@code super.applyOnSuccessorsUnsafe(visitor)} before
   * adding its own inputs!</li>
   *
   * @param visitor that produces new value for input.
   */
  protected void applyOnSuccessorsUnsafe(GraphVisitor.Applier<Node> visitor) {
    // subtypes must override default
  }


  /**
   * Applies the visitor's output on each input of this node.
   * If the new input node differs from the old one, this method will automatically handle
   * the usage transfer.
   *
   * <p>An example that replaces a specific input. Note that there is a dedicate method
   * for that purpose: {@link #replaceInput(Node, Node)}<pre>
   * node.applyOnInputs((from, toInput) -> {
   *  if (toInput == toReplace) {
   *    // we found the input to replace
   *    return replacee;
   *  }
   *  // we must apply the old value to leave it the same
   *  return toInput;
   * });
   * </pre></p>
   */
  public final void applyOnInputs(GraphVisitor.Applier<Node> visitor) {
    applyOnInputsUnsafe((self, oldInput) -> {
      var newInput = visitor.applyNullable(self, oldInput);
      if (newInput == oldInput) {
        // if nothing changes just return newInput
        return newInput;
      }

      // transfer usage of this from the old input to the new input
      // -> the old input will not have this node as usage anymore
      // -> the new input will have this node as usage
      updateUsageOf(oldInput, newInput);

      return newInput;
    });
  }

  /**
   * Applies the visitor's output on each successor of this node.
   * If the new successor node differs from the old one, this method will automatically handle
   * the usage transfer.
   */
  public final void applyOnSuccessors(GraphVisitor.Applier<Node> visitor) {
    applyOnSuccessorsUnsafe((self, oldSucc) -> {
      // produce new successor
      var newSucc = visitor.applyNullable(self, oldSucc);

      // set the predecessor of oldSucc to null
      // and the predecessor of newSucc to this
      updatePredecessorOf(oldSucc, newSucc);

      return newSucc;
    });
  }

  /**
   * For each input of the node it calls {@code visitor.visit(node, input)}.
   *
   * @param visitor the visitor that gets visited
   */
  public final void visitInputs(GraphVisitor visitor) {
    for (var input : inputs().toList()) {
      visitor.visit(this, input);
    }
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
    deleteObsoleteChildrenOf();
  }

  /**
   * Delete all children (inputs and successors) that aren’t used (no usages and predecessor).
   */
  private void deleteObsoleteChildrenOf() {
    ensure(isDeleted(), "Deletion of obsolete children is only possible for deleted nodes");
    var deletedNode = this;
    Consumer<Node> tryToDelete = (Node i) -> {
      var usagesOk = i.usages.isEmpty()
          || (i.usageCount() == 1 && i.usages.get(0) == deletedNode);
      var predecessorOk = i.predecessor == null || i.predecessor == deletedNode;
      if (usagesOk && predecessorOk && i.isActiveIn(graph)) {
        i.safeDelete();
      }
    };

    deletedNode.inputs()
        .forEach(tryToDelete);
    deletedNode.successors()
        .forEach(tryToDelete);
  }

  /**
   * Replaces this node by the given node.
   * If the given node is uninitialized, it is getting added to the graph first.
   *
   * <p>This method also handles replacement of usages and predecessor, so no further
   * handling is required.</p>
   *
   * @param replacement node that replaces this node
   * @return the replacement node.
   *     This might be useful if the replacement is not yet added to the graph, as the
   *     new node might be a different object
   */
  public <T extends Node> T replaceAndDelete(T replacement) {
    replacement.setSourceLocationIfNotSet(this.sourceLocation);
    if (replacement.isUninitialized() && graph != null) {
      replacement = graph.addWithInputs(replacement);
      if (replacement == this) {
        // as the graph might return the same node
        // again, we have to check if the new node is the same as this node.
        return (T) this;
      }
    }
    checkReplaceWith(replacement);
    replaceAtAllUsages(replacement);
    replaceAtPredecessor(replacement);
    this.safeDelete();
    return replacement;
  }

  /**
   * Replaces this node with the given one at all usages.
   * I.e., every node that uses this node as input will afterwards use the replacement as input.
   *
   * @param replacement new input of this node's usages.
   */
  public void replaceAtAllUsages(Node replacement) {
    checkReplaceWith(replacement);
    for (var u : this.usages().toList()) {
      u.replaceInput(this, replacement);
    }
  }

  /**
   * Sets the replacement as successor of this node's predecessor.
   * So the node that has this node as successor will use the replacement node as successor
   * after the operation is done.
   *
   * @param replacement that replaces this node.
   */
  public void replaceAtPredecessor(Node replacement) {
    checkReplaceWith(replacement);
    if (predecessor != null) {
      // Replace the successor of predecessor
      // so predecessors successor will be replacement instead of this
      predecessor.applyOnSuccessors((pred, succ) -> {
        if (succ == this) {
          return replacement;
        }
        return succ;
      });
      updatePredecessorOf(this, replacement);
    }
  }


  /**
   * Checks if it is valid to replace the node with the given node.
   */
  private boolean checkReplaceWith(Node node) {
    ensure(node != this, "cannot replace node with itself");
    ensure(!isDeleted(), "cannot replace deleted node");
    ensure(node.isActiveIn(graph), "node to replace must be active in same graph");
    return true;
  }

  /**
   * Replaces the old input with the new input in the graph by modifying the
   * input edges of the nodes.
   * If the replacement is successful, the usage of the old input is
   * transferred to the new input.
   * If the old input has no usages then delete it.
   *
   * @param oldInput the node to be replaced
   * @param newInput the node to replace the old input with
   */
  public void replaceInput(Node oldInput, Node newInput) {
    applyOnInputs((self, input) -> {
      if (input == oldInput) {
        return newInput;
      } else {
        return input;
      }
    });
  }


  /**
   * Replaces the given old successor of this node, by the new one.
   */
  public void replaceSuccessor(Node oldSuccessor, @Nullable Node newSuccessor) {
    applyOnSuccessors((self, successor) -> {
      if (successor == oldSuccessor) {
        return newSuccessor;
      }
      return successor;
    });
  }


  public void addUsage(Node usage) {
    usages.add(usage);
  }

  /**
   * Removes the given node of the set of usages from this node.
   *
   * @param usage the node to remove from usages
   * @return if there was something found to remove
   */
  public final boolean removeUsage(Node usage) {
    // we remove only the first occurrence of the given usage
    // even if the usages uses this node multiple times.
    // This is because a general pass algorithm would iterate over all inputs,
    // so removing all of them on the first occurrence would lead to an inconsistent state
    // were this is an input of some node, but it is not contained in the usages list
    return usages.remove(usage);
  }

  protected final void setPredecessor(@Nullable Node predecessor) {
    this.predecessor = predecessor;
  }

  /**
   * Removes {@code this} form usage list of the {@code oldInput} and
   * adds {@code this} this to the usage list of the {@code newInput}.
   *
   * <p>Below is the operation effect for the Usage-Relation:
   * <pre>
   *  Before:      This Node          |   After:    This Node
   *                /                 |                     \
   *               -                  |                      -
   *            oldInput    newInput  |         oldInput     newInput
   * </pre>
   *
   * @param oldInput will <b>not</b> have {@code this} as a usage after operation completed
   * @param newInput will have {@code this} as a usage after operation completed
   */
  protected final void updateUsageOf(@Nullable Node oldInput, @Nullable Node newInput) {
    ensure(isActive(), "node must be active on usage transfer");
    ensure(newInput == null || newInput.isActiveIn(graph),
        "cannot transfer usage to inactive node %s", newInput);
    if (oldInput != newInput) {
      if (oldInput != null && !oldInput.isUninitialized()) {
        boolean result = oldInput.removeUsage(this);
        ensure(result, "Old input (%s) hadn't a usage of this", oldInput);
      }
      if (newInput != null) {
        newInput.addUsage(this);
      }
    }
  }

  /**
   * Sets the predecessor of the {@code oldSucc} to null and
   * sets the predecessor of {@code newSucc} to {@code this}.
   *
   * <p>Below is the operation effect for the Predecessor-Relation:
   * <pre>
   *  Before:      This Node          |   After:    This Node
   *                /                 |                     \
   *               -                  |                      -
   *            oldSucc      newSucc  |         oldSucc     newSucc
   * </pre>
   *
   * @param oldSuccessor will <b>not</b> have {@code this} as the predecessor after operation
   *                     completed
   * @param newSuccessor will have {@code this} as the predecessor after operation completed
   */
  protected void updatePredecessorOf(@Nullable Node oldSuccessor, @Nullable Node newSuccessor) {
    ensure(isActive() && (newSuccessor == null || newSuccessor.isActiveIn(graph())),
        "failed to set remove %s as predecessor from %s and adding it to %s", this, oldSuccessor,
        newSuccessor);
    if (oldSuccessor != newSuccessor) {
      if (oldSuccessor != null) {
        ensure(newSuccessor == null || oldSuccessor.predecessor == this,
            "the old successor (%s) of this node has another predecessor: %s", oldSuccessor,
            oldSuccessor.predecessor);
        oldSuccessor.setPredecessor(null);
      }
      if (newSuccessor != null) {
        ensure(newSuccessor.predecessor == null,
            "the new successor (%s) already has a predecessor: %s", newSuccessor,
            newSuccessor.predecessor);
        newSuccessor.setPredecessor(this);
      }
    }
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
    inputs().forEach(e -> e.removeUsage(this)
    );
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
   * inputs, successors, usages, and predecessor.
   */
  public final void verify() {
    ensure(isActive(), "node is not active");
    ensure(graph != null, "graph of node is null");

    verifyAllEdges();
    verifyState();
  }

  /**
   * Creates a copy from {@code this} node and returns it. It will also copy all
   * data values recursively.
   */
  public abstract Node copy();

  /**
   * Indicates whether the node has commutative inputs.
   *
   * @return true if the inputs can be rearranged.
   */
  public boolean isCommutative() {
    return false;
  }

  /**
   * Creates a copy from {@code this} node and returns it. It will *NOT* copy all
   * data values recursively. Instead, it will use the old objects.
   */
  public abstract Node shallowCopy();

  /**
   * Defines assertions that ensure a consistent state
   * of the node.
   *
   * <p>This should be overridden by all nodes that have something to be checked against.</p>
   */
  public void verifyState() {
  }

  /**
   * Accepting method for the {@link GraphNodeVisitor}.
   */
  public abstract <T extends GraphNodeVisitor> void accept(T visitor);

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

  /**
   * Ensures that a given condition is true. If the condition is false, an exception is thrown
   * with the provided format string and arguments.
   *
   * <p>The thrown exception has context information about the node and graph.</p>
   *
   * @param condition the condition to check
   * @param format    the format string for the exception message
   * @param args      the arguments to replace in the format string
   * @throws ViamGraphError if the condition is false
   */
  @FormatMethod
  @Contract("false, _, _-> fail")
  public final void ensure(boolean condition, @FormatString String format,
                           @Nullable Object... args) {
    if (!condition) {
      throw new ViamGraphError(format, args)
          .addContext(this)
          .addContext(this.graph)
          .shrinkStacktrace(1);

    }
  }

  /**
   * Ensures that the given object is not null. If the object is null, an exception is thrown
   * with the specified message.
   *
   * <p>The thrown exception has context information about the node and graph.</p>
   *
   * @param obj the object to check for null
   * @param msg the message to include in the exception if the object is null
   * @throws ViamGraphError if the object is null
   */
  @Contract("null, _  -> fail")
  @FormatMethod
  public final void ensureNonNull(@Nullable Object obj, String msg) {
    ensure(obj != null, msg);
  }

  protected final void ensureDeleteIsPossible() {
    ensure(isActive(), "cannot delete: node is not active");
    ensure(this.usages.isEmpty(), "cannot delete: user of this node exist");
    ensure(predecessor == null, "cannot delete: predecessor exist");
  }

  /**
   * Clears all the usages from the {@link Node}.
   * This is helpful when adding a node from another graph. An
   * empty list of usages indicates that the node is a root node.
   */
  public void clearUsages() {
    usages.clear();
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

    /**
     * When copying a {@link Node} it must be possible to reset the id so the node can added to
     * a different graph.
     *
     * @deprecated as it leaves the node in an inconsistent state within the graph.
     *     Take a look at {@link Graph#deinitializeNodes()} for more information.
     */
    @Deprecated
    public void deactivate() {
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

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Id id = (Id) o;
      return numericId == id.numericId && state == id.state;
    }

    @Override
    public int hashCode() {
      return Objects.hash(state, numericId);
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
