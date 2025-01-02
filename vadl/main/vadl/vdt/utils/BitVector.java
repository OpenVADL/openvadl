package vadl.vdt.utils;

import java.math.BigInteger;
import java.util.Objects;

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
        if (value.charAt(i) != '0' && value.charAt(i) != '1') {
          throw new IllegalArgumentException("Invalid character in value");
        }
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
      if (get(width() - (i + 1)).value()) {
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

  public BitVector and(Vector<Bit> other) {
    final Bit[] result = new Bit[width()];
    for (int i = 0; i < width(); i++) {
      result[i] = get(i).and(other.get(i));
    }
    return new BitVector(result);
  }

  public BitVector or(Vector<Bit> other) {
    final Bit[] result = new Bit[width()];
    for (int i = 0; i < width(); i++) {
      result[i] = get(i).or(other.get(i));
    }
    return new BitVector(result);
  }

  public BitVector xor(Vector<Bit> other) {
    final Bit[] result = new Bit[width()];
    for (int i = 0; i < width(); i++) {
      result[i] = get(i).xor(other.get(i));
    }
    return new BitVector(result);
  }

  public BitVector not() {
    final Bit[] result = new Bit[width()];
    for (int i = 0; i < width(); i++) {
      result[i] = get(i).not();
    }
    return new BitVector(result);
  }

  public BitVector shiftLeft(int n, boolean fill) {
    final Bit[] result = new Bit[width()];
    for (int i = n; i < width(); i++) {
      result[i - n] = get(i);
    }
    for (int i = width() - n; i < width(); i++) {
      result[i] = fill ? new Bit(true) : new Bit(false);
    }
    return new BitVector(result);
  }

  public BitVector shiftRight(int n, boolean fill) {
    final Bit[] result = new Bit[width()];
    for (int i = 0; i < n; i++) {
      result[i] = fill ? new Bit(true) : new Bit(false);
    }
    for (int i = n; i < width(); i++) {
      result[i] = get(i - n);
    }
    return new BitVector(result);
  }

  @Override
  public int hashCode() {
    int result = 1;
    for (int i = 0; i < width(); i++) {
      result = 31 * result + Objects.hashCode(get(i));
    }
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final BitVector other = (BitVector) obj;
    if (width() != other.width()) {
      return false;
    }
    for (int i = 0; i < width(); i++) {
      if (!Objects.equals(get(i), other.get(i))) {
        return false;
      }
    }
    return true;
  }

  @Override
  public String toString() {
    final StringBuilder builder = new StringBuilder();
    for (int i = 0; i < width(); i++) {
      builder.append(get(i).value() ? '1' : '0');
    }
    return builder.toString();
  }
}
