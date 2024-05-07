package vadl.viam;

import java.math.BigInteger;
import vadl.types.Type;

public abstract class Constant {

  private final Type type;

  public Constant(Type type) {
    this.type = type;
  }

  public Type type() {
    return type;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Constant constant = (Constant) o;
    return type.equals(constant.type);
  }

  @Override
  public int hashCode() {
    return type.hashCode();
  }

  /**
   * Represents a constant value with a specific type.
   */
  public static class Value extends Constant {
    private final BigInteger value;

    public Value(BigInteger value, Type type) {
      super(type);
      this.value = value;
    }

    public static Value of(long value, Type type) {
      return new Value(BigInteger.valueOf(value), type);
    }

    public BigInteger value() {
      return value;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      if (!super.equals(o)) {
        return false;
      }

      Value value1 = (Value) o;
      return value.equals(value1.value);
    }

    @Override
    public int hashCode() {
      int result = super.hashCode();
      result = 31 * result + value.hashCode();
      return result;
    }
  }


  public static class Range extends Constant {

    private final Value from;
    private final Value to;

    public Range(Type type, Value from, Value to) {
      super(type);
      this.from = from;
      this.to = to;
    }

    public Value from() {
      return from;
    }

    public Value to() {
      return to;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      if (!super.equals(o)) {
        return false;
      }

      Range range = (Range) o;
      return from.equals(range.from) && to.equals(range.to);
    }

    @Override
    public int hashCode() {
      int result = super.hashCode();
      result = 31 * result + from.hashCode();
      result = 31 * result + to.hashCode();
      return result;
    }
  }
}
