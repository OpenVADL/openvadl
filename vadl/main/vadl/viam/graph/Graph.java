package vadl.viam.graph;

import static java.util.Objects.requireNonNull;

import com.google.errorprone.annotations.FormatMethod;
import com.google.errorprone.annotations.FormatString;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import vadl.utils.SourceLocation;
import vadl.viam.graph.control.AbstractEndNode;
import vadl.viam.graph.control.ControlNode;
import vadl.viam.graph.control.BranchEndNode;
import vadl.viam.graph.control.InstrCallNode;
import vadl.viam.graph.control.InstrEndNode;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.control.StartNode;
import vadl.viam.graph.dependency.DependencyNode;
import vadl.viam.graph.dependency.FuncParamNode;
import vadl.viam.graph.dependency.ParamNode;
import vadl.viam.graph.dependency.SideEffectNode;
import vadl.viam.graph.visualize.DotGraphVisualizer;

/**
 * The VIAM graph represents an execution flow definition
 * in VADL. E.g. instructions, functions, ...
 *
 * <p>It consists of a set of nodes that build up the control flow
 * and data dependencies between each other.
 */
public class Graph {

  public final String name;
  private SourceLocation sourceLocation = SourceLocation.INVALID_SOURCE_LOCATION;

  ArrayList<Node> nodes;

  private static final int INITIAL_GRAPH_SIZE = 32;

  public Graph(String name) {
    this.name = name;
    this.nodes = new ArrayList<>(INITIAL_GRAPH_SIZE);
  }

  protected Graph(String name, ArrayList<Node> nodes) {
    this.name = name;
    this.nodes = nodes;
  }

  /**
   * Get all nodes in graph.
   *
   * @return iterable over all nodes of graph.
   */
  public final Stream<Node> getNodes() {
    return nodes.stream().filter(Objects::nonNull);
  }

  /**
   * Gets all nodes of a specific type of this graph.
   *
   * @param clazz of node type
   * @return iterable of all nodes of type clazz
   */
  public final <T> Stream<T> getNodes(Class<T> clazz) {
    return getNodes().filter(clazz::isInstance).map(clazz::cast);
  }

  /**
   * Gets all nodes of a specific type of this graph.
   *
   * @param clazz of node type
   * @return iterable of all nodes with one of the given types.
   */
  public final Stream<Node> getNodes(Set<Class> clazz) {
    return getNodes().filter(Objects::nonNull)
        .filter(x -> clazz.stream().anyMatch(y -> y.isInstance(x)));
  }

  /**
   * Adds the node to the graph (without its inputs).
   * If the node is a unique node, it will check for duplication int the graph.
   *
   * <p>Always use the return value for further usage of the node.
   * The following shows a <b>valid</b> usage:
   * <pre>
   * {@code
   * var node = new Node(...);
   * node = graph.add(node);
   * doSomethingWith(node);
   * }
   * </pre>
   * The following shows a <b>wrong</b> usage:
   * <pre>
   * {@code
   * var node = new Node(...);
   * graph.add(node);
   * doSomethingWith(node);
   * }
   * </pre>
   * </p>
   *
   * @param node to be added
   * @return the node added to the graph or its duplicate
   */
  public <T extends Node> T add(T node) {
    if (node instanceof UniqueNode) {
      return addUniqueInternal(node);
    } else {
      return addSimpleInternal(node);
    }
  }

  /**
   * Adds the given node with all its inputs to the graph.
   * If this or any input is a unique node, it will be only
   * added if there is no duplicate.
   *
   * <p>Always use the return value for further usage of the node.
   * The following shows <b>valid</b> usage:
   * <pre>
   * {@code
   * var node = new Node(...);
   * node = graph.add(node);
   * doSomethingWith(node);
   * }
   * </pre>
   * The following shows <b>wrong</b> usage:
   * <pre>
   * {@code
   * var node = new Node(...);
   * graph.add(node);
   * doSomethingWith(node);
   * }
   * </pre>
   * </p>
   *
   * @param node to be added
   * @return the node added to the graph or its duplicate
   */
  public <T extends Node> T addWithInputs(T node) {
    node.ensure(node.isUninitialized(), "Node to be added must not be initialized");
    node.applyOnInputsUnsafe((n, target) -> {
      if (target == null || target.isActive()) {
        return target;
      }

      target.setSourceLocationIfNotSet(node.sourceLocation());
      target.ensure(!target.isDeleted(), "cannot add deleted input node");
      var newT = addWithInputs(target);
      // just return the new target AND the node, as the original one is not initialized and thus
      // there must not be any usage transfer
      return newT;
    });
    return add(node);
  }

