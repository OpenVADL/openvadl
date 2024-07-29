package vadl.viam;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import vadl.types.Type;


public class ConstantTests {


  // Bitslice tests

  @Test
  public void bitSliceSize() {
    Constant.BitSlice.Part part = new Constant.BitSlice.Part(5, 1);
    assertEquals(5, part.size());
  }

  @Test
  public void bitSliceIsIndex() {
    Constant.BitSlice.Part part = new Constant.BitSlice.Part(1, 1);
    assertTrue(part.isIndex());
  }

  @Test
  public void bitSliceIsRange() {
    Constant.BitSlice.Part part = new Constant.BitSlice.Part(5, 1);
    assertTrue(part.isRange());
  }

  @Test
  public void bitSliceJoin() {
    Constant.BitSlice.Part part1 = new Constant.BitSlice.Part(5, 1);
    Constant.BitSlice.Part part2 = new Constant.BitSlice.Part(3, 0);
    assertEquals(new Constant.BitSlice.Part(5, 0), part1.join(part2));
  }

  @Test
  public void bitSliceWithin() {
    Constant.BitSlice.Part part1 = new Constant.BitSlice.Part(5, 1);
    Constant.BitSlice.Part part2 = new Constant.BitSlice.Part(3, 2);
    assertTrue(part2.isSurroundedBy(part1));
  }

  @Test
  public void bitSliceNotWithin() {
    Constant.BitSlice.Part part1 = new Constant.BitSlice.Part(5, 1);
    Constant.BitSlice.Part part2 = new Constant.BitSlice.Part(6, 2);
    assertFalse(part2.isSurroundedBy(part1));
  }

  @Test
  public void bitSliceJoinNonOverlapping() {
    Constant.BitSlice.Part part1 = new Constant.BitSlice.Part(5, 3);
    Constant.BitSlice.Part part2 = new Constant.BitSlice.Part(2, 0);
    assertEquals(new Constant.BitSlice.Part(5, 0), part1.join(part2));
  }

  @Test
  public void bitSliceOverlappingOverlapping() {
    Constant.BitSlice.Part part1 = new Constant.BitSlice.Part(5, 3);
    Constant.BitSlice.Part part2 = new Constant.BitSlice.Part(3, 0);
    assertTrue(part1.isOverlapping(part2));
  }

  @Test
  public void bitSliceOverlappingOverlapping2() {
    Constant.BitSlice.Part part1 = new Constant.BitSlice.Part(5, 0);
    Constant.BitSlice.Part part2 = new Constant.BitSlice.Part(3, 1);
    assertTrue(part1.isOverlapping(part2));
  }

  @Test
  public void bitSliceOverlappingNonOverlapping() {
    Constant.BitSlice.Part part1 = new Constant.BitSlice.Part(5, 3);
    Constant.BitSlice.Part part2 = new Constant.BitSlice.Part(2, 0);
    assertFalse(part1.isOverlapping(part2));
  }

  @Test
  public void bitSliceSingleIndexSize() {
    Constant.BitSlice.Part part = new Constant.BitSlice.Part(1, 1);
    assertEquals(1, part.size());
  }

  @Test
  public void bitSliceCreationInvalidArguments() {
    assertThrows(ViamError.class, () -> new Constant.BitSlice.Part(1, 5));
    assertThrows(ViamError.class, () -> new Constant.BitSlice.Part(-1, 0));
  }

  @Test
  public void bitSliceCreationNormalized() {
    Constant.BitSlice bitSlice = new Constant.BitSlice(new Constant.BitSlice.Part[] {
        new Constant.BitSlice.Part(5, 3),
        new Constant.BitSlice.Part(2, 0)}
    );
    assertEquals(1, bitSlice.partSize());
    assertEquals(6, bitSlice.bitSize());
    var parts = bitSlice.parts().toList();
    assertEquals(new Constant.BitSlice.Part(5, 0), parts.get(0));

    var bitPositions = StreamSupport.stream(bitSlice.spliterator(), false).toList();
    assertEquals(List.of(5, 4, 3, 2, 1, 0), bitPositions);
  }

  @Test
  public void bitSliceCreationNormalized2() {
    Constant.BitSlice bitSlice = new Constant.BitSlice(new Constant.BitSlice.Part[] {
        new Constant.BitSlice.Part(0, 0),
        new Constant.BitSlice.Part(7, 5),
        new Constant.BitSlice.Part(4, 4),
        new Constant.BitSlice.Part(3, 2),
        new Constant.BitSlice.Part(9, 8)
    }
    );
    assertEquals(3, bitSlice.partSize());
    assertEquals(9, bitSlice.bitSize());
    assertEquals(List.of(
        new Constant.BitSlice.Part(0, 0),
        new Constant.BitSlice.Part(7, 2),
        new Constant.BitSlice.Part(9, 8)
    ), bitSlice.parts().toList());

    var bitPositions = StreamSupport.stream(bitSlice.spliterator(), false).toList();
    assertEquals(List.of(0, 7, 6, 5, 4, 3, 2, 9, 8), bitPositions);

  }

  @ParameterizedTest
  @MethodSource("testAddSources")
  void testAdd(Constant.Value a, Constant.Value b, long result, boolean isZero, boolean carry,
               boolean overflow, boolean isNegative) {
    var actual = a.add(b);
    testResultAndStatus(actual, result, isZero, carry, overflow, isNegative);
  }

