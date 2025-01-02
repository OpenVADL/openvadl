package vadl.vdt.utils;

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
}
