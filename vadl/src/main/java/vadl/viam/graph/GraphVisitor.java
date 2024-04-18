package vadl.viam.graph;

import javax.annotation.Nullable;

/**
 * The GraphVisitor interface represents a visitor that can visit nodes in a graph and obtain a result of type R.
 *
 * @param <R> the type of the result obtained from visiting the graph nodes
 */
public interface GraphVisitor<R> {

  @Nullable
  R visit(Node from, @Nullable Node to);

  /**
   * The Applier interface represents a graph visitor that assigns new values to inputs of a node in a graph.
   *
   * @param <R> the type of the result that gets assigned to the node's inputs
   */
  interface Applier<R extends Node> extends GraphVisitor<R> {
    @Nullable
    @Override
    default R visit(Node from, @Nullable Node to) {
      return applyNullable(from, to);
    }

    default <T extends Node> T apply(Node from, @Nullable Node to, Class<T> clazz) {
      var newNode = apply(from, to);
      newNode.ensure(clazz.isInstance(newNode), "unable to apply %s to %s.", newNode.getClass(),
          clazz);
      return clazz.cast(newNode);
    }

    default R apply(Node from, @Nullable Node to) {
      var newNode = applyNullable(from, to);
      ViamGraphError.ensureNonNull(newNode, from.graph(), from, to,
          "applier produced a null node for apply()");
      return newNode;
    }

    @Nullable
    R applyNullable(Node from, @Nullable Node to);
  }
}
