package vadl.viam.graph;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import vadl.utils.SourceLocation;
import vadl.viam.graph.control.AbstractEndNode;
import vadl.viam.graph.control.ControlNode;
import vadl.viam.graph.control.EndNode;
import vadl.viam.graph.control.InstrCallNode;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.control.StartNode;
import vadl.viam.graph.dependency.FuncParamNode;
import vadl.viam.graph.dependency.ParamNode;
import vadl.viam.graph.dependency.SideEffectNode;

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
  public final <T extends Node> Stream<T> getNodes(Class<T> clazz) {
    return getNodes().filter(clazz::isInstance).map(clazz::cast);
  }

  /**
   * Replaces the node {@code toReplace} with the given node {@code newNode}.
   */
  public <T extends Node> T replaceNode(Node toReplace, T newNode) {
    var node = this.addWithInputs(newNode);

    // All of toReplace's children are obsolete.
    // But, we cannot delete them because they might be used by other nodes.

    // First, keep track of the subtree of toReplace.
    ArrayList<Node> children = new ArrayList<>();
    toReplace.collectInputs(children);
    toReplace.collectSuccessors(children);

    // Relevant for data nodes.
    this.nodes.forEach(x -> x.replaceInput(toReplace, node));
    toReplace.usages().forEach(x -> x.transferUsageOfThis(toReplace, node));

    // Relevant for control nodes.
    var pred = toReplace.predecessor();
    if (pred != null) {
      node.setPredecessor(pred);
    }

    // Remove the link from the children to toReplace.
    toReplace.applyOnInputs(new GraphVisitor.Applier<>() {
      @Nullable
      @Override
      public Node applyNullable(Node from, @Nullable Node to) {
        if (to != null) {
          to.removeUsage(from);
          to.applyOnInputs(this);
        }
        return to;
      }
    });

    // Remove all nodes which are obsolete
    children.stream()
        .filter(x -> x.predecessor() == null && x.usageCount() == 0 && x.successorList().isEmpty())
        .distinct()
        .forEach(x -> {
          this.nodes.remove(x);
          x.safeDelete();
        });
    this.nodes.remove(toReplace);
    toReplace.safeDelete();

    return node;
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
      return addUniqueInternal(node, true);
    } else {
      return addSimpleInternal(node, true);
    }
  }

  /**
   * This method works like {@link Graph#add(Node)}. However,
   * it will not check whether the inputs were also added because
   * by cloning the graph, we know that we have an inconsistent state.
   */
  private <T extends Node> T unsafeAdd(T node) {
    if (node instanceof UniqueNode) {
      return addUniqueInternal(node, false);
    } else {
      return addSimpleInternal(node, false);
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
    node.applyOnInputsUnsafe((n, target) -> {
      if (target == null || target.isActive()) {
        return target;
      }

      target.ensure(!target.isDeleted(), "cannot add deleted input node");
      var newT = addWithInputs(target);
      if (newT != target) {
        // TODO: Check if we can use visit inputs for this lambda
        // as we are calling replaceInput on the node anyway
        node.replaceInput(target, newT);
      }
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
   * arbitrary many {@link InstrCallNode} and a {@link EndNode}. It must only reference
   * {@link FuncParamNode} but no other param nodes.</p>
   */
  public boolean isPseudoInstruction() {
    return getNodes(ControlNode.class).allMatch(
        e -> e instanceof InstrCallNode || e instanceof StartNode || e instanceof EndNode
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
  private <T extends Node> T addSimpleInternal(T node, boolean assertInputsAdded) {
    node.ensure(node.isUninitialized(), "node is not uninitialized");
    // ensure that all input dependencies are already added
    // to the graph
    if (assertInputsAdded) {
      // This check is optional because when cloning the graph,
      // the graph will be inconsistent.
      ensureInputsAdded(node);
    }

    node.initialize(this);
    return node;
  }

  private <T extends Node> T addUniqueInternal(T node, boolean assertInputsAdded) {
    node.ensure(node.isUninitialized(), "node is not uninitialized");
    var result = findDuplicate(node);
    if (result != null) {
      return result;
    }
    return addSimpleInternal(node, assertInputsAdded);
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
            "Failed to add `%s` as its input node `%s` is not yet initialized.}", node, input)
            .addContext(node)
            .addContext(this)
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

  /**
   * Copies the graph and returns it.
   */
  public Graph copy() {
    // Why do we need the cache?
    // It is possible that an object is unique. For example, every
    // Addition BuiltIn Node with two values might exist only once in the graph.
    // But it can still be used by multiple nodes. Without the cache, we would
    // create a new BuiltIn Node for every occurrence.

    // Key is the old object
    // Value the copied object
    Map<Node, Node> cache = new HashMap<>();
    var graph = new Graph(name);

    this.nodes.forEach(oldNode -> {
      var newNode = graph.unsafeAdd(oldNode.shallowCopy());
      cache.put(oldNode, newNode);
    });

    // Now, we have added all the nodes from the old to new graph.
    // However, they are not linked yet because they are shallow copies.
    graph.nodes.forEach(newNode -> {
      // Update the usages
      newNode.usages().forEach(oldUsage -> {
        var newUsage = cache.get(oldUsage);
        newNode.transferUsageOfThis(oldUsage, Objects.requireNonNull(newUsage));
      });

      // Update the inputs
      newNode.inputs().forEach(oldInput -> {
        var newInput = cache.get(oldInput);
        newNode.replaceInput(oldInput, Objects.requireNonNull(newInput));
      });

      // Update the predecessor (if it exists)
      var oldPred = newNode.predecessor();
      if (oldPred != null) {
        var newPred = cache.get(oldPred);
        newNode.setPredecessor(newPred);
      }
    });

    return graph;
  }
}

