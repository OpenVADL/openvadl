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

package vadl.cppCodeGen;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static vadl.utils.GraphUtils.binaryOp;
import static vadl.utils.GraphUtils.bitsNode;
import static vadl.utils.GraphUtils.getSingleNode;
import static vadl.utils.GraphUtils.unaryOp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.MountableFile;
import vadl.DockerExecutionTest;
import vadl.cppCodeGen.common.PureFunctionCodeGenerator;
import vadl.types.BuiltInTable;
import vadl.types.Type;
import vadl.utils.GraphUtils;
import vadl.viam.Constant;
import vadl.viam.Function;
import vadl.viam.Identifier;
import vadl.viam.Parameter;
import vadl.viam.graph.Graph;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.control.StartNode;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.SliceNode;

public class BuiltinCTest extends DockerExecutionTest {

  int counter = 0;

  @BeforeEach
  public void beforeEach() {
    counter = 0;
  }

  @TestFactory
  Stream<DynamicTest> sliceTests() {
    return runTests(
        // Extract bits 3 to 0 from 0xAB (10101011) → 1011 → 0x0B
        sliceTest(0x0B, 0xAB, 3, 0),

        // Extract bits 7 to 4 from 0xAB (10101011) → 1010 → 0x0A
        sliceTest(0x0A, 0xAB, 7, 4),

        // Extract bits 7 to 0 from 0xAB (10101011) → 10101011 → 0xAB
        sliceTest(0xAB, 0xAB, 7, 0),

        // Extract bits 7 to 0 from 0xAB (10101011) in reverse order
        sliceTest(0xD5, 0xAB, 0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 7),

        // Extract bits 7, 5, 3, 1 from 0xAB (10101011) → 1111
        sliceTest(0b1111, 0b10101011, 7, 7, 5, 5, 3, 3, 1, 1),

        sliceTest(0b10, 0b01, 0, 0, 1, 1),

        // Extract bits 3 to 0 and 11 to 8 from 0xF0F0 (1111000011110000) → 0000 0000 → 0x00
        sliceTest(0x00, 0xF0F0, 3, 0, 11, 8),

        // Extract bits 11 to 8 and 3 to 0 from 0xF0F0 (1111000011110000) → 1100 1100 → 0xCC
        sliceTest(0xCC, 0xF0F0, 13, 10, 5, 2),

        // Extract bits 11 to 8 and 3 to 0 from 0xF0F0 (1111000011110000) → 0110011100 → 0x19c
        sliceTest(0x19c, 0xF0F0, 0, 0, 13, 10, 14, 14, 5, 2),


        // Extract bit 15 and bits 6 to 0 from 0xFFFF (1111111111111111) → 1 1111111 → 0xFF
        sliceTest(0xFF, 0xFFFF, 15, 15, 6, 0),

        // Extract bits 15 to 8 from 0xABCD (1010101111001101) → 10101011 → 0xAB
        sliceTest(0xAB, 0xABCD, 15, 8),

        // Extract bits 7 to 0 from 0xABCD (1010101111001101) → 11001101 → 0xCD
        sliceTest(0xCD, 0xABCD, 7, 0),

        // Extract bits 15 to 0 from 0xABCD (1010101111001101) → 1010101111001101 → 0xABCD
        sliceTest(0xABCD, 0xABCD, 15, 0),

        // Extract bits 3, 2, 1, 0 from 0x0F (00001111) → 1111 → 0x0F
        sliceTest(0x0F, 0x0F, 3, 3, 2, 2, 1, 1, 0, 0),

        // Extract bits 0, 1, 2, 3 from 0x0F (00001111) → 1111 → 0x0F
        sliceTest(0x0F, 0x0F, 0, 0, 1, 1, 2, 2, 3, 3),

        // Extract bits 7 to 0 from 0xFF (11111111) → 11111111 → 0xFF
        sliceTest(0xFF, 0xFF, 7, 0),

        // Extract bits 7 to 0 from 0xFF (11111111) in reverse order
        sliceTest(0xFF, 0xFF, 0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 7),

        // Extract bits 15 to 8 and bits 7 to 0 from 0xABCD (1010101111001101) → 1010101111001101 → 0xABCD
        sliceTest(0xABCD, 0xABCD, 15, 8, 7, 0),

        // Extract bits 7 to 0 and bits 15 to 8 from 0xABCD (1010101111001101) → 1100110110101011 → 0xCDAB
        sliceTest(0xCDAB, 0xABCD, 7, 0, 15, 8),

        // Extract bits 3 to 0 from 0x0 (00000000) → 0000 → 0x00
        sliceTest(0x00, 0x0, 3, 0),

        // Extract bits 3 to 0 from 0xF (00001111) → 1111 → 0x0F
        sliceTest(0x0F, 0xF, 3, 0),

        // Extract bits 3 to 0 from 0x10 (00010000) → 0000 → 0x00
        sliceTest(0x00, 0x10, 3, 0)
    );
  }

  private Function sliceTest(long expected, long val, int... ranges) {
    if (ranges.length % 2 != 0) {
      throw new AssertionError("slice tests must have even number of arguments");
    }
    var parts = new ArrayList<Constant.BitSlice.Part>();
    for (int i = 0; i < ranges.length; i += 2) {
      parts.add(new Constant.BitSlice.Part(ranges[i], ranges[i + 1]));
    }
    var bitSlice = new Constant.BitSlice(parts.toArray(new Constant.BitSlice.Part[0]));
    return genericFunc("slice_" + counter++, binaryOp(BuiltInTable.EQU,
        Type.bool(),
        new SliceNode(
            GraphUtils.bits(val, 64).toNode(),
            bitSlice,
            Type.bits(bitSlice.bitSize())
        ),
        bitsNode(expected, bitSlice.bitSize())));
  }

  /// / ARITHMETIC OPERATIONS ////

  @TestFactory
  Stream<DynamicTest> negTests() throws IOException {
    return runTests(
        negTest(0, 1, 0),
        negTest(1, 1, -1),
        negTest(-1, 1, 1),
        negTest(0, 7, 0),
        negTest(64, 7, -64),
        negTest(-64, 7, 64),
        negTest(0, 9, 0),
        negTest(256, 9, -256),
        negTest(-256, 9, 256),
        negTest(0, 16, 0),
        negTest(32768, 16, -32768),
        negTest(-32768, 16, 32768),
        negTest(0, 31, 0),
        negTest(1073741824, 31, -1073741824),
        negTest(-1073741824, 31, 1073741824),
        negTest(0, 63, 0),
        negTest(4611686018427387904L, 63, -4611686018427387904L),
        negTest(-4611686018427387904L, 63, 4611686018427387904L),
        negTest(0, 64, 0),
        negTest(Long.MAX_VALUE, 64, -Long.MAX_VALUE),
        negTest(Long.MIN_VALUE, 64, Long.MIN_VALUE),
        negTest(-1, 64, 1)
    );
  }

  @TestFactory
  Stream<DynamicTest> addTests() {
    return runTests(
        addTest(0, 0, 1, 0),
        addTest(0, 1, 1, 1),
        // Overflow ignored: 1 + 1 = 10 (binary), result is 0 in 1-bit
        addTest(1, 1, 1, 0),
        addTest(0, 1, 2, 1),
        addTest(1, 1, 2, 2),
        addTest(1, 2, 2, 3),
        // Overflow ignored: 2 + 2 = 100 (binary), result is 0 in 2-bit
        addTest(2, 2, 2, 0),
        // Overflow ignored: 127 + 1 = 128, result is -128 in 8-bit two's complement
        addTest(127, 1, 8, -128),
        // Overflow ignored: -128 + (-1) = -129, result is 127 in 8-bit two's complement
        addTest(-128, -1, 8, 127),
        // Overflow ignored: 32767 + 1 = 32768, result is -32768 in 16-bit two's complement
        addTest(32767, 1, 16, -32768),
        // Overflow ignored: -32768 + (-1) = -32769, result is 32767 in 16-bit two's complement
        addTest(-32768, -1, 16, 32767),
        // Overflow ignored: 2147483647 + 1 = 2147483648, result is -2147483648 in 32-bit two's complement
        addTest(2147483647, 1, 32, -2147483648),
        // Overflow ignored: -2147483648 + (-1) = -2147483649, result is 2147483647 in 32-bit two's complement
        addTest(-2147483648L, -1, 32, 2147483647),
        // Overflow ignored: Long.MAX_VALUE + 1 = Long.MIN_VALUE in 64-bit two's complement
        addTest(Long.MAX_VALUE, 1, 64, Long.MIN_VALUE),
        // Overflow ignored: Long.MIN_VALUE + (-1) = Long.MAX_VALUE in 64-bit two's complement
        addTest(Long.MIN_VALUE, -1, 64, Long.MAX_VALUE)
    );
  }

  @TestFactory
  Stream<DynamicTest> ssataddTests() {
    return runTests(
        ssataddTest(0, 0, 1, 0),
        ssataddTest(0, 1, 1, 1),
        ssataddTest(1, 1, 1, 1), // Saturation: 1 + 1 exceeds 1-bit signed max (0), result is 1
        ssataddTest(0, 1, 2, 1),
        // Saturation: 1 + 1 exceeds 2-bit signed max (1), result is 1
        ssataddTest(1, 1, 2, 1),
        // Saturation: 1 + 2 (-2) will be -1
        ssataddTest(1, 2, 2, -1),
        // Saturation: 64 (-64) + 64 (-64) exceeds minimum (-64) results in -64
        ssataddTest(64, 64, 7, -64),
        // Saturation: -64 + (-64) exceeds 7-bit signed min (-64), result is -64
        ssataddTest(-64, -64, 7, -64),
        // Saturation: 127 + 1 exceeds 8-bit signed max (127), result is 127
        ssataddTest(127, 1, 8, 127),
        // Saturation: -128 + (-1) exceeds 8-bit signed min (-128), result is -128
        ssataddTest(-128, -1, 8, -128),
        // Saturation: 32767 + 1 exceeds 16-bit signed max (32767), result is 32767
        ssataddTest(32767, 1, 16, 32767),
        // Saturation: -32768 + (-1) exceeds 16-bit signed min (-32768), result is -32768
        ssataddTest(-32768, -1, 16, -32768),
        // Saturation: 2147483647 + 1 exceeds 32-bit signed max (2147483647), result is 2147483647
        ssataddTest(2147483647, 1, 32, 2147483647),
        // Saturation: -2147483648 + (-1) exceeds 32-bit signed min (-2147483648), result is -2147483648
        ssataddTest(-2147483648L, -1, 32, -2147483648L),
        // Saturation: Long.MAX_VALUE + 1 exceeds 64-bit signed max (Long.MAX_VALUE), result is Long.MAX_VALUE
        ssataddTest(Long.MAX_VALUE, 1, 64, Long.MAX_VALUE),
        // Saturation: Long.MIN_VALUE + (-1) exceeds 64-bit signed min (Long.MIN_VALUE), result is Long.MIN_VALUE
        ssataddTest(Long.MIN_VALUE, -1, 64, Long.MIN_VALUE)
    );
  }

  @TestFactory
  Stream<DynamicTest> usataddTests() {
    return runTests(
        // 1-bit unsigned addition: 0 + 0 = 0
        usataddTest(0x0, 0x0, 1, 0x0),
        // 1-bit unsigned addition: 0 + 1 = 1
        usataddTest(0x0, 0x1, 1, 0x1),
        // 1-bit unsigned addition with saturation: 1 + 1 exceeds 1-bit max (1), result is 1
        usataddTest(0x1, 0x1, 1, 0x1),
        // 2-bit unsigned addition: 1 + 2 = 3
        usataddTest(0x1, 0x2, 2, 0x3),
        // 2-bit unsigned addition with saturation: 2 + 2 exceeds 2-bit max (3), result is 3
        usataddTest(0x2, 0x2, 2, 0x3),
        // 3-bit unsigned addition: 3 + 4 = 7
        usataddTest(0x3, 0x4, 3, 0x7),
        // 3-bit unsigned addition with saturation: 4 + 4 exceeds 3-bit max (7), result is 7
        usataddTest(0x4, 0x4, 3, 0x7),
        // 8-bit unsigned addition: 100 + 27 = 127
        usataddTest(0x64, 0x1B, 8, 0x7F),
        // 8-bit unsigned addition with saturation: 200 + 100 exceeds 8-bit max (255), result is 255
        usataddTest(0xC8, 0x64, 8, 0xFF),
        // 16-bit unsigned addition: 30000 + 5000 = 35000
        usataddTest(0x7530, 0x1388, 16, 0x88B8),
        // 16-bit unsigned addition with saturation: 60000 + 10000 exceeds 16-bit max (65535), result is 65535
        usataddTest(0xEA60, 0x2710, 16, 0xFFFF),
        // 32-bit unsigned addition: 2000000000 + 1000000000 = 3000000000
        usataddTest(0x77359400, 0x3B9ACA00, 32, 0xB2D05E00L),
        // 32-bit unsigned addition with saturation: 3000000000 + 2000000000 exceeds 32-bit max (4294967295), result is 4294967295
        usataddTest(0xB8D6D800L, 0x77359400, 32, 0xFFFFFFFFL),
        // 64-bit unsigned addition: 5000000000000000000 + 4000000000000000000 = 9000000000000000000
        usataddTest(0x4563918244F40000L, 0x3782DACE9D900000L, 64, 0x7CE66C50E2840000L),
        // 64-bit unsigned addition with saturation: 9223372036854775807 + 9223372036854775807 exceeds 64-bit max (18446744073709551615), result is 18446744073709551615
        usataddTest(0x7FFFFFFFFFFFFFFFL, 0xFFFFFFFFFFFFFFF0L, 64, 0xFFFFFFFFFFFFFFFFL)
    );
  }

