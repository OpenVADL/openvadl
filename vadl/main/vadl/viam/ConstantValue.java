package vadl.viam;

import java.math.BigInteger;
import vadl.types.Type;

/**
 * Represents a constant value with a specific type.
 */
public record ConstantValue(
    BigInteger value,
    Type type
) {
  
  public static ConstantValue of(long value, Type type) {
    return new ConstantValue(BigInteger.valueOf(value), type);
  }

}
