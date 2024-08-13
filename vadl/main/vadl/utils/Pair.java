package vadl.utils;

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

  public static <T, X> Pair<T, X> of(T left, X right) {
    return new Pair<T, X>(left, right);
  }
}