  @TestFactory
  Stream<DynamicTest> subTests() {
    return runTests(
        // 1-bit unsigned subtraction: 0 - 0 = 0
        subTest(0x0, 0x0, 1, 0x0),
        subTest(0x1, 0x0, 1, 0x1),
        // 1-bit unsigned subtraction with wrap-around: 0 - 1 = 1 (mod 2)
        subTest(0x0, 0x1, 1, 0x1),
        subTest(0x1, 0x1, 1, 0x0),
        subTest(0x3, 0x1, 2, 0x2),
        // 2-bit unsigned subtraction with wrap-around: 1 - 2 = 3 (mod 4)
        subTest(0x1, 0x2, 2, 0x3),
        subTest(0x3, 0x3, 2, 0x0),
        subTest(0x64, 0x32, 8, 0x32),
        // 8-bit unsigned subtraction with wrap-around: 0 - 1 = 255
        subTest(0x0, 0x1, 8, 0xFF),
        subTest(0xFF, 0x1, 8, 0xFE),
        subTest(0x7530, 0x1388, 16, 0x61A8),
        subTest(0xFFFF, 0x1, 16, 0xFFFE),
        // 16-bit unsigned subtraction with wrap-around: 0 - 1 = 65535
        subTest(0x0, 0x1, 16, 0xFFFF),
        subTest(0x77359400, 0x3B9ACA00, 32, 0x3B9ACA00),
        subTest(0xFFFFFFFFL, 0x1, 32, 0xFFFFFFFEL),
        // 32-bit unsigned subtraction with wrap-around: 0 - 1 = 4294967295
        subTest(0x0, 0x1, 32, 0xFFFFFFFFL),
        subTest(0x4563918244F40000L, 0x3782DACE9D900000L, 64, 0xDE0B6B3A7640000L),
        subTest(0xFFFFFFFFFFFFFFFFL, 0x1, 64, 0xFFFFFFFFFFFFFFFEL),
        // 64-bit unsigned subtraction with wrap-around: 0 - 1 = 18446744073709551615
        subTest(0x0, 0x1, 64, 0xFFFFFFFFFFFFFFFFL)
    );
  }

  @TestFactory
  Stream<DynamicTest> ssatsubTests() {
    return runTests(
        // 1-bit signed saturated subtraction: 0 - 0 = 0
        ssatsubTest(0x0, 0x0, 1, 0x0),
        ssatsubTest(0x1, 0x0, 1, 0x1),
        // 1-bit signed saturated subtraction: 0 - (-1) saturates at 0
        ssatsubTest(0x0, 0x1, 1, 0x0),
        ssatsubTest(0x1, 0x1, 1, 0x0),
        // 2-bit signed saturated subtraction: 1 - (-2) = 1
        ssatsubTest(0x1, 0x2, 2, 1),
        ssatsubTest(0x3, 0x3, 2, 0x0),
        // 2-bit signed saturated subtraction: -2 - 1 saturates at -2
        ssatsubTest(-0x2, 0x1, 2, -0x2),
        ssatsubTest(0x7F, 0x32, 8, 0x4D),
        // 8-bit signed saturated subtraction: -128 - 1 saturates at -128
        ssatsubTest(-0x80, 0x1, 8, -0x80),
        // 8-bit signed saturated subtraction: 127 - (-1) saturates at 127
        ssatsubTest(0x7F, -0x1, 8, 0x7F),
        ssatsubTest(0x7530, 0x1388, 16, 0x61A8),
        // 16-bit signed saturated subtraction: -32768 - 1 saturates at -32768
        ssatsubTest(-0x8000, 0x1, 16, -0x8000),
        // 16-bit signed saturated subtraction: 32767 - (-1) saturates at 32767
        ssatsubTest(0x7FFF, -0x1, 16, 0x7FFF),
        ssatsubTest(0x7FFFFFFF, 0x1, 32, 0x7FFFFFFE),
        // 32-bit signed saturated subtraction: -2147483648 - 1 saturates at -2147483648
        ssatsubTest(-0x80000000, 0x1, 32, -0x80000000),
        // 32-bit signed saturated subtraction: 2147483647 - (-1) saturates at 2147483647
        ssatsubTest(0x7FFFFFFF, -0x1, 32, 0x7FFFFFFF),
        ssatsubTest(0x7FFFFFFFFFFFFFFFL, 0x1, 64, 0x7FFFFFFFFFFFFFFEL),
        // 64-bit signed saturated subtraction: -9223372036854775808 - 1 saturates at -9223372036854775808
        ssatsubTest(-0x8000000000000000L, 0x1, 64, -0x8000000000000000L),
        // 64-bit signed saturated subtraction: 9223372036854775807 - (-1) saturates at 9223372036854775807
        ssatsubTest(0x7FFFFFFFFFFFFFFFL, -0x1, 64, 0x7FFFFFFFFFFFFFFFL)
    );
  }

  @TestFactory
  Stream<DynamicTest> usatsubTests() {
    return runTests(
        // 1-bit unsigned saturated subtraction: 0 - 0 = 0
        usatsubTest(0x0, 0x0, 1, 0x0),
        usatsubTest(0x1, 0x0, 1, 0x1),
        // 1-bit unsigned saturated subtraction: 0 - 1 saturates at 0
        usatsubTest(0x0, 0x1, 1, 0x0),
        usatsubTest(0x1, 0x1, 1, 0x0),
        // 2-bit unsigned saturated subtraction: 1 - 2 saturates at 0
        usatsubTest(0x1, 0x2, 2, 0x0),
        usatsubTest(0x3, 0x3, 2, 0x0),
        // 2-bit unsigned saturated subtraction: 3 - 1 = 2
        usatsubTest(0x3, 0x1, 2, 0x2),
        usatsubTest(0x64, 0x32, 8, 0x32),
        // 8-bit unsigned saturated subtraction: 0 - 1 saturates at 0
        usatsubTest(0x0, 0x1, 8, 0x0),
        usatsubTest(0xFF, 0x1, 8, 0xFE),
        usatsubTest(0x7530, 0x1388, 16, 0x61A8),
        // 16-bit unsigned saturated subtraction: 0 - 1 saturates at 0
        usatsubTest(0x0, 0x1, 16, 0x0),
        usatsubTest(0xFFFF, 0x1, 16, 0xFFFE),
        usatsubTest(0x77359400, 0x3B9ACA00, 32, 0x3B9ACA00),
        // 32-bit unsigned saturated subtraction: 0 - 1 saturates at 0
        usatsubTest(0x0, 0x1, 32, 0x0),
        usatsubTest(0xFFFFFFFF, 0x1, 32, 0xFFFFFFFE),
        usatsubTest(0x4563918244F40000L, 0x3782DACE9D900000L, 64, 0xDE0B6B3A7640000L),
        // 64-bit unsigned saturated subtraction: 0 - 1 saturates at 0
        usatsubTest(0x0, 0x1, 64, 0x0),
        usatsubTest(0xFFFFFFFFFFFFFFFFL, 0x1, 64, 0xFFFFFFFFFFFFFFFEL)
    );
  }

  @TestFactory
  Stream<DynamicTest> mulTests() {
    return runTests(
        // 1-bit multiplication: 0 * 0 = 0
        mul(0x0, 0x0, 1, 0x0),
        mul(0x1, 0x0, 1, 0x0),
        mul(0x1, 0x1, 1, 0x1),
        // 1-bit multiplication with overflow: 1 * 1 = 1 (mod 2)
        mul(0x1, 0x1, 1, 0x1),
        mul(0x1, 0x2, 2, 0x2),
        // 2-bit multiplication with overflow: 3 * 3 = 9 (mod 4)
        mul(0x3, 0x3, 2, 0x1),
        mul(0x64, 0x32, 8, 0x88),
        // 8-bit multiplication with overflow: 255 * 2 = 510 (mod 256)
        mul(0xFF, 0x2, 8, 0xFE),
        mul(0x7530, 0x2, 16, 0xEA60),
        // 16-bit multiplication with overflow: 32768 * 2 = 65536 (mod 65536)
        mul(0x8000, 0x2, 16, 0x0),
        mul(0xFFFF, 0xFFFF, 16, 0x1),
        // 32-bit multiplication: 2000 * 3000 = 6000000
        mul(0x7D0, 0xBB8, 32, 0x5B8D80),
        // 32-bit multiplication with overflow: 4294967295 * 2 = 8589934590 (mod 4294967296)
        mul(0xFFFFFFFFL, 0x2, 32, 0xFFFFFFFEL),
        // 64-bit multiplication: 5000000000 * 4000000000 = 20000000000000000000
        mul(0x12A05F200L, 0xEE6B2800L, 64, 0x158e460913d00000L),
        // 64-bit multiplication with overflow: 9223372036854775807 * 2 = 18446744073709551614 (mod 2^64)
        mul(0x7FFFFFFFFFFFFFFFL, 0x2, 64, 0xFFFFFFFFFFFFFFFEL)
    );
  }

  // TODO: Test values exceeding 64 bit
  @TestFactory
  Stream<DynamicTest> smulTests() {
    return runTests(
        // 1-bit signed multiplication: 0 * 0 = 0
        smull(0x0, 0x0, 1, 0x0),
        smull(0x1, 0x0, 1, 0x0),
        smull(0x1, 0x1, 1, 0x1),
        // 1-bit signed multiplication: -1 * -1 = 1 (sign preserved)
        smull(-0x1, -0x1, 1, 0x1),
        // 2-bit signed multiplication: 2 * 2 = 4
        smull(0x2, 0x2, 2, 0x4),
        // 2-bit signed multiplication: -2 * 1 = -2
        smull(-0x2, 0x1, 2, 0xe),
        // 8-bit signed multiplication: 127 * -128 = -16256
        smull(0x7F, -0x80, 8, -0x3F80),
        // 8-bit signed multiplication: -128 * -128 = 16384
        smull(-0x80, -0x80, 8, 0x4000),
        // 16-bit signed multiplication: 32767 * -32768 = -1073709056
        smull(0x7FFF, -32768, 16, -0x3fff8000),
        // 16-bit signed multiplication: -32768 * -32768 = 1073741824
        smull(-0x8000, -0x8000, 16, 0x40000000),
        // 32-bit signed multiplication: 2147483647 * -2147483648 = -4611686018427387904
        smull(0x7FFFFFFF, -0x80000000, 32, -4611686016279904256L),
        // 32-bit signed multiplication: -2147483648 * -2147483648 = 4611686018427387904
        smull(-0x80000000, -0x80000000, 32, 0x4000000000000000L),
        // 64-bit signed multiplication: Maximum positive input within 64-bit
        smull(0x7FFFFFFFFFFFFFFFL, -0x1, 64, -0x7FFFFFFFFFFFFFFFL)
    );
  }

  @TestFactory
  Stream<DynamicTest> umulTests() {
    return runTests(
        // 1-bit unsigned multiplication: 0 * 0 = 0
        umull(0x0, 0x0, 1, 0x0),
        umull(0x1, 0x0, 1, 0x0),
        umull(0x1, 0x1, 1, 0x1),
        // 2-bit unsigned multiplication: 2 * 3 = 6
        umull(0x2, 0x3, 2, 0x6),
        umull(0x2, 0x1, 2, 0x2),
        // 8-bit unsigned multiplication: 127 * 128 = 16256
        umull(0x7F, 0x80, 8, 0x3F80),
        // 8-bit unsigned multiplication: 255 * 255 = 65025
        umull(0xFF, 0xFF, 8, 0xFE01),
        // 16-bit unsigned multiplication: 32767 * 32768 = 1073709056
        umull(0x7FFF, 0x8000, 16, 0x3FFF8000),
        // 16-bit unsigned multiplication: 65535 * 65535 = 4294836225
        umull(0xFFFF, 0xFFFF, 16, 0xFFFE0001L),
        // 32-bit unsigned multiplication: 2147483647 * 2147483648 = 4611686016279904256
        umull(0x7FFFFFFF, 0x80000000L, 32, 0x3FFFFFFF80000000L),
        // 32-bit unsigned multiplication: 4294967295 * 4294967295 = 18446744065119617025
        umull(0xFFFFFFFFL, 0xFFFFFFFFL, 32, 0xFFFFFFFE00000001L)
    );
  }

