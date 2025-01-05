package vadl.ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import vadl.types.BitsType;

/**
 * A helper class to determine if a given format fills all the bits it needs to.
 */
class FormatBitsVerifier {
  private final int bitWidth;
  private List<Integer> bitsUssages;
  private List<BitsType> typesToFill = new ArrayList<>();

  FormatBitsVerifier(int bitWidth) {
    this.bitWidth = bitWidth;
    bitsUssages = new ArrayList<>(Collections.nCopies(bitWidth, 0));
  }

  void addType(BitsType type) {
    typesToFill.add(type);
  }

  void addRange(int from, int to) {
    for (int i = to; i <= from; i++) {
      bitsUssages.set(i, bitsUssages.get(i) + 1);
    }
  }

  boolean hasViolations() {
    return getViolationsMessage() != null;
  }

  @Nullable
  String getViolationsMessage() {
    // FIXME: For now this class is a bit of a hack because the propper solution is a lot more
    //  involved with finding the correct fields.
    if (!typesToFill.isEmpty() && !bitsUssages.stream().allMatch(u -> u == 0)) {
      return "For now mixed ranges and type fields aren't yet supported";
    }

    if (!typesToFill.isEmpty()) {
      var actualBitWidth = typesToFill.stream()
          .map(t -> t.bitWidth())
          .reduce((a, b) -> a + b)
          .get();

      if (actualBitWidth != bitWidth) {
        return "Declared as bitwidth of size %d but actually size is %d".formatted(bitWidth,
            actualBitWidth);
      }
      return null;
    }

    var unUsedBits = new ArrayList<Integer>();
    var doubleUsedBits = new ArrayList<Integer>();
    for (var i = 0; i < bitWidth; i++) {
      if (bitsUssages.get(i) == 0) {
        unUsedBits.add(i);
      }

      if (bitsUssages.get(i) > 1) {
        doubleUsedBits.add(i);
      }
    }

    var message = "";
    if (!unUsedBits.isEmpty()) {
      message += "The following bits are unused: "
          + unUsedBits.stream()
          .map(String::valueOf)
          .collect(Collectors.joining(", "));
    }
    if (!doubleUsedBits.isEmpty()) {
      message += "The following bits are multiple times: "
          + doubleUsedBits.stream()
          .map(String::valueOf)
          .collect(Collectors.joining(", "));
    }

    return message.isEmpty() ? null : message;
  }

}
