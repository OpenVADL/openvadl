package vadl.vdt.utils;

/**
 * Represents a single bit.
 *
 * @param value the value of the bit (true for 1, false for 0)
 */
public record Bit(boolean value) implements BitWise<Bit> {

  @Override
  public Bit and(Bit other) {
    return this.value && other.value ? new Bit(true) : new Bit(false);
  }

  @Override
  public Bit or(Bit other) {
    return this.value || other.value ? new Bit(true) : new Bit(false);
  }

  @Override
  public Bit xor(Bit other) {
    return this.value ^ other.value ? new Bit(true) : new Bit(false);
  }

  @Override
  public Bit not() {
    return !this.value ? new Bit(true) : new Bit(false);
  }

  @Override
  public int hashCode() {
    return this.value ? 1 : 0;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof Bit)) {
      return false;
    }
    return this.value == ((Bit) obj).value;
  }
}