  @TestFactory
  Stream<DynamicTest> sumulTests() {
    return runTests(
        // 1-bit signed * unsigned multiplication: -1 * 1 = 1
        sumull(0x1, 0x1, 1, -0x1),
        // 1-bit signed * unsigned multiplication: -1 * 1 = -1
        sumull(-0x1, 0x1, 1, -0x1),
        // 2-bit signed * unsigned multiplication: -2 * 3 = -6
        sumull(-0x2, 0x3, 2, -0x6),
        // 2-bit signed * unsigned multiplication: 1 * 3 = 3
        sumull(0x1, 0x3, 2, 0x3),
        // 8-bit signed * unsigned multiplication: 127 * 255 = 32385
        sumull(0x7F, 0xFF, 8, 0x7E81),
        sumull(-0x80, 0xFF, 8, -0x7F80),
        sumull(0x7FFF, 0xFFFF, 16, 0x7FFE8001),
        sumull(-0x8000, 0xFFFF, 16, -0x7FFF8000),
        sumull(0x7FFFFFFF, 0xFFFFFFFFL, 32, 0x7FFFFFFE80000001L),
        sumull(-0x80000000, 0xFFFFFFFFL, 32, -0x7FFFFFFF80000000L)
    );
  }

  @TestFactory
  Stream<DynamicTest> smodTests() {
    return runTests(
        // 1-bit signed modulus: 1 % 1 = 0
        smod(0x1, 0x1, 1, 0x0),
        // 2-bit signed modulus: 2 % 3 = 2
        smod(0x2, 0x3, 2, 0x0),
        // 2-bit signed modulus: -2 % 3 = -2
        smod(-0x2, 0x3, 2, 0x0),
        // 8-bit signed modulus: 127 % -128 = -1
        smod(0x7F, 0x80, 8, 0x7F),
        // 8-bit signed modulus: -128 % 127 = 126
        smod(0x80, 0x7F, 8, 0xFF),
        // 16-bit signed modulus: 32767 % 32768 = 32767
        smod(0x7FFF, 0x8000, 16, 0x7FFF),
        // 16-bit signed modulus: -32768 % 32767 = -1
        smod(-0x8000, 0x7FFF, 16, -0x1),
        // 32-bit signed modulus: 2147483647 % 2147483648 = 2147483647
        smod(0x7FFFFFFF, 0x80000000L, 32, 0x7FFFFFFF),
        // 32-bit signed modulus: -2147483648 % 2147483647 = -1
        smod(-0x80000000, 0x7FFFFFFF, 32, -0x1),
        // 64-bit signed modulus: 9223372036854775807 % -9223372036854775808 = 9223372036854775807
        smod(0x7FFFFFFFFFFFFFFFL, -0x8000000000000000L, 64, 0x7FFFFFFFFFFFFFFFL),
        // 64-bit signed modulus: -9223372036854775808 % 9223372036854775807 = -1
        smod(-0x8000000000000000L, 0x7FFFFFFFFFFFFFFFL, 64, -0x1)
    );
  }

  @TestFactory
  Stream<DynamicTest> umodTests() {
    return runTests(
        // 1-bit unsigned modulus: 1 % 1 = 0
        umod(0x1, 0x1, 1, 0x0),
        // 2-bit unsigned modulus: 2 % 3 = 2
        umod(0x2, 0x3, 2, 0x2),
        // 2-bit unsigned modulus: 3 % 2 = 1
        umod(0x3, 0x2, 2, 0x1),
        // 8-bit unsigned modulus: 255 % 128 = 127
        umod(0xFF, 0x80, 8, 0x7F),
        // 8-bit unsigned modulus: 200 % 127 = 73
        umod(0xC8, 0x7F, 8, 0x49),
        // 8-bit signed modulus: 127 % 128 = 128
        umod(0x7F, 0x80, 8, 0x7F),
        // 8-bit signed modulus: 128 % 127 = 1
        umod(0x80, 0x7F, 8, 1),
        // 16-bit unsigned modulus: 65535 % 32768 = 32767
        umod(0xFFFF, 0x8000, 16, 0x7FFF),
        // 16-bit unsigned modulus: 50000 % 32767 = 17233
        umod(0xC350, 0x7FFF, 16, 0x4351),
        // 32-bit unsigned modulus: 4294967295 % 2147483648 = 2147483647
        umod(0xFFFFFFFF, 0x80000000, 32, 0x7FFFFFFF),
        // 32-bit unsigned modulus: 3000000000 % 2147483647 = 852516353
        umod(0xB2D05E00, 0x7FFFFFFF, 32, 0x32D05E01),
        // 64-bit unsigned modulus: 18446744073709551615 % 9223372036854775808 = 9223372036854775807
        umod(0xFFFFFFFFFFFFFFFFL, 0x8000000000000000L, 64, 0x7FFFFFFFFFFFFFFFL),
        // 64-bit unsigned modulus: 10000000000000000000 % 9223372036854775807 = 776627963145224192
        umod(0x8AC7230489E80000L, 0x7FFFFFFFFFFFFFFFL, 64, 0xAC7230489E80001L)
    );
  }

  @TestFactory
  Stream<DynamicTest> sdivTests() {
    return runTests(
        // 1-bit signed division: 1 / 1 = 1
        sdiv(0x1, 0x1, 1, 0x1),
        // 2-bit signed division: 2 / 1 = 2
        sdiv(0x2, 0x1, 2, 0x2),
        // 2-bit signed division: -2 / 1 = -2
        sdiv(-0x2, 0x1, 2, -0x2),
        // 2-bit signed division: -2 / -1 = 2
        sdiv(-0x2, -0x1, 2, 0x2),
        // 8-bit signed division: 127 / -1 = -127
        sdiv(0x7F, -0x1, 8, -0x7F),
        // 8-bit signed division: -128 / 1 = -128
        sdiv(-0x80, 0x1, 8, -0x80),
        // 8-bit signed division: -128 / -1 = 128 (overflow handling)
        sdiv(-0x80, -0x1, 8, 0x80),
        // 16-bit signed division: 32767 / 1 = 32767
        sdiv(0x7FFF, 0x1, 16, 0x7FFF),
        // 16-bit signed division: -32768 / -1 = 32768 (overflow handling)
        sdiv(-0x8000, -0x1, 16, 0x8000),
        // 32-bit signed division: 2147483647 / 1 = 2147483647
        sdiv(0x7FFFFFFF, 0x1, 32, 0x7FFFFFFF),
        // 32-bit signed division: -2147483648 / -1 = -2147483648 (overflow handling)
        sdiv(-0x80000000, -0x1, 32, -0x80000000),
        // 64-bit signed division: 9223372036854775807 / -1 = -9223372036854775807
        sdiv(0x7FFFFFFFFFFFFFFFL, -0x1, 64, -0x7FFFFFFFFFFFFFFFL),
        // 64-bit signed division: -9223372036854775808 / -1 = -9223372036854775808 (overflow handling)
        sdiv(-0x8000000000000000L, -0x1, 64, -0x8000000000000000L)
    );
  }

  @TestFactory
  Stream<DynamicTest> udivTests() {
    return runTests(
        // 1-bit unsigned division: 1 / 1 = 1
        udiv(0x1, 0x1, 1, 0x1),
        // 2-bit unsigned division: 2 / 1 = 2
        udiv(0x2, 0x1, 2, 0x2),
        // 2-bit unsigned division: 3 / 2 = 1
        udiv(0x3, 0x2, 2, 0x1),
        // 8-bit unsigned division: 255 / 128 = 1
        udiv(0xFF, 0x80, 8, 0x1),
        // 8-bit unsigned division: 200 / 127 = 1
        udiv(0xC8, 0x7F, 8, 0x1),
        // 8-bit unsigned division: 127 / 1 = 127
        udiv(0x7F, 0x1, 8, 0x7F),
        // 16-bit unsigned division: 65535 / 32768 = 1
        udiv(0xFFFF, 0x8000, 16, 0x1),
        // 16-bit unsigned division: 50000 / 32767 = 1
        udiv(0xC350, 0x7FFF, 16, 0x1),
        // 16-bit unsigned division: 32767 / 1 = 32767
        udiv(0x7FFF, 0x1, 16, 0x7FFF),
        // 32-bit unsigned division: 4294967295 / 2147483648 = 1
        udiv(0xFFFFFFFFL, 0x80000000L, 32, 0x1),
        // 32-bit unsigned division: 3000000000 / 2147483647 = 1
        udiv(0xB2D05E00L, 0x7FFFFFFF, 32, 0x1),
        // 32-bit unsigned division: 2147483647 / 1 = 2147483647
        udiv(0x7FFFFFFF, 0x1, 32, 0x7FFFFFFF),
        // 64-bit unsigned division: 18446744073709551615 / 9223372036854775808 = 1
        udiv(0xFFFFFFFFFFFFFFFFL, 0x8000000000000000L, 64, 0x1),
        // 64-bit unsigned division: 10000000000000000000 / 9223372036854775807 = 1
        udiv(0x8AC7230489E80000L, 0x7FFFFFFFFFFFFFFFL, 64, 0x1),
        // 64-bit unsigned division: 9223372036854775807 / 1 = 9223372036854775807
        udiv(0x7FFFFFFFFFFFFFFFL, 0x1, 64, 0x7FFFFFFFFFFFFFFFL)
    );
  }

  private Function negTest(long val, int size, long expected) {
    return unaryTest(BuiltInTable.NEG, val, size, expected);
  }

  private Function addTest(long a, long b, int size, long expected) {
    return binaryTest(BuiltInTable.ADD, a, b, size, expected);
  }

  private Function ssataddTest(long a, long b, int size, long expected) {
    return binaryTest(BuiltInTable.SSATADD, a, b, size, expected);
  }

  private Function usataddTest(long a, long b, int size, long expected) {
    return binaryTest(BuiltInTable.USATADD, a, b, size, expected);
  }

  private Function subTest(long a, long b, int size, long expected) {
    return binaryTest(BuiltInTable.SUB, a, b, size, expected);
  }

  private Function ssatsubTest(long a, long b, int size, long expected) {
    return binaryTest(BuiltInTable.SSATSUB, a, b, size, expected);
  }

  private Function usatsubTest(long a, long b, int size, long expected) {
    return binaryTest(BuiltInTable.USATSUB, a, b, size, expected);
  }

  private Function mul(long a, long b, int size, long expected) {
    return binaryTest(BuiltInTable.MUL, a, b, size, expected);
  }

  private Function smull(long a, long b, int size, long expected) {
    return binaryTest(BuiltInTable.SMULL, a, b, size, expected, Math.min(size * 2, 64));
  }

  private Function umull(long a, long b, int size, long expected) {
    return binaryTest(BuiltInTable.UMULL, a, b, size, expected, Math.min(size * 2, 64));
  }

  private Function sumull(long a, long b, int size, long expected) {
    return binaryTest(BuiltInTable.SUMULL, a, b, size, expected, Math.min(size * 2, 64));
  }

  private Function smod(long a, long b, int size, long expected) {
    return binaryTest(BuiltInTable.SMOD, a, b, size, expected);
  }

  private Function umod(long a, long b, int size, long expected) {
    return binaryTest(BuiltInTable.UMOD, a, b, size, expected);
  }

  private Function sdiv(long a, long b, int size, long expected) {
    return binaryTest(BuiltInTable.SDIV, a, b, size, expected);
  }

  private Function udiv(long a, long b, int size, long expected) {
    return binaryTest(BuiltInTable.UDIV, a, b, size, expected);
  }


  /// LOGICAL OPERATIONS ///

  @TestFactory
  Stream<DynamicTest> notTests() {
    return runTests(
        // 1-bit NOT: ~1 = 0
        not(0x1, 1, 0x0),
        // 2-bit NOT: ~2 = 1
        not(0x2, 2, 0x1),
        // 2-bit NOT: ~3 = 0
        not(0x3, 2, 0x0),
        // 8-bit NOT: ~255 = 0
        not(0xFF, 8, 0x00),
        // 8-bit NOT: ~0 = 255
        not(0x00, 8, 0xFF),
        // 16-bit NOT: ~32767 = 32768
        not(0x7FFF, 16, 0x8000),
        // 16-bit NOT: ~0 = 65535
        not(0x0000, 16, 0xFFFF),
        // 32-bit NOT: ~2147483647 = 2147483648
        not(0x7FFFFFFF, 32, 0x80000000L),
        // 32-bit NOT: ~0 = 4294967295
        not(0x00000000, 32, 0xFFFFFFFFL),
        // 64-bit NOT: ~9223372036854775807 = 9223372036854775808
        not(0x7FFFFFFFFFFFFFFFL, 64, 0x8000000000000000L),
        // 64-bit NOT: ~0 = 18446744073709551615
        not(0x0000000000000000L, 64, 0xFFFFFFFFFFFFFFFFL)
    );
  }

