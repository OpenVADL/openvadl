// SPDX-FileCopyrightText : © 2025 TU Wien <vadl@tuwien.ac.at>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

package vadl.viam;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static vadl.utils.BigIntUtils.mask;
import static vadl.viam.helper.TestGraphUtils.bits;
import static vadl.viam.helper.TestGraphUtils.bool;
import static vadl.viam.helper.TestGraphUtils.intS;
import static vadl.viam.helper.TestGraphUtils.intU;
import static vadl.viam.helper.TestGraphUtils.status;

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
                                                Constant.Tuple.Status status) {
    var actual = a.add(b, false);
    testResultAndStatus(actual, result, status);
  }


  static Stream<Arguments> testAddSources() {
    return Stream.of(
        Arguments.of(intS(2, 4), intS(3, 4), 5, status(false, false, false, false)),
        Arguments.of(intS(-2, 4), intS(3, 4), 1, status(false, false, true, false)),
        Arguments.of(intS(7, 4), intS(1, 4), -8, status(true, false, false, true)),
        Arguments.of(intS(-7, 4), intS(-2, 4), 7, status(false, false, true, true)),

        Arguments.of(intU(1, 4), intU(4, 4), 5, status(false, false, false, false)),
        Arguments.of(intU(7, 4), intU(1, 4), 8, status(true, false, false, true)),
        Arguments.of(intU(8, 4), intU(8, 4), 0, status(false, true, true, true)),

        Arguments.of(intU(80, 8), intU(80, 8), 160, status(true, false, false, true)),
        Arguments.of(intS(80, 8), intS(80, 8), -96, status(true, false, false, true)),
        Arguments.of(intS(80, 8), intS(-48, 8), 32, status(false, false, true, false)),

        Arguments.of(intS(0b111, 4), intS(0b0001, 4), -8, status(true, false, false, true)),
        Arguments.of(intU(0b111, 4), intU(0b0001, 4), 0b1000, status(true, false, false, true)),
        Arguments.of(intU(0b1000, 4), intU(0b1111, 4), 0b111, status(false, false, true, true)),

        Arguments.of(intU(0b1111, 4), intU(0b1111, 4), 0b1110, status(true, false, true, false))
    );
  }

  @ParameterizedTest
  @MethodSource("testAddWithCarrySources")
  void constantAddition_withCarry_shouldYieldCorrectValue(Constant.Value a, Constant.Value b,
                                                          long result,
                                                          Constant.Tuple.Status status) {
    var actual = a.add(b, true);
    testResultAndStatus(actual, result, status);
  }


  static Stream<Arguments> testAddWithCarrySources() {
    return Stream.of(
        Arguments.of(intS(2, 4), intS(3, 4), 6, status(false, false, false, false)),
        Arguments.of(intS(-2, 4), intS(3, 4), 2, status(false, false, true, false)),
        Arguments.of(intS(7, 4), intS(1, 4), -7, status(true, false, false, true)),
        Arguments.of(intS(-7, 4), intS(-2, 4), -8, status(true, false, true, false)),

        Arguments.of(intU(1, 4), intU(4, 4), 6, status(false, false, false, false)),
        Arguments.of(intU(7, 4), intU(1, 4), 9, status(true, false, false, true)),
        Arguments.of(intU(8, 4), intU(8, 4), 1, status(false, false, true, true)),

        Arguments.of(intU(80, 8), intU(80, 8), 161, status(true, false, false, true)),
        Arguments.of(intS(80, 8), intS(80, 8), -95, status(true, false, false, true)),
        Arguments.of(intS(80, 8), intS(-48, 8), 33, status(false, false, true, false)),

        Arguments.of(intS(0b111, 4), intS(0b0001, 4), -7, status(true, false, false, true)),
        Arguments.of(intU(0b111, 4), intU(0b0001, 4), 0b1001, status(true, false, false, true)),
        Arguments.of(intU(0b1000, 4), intU(0b1111, 4), 0b1000, status(true, false, true, false)),

        Arguments.of(intU(0b1111, 4), intU(0b1111, 4), 0b1111, status(true, false, true, false))
    );
  }

  @ParameterizedTest
  @MethodSource("testSubX86NoCarrySource")
  void constantSubtraction_withX86ModeAndNoCarry_shouldYieldCorrectValue(Constant.Value a,
                                                                         Constant.Value b,
                                                                         long result,
                                                                         Constant.Tuple.Status status) {
    var actual = a.subtract(b, Constant.Value.SubMode.X86_LIKE, false);
    testResultAndStatus(actual, result, status);
  }

  static Stream<Arguments> testSubX86NoCarrySource() {
    return Stream.of(

        // X86 LIKE MODE
        // NO CARRY SET

        Arguments.of(intS(-2, 4), intS(3, 4), -5, status(true, false, false, false)),

        Arguments.of(intU(80, 8), intU(176, 8), 160, status(true, false, true, true)),
        Arguments.of(intS(2, 3), intS(-4, 3), -2, status(true, false, true, true)),
        Arguments.of(bits(0b010, 3), bits(0b100, 3), 0b110, status(true, false, true, true)),
        Arguments.of(intS(-2, 3), intS(-4, 3), 2, status(false, false, false, false)),

        Arguments.of(intS(0, 4), intS(1, 4), -1, status(true, false, true, false)),
        Arguments.of(intS(-8, 4), intS(0b0001, 4), 0b111, status(false, false, false, true)),
        Arguments.of(intU(0b1111, 4), intU(0b0001, 4), 0b1110, status(true, false, false, false)),

        Arguments.of(intU(0b1000, 4), intU(0b1000, 4), 0b0, status(false, true, false, false)),

        Arguments.of(intU(0b0000, 4), intU(0b1000, 4), 0b1000, status(true, false, true, true)),

        // Tested on m1 ARM64
        // Every test is done in signed and unsigned form. In both cases the flags must be the same.
        Arguments.of(intU(0xFFFFFFFFL, 32), intU(0xFFFFFFFFL, 32), 0x0,
            status(false, true, false, false)),
        Arguments.of(intS(-0x1, 32), intS(-0x1, 32), 0x0, status(false, true, false, false)),

        Arguments.of(intU(0x0, 32), intU(0x1, 32), 0xFFFFFFFFL, status(true, false, true, false)),
        Arguments.of(intS(0x0, 32), intS(0x1, 32), -0x1, status(true, false, true, false)),

        Arguments.of(intU(0x0, 32), intU(0xFFFFFFFFL, 32), 0x1L,
            status(false, false, true, false)),

        Arguments.of(intU(0x80000000L, 32), intU(0x1, 32), 0x7FFFFFFFL,
            status(false, false, false, true)),
        Arguments.of(intS(-2147483648, 32), intS(0x1, 32), 0x7FFFFFFFL,
            status(false, false, false, true)),

        Arguments.of(intU(0x1, 32), intU(0x80000000L, 32), 0x80000001L,
            status(true, false, true, true)),

        Arguments.of(intU(0x0, 32), intU(0x0, 32), 0x0L,
            status(false, true, false, false))
    );
  }

  @ParameterizedTest
  @MethodSource("testSubtraction_X86WithCarry_Source")
  void constantSubtraction_withX86ModeAndCarrySet_shouldYieldCorrectValue(Constant.Value a,
                                                                          Constant.Value b,
                                                                          long result,
                                                                          Constant.Tuple.Status status) {
    var actual = a.subtract(b, Constant.Value.SubMode.X86_LIKE, true);
    testResultAndStatus(actual, result, status);
  }

  static Stream<Arguments> testSubtraction_X86WithCarry_Source() {
    return Stream.of(

        // X86 LIKE MODE
        // WITH CARRY SET

        Arguments.of(intU(0xFFFFFFFFL, 32), intU(0xFFFFFFFFL, 32), 0xFFFFFFFFL,
            status(true, false, true, false)),
        Arguments.of(intS(-0x1, 32), intS(-0x1, 32), -0x1, status(true, false, true, false)),

        Arguments.of(intU(0x0, 32), intU(0x1, 32), 0xFFFFFFFEL, status(true, false, true, false)),
        Arguments.of(intS(0x0, 32), intS(0x1, 32), -2, status(true, false, true, false)),

        Arguments.of(intU(0x80000000L, 32), intU(0x1, 32), 0x7FFFFFFE,
            status(false, false, false, true)),
        Arguments.of(intS(-2147483648, 32), intS(0x1, 32), 0x7FFFFFFE,
            status(false, false, false, true)),

        Arguments.of(intU(0x1, 32), intU(0x80000000L, 32), 0x80000000L,
            status(true, false, true, true)),

        Arguments.of(intU(0x8, 32), intU(0x3, 32), 0x4L,
            status(false, false, false, false)),

        Arguments.of(intU(0x0, 32), intU(0x0, 32), 0xFFFFFFFFL,
            status(true, false, true, false))
    );
  }

  @ParameterizedTest
  @MethodSource("testSubtraction_ArmWithNoCarry_Source")
  void constantSubtraction_withArmModeAndNoCarrySet_shouldYieldCorrectValue(Constant.Value a,
                                                                            Constant.Value b,
                                                                            long result,
                                                                            Constant.Tuple.Status status) {
    var actual = a.subtract(b, Constant.Value.SubMode.ARM_LIKE, false);
    testResultAndStatus(actual, result, status);
  }

  static Stream<Arguments> testSubtraction_ArmWithNoCarry_Source() {
    return Stream.of(

        // ARM LIKE MODE
        // NO CARRY SET

        Arguments.of(intU(0xFFFFFFFFL, 32), intU(0xFFFFFFFFL, 32), 0xFFFFFFFFL,
            status(true, false, false, false)),
        Arguments.of(intS(-0x1, 32), intS(-0x1, 32), -0x1, status(true, false, false, false)),

        Arguments.of(intU(0x0, 32), intU(0x1, 32), 0xFFFFFFFEL, status(true, false, false, false)),
        Arguments.of(intS(0x0, 32), intS(0x1, 32), -2, status(true, false, false, false)),

        Arguments.of(intU(0x80000000L, 32), intU(0x1, 32), 0x7FFFFFFE,
            status(false, false, true, true)),
        Arguments.of(intS(-2147483648, 32), intS(0x1, 32), 0x7FFFFFFE,
            status(false, false, true, true)),

        Arguments.of(intU(0x1, 32), intU(0x80000000L, 32), 0x80000000L,
            status(true, false, false, true)),

        Arguments.of(intU(0x8, 32), intU(0x3, 32), 0x4L,
            status(false, false, true, false)),

        Arguments.of(intU(0x0, 32), intU(0x0, 32), 0xFFFFFFFFL,
            status(true, false, false, false))
    );
  }

  @ParameterizedTest
  @MethodSource("testSubtraction_ArmWithCarry_Source")
  void constantSubtraction_withArmModeAndCarrySet_shouldYieldCorrectValue(Constant.Value a,
                                                                          Constant.Value b,
                                                                          long result,
                                                                          Constant.Tuple.Status status) {
    var actual = a.subtract(b, Constant.Value.SubMode.ARM_LIKE, true);
    testResultAndStatus(actual, result, status);
  }

  static Stream<Arguments> testSubtraction_ArmWithCarry_Source() {
    return Stream.of(

        // ARM LIKE MODE
        // NO CARRY SET

        Arguments.of(intU(0xFFFFFFFFL, 32), intU(0xFFFFFFFFL, 32), 0x0,
            status(false, true, true, false)),
        Arguments.of(intS(-0x1, 32), intS(-0x1, 32), 0x0, status(false, true, true, false)),

        Arguments.of(intU(0x0, 32), intU(0x1, 32), 0xFFFFFFFFL, status(true, false, false, false)),
        Arguments.of(intS(0x0, 32), intS(0x1, 32), -1, status(true, false, false, false)),

        Arguments.of(intU(0x0, 32), intU(0xFFFFFFFFL, 32), 0x1L,
            status(false, false, false, false)),

        Arguments.of(intU(0x80000000L, 32), intU(0x1, 32), 0x7FFFFFFFL,
            status(false, false, true, true)),
        Arguments.of(intS(-2147483648, 32), intS(0x1, 32), 0x7FFFFFFFL,
            status(false, false, true, true)),

        Arguments.of(intU(0x1, 32), intU(0x80000000L, 32), 0x80000001L,
            status(true, false, false, true)),

        Arguments.of(intU(0x8, 32), intU(0x3, 32), 0x5L,
            status(false, false, true, false)),

        Arguments.of(intU(0x0, 32), intU(0x0, 32), 0x0L,
            status(false, true, true, false))
    );
  }


  @ParameterizedTest
  @MethodSource("truncateTestSource")
  void constantTruncate_shouldYieldCorrectValue(Constant.Value a, DataType type,
                                                Constant.Value expected) {
    var actual = a.truncate(type);
    assertEquals(expected, actual);
  }

  static Stream<Arguments> truncateTestSource() {
    return Stream.of(
        Arguments.of(bits(0b0000, 4), Type.bits(3), bits(0b0, 3)),
        Arguments.of(bits(0b1000, 4), Type.bits(3), bits(0b0, 3)),
        Arguments.of(bits(0b1100, 4), Type.bits(3), bits(0b100, 3)),

        Arguments.of(intS(-1, 4), Type.signedInt(3), intS(-1, 3)),
        Arguments.of(intS(0b111, 4), Type.signedInt(2), intS(-1, 2)),

        Arguments.of(intU(0b1111, 4), Type.unsignedInt(3), intU(0b111, 3))
    );
  }

  @ParameterizedTest
  @MethodSource("lslTestSource")
  void constantLsl_shouldYieldCorrectValue(Constant.Value a, Constant.Value b,
                                           Constant.Value expected) {
    var actual = a.lsl(b);
    assertEquals(expected, actual);
  }

  static Stream<Arguments> lslTestSource() {
    return Stream.of(
        Arguments.of(bits(0b0000, 4), intU(2, 5), bits(0b0, 4)),
        Arguments.of(bits(0b0001, 4), intU(2, 5), bits(0b0100, 4)),
        Arguments.of(bits(0b0101, 4), intU(2, 5), bits(0b0100, 4)),
        Arguments.of(bits(0b1111, 4), intU(0, 5), bits(0b1111, 4))
    );
  }

  @Test
  void toBeRemoved() {
    var a = BigInteger.valueOf(0xFFFFFFFFL);
    var b = BigInteger.valueOf(0xFFFFFFFFL);

    var res = a.multiply(b);
    var res32 = res.and(mask(32, 0));
    System.out.println(res.toString(16));
    System.out.println(res32.toString(16));
  }

  @ParameterizedTest
  @MethodSource("multiplyTestSource")
  void constantMultiply_shouldYieldCorrectValue(Constant.Value a, Constant.Value b,
                                                boolean longVersion,
                                                Constant.Value expected) {
    var actual = a.multiply(b, longVersion, a.type().isSigned());
    assertEquals(expected, actual);
  }

  static Stream<Arguments> multiplyTestSource() {
    return Stream.of(
        Arguments.of(bits(0xFFFFFFFFL, 32), bits(0xFFFFFFFFL, 32), false, bits(0x1, 32)),
        Arguments.of(bits(0xFFFFFFFFL, 32), bits(0x1L, 32), false, bits(0xFFFFFFFFL, 32)),
        Arguments.of(bits(0xFFFFFFFFL, 32), bits(0x0L, 32), false, bits(0x0L, 32)),
        Arguments.of(bits(0xFFFFFFFFL, 32), bits(0x2L, 32), false, bits(0xFFFFFFFEL, 32)),

        Arguments.of(intU(0xFFFFFFFFL, 32), intU(0x2L, 32), true, intU(0xFFFFFFFFL * 2L, 64)),
        Arguments.of(intS(-1, 32), intS(0x2, 32), true, intS(-2, 64)),
        Arguments.of(intU(0xFFFFL, 16), intU(0xFFFFL, 16), true, intU(0xFFFE0001L, 32)),
        Arguments.of(intS(-1, 16), intS(-1, 16), true, intS(1, 32)),
        Arguments.of(intU(0x4L, 16), intU(0x3L, 16), true, intU(12, 32))

    );
  }

  @ParameterizedTest
  @MethodSource("divideTestSource")
  void constantDivide_shouldYieldCorrectValue(Constant.Value a, Constant.Value b,
                                              Constant.Value expected) {
    var actual = a.divide(b, a.type().isSigned());
    assertEquals(expected, actual);
  }

  static Stream<Arguments> divideTestSource() {
    return Stream.of(
        Arguments.of(intS(-1, 32), intS(0x2L, 32), intS(0x0, 32)),
        Arguments.of(intU(0xFFFFFFFFL, 32), intU(0x2L, 32), intU(0x7FFFFFFFL, 32)),

        Arguments.of(intS(-1, 32), intS(Integer.MIN_VALUE, 32), intS(0x0, 32)),
        Arguments.of(intU(0xFFFFFFFFL, 32), intU(0x80000000L, 32), intU(0x1L, 32))
    );
  }


  @ParameterizedTest
  @MethodSource("truncateFailTestSource")
  void constantTruncate_shouldFail(Constant.Value a, DataType type, String errorMsg) {
    var error = assertThrows(ViamError.class, () -> a.truncate(type));
    assertThat(error.getMessage(), containsString(errorMsg));
  }

  static Stream<Arguments> truncateFailTestSource() {
    return Stream.of(
        Arguments.of(intS(1, 4), Type.signedInt(5),
            "Truncated value's bitwidth must be less or equal"),
        Arguments.of(bits(1, 4), Type.bits(5), "Truncated value's bitwidth must be less or equal"),
        Arguments.of(intU(1, 4), Type.unsignedInt(5),
            "Truncated value's bitwidth must be less or equal")
    );
  }


  @ParameterizedTest
  @MethodSource("negTestSource")
  void constantNegation_shouldYieldCorrectValue(Constant.Value a, long result, DataType type) {
    var actual = a.negate();
    assertEquals(result, actual.longValue());
    assertEquals(type, actual.type());
  }

  static Stream<Arguments> negTestSource() {
    return Stream.of(
        Arguments.of(intU(0b1000, 4), 0b1000, Type.unsignedInt(4)),
        Arguments.of(bits(0b1000, 4), 0b1000, Type.bits(4)),
        Arguments.of(bits(0b0000, 4), 0b0000, Type.bits(4)),
        Arguments.of(bits(0b0001, 4), 0b1111, Type.bits(4)),
        Arguments.of(bits(0b1111, 4), 0b0001, Type.bits(4)),

        Arguments.of(intS(-8, 4), -8, Type.signedInt(4)),
        Arguments.of(intS(-1, 4), 1, Type.signedInt(4))
    );
  }

  @ParameterizedTest
  @MethodSource("andTestSource")
  void constantAnd_shouldYieldCorrectValue(Constant.Value a, Constant.Value b, long result,
                                           DataType type) {
    var actual = a.and(b);
    assertEquals(result, actual.longValue());
    assertEquals(type, actual.type());
  }

  static Stream<Arguments> andTestSource() {
    return Stream.of(
        Arguments.of(intU(0b0000, 4), intU(0b0000, 4), 0, Type.unsignedInt(4)),
        Arguments.of(intU(0b1111, 4), intU(0b0000, 4), 0, Type.unsignedInt(4)),
        Arguments.of(intU(0b0000, 4), intU(0b1111, 4), 0, Type.unsignedInt(4)),
        Arguments.of(intU(0b1111, 4), intU(0b1111, 4), 0b1111, Type.unsignedInt(4)),
        Arguments.of(intU(0b1100, 4), intU(0b0101, 4), 0b0100, Type.unsignedInt(4)),

        Arguments.of(bool(false), bool(false), 0, Type.bool()),
        Arguments.of(bool(false), bool(true), 0, Type.bool()),
        Arguments.of(bool(true), bool(true), 1, Type.bool())
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
        Arguments.of(0b111, Type.bits(3), 0b111),
        Arguments.of(-4, Type.bits(3), 0b100),
        Arguments.of(0b111, Type.unsignedInt(3), 0b111),
        Arguments.of(0b0, Type.unsignedInt(3), 0b0),
        Arguments.of(0b011, Type.signedInt(3), 0b011),
        Arguments.of(1, Type.bool(), 1),
        Arguments.of(0, Type.bool(), 0),
        Arguments.of(1, Type.unsignedInt(1), 1),
        Arguments.of(-1, Type.signedInt(1), -1),
        Arguments.of(-4, Type.signedInt(4), -4)
    );
  }


  @ParameterizedTest
  @MethodSource("testCastConstant_Sources")
  void constantValueCast_shouldResultInCorrectValue(Constant.Value value, DataType newType,
                                                    Constant.Value expected) {
    var casted = value.castTo(newType);
    assertEquals(expected, casted);
  }


  static Stream<Arguments> testCastConstant_Sources() {
    // remember  5: 0b00101
    // remember -5: 0b11011
    return Stream.of(
        // intS -> bits
        Arguments.of(intS(5, 4), Type.bits(4), bits(0b101, 4)),
        Arguments.of(intS(5, 4), Type.bits(6), bits(0b101, 6)),
        Arguments.of(intS(5, 4), Type.bits(2), bits(0b01, 2)),
        Arguments.of(intS(-5, 4), Type.bits(4), bits(0b1011, 4)),
        Arguments.of(intS(-5, 4), Type.bits(6), bits(0b111011, 6)),
        Arguments.of(intS(-5, 4), Type.bits(2), bits(0b11, 2)),

        // intS -> uInt
        Arguments.of(intS(5, 4), Type.unsignedInt(4), intU(0b101, 4)),
        Arguments.of(intS(5, 4), Type.unsignedInt(6), intU(0b101, 6)),
        Arguments.of(intS(5, 4), Type.unsignedInt(2), intU(0b01, 2)),
        Arguments.of(intS(-5, 4), Type.unsignedInt(4), intU(0b1011, 4)),
        Arguments.of(intS(-5, 4), Type.unsignedInt(6), intU(0b111011, 6)),
        Arguments.of(intS(-5, 4), Type.unsignedInt(2), intU(0b11, 2)),

        // intS -> intS
        Arguments.of(intS(5, 4), Type.signedInt(4), intS(5, 4)),
        Arguments.of(intS(5, 4), Type.signedInt(6), intS(5, 6)),
        Arguments.of(intS(5, 4), Type.signedInt(2), intS(1, 2)),
        Arguments.of(intS(-5, 4), Type.signedInt(4), intS(-5, 4)),
        Arguments.of(intS(-5, 4), Type.signedInt(6), intS(-5, 6)),
        Arguments.of(intS(-5, 4), Type.signedInt(2), intS(-1, 2)),

        // intS -> bool
        Arguments.of(intS(5, 4), Type.bool(), bool(true)),
        Arguments.of(intS(2, 4), Type.bool(), bool(true)),
        Arguments.of(intS(0, 4), Type.bool(), bool(false)),
        Arguments.of(intS(-5, 4), Type.bool(), bool(true)),

        // bits -> intS
        Arguments.of(bits(0b0101, 4), Type.signedInt(4), intS(5, 4)),
        Arguments.of(bits(0b0101, 4), Type.signedInt(6), intS(5, 6)),
        Arguments.of(bits(0b0101, 4), Type.signedInt(2), intS(1, 2)),
        Arguments.of(bits(0b1011, 4), Type.signedInt(4), intS(-5, 4)),
        Arguments.of(bits(0b1011, 4), Type.signedInt(6), intS(-5, 6)),
        Arguments.of(bits(0b1011, 4), Type.signedInt(2), intS(-1, 2)),

        // bits -> intU
        Arguments.of(bits(0b0101, 4), Type.unsignedInt(4), intU(5, 4)),
        Arguments.of(bits(0b0101, 4), Type.unsignedInt(6), intU(5, 6)),
        Arguments.of(bits(0b0101, 4), Type.unsignedInt(2), intU(1, 2)),
        Arguments.of(bits(0b1011, 4), Type.unsignedInt(4), intU(11, 4)),
        Arguments.of(bits(0b1011, 4), Type.unsignedInt(6), intU(11, 6)),
        Arguments.of(bits(0b1011, 4), Type.unsignedInt(2), intU(3, 2)),

        // bits -> bool
        Arguments.of(bits(0b0101, 4), Type.bool(), bool(true)),
        Arguments.of(bits(0b0, 4), Type.bool(), bool(false)),
        Arguments.of(bits(0b1011, 4), Type.bool(), bool(true)),

        // intU -> intS
        Arguments.of(intU(0b0101, 4), Type.signedInt(4), intS(5, 4)),
        Arguments.of(intU(0b0101, 4), Type.signedInt(6), intS(5, 6)),
        Arguments.of(intU(0b0101, 4), Type.signedInt(2), intS(1, 2)),
        Arguments.of(intU(0b1011, 4), Type.signedInt(4), intS(-5, 4)),
        Arguments.of(intU(0b1011, 4), Type.signedInt(6), intS(11, 6)),
        Arguments.of(intU(0b1011, 4), Type.signedInt(2), intS(-1, 2)),

        // intU -> bits
        Arguments.of(intU(0b0101, 4), Type.bits(4), bits(5, 4)),
        Arguments.of(intU(0b0101, 4), Type.bits(6), bits(5, 6)),
        Arguments.of(intU(0b0101, 4), Type.bits(2), bits(1, 2)),
        Arguments.of(intU(0b1011, 4), Type.bits(4), bits(11, 4)),
        Arguments.of(intU(0b1011, 4), Type.bits(6), bits(11, 6)),
        Arguments.of(intU(0b1011, 4), Type.bits(2), bits(3, 2)),

        // intU -> bool
        Arguments.of(intU(0b0101, 4), Type.bool(), bool(true)),
        Arguments.of(intU(0b0, 4), Type.bool(), bool(false)),
        Arguments.of(intU(0b1011, 4), Type.bool(), bool(true)),

        // bool -> intS
        Arguments.of(bool(true), Type.signedInt(1), intS(-1, 1)),
        Arguments.of(bool(true), Type.signedInt(3), intS(1, 3)),
        Arguments.of(bool(false), Type.signedInt(1), intS(0, 1)),
        Arguments.of(bool(false), Type.signedInt(3), intS(0, 3)),

        // bool -> intU
        Arguments.of(bool(true), Type.unsignedInt(1), intU(1, 1)),
        Arguments.of(bool(true), Type.unsignedInt(3), intU(1, 3)),
        Arguments.of(bool(false), Type.unsignedInt(1), intU(0, 1)),
        Arguments.of(bool(false), Type.unsignedInt(3), intU(0, 3)),

        // bool -> bits
        Arguments.of(bool(true), Type.bits(1), bits(1, 1)),
        Arguments.of(bool(true), Type.bits(3), bits(1, 3)),
        Arguments.of(bool(false), Type.bits(1), bits(0, 1)),
        Arguments.of(bool(false), Type.bits(3), bits(0, 3))
    );
  }


  // Helper functions

  private void testResultAndStatus(Constant.Tuple actual, long result,
                                   Constant.Tuple.Status expectedStatus) {
    var res = actual.get(0, Constant.Value.class);

    assertEquals(result, res.integer().longValue(), "Wrong result value");
    // test status
    var status = actual.get(1, Constant.Tuple.Status.class);
    assertEquals(expectedStatus.negative().bool(),
        status.negative().integer().equals(BigInteger.ONE),
        "Wrong negative flag");
    assertEquals(expectedStatus.zero().bool(), status.zero().integer().equals(BigInteger.ONE),
        "Wrong zero flag");
    assertEquals(expectedStatus.carry().bool(), status.carry().integer().equals(BigInteger.ONE),
        "Wrong carry flag");
    assertEquals(expectedStatus.overflow().bool(),
        status.overflow().integer().equals(BigInteger.ONE),
        "Wrong overflow flag");
  }


}