  /**
   * Finds a duplicated node in this graph, such that
   * it has the same inputs and data values.
   *
   * @param node the node to search for
   * @return the duplicated node if found
   */
  public <T extends Node> @Nullable T findDuplicate(T node) {
    // TODO: Implementation of this might cause performance issues
    // TODO: We should differ between leafs and none-leafs, and also maintain a leafs cache
    node.ensure(node instanceof UniqueNode, "Only UniqueNodes might be used to find duplicates");

    var inputs = node.inputList();

    // if it is leaf
    if (inputs.isEmpty()) {
      //noinspection unchecked
      return (T) getNodes(node.getClass())
          .filter(node::equalData)
          .findFirst()
          .orElse(null);
    }

    // if node is not yet in graph, the usage number of the input might be 0
    var minUsageNumber = node.graph() == null ? 0 : 1;
    var minCount = Integer.MAX_VALUE;
    Node minNode = null;

    for (Node input : inputs) {
      if (input.usageCount() <= minUsageNumber) {
        // there will be no duplicated node, as only this
        // node is user of some of its inputs
        return null;
      }

      if (input.usageCount() < minCount) {
        minCount = input.usageCount();
        minNode = input;
      }
    }

    requireNonNull(minNode);
    //noinspection unchecked
    return (T) minNode.usages()
        .filter(user ->
            user != node
                // check if class, input and data is the same
                // the input is just checked for same references, as
                // those must have been added before anyways (with unique).
                && node.getClass() == user.getClass()
                && node.equalInputs(user)
                && node.equalData(user)
                && user.graph() == this
        )
        .findFirst()
        .orElse(null);
  }

  /**
   * Returns if the graph represents an instruction.
   *
   * <p>A instruction must end with an {@link vadl.viam.graph.control.InstrEndNode}
   * and must not have any {@link FuncParamNode}s</p>
   */
  public boolean isInstruction() {
    return getNodes(FuncParamNode.class).findAny().isEmpty()
        && getNodes(InstrEndNode.class).count() == 1;
  }

  /**
   * Returns if the graph represents a pure function.
   *
   * <p>A pure function has no control flow (except for start and end),
   * must end with a return node and must have no side effects.
   *
   * @return true if the graph is a pure function and false otherwise.
   */
  public boolean isPureFunction() {
    var endNodes = getNodes(AbstractEndNode.class).toList();
    return endNodes.size() == 1 && endNodes.get(0) instanceof ReturnNode
        && getNodes(SideEffectNode.class).findAny().isEmpty();
  }

  /**
   * Returns if the graph represents a pseudo instruction body.
   *
   * <p>A pseudo instruction has a simple control flow consisting of a {@link StartNode},
   * arbitrary many {@link InstrCallNode} and a {@link BranchEndNode}. It must only reference
   * {@link FuncParamNode} but no other param nodes.</p>
   */
  public boolean isPseudoInstruction() {
    return getNodes(ControlNode.class).allMatch(
        e -> e instanceof InstrCallNode || e instanceof StartNode || e instanceof BranchEndNode
    ) && getNodes(ParamNode.class).allMatch(e -> e instanceof FuncParamNode);
  }

  /**
   * Includes the node in the graph and assigns a new id to it.
   * This is only called by the node itself.
   *
   * @param node to be included.
   */
  protected void include(Node node) {
    node.ensure(node.id.isInit(), "cannot include node that is other than in Init state");
    int id = nodes.size();
    nodes.add(node);
    node.id.turnActive(id);
  }

  /**
   * Removes the node from the graph.
   * This is only called by the node itself.
   *
   * @param node to be removed.
   */
  protected void remove(Node node) {
    node.ensure(node.isActiveIn(this), "node is not active in this graph %s", this);
    nodes.set(node.id().numericId(), null);
    node.id().turnDeleted();
  }

  // helper method to add node to graph
  private <T extends Node> T addSimpleInternal(T node) {
    node.ensure(node.isUninitialized(), "node is not uninitialized");
    // ensure that all input dependencies are already added
    // to the graph
    ensureInputsAdded(node);

    node.initialize(this);
    return node;
  }

  private <T extends Node> T addUniqueInternal(T node) {
    node.ensure(node.isUninitialized(), "node is not uninitialized");
    var result = findDuplicate(node);
    if (result != null) {
      return result;
    }
    return addSimpleInternal(node);
  }


  public void verify() {
    verifyNodes();
  }

  private void verifyNodes() {
    getNodes().forEach(Node::verify);
  }


  /**
   * Checks if all inputs were added to the graph.
   */
  private void ensureInputsAdded(Node node) {
    for (var input : node.inputList()) {
      if (!input.isActiveIn(this)) {
        throw new ViamGraphError(
            "Failed to add `%s` as its input node `%s` is not yet initialized. %s",
            node, input, "You might want use Graph#addWithInputs()")
            .addContext(node)
            .addContext(this)
            .addLocation(sourceLocation)
            .shrinkStacktrace(1);
      }
    }
  }

