package vadl.viam;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static vadl.viam.helper.TestGraphUtils.bool;
import static vadl.viam.helper.TestGraphUtils.intS;
import static vadl.viam.helper.TestGraphUtils.intU;

import java.math.BigInteger;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import vadl.types.DataType;
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
  void constantAddition_shouldYieldCorrectValue(Constant.Value a, Constant.Value b, long result,
                                                boolean isZero, boolean carry,
                                                boolean overflow, boolean isNegative) {
    var actual = a.add(b);
    testResultAndStatus(actual, result, isZero, carry, overflow, isNegative);
  }


  static Stream<Arguments> testAddSources() {
    return Stream.of(
        Arguments.of(intS(2, 4), intS(3, 4), 5, false, false, false, false),
        Arguments.of(intS(-2, 4), intS(3, 4), 1, false, true, false, false),
        Arguments.of(intS(7, 4), intS(1, 4), -8, false, false, true, true),
        Arguments.of(intS(-7, 4), intS(-2, 4), 7, false, true, true, false),

        Arguments.of(intU(1, 4), intU(4, 4), 5, false, false, false, false),
        Arguments.of(intU(7, 4), intU(1, 4), 8, false, false, true, true),
        Arguments.of(intU(8, 4), intU(8, 4), 0, true, true, true, false),

        Arguments.of(intU(80, 8), intU(80, 8), 160, false, false, true, true),
        Arguments.of(intS(80, 8), intS(80, 8), -96, false, false, true, true),
        Arguments.of(intS(80, 8), intS(-48, 8), 32, false, true, false, false),

        Arguments.of(intS(0b111, 4), intS(0b0001, 4), -8, false, false, true, true),
        Arguments.of(intU(0b111, 4), intU(0b0001, 4), 0b1000, false, false, true, true),
        Arguments.of(intU(0b1000, 4), intU(0b1111, 4), 0b111, false, true, true, false),

        Arguments.of(intU(0b1111, 4), intU(0b1111, 4), 0b1110, false, true, false, true)
    );
  }

  @ParameterizedTest
  @MethodSource("testSubSources")
  void constantSubtraction_shouldYieldCorrectValue(Constant.Value a, Constant.Value b, long result,
                                                   boolean isZero, boolean carry,
                                                   boolean overflow, boolean isNegative) {
    var actual = a.subtract(b);
    testResultAndStatus(actual, result, isZero, carry, overflow, isNegative);
  }

  // TODO: Update as soon as https://ea.complang.tuwien.ac.at/vadl/open-vadl/issues/76 is resolved
  static Stream<Arguments> testSubSources() {
    return Stream.of(
        Arguments.of(intS(-2, 4), intS(3, 4), -5, false, false, false, true),

        Arguments.of(intU(80, 8), intU(176, 8), 160, false, true, true, true),
        Arguments.of(intS(2, 3), intS(-4, 3), -2, false, true, true, true),
        Arguments.of(intS(-2, 3), intS(-4, 3), 2, false, false, false, false),

        Arguments.of(intS(0, 4), intS(1, 4), -1, false, true, false, true),
        Arguments.of(intS(-8, 4), intS(0b0001, 4), 0b111, false, false, true, false),
        Arguments.of(intU(0b1111, 4), intU(0b0001, 4), 0b1110, false, false, false, true),

        Arguments.of(intU(0b1000, 4), intU(0b1000, 4), 0b0, true, false, false, false),

        Arguments.of(intU(0b0000, 4), intU(0b1000, 4), 0b1000, false, true, true, true),

        // Tested on m1 ARM64
        // Every test is done in signed and unsigned form. In both cases the flags must be the same.
        Arguments.of(intU(0xFFFFFFFFL, 32), intU(0xFFFFFFFFL, 32), 0x0, true, false, false, false),
        Arguments.of(intS(-0x1, 32), intS(-0x1, 32), 0x0, true, false, false, false),

        Arguments.of(intU(0x0, 32), intU(0x1, 32), 0xFFFFFFFF, false, true, false, true),
        Arguments.of(intS(0x0, 32), intS(0x1, 32), -0x1, false, true, false, true),

        Arguments.of(intU(0x80000000L, 32), intU(0x1, 32), 0x7FFFFFFF, false, false, true, false),
        Arguments.of(intS(-2147483648, 32), intS(0x1, 32), 0x7FFFFFFF, false, false, true, false),

        Arguments.of(intU(0x1, 32), intU(0x80000000L, 32), -2147483647, false, true, true, true)
    );
  }


  @ParameterizedTest
  @MethodSource("negTestSource")
  void constantNegation_shouldYieldCorrectValue(Constant.Value a, long result,
                                                boolean isZero, boolean carry,
                                                boolean overflow, boolean isNegative) {
    var actual = a.negate();
//    testResultAndStatus(actual, result, isZero, carry, overflow, isNegative);
  }

  static Stream<Arguments> negTestSource() {
    return Stream.of(
//        Arguments.of(intS(-2, 4), intS(3, 4), -5, false, false, false, true)
    );
  }

  @ParameterizedTest
  @MethodSource("andTestSource")
  void constantAnd_shouldYieldCorrectValue(Constant.Value a, Constant.Value b, long result,
                                           DataType type) {
    var actual = a.and(b);
//    testResultAndStatus(actual, result, isZero, carry, overflow, isNegative);
  }

  static Stream<Arguments> andTestSource() {
    return Stream.of(
//        Arguments.of(intS(-2, 4), intS(3, 4), -5, false, false, false, true)
    );
  }

  @ParameterizedTest
  @MethodSource("notTestSource")
  void constantNot_shouldYieldCorrectValue(Constant.Value a, long result, DataType type) {
    var actual = a.not();
    assertEquals(result, actual.longValue());
    assertEquals(type, actual.type());
  }

  static Stream<Arguments> notTestSource() {
    return Stream.of(
        Arguments.of(intU(0b0000, 4), 0b1111, Type.unsignedInt(4)),
        Arguments.of(intU(0b1111, 4), 0b0000, Type.unsignedInt(4)),
        Arguments.of(intU(0b1010, 4), 0b0101, Type.unsignedInt(4)),
        Arguments.of(intU(0b1, 1), 0b0, Type.unsignedInt(1)),
        Arguments.of(intU(0b0, 1), 0b1, Type.unsignedInt(1)),

        Arguments.of(intS(-0b1, 1), 0b0, Type.signedInt(1)),
        Arguments.of(intS(0b0, 1), -0b1, Type.signedInt(1)),

        Arguments.of(bool(false), 0b1, Type.bool()),
        Arguments.of(bool(true), 0b0, Type.bool())
    );
  }

  @ParameterizedTest
  @MethodSource("testValueOutOfRanges_Sources")
  void constantValueConstruction_shouldBeOutOfRange(long value, DataType type) {
    assertThrows(ViamError.class, () -> Constant.Value.of(value, type));
  }


  static Stream<Arguments> testValueOutOfRanges_Sources() {
    return Stream.of(
        Arguments.of(0b111, Type.unsignedInt(2)),
        Arguments.of(0b100, Type.signedInt(3)),
        Arguments.of(-0b1, Type.unsignedInt(3)),
        Arguments.of(-5, Type.signedInt(3)),
        Arguments.of(0b1000, Type.bits(3)),
        Arguments.of(1, Type.signedInt(1), 1)
    );
  }

  @ParameterizedTest
  @MethodSource("testValueInRanges_Sources")
  void constantValueConstruction_shouldConstructCorrectValue(long value, DataType type,
                                                             long expected) {
    var val = Constant.Value.of(value, type);
    assertEquals(expected, val.integer().longValue());
  }


  static Stream<Arguments> testValueInRanges_Sources() {
    return Stream.of(
        Arguments.of(0b111, Type.bits(3), -1),
        Arguments.of(-4, Type.bits(3), -4),
        Arguments.of(0b111, Type.unsignedInt(3), 0b111),
        Arguments.of(0b0, Type.unsignedInt(3), 0b0),
        Arguments.of(0b011, Type.signedInt(3), 0b011),
        Arguments.of(1, Type.bool(), 1),
        Arguments.of(0, Type.bool(), 0),
        Arguments.of(1, Type.unsignedInt(1), 1),
        Arguments.of(-1, Type.signedInt(1), -1)
    );
  }


  // Helper functions

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


}
