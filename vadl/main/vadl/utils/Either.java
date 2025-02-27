package vadl.utils;

import java.util.Objects;
import javax.annotation.Nullable;
import vadl.viam.ViamError;

/**
 * Represents that a value can be either of one type or another one.
 */
public final class Either<T, X> {
  @Nullable
  private T left;

  @Nullable
  private X right;

  /**
   * Constructor.
   */
  public Either(@Nullable T left, @Nullable X right) {
    ViamError.ensure((left != null && right == null) || (left == null && right != null),
        "left and right cannot both be set");
    this.left = left;
    this.right = right;
  }

  public boolean isLeft() {
    return this.left != null;
  }

  public boolean isRight() {
    return this.right != null;
  }

  public T left() {
    return Objects.requireNonNull(left);
  }

  public X right() {
    return Objects.requireNonNull(right);
  }
}
