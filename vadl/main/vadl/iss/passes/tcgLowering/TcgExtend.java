package vadl.iss.passes.tcgLowering;

/**
 * Enum representing extension modes for TCG (Tiny Code Generation) operations.
 * The modes specify how to handle the extension of values when manipulating data of varying sizes.
 *
 * <p>The two modes are:
 * <ul>
 *    <li>SIGN: Sign-extend the value, preserving the sign bit.</li>
 *    <li>ZERO: Zero-extend the value, filling with zeroes.</li>
 * </ul>
 */
public enum TcgExtend {
  SIGN,
  ZERO;

  public static TcgExtend fromBoolean(boolean value) {
    return value ? SIGN : ZERO;
  }

}
