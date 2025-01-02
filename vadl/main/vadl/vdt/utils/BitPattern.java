package vadl.vdt.utils;

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
}
