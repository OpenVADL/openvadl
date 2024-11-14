package vadl.ast;

import java.math.BigInteger;
import vadl.types.Type;

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

  public BigInteger getValue() {
    return value;
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
