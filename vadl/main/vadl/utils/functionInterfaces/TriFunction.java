package vadl.utils.functionInterfaces;

import java.util.Objects;
import java.util.function.Function;

/**
 * A Java function interface with 3 parameter and a return value.
 */
@FunctionalInterface
public interface TriFunction<A, B, C, R> {
  R apply(A var1, B var2, C var3);

  default <V> TriFunction<A, B, C, V> andThen(
      Function<? super R, ? extends V> after) {
    Objects.requireNonNull(after);
    return (a, b, c) -> after.apply(this.apply(a, b, c));
  }
}