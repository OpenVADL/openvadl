package vadl.ast;

import java.math.BigInteger;
import vadl.types.BitsType;
import vadl.types.SIntType;
import vadl.types.Type;
import vadl.types.UIntType;

/**
 * A type for constants that voids many casts.
 *
 * <p>Constants have the type of the value they hold.
 */
public class ConstantType extends Type {
  private final BigInteger value;

  public ConstantType(BigInteger value) {
    this.value = value;
  }

  BigInteger getValue() {
    return value;
  }

  int requiredBitWidth() {
    var isNegative = value.compareTo(BigInteger.ZERO) < 0;
    return value.bitLength() + (isNegative ? 1 : 0);
  }

  SIntType closestSInt() {
    return Type.signedInt(requiredBitWidth());
  }

  UIntType closestUInt() {
    return Type.unsignedInt(requiredBitWidth());
  }

  BitsType closestBits() {
    return Type.bits(requiredBitWidth());
  }

  Type closestTo(Type target) {
    if (target instanceof SIntType targetSInt) {
      var bitsWidth = Math.min(targetSInt.bitWidth(), requiredBitWidth());
      return Type.signedInt(bitsWidth);
    }

    if (target instanceof UIntType targetUInt) {
      var bitsWidth = Math.min(targetUInt.bitWidth(), requiredBitWidth());
      if (value.compareTo(BigInteger.ZERO) < 0) {
        // Cannot pack into uint is negative, closest is still SInt
        return Type.signedInt(bitsWidth);
      }
      return Type.unsignedInt(bitsWidth);
    }

    if (target instanceof BitsType targetBits) {
      var bitsWidth = Math.min(targetBits.bitWidth(), requiredBitWidth());

      if (value.compareTo(BigInteger.ZERO) < 0) {
        // Cannot pack into uint is negative, closest is still SInt
        return Type.signedInt(bitsWidth);
      }
      return Type.bits(bitsWidth);
    }

    return this;
  }

  @Override
  public String name() {
    return "Const<%s>".formatted(value);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ConstantType that = (ConstantType) o;
    return value.equals(that.value);
  }

  @Override
  public int hashCode() {
    return value.hashCode();
  }
}