  private Function not(long a, int size, long expected) {
    return unaryTest(BuiltInTable.NOT, a, size, expected);
  }

  @TestFactory
  Stream<DynamicTest> andTests() {
    return runTests(
        // 1-bit AND: 1 & 1 = 1
        and(0x1, 0x1, 1, 0x1),
        and(0x1, 0x0, 1, 0x0),
        // 2-bit AND: 2 & 3 = 2
        and(0x2, 0x3, 2, 0x2),
        // 2-bit AND: 2 & 0 = 0
        and(0x2, 0x0, 2, 0x0),
        // 8-bit AND: 255 & 128 = 128
        and(0xFF, 0x80, 8, 0x80),
        and(0xFF, 0x0, 8, 0x0),
        // 8-bit AND: 200 & 127 = 72
        and(0xC8, 0x7F, 8, 0x48),
        // 16-bit AND: 65535 & 32768 = 32768
        and(0xFFFF, 0x8000, 16, 0x8000),
        // 16-bit AND: 50000 & 32767 = 17232
        and(0xC350, 0x7FFF, 16, 0x4350),
        // 32-bit AND: 4294967295 & 2147483648 = 2147483648
        and(0xFFFFFFFFL, 0x80000000L, 32, 0x80000000L),
        // 32-bit AND: 3000000000 & 2147483647 = 852516352
        and(0xB2D05E00L, 0x7FFFFFFF, 32, 0x32d05e00),
        // 64-bit AND: 18446744073709551615 & 9223372036854775808 = 9223372036854775808
        and(0xFFFFFFFFFFFFFFFFL, 0x8000000000000000L, 64, 0x8000000000000000L),
        // 64-bit AND: 10000000000000000000 & 9223372036854775807 = 776627963145224192
        and(0x8AC7230489E80000L, 0x7FFFFFFFFFFFFFFFL, 64, 0x0AC7230489E80000L)
    );
  }

  private Function and(long a, long b, int size, long expected) {
    return binaryTest(BuiltInTable.AND, a, b, size, expected);
  }

  @TestFactory
  Stream<DynamicTest> xorTests() {
    return runTests(
        // 1-bit XOR: 1 ^ 1 = 0
        xor(0x1, 0x1, 1, 0x0),
        // 2-bit XOR: 2 ^ 3 = 1
        xor(0x2, 0x3, 2, 0x1),
        // 2-bit XOR: 2 ^ 0 = 2
        xor(0x2, 0x0, 2, 0x2),
        // 8-bit XOR: 255 ^ 128 = 127
        xor(0xFF, 0x80, 8, 0x7F),
        // 8-bit XOR: 200 ^ 127 = 183
        xor(0xC8, 0x7F, 8, 0xB7),
        // 16-bit XOR: 65535 ^ 32768 = 32767
        xor(0xFFFF, 0x8000, 16, 0x7FFF),
        // 16-bit XOR: 50000 ^ 32767 = 17233
        xor(0xC350, 0x7FFF, 16, 0xbcaf),
        // 32-bit XOR: 4294967295 ^ 2147483648 = 2147483647
        xor(0xFFFFFFFFL, 0x80000000L, 32, 0x7FFFFFFF),
        // 32-bit XOR: 3000000000 ^ 2147483647 = 852516353
        xor(0xB2D05E00L, 0x7FFFFFFF, 32, 0xcd2fa1ffL),
        // 64-bit XOR: 18446744073709551615 ^ 9223372036854775808 = 9223372036854775807
        xor(0xFFFFFFFFFFFFFFFFL, 0x8000000000000000L, 64, 0x7FFFFFFFFFFFFFFFL),
        // 64-bit XOR: 10000000000000000000 ^ 9223372036854775807 = 776627963145224192
        xor(0x8AC7230489E80000L, 0x7FFFFFFFFFFFFFFFL, 64, 0xf538dcfb7617ffffL)
    );
  }


  private Function xor(long a, long b, int size, long expected) {
    return binaryTest(BuiltInTable.XOR, a, b, size, expected);
  }


  @TestFactory
  Stream<DynamicTest> orTests() {
    return runTests(
        // 1-bit OR: 1 | 1 = 1
        or(0x1, 0x1, 1, 0x1),
        // 2-bit OR: 2 | 3 = 3
        or(0x2, 0x3, 2, 0x3),
        // 2-bit OR: 2 | 0 = 2
        or(0x2, 0x0, 2, 0x2),
        // 8-bit OR: 255 | 128 = 255
        or(0xFF, 0x80, 8, 0xFF),
        // 8-bit OR: 200 | 127 = 255
        or(0xC8, 0x7F, 8, 0xFF),
        // 16-bit OR: 50000 | 32767 = 50047
        or(0xC8, 0x0, 11, 0xC8),
        // 16-bit OR: 65535 | 32768 = 65535
        or(0xFFFF, 0x8000, 16, 0xFFFF),
        // 16-bit OR: 50000 | 32767 = 50047
        or(0xC350, 0x7FFF, 16, 0xffff),
        // 32-bit OR: 4294967295 | 2147483648 = 4294967295
        or(0xFFFFFFFFL, 0x80000000L, 32, 0xFFFFFFFFL),
        // 32-bit OR: 3000000000 | 2147483647 = 4147483647
        or(0xB2D05E00L, 0x7FFFFFFF, 32, 0xFFFFFFFFL),
        // 64-bit OR: 18446744073709551615 | 9223372036854775808 = 18446744073709551615
        or(0xFFFFFFFFFFFFFFFFL, 0x8000000000000000L, 64, 0xFFFFFFFFFFFFFFFFL),
        // 64-bit OR: 10000000000000000000 | 9223372036854775807 = 18446744073709551615
        or(0x8AC7230489E80000L, 0x7FFFFFFFFFFFFFFFL, 64, 0xFFFFFFFFFFFFFFFFL)
    );
  }

  private Function or(long a, long b, int size, long expected) {
    return binaryTest(BuiltInTable.OR, a, b, size, expected);
  }

  /// COMPARISON OPERATIONS ///

  @TestFactory
  Stream<DynamicTest> equTests() {
    return runTests(
        // 1-bit EQUAL: 1 == 1 -> true
        equ(0x1, 0x1, 1, true),
        // 1-bit EQUAL: 1 == 0 -> false
        equ(0x1, 0x0, 1, false),
        // 2-bit EQUAL: 2 == 3 -> false
        equ(0x2, 0x3, 2, false),
        // 2-bit EQUAL: 2 == 2 -> true
        equ(0x2, 0x2, 2, true),
        // 8-bit EQUAL: 255 == 128 -> false
        equ(0xFF, 0x80, 8, false),
        // 8-bit EQUAL: 200 == 200 -> true
        equ(0xC8, 0xC8, 8, true),
        // 16-bit EQUAL: 65535 == 32768 -> false
        equ(0xFFFF, 0x8000, 16, false),
        // 16-bit EQUAL: 50000 == 50000 -> true
        equ(0xC350, 0xC350, 16, true),
        // 32-bit EQUAL: 4294967295 == 2147483648 -> false
        equ(0xFFFFFFFFL, 0xFFFFFFFL, 32, false),
        // 32-bit EQUAL: 3000000000 == 3000000000 -> true
        equ(0xB2D05E00L, 0xB2D05E00L, 32, true),
        // 64-bit EQUAL: 18446744073709551615 == 9223372036854775808 -> false
        equ(0xFFFFFFFFFFFFFFFFL, 0x8000000000000000L, 64, false),
        // 64-bit EQUAL: 10000000000000000000 == 10000000000000000000 -> true
        equ(0x8AC7230489E80000L, 0x8AC7230489E80000L, 64, true)
    );
  }

  private Function equ(long a, long b, int size, boolean expected) {
    return binaryTest(BuiltInTable.EQU, a, b, size, expected ? 0x1 : 0x0);
  }

  @TestFactory
  Stream<DynamicTest> neqTests() {
    return runTests(
        // 1-bit NOT EQUAL: 1 != 1 -> false
        neq(0x1, 0x1, 1, false),
        // 1-bit NOT EQUAL: 1 != 0 -> true
        neq(0x1, 0x0, 1, true),
        // 2-bit NOT EQUAL: 2 != 3 -> true
        neq(0x2, 0x3, 2, true),
        // 2-bit NOT EQUAL: 2 != 2 -> false
        neq(0x2, 0x2, 2, false),
        // 8-bit NOT EQUAL: 255 != 128 -> true
        neq(0xFF, 0x80, 8, true),
        // 8-bit NOT EQUAL: 200 != 200 -> false
        neq(0xC8, 0xC8, 8, false),
        // 16-bit NOT EQUAL: 65535 != 32768 -> true
        neq(0xFFFF, 0x8000, 16, true),
        // 16-bit NOT EQUAL: 50000 != 50000 -> false
        neq(0xC350, 0xC350, 16, false),
        // 32-bit NOT EQUAL: 4294967295 != 2147483648 -> true
        neq(0xFFFFFFFFL, 0x80000000L, 32, true),
        // 32-bit NOT EQUAL: 3000000000 != 3000000000 -> false
        neq(0xB2D05E00L, 0xB2D05E00L, 32, false),
        // 64-bit NOT EQUAL: 18446744073709551615 != 9223372036854775808 -> true
        neq(0xFFFFFFFFFFFFFFFFL, 0x8000000000000000L, 64, true),
        // 64-bit NOT EQUAL: 10000000000000000000 != 10000000000000000000 -> false
        neq(0x8AC7230489E80000L, 0x8AC7230489E80000L, 64, false)
    );
  }

  private Function neq(long a, long b, int size, boolean expected) {
    return binaryTest(BuiltInTable.NEQ, a, b, size, expected ? 0x1 : 0x0);
  }

  @TestFactory
  Stream<DynamicTest> slthTests() {
    return runTests(
        // 1-bit signed less than: 0 < -1 -> false
        slth(0x0, 0x1, 1, false),
        // 1-bit signed less than: -1 < 0 -> false
        slth(0x1, 0x0, 1, true),
        // 2-bit signed less than: -2 < -1 -> true
        slth(-0x2, -0x1, 2, true),
        // 2-bit signed less than: -1 < -2 -> false
        slth(-0x1, -0x2, 2, false),
        // 8-bit signed less than: -128 < 127 -> true
        slth(-0x80, 0x7F, 8, true),
        // 8-bit signed less than: 127 < -128 -> false
        slth(0x7F, -0x80, 8, false),
        // 16-bit signed less than: -32768 < 32767 -> true
        slth(-0x8000, 0x7FFF, 16, true),
        // 16-bit signed less than: 32767 < -32768 -> false
        slth(0x7FFF, -0x8000, 16, false),
        // 32-bit signed less than: -2147483648 < 2147483647 -> true
        slth(-0x80000000, 0x7FFFFFFF, 32, true),
        // 32-bit signed less than: 2147483647 < -2147483648 -> false
        slth(0x7FFFFFFF, -0x80000000, 32, false),
        // 64-bit signed less than: -9223372036854775808 < 9223372036854775807 -> true
        slth(-0x8000000000000000L, 0x7FFFFFFFFFFFFFFFL, 64, true),
        // 64-bit signed less than: 9223372036854775807 < -9223372036854775808 -> false
        slth(0x7FFFFFFFFFFFFFFFL, -0x8000000000000000L, 64, false)
    );
  }

  private Function slth(long a, long b, int size, boolean expected) {
    return binaryTest(BuiltInTable.SLTH, a, b, size, expected ? 0x1 : 0x0);
  }

