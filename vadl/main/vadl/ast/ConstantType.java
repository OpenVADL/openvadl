package vadl.ast;

import java.math.BigInteger;
import vadl.types.Type;

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
}
