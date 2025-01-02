package vadl.vdt.model;

import javax.annotation.Nullable;

/**
 * A visitor for nodes in the decode tree.
 *
 * @param <T> the type of the result of visiting a node (can be {@code Void})
 */
public interface Visitor<T> {

  default @Nullable T visit(InnerNode node) {
    // Do nothing by default
    return null;
  }

  default @Nullable T visit(LeafNode node) {
    // Do nothing by default
    return null;
  }

}
