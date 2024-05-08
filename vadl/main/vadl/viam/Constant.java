package vadl.viam;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import vadl.types.Type;

/**
 * The Constant class represents some kind of constant with a specific type.
 */
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
    public String toString() {
      return value.toString();
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


  /**
   * The Range class represents a constant range of values.
   * A range is defined by a starting value (from) and an ending value (to) (both inclusive).
   *
   * <p>The range's from is always less or qual the to value. TODO: Discuss if this fine/good
   */
  public static class Range extends Constant {

    private final Value from;
    private final Value to;

    /**
     * Constructs a Range object with the given starting value (from) and ending value (to).
     * The Range instance will have a starting value (from) less or equal the ending value (to).
     * The boundary values must be of the same type.
     *
     * @param a first boundary value of the range
     * @param b second boundary value of the range
     */
    public Range(Value a, Value b) {
      super(Type.range(a.type()));

      if (a.value.compareTo(b.value) < 0) {
        this.from = a;
        this.to = b;
      } else {
        this.from = b;
        this.to = a;
      }

      ViamError.ensure(a.type().equals(b.type()),
          "range boundary values must be of the same type. from=%s, to=%s", this.from.type(),
          this.to.type());
    }

    public Value from() {
      return from;
    }

    public Value to() {
      return to;
    }

    public boolean isIndex() {
      return from.value.equals(to.value);
    }

    public boolean isRange() {
      return !isIndex();
    }

    final int size() {
      if (isIndex()) {
        return 1;
      }

      var from = this.from.value.intValue();
      var to = this.to.value.intValue();
      return to - from + 1;
    }


    public List<Value> toList() {
      List<Value> list = new ArrayList<>();
      for (BigInteger i = from.value(); i.compareTo(to.value()) <= 0; i = i.add(BigInteger.ONE)) {
        list.add(new Value(i, from.type()));
      }
      return list;
    }

    @Override
    public String toString() {
      return "Range{" +
          "from=" + from +
          ", to=" + to +
          '}';
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
