package vadl.vdt.utils;

import java.math.BigInteger;

public class BitVector implements Vector<Bit> {

  private final Bit[] bits;

  public BitVector(Bit[] bits) {
    this.bits = bits;
  }

  public static BitVector fromValue(BigInteger value, int width) {
    final Bit[] bits = new Bit[width];
    for (int i = 0; i < width; i++) {
      bits[i] = new Bit(value.testBit(i));
    }
    return new BitVector(bits);
  }

  public static BitVector fromString(String value, int width) {
    final Bit[] bits = new Bit[width];
    for (int i = 0; i < width; i++) {
      if (i < value.length()) {
        bits[i] = new Bit(value.charAt(i) == '1');
      } else {
        bits[i] = new Bit(false);
      }
    }
    return new BitVector(bits);
  }

  public BigInteger toValue() {
    BigInteger value = BigInteger.ZERO;
    for (int i = 0; i < width(); i++) {
      if (get(i).value()) {
        value = value.setBit(i);
      }
    }
    return value;
  }

  @Override
  public int width() {
    return bits.length;
  }

  @Override
  public Bit get(int i) {
    return bits[i];
  }

  public Vector<Bit> and(Vector<Bit> other) {
    final Bit[] result = new Bit[width()];
    for (int i = 0; i < width(); i++) {
      result[i] = get(i).and(other.get(i));
    }
    return new BitVector(result);
  }

  public Vector<Bit> or(Vector<Bit> other) {
    final Bit[] result = new Bit[width()];
    for (int i = 0; i < width(); i++) {
      result[i] = get(i).or(other.get(i));
    }
    return new BitVector(result);
  }

  public Vector<Bit> xor(Vector<Bit> other) {
    final Bit[] result = new Bit[width()];
    for (int i = 0; i < width(); i++) {
      result[i] = get(i).xor(other.get(i));
    }
    return new BitVector(result);
  }

  public Vector<Bit> not() {
    final Bit[] result = new Bit[width()];
    for (int i = 0; i < width(); i++) {
      result[i] = get(i).not();
    }
    return new BitVector(result);
  }

  public Vector<Bit> shiftLeft(int n, boolean fill) {
    final Bit[] result = new Bit[width()];
    for (int i = n; i < width(); i++) {
      result[i - n] = get(i);
    }
    for (int i = width() - n; i < width(); i++) {
      result[i] = fill ? new Bit(true) : new Bit(false);
    }
    return new BitVector(result);
  }

  public Vector<Bit> shiftRight(int n, boolean fill) {
    final Bit[] result = new Bit[width()];
    for (int i = 0; i < n; i++) {
      result[i] = fill ? new Bit(true) : new Bit(false);
    }
    for (int i = n; i < width(); i++) {
      result[i] = get(i - n);
    }
    return new BitVector(result);
  }
}
