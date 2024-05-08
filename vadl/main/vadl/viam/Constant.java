package vadl.viam;

import java.lang.reflect.Array;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
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


  public static class BitSlice extends Constant implements Iterable<Integer> {

    private final List<Part> parts;

    public BitSlice(Part[] parts) {
      super(Type.bitSlice());

      ViamError.ensure(parts.length > 0,
          "slice cannot be empty: %s", this);
      this.parts = Arrays.asList(parts);
      ViamError.ensure(
          !hasOverlappingParts(),
          "parts of slice must not overlap: %s", this);
    }

    final int size() {
      return parts.stream()
          .mapToInt(Part::size)
          .sum();
    }

    @Override
    public String toString() {
      return "[" + parts.stream().map(Part::toString).collect(Collectors.joining(", ")) + "]";
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

      BitSlice integers = (BitSlice) o;
      return parts.equals(integers.parts);
    }

    @Override
    public int hashCode() {
      int result = super.hashCode();
      result = 31 * result + parts.hashCode();
      return result;
    }

    @NotNull
    @Override
    public Iterator<Integer> iterator() {
      return new Iterator<>() {
        private int nextPartIdx = 1;
        private Iterator<Integer> currentPart = parts.get(0).iterator();

        @Override
        public boolean hasNext() {
          return currentPart.hasNext()
              || nextPartIdx < parts.size();
        }

        @Override
        public Integer next() {
          if (!hasNext()) {
            throw new NoSuchElementException();
          }
          if (!currentPart.hasNext()) {
            currentPart = parts.get(nextPartIdx++).iterator();
          }
          return currentPart.next();
        }
      };
    }

    private boolean hasOverlappingParts() {
      return parts.stream()
          .anyMatch(part -> parts.stream()
              .anyMatch(p -> p != part && p.msb() >= part.lsb() && p.lsb() <= part.msb()));
    }


    public static class Part implements Iterable<Integer> {

      private final int msb;
      private final int lsb;

      public Part(int msb, int lsb) {
        ViamError.ensure(msb >= lsb,
            "msb index must be greater or equal lsb index: %s", this);
        ViamError.ensure(lsb >= 0,
            "lsb index must be >= 0: %s", this);

        this.msb = msb;
        this.lsb = lsb;
      }

      public static Part of(int msb, int lsb) {
        return new Part(msb, lsb);
      }

      public final int msb() {
        return msb;
      }

      public final int lsb() {
        return lsb;
      }

      public final boolean isIndex() {
        return msb == lsb;
      }

      public final boolean isRange() {
        return !isIndex();
      }

      public final int size() {
        if (isIndex()) {
          return 1;
        }
        return msb - lsb + 1;
      }

      @NotNull
      @Override
      public Iterator<Integer> iterator() {
        return new Iterator<>() {
          int current = lsb;

          @Override
          public boolean hasNext() {
            return current <= msb;
          }

          @Override
          public Integer next() {
            return current++;
          }
        };
      }
    }
  }
}

