package vadl.viam.graph;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Objects;
import java.util.stream.Stream;
import javax.annotation.Nullable;

/**
 * The VIAM graph represents an execution flow definition
 * in VADL. E.g. instructions, functions, ...
 *
 * <p>It consists of a set of nodes that build up the control flow
 * and data dependencies between each other.
 */
public class Graph {

  public final String name;

  ArrayList<Node> nodes;
  private long removedNodes = 0;

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
        )
        .findFirst()
        .orElse(null);
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
    removedNodes++;
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
}