  public SourceLocation sourceLocation() {
    return sourceLocation;
  }

  public void setSourceLocation(SourceLocation sourceLocation) {
    this.sourceLocation = sourceLocation;
  }

  protected Graph createEmptyInstance(String name) {
    return new Graph(name);
  }

  /**
   * Copies the graph and returns it.
   */
  public Graph copy() {
    return copy(name);
  }

  /**
   * Copies the graph and returns it.
   *
   * @param name the name of the copied graph
   */
  public Graph copy(String name) {
    // The process of coping a graph:
    // 1. Make a shallow copy of each node in the graph.
    //    This will return an uninitialized new node that is linked (input/successor) to
    //    the existing node in the original graph.
    //    We store them in the cache, with the original node as the key and the new node as value.
    // 2. For every uninitialized new node, we replace the original links (inputs/successors)
    //    to the original nodes, by the corresponding new nodes we copied before.
    //    The cache is the lookup store to get for each link the corresponding new node.
    // 3. Add all new nodes to a new empty graph instance.

    // Key is the old object
    // Value the copied object
    Map<Node, Node> cache = new LinkedHashMap<>();

    // make shallow copy of all nodes. we will replace the links in the next step
    this.nodes.stream().filter(Objects::nonNull).forEach(oldNode -> {
      var newNode = oldNode.shallowCopy();
      cache.put(oldNode, newNode);
    });

    // Now, we have a shallow copy of all original nodes.
    // However, the new nodes are still linked to the original nodes.
    // In this step we replace all inputs and successors by the corresponding new nodes.
    cache.values().forEach(newNode -> {
      // replace shallow copied input by new uninitialized one
      newNode.inputs().forEach(oldInput -> {
        var newInput = cache.get(oldInput);
        // replace inputs
        newNode.applyOnInputsUnsafe((self, input) -> {
          if (input == oldInput) {
            return newInput;
          }
          return input;
        });
      });

      // replace shallow copied successor by new uninitialized one
      newNode.successors().forEach(oldSuccessor -> {
        var newSuccessor = cache.get(oldSuccessor);
        // replace successor
        newNode.applyOnSuccessorsUnsafe((self, succ) -> {
          if (succ == oldSuccessor) {
            return newSuccessor;
          }
          return succ;
        });
      });
    });

    // create new empty graph instance
    var graph = createEmptyInstance(name);

    // add all nodes to the graph
    cache.values().forEach(newNode -> {
      if (newNode.isUninitialized()) {
        // only if not yet initialized
        // might be initialized because of recursive input addition
        graph.addWithInputs(newNode);
      }
    });

    return graph;
  }

  /**
   * Returns the dot representation of this graph as String.
   */
  public String dotGraph() {
    return new DotGraphVisualizer()
        .load(this)
        .withSourceLocation(true)
        .visualize();
  }

  @Override
  public String toString() {
    return "Graph{ name='" + name + "', sourceLocation=" + sourceLocation + "}";
  }


  /**
   * Ensures the condition. If the condition is not met, it will throw an error with
   * the Graph's context.
   */
  @FormatMethod
  public void ensure(boolean condition, @FormatString String fmt, Object... args) {
    if (!condition) {
      throw new ViamGraphError(fmt, args)
          .addContext(this)
          .addLocation(this.sourceLocation)
          .shrinkStacktrace(1);
    }
  }

  /**
   * When {@link #copy()} then all the nodes remain activated. This can lead to crashes
   * when calling e.g {@link Node#replaceAndDelete(Node)}. This method deactivates all the ids.
   * This method is idempotent.
   *
   * @deprecated Because it leaves the given graph in an inconsistent state.
   *     If you want to copy a sub-graph to another graph, you want to use a {@link Node#copy()}
   *     version of the node that should be added.
   *     So transfering a node from one graph to another is done by copying the node.
   *     E.g. take a look at {@link vadl.viam.passes.functionInliner.FunctionInlinerPass#execute}
   */
  @Deprecated
  public void deinitializeNodes() {
    this.nodes.stream().filter(Objects::nonNull).filter(node -> node.id() != null)
        .map(node -> node.id)
        .forEach(Node.Id::deactivate);
  }

  /**
   * Gets the root {@link Node} from the graph which are dataflow nodes.
   * This is useful when we know that the graph has no control nodes, and we would like to apply
   * a recursive visitor.
   *
   * @return dataflow nodes which have no parents.
   */
  public List<DependencyNode> getDataflowRoots() {
    return getNodes(DependencyNode.class)
        .filter(x -> x.usageCount() == 0)
        .toList();
  }
}