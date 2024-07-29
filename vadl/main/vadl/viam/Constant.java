package vadl.viam;

import static vadl.utils.BigIntUtils.mask;
import static vadl.utils.BigIntUtils.twosComplement;

import com.google.errorprone.annotations.FormatMethod;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IntSummaryStatistics;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import vadl.types.BitsType;
import vadl.types.BoolType;
import vadl.types.DataType;
import vadl.types.TupleType;
import vadl.types.Type;
import vadl.utils.BigIntUtils;
import vadl.utils.StreamUtils;

/**
 * The Constant class represents some kind of constant with a specific type.
 */
public abstract class Constant {

  private Type type;

  public Constant(Type type) {
    this.type = type;
  }

  public Type type() {
    return type;
  }

  public void setType(Type type) {
    this.type = type;
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
   *
   * <p>It stores values of type bits and bool.
   * The value itself is represented as two's complement; thus BigInteger value is
   * only a data container, not the actual number.
   * The {@link #integer()} returns the integer value depending on the constant's type.
   */
  public static class Value extends Constant {
    // not really an integer, just a data container
    private final BigInteger value;

    /**
     * WARNING: Never use this constructor directly!
     * Always use either {@link #fromInteger(BigInteger, DataType)} or
     * {@link #fromTwosComplement(BigInteger, DataType)}.
     *
     * <p>All public construction overloads of {@link #of} take an integer as input.</p>
     */
    private Value(BigInteger value, DataType type) {
      super(type);
      this.value = value;
    }

    /**
     * Constructor for input values that are in two's complement.
     * This means the {@code value} argument is NOT an integer, but binary representation
     * of a number in two's compliment.
     */
    private static Value fromTwosComplement(BigInteger value, DataType type) {
      if (value.signum() < 0 || value.bitLength() > type.bitWidth()) {
        throw new ViamError("Internal error; value not in two's complement.");
      }
      return new Value(value, type);
    }

    /**
     * Constructor of a constant value from an integer (that is not in two's complement form).
     * So the {@code integer} argument might be negative.
     */
    private static Value fromInteger(BigInteger integer, DataType type) {
      if (type instanceof BoolType) {
        // hard code boolean value
        var val = integer.compareTo(BigInteger.ZERO) == 0 ? integer : BigInteger.ONE;
        return new Value(val, type);
      } else if (type instanceof BitsType bitsType) {
        if (bitsType.getClass() == BitsType.class) {
          // for bitsType, it must just fit into the bit width, but it has no integer value boundaries
          if (integer.bitLength() > bitsType.bitWidth()) {
            throw new ViamError("Value %s does not fit in type %s".formatted(integer.toString(16),
                bitsType.getClass()));
          }
        } else if (minValueOf(bitsType).integer().compareTo(integer) > 0 ||
            maxValueOf(bitsType).integer().compareTo(integer) < 0) {
          // for SInt and UInt types the integer value must fit in the allowed range
          throw new ViamError(
              "Value %s does not fit in type %s. Possible range: %s .. %s".formatted(
                  integer.toString(16), type,
                  minValueOf(bitsType), maxValueOf(bitsType)));
        }
        var value = twosComplement(integer, type.bitWidth());
        return new Value(value, type);
      } else {
        throw new ViamError("Only BitsType and BoolType are supported, but got %s".formatted(type));
      }
    }

    public static Value of(long value, DataType type) {
      return fromInteger(BigInteger.valueOf(value), type);
    }

    public static Value of(boolean value) {
      return fromInteger(BigInteger.valueOf(value ? 1 : 0), Type.bool());
    }

    public static Value of(String value, DataType type) {
      return fromInteger(new BigInteger(value), Type.bool());
    }

    /**
     * Returns the integer value represented by this value object.
     * If the type of the value is BoolType, the underlying BigInteger value is returned.
     * Otherwise, the two's complement representation of the BigInteger value is converted
     * to its original value based on the specified BitsType.
     *
     * @return the integer value represented by this value object
     */
    public BigInteger integer() {
      if (type() instanceof BoolType) {
        return this.value;
      } else {
        return BigIntUtils.fromTwosComplement(value, (BitsType) type());
      }
    }

    public int intValue() {
      return integer().intValue();
    }

    public long longValue() {
      return integer().longValue();
    }

    public boolean bool() {
      ensure(type() instanceof BoolType, "constant must be of bool type");
      return this.value.bitLength() != 0;
    }

    @Override
    public DataType type() {
      return (DataType) super.type();
    }

    /**
     * Casts the constant value to the specified data type.
     *
     * @param type the data type to cast the value to
     * @return a new Constant.Value object representing the casted value
     * @throws ViamError if the constant cannot be cast to the specified data type
     */
    public Constant.Value castTo(DataType type) {
      var truncatedValue = value
          .and(mask(type.bitWidth(), 0));
      return Value.fromTwosComplement(truncatedValue, type);
    }

    /**
     * Returns the addition of this and other together with the status.
     *
     * @return a tuple constant of form {@code ( result, ( z, c, o, n ) )}
     */
    public Constant.Tuple add(Constant.Value other) {
      ensure(type() instanceof BitsType, "Invalid type for addition");
      ensure(type().equals(other.type()), "Types don't match, %s vs %s", type(), other.type());

      var result = value.add(other.value);
      var truncated = result.and(mask(type().bitWidth(), 0));

      var isZero = truncated.equals(BigInteger.ZERO);
      // check msb
      var isNegative = result.testBit(type().bitWidth() - 1);

      // the carry flag is set if the addition of two numbers causes a carry
      // out of the most significant (leftmost) bits added.
      // can be ignored for signed interpretation of result.
      // https://teaching.idallen.com/dat2343/10f/notes/040_overflow.txt
      var isCarry = result.bitLength() > type().bitWidth();

      // overflow if both operands have same sign and differ from result sign.
      // overflow is ignored for unsigned interpretation of result.
      // https://teaching.idallen.com/dat2343/10f/notes/040_overflow.txt
      var isOverflow = this.isSignBit() == other.isSignBit() && (this.isSignBit() != isNegative);


      return new Constant.Tuple(
          Constant.Value.fromTwosComplement(truncated, type()),
          Constant.Tuple.status(isZero, isCarry, isOverflow, isNegative)
      );
    }

    /**
     * Subtracts the given value from this value.
     *
     * <p>It uses the {@link #add(Value)} function by negating the second operand.
     * Therefore the carry flag must be inverse and the overflow must be specially
     * handled if the second operand is the minimal signed value.</p>
     *
     * @param other the value to subtract from this value
     * @return a tuple constant representing the result of the subtraction
     */
    public Constant.Tuple subtract(Constant.Value other) {
      ensure(type() instanceof BitsType, "Invalid type for subtraction");
      ensure(type().equals(other.type()), "Types don't match, %s vs %s", type(), other.type());

      // from a - b to a + (-b)
      var negatedOther = other.negate();
      var result = this.add(negatedOther);

      // swap the carry flag by inverse of addition open-vadl#76
      var resStatus = result.get(1, Constant.Tuple.class);
      // the carry of the subtraction is the inverse to the carry of the addition
      var carry = resStatus.get(1, Constant.Value.class)
          .not();

      var overflow = resStatus.get(2, Constant.Value.class);
      if (minSignedValue().equals(other.value)) {
        // if b equals to minimal value (100..), the negation results in the same
        // value. This will invalidate the overflow flag of the addition result, as
        // the value is taken as negative eventhough it is actually positive (out of range).
        // Therefore we want to invert the overflow flag from the addition.
        overflow = overflow.not();
      }

      var status = new Constant.Tuple(
          resStatus.get(0, Constant.Value.class),
          carry,
          overflow,
          resStatus.get(3, Constant.Value.class)
      );

      return new Constant.Tuple(result.get(0), status);
    }

    /**
     * Checks if the sign bit of the value is set.
     */
    public boolean isSignBit() {
      return value.testBit(type().bitWidth() - 1);
    }

    /**
     * Returns the logical negation of the current value object.
     *
     * @return the negation value of the current value object
     * @throws ViamError if the type of the value object is not BoolType.
     */
    public Constant.Value not() {
      ensure(type() instanceof BoolType, "not() currently only available for bool type");
      return Constant.Value.of(!this.bool());
    }

    /**
     * Negates the value represented by this Constant.Value object.
     *
     * <p>
     * This method calculates the negation of the value by inverting all bits, adding one to it,
     * and then truncating the result based on the number of bits specified by the DataType.
     * The resulting negated value is returned as a new Constant.Value object.
     * </p>
     *
     * <p><b>Note: </b>If the value is the minimal possible signed number, this will truncate the
     * result and lead
     * to a wrong value.
     *
     * @return a new Constant.Value object representing the negated value
     */
    public Constant.Value negate() {
      var mask = mask(type().bitWidth(), 0);
      var negated = value
          .xor(mask) // invert all bits
          .add(BigInteger.ONE) // add one to it
          .and(mask); // truncate result
      return Constant.Value.fromTwosComplement(negated, type());
    }

    private BigInteger maxUnsignedValue() {
      // e.g. 4 bits: 1000 ... -8
      return BigInteger.ZERO.setBit(type().bitWidth()).subtract(BigInteger.ONE);
    }

    private BigInteger maxSignedValue() {
      // e.g. 4 bits: 1000 ... -8
      return BigInteger.ZERO.setBit(type().bitWidth() - 1).subtract(BigInteger.ONE);
    }

    private BigInteger minSignedValue() {
      // e.g. 4 bits: 1000 ... -8
      return BigInteger.ZERO.setBit(type().bitWidth() - 1);
    }

    @Override
    public java.lang.String toString() {
      return integer() + ": " + type().toString();
    }

    public static Constant.Value maxValueOf(BitsType type) {
      BigInteger result;
      if (type.isSigned()) {
        result = BigInteger.ONE.shiftLeft(type.bitWidth() - 1).subtract(BigInteger.ONE);
      } else {
        result = BigInteger.ONE.shiftLeft(type.bitWidth()).subtract(BigInteger.ONE);
      }
      return fromTwosComplement(result, type);
    }

    public static Constant.Value minValueOf(BitsType type) {
      BigInteger result;
      if (type.isSigned()) {
        result = BigInteger.ZERO.setBit(type.bitWidth() - 1);
      } else {
        result = BigInteger.ZERO;
      }
      return fromTwosComplement(result, type);
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

    public String decimal() {
      return asString("", 10);
    }

    public String hexadecimal() {
      return hexadecimal("0x");
    }

    public String hexadecimal(String prefix) {
      return asString(prefix, 16);
    }

    public String binary() {
      return binary("0b");
    }

    public String binary(String prefix) {
      return asString(prefix, 2);
    }

    private String asString(String prefix, int radix) {
      Integer padFactor = null;
      switch (radix) {
        case 2:
          padFactor = 1;
          break;
        case 10:
          padFactor = 0;
          break;
        case 16:
          padFactor = 4;
          break;
        default:
          return "Invalid radix %s".formatted(radix);
      }

      if (type() instanceof BoolType) {
        return prefix + this.value.toString(radix);
      }

      var str = this.value.toString(radix);
      if (padFactor > 0) {
        var padSize = (type().bitWidth() / padFactor) - str.length();
        str = "0".repeat(padSize) + str;
      }
      return prefix + str;
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

    // cache the statistic summary for performance
    private final IntSummaryStatistics statistics;

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
      this.statistics = stream().summaryStatistics();
      ViamError.ensure(
          !hasOverlappingParts(),
          "parts of slice must not overlap: %s", this);
    }

    @Override
    public Type type() {
      return Type.bitSlice();
    }

    public final int bitSize() {
      return (int) statistics.getCount();
    }

    public final int partSize() {
      return parts.size();
    }

    public final Stream<Part> parts() {
      return parts.stream();
    }

    public boolean isContinuous() {
      // this works because the parts are normalized
      return parts.size() == 1;
    }

    /**
     * Returns the most significant bit index of the bit-slice.
     */
    public int msb() {
      return statistics.getMax();
    }

    /**
     * Returns the least significant bit index of the bit-slice.
     */
    public int lsb() {
      return statistics.getMin();
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

    public IntStream stream() {
      return this.parts.stream()
          .flatMapToInt(part -> StreamUtils.directionalRangeClosed(part.msb(), part.lsb()));
    }

    @NotNull
    @Override
    public Iterator<Integer> iterator() {
      return stream()
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

  /**
   * Represents a tuple constant containing other constants.
   */
  public static class Tuple extends Constant {
    private final List<Constant> values;


    /**
     * Creates a Tuple constant with the given values and type.
     *
     * @param values The list of Constant values contained in the Tuple.
     * @param type   The TupleType of the Tuple constant.
     */
    public Tuple(List<Constant> values, TupleType type) {
      super(type);
      this.values = values;
    }

    /**
     * Creates a Tuple constant with the given values.
     * The type of the tuple gets automatically inferred.
     *
     * @param values The list of Constant values contained in the Tuple.
     */
    public Tuple(List<Constant> values) {
      this(values,
          TupleType.tuple(
              values.stream()
                  .map(e -> (DataType) e.type)
                  .toArray(DataType[]::new)
          )
      );
    }

    /**
     * Creates a Tuple constant with the given values.
     */
    public Tuple(Constant... values) {
      this(List.of(values));
    }

    /**
     * Constructs a tuple constant representing a status tuple.
     */
    public static Tuple status(boolean zero, boolean carry, boolean overflow, boolean negative) {
      return new Tuple(
          List.of(Constant.Value.of(zero), Constant.Value.of(carry), Constant.Value.of(overflow),
              Constant.Value.of(negative)
          ), Type.status().asTuple()
      );
    }

    public List<Constant> values() {
      return values;
    }

    /**
     * Retrieves the Constant value at the specified index.
     *
     * @param index The index of the Constant value to retrieve.
     * @return The Constant value at the specified index.
     */
    public Constant get(int index) {
      return values.get(index);
    }

    /**
     * Retrieves the constant value at the specified index and casts it to the given type.
     *
     * @param index               The index of the constant value to retrieve.
     * @param typeOfConstantClass The class type to cast the constant value to.
     * @param <T>                 The generic type parameter representing the class type.
     * @return The constant value at the specified index, casted to the given type.
     * @throws ViamError if the constant value at the specified
     *                   index is not of the given type.
     */
    public <T extends Constant> T get(int index, Class<T> typeOfConstantClass) {
      var val = values.get(index);
      ensure(typeOfConstantClass.isInstance(val), "Expected constant of type %s but got %s",
          typeOfConstantClass, val);
      //noinspection unchecked
      return (T) val;
    }

    public int size() {
      return values.size();
    }

    @Override
    public String toString() {
      return "(" + values.stream().map(Constant::toString).collect(Collectors.joining(", ")) + ")";
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

      Tuple tuple = (Tuple) o;
      return values.equals(tuple.values);
    }

    @Override
    public int hashCode() {
      int result = super.hashCode();
      result = 31 * result + values.hashCode();
      return result;
    }
  }


  // HELPER FUNCTIONS

  /**
   * Ensure that a condition is true, otherwise throw a ViamError with a formatted error message.
   * It adds the constant as context in case of an error.
   *
   * @param condition the condition to check
   * @param fmt       the format string for the error message
   * @param args      the arguments to format the error message
   * @throws ViamError if the condition is false
   */
  @Contract("false, _, _ -> fail")
  @FormatMethod
  public void ensure(boolean condition, String fmt, Object... args) {
    if (!condition) {
      throw new ViamError(fmt.formatted(args))
          .addContext("constant", this)
          .shrinkStacktrace(1);
    }
  }
}