  @TestFactory
  Stream<DynamicTest> ulthTests() {
    return runTests(
        // 1-bit unsigned less than: 0 < 1 -> true
        ulth(0x0, 0x1, 1, true),
        // 1-bit unsigned less than: 1 < 0 -> false
        ulth(0x1, 0x0, 1, false),
        // 2-bit unsigned less than: 2 < 3 -> true
        ulth(0x2, 0x3, 2, true),
        // 2-bit unsigned less than: 3 < 2 -> false
        ulth(0x3, 0x2, 2, false),
        // 8-bit unsigned less than: 128 < 255 -> true
        ulth(0x80, 0xFF, 8, true),
        // 8-bit unsigned less than: 255 < 128 -> false
        ulth(0xFF, 0x80, 8, false),
        // 16-bit unsigned less than: 32768 < 65535 -> true
        ulth(0x8000, 0xFFFF, 16, true),
        // 16-bit unsigned less than: 65535 < 32768 -> false
        ulth(0xFFFF, 0x8000, 16, false),
        // 32-bit unsigned less than: 2147483648 < 4294967295 -> true
        ulth(0x80000000, 0xFFFFFFFF, 32, true),
        // 32-bit unsigned less than: 4294967295 < 2147483648 -> false
        ulth(0xFFFFFFFF, 0x80000000, 32, false),
        // 64-bit unsigned less than: 9223372036854775808 < 18446744073709551615 -> true
        ulth(0x8000000000000000L, 0xFFFFFFFFFFFFFFFFL, 64, true),
        // 64-bit unsigned less than: 18446744073709551615 < 9223372036854775808 -> false
        ulth(0xFFFFFFFFFFFFFFFFL, 0x8000000000000000L, 64, false)
    );
  }

  private Function ulth(long a, long b, int size, boolean expected) {
    return binaryTest(BuiltInTable.ULTH, a, b, size, expected ? 0x1 : 0x0);
  }

  @TestFactory
  Stream<DynamicTest> sleqTests() {
    return runTests(
        // 1-bit signed less than or equal: 0 <= -1 -> false
        sleq(0x0, 0x1, 1, false),
        // 1-bit signed less than or equal: 1 <= 1 -> true
        sleq(0x1, 0x1, 1, true),
        // 1-bit signed less than or equal: -1 <= 0 -> true
        sleq(0x1, 0x0, 1, true),
        // 2-bit signed less than or equal: -2 <= -1 -> true
        sleq(-0x2, -0x1, 2, true),
        // 2-bit signed less than or equal: -1 <= -2 -> false
        sleq(-0x1, -0x2, 2, false),
        // 2-bit signed less than or equal: -1 <= -1 -> true
        sleq(-0x1, -0x1, 2, true),
        // 8-bit signed less than or equal: -128 <= 127 -> true
        sleq(-0x80, 0x7F, 8, true),
        // 8-bit signed less than or equal: 127 <= -128 -> false
        sleq(0x7F, -0x80, 8, false),
        // 8-bit signed less than or equal: -128 <= -128 -> true
        sleq(-0x80, -0x80, 8, true),
        // 16-bit signed less than or equal: -32768 <= 32767 -> true
        sleq(-0x8000, 0x7FFF, 16, true),
        // 16-bit signed less than or equal: 32767 <= -32768 -> false
        sleq(0x7FFF, -0x8000, 16, false),
        // 16-bit signed less than or equal: 32767 <= 32767 -> true
        sleq(0x7FFF, 0x7FFF, 16, true),
        // 32-bit signed less than or equal: -2147483648 <= 2147483647 -> true
        sleq(-0x80000000, 0x7FFFFFFF, 32, true),
        // 32-bit signed less than or equal: 2147483647 <= -2147483648 -> false
        sleq(0x7FFFFFFF, -0x80000000, 32, false),
        // 32-bit signed less than or equal: -2147483648 <= -2147483648 -> true
        sleq(-0x80000000, -0x80000000, 32, true),
        // 64-bit signed less than or equal: -9223372036854775808 <= 9223372036854775807 -> true
        sleq(-0x8000000000000000L, 0x7FFFFFFFFFFFFFFFL, 64, true),
        // 64-bit signed less than or equal: 9223372036854775807 <= -9223372036854775808 -> false
        sleq(0x7FFFFFFFFFFFFFFFL, -0x8000000000000000L, 64, false),
        // 64-bit signed less than or equal: -9223372036854775808 <= -9223372036854775808 -> true
        sleq(-0x8000000000000000L, -0x8000000000000000L, 64, true)
    );
  }

  private Function sleq(long a, long b, int size, boolean expected) {
    return binaryTest(BuiltInTable.SLEQ, a, b, size, expected ? 0x1 : 0x0);
  }

  @TestFactory
  Stream<DynamicTest> uleqTests() {
    return runTests(
        // 1-bit unsigned less than or equal: 0 <= 1 -> true
        uleq(0x0, 0x1, 1, true),
        // 1-bit unsigned less than or equal: 1 <= 1 -> true
        uleq(0x1, 0x1, 1, true),
        // 1-bit unsigned less than or equal: 1 <= 0 -> false
        uleq(0x1, 0x0, 1, false),
        // 2-bit unsigned less than or equal: 2 <= 3 -> true
        uleq(0x2, 0x3, 2, true),
        // 2-bit unsigned less than or equal: 3 <= 2 -> false
        uleq(0x3, 0x2, 2, false),
        // 2-bit unsigned less than or equal: 3 <= 3 -> true
        uleq(0x3, 0x3, 2, true),
        // 8-bit unsigned less than or equal: 128 <= 255 -> true
        uleq(0x80, 0xFF, 8, true),
        // 8-bit unsigned less than or equal: 255 <= 128 -> false
        uleq(0xFF, 0x80, 8, false),
        // 8-bit unsigned less than or equal: 128 <= 128 -> true
        uleq(0x80, 0x80, 8, true),
        // 16-bit unsigned less than or equal: 32768 <= 65535 -> true
        uleq(0x8000, 0xFFFF, 16, true),
        // 16-bit unsigned less than or equal: 65535 <= 32768 -> false
        uleq(0xFFFF, 0x8000, 16, false),
        // 16-bit unsigned less than or equal: 32768 <= 32768 -> true
        uleq(0x8000, 0x8000, 16, true),
        // 32-bit unsigned less than or equal: 2147483648 <= 4294967295 -> true
        uleq(0x80000000, 0xFFFFFFFF, 32, true),
        // 32-bit unsigned less than or equal: 4294967295 <= 2147483648 -> false
        uleq(0xFFFFFFFF, 0x80000000, 32, false),
        // 32-bit unsigned less than or equal: 4294967295 <= 4294967295 -> true
        uleq(0xFFFFFFFF, 0xFFFFFFFF, 32, true),
        // 64-bit unsigned less than or equal: 9223372036854775808 <= 18446744073709551615 -> true
        uleq(0x8000000000000000L, 0xFFFFFFFFFFFFFFFFL, 64, true),
        // 64-bit unsigned less than or equal: 18446744073709551615 <= 9223372036854775808 -> false
        uleq(0xFFFFFFFFFFFFFFFFL, 0x8000000000000000L, 64, false),
        // 64-bit unsigned less than or equal: 9223372036854775808 <= 9223372036854775808 -> true
        uleq(0x8000000000000000L, 0x8000000000000000L, 64, true)
    );
  }

  private Function uleq(long a, long b, int size, boolean expected) {
    return binaryTest(BuiltInTable.ULEQ, a, b, size, expected ? 0x1 : 0x0);
  }

  @TestFactory
  Stream<DynamicTest> sqthTests() {
    return runTests(
        // 1-bit signed greater than: -1 > 0 -> false
        sqth(0x1, 0x0, 1, false),
        // 1-bit signed greater than: 0 > -1 -> true
        sqth(0x0, 0x1, 1, true),
        // 2-bit signed greater than: -1 > -2 -> true
        sqth(-0x1, -0x2, 2, true),
        // 2-bit signed greater than: -2 > -1 -> false
        sqth(-0x2, -0x1, 2, false),
        // 2-bit signed greater than: -1 > -1 -> false
        sqth(-0x1, -0x1, 2, false),
        // 8-bit signed greater than: 127 > -128 -> true
        sqth(0x7F, -0x80, 8, true),
        // 8-bit signed greater than: -128 > 127 -> false
        sqth(-0x80, 0x7F, 8, false),
        // 8-bit signed greater than: 127 > 127 -> false
        sqth(0x7F, 0x7F, 8, false),
        // 16-bit signed greater than: 32767 > -32768 -> true
        sqth(0x7FFF, -0x8000, 16, true),
        // 16-bit signed greater than: -32768 > 32767 -> false
        sqth(-0x8000, 0x7FFF, 16, false),
        // 16-bit signed greater than: 32767 > 32767 -> false
        sqth(0x7FFF, 0x7FFF, 16, false),
        // 32-bit signed greater than: 2147483647 > -2147483648 -> true
        sqth(0x7FFFFFFF, -0x80000000, 32, true),
        // 32-bit signed greater than: -2147483648 > 2147483647 -> false
        sqth(-0x80000000, 0x7FFFFFFF, 32, false),
        // 32-bit signed greater than: -2147483648 > -2147483648 -> false
        sqth(-0x80000000, -0x80000000, 32, false),
        // 64-bit signed greater than: 9223372036854775807 > -9223372036854775808 -> true
        sqth(0x7FFFFFFFFFFFFFFFL, -0x8000000000000000L, 64, true),
        // 64-bit signed greater than: -9223372036854775808 > 9223372036854775807 -> false
        sqth(-0x8000000000000000L, 0x7FFFFFFFFFFFFFFFL, 64, false),
        // 64-bit signed greater than: -9223372036854775808 > -9223372036854775808 -> false
        sqth(-0x8000000000000000L, -0x8000000000000000L, 64, false)
    );
  }

  private Function sqth(long a, long b, int size, boolean expected) {
    return binaryTest(BuiltInTable.SGTH, a, b, size, expected ? 0x1 : 0x0);
  }

  @TestFactory
  Stream<DynamicTest> uqthTests() {
    return runTests(
        // 1-bit unsigned greater than: 1 > 0 -> true
        uqth(0x1, 0x0, 1, true),
        // 1-bit unsigned greater than: 0 > 1 -> false
        uqth(0x0, 0x1, 1, false),
        // 2-bit unsigned greater than: 3 > 2 -> true
        uqth(0x3, 0x2, 2, true),
        // 2-bit unsigned greater than: 2 > 3 -> false
        uqth(0x2, 0x3, 2, false),
        // 2-bit unsigned greater than: 3 > 3 -> false
        uqth(0x3, 0x3, 2, false),
        // 8-bit unsigned greater than: 255 > 128 -> true
        uqth(0xFF, 0x80, 8, true),
        // 8-bit unsigned greater than: 128 > 255 -> false
        uqth(0x80, 0xFF, 8, false),
        // 8-bit unsigned greater than: 255 > 255 -> false
        uqth(0xFF, 0xFF, 8, false),
        // 16-bit unsigned greater than: 65535 > 32768 -> true
        uqth(0xFFFF, 0x8000, 16, true),
        // 16-bit unsigned greater than: 32768 > 65535 -> false
        uqth(0x8000, 0xFFFF, 16, false),
        // 16-bit unsigned greater than: 65535 > 65535 -> false
        uqth(0xFFFF, 0xFFFF, 16, false),
        // 32-bit unsigned greater than: 4294967295 > 2147483648 -> true
        uqth(0xFFFFFFFFL, 0x80000000L, 32, true),
        // 32-bit unsigned greater than: 2147483648 > 4294967295 -> false
        uqth(0x80000000L, 0xFFFFFFFFL, 32, false),
        // 32-bit unsigned greater than: 4294967295 > 4294967295 -> false
        uqth(0xFFFFFFFFL, 0xFFFFFFFFL, 32, false),
        // 64-bit unsigned greater than: 18446744073709551615 > 9223372036854775808 -> true
        uqth(0xFFFFFFFFFFFFFFFFL, 0x8000000000000000L, 64, true),
        // 64-bit unsigned greater than: 9223372036854775808 > 18446744073709551615 -> false
        uqth(0x8000000000000000L, 0xFFFFFFFFFFFFFFFFL, 64, false),
        // 64-bit unsigned greater than: 18446744073709551615 > 18446744073709551615 -> false
        uqth(0xFFFFFFFFFFFFFFFFL, 0xFFFFFFFFFFFFFFFFL, 64, false)
    );
  }

  private Function uqth(long a, long b, int size, boolean expected) {
    return binaryTest(BuiltInTable.UGTH, a, b, size, expected ? 0x1 : 0x0);
  }

