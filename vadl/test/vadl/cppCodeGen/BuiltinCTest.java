package vadl.cppCodeGen;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static vadl.utils.GraphUtils.binaryOp;
import static vadl.utils.GraphUtils.bitsNode;
import static vadl.utils.GraphUtils.getSingleNode;
import static vadl.utils.GraphUtils.unaryOp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
import vadl.viam.Function;
import vadl.viam.Identifier;
import vadl.viam.Parameter;
import vadl.viam.graph.Graph;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.control.StartNode;
import vadl.viam.graph.dependency.ExpressionNode;

public class BuiltinCTest extends DockerExecutionTest {

  int counter = 0;

  @BeforeEach
  void beforeEach() {
    counter = 0;
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

  private Function unaryTest(BuiltInTable.BuiltIn builtIn, long val, int size, long expected) {
    val = norm(val, size);
    expected = norm(expected, size);
    var name = builtIn.name() + "_" + counter++;
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
      System.out.println("\nTest function: \n" +
          new PureFunctionCodeGenerator(function).genFunctionDefinition());

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
