// SPDX-FileCopyrightText : © 2025 TU Wien <vadl@tuwien.ac.at>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

package vadl.viam;

import static vadl.error.Diagnostic.warning;
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
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.jetbrains.annotations.Contract;
import vadl.error.DeferredDiagnosticStore;
import vadl.types.BitsType;
import vadl.types.BoolType;
import vadl.types.DataType;
import vadl.types.SIntType;
import vadl.types.TupleType;
import vadl.types.Type;
import vadl.types.UIntType;
import vadl.utils.BigIntUtils;
import vadl.utils.SourceLocation;
import vadl.utils.StreamUtils;
import vadl.viam.graph.dependency.ConstantNode;

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

  public Constant.Value asVal() {
    ensure(this instanceof Value, "Constant is not a value");
    return (Constant.Value) this;
  }

  public Constant.Tuple asTuple() {
    ensure(this instanceof Tuple, "Constant is not a tuple");
    return (Constant.Tuple) this;
  }

  public ConstantNode toNode() {
    return new ConstantNode(this);
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
     * Constructor of a constant value from a boolean.
     */
    public static Value fromBoolean(Boolean value) {
      return fromInteger(value ? BigInteger.ONE : BigInteger.ZERO, Type.bool());
    }

    /**
     * Constructor of a constant value from an integer (that is not in two's complement form).
     * So the {@code integer} argument might be negative.
     */
    public static Value fromInteger(BigInteger integer, DataType type) {
      if (type instanceof BoolType) {
        // hard code boolean value
        var val = integer.compareTo(BigInteger.ZERO) == 0 ? integer : BigInteger.ONE;
        return new Value(val, type);
      } else if (type instanceof BitsType bitsType) {
        if (bitsType.getClass() == BitsType.class) {
          // for bitsType, it must just fit into the bit width, but it has no
          // integer value boundaries
          if (integer.bitLength() > bitsType.bitWidth()) {
            throw new ViamError("Value 0x%s does not fit in type %s".formatted(integer.toString(16),
                bitsType));
          }
        } else if (minValueOf(bitsType).integer().compareTo(integer) > 0
            || maxValueOf(bitsType).integer().compareTo(integer) < 0) {
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
      return fromInteger(new BigInteger(value), type);
    }

    /**
     * Translates a byte array containing the two's-complement binary representation of an integer
     * into a Value.
     * The input array is assumed to be in big-endian byte-order: the most
     * significant byte is in the zeroth element.
     * The {@code value} array is assumed to be unchanged for the duration of the constructor call.
     *
     * @param value value in two's complement.
     * @param type  type of the value
     */
    public static Value of(byte[] value, DataType type) {
      return fromInteger(new BigInteger(value), type);
    }

    /**
     * Returns the unsigned big integer representation of this value, regardless of the value's
     * type.
     */
    public BigInteger unsignedInteger() {
      return value;
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
        return BigIntUtils.fromTwosComplement(value, type().bitWidth(), type().isSigned());
      }
    }

    public int intValue() {
      return integer().intValue();
    }

    public long longValue() {
      return integer().longValue();
    }

    public boolean bool() {
      ensure(type().isTrivialCastTo(Type.bool()), "constant must be of bool type");
      return this.value.bitLength() != 0;
    }

    @Override
    public DataType type() {
      return (DataType) super.type();
    }

    /**
     * Keeps the same value as is, but sets the type to {@code Bits},
     * with the same size as the current type.
     */
    public Value toBits() {
      return fromTwosComplement(value, Type.bits(type().bitWidth()));
    }

    /**
     * Casts the constant value to the specified data type.
     *
     * @param targetType the data type to cast the value to
     * @return a new Constant.Value object representing the cast value
     */
    public Constant.Value castTo(DataType targetType) {
      var sourceType = type();

      if (sourceType.isTrivialCastTo(targetType)) {
        // same memory representation
        return fromTwosComplement(value, targetType);
      } else if (targetType instanceof BoolType) {
        // != 0 for casts to boolean
        return of(!this.integer().equals(BigInteger.ZERO));
      } else if (targetType.bitWidth() < sourceType.bitWidth()) {
        // the current type is larger (so we just truncate)
        var truncatedValue = value
            .and(mask(targetType.bitWidth(), 0));
        return Value.fromTwosComplement(truncatedValue, targetType);
      } else if (sourceType.getClass() == SIntType.class) {
        // source type is SInt -> sign extend
        return signExtend(targetType);
      } else if (sourceType.getClass() == BitsType.class
          && targetType.getClass() == SIntType.class) {
        // source type is Bits and target type is SInt -> zero extend
        return signExtend(targetType);
      } else if (targetType.getClass() == UIntType.class
          || targetType.getClass() == BitsType.class
          || targetType.getClass() == SIntType.class) {
        // if the target type is one of UInt, Bits, SInt, we zero extend
        return zeroExtend(targetType);
      } else {
        throw new ViamError("Couldn't cast %s to %s. None of the rules apply.", this, targetType);
      }
    }

    /**
     * Returns the addition of this and other together with the status.
     *
     * @return a tuple constant of form {@code ( result, ( z, c, o, n ) )}
     */
    public Constant.Tuple add(Constant.Value other, boolean withCarrySet) {
      ensure(type() instanceof BitsType, "Invalid type for addition");
      ensure(type().isTrivialCastTo(other.type()), "Types don't match, %s vs %s", type(),
          other.type());

      // a + b + c where c is the carry flag
      var c = withCarrySet ? BigInteger.ONE : BigInteger.ZERO;
      var result = value.add(other.value).add(c);

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
          Constant.Tuple.status(isNegative, isZero, isCarry, isOverflow)
      );
    }

    /**
     * Subtracts the given value from this value.
     *
     * <p>While the carry flag is well-defined for addition, there are two ways in common use to
     * use the carry flag for subtraction operations.
     * The first uses the bit as a borrow flag, setting it if a < b when computing a-b, and a
     * borrow must be performed. If a>=b, the bit is cleared. The subtract with borrow (subb)
     * built-in function will compute a-b-C = a-(b+C), while a subtract without borrow (subsb)
     * acts as if the borrow bit were clear. The 8080, 6800, Z80, 8051, x86 and 68k families of
     * instruction set architectures use a borrow bit.
     *
     * <p>The second uses the identity that -x = not(x)+1 directly (i.e. without storing the carry
     * bit inverted) and computes a-b as a+not(b)+1.
     * The carry flag is set according to this addition,
     * and subtract with carry (subc) computes a+not(b)+C, while subtract without carry (subsc)
     * acts as if the carry bit were set. The result is that the carry bit is set if a>=b, and
     * clear if a < b. The System/360, 6502, MSP430, COP8, ARM and PowerPC instruction set
     * architectures use this convention. The 6502 is a particularly well-known example because it
     * does not have a subtract without carry operation, so programmers must ensure that the carry
     * flag is set before every subtract operation where a borrow is not required.</p>
     *
     * @param b the value to subtract from this value
     * @return a tuple constant representing the result of the subtraction (including status)
     * @see <a href="https://arc.net/l/quote/fmdsnowl">Wikipedia carry vs. borrow flag</a>
     */
    public Constant.Tuple subtract(Constant.Value b, SubMode mode, boolean withCarryBorrowSet) {
      ensure(type() instanceof BitsType, "Invalid type for subtraction");
      ensure(type().isTrivialCastTo(b.type()), "Types don't match, %s vs %s", type(), b.type());

      // calculation options:
      // X86_LIKE: a + not(b) + not(withCarryBorrow)
      // ARM_LIKE: a + not(b) + withCarryBorrow
      // checkout wikipedia for more
      //noinspection SimplifiableConditionalExpression
      var c = mode == SubMode.X86_LIKE
          ? !withCarryBorrowSet
          : withCarryBorrowSet;

      // The subtraction is performed by `a+not(b)+c`.
      // The value of c is either 0 or 1. Depending on the mode, the `c` is flipped.
      // Read more on the wiki page (https://en.wikipedia.org/wiki/Carry_flag)
      var notB = b.not();
      var result = this.add(notB, c);

      // swap the carry flag by inverse of addition open-vadl#76
      var resStatus = result.get(1, Constant.Tuple.Status.class);
      var carry = resStatus.carry();

      if (mode == SubMode.X86_LIKE) {
        // the carry of the subtraction is the inverse to the carry of the addition
        // in case of X86
        // -> a > b ... carry set, otherwise not
        carry = carry.not();
      }

      var overflow = resStatus.overflow();

      var status = new Constant.Tuple.Status(
          resStatus.negative(),
          resStatus.zero(),
          carry,
          overflow
      );

      return new Constant.Tuple(result.get(0), status);
    }

    /**
     * This utility subtraction will discard the status tuple.
     */
    private Constant.Value subtract(Constant.Value other) {
      return subtract(other, SubMode.X86_LIKE, false)
          .firstValue();
    }

    /**
     * Multiplies two constant values.
     *
     * <p>If longVersion is true, then the result type will be double the size of the operands,
     * otherwise it will be the same size
     *
     * @param other       the second operand
     * @param longVersion if the result should be double the size of the operands.
     * @param signed      if the multiplication should be signed or unsigned.
     *                    This is only in case of longVersion == true.
     *                    Otherwise, the value is ignored
     * @return the result of the multiplication. If longVersion is true, then the result type
     *     will be double the size of the operands, otherwise it will be the same size.
     */
    public Constant.Value multiply(Constant.Value other, boolean longVersion, boolean signed) {
      ensure(type().isTrivialCastTo(other.type()),
          "Multiplication requires same type but other was %s",
          other.type());

      if (longVersion) {
        var divType = signed
            ? Type.signedInt(type().bitWidth())
            : Type.unsignedInt(type().bitWidth());
        var a = this.trivialCastTo(divType);
        var b = other.trivialCastTo(divType);

        var newValue = a.integer()
            .multiply(b.integer()); // multiply with other value

        var newType = Type.constructDataType(divType.getClass(), 2 * divType.bitWidth());
        Objects.requireNonNull(newType);

        return fromInteger(newValue, newType);
      } else {
        // for the non-long version we truncate the result
        var newValue = value
            .multiply(other.value)
            .and(mask(type().bitWidth(), 0)); // truncate result
        return fromTwosComplement(newValue, type());
      }
    }

    /**
     * Divides this constant by the other one.
     * If the division would result in an overflow, it is undefined behavior.
     */
    public Constant.Value divide(Constant.Value other, boolean signed) {
      ensure(type().isTrivialCastTo(other.type()),
          "Division must be of same type, but other was %s",
          other.type());

      var divType = signed
          ? Type.signedInt(type().bitWidth())
          : Type.unsignedInt(type().bitWidth());
      Objects.requireNonNull(divType);

      var a = this.trivialCastTo(divType);
      var b = other.trivialCastTo(divType);


      if (b.value.equals(BigInteger.ZERO)) {
        // this is undefined behavior, however to avoid crashes we just emit warning and zero
        DeferredDiagnosticStore.add(
            warning("Division by zero", SourceLocation.INVALID_SOURCE_LOCATION)
                .description("There was some division by zero during constant evaluation.")
                .note("Unfortunately we are currently not able to provide location information.")
        );
        return zero(divType);
      }

      var newIntegerValue = a.integer()
          .divide(b.integer());

      if (signed && newIntegerValue.compareTo(maxValueOf(divType).integer()) > 0) {
        // overflow: undefined behavior, but to avoid crashes we just emit zero
        DeferredDiagnosticStore.add(
            warning("Division overflow", SourceLocation.INVALID_SOURCE_LOCATION)
                .description("There was some division with overflow during constant evaluation.")
                .note("Unfortunately we are currently not able to provide location information.")
        );
        return zero(divType);
      }

      return fromInteger(newIntegerValue, divType);
    }

    /**
     * Modulo of this constant divided by the other one.
     */
    public Constant.Value modulo(Constant.Value other, boolean signed) {
      ensure(type().isTrivialCastTo(other.type()),
          "Division must be of same type, but other was %s",
          other.type());

      var divType = signed
          ? Type.signedInt(type().bitWidth())
          : Type.unsignedInt(type().bitWidth());
      Objects.requireNonNull(divType);

      var a = this.trivialCastTo(divType);
      var b = other.trivialCastTo(divType);

      var newIntegerValue = a.integer()
          .mod(b.integer());
      return fromInteger(newIntegerValue, divType);
    }

    /**
     * Converts the constant to the given type, which must be a trivial cast.
     * The value remains the same, but the constant type changes.
     */
    public Constant.Value trivialCastTo(Type newType) {
      ensure(type().isTrivialCastTo(newType), "Trivial type from %s to %s is not possible.",
          type(), newType);
      if (type() == newType) {
        return this;
      }
      return fromTwosComplement(value, (DataType) newType);
    }

    /**
     * Enumeration of available subtraction modes.
     *
     * <p>The X86 like mode is also called <i>subtract with borrow</i>, while the
     * ARM like mode is also called <i>subtract with carry</i>. </p>
     *
     * @see #subtract(Value, SubMode, boolean)
     */
    public enum SubMode {
      X86_LIKE,
      ARM_LIKE
    }

    /**
     * Checks if the sign bit of the value is set.
     */
    public boolean isSignBit() {
      return value.testBit(type().bitWidth() - 1);
    }

    /**
     * Returns the bitwise negation of the current value object.
     *
     * @return the bitwise negation value of the current value object
     */
    public Constant.Value not() {
      var mask = mask(type().bitWidth(), 0);
      var notResult = value.xor(mask);
      return fromTwosComplement(notResult, type());
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
     * result and lead to a wrong value.
     *
     * @return a new Constant.Value object representing the negated value
     */
    public Constant.Value negate() {
      // calculate using 0 - this
      return zero(type())
          .subtract(this);
    }

    /**
     * Performs a bitwise AND. Both operands must have the same width.
     */
    public Constant.Value and(Constant.Value other) {
      ensureSameWidth(other);
      var andResult = this.value.and(other.value);
      return Constant.Value.fromTwosComplement(andResult, type());
    }

    /**
     * Performs a bitwise XOR. Both operands must have the same width.
     */
    public Constant.Value xor(Constant.Value other) {
      ensureSameWidth(other);
      var xorResult = this.value.xor(other.value);
      return Constant.Value.fromTwosComplement(xorResult, type());
    }

    /**
     * Performs a bitwise OR. Both operands must have the same width.
     */
    public Constant.Value or(Constant.Value other) {
      ensureSameWidth(other);
      var orResult = this.value.or(other.value);
      return Constant.Value.fromTwosComplement(orResult, type());
    }

    /**
     * Performs singed or and unsigned less comparison.
     */
    public Constant.Value lth(Constant.Value other, boolean singed) {
      return compare(other, singed, (a, b) -> a.compareTo(b) < 0);
    }

    /**
     * Performs singed or and unsigned less equals comparison.
     */
    public Constant.Value leq(Constant.Value other, boolean singed) {
      return compare(other, singed, (a, b) -> a.compareTo(b) <= 0);
    }

    /**
     * Performs singed or and unsigned greater comparison.
     */
    public Constant.Value gth(Constant.Value other, boolean singed) {
      return compare(other, singed, (a, b) -> a.compareTo(b) > 0);
    }

    /**
     * Performs singed or and unsigned less equals comparison.
     */
    public Constant.Value geq(Constant.Value other, boolean singed) {
      return compare(other, singed, (a, b) -> a.compareTo(b) >= 0);
    }

    private Constant.Value compare(Constant.Value other, boolean singed,
                                   BiFunction<BigInteger, BigInteger, Boolean> cmp) {
      boolean result;
      if (singed) {
        var thisBigInt = BigIntUtils.fromTwosComplement(value, type().bitWidth(), true);
        var otherBigInt =
            BigIntUtils.fromTwosComplement(other.value, other.type().bitWidth(), true);
        result = cmp.apply(thisBigInt, otherBigInt);
      } else {
        result = cmp.apply(value, other.value);
      }
      return Constant.Value.fromBoolean(result);
    }

    /**
     * Performs a logical shift left of this constant value by the specified amount
     * of the other value (which must be an unsigned integer).
     * The resulting type is the same as this type, the result is truncated on overflow.
     */
    public Constant.Value lsl(Constant.Value other) {
      other = other.trivialCastTo(Type.unsignedInt(other.type().bitWidth()));
      ensure(other.type().getClass() == UIntType.class,
          "LSL shift argument must be an unsigned integer.");

      var newValue = value
          .shiftLeft(other.intValue()) // shift value by other
          .and(mask(type().bitWidth(), 0)); // truncate value
      return fromTwosComplement(newValue, type());
    }

    /**
     * Performs a logical shift left of this constant value by the specified amount
     * of the other value (which must be an unsigned integer).
     * The resulting type is the same as this type, the result is truncated on overflow.
     */
    public Constant.Value lsr(Constant.Value other) {
      var shift = other;
      var valWidth = BigInteger.valueOf(type().bitWidth());
      if (shift.value.compareTo(valWidth) >= 0) {
        // if shift value is greather equal the value width, we must take the modulo
        shift = other.modulo(of(type().bitWidth(), other.type()), false);
      }
      var newValue = value
          .shiftRight(shift.intValue());
      return fromTwosComplement(newValue, type());
    }

    /**
     * Performs a rotation right of this constant value by the specified amount
     * of the other value (which must be an unsigned integer).
     * The resulting type is the same as this type, the result is truncated on overflow.
     */
    public Constant.Value ror(Constant.Value other) {
      int width = type().bitWidth();
      int amt = other.intValue() % width;
      if (amt == 0) {
        return this;                  // nothing to do
      }

      BigInteger mask = BigInteger.ONE.shiftLeft(width).subtract(BigInteger.ONE); // width-bit mask

      BigInteger rotated =
          value.shiftRight(amt)                     // lower part
              .or(value.shiftLeft(width - amt))    // wrapped-around part
              .and(mask);                          // truncate to <width> bits

      return fromTwosComplement(rotated, type());
    }

    /**
     * Truncates this value to the width of the newType argument.
     * The newType must have the same type class as this type and its with must be
     * less or equal to this constant's width.
     */
    public Constant.Value truncate(DataType newType) {
      ensure(type().bitWidth() >= newType.bitWidth(),
          "Truncated value's bitwidth must be less or equal to truncate type: %s", newType);

      if (newType.bitWidth() == type().bitWidth()) {
        // no truncation required
        return this;
      }

      var mask = mask(newType.bitWidth(), 0);
      var result = value.and(mask);
      return fromTwosComplement(result, newType);
    }

    /**
     * Concatenates the bits of two constants.
     *
     * @param other constant to be appended
     * @return a new concatenated constant.
     */
    public Constant.Value concat(Constant.Value other) {
      var type = Type.bits(this.type().bitWidth() + other.type().bitWidth());
      var result = Constant.Value.fromInteger(this.integer(), type);
      result =
          result.lsl(Constant.Value.fromInteger(BigInteger.valueOf(other.type().bitWidth()), type));
      result = result.or(Constant.Value.fromInteger(other.integer(), type));
      return result;
    }


    /**
     * Extract a sub-value by copying the bits addressed by {@code slice}
     * into a new {@link Constant.Value}.
     *
     * <p>Bits are taken from this value in the order delivered by
     * {@code slice.stream()} and packed densely, so the first index in the
     * slice becomes bit msb of the result, the second index becomes bit msb-1, and
     * so on. The resulting value is therefore {@code slice.bitSize()} bits wide
     * and is always interpreted as an <em>unsigned</em> (two’s-complement) integer.
     *
     * @param slice Bit positions to copy. All indices must satisfy
     *              {@code 0 <= index < type().bitWidth()}.
     * @return A new constant whose width equals {@code slice.bitSize()} and
     *     whose bit <i>i</i> equals this value’s bit at the
     *     <i>i</i>-th position of {@code slice}.
     */
    public Constant.Value slice(BitSlice slice) {
      ensure(slice.msb() < type().bitWidth(), "Slice accesses out of value width (msb >= width)");
      var result = BigInteger.ZERO;
      // reversed index positions, e.g. (4, 1..3) -> 3,2,1,4
      var idxPos = slice.stream().boxed().toList().reversed();
      for (var i = 0; i < idxPos.size(); i++) {
        var pos = idxPos.get(i);
        // build the result from 0 to slice.size - 1
        if (value.testBit(pos)) {
          // if the value has 1 at pos, then we set the bit in the result
          result = result.setBit(i);
        }
      }

      return fromTwosComplement(result, Type.bits(slice.bitSize()));
    }


    /**
     * Zero extends the value to the given type.
     */
    public Constant.Value zeroExtend(DataType newType) {
      ensure(type().bitWidth() <= newType.bitWidth(),
          "Value's bit-width must be less or equal to result type: %s", newType);
      // just create new constant with the new (bigger type)
      return fromTwosComplement(value, newType);
    }

    /**
     * Sign extends the value to the given type.
     */
    public Constant.Value signExtend(DataType newType) {
      ensure(type().bitWidth() <= newType.bitWidth(),
          "Value's bit-width must be less or equal to result type: %s", newType);

      var signSet = value.testBit(type().bitWidth() - 1);

      if (signSet) {
        var lenDiff = newType.bitWidth() - type().bitWidth();
        var shiftLeft = type().bitWidth();
        var bitMask = mask(lenDiff, shiftLeft);
        var result = value.or(bitMask);
        return fromTwosComplement(result, newType);
      } else {
        // sign not set -> no sign extension
        return fromTwosComplement(value, newType);
      }
    }

    @Override
    public java.lang.String toString() {
      return hexadecimal() + ": " + type().toString();
    }

    /**
     * Returns the maximal value for the given bits type as Constant.Value.
     */
    public static Constant.Value maxValueOf(BitsType type) {
      BigInteger result;
      if (type.isSigned()) {
        result = BigInteger.ONE.shiftLeft(type.bitWidth() - 1).subtract(BigInteger.ONE);
      } else {
        result = BigInteger.ONE.shiftLeft(type.bitWidth()).subtract(BigInteger.ONE);
      }
      return fromTwosComplement(result, type);
    }

    /**
     * Returns the minimal value for the given bits type as Constant.Value.
     */
    public static Constant.Value minValueOf(BitsType type) {
      BigInteger result;
      if (type.isSigned()) {
        result = BigInteger.ZERO.setBit(type.bitWidth() - 1);
      } else {
        result = BigInteger.ZERO;
      }
      return fromTwosComplement(result, type);
    }

    public static Constant.Value zero(DataType type) {
      return fromInteger(BigInteger.ZERO, type);
    }

    public static Constant.Value one(DataType type) {
      return fromInteger(BigInteger.ONE, type);
    }

    /**
     * Returns true if the memory value representation of this constant is equal to the
     * other constant.
     * This does not compare the types of the constants.
     */
    public boolean equalValue(Constant other) {
      if (!(other instanceof Constant.Value otherValue)) {
        return false;
      }
      return value.equals(otherValue.value);
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
      return asString(prefix, radix, false);
    }

    /**
     * Returns a string representing the value.
     *
     * @param prefix  string prefix
     * @param radix   used for numeric representation
     * @param padding if true, the String is padded with 0
     */
    public String asString(String prefix, int radix, boolean padding) {
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
      if (padding) {
        if (padFactor > 0) {
          var padSize = (type().bitWidth() / padFactor) - str.length();
          if (padSize > 0) {
            str = "0".repeat(padSize) + str;
          }
        }
      }
      return prefix + str;
    }

    // Helper functions

    public void ensureSameWidth(Constant.Value other) {
      ensure(type().bitWidth() == other.type().bitWidth(),
          "Type has not the same bit width as %s", other);
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
    public BitSlice(Part... parts) {
      super(Type.bitSlice());

      ViamError.ensure(parts.length > 0,
          "slice cannot be empty: %s", this);
      this.parts = normalized(parts);
      this.statistics = stream().summaryStatistics();
    }

    public static BitSlice of(int msb, int lsb) {
      return new BitSlice(new Part(msb, lsb));
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

    /**
     * Returns a stream with every bit which is a part of the bit slice.
     * Example: [(3, 6), (21, 21)] = [3, 4, 5, 6, 21]
     */
    public IntStream stream() {
      return this.parts.stream()
          .flatMapToInt(part -> StreamUtils.directionalRangeClosed(part.msb(), part.lsb()));
    }

    /**
     * Returns if the slice can be extracted from one consecutive slice of the value.
     * E.g. {@code [4..1]} is continuous while {@code [2, 4..3]} is not.
     *
     * @return true if slice is continuous, otherwise false.
     */
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

    public Constant.Value mask() {
      var mask = stream().boxed().reduce(BigInteger.ZERO, BigInteger::setBit, BigInteger::or);
      return Value.fromTwosComplement(mask, Type.bits(msb() + 1));
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

    @Nonnull
    @Override
    public Iterator<Integer> iterator() {
      return stream()
          .iterator();
    }

    /**
     * Determines if there are any overlapping parts in the slice.
     * This returns true if two or more parts are indexing the same position.
     */
    public boolean hasOverlappingParts() {
      return parts.stream()
          .anyMatch(part -> parts.stream()
              .anyMatch(
                  p -> !Objects.equals(p, part) && p.isOverlapping(part)
              ));
    }

    /**
     * Apply another slicing operation to the current bit slice. E.g.: for given a slice [17..13]
     * and applying the slice [2, 0] to it, results in the bit slice [15, 13].
     *
     * @param slice The additional slice to apply
     * @return The new bit slice, after applying the additional slicing operation.
     */
    public BitSlice apply(BitSlice slice) {

      if (slice.msb() > bitSize()) {
        throw new IllegalArgumentException("Cannot apply bit slice " + slice + " to " + this);
      }

      int[] baseIndices = stream().toArray();
      int[] sliceIndices = slice.stream().toArray();

      final Constant.BitSlice.Part[] newParts = new Constant.BitSlice.Part[sliceIndices.length];
      for (int idx = 0; idx < sliceIndices.length; idx++) {
        int si = baseIndices[baseIndices.length - 1 - sliceIndices[idx]];
        newParts[idx] = new Constant.BitSlice.Part(si, si);
      }

      return new Constant.BitSlice(newParts);
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

      @Nonnull
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
                  .map(e -> e.type)
                  .toArray(Type[]::new)
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
    public static Tuple.Status status(boolean negative, boolean zero, boolean carry,
                                      boolean overflow) {
      return new Tuple.Status(negative, zero, carry, overflow);
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

    /**
     * Retrieves the first constant value in the tuple.
     */
    public Constant.Value firstValue() {
      var val = values.stream().filter(e -> e instanceof Value).findFirst();
      ensure(val.isPresent(), "No constant value found in tuple");
      return (Constant.Value) val.get();
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

    /**
     * The constant of a status tuple containing the NZCV values (negative, zero, carry, overflow).
     */
    public static class Status extends Tuple {

      /**
       * Constructs a status constant by Java boolean value.
       */
      public Status(boolean negative, boolean zero, boolean carry, boolean overflow) {
        super(
            List.of(Constant.Value.of(negative), Constant.Value.of(zero), Constant.Value.of(carry),
                Constant.Value.of(overflow)
            ), Type.status()
        );
      }

      /**
       * Constructs a status constant by value constants.
       * All constants must have the type boolean.
       */
      public Status(Constant.Value negative, Constant.Value zero, Constant.Value carry,
                    Constant.Value overflow
      ) {
        super(negative, zero, carry, overflow);

        ensure(values().stream().allMatch(e -> e.type() == Type.bool()),
            "A status' values must all be bools");
      }

      public Constant.Value negative() {
        return get(0, Value.class);
      }

      public Constant.Value zero() {
        return get(1, Value.class);
      }

      public Constant.Value carry() {
        return get(2, Value.class);
      }

      public Constant.Value overflow() {
        return get(3, Value.class);
      }

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

