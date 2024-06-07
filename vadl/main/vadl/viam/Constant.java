package vadl.viam;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import vadl.types.DataType;
import vadl.types.Type;
import vadl.utils.StreamUtils;

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

    public Value(BigInteger value, DataType type) {
      super(type);
      this.value = value;
    }

    public static Value of(long value, DataType type) {
      return new Value(BigInteger.valueOf(value), type);
    }

    public BigInteger value() {
      return value;
    }

    @Override
    public DataType type() {
      return (DataType) super.type();
    }

    @Override
    public java.lang.String toString() {
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
   * Represents a constant string.
   */
  public static class Str extends Constant {

    private final java.lang.String value;

    public Str(java.lang.String value) {
      super(Type.string());
      this.value = value;
    }

    @Override
    public Type type() {
      return super.type();
    }

    public java.lang.String value() {
      return value;
    }

    @Override
    public java.lang.String toString() {
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

      Str str = (Str) o;
      return value.equals(str.value);
    }

    @Override
    public int hashCode() {
      int result = super.hashCode();
      result = 31 * result + value.hashCode();
      return result;
    }
  }


  /**
   * The constant of a VADL bit-slice. It represents a statically known, non-overlapping
   * list of bit indices. It allows iterating over it in an order preserving manner.
   *
   * @see vadl.types.BitSliceType
   */
  public static class BitSlice extends Constant implements Iterable<Integer> {

    private final List<Part> parts;

    /**
     * The constructor of a BitSlice from an array of sub-ranges (parts).
     *
     * <p>The resulting BitSlice is normalized to the least necessary parts.</p>
     */
    public BitSlice(Part[] parts) {
      super(Type.bitSlice());

      ViamError.ensure(parts.length > 0,
          "slice cannot be empty: %s", this);
      this.parts = normalized(parts);
      ViamError.ensure(
          !hasOverlappingParts(),
          "parts of slice must not overlap: %s", this);
    }


    public final int bitSize() {
      return parts.stream()
          .mapToInt(Part::size)
          .sum();
    }

    public final int partSize() {
      return parts.size();
    }

    public final Stream<Part> parts() {
      return parts.stream();
    }

    public boolean isContinuous() {
      return parts.size() == 1;
    }

    @Override
    public java.lang.String toString() {
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
      return this.parts.stream()
          .flatMapToInt(part -> StreamUtils.directionalRangeClosed(part.msb(), part.lsb()))
          .iterator();
    }

    private boolean hasOverlappingParts() {
      return parts.stream()
          .anyMatch(part -> parts.stream()
              .anyMatch(
                  p -> !Objects.equals(p, part) && p.isOverlapping(part)
              ));
    }

    private static List<Part> normalized(Part[] parts) {
      // flat map all parts to a single array of integers
      var flattened = Arrays.stream(parts)
          .flatMapToInt(p -> StreamUtils.directionalRangeClosed(p.msb(), p.lsb()))
          .toArray();
      var normalized = new ArrayList<Part>();

      var current = new ArrayList<Integer>();
      for (int i : flattened) {
        if (current.isEmpty()) {
          current.add(i);
          continue;
        }
        var last = current.get(current.size() - 1);
        if (last - 1 == i) {
          current.add(i);
        } else {
          normalized.add(new Part(current.get(0), current.get(current.size() - 1)));
          current = new ArrayList<>();
          current.add(i);
        }
      }
      normalized.add(new Part(current.get(0), current.get(current.size() - 1)));

      return normalized;
    }


    /**
     * The {@code Part} class represents a part of a {@link BitSlice}.
     * It is either a range of bit indices from the msb to the lsb, or a single index
     * if both values are equal.
     *
     * <p>It implements the {@link Iterable} interface to allow iteration
     * over the elements in the part.
     *
     * @param msb the most significant bit index
     * @param lsb the least significant bit index
     */
    public record Part(int msb, int lsb) implements Iterable<Integer> {

      /**
       * Constructs a Part object with the specified most significant bit (msb)
       * and least significant bit (lsb) indices.
       *
       * @throws ViamError if the msb index is not greater than or equal to the lsb index,
       *                   or if the lsb index is less than 0
       */
      public Part {
        ViamError.ensure(msb >= lsb,
            "msb index must be greater or equal lsb index: %s", this);
        ViamError.ensure(lsb >= 0,
            "lsb index must be >= 0: %s", this);

      }

      public static Part of(int msb, int lsb) {
        return new Part(msb, lsb);
      }

      public boolean isIndex() {
        return msb == lsb;
      }

      public boolean isRange() {
        return !isIndex();
      }

      public Part join(Part other) {
        return new Part(Math.max(msb, other.msb), Math.min(lsb, other.lsb));
      }

      public boolean isSurroundedBy(Part other) {
        return this.join(other).equals(other);
      }

      public boolean isOverlapping(Part other) {
        return this.msb >= other.lsb && this.lsb <= other.msb;
      }

      /**
       * Returns the size of this bit-slice sub-range.
       */
      public int size() {
        if (isIndex()) {
          return 1;
        }
        return msb - lsb + 1;
      }

      @Override
      public java.lang.String toString() {
        return isIndex() ? "" + msb : msb + ".." + lsb;
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

