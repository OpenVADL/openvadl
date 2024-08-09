package vadl.viam.passes;

import vadl.viam.graph.Node;

/**
 * A helper construct to define a tuple.
 */
public class Pair<T, X> {
  private final T left;
  private final X right;

  public Pair(T left, X right) {
    this.left = left;
    this.right = right;
  }

  public X right() {
    return right;
  }

  public T left() {
    return left;
  }
}