  @TestFactory
  Stream<DynamicTest> sgeqTests() {
    return runTests(
        // 1-bit signed greater than or equal: -1 >= 0 -> false
        sgeq(0x1, 0x0, 1, false),
        // 1-bit signed greater than or equal: 1 >= 1 -> true
        sgeq(0x1, 0x1, 1, true),
        // 1-bit signed greater than or equal: 0 >= -1 -> true
        sgeq(0x0, 0x1, 1, true),
        // 2-bit signed greater than or equal: -1 >= -2 -> true
        sgeq(-0x1, -0x2, 2, true),
        // 2-bit signed greater than or equal: -2 >= -1 -> false
        sgeq(-0x2, -0x1, 2, false),
        // 2-bit signed greater than or equal: -1 >= -1 -> true
        sgeq(-0x1, -0x1, 2, true),
        // 8-bit signed greater than or equal: 127 >= -128 -> true
        sgeq(0x7F, -0x80, 8, true),
        // 8-bit signed greater than or equal: -128 >= 127 -> false
        sgeq(-0x80, 0x7F, 8, false),
        // 8-bit signed greater than or equal: 127 >= 127 -> true
        sgeq(0x7F, 0x7F, 8, true),
        // 16-bit signed greater than or equal: 32767 >= -32768 -> true
        sgeq(0x7FFF, -0x8000, 16, true),
        // 16-bit signed greater than or equal: -32768 >= 32767 -> false
        sgeq(-0x8000, 0x7FFF, 16, false),
        // 16-bit signed greater than or equal: 32767 >= 32767 -> true
        sgeq(0x7FFF, 0x7FFF, 16, true),
        // 32-bit signed greater than or equal: 2147483647 >= -2147483648 -> true
        sgeq(0x7FFFFFFF, -0x80000000, 32, true),
        // 32-bit signed greater than or equal: -2147483648 >= 2147483647 -> false
        sgeq(-0x80000000, 0x7FFFFFFF, 32, false),
        // 32-bit signed greater than or equal: -2147483648 >= -2147483648 -> true
        sgeq(-0x80000000, -0x80000000, 32, true),
        // 64-bit signed greater than or equal: 9223372036854775807 >= -9223372036854775808 -> true
        sgeq(0x7FFFFFFFFFFFFFFFL, -0x8000000000000000L, 64, true),
        // 64-bit signed greater than or equal: -9223372036854775808 >= 9223372036854775807 -> false
        sgeq(-0x8000000000000000L, 0x7FFFFFFFFFFFFFFFL, 64, false),
        // 64-bit signed greater than or equal: -9223372036854775808 >= -9223372036854775808 -> true
        sgeq(-0x8000000000000000L, -0x8000000000000000L, 64, true)
    );
  }

  private Function sgeq(long a, long b, int size, boolean expected) {
    return binaryTest(BuiltInTable.SGEQ, a, b, size, expected ? 0x1 : 0x0);
  }

  @TestFactory
  Stream<DynamicTest> ugeqTests() {
    return runTests(
        // 1-bit unsigned greater than or equal: 1 >= 0 -> true
        ugeq(0x1, 0x0, 1, true),
        // 1-bit unsigned greater than or equal: 1 >= 1 -> true
        ugeq(0x1, 0x1, 1, true),
        // 1-bit unsigned greater than or equal: 0 >= 1 -> false
        ugeq(0x0, 0x1, 1, false),
        // 2-bit unsigned greater than or equal: 3 >= 2 -> true
        ugeq(0x3, 0x2, 2, true),
        // 2-bit unsigned greater than or equal: 2 >= 3 -> false
        ugeq(0x2, 0x3, 2, false),
        // 2-bit unsigned greater than or equal: 3 >= 3 -> true
        ugeq(0x3, 0x3, 2, true),
        // 8-bit unsigned greater than or equal: 255 >= 128 -> true
        ugeq(0xFF, 0x80, 8, true),
        // 8-bit unsigned greater than or equal: 128 >= 255 -> false
        ugeq(0x80, 0xFF, 8, false),
        // 8-bit unsigned greater than or equal: 255 >= 255 -> true
        ugeq(0xFF, 0xFF, 8, true),
        // 16-bit unsigned greater than or equal: 65535 >= 32768 -> true
        ugeq(0xFFFF, 0x8000, 16, true),
        // 16-bit unsigned greater than or equal: 32768 >= 65535 -> false
        ugeq(0x8000, 0xFFFF, 16, false),
        // 16-bit unsigned greater than or equal: 65535 >= 65535 -> true
        ugeq(0xFFFF, 0xFFFF, 16, true),
        // 32-bit unsigned greater than or equal: 4294967295 >= 2147483648 -> true
        ugeq(0xFFFFFFFF, 0x80000000, 32, true),
        // 32-bit unsigned greater than or equal: 2147483648 >= 4294967295 -> false
        ugeq(0x80000000, 0xFFFFFFFF, 32, false),
        // 32-bit unsigned greater than or equal: 4294967295 >= 4294967295 -> true
        ugeq(0xFFFFFFFF, 0xFFFFFFFF, 32, true),
        // 64-bit unsigned greater than or equal: 18446744073709551615 >= 9223372036854775808 -> true
        ugeq(0xFFFFFFFFFFFFFFFFL, 0x8000000000000000L, 64, true),
        // 64-bit unsigned greater than or equal: 9223372036854775808 >= 18446744073709551615 -> false
        ugeq(0x8000000000000000L, 0xFFFFFFFFFFFFFFFFL, 64, false),
        // 64-bit unsigned greater than or equal: 18446744073709551615 >= 18446744073709551615 -> true
        ugeq(0xFFFFFFFFFFFFFFFFL, 0xFFFFFFFFFFFFFFFFL, 64, true)
    );
  }

  private Function ugeq(long a, long b, int size, boolean expected) {
    return binaryTest(BuiltInTable.UGEQ, a, b, size, expected ? 0x1 : 0x0);
  }


  /// SHIFTING OPERATIONS ///

  @TestFactory
  Stream<DynamicTest> lslTests() {
    return runTests(
        // 1-bit logical shift left: 1 << 0 = 1
        lsl(0x1, 0x0, 1, 0x1),
        // 1-bit logical shift left: 1 << 1 = 0 (shift out of range for 1-bit)
        lsl(0x1, 0x1, 1, 0x0),
        // 2-bit logical shift left: 2 << 1 = 0 (shift out of range for 2-bit)
        lsl(0x2, 0x1, 2, 0x0),
        // 2-bit logical shift left: 1 << 1 = 2
        lsl(0x1, 0x1, 2, 0x2),
        // 8-bit logical shift left: 127 << 1 = 254
        lsl(0x7F, 0x1, 8, 0xFE),
        // 8-bit logical shift left: 128 << 1 = 0 (shift out of range for 8-bit)
        lsl(0x80, 0x1, 8, 0x0),
        // 16-bit logical shift left: 32767 << 1 = 65534
        lsl(0x7FFF, 0x1, 16, 0xFFFE),
        // 16-bit logical shift left: 32768 << 1 = 0 (shift out of range for 16-bit)
        lsl(0x8000, 0x1, 16, 0x0),
        // 32-bit logical shift left: 2147483647 << 1 = 4294967294
        lsl(0x7FFFFFFF, 0x1, 32, 0xFFFFFFFEL),
        // 32-bit logical shift left: 2147483648 << 1 = 0 (shift out of range for 32-bit)
        lsl(0x80000000L, 0x1, 32, 0x0),
        // 64-bit logical shift left: 9223372036854775807 << 1 = 18446744073709551614
        lsl(0x7FFFFFFFFFFFFFFFL, 0x1, 64, 0xFFFFFFFFFFFFFFFEL),
        // 64-bit logical shift left: 9223372036854775808 << 1 = 0 (shift out of range for 64-bit)
        lsl(0x8000000000000000L, 0x1, 64, 0x0)
    );
  }

  private Function lsl(long a, long b, int size, long expected) {
    return binaryTest(BuiltInTable.LSL, a, b, size, expected);
  }

  @TestFactory
  Stream<DynamicTest> asrTests() {
    return runTests(
        // 1-bit arithmetic shift right: 1 >> 0 = 1
        asr(0x1, 0x0, 1, 0x1),
        // 1-bit arithmetic shift right: 1 >> 1 = 1 (preserves sign)
        asr(0x1, 0x1, 1, 0x1),
        // 1-bit arithmetic shift right: 0 >> 1 = 0
        asr(0x0, 0x1, 1, 0x0),
        // 1-bit arithmetic shift right: -1 >> 1 = -1 (preserves sign)
        asr(-0x1, 0x1, 1, -0x1),
        // 2-bit arithmetic shift right: 2 >> 1 = 3
        asr(0x2, 0x1, 2, 0x3),
        // 2-bit arithmetic shift right: -2 >> 1 = -1
        asr(-0x2, 0x1, 2, -0x1),
        // 8-bit arithmetic shift right: 127 >> 1 = 63
        asr(0x7F, 0x1, 8, 0x3F),
        // 8-bit arithmetic shift right: -128 >> 1 = -64
        asr(-0x80, 0x1, 8, -0x40),
        // 8-bit arithmetic shift right: -1 >> 1 = -1
        asr(-0x1, 0x1, 8, -0x1),
        // 16-bit arithmetic shift right: 32767 >> 1 = 16383
        asr(0x7FFF, 0x1, 16, 0x3FFF),
        // 16-bit arithmetic shift right: -32768 >> 1 = -16384
        asr(-0x8000, 0x1, 16, -0x4000),
        // 32-bit arithmetic shift right: 2147483647 >> 1 = 1073741823
        asr(0x7FFFFFFF, 0x1, 32, 0x3FFFFFFF),
        // 32-bit arithmetic shift right: -2147483648 >> 1 = -1073741824
        asr(-0x80000000, 0x1, 32, -0x40000000),
        // 64-bit arithmetic shift right: 9223372036854775807 >> 1 = 4611686018427387903
        asr(0x7FFFFFFFFFFFFFFFL, 0x1, 64, 0x3FFFFFFFFFFFFFFFL),
        // 64-bit arithmetic shift right: -9223372036854775808 >> 1 = -4611686018427387904
        asr(-0x8000000000000000L, 0x1, 64, -0x4000000000000000L)
    );
  }

  private Function asr(long a, long b, int size, long expected) {
    return binaryTest(BuiltInTable.ASR, a, b, size, expected);
  }

  @TestFactory
  Stream<DynamicTest> lsrTests() {
    return runTests(
        // 1-bit logical shift right: 1 >> 0 = 1
        lsr(0x1, 0x0, 1, 0x1),
        // 1-bit logical shift right: 1 >> 1 = 0
        lsr(0x1, 0x1, 1, 0x0),
        // 2-bit logical shift right: 2 >> 1 = 1
        lsr(0x2, 0x1, 2, 0x1),
        // 2-bit logical shift right: 3 >> 1 = 1
        lsr(0x3, 0x1, 2, 0x1),
        // 8-bit logical shift right: 255 >> 1 = 127
        lsr(0xFF, 0x1, 8, 0x7F),
        // 8-bit logical shift right: 128 >> 1 = 64
        lsr(0x80, 0x1, 8, 0x40),
        // 8-bit logical shift right: 0 >> 1 = 0
        lsr(0x0, 0x1, 8, 0x0),
        // 16-bit logical shift right: 65535 >> 1 = 32767
        lsr(0xFFFF, 0x1, 16, 0x7FFF),
        // 16-bit logical shift right: 32768 >> 1 = 16384
        lsr(0x8000, 0x1, 16, 0x4000),
        // 32-bit logical shift right: 4294967295 >> 1 = 2147483647
        lsr(0xFFFFFFFFL, 0x1, 32, 0x7FFFFFFF),
        // 32-bit logical shift right: 2147483648 >> 1 = 1073741824
        lsr(0x80000000L, 0x1, 32, 0x40000000),
        // 64-bit logical shift right: 18446744073709551615 >> 1 = 9223372036854775807
        lsr(0xFFFFFFFFFFFFFFFFL, 0x1, 64, 0x7FFFFFFFFFFFFFFFL),
        // 64-bit logical shift right: 9223372036854775808 >> 1 = 4611686018427387904
        lsr(0x8000000000000000L, 0x1, 64, 0x4000000000000000L)
    );
  }

  private Function lsr(long a, long b, int size, long expected) {
    return binaryTest(BuiltInTable.LSR, a, b, size, expected);
  }

  @TestFactory
  Stream<DynamicTest> rolTests() {
    return runTests(
        // 1-bit rotate left: 1 << 0 (mod 1) = 1
        rol(0x1, 0x0, 1, 0x1),
        // 1-bit rotate left: 1 << 1 (mod 1) = 1
        rol(0x1, 0x1, 1, 0x1),
        // 2-bit rotate left: 1 << 1 (mod 2) = 2
        rol(0x1, 0x1, 2, 0x2),
        // 2-bit rotate left: 2 << 1 (mod 2) = 1
        rol(0x2, 0x1, 2, 0x1),
        // 8-bit rotate left: 0x80 << 1 (mod 8) = 0x1
        rol(0x80, 0x1, 8, 0x1),
        // 8-bit rotate left: 0x7F << 1 (mod 8) = 0xFE
        rol(0x7F, 0x1, 8, 0xFE),
        // 16-bit rotate left: 0x8000 << 1 (mod 16) = 0x1
        rol(0x8000, 0x1, 16, 0x1),
        // 16-bit rotate left: 0x7FFF << 1 (mod 16) = 0xFFFE
        rol(0x7FFF, 0x1, 16, 0xFFFE),
        // 32-bit rotate left: 0x80000000 << 1 (mod 32) = 0x1
        rol(0x80000000L, 0x1, 32, 0x1),
        // 32-bit rotate left: 0x7FFFFFFF << 1 (mod 32) = 0xFFFFFFFE
        rol(0x7FFFFFFF, 0x1, 32, 0xFFFFFFFEL),
        // 64-bit rotate left: 0x8000000000000000 << 1 (mod 64) = 0x1
        rol(0x8000000000000000L, 0x1, 64, 0x1),
        // 64-bit rotate left: 0x7FFFFFFFFFFFFFFF << 1 (mod 64) = 0xFFFFFFFFFFFFFFFE
        rol(0x7FFFFFFFFFFFFFFFL, 0x1, 64, 0xFFFFFFFFFFFFFFFEL)
    );
  }

