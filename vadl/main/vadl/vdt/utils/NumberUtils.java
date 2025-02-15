package vadl.vdt.utils;

import java.math.BigInteger;

public class NumberUtils {

  private NumberUtils() {
    // Utility class, no instantiation
  }

  /**
   * Find the smallest power of two that is greater or equal to n.
   *
   * @param n the input number
   * @return the smallest fitting power of two
   */
  public static int fittingPowerOfTwo(int n) {
    final BigInteger bigN = BigInteger.valueOf(n);
    if (bigN.compareTo(BigInteger.ZERO) <= 0) {
      throw new IllegalArgumentException("Input must be a positive integer");
    }
    if (bigN.getLowestSetBit() == bigN.bitLength() - 1) {
      // n is already a power of two
      return n;
    }
    return BigInteger.ONE.shiftLeft(bigN.bitLength()).intValue();
  }
}
