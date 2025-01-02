package vadl.vdt.utils;

import java.util.Objects;
import java.util.function.Predicate;

public class BitPattern implements Vector<PBit>, Predicate<BitVector> {

  private final PBit[] bits;

  public BitPattern(PBit[] bits) {
    this.bits = bits;
  }

  @Override
  public int width() {
    return bits.length;
  }

  @Override
  public PBit get(int i) {
    return bits[i];
  }

  @Override
  public boolean test(BitVector bitVector) {
    if (bitVector.width() != width()) {
      return false;
    }
    for (int i = 0; i < width(); i++) {
      if (get(i).getValue() != PBit.Value.DONT_CARE && (
          (get(i).getValue() == PBit.Value.ONE && !bitVector.get(i).value()) || (
              get(i).getValue() == PBit.Value.ZERO && bitVector.get(i).value()))) {
        return false;
      }
    }
    return true;
  }

  public static BitPattern fromString(String pattern, int width) {
    final PBit[] bits = new PBit[width];
    if (pattern.length() != width) {
      throw new IllegalArgumentException("Pattern length must match width");
    }
    for (int i = 0; i < pattern.length(); i++) {
      bits[i] = new PBit(pattern.charAt(i) == '1' ? PBit.Value.ONE
          : (pattern.charAt(i) == '0' ? PBit.Value.ZERO : PBit.Value.DONT_CARE));
    }
    return new BitPattern(bits);
  }

  public boolean doesMatchAll() {
    for (int i = 0; i < width(); i++) {
      if (get(i).getValue() != PBit.Value.DONT_CARE) {
        return false;
      }
    }
    return true;
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
    final BitPattern other = (BitPattern) obj;
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
    final StringBuilder sb = new StringBuilder();
    for (int i = 0; i < width(); i++) {
      sb.append(get(i).getValue() == PBit.Value.ONE ? '1' : (
          get(i).getValue() == PBit.Value.ZERO ? '0' : '-'));
    }
    return sb.toString();
  }
}
