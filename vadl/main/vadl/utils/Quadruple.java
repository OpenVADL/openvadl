package vadl.utils;

/**
 * A helper construct to define a tuple.
 */
public class Quadruple<T, X, Z, Y> {
  private final T first;
  private final X second;
  private final Z third;
  private final Y fourth;

  /**
   * Tuple constructor for four parameters.
   */
  public Quadruple(T first, X second, Z third, Y fourth) {
    this.first = first;
    this.second = second;
    this.third = third;
    this.fourth = fourth;
  }

  public T first() {
    return first;
  }

  public X second() {
    return second;
  }

  public Z third() {
    return third;
  }

  public Y fourth() {
    return fourth;
  }
}
