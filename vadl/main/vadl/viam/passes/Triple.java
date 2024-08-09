package vadl.viam.passes;

/**
 * A helper construct to define a tuple.
 */
public class Triple<T, X, Z> {
  private final T left;
  private final X middle;
  private final Z right;

  public Triple(T left, X middle, Z right) {
    this.left = left;
    this.middle = middle;
    this.right = right;
  }

  public Z right() {
    return right;
  }

  public T left() {
    return left;
  }

  public X middle() {
    return middle;
  }
}
