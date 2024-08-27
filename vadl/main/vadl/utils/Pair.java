package vadl.utils;

import java.util.Objects;

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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Pair<?, ?> pair = (Pair<?, ?>) o;
    return Objects.equals(left, pair.left) && Objects.equals(right, pair.right);
  }

  @Override
  public int hashCode() {
    return Objects.hash(left, right);
  }
}
