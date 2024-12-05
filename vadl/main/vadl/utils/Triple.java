package vadl.utils;

/**
 * A helper construct to define a tuple.
 */
public class Triple<T, X, Z> {
  private final T left;
  private final X middle;
  private final Z right;

  /**
   * Tuple constructor for three parameters.
   */
  public Triple(T left, X middle, Z right) {
    this.left = left;
    this.middle = middle;
    this.right = right;
  }

  public static <T, X, Z> Triple<T, X, Z> of(T left, X middle, Z right) {
    return new Triple<T, X, Z>(left, middle, right);
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