  private Function rol(long a, long b, int size, long expected) {
    return binaryTest(BuiltInTable.ROL, a, b, size, expected);
  }

  @TestFactory
  Stream<DynamicTest> rorTests() {
    return runTests(
        // 1-bit rotate right: 1 >> 0 (mod 1) = 1
        ror(0x1, 0x0, 1, 0x1),
        // 1-bit rotate right: 1 >> 1 (mod 1) = 1
        ror(0x1, 0x1, 1, 0x1),
        // 2-bit rotate right: 1 >> 1 (mod 2) = 2
        ror(0x1, 0x1, 2, 0x2),
        // 2-bit rotate right: 2 >> 1 (mod 2) = 1
        ror(0x2, 0x1, 2, 0x1),
        // 8-bit rotate right: 0x80 >> 1 (mod 8) = 0x40
        ror(0x80, 0x1, 8, 0x40),
        // 8-bit rotate right: 0x01 >> 1 (mod 8) = 0x80
        ror(0x01, 0x1, 8, 0x80),
        // 16-bit rotate right: 0x8000 >> 1 (mod 16) = 0x4000
        ror(0x8000, 0x1, 16, 0x4000),
        // 16-bit rotate right: 0x0001 >> 1 (mod 16) = 0x8000
        ror(0x0001, 0x1, 16, 0x8000),
        // 32-bit rotate right: 0x80000000 >> 1 (mod 32) = 0x40000000
        ror(0x80000000L, 0x1, 32, 0x40000000),
        // 32-bit rotate right: 0x00000001 >> 1 (mod 32) = 0x80000000
        ror(0x00000001, 0x1, 32, 0x80000000L),
        // 64-bit rotate right: 0x8000000000000000 >> 1 (mod 64) = 0x4000000000000000
        ror(0x8000000000000000L, 0x1, 64, 0x4000000000000000L),
        // 64-bit rotate right: 0x0000000000000001 >> 1 (mod 64) = 0x8000000000000000
        ror(0x0000000000000001L, 0x1, 64, 0x8000000000000000L)
    );
  }


  private Function ror(long a, long b, int size, long expected) {
    return binaryTest(BuiltInTable.ROR, a, b, size, expected);
  }

  @TestFactory
  Stream<DynamicTest> rrxTests() {
    return runTests(
        // 1-bit rotate right with carry: 1 >> 1 + carry(0) = 0
        rrx(0x1, 0x1, false, 1, 0x0),
        // 1-bit rotate right with carry: 1 >> 1 + carry(1) = 1
        rrx(0x1, 0x1, true, 1, 0x1),
        // 2-bit rotate right with carry: 2 >> 1 + carry(0) = 1
        rrx(0x2, 0x1, false, 2, 0x1),
        // 2-bit rotate right with carry: 2 >> 1 + carry(1) = 3
        rrx(0x2, 0x1, true, 2, 0x3),
        // 8-bit rotate right with carry: 0x80 >> 1 + carry(0) = 0x40
        rrx(0x80, 0x1, false, 8, 0x40),
        // 8-bit rotate right with carry: 0x80 >> 1 + carry(1) = 0xC0
        rrx(0x80, 0x1, true, 8, 0xC0),
        // 8-bit rotate right with carry: 0x01 >> 1 + carry(1) = 0x80
        rrx(0x01, 0x1, true, 8, 0x80),
        // 16-bit rotate right with carry: 0x8000 >> 1 + carry(0) = 0x4000
        rrx(0x8000, 0x1, false, 16, 0x4000),
        // 16-bit rotate right with carry: 0x8000 >> 1 + carry(1) = 0xC000
        rrx(0x8000, 0x1, true, 16, 0xC000),
        // 32-bit rotate right with carry: 0x80000000 >> 1 + carry(0) = 0x40000000
        rrx(0x80000000L, 0x1, false, 32, 0x40000000),
        // 32-bit rotate right with carry: 0x80000000 >> 1 + carry(1) = 0xC0000000
        rrx(0x80000000L, 0x1, true, 32, 0xC0000000L),
        // 64-bit rotate right with carry: 0x8000000000000000 >> 1 + carry(0) = 0x4000000000000000
        rrx(0x8000000000000000L, 0x1, false, 64, 0x4000000000000000L),
        // 64-bit rotate right with carry: 0x8000000000000000 >> 1 + carry(1) = 0xC000000000000000
        rrx(0x8000000000000000L, 0x1, true, 64, 0xC000000000000000L)
    );
  }

  private Function rrx(long a, long b, boolean carry, int size, long expected) {
    a = norm(a, size);
    b = norm(b, size);
    expected = norm(expected, size);
    var builtIn = BuiltInTable.RRX;
    var name = builtIn.name().toLowerCase() + "_" + counter++;
    name = name.replaceFirst("vadl::", "");
    return genericFunc(name, binaryOp(BuiltInTable.EQU,
        Type.bool(),
        new BuiltInCall(builtIn,
            new NodeList<>(
                GraphUtils.bitsNode(a, size),
                GraphUtils.bitsNode(b, size),
                GraphUtils.bitsNode(carry ? 1 : 0, 1)
            ),
            Type.bits(size)
        ),
        bitsNode(expected, size)));
  }

  /// BIT COUNTING OPERATIONS ///

  @TestFactory
  Stream<DynamicTest> cobTests() {
    return runTests(
        // 1-bit count ones: 1 has 1 one
        cob(0x1, 1, 0x1),
        // 1-bit count ones: 0 has 0 ones
        cob(0x0, 1, 0x0),
        // 2-bit count ones: 3 (0b11) has 2 ones
        cob(0x3, 2, 0x2),
        // 2-bit count ones: 2 (0b10) has 1 one
        cob(0x2, 2, 0x1),
        // 8-bit count ones: 255 (0xFF) has 8 ones
        cob(0xFF, 8, 0x8),
        // 8-bit count ones: 128 (0x80) has 1 one
        cob(0x80, 8, 0x1),
        // 8-bit count ones: 0 has 0 ones
        cob(0x00, 8, 0x0),
        // 16-bit count ones: 65535 (0xFFFF) has 16 ones
        cob(0xFFFF, 16, 0x10),
        // 16-bit count ones: 32768 (0x8000) has 1 one
        cob(0x8000, 16, 0x1),
        // 32-bit count ones: 4294967295 (0xFFFFFFFF) has 32 ones
        cob(0xFFFFFFFFL, 32, 0x20),
        // 32-bit count ones: 2147483648 (0x80000000) has 1 one
        cob(0x80000000L, 32, 0x1),
        // 64-bit count ones: 18446744073709551615 (0xFFFFFFFFFFFFFFFF) has 64 ones
        cob(0xFFFFFFFFFFFFFFFFL, 64, 0x40),
        // 64-bit count ones: 9223372036854775808 (0x8000000000000000) has 1 one
        cob(0x8000000000000000L, 64, 0x1),
        // 64-bit count ones: 0 has 0 ones
        cob(0x0000000000000000L, 64, 0x0)
    );
  }

  private Function cob(long a, int size, long expected) {
    return unaryTest(BuiltInTable.COB, a, size, expected);
  }

  @TestFactory
  Stream<DynamicTest> czbTests() {
    return runTests(
        // 1-bit count zeros: 1 has 0 zeros
        czb(0x1, 1, 0x0),
        // 1-bit count zeros: 0 has 1 zero
        czb(0x0, 1, 0x1),
        // 2-bit count zeros: 3 (0b11) has 0 zeros
        czb(0x3, 2, 0x0),
        // 2-bit count zeros: 2 (0b10) has 1 zero
        czb(0x2, 2, 0x1),
        // 8-bit count zeros: 255 (0xFF) has 0 zeros
        czb(0xFF, 8, 0x0),
        // 8-bit count zeros: 128 (0x80) has 7 zeros
        czb(0x80, 8, 0x7),
        // 8-bit count zeros: 0 has 8 zeros
        czb(0x00, 8, 0x8),
        // 16-bit count zeros: 65535 (0xFFFF) has 0 zeros
        czb(0xFFFF, 16, 0x0),
        // 16-bit count zeros: 32768 (0x8000) has 15 zeros
        czb(0x8000, 16, 0xF),
        // 32-bit count zeros: 4294967295 (0xFFFFFFFF) has 0 zeros
        czb(0xFFFFFFFFL, 32, 0x0),
        // 32-bit count zeros: 2147483648 (0x80000000) has 31 zeros
        czb(0x80000000L, 32, 0x1F),
        // 64-bit count zeros: 18446744073709551615 (0xFFFFFFFFFFFFFFFF) has 0 zeros
        czb(0xFFFFFFFFFFFFFFFFL, 64, 0x0),
        // 64-bit count zeros: 9223372036854775808 (0x8000000000000000) has 63 zeros
        czb(0x8000000000000000L, 64, 0x3F),
        // 64-bit count zeros: 0 has 64 zeros
        czb(0x0000000000000000L, 64, 0x40)
    );
  }

  private Function czb(long a, int size, long expected) {
    return unaryTest(BuiltInTable.CZB, a, size, expected);
  }

  @TestFactory
  Stream<DynamicTest> clzTests() {
    return runTests(
        // 1-bit count leading zeros: 0 has 1 leading zero
        clz(0x0, 1, 0x1),
        // 1-bit count leading zeros: 1 has 0 leading zeros
        clz(0x1, 1, 0x0),
        // 2-bit count leading zeros: 0 (0b00) has 2 leading zeros
        clz(0x0, 2, 0x2),
        // 2-bit count leading zeros: 1 (0b01) has 1 leading zero
        clz(0x1, 2, 0x1),
        // 2-bit count leading zeros: 2 (0b10) has 0 leading zeros
        clz(0x2, 2, 0x0),
        // 8-bit count leading zeros: 255 (0xFF) has 0 leading zeros
        clz(0xFF, 8, 0x0),
        // 8-bit count leading zeros: 128 (0x80) has 0 leading zeros
        clz(0x80, 8, 0x0),
        // 8-bit count leading zeros: 1 (0x01) has 7 leading zeros
        clz(0x01, 8, 0x7),
        // 8-bit count leading zeros: 0 has 8 leading zeros
        clz(0x00, 8, 0x8),
        // 16-bit count leading zeros: 32768 (0x8000) has 0 leading zeros
        clz(0x8000, 16, 0x0),
        // 16-bit count leading zeros: 1 (0x0001) has 15 leading zeros
        clz(0x0001, 16, 0xF),
        // 16-bit count leading zeros: 0 has 16 leading zeros
        clz(0x0000, 16, 0x10),
        // 32-bit count leading zeros: 2147483648 (0x80000000) has 0 leading zeros
        clz(0x80000000L, 32, 0x0),
        // 32-bit count leading zeros: 1 (0x00000001) has 31 leading zeros
        clz(0x00000001, 32, 0x1F),
        // 32-bit count leading zeros: 0 has 32 leading zeros
        clz(0x00000000, 32, 0x20),
        // 64-bit count leading zeros: 9223372036854775808 (0x8000000000000000) has 0 leading zeros
        clz(0x8000000000000000L, 64, 0x0),
        // 64-bit count leading zeros: 1 (0x0000000000000001) has 63 leading zeros
        clz(0x0000000000000001L, 64, 0x3F),
        // 64-bit count leading zeros: 0 has 64 leading zeros
        clz(0x0000000000000000L, 64, 0x40)
    );
  }

  private Function clz(long a, int size, long expected) {
    return unaryTest(BuiltInTable.CLZ, a, size, expected);
  }

