package vadl.viam.graph;

import javax.annotation.Nullable;

/**
 * The {@link GraphEdgeVisitor} interface represents a visitor that can visit nodes in a graph and
 * obtain a result of type R.
 *
 * @param <R> the type of the result obtained from visiting the graph nodes
 */
public interface GraphEdgeVisitor<R> {

  @Nullable
  R visit(Node from, @Nullable Node to);

  /**
   * The Applier interface represents a graph visitor that assigns new values to inputs of
   * a node in a graph.
   *
   * @param <R> the type of the result that gets assigned to the node's inputs
   */
  interface Applier<R extends Node> extends GraphEdgeVisitor<R> {
    @Nullable
    @Override
    default R visit(Node from, @Nullable Node to) {
      return applyNullable(from, to);
    }

    /**
     * Applies a transformation to an edge and returns the result as an instance of the
     * specified class.
     *
     * @param from  the start of the edge (not input node)
     * @param to    the end of the edge (input node)
     * @param clazz the class representing the desired type of the result
     * @param <T>   the type of the result
     * @return the result that is applied to the {@code to} node
     * @throws ViamGraphError if the applier produces invalid node type
     */
    default <T extends Node> T apply(Node from, @Nullable Node to, Class<T> clazz) {
      var newNode = apply(from, to);
      newNode.ensure(clazz.isInstance(newNode), "unable to apply %s to %s: types are mismatching",
          newNode.getClass(),
          clazz);
      return clazz.cast(newNode);
    }

    /**
     * Applies a transformation to the given nodes and returns the result.
     *
     * @param from the start of the edge (not input node)
     * @param to   the end of the edge (input node)
     * @return the result of the transformation
     * @throws ViamGraphError if the applier produces a null node
     */
    default R apply(Node from, @Nullable Node to) {
      var newNode = applyNullable(from, to);
      ViamGraphError.ensureNonNull(newNode, from.graph(), from, to,
          "applier produced a null node for apply(). Checkout the implementation of the "
              + "from-node, you probably want to use applyNullable for the Nullable field.");
      return newNode;
    }

    /**
     * Applies a transformation to an edge and returns the result as an instance of the
     * specified class. The result may be null.
     *
     * @param from  the start of the edge (not input node)
     * @param to    the end of the edge (input node)
     * @param clazz the class representing the desired type of the result
     * @param <T>   the type of the result
     * @return the result that is applied to the {@code to} node
     * @throws ViamGraphError if the applier produces invalid node type
     */
    @Nullable
    default <T extends Node> T applyNullable(Node from, @Nullable Node to, Class<T> clazz) {
      var newNode = applyNullable(from, to);
      if (newNode == null) {
        return null;
      }
      newNode.ensure(clazz.isInstance(newNode), "unable to apply %s to %s: types are mismatching",
          newNode.getClass(),
          clazz);
      return clazz.cast(newNode);
    }

    @Nullable
    R applyNullable(Node from, @Nullable Node to);


  }
}