  @ParameterizedTest
  @MethodSource("testSubSources")
  void testSub(Constant.Value a, Constant.Value b, long result, boolean isZero, boolean carry,
               boolean overflow, boolean isNegative) {
    var actual = a.subtract(b);
    testResultAndStatus(actual, result, isZero, carry, overflow, isNegative);
  }

  private void testResultAndStatus(Constant.Tuple actual, long result, boolean isZero,
                                   boolean carry,
                                   boolean overflow, boolean isNegative) {
    var res = actual.get(0, Constant.Value.class);

    assertEquals(result, res.integer().intValue(), "Wrong result value");
    // test status
    var status = actual.get(1, Constant.Tuple.class);
    assertEquals(isZero, status.get(0, Constant.Value.class).integer().equals(BigInteger.ONE),
        "Wrong zero flag");
    assertEquals(carry, status.get(1, Constant.Value.class).integer().equals(BigInteger.ONE),
        "Wrong carry flag");
    assertEquals(overflow, status.get(2, Constant.Value.class).integer().equals(BigInteger.ONE),
        "Wrong overflow flag");
    assertEquals(isNegative, status.get(3, Constant.Value.class).integer().equals(BigInteger.ONE),
        "Wrong negative flag");
  }

  static Stream<Arguments> testAddSources() {
    return Stream.of(
        Arguments.of(valS(2, 4), valS(3, 4), 5, false, false, false, false),
        Arguments.of(valS(-2, 4), valS(3, 4), 1, false, true, false, false),
        Arguments.of(valS(7, 4), valS(1, 4), -8, false, false, true, true),
        Arguments.of(valS(-7, 4), valS(-2, 4), 7, false, true, true, false),

        Arguments.of(valU(1, 4), valU(4, 4), 5, false, false, false, false),
        Arguments.of(valU(7, 4), valU(1, 4), 8, false, false, true, true),
        Arguments.of(valU(8, 4), valU(8, 4), 0, true, true, true, false),

        Arguments.of(valU(80, 8), valU(80, 8), 160, false, false, true, true),
        Arguments.of(valS(80, 8), valS(80, 8), -96, false, false, true, true),
        Arguments.of(valS(80, 8), valS(-48, 8), 32, false, true, false, false),

        Arguments.of(valS(0b111, 4), valS(0b0001, 4), -8, false, false, true, true),
        Arguments.of(valU(0b111, 4), valU(0b0001, 4), 0b1000, false, false, true, true),
        Arguments.of(valU(0b1000, 4), valU(0b1111, 4), 0b111, false, true, true, false),

        Arguments.of(valU(0b1111, 4), valU(0b1111, 4), 0b1110, false, true, false, true)
    );
  }

  // TODO: Update as soon as https://ea.complang.tuwien.ac.at/vadl/open-vadl/issues/76 is resolved
  static Stream<Arguments> testSubSources() {
    return Stream.of(
        Arguments.of(valS(-2, 4), valS(3, 4), -5, false, false, false, true),

        Arguments.of(valU(80, 8), valU(176, 8), 160, false, true, true, true),
        Arguments.of(valS(2, 3), valS(-4, 3), -2, false, true, true, true),
        Arguments.of(valS(-2, 3), valS(-4, 3), 2, false, false, false, false),

        Arguments.of(valS(0, 4), valS(1, 4), -1, false, true, false, true),
        Arguments.of(valS(-8, 4), valS(0b0001, 4), 0b111, false, false, true, false),
        Arguments.of(valU(0b1111, 4), valU(0b0001, 4), 0b1110, false, false, false, true),

        Arguments.of(valU(0b1000, 4), valU(0b1000, 4), 0b0, true, false, false, false),

        Arguments.of(valU(0b0000, 4), valU(0b1000, 4), 0b1000, false, true, true, true),

        // Tested on m1 ARM64
        // Every test is done in signed and unsigned form. In both cases the flags must be the same.
        Arguments.of(valU(0xFFFFFFFFL, 32), valU(0xFFFFFFFFL, 32), 0x0, true, false, false, false),
        Arguments.of(valS(-0x1, 32), valS(-0x1, 32), 0x0, true, false, false, false),

        Arguments.of(valU(0x0, 32), valU(0x1, 32), 0xFFFFFFFF, false, true, false, true),
        Arguments.of(valS(0x0, 32), valS(0x1, 32), -0x1, false, true, false, true),

        Arguments.of(valU(0x80000000L, 32), valU(0x1, 32), 0x7FFFFFFF, false, false, true, false),
        Arguments.of(valS(-2147483648, 32), valS(0x1, 32), 0x7FFFFFFF, false, false, true, false),

        Arguments.of(valU(0x1, 32), valU(0x80000000L, 32), -2147483647, false, true, true, true)
    );
  }


  // Helper functions

  private static Constant.Value valS(int val, int width) {
    return Constant.Value.of(val, Type.signedInt(width));
  }

  private static Constant.Value valS(long val, int width) {
    return Constant.Value.of(val, Type.signedInt(width));
  }

  private static Constant.Value valU(int val, int width) {
    return Constant.Value.of(val, Type.unsignedInt(width));
  }

  private static Constant.Value valU(long val, int width) {
    return Constant.Value.of(val, Type.unsignedInt(width));
  }


}