  @TestFactory
  Stream<DynamicTest> cloTests() {
    return runTests(
        // 1-bit count leading ones: 0 has 0 leading ones
        clo(0x0, 1, 0x0),
        // 1-bit count leading ones: 1 has 1 leading one
        clo(0x1, 1, 0x1),
        // 2-bit count leading ones: 0b00 has 0 leading ones
        clo(0x0, 2, 0x0),
        // 2-bit count leading ones: 0b01 has 0 leading ones
        clo(0x1, 2, 0x0),
        // 2-bit count leading ones: 0b11 has 2 leading ones
        clo(0x3, 2, 0x2),
        // 8-bit count leading ones: 0xFF has 8 leading ones
        clo(0xFF, 8, 0x8),
        // 8-bit count leading ones: 0x80 has 1 leading one
        clo(0x80, 8, 0x1),
        // 8-bit count leading ones: 0x7F has 0 leading ones
        clo(0x7F, 8, 0x0),
        // 16-bit count leading ones: 0xFFFF has 16 leading ones
        clo(0xFFFF, 16, 0x10),
        // 16-bit count leading ones: 0x8000 has 1 leading one
        clo(0x8000, 16, 0x1),
        // 16-bit count leading ones: 0x7FFF has 0 leading ones
        clo(0x7FFF, 16, 0x0),
        // 32-bit count leading ones: 0xFFFFFFFF has 32 leading ones
        clo(0xFFFFFFFFL, 32, 0x20),
        // 32-bit count leading ones: 0x80000000 has 1 leading one
        clo(0x80000000L, 32, 0x1),
        // 32-bit count leading ones: 0x7FFFFFFF has 0 leading ones
        clo(0x7FFFFFFF, 32, 0x0),
        // 64-bit count leading ones: 0xFFFFFFFFFFFFFFFF has 64 leading ones
        clo(0xFFFFFFFFFFFFFFFFL, 64, 0x40),
        // 64-bit count leading ones: 0x8000000000000000 has 1 leading one
        clo(0x8000000000000000L, 64, 0x1),
        // 64-bit count leading ones: 0x7FFFFFFFFFFFFFFF has 0 leading ones
        clo(0x7FFFFFFFFFFFFFFFL, 64, 0x0)
    );
  }

  private Function clo(long a, int size, long expected) {
    return unaryTest(BuiltInTable.CLO, a, size, expected);
  }

  @TestFactory
  Stream<DynamicTest> clsTests() {
    return runTests(
        // 1-bit count leading sign bits: 0 has 0 leading sign bits (same as MSB)
        cls(0x0, 1, 0x0),
        // 1-bit count leading sign bits: 1 has 0 leading sign bits (same as MSB)
        cls(0x1, 1, 0x0),
        // 2-bit count leading sign bits: 0b00 has 1 leading sign bit
        cls(0x0, 2, 0x1),
        // 2-bit count leading sign bits: 0b01 has 0 leading sign bits
        cls(0x1, 2, 0x0),
        // 2-bit count leading sign bits: 0b11 has 1 leading sign bit
        cls(0x3, 2, 0x1),
        // 2-bit count leading sign bits: 0b10 has 0 leading sign bits
        cls(0x2, 2, 0x0),
        // 8-bit count leading sign bits: 0xFF (signed -1) has 7 leading sign bits
        cls(0xFF, 8, 0x7),
        cls(0x80, 8, 0x0),
        // 8-bit count leading sign bits: 0x7F (signed 127) has 0 leading sign bits
        cls(0x7F, 8, 0x0),
        cls(0x7ffa, 15, 11),
        // 16-bit count leading sign bits: 0xFFFF (signed -1) has 15 leading sign bits
        cls(0xFFFF, 16, 0xF),
        // 16-bit count leading sign bits: 0x8000 (signed -32768) has 15 leading sign bits
        cls(0x8000, 16, 0),
        // 16-bit count leading sign bits: 0x7FFF (signed 32767) has 0 leading sign bits
        cls(0x7FFF, 16, 0x0),
        // 32-bit count leading sign bits: 0xFFFFFFFF (signed -1) has 31 leading sign bits
        cls(0xFFFFFFFFL, 32, 0x1F),
        // 32-bit count leading sign bits: 0x80000000 (signed -2147483648) has 31 leading sign bits
        cls(0x80000000L, 32, 0x00),
        // 32-bit count leading sign bits: 0x7FFFFFFF (signed 2147483647) has 0 leading sign bits
        cls(0x7FFFFFFF, 32, 0x0),
        // 64-bit count leading sign bits: 0xFFFFFFFFFFFFFFFF (signed -1) has 63 leading sign bits
        cls(0xFFFFFFFFFFFFFFFFL, 64, 0x3F),
        // 64-bit count leading sign bits: 0x8000000000000000 (signed -9223372036854775808) has 63 leading sign bits
        cls(0x8000000000000000L, 64, 0x0),
        // 64-bit count leading sign bits: 0x7FFFFFFFFFFFFFFF (signed 9223372036854775807) has 0 leading sign bits
        cls(0x7FFFFFFFFFFFFFFFL, 64, 0x0)
    );
  }

  private Function cls(long a, int size, long expected) {
    return unaryTest(BuiltInTable.CLS, a, size, expected);
  }

  /* ───── Count Trailing Zeros ────────────────────────────── */
  @TestFactory
  Stream<DynamicTest> ctzTests() {
    return runTests(
        // 1-bit
        ctz(0x0, 1, 0x1),           // 0 → 1 trailing zero
        ctz(0x1, 1, 0x0),           // 1 → 0
        // 2-bit
        ctz(0x0, 2, 0x2),
        ctz(0x1, 2, 0x0),
        ctz(0x2, 2, 0x1),           // 10b → 1
        // 8-bit
        ctz(0xFF, 8, 0x0),
        ctz(0x80, 8, 0x7),
        ctz(0x01, 8, 0x0),
        ctz(0x00, 8, 0x8),
        // 16-bit
        ctz(0x8000, 16, 0xF),
        ctz(0x0001, 16, 0x0),
        ctz(0x0000, 16, 0x10),
        // 32-bit
        ctz(0x80000000L, 32, 0x1F),
        ctz(0x00000001, 32, 0x0),
        ctz(0x00000000, 32, 0x20),
        // 64-bit
        ctz(0x8000000000000000L, 64, 0x3F),
        ctz(0x0000000000000001L, 64, 0x0),
        ctz(0x0000000000000000L, 64, 0x40)
    );
  }

  private Function ctz(long a, int size, long expected) {
    return unaryTest(BuiltInTable.CTZ, a, size, expected);
  }

  /* ───── Count Trailing Ones ─────────────────────────────── */
  @TestFactory
  Stream<DynamicTest> ctoTests() {
    return runTests(
        // 1-bit
        cto(0x0, 1, 0x0),
        cto(0x1, 1, 0x1),
        // 2-bit
        cto(0x0, 2, 0x0),
        cto(0x1, 2, 0x1),
        cto(0x3, 2, 0x2),           // 11b → 2
        // 8-bit
        cto(0xFF, 8, 0x8),
        cto(0x01, 8, 0x1),
        cto(0xFE, 8, 0x0),
        // 16-bit
        cto(0xFFFF, 16, 0x10),
        cto(0x0001, 16, 0x1),
        cto(0xFFFE, 16, 0x0),
        // 32-bit
        cto(0xFFFFFFFFL, 32, 0x20),
        cto(0x00000001, 32, 0x1),
        cto(0xFFFFFFFE, 32, 0x0),
        // 64-bit
        cto(0xFFFFFFFFFFFFFFFFL, 64, 0x40),
        cto(0x0000000000000001L, 64, 0x1),
        cto(0xFFFFFFFFFFFFFFFEL, 64, 0x0)
    );
  }

  private Function cto(long a, int size, long expected) {
    return unaryTest(BuiltInTable.CTO, a, size, expected);
  }

  /// TEST HELPER FUNCTIONS ///

  private Function unaryTest(BuiltInTable.BuiltIn builtIn, long val, int size, long expected) {
    val = norm(val, size);
    expected = norm(expected, size);
    var name = builtIn.name() + "_" + counter++;
    name = name.replaceFirst("VADL::", "");
    return genericFunc(name, binaryOp(BuiltInTable.EQU,
        Type.bool(),
        unaryOp(builtIn, GraphUtils.bits(val, size)),
        bitsNode(expected, size)));
  }

  private Function binaryTest(BuiltInTable.BuiltIn builtIn, long a, long b, int size,
                              long expected) {
    return binaryTest(builtIn, a, b, size, expected, size);
  }

  private Function binaryTest(BuiltInTable.BuiltIn builtIn, long a, long b, int size,
                              long expected, int outSize) {
    a = norm(a, size);
    b = norm(b, size);
    expected = norm(expected, outSize);
    var name = builtIn.name().toLowerCase() + "_" + counter++;
    name = name.replaceFirst("vadl::", "");
    return genericFunc(name, binaryOp(BuiltInTable.EQU,
        Type.bool(),
        binaryOp(builtIn,
            Type.bits(outSize),
            GraphUtils.bitsNode(a, size),
            GraphUtils.bitsNode(b, size)),
        bitsNode(expected, outSize)));
  }

  // normalizes a number to its two's positive complement
  private long norm(long val, int size) {

    // normalize val to a two's complement representation
    var mask = size < 64 ? (1L << size) - 1 : 0xffffffffffffffffL;
    return val & mask;

  }

  private Stream<DynamicTest> runTests(Function... functions) {
    try {

      var testDir = Path.of("build/test-builtin-c-test");
      var mainC = testDir.resolve("main.c");
      var builtinLib = testDir.resolve("vadl-builtins.h");

      Files.createDirectories(mainC.getParent());

      produceMain(mainC, List.of(functions));

      try (var in = BuiltInTable.class.getResourceAsStream("/templates/common/vadl-builtins.h")) {
        Files.write(builtinLib, Objects.requireNonNull(in).readAllBytes());
      }
      var gccImage = new ImageFromDockerfile().withDockerfileFromBuilder(builder -> builder
          .from("gcc:latest"));


      AtomicReference<String> stdout = new AtomicReference<>();
      runContainer(gccImage,
          gccContainer -> gccContainer.withCopyFileToContainer(MountableFile.forHostPath(mainC),
                  "/test/main.c")
              .withCopyFileToContainer(MountableFile.forHostPath(builtinLib),
                  "/test/vadl-builtins.h")
              .withWorkingDirectory("/test")
              .withCommand("sh", "-c", "gcc main.c -o main && ./main"),
          container -> stdout.set(container.getLogs(OutputFrame.OutputType.STDOUT))
      );

      var testResults = getResults(stdout.get());

      return Stream.of(functions)
          .map(f -> dynamicTestOf(f, testResults));

    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private DynamicTest dynamicTestOf(Function function, Map<String, Boolean> testOutcome) {
    var testResult = testOutcome.get(function.simpleName());
    return DynamicTest.dynamicTest(function.simpleName(), () -> {
      var returnExpr = getSingleNode(function.behavior(), ReturnNode.class);
      System.out.println("------------");
      System.out.println("Test pretty print:\n" + returnExpr.value().prettyPrint());
      System.out.println("\nTest function: \n"
          + new PureFunctionCodeGenerator(function).genFunctionDefinition());

      assertThat(testOutcome.get(function.simpleName())).isTrue();
    });
  }

  private Map<String, Boolean> getResults(String stdout) {
    return stdout.lines()
        .collect(Collectors.toMap(l -> l.split(":")[0], l -> l.split(":")[1].equals("success")));
  }

  private void produceMain(Path mainC, List<Function> tests) throws IOException {
    var testDefinitions =
        tests.stream().map(t -> new PureFunctionCodeGenerator(t).genFunctionDefinition())
            .collect(Collectors.joining("\n\n"));

    var testCalls = tests.stream().map(t -> "\ttest(\"%s\", %s);"
        .formatted(t.simpleName(), t.simpleName())
    ).collect(Collectors.joining("\n"));

    var testVerifier = """
        void test(const char* name, bool (*test_func)(void)) {
            if (!test_func()) {
                printf("%s:failed\\n", name);
            } else {
                printf("%s:success\\n", name);
            }
        }
        """;

    var main = """
        #include <stdio.h>
        #include <stdbool.h>
        #include "vadl-builtins.h"
        
        %s
        
        // Test function helper
        %s
        
        int main() {
        %s
          return 0;
        }
        """.formatted(testDefinitions, testVerifier, testCalls);

    Files.writeString(mainC, main);
  }

  private Function genericFunc(String name, ExpressionNode returnValue) {
    var behavior = genericFuncBehavior(name, returnValue);
    return new Function(Identifier.noLocation(name), new Parameter[] {}, returnValue.type(),
        behavior);
  }

  private Graph genericFuncBehavior(String name, ExpressionNode returnValue) {
    var behavior = new Graph(name);
    var ret = behavior.addWithInputs(new ReturnNode(returnValue));
    behavior.add(new StartNode(ret));
    return behavior;
  }

}
