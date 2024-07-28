package vadl.utils;

import java.math.BigInteger;
import vadl.error.VadlException;
import vadl.types.BitsType;
import vadl.viam.ViamError;

public class BigIntUtils {

  public static BigInteger twosComplement(BigInteger value, int bitWidth) {
    if (value.bitLength() > bitWidth) {
      throw new ViamError(
          "Unexpected value %s for bit-width %s. Value does not fit in size.".formatted(value,
              bitWidth));
    }

    if (value.signum() >= 0) {
      // positive values are already in twos complement
      return value;
    }

    var oneBeforeWidth =
        BigInteger.ZERO.setBit(bitWidth);          // (msb|sign) msb-1 ... b0 := ( 1, 0, ..., 0 )
    var maxUnsigned =
        oneBeforeWidth.subtract(BigInteger.ONE);   // (msb|sign) msb-1 ... b0 := ( 0, 1, ..., 1 )
    var maxAbs =
        BigInteger.ZERO.setBit(bitWidth - 1);      // (msb|sign) msb-1 ... b0 := ( 0, 1, ..., 0 )

    var absValue = value.negate();

    if (absValue.compareTo(maxAbs) > 0) {
      throw new ViamError("Unexpected value %s for bit-width %s. Value is out of range (max: %s)."
          .formatted(value, bitWidth, maxAbs));
    }

    // to obtain the two's complement from an absValue, we apply
    // (abs( value ) - 1) ^ maxUnsigned
    return absValue.subtract(BigInteger.ONE).xor(maxUnsigned);
  }

  public static BigInteger onesComplement(BigInteger value, int bitWidth) {
    var mask = mask(bitWidth, 0);
    return value.xor(mask);
  }

  public static BigInteger fromTwosComplement(BigInteger value, BitsType type) {
    var maxUnsigned = BigInteger.ZERO.setBit(type.bitWidth()).subtract(BigInteger.ONE);
    var isSignBit = value.testBit(type.bitWidth() - 1); // check if sign bit active

    if (type.isSigned() && isSignBit) {
      //  negate  inverse        inc
      // - ((value ^ maxUnsigned) + 1)
      return value.xor(maxUnsigned).add(BigInteger.ONE).negate();
    }
    return value;
  }

  public static BigInteger setBitsInRange(BigInteger number, int from, int to) {
    var diff = Math.abs((from - to)) + 1;
    var lsb = Math.min(to, from);

    var mask = BigInteger.ONE.shiftLeft(diff)
        .subtract(BigInteger.ONE)
        .shiftLeft(lsb);

    return number.or(mask);
  }

  public static BigInteger clearBitsInRange(BigInteger number, int from, int to) {
    var diff = Math.abs((from - to)) + 1;
    var lsb = Math.min(to, from);

    var blackMask = BigInteger.ONE.shiftLeft(number.bitLength())
        .subtract(BigInteger.ONE);
    var whiteMask = BigInteger.ONE.shiftLeft(diff)
        .subtract(BigInteger.ONE)
        .shiftLeft(lsb);

    var mask = blackMask.xor(whiteMask);
    return number.and(mask);
  }

  public static BigInteger mask(int len, int lsh) {
    return BigInteger.ONE.shiftLeft(len).subtract(BigInteger.ONE).shiftLeft(lsh);
  }

  public static BigInteger unsigned(BigInteger value) {
    if (value.signum() >= 0) {
      return value;
    }
    return new BigInteger(1, value.toByteArray());
  }

}
